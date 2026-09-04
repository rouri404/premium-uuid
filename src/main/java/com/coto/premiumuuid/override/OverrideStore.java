package com.coto.premiumuuid.override;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe, disk-persistent store for per-nickname premium-check overrides.
 * <p>
 * Backed by {@code <dataFolder>/overrides.yml} with the structure:
 * <pre>
 * overrides:
 *   nick-lowercase: true|false
 * </pre>
 * {@code true}  = "active"   — force premium check even when global flag is off.<br>
 * {@code false} = "inactive" — skip premium check even when global flag is on.<br>
 * Absence of entry = follow global {@code premium-uuid-enabled}.
 */
public final class OverrideStore {

    private static final String SECTION = "overrides";

    private final File file;
    private final Logger logger;

    /** nick (lowercase) -&gt; true (active) | false (inactive) */
    private final ConcurrentHashMap<String, Boolean> overrides = new ConcurrentHashMap<>();

    public OverrideStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "overrides.yml");
        this.logger = logger;
        load();
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Returns the override value for the given nick (must be lowercase),
     * or {@code null} if no override is set.
     *
     * @param nickLower lowercase nickname
     * @return {@link Boolean#TRUE} (active), {@link Boolean#FALSE} (inactive), or {@code null}
     */
    public Boolean get(String nickLower) {
        return overrides.get(nickLower);
    }

    /**
     * Sets the override for the given nick and immediately persists to disk.
     *
     * @param nickLower lowercase nickname
     * @param active    {@code true} = force premium on; {@code false} = force premium off
     */
    public void set(String nickLower, boolean active) {
        overrides.put(nickLower, active);
        save();
    }

    /** Returns an unmodifiable snapshot of all entries (for iteration). */
    public Set<Map.Entry<String, Boolean>> entrySet() {
        return overrides.entrySet();
    }

    // ── Persistence ──────────────────────────────────────────────────────

    /** Loads overrides from disk. Safe to call multiple times. */
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

    /** Saves overrides to disk. Must be called from a safe context (main thread or async). */
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
