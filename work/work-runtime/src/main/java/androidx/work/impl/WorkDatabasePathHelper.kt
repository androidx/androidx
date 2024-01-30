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
package androidx.work.impl

import android.content.Context
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.work.Logger
import java.io.File

private val TAG = Logger.tagWithPrefix("WrkDbPathHelper")

/**
 * @return The name of the database.
 */
internal const val WORK_DATABASE_NAME = "androidx.work.workdb"

// Supporting files for a SQLite database
private val DATABASE_EXTRA_FILES = arrayOf("-journal", "-shm", "-wal")

/**
 * Keeps track of {@link WorkDatabase} paths.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object WorkDatabasePathHelper {
    /**
     * Migrates [WorkDatabase] to the no-backup directory.
     *
     * @param context The application context.
     */
    @JvmStatic
    fun migrateDatabase(context: Context) {
        val defaultDatabasePath = getDefaultDatabasePath(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && defaultDatabasePath.exists()) {
            Logger.get().debug(TAG, "Migrating WorkDatabase to the no-backup directory")
            migrationPaths(context).forEach { (source, destination) ->
                if (source.exists()) {
                    if (destination.exists()) {
                        Logger.get().warning(TAG, "Over-writing contents of $destination")
                    }
                    val renamed = source.renameTo(destination)
                    val message = if (renamed) {
                        "Migrated ${source}to $destination"
                    } else {
                        "Renaming $source to $destination failed"
                    }
                    Logger.get().debug(TAG, message)
                }
            }
        }
    }

    /**
     * Returns a [Map] of all paths which need to be migrated to the no-backup directory.
     *
     * @param context The application [Context]
     * @return a [Map] of paths to be migrated from source -> destination
     */
    fun migrationPaths(context: Context): Map<File, File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val databasePath = getDefaultDatabasePath(context)
            val migratedPath = getDatabasePath(context)
            val map = DATABASE_EXTRA_FILES.associate { extra ->
                File(databasePath.path + extra) to File(migratedPath.path + extra)
            }
            map + (databasePath to migratedPath)
        } else emptyMap()
    }

    /**
     * @param context The application [Context]
     * @return The database path before migration to the no-backup directory.
     */
    fun getDefaultDatabasePath(context: Context): File {
        return context.getDatabasePath(WORK_DATABASE_NAME)
    }

    /**
     * @param context The application [Context]
     * @return The the migrated database path.
     */
    fun getDatabasePath(context: Context): File {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // No notion of a backup directory exists.
            getDefaultDatabasePath(context)
        } else {
            getNoBackupPath(context)
        }
    }

    /**
     * Return the path for a [File] path in the [Context.getNoBackupFilesDir]
     * identified by the [String] fragment.
     *
     * @param context  The application [Context]
     * @return the [File]
     */
    @RequiresApi(23)
    private fun getNoBackupPath(context: Context): File {
        return File(Api21Impl.getNoBackupFilesDir(context), WORK_DATABASE_NAME)
    }
}

@RequiresApi(21)
internal object Api21Impl {
    @DoNotInline
    fun getNoBackupFilesDir(context: Context): File {
        return context.noBackupFilesDir
    }
}
