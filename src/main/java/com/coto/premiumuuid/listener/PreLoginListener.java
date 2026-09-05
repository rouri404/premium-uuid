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
 * Intercepts pre-login to resolve the player's UUID against the Mojang API
 * (cache-first) before any other plugin sees the event.
 */
public final class PreLoginListener implements Listener {

    private final PluginConfig config;
    private final UUIDCache cache;
    private final MojangApiClient mojangApi;
    private final OverrideStore overrides;
    private final Logger logger;

    public PreLoginListener(PluginConfig config, UUIDCache cache, MojangApiClient mojangApi,
                            OverrideStore overrides, Logger logger) {
        this.config = config;
        this.cache = cache;
        this.mojangApi = mojangApi;
        this.overrides = overrides;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String key = username.toLowerCase();

        // 0) Precedência de override individual
        Boolean override = overrides.get(key);
        if (override != null) {
            if (!override) {
                // inactive — força UUID offline, sem consultar cache nem API
                if (config.isDebugLogging()) {
                    UUID offlineUuid = MojangApiClient.computeOfflineUUID(username);
                    logger.info("[DEBUG] Player '" + username + "' tem override INACTIVE → offline UUID " + offlineUuid);
                }
                return; // não faz nada: o servidor usa UUID offline por padrão
            }
            // active — cai para o fluxo premium (cache → API → fallback) abaixo
            if (config.isDebugLogging()) {
                logger.info("[DEBUG] Player '" + username + "' tem override ACTIVE → checagem premium forçada.");
            }
        } else {
            // Sem override — segue o comportamento global
            if (!config.isEnabled()) return;
        }

        // 1) Check cache
        CacheEntry cached = cache.get(key);
        if (cached != null && cache.isValid(cached, config.getCacheTtlMinutes())) {
            applyFromCache(event, cached, username, key, "valid cache");
            return;
        }

        // 2) Cache miss or expired → call Mojang API
        MojangApiClient.LookupResult result = mojangApi.lookup(username, config.getTimeoutMs(), config.isDebugLogging());

        if (result instanceof Success success) {
            handleSuccess(event, success, key);
        } else if (result instanceof Failure failure) {
            handleFailure(event, failure, cached, username, key);
        }
    }

    // ── Handlers ────────────────────────────────────────────────────────

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
        // API failed — use stale cache if available
        if (cached != null) {
            applyFromCache(event, cached, username, key, "expired cache (API failure: " + failure.reason() + ")");
        } else {
            // No cache at all — fall back to offline UUID
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

    /**
     * Sets the UUID on the login event using Paper's PlayerProfile API.
     */
    private void setEventUUID(AsyncPlayerPreLoginEvent event, UUID uuid) {
        PlayerProfile profile = Bukkit.createProfile(uuid, event.getName());
        event.setPlayerProfile(profile);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(org.bukkit.event.player.PlayerLoginEvent event) {
        if (event.getResult() != org.bukkit.event.player.PlayerLoginEvent.Result.KICK_WHITELIST) {
            return;
        }

        String name = event.getPlayer().getName();
        String key = name.toLowerCase();

        // The plugin may have swapped this player's UUID in a previous or current session,
        // causing a mismatch between the UUID the server is using and the one stored in whitelist.json.
        // We check both the offline UUID and the cached premium UUID against the whitelist.

        // 1) Check offline UUID
        UUID offlineUuid = MojangApiClient.computeOfflineUUID(name);
        if (Bukkit.getOfflinePlayer(offlineUuid).isWhitelisted()) {
            event.allow();
            if (config.isDebugLogging()) {
                logger.info("[DEBUG] Allowed whitelisted player '" + name + "' (matched by offline UUID).");
            }
            return;
        }

        // 2) Check premium UUID from cache
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.premium()) {
            if (Bukkit.getOfflinePlayer(cached.uuid()).isWhitelisted()) {
                event.allow();
                if (config.isDebugLogging()) {
                    logger.info("[DEBUG] Allowed whitelisted player '" + name + "' (matched by cached premium UUID).");
                }
                return;
            }
        }

        // 3) Fallback: check by name
        for (org.bukkit.OfflinePlayer wp : Bukkit.getWhitelistedPlayers()) {
            String wpName = wp.getName();
            if (wpName != null && wpName.equalsIgnoreCase(name)) {
                event.allow();
                if (config.isDebugLogging()) {
                    logger.info("[DEBUG] Allowed whitelisted player '" + name + "' (matched by name).");
                }
                return;
            }
        }
    }
}
