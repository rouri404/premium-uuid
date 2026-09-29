package com.coto.premiumuuid.override;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class OverrideStoreTest {

    private OverrideStore store;

    @BeforeEach
    void setUp(@TempDir File tempDir) {
        // Use a mock logger for the test
        Logger logger = Logger.getLogger("TestLogger");
        store = new OverrideStore(tempDir, logger);
    }

    @Test
    void testSetAndGet() {
        assertNull(store.get("steve"));

        store.set("steve", true);
        assertEquals(true, store.get("steve"));

        store.set("alex", false);
        assertEquals(false, store.get("alex"));
    }

    @Test
    void testResetWorksAndRemovesOverride() {
        // 1. Initial state
        assertNull(store.get("notch"), "Initial state should be unset (null)");

        // 2. Set an override
        store.set("notch", true);
        assertEquals(true, store.get("notch"), "Override should be active (true)");

        // 3. Reset the override
        boolean removed = store.remove("notch");
        
        // 4. Verification
        assertTrue(removed, "remove() should return true when an item was actually removed");
        assertNull(store.get("notch"), "After reset, the override should be unset (null) again");
        
        // 5. Try removing again to verify it handles non-existent elements correctly
        boolean removedAgain = store.remove("notch");
        assertFalse(removedAgain, "remove() should return false when the item does not exist");
    }
}
