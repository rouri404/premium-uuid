package com.coto.premiumuuid.override;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe persistence for per-player UUID overrides.
 */
public final class OverrideStore {

    private static final String SECTION = "overrides";

    private final File file;
    private final Logger logger;
    private final ConcurrentHashMap<String, Boolean> overrides = new ConcurrentHashMap<>();
    private final Executor ioExecutor;

    public OverrideStore(File dataFolder, Logger logger) {
        this(dataFolder, logger, Runnable::run);
    }

    /**
     * @param ioExecutor runs the disk writes triggered by {@link #set} and {@link #remove}, so callers
     *                   on the main thread do not block on I/O
     */
    public OverrideStore(File dataFolder, Logger logger, Executor ioExecutor) {
        this.file = new File(dataFolder, "overrides.yml");
        this.logger = logger;
        this.ioExecutor = ioExecutor;
        load();
    }

    public Boolean get(String nickLower) {
        return overrides.get(nickLower);
    }

    public void set(String nickLower, boolean active) {
        overrides.put(nickLower, active);
        ioExecutor.execute(this::save);
    }

    public boolean remove(String nickLower) {
        Boolean removed = overrides.remove(nickLower);
        if (removed != null) {
            ioExecutor.execute(this::save);
            return true;
        }
        return false;
    }

    public Set<Map.Entry<String, Boolean>> entrySet() {
        return overrides.entrySet();
    }

    public void load() {
        overrides.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection(SECTION);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            overrides.put(key, section.getBoolean(key));
        }

        logger.info("Loaded " + overrides.size() + " override entries from " + file.getName());
    }

    // Snapshots the map under the lock, so whichever write runs last always persists the latest state
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : overrides.entrySet()) {
            yaml.set(SECTION + "." + entry.getKey(), entry.getValue());
        }
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save overrides to " + file.getName(), e);
        }
    }
}
