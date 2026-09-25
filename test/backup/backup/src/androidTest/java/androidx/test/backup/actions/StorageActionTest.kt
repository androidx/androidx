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

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.test.backup.BackupActionInputKeys
import androidx.test.backup.BackupActionOutputKeys
import androidx.test.backup.BackupActionValues
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupRestoreTestRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests verifying the correctness of on-device backup and restore actions.
 *
 * This test suite asserts that [PopulateStorageAction] successfully seeds test data into
 * SharedPreferences, raw file storage, and SQLite databases, and that [AssertStorageAction]
 * correctly asserts the presence and contents of that data.
 */
@RunWith(AndroidJUnit4::class)
public class StorageActionTest {

    private lateinit var context: Context

    @Before
    public fun setUp() {
        // Initialize BackupRestoreTestRunner.instance with active test instrumentation
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        BackupRestoreTestRunner.instance = instrumentation
        context = instrumentation.targetContext.createDeviceProtectedStorageContext()
    }

    /**
     * Verifies that [PopulateStorageAction] seeds preferences and [AssertStorageAction] validates
     * them.
     */
    @Test
    public fun testPrefsPopulateAndVerify() {
        val prefName = "test_pref_store"
        val key = "test_key"
        val value = "test_value"

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value,
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_STRING,
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Assert preference was physically seeded
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getString(key, null))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value,
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_STRING,
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [PopulateStorageAction] and [AssertStorageAction] support typed integer
     * preferences.
     */
    @Test
    public fun testPrefsPopulateAndVerify_Int() {
        val prefName = "test_pref_store_int"
        val key = "test_int_key"
        val value = 42

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value.toString(),
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_INT,
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Assert preference was physically seeded with correct type
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getInt(key, 0))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value.toString(),
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_INT,
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [PopulateStorageAction] and [AssertStorageAction] support typed boolean
     * preferences.
     */
    @Test
    public fun testPrefsPopulateAndVerify_Boolean() {
        val prefName = "test_pref_store_bool"
        val key = "test_bool_key"
        val value = true

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value.toString(),
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_BOOLEAN,
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Assert preference was physically seeded with correct type
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getBoolean(key, false))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.VALUE to value.toString(),
                    BackupActionInputKeys.VALUE_TYPE to BackupActionValues.VALUE_TYPE_BOOLEAN,
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [PopulateStorageAction] writes local files and [AssertStorageAction] validates
     * their content.
     */
    @Test
    public fun testFilesPopulateAndVerify() {
        val fileName = "test_file_store.txt"
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        val fileContent = "This is a local backup test file content."

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to file.absolutePath,
                    BackupActionInputKeys.VALUE to fileContent,
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Assert file was physically created
        assertEquals(fileContent, file.readText())

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to file.absolutePath,
                    BackupActionInputKeys.VALUE to fileContent,
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [PopulateStorageAction] populates database tables and [AssertStorageAction]
     * validates the records.
     */
    @Test
    public fun testDatabasePopulateAndVerify() {
        val dbName = "test_db_store.db"
        val table = "test_table"
        val colName = "test_col"
        val colVal = "test_val"

        context.deleteDatabase(dbName)

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_DATABASE,
                    BackupActionInputKeys.DB_NAME to dbName,
                    BackupActionInputKeys.TABLE to table,
                    BackupActionInputKeys.VALUES to "$colName=$colVal",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_DATABASE,
                    BackupActionInputKeys.DB_NAME to dbName,
                    BackupActionInputKeys.TABLE to table,
                    BackupActionInputKeys.KEY_COL to colName,
                    BackupActionInputKeys.KEY_VAL to colVal,
                    BackupActionInputKeys.VALUES to "$colName=$colVal",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [PopulateStorageAction] populates database tables with multiple columns and
     * [AssertStorageAction] correctly parses ampersand-separated pairs.
     */
    @Test
    public fun testDatabasePopulateAndVerifyMultipleColumns() {
        val dbName = "test_multi_db_store.db"
        val table = "test_multi_table"
        val col1Name = "col1"
        val col1Val = "val1"
        val col2Name = "col2"
        val col2Val = "val2"

        context.deleteDatabase(dbName)

        // 1. Populate multiple columns
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_DATABASE,
                    BackupActionInputKeys.DB_NAME to dbName,
                    BackupActionInputKeys.TABLE to table,
                    BackupActionInputKeys.VALUES to "$col1Name=$col1Val&$col2Name=$col2Val",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // 2. Verify using multi-column KEY_VALUES
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_DATABASE,
                    BackupActionInputKeys.DB_NAME to dbName,
                    BackupActionInputKeys.TABLE to table,
                    BackupActionInputKeys.KEY_COL to col1Name,
                    BackupActionInputKeys.KEY_VAL to col1Val,
                    BackupActionInputKeys.VALUES to "$col1Name=$col1Val&$col2Name=$col2Val",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * A mismatch in a column other than the first fails the assertion.
     *
     * [AssertStorageAction] used to verify only the first pair of [BackupActionInputKeys.VALUES],
     * so a restore that corrupted any later column of the row passed silently.
     */
    @Test
    public fun testDatabaseVerifyDetectsNonFirstColumnMismatch() {
        val dbName = "test_multi_mismatch_db.db"
        val table = "test_multi_mismatch_table"

        context.deleteDatabase(dbName)

        val putResult =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.VALUES to "id=row1&payload=stored&extra=kept",
                        )
                    ),
                )
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // The key column still matches, so the row is found; only 'payload' differs.
        val verifyResult =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.KEY_COL to "id",
                            BackupActionInputKeys.KEY_VAL to "row1",
                            BackupActionInputKeys.VALUES to "id=row1&payload=corrupted&extra=kept",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
        val error = verifyResult.payload[BackupActionOutputKeys.ERROR]
        assertNotNull(error)
        assertTrue(
            "Expected the mismatching column to be named, but was: $error",
            error!!.contains("payload"),
        )
        assertTrue(
            "Expected the actual stored value to be reported, but was: $error",
            error.contains("stored"),
        )
    }

    /** Every mismatching column is reported at once, so a partial restore is diagnosable. */
    @Test
    public fun testDatabaseVerifyReportsEveryColumnMismatch() {
        val dbName = "test_multi_all_mismatch_db.db"
        val table = "test_multi_all_mismatch_table"

        context.deleteDatabase(dbName)

        PopulateStorageAction()
            .execute(
                context,
                BackupDeviceActionArgs(
                    mapOf(
                        BackupActionInputKeys.STORAGE_TYPE to
                            BackupActionValues.STORAGE_TYPE_DATABASE,
                        BackupActionInputKeys.DB_NAME to dbName,
                        BackupActionInputKeys.TABLE to table,
                        BackupActionInputKeys.VALUES to "id=row1&first=a&second=b",
                    )
                ),
            )

        val verifyResult =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.KEY_COL to "id",
                            BackupActionInputKeys.KEY_VAL to "row1",
                            BackupActionInputKeys.VALUES to "id=row1&first=x&second=y",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
        val error = verifyResult.payload[BackupActionOutputKeys.ERROR]
        assertNotNull(error)
        assertTrue(
            "Expected both mismatching columns to be reported, but was: $error",
            error!!.contains("first") && error.contains("second"),
        )
    }

    /**
     * Verifies that [PopulateStorageAction] writes binary files when KEY_IS_BINARY is true, and
     * [AssertStorageAction] validates their content correctly.
     */
    @Test
    public fun testBinaryFilesPopulateAndVerify() {
        val fileName = "test_binary_store.bin"
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        val binaryData = byteArrayOf(0, 1, 2, 3, 4, 127, -128, -1)
        val base64Encoded =
            android.util.Base64.encodeToString(binaryData, android.util.Base64.DEFAULT)

        // 1. Populate
        val putAction = PopulateStorageAction()
        val putArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to file.absolutePath,
                    BackupActionInputKeys.VALUE to base64Encoded,
                    BackupActionInputKeys.IS_BINARY to "true",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Assert file was physically created with exact bytes
        assertTrue(file.exists())
        assertTrue(binaryData.contentEquals(file.readBytes()))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to file.absolutePath,
                    BackupActionInputKeys.VALUE to base64Encoded,
                    BackupActionInputKeys.IS_BINARY to "true",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that [AssertStorageAction] validates preference absence correctly when
     * KEY_EXPECT_NULL is true.
     */
    @Test
    public fun testPrefsNullVerify() {
        val prefName = "test_null_pref_store"
        val key = "test_absent_key"

        // Ensure key is absent
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(key).commit()

        // Verify absence
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_PREFS,
                    BackupActionInputKeys.PREF_NAME to prefName,
                    BackupActionInputKeys.PREF_KEY to key,
                    BackupActionInputKeys.EXPECT_NULL to "true",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * Verifies that a column value containing the wire-format separators survives the round trip.
     *
     * Before the `values` argument was percent-encoded on both sides, an embedded `&` or `=` split
     * the value into bogus extra columns.
     */
    @Test
    public fun testDatabaseValuesContainingSeparators() {
        val dbName = "test_escaped_db_store.db"
        val table = "test_escaped_table"
        val colName = "note"
        val colVal = "a&b=c 100%"
        val encoded =
            URLEncoder.encode(colName, StandardCharsets.UTF_8.name()) +
                "=" +
                URLEncoder.encode(colVal, StandardCharsets.UTF_8.name())

        context.deleteDatabase(dbName)

        val putResult =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.VALUES to encoded,
                        )
                    ),
                )
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        val verifyResult =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.KEY_COL to colName,
                            BackupActionInputKeys.KEY_VAL to colVal,
                            BackupActionInputKeys.VALUES to encoded,
                        )
                    ),
                )
        assertEquals(
            "Verification failed: ${verifyResult.payload[BackupActionOutputKeys.ERROR]}",
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /** A malformed `values` segment fails the action instead of being silently dropped. */
    @Test
    public fun testDatabaseRejectsMalformedValues() {
        val result =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to "test_malformed_db.db",
                            BackupActionInputKeys.TABLE to "t",
                            BackupActionInputKeys.VALUES to "col1=val1&orphan",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            result.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /** An unrecognized value type fails rather than silently degrading to a string preference. */
    @Test
    public fun testPrefsRejectsUnsupportedValueType() {
        val result =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_PREFS,
                            BackupActionInputKeys.PREF_NAME to "test_bad_type_prefs",
                            BackupActionInputKeys.PREF_KEY to "k",
                            BackupActionInputKeys.VALUE to "1",
                            BackupActionInputKeys.VALUE_TYPE to "DOUBLE",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            result.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /** Storage and value types are matched without regard to case. */
    @Test
    public fun testTypesAreCaseInsensitive() {
        val prefName = "test_case_prefs"

        val putResult =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to "prefs",
                            BackupActionInputKeys.PREF_NAME to prefName,
                            BackupActionInputKeys.PREF_KEY to "k",
                            BackupActionInputKeys.VALUE to "7",
                            BackupActionInputKeys.VALUE_TYPE to "int",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )
        assertEquals(7, context.getSharedPreferences(prefName, Context.MODE_PRIVATE).getInt("k", 0))
    }

    /**
     * Type matching must not depend on the device locale.
     *
     * In Turkish and Azerbaijani, `"i"` uppercases to a dotted `İ`, so a locale-sensitive fold
     * would turn `"files"` into `FİLES` and `"int"` into `İNT`, neither of which matches the
     * declared constants. The actions rely on [String.uppercase], which folds with the invariant
     * locale rather than the default one; this test pins that guarantee so a future switch to the
     * locale-sensitive [String.uppercase] overload or to `toUpperCase()` is caught here.
     */
    @Test
    public fun testTypesAreCaseInsensitiveInTurkishLocale() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        try {
            // Guards the guard: if the hostile locale were not actually in effect, the assertions
            // below would pass for the wrong reason.
            assertEquals("İNT", "int".uppercase(Locale.getDefault()))

            val prefName = "test_turkish_prefs"
            val putResult =
                PopulateStorageAction()
                    .execute(
                        context,
                        BackupDeviceActionArgs(
                            mapOf(
                                BackupActionInputKeys.STORAGE_TYPE to "prefs",
                                BackupActionInputKeys.PREF_NAME to prefName,
                                BackupActionInputKeys.PREF_KEY to "k",
                                BackupActionInputKeys.VALUE to "7",
                                BackupActionInputKeys.VALUE_TYPE to "int",
                            )
                        ),
                    )
            assertEquals(
                BackupActionValues.STATUS_SUCCESS,
                putResult.payload[BackupActionOutputKeys.STATUS],
            )
            assertEquals(
                7,
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).getInt("k", 0),
            )

            // 'files' is the storage type most affected by the dotted-I fold.
            val fileName = "turkish_locale.txt"
            val fileResult =
                PopulateStorageAction()
                    .execute(
                        context,
                        BackupDeviceActionArgs(
                            mapOf(
                                BackupActionInputKeys.STORAGE_TYPE to "files",
                                BackupActionInputKeys.PATH to fileName,
                                BackupActionInputKeys.VALUE to "content",
                            )
                        ),
                    )
            assertEquals(
                BackupActionValues.STATUS_SUCCESS,
                fileResult.payload[BackupActionOutputKeys.STATUS],
            )
            assertEquals("content", File(context.filesDir, fileName).readText())
        } finally {
            Locale.setDefault(original)
        }
    }

    /**
     * A malformed `values` reports itself rather than being reported as a missing `expected_col`.
     */
    @Test
    public fun testDatabaseMalformedValuesIsNotMaskedAsMissingColumn() {
        val result =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to "test_masked_db.db",
                            BackupActionInputKeys.TABLE to "t",
                            BackupActionInputKeys.KEY_COL to "k",
                            BackupActionInputKeys.KEY_VAL to "v",
                            BackupActionInputKeys.VALUES to "orphan",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            result.payload[BackupActionOutputKeys.STATUS],
        )
        val error = result.payload[BackupActionOutputKeys.ERROR]
        assertTrue(
            "Expected a malformed-argument error but was '$error'",
            error != null && !error.contains(BackupActionInputKeys.EXPECTED_COL),
        )
    }

    /**
     * A bare `value` carries no column pair, so database verification still falls back to the
     * explicit `expected_col` and `expected_val` arguments.
     */
    @Test
    public fun testDatabaseBareValueFallsBackToExpectedColumns() {
        val dbName = "test_bare_value_db.db"
        val table = "test_table"
        val colName = "test_col"
        val colVal = "test_val"

        context.deleteDatabase(dbName)

        val putResult =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.VALUES to "$colName=$colVal",
                        )
                    ),
                )
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        val verifyResult =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.KEY_COL to colName,
                            BackupActionInputKeys.KEY_VAL to colVal,
                            BackupActionInputKeys.VALUE to "a bare value",
                            BackupActionInputKeys.EXPECTED_COL to colName,
                            BackupActionInputKeys.EXPECTED_VAL to colVal,
                        )
                    ),
                )
        assertEquals(
            "Verification failed: ${verifyResult.payload[BackupActionOutputKeys.ERROR]}",
            BackupActionValues.STATUS_SUCCESS,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
    }

    /**
     * A row that is absent is reported differently from a column that is present but null.
     *
     * A restore that did not run at all produces the absent-row case, which is the single most
     * common real failure; conflating it with a null column sends you looking in the wrong place.
     */
    @Test
    public fun testDatabaseMissingRowIsReportedDistinctly() {
        val dbName = "test_missing_row_db.db"
        val table = "test_table"

        context.deleteDatabase(dbName)

        val putResult =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.VALUES to "id=row1&payload=hello",
                        )
                    ),
                )
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            putResult.payload[BackupActionOutputKeys.STATUS],
        )

        // Ask for a row that was never inserted.
        val verifyResult =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.KEY_COL to "id",
                            BackupActionInputKeys.KEY_VAL to "row_that_does_not_exist",
                            BackupActionInputKeys.EXPECTED_COL to "payload",
                            BackupActionInputKeys.EXPECTED_VAL to "hello",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            verifyResult.payload[BackupActionOutputKeys.STATUS],
        )
        val error = verifyResult.payload[BackupActionOutputKeys.ERROR]
        assertTrue(
            "Expected a missing-row error but was '$error'",
            error != null && error.contains("No row with"),
        )
    }

    /**
     * A column named in `values` that the existing table does not have fails the populate, and the
     * underlying SQLite error is surfaced rather than swallowed.
     */
    @Test
    public fun testDatabaseInsertFailureIsReported() {
        val dbName = "test_insert_fail_db.db"
        val table = "test_table"

        context.deleteDatabase(dbName)

        // Create the table with a single column so a later insert naming an unknown column fails.
        context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `$table` (`id` TEXT)")
        }

        val result =
            PopulateStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(
                        mapOf(
                            BackupActionInputKeys.STORAGE_TYPE to
                                BackupActionValues.STORAGE_TYPE_DATABASE,
                            BackupActionInputKeys.DB_NAME to dbName,
                            BackupActionInputKeys.TABLE to table,
                            BackupActionInputKeys.VALUES to "id=row1&unknown_column=boom",
                        )
                    ),
                )

        assertEquals(
            BackupActionValues.STATUS_FAILURE,
            result.payload[BackupActionOutputKeys.STATUS],
        )
        // Assert on the specific cause, otherwise this test would also pass for an unrelated
        // failure such as a missing argument.
        val error = result.payload[BackupActionOutputKeys.ERROR]
        assertTrue(
            "Expected the SQLite error naming the bad column but was '$error'",
            error != null && error.contains("unknown_column"),
        )
    }

    /**
     * Pins the [SQLiteDatabase.insertWithOnConflict] contract that [PopulateStorageAction] relies
     * on: SQLite errors are thrown, they are not reported as a `-1` return. Only
     * [SQLiteDatabase .insert] and [SQLiteDatabase.replace] convert the exception to `-1`. If this
     * ever changes, the `catch` in [PopulateStorageAction] would stop being the path that reports
     * bad inserts.
     */
    @Test
    public fun testInsertWithOnConflictThrowsRatherThanReturningMinusOne() {
        val dbName = "test_insert_contract_db.db"
        val table = "test_table"

        context.deleteDatabase(dbName)

        context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `$table` (`id` TEXT)")
            val cv = ContentValues().apply { put("unknown_column", "boom") }

            try {
                val rowId =
                    db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                fail("Expected insertWithOnConflict to throw, but it returned $rowId")
            } catch (expected: SQLException) {
                // Expected.
            }

            // The forgiving convenience wrapper is the one that returns -1.
            assertEquals(-1L, db.insert(table, null, cv))
        }
    }
}
