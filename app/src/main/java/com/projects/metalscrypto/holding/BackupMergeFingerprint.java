package com.projects.metalscrypto.holding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

final class BackupMergeFingerprint {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private BackupMergeFingerprint() {
    }

    static String create(String serializedItems) {
        try {
            byte[] payload = (serializedItems == null ? "[]" : serializedItems).getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            char[] fingerprint = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i] & 0xff;
                fingerprint[i * 2] = HEX[value >>> 4];
                fingerprint[i * 2 + 1] = HEX[value & 0x0f];
            }
            return new String(fingerprint);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    static boolean shouldSkipMerge(boolean mergeMode, String fingerprint, Set<String> previouslyMerged) {
        return mergeMode && fingerprint != null && previouslyMerged != null && previouslyMerged.contains(fingerprint);
    }
}