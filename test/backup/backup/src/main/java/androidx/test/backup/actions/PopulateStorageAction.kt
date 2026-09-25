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
import androidx.test.backup.BackupActionInputKeys.DB_NAME
import androidx.test.backup.BackupActionInputKeys.IS_BINARY
import androidx.test.backup.BackupActionInputKeys.IS_DEVICE_PROTECTED
import androidx.test.backup.BackupActionInputKeys.PATH
import androidx.test.backup.BackupActionInputKeys.PREF_KEY
import androidx.test.backup.BackupActionInputKeys.PREF_NAME
import androidx.test.backup.BackupActionInputKeys.STORAGE_TYPE
import androidx.test.backup.BackupActionInputKeys.TABLE
import androidx.test.backup.BackupActionInputKeys.VALUE
import androidx.test.backup.BackupActionInputKeys.VALUES
import androidx.test.backup.BackupActionInputKeys.VALUE_TYPE
import androidx.test.backup.BackupActionPhase
import androidx.test.backup.BackupActionValues.DEFAULT_PREF_NAME
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_DATABASE
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_FILES
import androidx.test.backup.BackupActionValues.STORAGE_TYPE_PREFS
import androidx.test.backup.BackupActionValues.VALUE_TYPE_BOOLEAN
import androidx.test.backup.BackupActionValues.VALUE_TYPE_FLOAT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_INT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_LONG
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import java.io.File

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
        val isDeviceProtected = args[IS_DEVICE_PROTECTED]?.toBoolean() ?: false
        val targetContext =
            if (isDeviceProtected) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
        val storageType = args[STORAGE_TYPE]?.uppercase() ?: STORAGE_TYPE_PREFS

        return try {
            when (storageType) {
                STORAGE_TYPE_PREFS -> {
                    val prefName = args[PREF_NAME] ?: DEFAULT_PREF_NAME
                    val key =
                        args[PREF_KEY]
                            ?: return failure("Missing '$PREF_KEY' argument for PREFS populate.")
                    val value =
                        args[VALUE]
                            ?: return failure("Missing '$VALUE' argument for PREFS populate.")
                    val valueType = args[VALUE_TYPE]?.uppercase() ?: VALUE_TYPE_STRING

                    val editor =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
                    when (valueType) {
                        VALUE_TYPE_INT -> editor.putInt(key, value.toInt())
                        VALUE_TYPE_LONG -> editor.putLong(key, value.toLong())
                        VALUE_TYPE_FLOAT -> editor.putFloat(key, value.toFloat())
                        VALUE_TYPE_BOOLEAN -> editor.putBoolean(key, value.toBoolean())
                        VALUE_TYPE_STRING -> editor.putString(key, value)
                        else -> return failure("Unsupported $VALUE_TYPE: ${args[VALUE_TYPE]}")
                    }
                    editor.commit()
                }

                STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        args[DB_NAME]
                            ?: return failure("Missing '$DB_NAME' argument for DATABASE populate.")
                    val table =
                        args[TABLE]
                            ?: return failure("Missing '$TABLE' argument for DATABASE populate.")
                    val valuesStr =
                        args[VALUES]
                            ?: return failure("Missing '$VALUES' argument for DATABASE populate.")
                    val columns =
                        try {
                            decodeColumnValues(valuesStr)
                        } catch (e: IllegalArgumentException) {
                            return failure(e.message ?: "Malformed '$VALUES' argument.")
                        }

                    targetContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db
                        ->
                        // Ensure the table exists
                        val cv = ContentValues()
                        val colDefs = mutableListOf<String>()
                        for ((columnName, columnValue) in columns) {
                            colDefs.add("`$columnName` TEXT")
                            cv.put(columnName, columnValue)
                        }
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `$table` (${colDefs.joinToString(", ")})"
                        )
                        // SQLite errors (unknown column, constraint violation) propagate out of
                        // insertWithOnConflict as SQLException and are turned into a failure by
                        // the catch below. A -1 return means the statement ran but inserted no
                        // row, which CONFLICT_REPLACE should never produce; check it anyway so a
                        // future change to the conflict algorithm cannot seed nothing silently.
                        val rowId =
                            db.insertWithOnConflict(
                                table,
                                null,
                                cv,
                                SQLiteDatabase.CONFLICT_REPLACE,
                            )
                        if (rowId == -1L) {
                            return failure(
                                "Insert into table '$table' did not add a row " +
                                    "(insertWithOnConflict returned -1)."
                            )
                        }
                    }
                }

                STORAGE_TYPE_FILES -> {
                    val path =
                        args[PATH] ?: return failure("Missing '$PATH' argument for FILES populate.")
                    val value =
                        args[VALUE]
                            ?: return failure("Missing '$VALUE' argument for FILES populate.")

                    val file =
                        File(path).let {
                            if (it.isAbsolute) {
                                it
                            } else {
                                File(targetContext.filesDir, path)
                            }
                        }
                    file.parentFile?.mkdirs()

                    val isBinary = args[IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val bytes = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
                        file.writeBytes(bytes)
                    } else {
                        file.writeText(value)
                    }
                }

                else -> return failure("Unsupported $STORAGE_TYPE: ${args[STORAGE_TYPE]}")
            }

            // Automatically notify BackupManager that data has changed
            BackupManager(targetContext).dataChanged()

            BackupDeviceActionResult.success()
        } catch (e: Exception) {
            failure("PopulateStorageAction exception: ${e.message}")
        }
    }

    private fun failure(message: String): BackupDeviceActionResult =
        BackupDeviceActionResult.failure(message)
}
