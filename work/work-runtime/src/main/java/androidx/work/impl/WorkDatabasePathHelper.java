/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work.impl;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of {@link WorkDatabase} paths.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkDatabasePathHelper {
    private WorkDatabasePathHelper() {
    }

    private static final String TAG = Logger.tagWithPrefix("WrkDbPathHelper");

    private static final String WORK_DATABASE_NAME = "androidx.work.workdb";

    // Supporting files for a SQLite database
    private static final String[] DATABASE_EXTRA_FILES = new String[]{"-journal", "-shm", "-wal"};

    /**
     * @return The name of the database.
     */
    @NonNull
    public static String getWorkDatabaseName() {
        return WORK_DATABASE_NAME;
    }

    /**
     * Migrates {@link WorkDatabase} to the no-backup directory.
     *
     * @param context The application context.
     */
    public static void migrateDatabase(@NonNull Context context) {
        File defaultDatabasePath = getDefaultDatabasePath(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && defaultDatabasePath.exists()) {
            Logger.get().debug(TAG, "Migrating WorkDatabase to the no-backup directory");
            Map<File, File> paths = migrationPaths(context);
            for (File source : paths.keySet()) {
                File destination = paths.get(source);
                if (source.exists() && destination != null) {
                    if (destination.exists()) {
                        String message = "Over-writing contents of " + destination;
                        Logger.get().warning(TAG, message);
                    }
                    boolean renamed = source.renameTo(destination);
                    String message;
                    if (renamed) {
                        message = "Migrated " + source + "to " + destination;
                    } else {
                        message = "Renaming " + source + " to " + destination + " failed";
                    }
                    Logger.get().debug(TAG, message);
                }
            }
        }
    }

    /**
     * Returns a {@link Map} of all paths which need to be migrated to the no-backup directory.
     *
     * @param context The application {@link Context}
     * @return a {@link Map} of paths to be migrated from source -> destination
     */
    @NonNull
    @VisibleForTesting
    public static Map<File, File> migrationPaths(@NonNull Context context) {
        Map<File, File> paths = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            File databasePath = getDefaultDatabasePath(context);
            File migratedPath = getDatabasePath(context);
            paths.put(databasePath, migratedPath);
            for (String extra : DATABASE_EXTRA_FILES) {
                File source = new File(databasePath.getPath() + extra);
                File destination = new File(migratedPath.getPath() + extra);
                paths.put(source, destination);
            }
        }
        return paths;
    }

    /**
     * @param context The application {@link Context}
     * @return The database path before migration to the no-backup directory.
     */
    @NonNull
    @VisibleForTesting
    public static File getDefaultDatabasePath(@NonNull Context context) {
        return context.getDatabasePath(WORK_DATABASE_NAME);
    }

    /**
     * @param context The application {@link Context}
     * @return The the migrated database path.
     */
    @NonNull
    @VisibleForTesting
    public static File getDatabasePath(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // No notion of a backup directory exists.
            return getDefaultDatabasePath(context);
        } else {
            return getNoBackupPath(context, WORK_DATABASE_NAME);
        }
    }

    /**
     * Return the path for a {@link File} path in the {@link Context#getNoBackupFilesDir()}
     * identified by the {@link String} fragment.
     *
     * @param context  The application {@link Context}
     * @param filePath The {@link String} file path
     * @return the {@link File}
     */
    @RequiresApi(23)
    private static File getNoBackupPath(@NonNull Context context, @NonNull String filePath) {
        return new File(context.getNoBackupFilesDir(), filePath);
    }
}
