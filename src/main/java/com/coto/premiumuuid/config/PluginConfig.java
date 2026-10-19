package com.coto.premiumuuid.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Config values are read by async login threads while reloads happen on the main thread,
 * so they are published as one immutable snapshot through a volatile field.
 */
public final class PluginConfig {

    private record Values(boolean enabled, int timeoutMs, String cacheFile, long cacheTtlMinutes,
                          boolean fallbackLogWarning, boolean debugLogging) {}

    private final JavaPlugin plugin;
    private final String cacheFile; // only read at startup; changing it requires a restart

    private volatile Values values;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.values = read();
        this.cacheFile = values.cacheFile();
    }

    /**
     * @return true if {@code cache.file} changed, which only takes effect after a restart
     */
    public boolean reload() {
        plugin.reloadConfig();
        Values fresh = read();
        values = fresh;
        return !fresh.cacheFile().equals(cacheFile);
    }

    private Values read() {
        FileConfiguration cfg = plugin.getConfig();
        String level = cfg.getString("logging.level", "INFO");
        return new Values(
                cfg.getBoolean("premium-uuid-enabled", true),
                cfg.getInt("mojang-api.timeout-ms", 3000),
                cfg.getString("cache.file", "uuid-cache.yml"),
                cfg.getLong("cache.ttl-minutes", 1440),
                cfg.getBoolean("fallback.log-warning", true),
                "DEBUG".equalsIgnoreCase(level));
    }

    public boolean isEnabled() {
        return values.enabled();
    }

    public int getTimeoutMs() {
        return values.timeoutMs();
    }

    public String getCacheFile() {
        return cacheFile;
    }

    public long getCacheTtlMinutes() {
        return values.cacheTtlMinutes();
    }

    public boolean isFallbackLogWarning() {
        return values.fallbackLogWarning();
    }

    public boolean isDebugLogging() {
        return values.debugLogging();
    }
}
