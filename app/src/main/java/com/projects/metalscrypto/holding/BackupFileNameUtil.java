package com.projects.metalscrypto.holding;

import java.util.Locale;
import java.util.regex.Pattern;

final class BackupFileNameUtil {
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private BackupFileNameUtil() {
    }

    static String normalize(String backupFileName, String defaultName) {
        String trimmed = backupFileName == null ? "" : backupFileName.trim();
        if (trimmed.isEmpty()) {
            return defaultName;
        }
        if (!isSafeFileName(trimmed)) {
            return defaultName;
        }
        if (!trimmed.toLowerCase(Locale.US).endsWith(".json")) {
            trimmed = trimmed + ".json";
        }
        return trimmed;
    }

    static boolean isSafeFileName(String fileName) {
        if (fileName == null) {
            return false;
        }
        String trimmed = fileName.trim();
        if (trimmed.isEmpty() || trimmed.contains("..")) {
            return false;
        }
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains(":")) {
            return false;
        }
        return SAFE_FILE_NAME.matcher(trimmed).matches();
    }
}
