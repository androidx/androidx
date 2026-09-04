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
import androidx.test.backup.ActionPhase
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceAction.Companion.KEY_DB_NAME
import androidx.test.backup.BackupDeviceAction.Companion.KEY_ERROR
import androidx.test.backup.BackupDeviceAction.Companion.KEY_IS_BINARY
import androidx.test.backup.BackupDeviceAction.Companion.KEY_IS_DEVICE_PROTECTED
import androidx.test.backup.BackupDeviceAction.Companion.KEY_PATH
import androidx.test.backup.BackupDeviceAction.Companion.KEY_PREF_KEY
import androidx.test.backup.BackupDeviceAction.Companion.KEY_PREF_NAME
import androidx.test.backup.BackupDeviceAction.Companion.KEY_STATUS
import androidx.test.backup.BackupDeviceAction.Companion.KEY_STORAGE_TYPE
import androidx.test.backup.BackupDeviceAction.Companion.KEY_TABLE
import androidx.test.backup.BackupDeviceAction.Companion.KEY_VALUE
import androidx.test.backup.BackupDeviceAction.Companion.KEY_VALUES
import androidx.test.backup.BackupDeviceAction.Companion.KEY_VALUE_TYPE
import androidx.test.backup.BackupDeviceAction.Companion.STATUS_FAILURE
import androidx.test.backup.BackupDeviceAction.Companion.STATUS_SUCCESS
import androidx.test.backup.BackupDeviceAction.Companion.STORAGE_TYPE_DATABASE
import androidx.test.backup.BackupDeviceAction.Companion.STORAGE_TYPE_FILES
import androidx.test.backup.BackupDeviceAction.Companion.STORAGE_TYPE_PREFS
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import java.io.File

/**
 * Populates test data inside the application sandbox before a backup.
 *
 * Parses the incoming [BackupDeviceAction.KEY_STORAGE_TYPE] argument and seeds the specified data
 * into [android.content.SharedPreferences], SQLite database, or raw file storage.
 */
public class PopulateStorageAction : BackupDeviceAction {
    @get:ActionPhase override val phase: Int = BackupDeviceAction.PHASE_POPULATE

    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val payload = args.payload
        val isDeviceProtected = payload[KEY_IS_DEVICE_PROTECTED]?.toBoolean() ?: false
        val targetContext =
            if (isDeviceProtected) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
        val storageTypeStr = payload[KEY_STORAGE_TYPE] ?: STORAGE_TYPE_PREFS

        return try {
            when (storageTypeStr) {
                STORAGE_TYPE_PREFS -> {
                    val prefName = payload[KEY_PREF_NAME] ?: "default_prefs"
                    val key =
                        payload[KEY_PREF_KEY]
                            ?: return errorResult("Missing 'pref_key' argument for PREFS populate.")
                    val value =
                        payload[KEY_VALUE]
                            ?: return errorResult("Missing 'value' argument for PREFS populate.")
                    val valueType = payload[KEY_VALUE_TYPE] ?: "STRING"

                    val editor =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
                    when (valueType.uppercase()) {
                        "INT" -> editor.putInt(key, value.toInt())
                        "LONG" -> editor.putLong(key, value.toLong())
                        "FLOAT" -> editor.putFloat(key, value.toFloat())
                        "BOOLEAN" -> editor.putBoolean(key, value.toBoolean())
                        else -> editor.putString(key, value)
                    }
                    editor.commit()
                }

                STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        payload[KEY_DB_NAME]
                            ?: return errorResult(
                                "Missing 'db_name' argument for DATABASE populate."
                            )
                    val table =
                        payload[KEY_TABLE]
                            ?: return errorResult("Missing 'table' argument for DATABASE populate.")
                    val valuesStr =
                        payload[KEY_VALUES]
                            ?: return errorResult(
                                "Missing 'values' argument for DATABASE populate."
                            )

                    targetContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db
                        ->
                        // Ensure the table exists
                        val cv = ContentValues()
                        val colDefs = mutableListOf<String>()
                        valuesStr.split("&").forEach { pair ->
                            val kv = pair.split("=")
                            if (kv.size == 2) {
                                val columnName =
                                    java.net.URLDecoder.decode(
                                        kv[0],
                                        java.nio.charset.StandardCharsets.UTF_8.name(),
                                    )
                                val columnValue =
                                    java.net.URLDecoder.decode(
                                        kv[1],
                                        java.nio.charset.StandardCharsets.UTF_8.name(),
                                    )
                                colDefs.add("`$columnName` TEXT")
                                cv.put(columnName, columnValue)
                            }
                        }
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `$table` (${colDefs.joinToString(", ")})"
                        )
                        db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                STORAGE_TYPE_FILES -> {
                    val path =
                        payload[KEY_PATH]
                            ?: return errorResult("Missing 'path' argument for FILES populate.")
                    val value =
                        payload[KEY_VALUE]
                            ?: return errorResult("Missing 'value' argument for FILES populate.")

                    val file =
                        File(path).let {
                            if (it.isAbsolute) {
                                it
                            } else {
                                File(targetContext.filesDir, path)
                            }
                        }
                    file.parentFile?.mkdirs()

                    val isBinary = payload[KEY_IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val bytes = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
                        file.writeBytes(bytes)
                    } else {
                        file.writeText(value)
                    }
                }

                else -> return errorResult("Unsupported storage type: $storageTypeStr")
            }

            // Automatically notify BackupManager that data has changed
            BackupManager(targetContext).dataChanged()

            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
        } catch (e: Exception) {
            errorResult("PopulateStorageAction exception: ${e.message}")
        }
    }

    private fun errorResult(message: String): BackupDeviceActionResult {
        return BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_FAILURE, KEY_ERROR to message))
    }
}
