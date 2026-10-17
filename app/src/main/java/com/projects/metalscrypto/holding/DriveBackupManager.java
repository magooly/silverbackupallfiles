package com.projects.metalscrypto.holding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class DriveBackupManager {
    private static final String TAG = "DriveBackupManager";
    private static final String PREFS_NAME = "backup_sync";
    private static final String PREF_DRIVE_TREE_URI = "drive_backup_tree_uri";
    private static final String PROJECT_FOLDER_NAME = "Silver Project";
    private static final Pattern SON_NAME = Pattern.compile("Son_\\d{4}-\\d{2}-\\d{2}\\.json");
    private static final Pattern FATHER_NAME = Pattern.compile("Father_\\d{4}-W\\d{2}\\.json");
    private static final Pattern GRANDFATHER_NAME = Pattern.compile("Grandfather_\\d{4}-\\d{2}\\.json");
    private static final int SON_RETENTION = 7;
    private static final int FATHER_RETENTION = 4;
    private static final int GRANDFATHER_RETENTION = 12;

    private DriveBackupManager() {
    }

    static Intent createFolderPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }

    static void saveTreeUri(Context context, Uri treeUri, int grantedFlags) {
        int persistableFlags = grantedFlags
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(treeUri, persistableFlags);
        preferences(context).edit().putString(PREF_DRIVE_TREE_URI, treeUri.toString()).apply();
    }

    static boolean hasSavedTreeUri(Context context) {
        return preferences(context).contains(PREF_DRIVE_TREE_URI);
    }

    static String getFolderLocation(Context context) {
        String savedUri = preferences(context).getString(PREF_DRIVE_TREE_URI, "");
        if (savedUri.isEmpty()) {
            return "";
        }
        try {
            DocumentFile folder = getProjectFolder(context, false);
            if (folder != null) {
                Uri folderUri = folder.getUri();
                if ("com.android.externalstorage.documents".equals(folderUri.getAuthority())) {
                    return formatStorageLocation(DocumentsContract.getDocumentId(folderUri),
                            context.getString(R.string.backup_internal_storage),
                            context.getString(R.string.backup_storage_volume));
                }
                String name = folder.getName();
                return name == null ? PROJECT_FOLDER_NAME : name;
            }
        } catch (Exception exception) {
            Log.w(TAG, "Could not resolve backup folder location", exception);
        }
        Uri treeUri = Uri.parse(savedUri);
        if ("com.android.externalstorage.documents".equals(treeUri.getAuthority())) {
            String documentId = DocumentsContract.getTreeDocumentId(treeUri);
            String location = formatStorageLocation(documentId,
                    context.getString(R.string.backup_internal_storage),
                    context.getString(R.string.backup_storage_volume));
            return location.endsWith("/" + PROJECT_FOLDER_NAME) ? location : location + "/" + PROJECT_FOLDER_NAME;
        }
        return PROJECT_FOLDER_NAME;
    }

    static String formatStorageLocation(String documentId, String internalStorage, String storageVolume) {
        int separator = documentId.indexOf(':');
        String volume = separator < 0 ? documentId : documentId.substring(0, separator);
        String path = separator < 0 ? "" : documentId.substring(separator + 1);
        String root = "primary".equalsIgnoreCase(volume) ? internalStorage : storageVolume + " (" + volume + ")";
        return path.isEmpty() ? root : root + "/" + path;
    }

    static String prepareProjectFolder(Context context) {
        try {
            DocumentFile folder = getProjectFolder(context, true);
            return folder == null ? "Could not open or create the Silver Project folder." : null;
        } catch (Exception exception) {
            Log.w(TAG, "Could not prepare Drive backup folder", exception);
            return errorMessage(exception);
        }
    }

    static String backupSnapshotIfConfigured(Context context, String json) {
        if (!hasSavedTreeUri(context)) {
            return null;
        }
        try {
            DocumentFile folder = getProjectFolder(context, true);
            if (folder == null) {
                return "Could not open the Silver Project folder.";
            }

            Calendar now = Calendar.getInstance();
            writeSnapshot(context, folder, sonName(now), json);
            writeSnapshot(context, folder, fatherName(now), json);
            writeSnapshot(context, folder, grandfatherName(now), json);
            prune(folder, "Son_", SON_NAME, SON_RETENTION);
            prune(folder, "Father_", FATHER_NAME, FATHER_RETENTION);
            prune(folder, "Grandfather_", GRANDFATHER_NAME, GRANDFATHER_RETENTION);
            return "";
        } catch (Exception exception) {
            Log.w(TAG, "Drive GFS backup failed", exception);
            return errorMessage(exception);
        }
    }

    static List<String> listSnapshots(Context context) {
        List<String> names = new ArrayList<>();
        try {
            DocumentFile folder = getProjectFolder(context, false);
            if (folder == null) {
                return names;
            }
            for (DocumentFile file : folder.listFiles()) {
                String name = file.getName();
                if (file.isFile() && isSnapshotName(name)) {
                    names.add(name);
                }
            }
            Collections.sort(names, String::compareToIgnoreCase);
        } catch (Exception exception) {
            Log.w(TAG, "Could not list Drive backups", exception);
        }
        return names;
    }

    static boolean isSnapshotName(String name) {
        if (name == null) {
            return false;
        }
        if (SON_NAME.matcher(name).matches()) {
            return isValidDate(name.substring(4, 14), "yyyy-MM-dd");
        }
        if (FATHER_NAME.matcher(name).matches()) {
            int week = Integer.parseInt(name.substring(13, 15));
            return week >= 1 && week <= 53;
        }
        return GRANDFATHER_NAME.matcher(name).matches()
                && isValidDate(name.substring(12, 19), "yyyy-MM");
    }

    static boolean hasSnapshot(Context context, String name) {
        try {
            DocumentFile folder = getProjectFolder(context, false);
            DocumentFile file = folder == null ? null : folder.findFile(name);
            return file != null && file.isFile() && isSnapshotName(name);
        } catch (Exception exception) {
            Log.w(TAG, "Could not inspect Drive backup " + name, exception);
            return false;
        }
    }

    static String readSnapshot(Context context, String name) throws Exception {
        if (!isSnapshotName(name)) {
            throw new IllegalArgumentException("Invalid Drive backup name.");
        }
        DocumentFile folder = getProjectFolder(context, false);
        DocumentFile file = folder == null ? null : folder.findFile(name);
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Drive backup not found.");
        }
        try (InputStream input = context.getContentResolver().openInputStream(file.getUri());
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IllegalStateException("Could not read Drive backup.");
            }
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static DocumentFile getProjectFolder(Context context, boolean create) {
        String savedUri = preferences(context).getString(PREF_DRIVE_TREE_URI, null);
        if (savedUri == null || savedUri.trim().isEmpty()) {
            return null;
        }
        DocumentFile selected = DocumentFile.fromTreeUri(context, Uri.parse(savedUri));
        if (selected == null || !selected.isDirectory()) {
            return null;
        }
        if (PROJECT_FOLDER_NAME.equalsIgnoreCase(selected.getName())) {
            return selected;
        }
        DocumentFile projectFolder = selected.findFile(PROJECT_FOLDER_NAME);
        if (projectFolder != null) {
            return projectFolder.isDirectory() ? projectFolder : null;
        }
        return create ? selected.createDirectory(PROJECT_FOLDER_NAME) : null;
    }

    static String sonName(Calendar date) {
        return "Son_" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()) + ".json";
    }

    static String fatherName(Calendar date) {
        Calendar week = (Calendar) date.clone();
        week.setFirstDayOfWeek(Calendar.MONDAY);
        week.setMinimalDaysInFirstWeek(4);
        int weekNumber = week.get(Calendar.WEEK_OF_YEAR);
        int weekYear = week.get(Calendar.YEAR);
        if (week.get(Calendar.MONTH) == Calendar.DECEMBER && weekNumber == 1) {
            weekYear++;
        } else if (week.get(Calendar.MONTH) == Calendar.JANUARY && weekNumber >= 52) {
            weekYear--;
        }
        return String.format(Locale.US, "Father_%04d-W%02d.json",
                weekYear, weekNumber);
    }

    static String grandfatherName(Calendar date) {
        return "Grandfather_" + new SimpleDateFormat("yyyy-MM", Locale.US).format(date.getTime()) + ".json";
    }

    private static void writeSnapshot(Context context, DocumentFile folder, String name, String json)
            throws Exception {
        DocumentFile file = folder.findFile(name);
        if (file == null) {
            file = folder.createFile("application/json", name);
        }
        if (file == null || !file.isFile()) {
            throw new IllegalStateException("Could not create " + name);
        }
        try (OutputStream output = context.getContentResolver().openOutputStream(file.getUri(), "wt")) {
            if (output == null) {
                throw new IllegalStateException("Could not write " + name);
            }
            output.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void prune(DocumentFile folder, String prefix, Pattern namePattern, int retention)
            throws Exception {
        List<DocumentFile> files = new ArrayList<>();
        for (DocumentFile file : folder.listFiles()) {
            String name = file.getName();
            if (file.isFile() && name != null && name.startsWith(prefix) && namePattern.matcher(name).matches()) {
                files.add(file);
            }
        }
        Collections.sort(files, new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile left, DocumentFile right) {
                return right.getName().compareTo(left.getName());
            }
        });
        for (int index = retention; index < files.size(); index++) {
            if (!files.get(index).delete()) {
                throw new IllegalStateException("Could not prune old Drive backup " + files.get(index).getName());
            }
        }
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Drive folder is unavailable." : message;
    }

    private static boolean isValidDate(String value, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
        dateFormat.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return dateFormat.parse(value, position) != null && position.getIndex() == value.length();
    }
}