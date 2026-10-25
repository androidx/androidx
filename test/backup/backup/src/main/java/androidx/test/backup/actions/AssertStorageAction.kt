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
import android.database.DatabaseUtils
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
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult

/**
 * Asserts the correctness of restored test data.
 *
 * Parses the configuration from [BackupDeviceActionArgs] and verifies
 * [android.content.SharedPreferences], Preferences DataStores, SQLite databases, or raw file
 * contents in credential-encrypted or device-protected storage. See
 * [androidx.test.backup.BackupActionInputKeys] for the keys each storage type consumes. With
 * [androidx.test.backup.BackupActionInputKeys.EXPECT_NULL], it verifies that the data is absent.
 *
 * A mismatch is reported as [BackupDeviceActionResult.failure] carrying the expected and the actual
 * value, not as a thrown exception. Long values are shortened to the part where they differ.
 */
public class AssertStorageAction : BackupDeviceAction {
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
                is StorageTarget.PreferencesDataStore -> verify(target, args)
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
    ): BackupDeviceActionResult =
        verifyPreference(
            target.context.getSharedPreferences(target.prefName, Context.MODE_PRIVATE).all,
            target.key,
            "'${target.prefName}'",
            args,
            ::parsePreferenceValue,
        )

    private fun verify(
        target: StorageTarget.PreferencesDataStore,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult =
        verifyPreference(
            readPreferencesDataStore(target.file),
            target.key,
            "DataStore '${target.name}'",
            args,
            ::parseDataStoreValue,
        )

    /**
     * Verifies the preference [key] in [preferences] against the value that [parse] reads from
     * [args]. Failure messages name the store [storeName].
     */
    private fun verifyPreference(
        preferences: Map<String, Any?>,
        key: String,
        storeName: String,
        args: BackupDeviceActionArgs,
        parse: (value: String, valueType: String) -> Any,
    ): BackupDeviceActionResult {
        val actual = preferences[key]
        // Absence is checked before the type, so a key stored with an unexpected type is reported
        // as present rather than as a type mismatch.
        if (args[EXPECT_NULL].toBoolean()) {
            return if (key !in preferences) {
                BackupDeviceActionResult.success()
            } else {
                failure(
                    "Expected preference '$key' in $storeName to be absent, but found " +
                        "${shown(actual)}."
                )
            }
        }

        val expectedRaw =
            args[EXPECTED]
                ?: args[VALUE]
                ?: return failure(
                    "Missing '$EXPECTED' or '$VALUE' argument for preference '$key' in $storeName."
                )
        val valueType = args[VALUE_TYPE] ?: VALUE_TYPE_STRING
        val expected =
            try {
                parse(expectedRaw, valueType)
            } catch (e: IllegalArgumentException) {
                return failure(e.message ?: "Invalid expected preference value.")
            }
        if (key !in preferences) {
            return failure(
                "Preference '$key' not found in $storeName; expected ${shown(expected)}."
            )
        }
        // Sets are equal whatever their class and the order of their elements.
        if (actual?.javaClass != expected.javaClass && !(actual is Set<*> && expected is Set<*>)) {
            return failure(
                "Preference '$key' in $storeName is not of expected type " +
                    "${valueType.uppercase()}: found ${actual?.javaClass?.simpleName}."
            )
        }
        if (expected is ByteArray) {
            val bytes = actual as ByteArray
            return if (expected.contentEquals(bytes)) {
                BackupDeviceActionResult.success()
            } else {
                failure(
                    "Preference '$key' in $storeName: ${binaryMismatch("value", expected, bytes)}."
                )
            }
        }
        return if (actual == expected) {
            BackupDeviceActionResult.success()
        } else {
            failure(
                "Preference '$key' in $storeName: expected ${shown(expected)} but found " +
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
        if (args[EXPECT_NULL].toBoolean()) return verifyRowAbsent(target, keyCol, keyVal)

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

    /**
     * Returns success unless the table of [target] has a row with [keyVal] in [keyCol]. A database
     * or a table that doesn't exist has no such row.
     */
    private fun verifyRowAbsent(
        target: StorageTarget.Database,
        keyCol: String,
        keyVal: String,
    ): BackupDeviceActionResult {
        // Verifying must not create a database that the restore did not bring back.
        val dbFile = target.context.getDatabasePath(target.dbName)
        if (!dbFile.exists()) return BackupDeviceActionResult.success()
        val rows =
            SQLiteDatabase.openDatabase(dbFile.path, null, OPEN_READONLY).use { db ->
                val tables =
                    DatabaseUtils.queryNumEntries(
                        db,
                        "sqlite_master",
                        "type = 'table' AND name = ? COLLATE NOCASE",
                        arrayOf(target.table),
                    )
                if (tables == 0L) 0L
                else
                    DatabaseUtils.queryNumEntries(
                        db,
                        "`${target.table}`",
                        "`$keyCol` = ?",
                        arrayOf(keyVal),
                    )
            }
        return if (rows == 0L) {
            BackupDeviceActionResult.success()
        } else {
            failure(
                "Expected no row with $keyCol='$keyVal' in table '${target.table}', but found $rows."
            )
        }
    }

    private fun verify(
        target: StorageTarget.FileContent,
        args: BackupDeviceActionArgs,
    ): BackupDeviceActionResult {
        val file = target.file
        if (args[EXPECT_NULL].toBoolean()) {
            return if (!file.exists()) {
                BackupDeviceActionResult.success()
            } else {
                failure("Expected file ${file.absolutePath} to be absent, but found it.")
            }
        }
        val expected =
            args[EXPECTED]
                ?: args[VALUE]
                ?: return failure(
                    "Missing '$EXPECTED' or '$VALUE' argument for FILES verification."
                )
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
