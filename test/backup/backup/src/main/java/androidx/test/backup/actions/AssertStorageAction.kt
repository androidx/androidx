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
import androidx.test.backup.ActionPhase
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceAction.Companion.KEY_DB_NAME
import androidx.test.backup.BackupDeviceAction.Companion.KEY_ERROR
import androidx.test.backup.BackupDeviceAction.Companion.KEY_EXPECTED
import androidx.test.backup.BackupDeviceAction.Companion.KEY_EXPECTED_COL
import androidx.test.backup.BackupDeviceAction.Companion.KEY_EXPECTED_VAL
import androidx.test.backup.BackupDeviceAction.Companion.KEY_EXPECT_NULL
import androidx.test.backup.BackupDeviceAction.Companion.KEY_IS_BINARY
import androidx.test.backup.BackupDeviceAction.Companion.KEY_IS_DEVICE_PROTECTED
import androidx.test.backup.BackupDeviceAction.Companion.KEY_KEY_COL
import androidx.test.backup.BackupDeviceAction.Companion.KEY_KEY_VAL
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
 * Asserts the correctness of restored test data.
 *
 * Parses the configuration from [BackupDeviceActionArgs] and verifies
 * [android.content.SharedPreferences], SQLite databases, or raw file contents in
 * credential-encrypted or device-protected storage.
 */
public class AssertStorageAction : BackupDeviceAction {
    @get:ActionPhase override val phase: Int = BackupDeviceAction.PHASE_VERIFY

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
                            ?: return errorResult(
                                "Missing 'pref_key' argument for PREFS verification."
                            )

                    val expectNull = payload[KEY_EXPECT_NULL]?.toBoolean() ?: false
                    val sharedPrefs =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val valueType = payload[KEY_VALUE_TYPE] ?: "STRING"

                    val actual =
                        if (!sharedPrefs.contains(key)) {
                            null
                        } else {
                            when (valueType.uppercase()) {
                                "INT" -> sharedPrefs.getInt(key, 0).toString()
                                "LONG" -> sharedPrefs.getLong(key, 0L).toString()
                                "FLOAT" -> sharedPrefs.getFloat(key, 0.0f).toString()
                                "BOOLEAN" -> sharedPrefs.getBoolean(key, false).toString()
                                else -> sharedPrefs.getString(key, null)
                            }
                        }

                    if (expectNull) {
                        if (actual == null) {
                            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
                        } else {
                            errorResult(
                                "Expected preference '$key' to be absent (null), but found '$actual'"
                            )
                        }
                    } else {
                        val expected =
                            payload[KEY_EXPECTED]
                                ?: payload[KEY_VALUE]
                                ?: return errorResult(
                                    "Missing 'expected' or 'value' argument for PREFS verification."
                                )
                        if (actual == expected) {
                            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
                        } else {
                            errorResult("Expected '$expected' but found '$actual'")
                        }
                    }
                }

                STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        payload[KEY_DB_NAME]
                            ?: return errorResult(
                                "Missing 'db_name' argument for DATABASE verification."
                            )
                    val table =
                        payload[KEY_TABLE]
                            ?: return errorResult(
                                "Missing 'table' argument for DATABASE verification."
                            )
                    val keyCol =
                        payload[KEY_KEY_COL] ?: return errorResult("Missing 'key_col' argument.")
                    val keyVal =
                        payload[KEY_KEY_VAL] ?: return errorResult("Missing 'key_val' argument.")

                    // Extract column name and expected column value from parameters.
                    // If 'values' is provided as a 'colName=colVal' pair, parse it.
                    // Otherwise, rely on separate 'expected_col' and 'expected_val' arguments.
                    val expectedCol: String
                    val expectedVal: String
                    val values = payload[KEY_VALUES] ?: payload[KEY_VALUE]
                    if ((values != null) && values.contains("=")) {
                        val pair = values.split("&").firstOrNull { it.contains("=") } ?: ""
                        val parts = pair.split("=", limit = 2)
                        expectedCol = parts[0]
                        expectedVal = if (parts.size > 1) parts[1] else ""
                    } else {
                        expectedCol =
                            payload[KEY_EXPECTED_COL]
                                ?: return errorResult("Missing 'expected_col' argument.")
                        expectedVal =
                            payload[KEY_EXPECTED_VAL]
                                ?: return errorResult("Missing 'expected_val' argument.")
                    }

                    targetContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db
                        ->
                        val cursor =
                            db.rawQuery(
                                "SELECT `$expectedCol` FROM `$table` WHERE `$keyCol` = ?",
                                arrayOf(keyVal),
                            )
                        var actual: String? = null
                        if (cursor.moveToFirst()) {
                            actual = cursor.getString(0)
                        }
                        cursor.close()

                        if (actual == expectedVal) {
                            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
                        } else {
                            errorResult(
                                "Expected column '$expectedCol' to be '$expectedVal' but was '$actual'"
                            )
                        }
                    }
                }

                STORAGE_TYPE_FILES -> {
                    val path =
                        payload[KEY_PATH]
                            ?: return errorResult("Missing 'path' argument for FILES verification.")
                    val expected =
                        payload[KEY_EXPECTED]
                            ?: payload[KEY_VALUE]
                            ?: return errorResult(
                                "Missing 'expected' or 'value' argument for FILES verification."
                            )

                    val file =
                        File(path).let {
                            if (it.isAbsolute) {
                                it
                            } else {
                                File(targetContext.filesDir, path)
                            }
                        }
                    if (!file.exists()) {
                        return errorResult("File not found at path: ${file.absolutePath}")
                    }

                    val isBinary = payload[KEY_IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val actualBytes = file.readBytes()
                        val expectedBytes =
                            android.util.Base64.decode(expected, android.util.Base64.DEFAULT)
                        if (actualBytes.contentEquals(expectedBytes)) {
                            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
                        } else {
                            errorResult("Binary file contents did not match expected.")
                        }
                    } else {
                        val actual = file.readText()
                        if (actual == expected) {
                            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
                        } else {
                            errorResult("Expected file content '$expected' but was '$actual'")
                        }
                    }
                }

                else -> return errorResult("Unsupported storage type: $storageTypeStr")
            }
        } catch (e: Exception) {
            errorResult("AssertStorageAction exception: ${e.message}")
        }
    }

    private fun errorResult(message: String): BackupDeviceActionResult {
        return BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_FAILURE, KEY_ERROR to message))
    }
}
