package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

public class DriveBackupManagerTest {
    @Test
    public void createsDailyAndMonthlySnapshotNames() {
        Calendar date = date(2026, Calendar.OCTOBER, 16);

        assertEquals("Son_2026-10-16.json", DriveBackupManager.sonName(date));
        assertEquals("Grandfather_2026-10.json", DriveBackupManager.grandfatherName(date));
    }

    @Test
    public void weeklySnapshotUsesIsoWeekYearAtYearBoundaries() {
        assertEquals("Father_2025-W01.json",
                DriveBackupManager.fatherName(date(2024, Calendar.DECEMBER, 30)));
        assertEquals("Father_2020-W53.json",
                DriveBackupManager.fatherName(date(2021, Calendar.JANUARY, 1)));
    }

    @Test
    public void onlyRecognizesGenerationSnapshotNames() {
        assertTrue(DriveBackupManager.isSnapshotName("Son_2026-10-16.json"));
        assertTrue(DriveBackupManager.isSnapshotName("Father_2026-W42.json"));
        assertTrue(DriveBackupManager.isSnapshotName("Grandfather_2026-10.json"));
        assertFalse(DriveBackupManager.isSnapshotName("holdings_backup.json"));
        assertFalse(DriveBackupManager.isSnapshotName("Son_2026-13-40.json"));
    }

    @Test
    public void displaysReadableInternalStoragePath() {
        assertEquals("Internal storage/Portfolio/Silver Project",
                DriveBackupManager.formatStorageLocation("primary:Portfolio/Silver Project",
                        "Internal storage", "Storage volume"));
        assertEquals("Internal storage",
                DriveBackupManager.formatStorageLocation("primary:", "Internal storage", "Storage volume"));
    }

    @Test
    public void distinguishesRemovableStorageFromInternalStorage() {
        assertEquals("Storage volume (1234-ABCD)/Backups/Silver Project",
                DriveBackupManager.formatStorageLocation("1234-ABCD:Backups/Silver Project",
                        "Internal storage", "Storage volume"));
    }

    private Calendar date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.clear();
        calendar.set(year, month, day, 12, 0, 0);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }
}