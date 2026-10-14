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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.util.Base64
import androidx.test.backup.BackupActionInputKeys.EXPECTED
import androidx.test.backup.BackupActionInputKeys.EXPECTED_COL
import androidx.test.backup.BackupActionInputKeys.EXPECTED_VAL
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
 * Asserts the correctness of restored test data.
 *
 * Parses the configuration from [BackupDeviceActionArgs] and verifies
 * [android.content.SharedPreferences], SQLite databases, or raw file contents in
 * credential-encrypted or device-protected storage. See
 * [androidx.test.backup.BackupActionInputKeys] for the keys each storage type consumes.
 *
 * A mismatch is reported as [BackupDeviceActionResult.failure] carrying the expected and the actual
 * value, not as a thrown exception. Long values are shortened to the part where they differ.
 */
public class AssertStorageAction : BackupDeviceAction {
    @get:BackupActionPhase override val phase: Int = BackupDeviceAction.PHASE_VERIFY

    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val target =
            try {
                storageTargetOf(context, args, operation = "verification")
            } catch (e: StorageArgumentException) {
                return failure(e.message.orEmpty())
            }
        return try {
            when (target) {
                is StorageTarget.Preference -> verify(target, args)
                is StorageTarget.Database -> verify(target, args)
                is StorageTarget.FileContent -> verify(target, args)
            }
        } catch (e: Exception) {
            failure("AssertStorageAction exception: ${e.message}")
        }
    }

    private fun verify(
        target: StorageTarget.Preference,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val key = target.key
        val prefName = target.prefName
        val sharedPrefs = target.context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        // Absence is checked before any typed read, so a key stored with an unexpected type is
        // reported as present rather than as a type mismatch.
        if (args[EXPECT_NULL].toBoolean()) {
            return if (!sharedPrefs.contains(key)) {
                BackupDeviceActionResult.success()
            } else {
                failure(
                    "Expected preference '$key' in '$prefName' to be absent, but found " +
                        "${shown(sharedPrefs.all[key])}."
                )
            }
        }

        val expectedRaw =
            args[EXPECTED]
                ?: args[VALUE]
                ?: return failure(
                    "Missing '$EXPECTED' or '$VALUE' argument for PREFS verification."
                )
        val valueType = args[VALUE_TYPE] ?: VALUE_TYPE_STRING
        val expected =
            try {
                parsePreferenceValue(expectedRaw, valueType)
            } catch (e: IllegalArgumentException) {
                return failure(e.message ?: "Invalid expected preference value.")
            }
        if (!sharedPrefs.contains(key)) {
            return failure(
                "Preference '$key' not found in '$prefName'; expected ${shown(expected)}."
            )
        }
        val actual =
            try {
                when (expected) {
                    is Int -> sharedPrefs.getInt(key, 0)
                    is Long -> sharedPrefs.getLong(key, 0L)
                    is Float -> sharedPrefs.getFloat(key, 0.0f)
                    is Boolean -> sharedPrefs.getBoolean(key, false)
                    is String -> sharedPrefs.getString(key, null)
                    else -> error("Unexpected parsed preference value: $expected")
                }
            } catch (e: ClassCastException) {
                return failure(
                    "Preference '$key' in '$prefName' is not of expected type " +
                        "${valueType.uppercase()}: ${e.message}"
                )
            }
        return if (actual == expected) {
            BackupDeviceActionResult.success()
        } else {
            failure(
                "Preference '$key' in '$prefName': expected ${shown(expected)} but found " +
                    "${shown(actual)}."
            )
        }
    }

    private fun verify(
        target: StorageTarget.Database,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val dbName = target.dbName
        val table = target.table
        val keyCol = args[KEY_COL] ?: return failure("Missing '$KEY_COL' argument.")
        val keyVal = args[KEY_VAL] ?: return failure("Missing '$KEY_VAL' argument.")

        // Collect every column to verify. When 'values' is supplied it carries the whole row that
        // PopulateStorageAction inserted, so every pair is checked — verifying only the first would
        // let a restore that corrupts a later column pass. Otherwise fall back to the single
        // explicit 'expected_col' / 'expected_val' pair.
        //
        // 'values' is always an encoded pair list, so a malformed one is reported as such rather
        // than silently falling through to 'expected_col'. The legacy 'value' fallback is a bare
        // single value for preference and file storage, so it is only treated as a pair list when
        // it actually looks like one.
        val values = args[VALUES] ?: args[VALUE]?.takeIf { it.contains("=") }
        val expectedColumns: List<Pair<String, String?>> =
            if (values != null) {
                try {
                    decodeColumnValues(values)
                } catch (e: IllegalArgumentException) {
                    return failure(e.message ?: "Malformed '$VALUES' argument.")
                }
            } else {
                val expectedCol =
                    args[EXPECTED_COL] ?: return failure("Missing '$EXPECTED_COL' argument.")
                val expectedVal =
                    args[EXPECTED_VAL] ?: return failure("Missing '$EXPECTED_VAL' argument.")
                listOf(expectedCol to expectedVal)
            }

        // Verifying must not create a database that the restore did not bring back.
        val dbFile = target.context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            return failure("Database '$dbName' not found at ${dbFile.absolutePath}.")
        }
        SQLiteDatabase.openDatabase(dbFile.path, null, OPEN_READONLY).use { db ->
            val projection = expectedColumns.joinToString(", ") { "`${it.first}`" }
            db.rawQuery("SELECT $projection FROM `$table` WHERE `$keyCol` = ?", arrayOf(keyVal))
                .use { cursor ->
                    // A restore that did not happen at all leaves the row missing rather than the
                    // column null, so the two are reported differently.
                    if (!cursor.moveToFirst()) {
                        return failure("No row with $keyCol='$keyVal' in table '$table'.")
                    }
                    if (cursor.count > 1) {
                        return failure(
                            "Found ${cursor.count} rows with $keyCol='$keyVal' in table " +
                                "'$table'; expected exactly one."
                        )
                    }
                    // Report every mismatch at once; failing on the first would hide the shape of
                    // a partial restore.
                    val mismatches =
                        expectedColumns.mapIndexedNotNull { index, (column, expected) ->
                            val actual = if (cursor.isNull(index)) null else cursor.getString(index)
                            if (actual == expected) {
                                null
                            } else {
                                "Expected column '$column' to be ${sqlValue(expected)} but was " +
                                    sqlValue(actual)
                            }
                        }
                    return if (mismatches.isEmpty()) {
                        BackupDeviceActionResult.success()
                    } else {
                        failure(mismatches.joinToString("; "))
                    }
                }
        }
    }

    private fun verify(
        target: StorageTarget.FileContent,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val expected =
            args[EXPECTED]
                ?: args[VALUE]
                ?: return failure(
                    "Missing '$EXPECTED' or '$VALUE' argument for FILES verification."
                )
        val file = target.file
        if (!file.exists()) {
            return failure("File not found at path: ${file.absolutePath}")
        }
        return if (target.isBinary) {
            val actualBytes = file.readBytes()
            val expectedBytes = Base64.decode(expected, Base64.DEFAULT)
            if (actualBytes.contentEquals(expectedBytes)) {
                BackupDeviceActionResult.success()
            } else {
                failure(binaryMismatch("binary file content", expectedBytes, actualBytes))
            }
        } else {
            val actual = file.readText()
            if (actual == expected) {
                BackupDeviceActionResult.success()
            } else {
                failure(textMismatch("file content", expected, actual))
            }
        }
    }

    private fun failure(message: String): BackupDeviceActionResult =
        BackupDeviceActionResult.failure(message)

    /** Returns [value] as messages show a column value: quoted, or `NULL` for SQL `NULL`. */
    private fun sqlValue(value: String?): String = if (value == null) "NULL" else shown(value)
}
