package com.coto.premiumuuid.listener;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.cache.UUIDCache.CacheEntry;
import com.coto.premiumuuid.config.PluginConfig;
import com.coto.premiumuuid.mojang.MojangApiClient;
import com.coto.premiumuuid.mojang.MojangApiClient.Failure;
import com.coto.premiumuuid.mojang.MojangApiClient.Success;
import com.coto.premiumuuid.override.OverrideStore;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core listener handling UUID resolution in two steps:
 * 1. AsyncPreLogin (LOWEST): Fetches Premium UUID via API/Cache asynchronously.
 * 2. SyncLogin (HIGHEST): Intercepts Ban/Whitelist kicks to verify all possible identities.
 */
public final class PreLoginListener implements Listener {

    private final PluginConfig config;
    private final UUIDCache cache;
    private final MojangApiClient mojangApi;
    private final OverrideStore overrides;
    private final Logger logger;
    private final org.bukkit.plugin.Plugin plugin;

    public PreLoginListener(PluginConfig config, UUIDCache cache, MojangApiClient mojangApi,
                            OverrideStore overrides, Logger logger, org.bukkit.plugin.Plugin plugin) {
        this.config = config;
        this.cache = cache;
        this.mojangApi = mojangApi;
        this.overrides = overrides;
        this.logger = logger;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String key = username.toLowerCase();

        Boolean override = overrides.get(key);
        if (override != null) {
            if (!override) { // if inactive, keep offline UUID
                if (config.isDebugLogging()) {
                    UUID offlineUuid = MojangApiClient.computeOfflineUUID(username);
                    logger.info("[DEBUG] Player '" + username + "' has INACTIVE override → offline UUID " + offlineUuid);
                }
                return;
            }
            if (config.isDebugLogging()) {
                logger.info("[DEBUG] Player '" + username + "' has ACTIVE override → forcing premium check.");
            }
        } else {
            if (!config.isEnabled()) return;
        }

        CacheEntry cached = cache.get(key);
        if (cached != null && cache.isValid(cached, config.getCacheTtlMinutes())) {
            applyFromCache(event, cached, username, key, "valid cache");
            return;
        }

        MojangApiClient.LookupResult result = mojangApi.lookup(username, config.getTimeoutMs(), config.isDebugLogging());

        if (result instanceof Success success) {
            handleSuccess(event, success, key);
        } else if (result instanceof Failure failure) {
            handleFailure(event, failure, cached, username, key);
        }
    }

    private void handleSuccess(AsyncPlayerPreLoginEvent event, Success success, String key) {
        CacheEntry newEntry = new CacheEntry(success.uuid(), success.premium(), System.currentTimeMillis());
        cache.put(key, newEntry);

        if (success.premium()) {
            setEventUUID(event, success.uuid());
            logger.info("Player '" + success.correctName() + "' identified as PREMIUM → UUID " + success.uuid());
        } else {
            UUID offlineUuid = MojangApiClient.computeOfflineUUID(event.getName());
            logger.info("Player '" + event.getName() + "' is NOT premium → offline UUID " + offlineUuid);
        }
    }

    private void handleFailure(AsyncPlayerPreLoginEvent event, Failure failure, CacheEntry cached, String username, String key) {
        if (cached != null) {
            applyFromCache(event, cached, username, key, "expired cache (API failure: " + failure.reason() + ")");
        } else {
            UUID offlineUuid = MojangApiClient.computeOfflineUUID(username);
            if (config.isFallbackLogWarning()) {
                logger.warning("Mojang API check failed for '" + username + "': " + failure.reason()
                        + ". No cache available, using offline UUID " + offlineUuid);
            }
        }
    }

    private void applyFromCache(AsyncPlayerPreLoginEvent event, CacheEntry cached, String username, String key, String source) {
        if (cached.premium()) {
            setEventUUID(event, cached.uuid());
            logger.info("Player '" + username + "' identified as PREMIUM from " + source + " → UUID " + cached.uuid());
        } else {
            UUID offlineUuid = MojangApiClient.computeOfflineUUID(username);
            if (config.isDebugLogging()) {
                logger.info("[DEBUG] Player '" + username + "' is NOT premium from " + source + " → offline UUID " + offlineUuid);
            }
        }
    }

    private void setEventUUID(AsyncPlayerPreLoginEvent event, UUID uuid) {
        PlayerProfile profile = Bukkit.createProfile(uuid, event.getName());
        event.setPlayerProfile(profile);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(org.bukkit.event.player.PlayerLoginEvent event) {
        String name = event.getPlayer().getName();
        UUID currentUuid = event.getPlayer().getUniqueId();

        String banMsg = getAnyIdentityBanMessage(name, currentUuid);
        if (banMsg != null) {
            if (event.getResult() != org.bukkit.event.player.PlayerLoginEvent.Result.KICK_BANNED) {
                event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_BANNED, banMsg);
            }
            return;
        } else if (event.getResult() == org.bukkit.event.player.PlayerLoginEvent.Result.KICK_BANNED) {
            event.allow();
            debugLog("Player '" + name + "' was kicked as banned, but no ban found for any known identity. Allowing login.");
        }

        if (Bukkit.hasWhitelist()) {
            if (!isAnyIdentityWhitelisted(name, currentUuid)) {
                if (event.getResult() != org.bukkit.event.player.PlayerLoginEvent.Result.KICK_WHITELIST) {
                    event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_WHITELIST, "You are not whitelisted on this server!");
                }
            } else if (event.getResult() == org.bukkit.event.player.PlayerLoginEvent.Result.KICK_WHITELIST) {
                event.allow();
                debugLog("Allowed whitelisted player '" + name + "' (matched by alternate identity).");
            }
        }
    }

    private String getAnyIdentityBanMessage(String name, UUID currentUuid) {
        org.bukkit.ban.ProfileBanList banList = Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE);
        String msg;

        if ((msg = checkBan(banList, currentUuid, name)) != null) {
            debugLog("Player '" + name + "' is banned by current UUID " + currentUuid);
            return msg;
        }

        UUID offlineUuid = MojangApiClient.computeOfflineUUID(name);
        if (!offlineUuid.equals(currentUuid) && (msg = checkBan(banList, offlineUuid, name)) != null) {
            debugLog("Player '" + name + "' is banned by offline UUID " + offlineUuid);
            return msg;
        }

        CacheEntry cached = cache.get(name.toLowerCase());
        if (cached != null && cached.premium()) {
            UUID premiumUuid = cached.uuid();
            if (!premiumUuid.equals(currentUuid) && (msg = checkBan(banList, premiumUuid, name)) != null) {
                debugLog("Player '" + name + "' is banned by cached premium UUID " + premiumUuid);
                return msg;
            }
        }

        if ((msg = checkBan(banList, null, name)) != null) {
            debugLog("Player '" + name + "' is banned by name.");
            return msg;
        }

        return null;
    }

    private String checkBan(org.bukkit.ban.ProfileBanList banList, UUID uuid, String name) {
        PlayerProfile profile = Bukkit.createProfile(uuid, name);
        if (!banList.isBanned(profile)) {
            return null;
        }

        try {
            org.bukkit.BanEntry<?> directEntry = banList.getBanEntry(profile);
            if (directEntry != null && directEntry.getReason() != null) {
                return "You are banned from this server.\nReason: " + directEntry.getReason();
            }
        } catch (Exception ignored) {}

        for (org.bukkit.BanEntry<?> entry : banList.getEntries()) {
            String targetStr = entry.getTarget();
            boolean match = false;

            if (name != null && name.equalsIgnoreCase(targetStr)) {
                match = true;
            } else if (uuid != null && uuid.toString().equalsIgnoreCase(targetStr)) {
                match = true;
            } else {
                try {
                    Object obj = entry.getBanTarget();
                    if (obj instanceof org.bukkit.profile.PlayerProfile p) {
                        if (uuid != null && uuid.equals(p.getUniqueId())) match = true;
                        if (!match && name != null && name.equalsIgnoreCase(p.getName())) match = true;
                    } else if (obj instanceof com.destroystokyo.paper.profile.PlayerProfile p2) {
                        if (uuid != null && uuid.equals(p2.getId())) match = true;
                        if (!match && name != null && name.equalsIgnoreCase(p2.getName())) match = true;
                    }
                } catch (Exception ignored) {}
            }

            if (match) {
                String r = entry.getReason();
                return (r != null) ? "You are banned from this server.\nReason: " + r : "You are banned from this server.";
            }
        }

        return "You are banned from this server.";
    }

    private boolean isAnyIdentityWhitelisted(String name, UUID currentUuid) {
        if (Bukkit.getOfflinePlayer(currentUuid).isWhitelisted()) return true;

        UUID offlineUuid = MojangApiClient.computeOfflineUUID(name);
        if (!offlineUuid.equals(currentUuid) && Bukkit.getOfflinePlayer(offlineUuid).isWhitelisted()) return true;

        CacheEntry cached = cache.get(name.toLowerCase());
        if (cached != null && cached.premium()) {
            UUID premiumUuid = cached.uuid();
            if (!premiumUuid.equals(currentUuid) && Bukkit.getOfflinePlayer(premiumUuid).isWhitelisted()) return true;
        }

        return Bukkit.getWhitelistedPlayers().stream().anyMatch(wp -> name.equalsIgnoreCase(wp.getName()));
    }

    private void debugLog(String message) {
        if (config.isDebugLogging()) {
            logger.info("[DEBUG] " + message);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        handleUnbanCommand(event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(org.bukkit.event.server.ServerCommandEvent event) {
        handleUnbanCommand(event.getCommand());
    }

    private void handleUnbanCommand(String commandLine) {
        String cmd = commandLine.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        String[] parts = cmd.split("\\s+");
        if (parts.length >= 2) {
            String label = parts[0].toLowerCase();
            if (label.equals("pardon") || label.equals("unban")) {
                String targetName = parts[1];
                Bukkit.getScheduler().runTask(plugin, () -> pardonAllIdentities(targetName));
            }
        }
    }

    private void pardonAllIdentities(String name) {
        org.bukkit.ban.ProfileBanList banList = Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE);
        
        UUID offlineUuid = MojangApiClient.computeOfflineUUID(name);
        banList.pardon(Bukkit.createProfile(offlineUuid, null));
        
        CacheEntry cached = cache.get(name.toLowerCase());
        if (cached != null && cached.premium()) {
            banList.pardon(Bukkit.createProfile(cached.uuid(), null));
        }

        banList.pardon(Bukkit.createProfile(null, name));
        
        debugLog("Automatically pardoned all known identities for '" + name + "' after command interception.");
    }
}
