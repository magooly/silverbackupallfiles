package com.projects.metalscrypto.holding.DBManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionManagerTest {
    @Test
    public void shouldRecordVersionWhenHistoryIsEmpty() {
        assertTrue(VersionManager.shouldRecordVersion("1.0.0", null));
        assertTrue(VersionManager.shouldRecordVersion("1.0.0", ""));
    }

    @Test
    public void shouldNotRecordDuplicateVersion() {
        assertFalse(VersionManager.shouldRecordVersion("1.0.0", "1.0.0"));
    }

    @Test
    public void shouldRecordNewVersion() {
        assertTrue(VersionManager.shouldRecordVersion("1.1.0", "1.0.0"));
    }
}
