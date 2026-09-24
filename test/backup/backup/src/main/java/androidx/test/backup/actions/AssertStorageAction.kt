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
import androidx.test.backup.BackupActionInputKeys.EXPECTED
import androidx.test.backup.BackupActionInputKeys.EXPECTED_COL
import androidx.test.backup.BackupActionInputKeys.EXPECTED_VAL
import androidx.test.backup.BackupActionInputKeys.EXPECT_NULL
import androidx.test.backup.BackupActionInputKeys.IS_BINARY
import androidx.test.backup.BackupActionInputKeys.IS_DEVICE_PROTECTED
import androidx.test.backup.BackupActionInputKeys.KEY_COL
import androidx.test.backup.BackupActionInputKeys.KEY_VAL
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
 * Asserts the correctness of restored test data.
 *
 * Parses the configuration from [BackupDeviceActionArgs] and verifies
 * [android.content.SharedPreferences], SQLite databases, or raw file contents in
 * credential-encrypted or device-protected storage. See
 * [androidx.test.backup.BackupActionInputKeys] for the keys each storage type consumes.
 *
 * A mismatch is reported as [BackupDeviceActionResult.failure] carrying the expected and the actual
 * value, not as a thrown exception.
 */
public class AssertStorageAction : BackupDeviceAction {
    @get:BackupActionPhase override val phase: Int = BackupDeviceAction.PHASE_VERIFY

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
                            ?: return failure(
                                "Missing '$PREF_KEY' argument for PREFS verification."
                            )

                    val expectNull = args[EXPECT_NULL]?.toBoolean() ?: false
                    val sharedPrefs =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val valueType = args[VALUE_TYPE]?.uppercase() ?: VALUE_TYPE_STRING

                    val actual =
                        if (!sharedPrefs.contains(key)) {
                            null
                        } else {
                            when (valueType) {
                                VALUE_TYPE_INT -> sharedPrefs.getInt(key, 0).toString()
                                VALUE_TYPE_LONG -> sharedPrefs.getLong(key, 0L).toString()
                                VALUE_TYPE_FLOAT -> sharedPrefs.getFloat(key, 0.0f).toString()
                                VALUE_TYPE_BOOLEAN -> sharedPrefs.getBoolean(key, false).toString()
                                VALUE_TYPE_STRING -> sharedPrefs.getString(key, null)
                                else ->
                                    return failure("Unsupported $VALUE_TYPE: ${args[VALUE_TYPE]}")
                            }
                        }

                    if (expectNull) {
                        if (actual == null) {
                            BackupDeviceActionResult.success()
                        } else {
                            failure(
                                "Expected preference '$key' to be absent (null), but found '$actual'"
                            )
                        }
                    } else {
                        val expected =
                            args[EXPECTED]
                                ?: args[VALUE]
                                ?: return failure(
                                    "Missing '$EXPECTED' or '$VALUE' argument for PREFS verification."
                                )
                        if (actual == expected) {
                            BackupDeviceActionResult.success()
                        } else {
                            failure("Expected '$expected' but found '$actual'")
                        }
                    }
                }

                STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        args[DB_NAME]
                            ?: return failure(
                                "Missing '$DB_NAME' argument for DATABASE verification."
                            )
                    val table =
                        args[TABLE]
                            ?: return failure(
                                "Missing '$TABLE' argument for DATABASE verification."
                            )
                    val keyCol = args[KEY_COL] ?: return failure("Missing '$KEY_COL' argument.")
                    val keyVal = args[KEY_VAL] ?: return failure("Missing '$KEY_VAL' argument.")

                    // Collect every column to verify. When 'values' is supplied it carries the
                    // whole row that PopulateStorageAction inserted, so every pair is checked —
                    // verifying only the first would let a restore that corrupts a later column
                    // pass. Otherwise fall back to the single explicit 'expected_col' /
                    // 'expected_val' pair.
                    //
                    // 'values' is always an encoded pair list, so a malformed one is reported as
                    // such rather than silently falling through to 'expected_col'. The legacy
                    // 'value' fallback is a bare single value for preference and file storage, so
                    // it is only treated as a pair list when it actually looks like one.
                    val values = args[VALUES] ?: args[VALUE]?.takeIf { it.contains("=") }
                    val expectedColumns: List<Pair<String, String>> =
                        if (values != null) {
                            try {
                                decodeColumnValues(values)
                            } catch (e: IllegalArgumentException) {
                                return failure(e.message ?: "Malformed '$VALUES' argument.")
                            }
                        } else {
                            val expectedCol =
                                args[EXPECTED_COL]
                                    ?: return failure("Missing '$EXPECTED_COL' argument.")
                            val expectedVal =
                                args[EXPECTED_VAL]
                                    ?: return failure("Missing '$EXPECTED_VAL' argument.")
                            listOf(expectedCol to expectedVal)
                        }

                    targetContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db
                        ->
                        val projection = expectedColumns.joinToString(", ") { "`${it.first}`" }
                        db.rawQuery(
                                "SELECT $projection FROM `$table` WHERE `$keyCol` = ?",
                                arrayOf(keyVal),
                            )
                            .use { cursor ->
                                // A restore that did not happen at all leaves the row missing
                                // rather than the column null, so the two are reported
                                // differently.
                                if (!cursor.moveToFirst()) {
                                    return failure(
                                        "No row with $keyCol='$keyVal' in table '$table'."
                                    )
                                }
                                // Report every mismatch at once; failing on the first would hide
                                // the shape of a partial restore.
                                val mismatches =
                                    expectedColumns.mapIndexedNotNull { index, (column, expected) ->
                                        val actual = cursor.getString(index)
                                        if (actual == expected) {
                                            null
                                        } else {
                                            "Expected column '$column' to be '$expected' but " +
                                                "was '$actual'"
                                        }
                                    }
                                if (mismatches.isEmpty()) {
                                    BackupDeviceActionResult.success()
                                } else {
                                    failure(mismatches.joinToString("; "))
                                }
                            }
                    }
                }

                STORAGE_TYPE_FILES -> {
                    val path =
                        args[PATH]
                            ?: return failure("Missing '$PATH' argument for FILES verification.")
                    val expected =
                        args[EXPECTED]
                            ?: args[VALUE]
                            ?: return failure(
                                "Missing '$EXPECTED' or '$VALUE' argument for FILES verification."
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
                        return failure("File not found at path: ${file.absolutePath}")
                    }

                    val isBinary = args[IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val actualBytes = file.readBytes()
                        val expectedBytes =
                            android.util.Base64.decode(expected, android.util.Base64.DEFAULT)
                        if (actualBytes.contentEquals(expectedBytes)) {
                            BackupDeviceActionResult.success()
                        } else {
                            failure("Binary file contents did not match expected.")
                        }
                    } else {
                        val actual = file.readText()
                        if (actual == expected) {
                            BackupDeviceActionResult.success()
                        } else {
                            failure("Expected file content '$expected' but was '$actual'")
                        }
                    }
                }

                else -> return failure("Unsupported $STORAGE_TYPE: ${args[STORAGE_TYPE]}")
            }
        } catch (e: Exception) {
            failure("AssertStorageAction exception: ${e.message}")
        }
    }

    private fun failure(message: String): BackupDeviceActionResult =
        BackupDeviceActionResult.failure(message)
}
