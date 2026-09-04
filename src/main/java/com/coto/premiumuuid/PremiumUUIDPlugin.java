package com.coto.premiumuuid;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.command.PremiumUUIDCommand;
import com.coto.premiumuuid.config.PluginConfig;
import com.coto.premiumuuid.listener.PreLoginListener;
import com.coto.premiumuuid.mojang.MojangApiClient;
import com.coto.premiumuuid.override.OverrideStore;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * PremiumUUID — resolves premium Mojang UUIDs on offline-mode Paper servers.
 */
public final class PremiumUUIDPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private UUIDCache uuidCache;
    private OverrideStore overrideStore;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        // Config
        pluginConfig = new PluginConfig(this);

        // Cache
        uuidCache = new UUIDCache(getDataFolder(), pluginConfig.getCacheFile(), getLogger());

        // Override store (persistent per-nick decisions)
        overrideStore = new OverrideStore(getDataFolder(), getLogger());

        // Mojang API client
        MojangApiClient mojangApi = new MojangApiClient(getLogger());

        // Register listener
        getServer().getPluginManager().registerEvents(
                new PreLoginListener(pluginConfig, uuidCache, mojangApi, overrideStore, getLogger()), this);

        // Register command
        PremiumUUIDCommand cmdHandler = new PremiumUUIDCommand(pluginConfig, uuidCache, overrideStore);
        PluginCommand cmd = getCommand("premiumuuid");
        if (cmd != null) {
            cmd.setExecutor(cmdHandler);
            cmd.setTabCompleter(cmdHandler);
        }

        // Periodic cache save every 5 minutes (6000 ticks) to minimize data loss on crash
        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            uuidCache.save();
            if (pluginConfig.isDebugLogging()) {
                getLogger().info("[DEBUG] Periodic cache save completed.");
            }
        }, 6000L, 6000L);

        getLogger().info("PremiumUUID enabled. Premium UUID resolution is "
                + (pluginConfig.isEnabled() ? "ACTIVE" : "DISABLED") + ".");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (uuidCache != null) {
            uuidCache.save();
            getLogger().info("UUID cache saved to disk.");
        }
    }
}
