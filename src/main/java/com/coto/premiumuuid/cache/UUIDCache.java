package com.coto.premiumuuid.cache;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe cache accessed concurrently by async logins and main thread commands.
 */
public final class UUIDCache {

    private final File file;
    private final Logger logger;
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public UUIDCache(File dataFolder, String fileName, Logger logger) {
        this.file = new File(dataFolder, fileName);
        this.logger = logger;
        load();
    }

    public record CacheEntry(UUID uuid, boolean premium, long lastChecked) {}

    public CacheEntry get(String usernameLower) {
        return entries.get(usernameLower);
    }

    public void put(String usernameLower, CacheEntry entry) {
        entries.put(usernameLower, entry);
    }

    public boolean remove(String usernameLower) {
        return entries.remove(usernameLower) != null;
    }

    public void clear() {
        entries.clear();
    }

    public Set<Map.Entry<String, CacheEntry>> entrySet() {
        return entries.entrySet();
    }

    public boolean isValid(CacheEntry entry, long ttlMinutes) {
        if (ttlMinutes <= 0) return true; // 0 or negative = never expires
        long ttlMs = ttlMinutes * 60_000L;
        return (System.currentTimeMillis() - entry.lastChecked()) < ttlMs;
    }

    public void load() {
        entries.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cacheSection = yaml.getConfigurationSection("cache");
        if (cacheSection == null) return;

        for (String key : cacheSection.getKeys(false)) {
            ConfigurationSection entry = cacheSection.getConfigurationSection(key);
            if (entry == null) continue;
            try {
                UUID uuid = UUID.fromString(entry.getString("uuid", ""));
                boolean premium = entry.getBoolean("premium");
                long lastChecked = entry.getLong("last-checked", 0L);
                entries.put(key, new CacheEntry(uuid, premium, lastChecked));
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping malformed cache entry for '" + key + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + entries.size() + " cached UUID entries from " + file.getName());
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (var mapEntry : entries.entrySet()) {
            String path = "cache." + mapEntry.getKey();
            CacheEntry ce = mapEntry.getValue();
            yaml.set(path + ".uuid", ce.uuid().toString());
            yaml.set(path + ".premium", ce.premium());
            yaml.set(path + ".last-checked", ce.lastChecked());
        }

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save UUID cache to " + file.getName(), e);
        }
    }
}
