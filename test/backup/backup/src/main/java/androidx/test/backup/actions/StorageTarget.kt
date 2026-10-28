/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import android.content.Context
import androidx.test.backup.BackupActionInputKeys.DB_NAME
import androidx.test.backup.BackupActionInputKeys.IS_BINARY
import androidx.test.backup.BackupActionInputKeys.IS_DEVICE_PROTECTED
import androidx.test.backup.BackupActionInputKeys.PATH
import androidx.test.backup.BackupActionInputKeys.PREF_KEY
import androidx.test.backup.BackupActionInputKeys.PREF_NAME
import androidx.test.backup.BackupActionInputKeys.STORAGE_TYPE
import androidx.test.backup.BackupActionInputKeys.TABLE
import androidx.test.backup.BackupActionValues.DEFAULT_PREF_NAME
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_DATABASE
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_DATASTORE_PREFS
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_FILES
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_PREFS
import androidx.test.backup.BackupDeviceActionArgs
import java.io.File

/**
 * The storage that the arguments of a storage action address.
 *
 * Both actions in this package resolve their arguments with [storageTargetOf], so that the populate
 * and the verify phase always address the same storage: the same context, preference file, database
 * or file.
 *
 * @property context the context that holds the storage: a device-protected one when
 *   [IS_DEVICE_PROTECTED] is set, and the context of the action otherwise
 */
internal sealed class StorageTarget(val context: Context) {

    /** The preference [key] in the `SharedPreferences` file [prefName]. */
    class Preference(context: Context, val prefName: String, val key: String) :
        StorageTarget(context)

    /** The preference [key] in the Preferences DataStore [name]. */
    class PreferencesDataStore(context: Context, val name: String, val key: String) :
        StorageTarget(context) {
        /**
         * The DataStore file, `datastore/<name>.preferences_pb` in the files directory of
         * [context]: where `preferencesDataStore(name)` keeps it, or, for a device-protected
         * [context], where `deviceProtectedDataStoreFile("<name>.preferences_pb")` does.
         */
        val file: File
            get() = File(context.filesDir, "datastore/$name.preferences_pb")
    }

    /** The table [table] of the SQLite database [dbName]. */
    class Database(context: Context, val dbName: String, val table: String) : StorageTarget(context)

    /** The file [file], whose content is Base64-encoded binary data when [isBinary] is set. */
    class FileContent(context: Context, val file: File, val isBinary: Boolean) :
        StorageTarget(context)
}

/** Thrown when the arguments of a storage action don't name a storage that it can act on. */
internal class StorageArgumentException(message: String) : IllegalArgumentException(message)

/**
 * Returns the storage in [context] that [args] address.
 *
 * A relative [PATH] is resolved against the files directory of the storage context.
 *
 * @param operation what the action does, as failure messages name it, such as `"populate"`
 * @throws StorageArgumentException if [args] name an unsupported storage type, or lack an argument
 *   that the storage type requires
 */
internal fun storageTargetOf(
    context: Context,
    args: BackupDeviceActionArgs,
    operation: String,
): StorageTarget {
    val storageContext =
        if (args[IS_DEVICE_PROTECTED].toBoolean()) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    val storageType = args[STORAGE_TYPE]?.uppercase() ?: STORAGE_TYPE_PREFS
    fun required(key: String): String =
        args[key]
            ?: throw StorageArgumentException(
                "Missing '$key' argument for $storageType $operation."
            )
    return when (storageType) {
        STORAGE_TYPE_PREFS ->
            StorageTarget.Preference(
                storageContext,
                prefName = args[PREF_NAME] ?: DEFAULT_PREF_NAME,
                key = required(PREF_KEY),
            )
        STORAGE_TYPE_DATASTORE_PREFS ->
            StorageTarget.PreferencesDataStore(
                storageContext,
                name = required(PREF_NAME),
                key = required(PREF_KEY),
            )
        STORAGE_TYPE_DATABASE ->
            StorageTarget.Database(
                storageContext,
                dbName = required(DB_NAME),
                table = required(TABLE),
            )
        STORAGE_TYPE_FILES -> {
            val path = File(required(PATH))
            StorageTarget.FileContent(
                storageContext,
                file = if (path.isAbsolute) path else File(storageContext.filesDir, path.path),
                isBinary = args[IS_BINARY].toBoolean(),
            )
        }
        else -> throw StorageArgumentException("Unsupported $STORAGE_TYPE: ${args[STORAGE_TYPE]}")
    }
}
