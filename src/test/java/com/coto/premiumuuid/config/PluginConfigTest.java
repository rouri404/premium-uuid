package com.coto.premiumuuid.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginConfigTest {

    private static YamlConfiguration yaml(boolean enabled, int timeout, String cacheFile) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("premium-uuid-enabled", enabled);
        cfg.set("mojang-api.timeout-ms", timeout);
        cfg.set("cache.file", cacheFile);
        return cfg;
    }

    @Test
    void reload_AppliesValues_ButKeepsStartupCacheFile() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(yaml(true, 3000, "uuid-cache.yml"), yaml(false, 500, "other.yml"));

        PluginConfig config = new PluginConfig(plugin);
        assertTrue(config.isEnabled());
        assertEquals(3000, config.getTimeoutMs());

        assertTrue(config.reload(), "Changed cache.file is reported");
        assertFalse(config.isEnabled());
        assertEquals(500, config.getTimeoutMs());
        assertEquals("uuid-cache.yml", config.getCacheFile(), "cache.file only applies after restart");
    }

    @Test
    void reload_UnchangedCacheFile_NotReported() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(yaml(true, 3000, "uuid-cache.yml"));

        PluginConfig config = new PluginConfig(plugin);

        assertFalse(config.reload());
    }
}
