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
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import java.io.File

/**
 * Asserts the correctness of restored test data.
 *
 * Parses the configuration from [BackupDeviceActionArgs] and verifies
 * [android.content.SharedPreferences], SQLite databases, or raw file contents in
 * credential-encrypted or device-protected storage matching seeded criteria.
 */
public class AssertStorageAction : BackupDeviceAction {
    override val phase: ActionPhase = ActionPhase.VERIFY

    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val payload = args.payload
        val isDeviceProtected =
            payload[PopulateStorageAction.KEY_IS_DEVICE_PROTECTED]?.toBoolean() ?: false
        val targetContext =
            if (isDeviceProtected) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
        val storageTypeStr =
            payload[PopulateStorageAction.KEY_STORAGE_TYPE]
                ?: PopulateStorageAction.STORAGE_TYPE_PREFS

        return try {
            when (storageTypeStr) {
                PopulateStorageAction.STORAGE_TYPE_PREFS -> {
                    val prefName = payload[PopulateStorageAction.KEY_PREF_NAME] ?: "default_prefs"
                    val key =
                        payload[PopulateStorageAction.KEY_PREF_KEY]
                            ?: return errorResult(
                                "Missing 'pref_key' argument for PREFS verification."
                            )

                    val expectNull = payload[KEY_EXPECT_NULL]?.toBoolean() ?: false
                    val sharedPrefs =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val valueType = payload[PopulateStorageAction.KEY_VALUE_TYPE] ?: "STRING"

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
                            BackupDeviceActionResult(
                                mapOf(
                                    PopulateStorageAction.KEY_STATUS to
                                        PopulateStorageAction.STATUS_SUCCESS
                                )
                            )
                        } else {
                            errorResult(
                                "Expected preference '$key' to be absent (null), but found '$actual'"
                            )
                        }
                    } else {
                        val expected =
                            payload[KEY_EXPECTED]
                                ?: payload[PopulateStorageAction.KEY_VALUE]
                                ?: return errorResult(
                                    "Missing 'expected' or 'value' argument for PREFS verification."
                                )
                        if (actual == expected) {
                            BackupDeviceActionResult(
                                mapOf(
                                    PopulateStorageAction.KEY_STATUS to
                                        PopulateStorageAction.STATUS_SUCCESS
                                )
                            )
                        } else {
                            errorResult("Expected '$expected' but found '$actual'")
                        }
                    }
                }

                PopulateStorageAction.STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        payload[PopulateStorageAction.KEY_DB_NAME]
                            ?: return errorResult(
                                "Missing 'db_name' argument for DATABASE verification."
                            )
                    val table =
                        payload[PopulateStorageAction.KEY_TABLE]
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
                    val values =
                        payload[PopulateStorageAction.KEY_VALUES]
                            ?: payload[PopulateStorageAction.KEY_VALUE]
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
                            BackupDeviceActionResult(
                                mapOf(
                                    PopulateStorageAction.KEY_STATUS to
                                        PopulateStorageAction.STATUS_SUCCESS
                                )
                            )
                        } else {
                            errorResult(
                                "Expected column '$expectedCol' to be '$expectedVal' but was '$actual'"
                            )
                        }
                    }
                }

                PopulateStorageAction.STORAGE_TYPE_FILES -> {
                    val path =
                        payload[PopulateStorageAction.KEY_PATH]
                            ?: return errorResult("Missing 'path' argument for FILES verification.")
                    val expected =
                        payload[KEY_EXPECTED]
                            ?: payload[PopulateStorageAction.KEY_VALUE]
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

                    val isBinary =
                        payload[PopulateStorageAction.KEY_IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val actualBytes = file.readBytes()
                        val expectedBytes =
                            android.util.Base64.decode(expected, android.util.Base64.DEFAULT)
                        if (actualBytes.contentEquals(expectedBytes)) {
                            BackupDeviceActionResult(
                                mapOf(
                                    PopulateStorageAction.KEY_STATUS to
                                        PopulateStorageAction.STATUS_SUCCESS
                                )
                            )
                        } else {
                            errorResult("Binary file contents did not match expected.")
                        }
                    } else {
                        val actual = file.readText()
                        if (actual == expected) {
                            BackupDeviceActionResult(
                                mapOf(
                                    PopulateStorageAction.KEY_STATUS to
                                        PopulateStorageAction.STATUS_SUCCESS
                                )
                            )
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
        return BackupDeviceActionResult(
            mapOf(
                PopulateStorageAction.KEY_STATUS to PopulateStorageAction.STATUS_FAILURE,
                "error" to message,
            )
        )
    }

    public companion object {
        /**
         * The payload key specifying whether to assert complete absence of a preference.
         *
         * The corresponding value must be a string representation of a boolean: `"true"` or
         * `"false"`.
         */
        public const val KEY_EXPECT_NULL: String = "expect_null"

        /**
         * The payload key specifying the expected value to verify, formatted as a string.
         *
         * Since instrumentation arguments are restricted to strings, primitive values (such as
         * integers or booleans) must be passed as their string representations, which are parsed
         * internally to match the target preference type. For binary files, the value represents
         * the Base64-encoded string of the expected file content.
         */
        public const val KEY_EXPECTED: String = "expected"

        /** The payload key specifying the key column name in SQL query verification. */
        public const val KEY_KEY_COL: String = "key_col"

        /**
         * The payload key specifying the key column value to filter on in SQL verification.
         *
         * Must be passed as a string representation of the primitive key column value (e.g.,
         * `"123"` for numerical IDs).
         */
        public const val KEY_KEY_VAL: String = "key_val"

        /**
         * The payload key specifying the column containing the expected value in SQL verification.
         */
        public const val KEY_EXPECTED_COL: String = "expected_col"

        /**
         * The payload key specifying the expected value within the target column in SQL
         * verification.
         *
         * Must be passed as a string representation of the expected column value (e.g., `"true"` or
         * `"false"` for SQLite booleans, or `"text"` for strings).
         */
        public const val KEY_EXPECTED_VAL: String = "expected_val"
    }
}
