package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;

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
}
