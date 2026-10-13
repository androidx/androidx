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

import android.app.backup.BackupManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.test.backup.BackupActionInputKeys.EXPECT_NULL
import androidx.test.backup.BackupActionInputKeys.KEY_COL
import androidx.test.backup.BackupActionInputKeys.KEY_VAL
import androidx.test.backup.BackupActionInputKeys.VALUE
import androidx.test.backup.BackupActionInputKeys.VALUES
import androidx.test.backup.BackupActionInputKeys.VALUE_TYPE
import androidx.test.backup.BackupActionPhase
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult

/**
 * Populates test data inside the application sandbox before a backup.
 *
 * Reads [androidx.test.backup.BackupActionInputKeys.STORAGE_TYPE] from the incoming arguments and
 * seeds the specified data into [android.content.SharedPreferences], a SQLite database, or raw file
 * storage. See [androidx.test.backup.BackupActionInputKeys] for the keys each storage type
 * consumes.
 *
 * Once the data is written, [BackupManager.dataChanged] is called so the platform knows the app has
 * backup-eligible changes pending.
 */
public class PopulateStorageAction : BackupDeviceAction {
    @get:BackupActionPhase override val phase: Int = BackupDeviceAction.PHASE_POPULATE

    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val target =
            try {
                storageTargetOf(context, args, operation = "populate")
            } catch (e: StorageArgumentException) {
                return failure(e.message.orEmpty())
            }
        return try {
            val result =
                when (target) {
                    is StorageTarget.Preference -> populate(target, args)
                    is StorageTarget.Database -> populate(target, args)
                    is StorageTarget.FileContent -> populate(target, args)
                }
            if (result.isSuccess) {
                // Tells the platform that the app has data to back up.
                BackupManager(target.context).dataChanged()
            }
            result
        } catch (e: Exception) {
            failure("PopulateStorageAction exception: ${e.message}")
        }
    }

    private fun populate(
        target: StorageTarget.Preference,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val editor =
            target.context.getSharedPreferences(target.prefName, Context.MODE_PRIVATE).edit()
        if (args[EXPECT_NULL].toBoolean()) {
            editor.remove(target.key)
        } else {
            val value =
                args[VALUE] ?: return failure("Missing '$VALUE' argument for PREFS populate.")
            val parsedValue =
                try {
                    parsePreferenceValue(value, args[VALUE_TYPE] ?: VALUE_TYPE_STRING)
                } catch (e: IllegalArgumentException) {
                    return failure(e.message ?: "Invalid preference value.")
                }
            when (parsedValue) {
                is Int -> editor.putInt(target.key, parsedValue)
                is Long -> editor.putLong(target.key, parsedValue)
                is Float -> editor.putFloat(target.key, parsedValue)
                is Boolean -> editor.putBoolean(target.key, parsedValue)
                is String -> editor.putString(target.key, parsedValue)
                else -> error("Unexpected parsed preference value: $parsedValue")
            }
        }
        if (!editor.commit()) {
            return failure("Failed to commit SharedPreferences changes for '${target.prefName}'.")
        }
        return BackupDeviceActionResult.success()
    }

    private fun populate(
        target: StorageTarget.Database,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val valuesStr =
            args[VALUES] ?: return failure("Missing '$VALUES' argument for DATABASE populate.")
        val columns =
            try {
                decodeColumnValues(valuesStr)
            } catch (e: IllegalArgumentException) {
                return failure(e.message ?: "Malformed '$VALUES' argument.")
            }
        val table = target.table
        val keyCol = args[KEY_COL]
        val keyVal = args[KEY_VAL]

        target.context.openOrCreateDatabase(target.dbName, Context.MODE_PRIVATE, null).use { db ->
            // Ensure the table exists
            val cv = ContentValues()
            val colDefs = mutableListOf<String>()
            for ((columnName, columnValue) in columns) {
                colDefs.add("`$columnName` TEXT")
                if (columnValue == null) {
                    cv.putNull(columnName)
                } else {
                    cv.put(columnName, columnValue)
                }
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS `$table` (${colDefs.joinToString(", ")})")
            db.beginTransaction()
            try {
                if (keyCol != null && keyVal != null) {
                    db.delete(table, "`$keyCol` = ?", arrayOf(keyVal))
                }
                // SQLite errors (unknown column, constraint violation) propagate out of
                // insertWithOnConflict as SQLException and are turned into a failure by the catch
                // in execute. A -1 return means the statement ran but inserted no row, which
                // CONFLICT_REPLACE should never produce; check it anyway so a future change to the
                // conflict algorithm cannot seed nothing silently.
                val rowId =
                    db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId == -1L) {
                    return failure(
                        "Insert into table '$table' did not add a row " +
                            "(insertWithOnConflict returned -1)."
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        return BackupDeviceActionResult.success()
    }

    private fun populate(
        target: StorageTarget.FileContent,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val value = args[VALUE] ?: return failure("Missing '$VALUE' argument for FILES populate.")
        val file = target.file
        file.parentFile?.mkdirs()
        if (target.isBinary) {
            file.writeBytes(Base64.decode(value, Base64.DEFAULT))
        } else {
            file.writeText(value)
        }
        return BackupDeviceActionResult.success()
    }

    private fun failure(message: String): BackupDeviceActionResult =
        BackupDeviceActionResult.failure(message)
}
