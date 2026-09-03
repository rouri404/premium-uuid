package com.coto.premiumuuid.listener;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.cache.UUIDCache.CacheEntry;
import com.coto.premiumuuid.config.PluginConfig;
import com.coto.premiumuuid.mojang.MojangApiClient;
import com.coto.premiumuuid.mojang.MojangApiClient.Failure;
import com.coto.premiumuuid.mojang.MojangApiClient.Success;

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
    private final Logger logger;

    public PreLoginListener(PluginConfig config, UUIDCache cache, MojangApiClient mojangApi, Logger logger) {
        this.config = config;
        this.cache = cache;
        this.mojangApi = mojangApi;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.isEnabled()) return;

        String username = event.getName();
        String key = username.toLowerCase();

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
}

