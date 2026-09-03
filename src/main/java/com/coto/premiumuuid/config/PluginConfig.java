package com.coto.premiumuuid.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and exposes all values from config.yml.
 * Supports live reload via {@link #reload()}.
 */
public final class PluginConfig {

    private final JavaPlugin plugin;

    private boolean enabled;
    private int timeoutMs;
    private String cacheFile;
    private long cacheTtlMinutes;
    private boolean fallbackLogWarning;
    private boolean debugLogging;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    /** Reloads config.yml from disk. */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();

        this.enabled = cfg.getBoolean("premium-uuid-enabled", true);
        this.timeoutMs = cfg.getInt("mojang-api.timeout-ms", 3000);
        this.cacheFile = cfg.getString("cache.file", "uuid-cache.yml");
        this.cacheTtlMinutes = cfg.getLong("cache.ttl-minutes", 1440);
        this.fallbackLogWarning = cfg.getBoolean("fallback.log-warning", true);

        String level = cfg.getString("logging.level", "INFO");
        this.debugLogging = "DEBUG".equalsIgnoreCase(level);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public String getCacheFile() {
        return cacheFile;
    }

    public long getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public boolean isFallbackLogWarning() {
        return fallbackLogWarning;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }
}
