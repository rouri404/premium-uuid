package com.coto.premiumuuid.cache;

import com.coto.premiumuuid.cache.UUIDCache.CacheEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UUIDCacheTest {

    private final Logger logger = Logger.getLogger("UUIDCacheTest");

    @Test
    void saveIfDirty_OnlyWritesAfterChanges(@TempDir File dir) {
        UUIDCache cache = new UUIDCache(dir, "cache.yml", logger);
        File file = new File(dir, "cache.yml");

        assertFalse(cache.saveIfDirty(), "Nothing changed yet");
        assertFalse(file.exists());

        cache.put("notch", new CacheEntry(UUID.randomUUID(), true, System.currentTimeMillis()));
        assertTrue(cache.saveIfDirty());
        assertTrue(file.exists());

        assertFalse(cache.saveIfDirty(), "Second save without changes is skipped");

        assertFalse(cache.remove("missing"));
        assertFalse(cache.isDirty(), "Removing a missing key is not a change");

        assertTrue(cache.remove("notch"));
        assertTrue(cache.saveIfDirty());
    }

    @Test
    void savedEntries_ReloadIntoNewInstance(@TempDir File dir) {
        UUID uuid = UUID.randomUUID();
        UUIDCache cache = new UUIDCache(dir, "cache.yml", logger);
        cache.put("notch", new CacheEntry(uuid, true, 123L));
        cache.saveIfDirty();

        UUIDCache reloaded = new UUIDCache(dir, "cache.yml", logger);

        assertEquals(new CacheEntry(uuid, true, 123L), reloaded.get("notch"));
        assertFalse(reloaded.isDirty());
    }
}
