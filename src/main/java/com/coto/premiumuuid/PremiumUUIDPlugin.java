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

import java.util.concurrent.Executor;

public final class PremiumUUIDPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private UUIDCache uuidCache;
    private OverrideStore overrideStore;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);

        Executor ioExecutor = task -> getServer().getScheduler().runTaskAsynchronously(this, task);

        uuidCache = new UUIDCache(getDataFolder(), pluginConfig.getCacheFile(), getLogger());

        overrideStore = new OverrideStore(getDataFolder(), getLogger(), ioExecutor);

        MojangApiClient mojangApi = new MojangApiClient(getLogger());

        getServer().getPluginManager().registerEvents(
                new PreLoginListener(pluginConfig, uuidCache, mojangApi, overrideStore, getLogger(), this), this);

        PremiumUUIDCommand cmdHandler = new PremiumUUIDCommand(pluginConfig, uuidCache, overrideStore, ioExecutor);
        PluginCommand cmd = getCommand("premiumuuid");
        if (cmd != null) {
            cmd.setExecutor(cmdHandler);
            cmd.setTabCompleter(cmdHandler);
        }

        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (uuidCache.saveIfDirty() && pluginConfig.isDebugLogging()) {
                getLogger().info("[DEBUG] Periodic cache save completed.");
            }
        }, 6000L, 6000L); // 5 minutes in ticks

        getLogger().info("PremiumUUID enabled. Premium UUID resolution is "
                + (pluginConfig.isEnabled() ? "ACTIVE" : "DISABLED") + ".");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (uuidCache != null && uuidCache.saveIfDirty()) {
            getLogger().info("UUID cache saved to disk.");
        }
        if (overrideStore != null) {
            overrideStore.save(); // async writes scheduled during shutdown may never run
        }
    }
}
