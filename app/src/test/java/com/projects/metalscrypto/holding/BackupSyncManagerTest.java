package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class BackupSyncManagerTest {

    @Test
    public void normalizeBackupFileName_addsJsonExtension() {
        assertEquals("portfolio.json", BackupFileNameUtil.normalize("portfolio", "holdings_backup.json"));
    }

    @Test
    public void normalizeBackupFileName_keepsExistingJsonExtension() {
        assertEquals("holdings_backup.json",
                BackupFileNameUtil.normalize("holdings_backup.json", "holdings_backup.json"));
    }

    @Test
    public void normalizeBackupFileName_fallsBackForBlankValues() {
        assertEquals("holdings_backup.json", BackupFileNameUtil.normalize("   ", "holdings_backup.json"));
    }

    @Test
    public void normalizeBackupFileName_rejectsPathTraversal() {
        assertEquals("holdings_backup.json",
                BackupFileNameUtil.normalize("..\\..\\secrets", "holdings_backup.json"));
    }

    @Test
    public void normalizeBackupFileName_rejectsPathSeparators() {
        assertEquals("holdings_backup.json",
                BackupFileNameUtil.normalize("folder/backup", "holdings_backup.json"));
    }

    @Test
    public void identicalItemPayloadsHaveTheSameFingerprint() {
        assertEquals(BackupMergeFingerprint.create("[{\"type\":\"xxx\",\"total\":\"65\"}]"),
                BackupMergeFingerprint.create("[{\"type\":\"xxx\",\"total\":\"65\"}]"));
    }

    @Test
    public void changedItemPayloadHasADifferentFingerprint() {
        assertNotEquals(BackupMergeFingerprint.create("[{\"type\":\"xxx\",\"total\":\"65\"}]"),
                BackupMergeFingerprint.create("[{\"type\":\"xxx\",\"total\":\"80\"}]"));
    }

    @Test
    public void repeatedMergeIsSkippedButReplaceIsNot() {
        String fingerprint = BackupMergeFingerprint.create("items");
        assertTrue(BackupMergeFingerprint.shouldSkipMerge(true, fingerprint,
                Collections.singleton(fingerprint)));
        assertFalse(BackupMergeFingerprint.shouldSkipMerge(false, fingerprint,
                Collections.singleton(fingerprint)));
    }
}
