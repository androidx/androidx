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
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupRestoreTestRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value,
                    PopulateStorageAction.KEY_VALUE_TYPE to "STRING",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // Assert preference was physically seeded
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getString(key, null))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value,
                    PopulateStorageAction.KEY_VALUE_TYPE to "STRING",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value.toString(),
                    PopulateStorageAction.KEY_VALUE_TYPE to "INT",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // Assert preference was physically seeded with correct type
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getInt(key, 0))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value.toString(),
                    PopulateStorageAction.KEY_VALUE_TYPE to "INT",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value.toString(),
                    PopulateStorageAction.KEY_VALUE_TYPE to "BOOLEAN",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // Assert preference was physically seeded with correct type
        val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        assertEquals(value, sharedPrefs.getBoolean(key, false))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    PopulateStorageAction.KEY_VALUE to value.toString(),
                    PopulateStorageAction.KEY_VALUE_TYPE to "BOOLEAN",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_FILES,
                    PopulateStorageAction.KEY_PATH to file.absolutePath,
                    PopulateStorageAction.KEY_VALUE to fileContent,
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // Assert file was physically created
        assertEquals(fileContent, file.readText())

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_FILES,
                    PopulateStorageAction.KEY_PATH to file.absolutePath,
                    PopulateStorageAction.KEY_VALUE to fileContent,
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_DATABASE,
                    PopulateStorageAction.KEY_DB_NAME to dbName,
                    PopulateStorageAction.KEY_TABLE to table,
                    PopulateStorageAction.KEY_VALUES to "$colName=$colVal",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_DATABASE,
                    PopulateStorageAction.KEY_DB_NAME to dbName,
                    PopulateStorageAction.KEY_TABLE to table,
                    AssertStorageAction.KEY_KEY_COL to colName,
                    AssertStorageAction.KEY_KEY_VAL to colVal,
                    PopulateStorageAction.KEY_VALUES to "$colName=$colVal",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_DATABASE,
                    PopulateStorageAction.KEY_DB_NAME to dbName,
                    PopulateStorageAction.KEY_TABLE to table,
                    PopulateStorageAction.KEY_VALUES to "$col1Name=$col1Val&$col2Name=$col2Val",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // 2. Verify using multi-column KEY_VALUES
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_DATABASE,
                    PopulateStorageAction.KEY_DB_NAME to dbName,
                    PopulateStorageAction.KEY_TABLE to table,
                    AssertStorageAction.KEY_KEY_COL to col1Name,
                    AssertStorageAction.KEY_KEY_VAL to col1Val,
                    PopulateStorageAction.KEY_VALUES to "$col1Name=$col1Val&$col2Name=$col2Val",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_FILES,
                    PopulateStorageAction.KEY_PATH to file.absolutePath,
                    PopulateStorageAction.KEY_VALUE to base64Encoded,
                    PopulateStorageAction.KEY_IS_BINARY to "true",
                )
            )
        val putResult = putAction.execute(context, putArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            putResult.payload[PopulateStorageAction.KEY_STATUS],
        )

        // Assert file was physically created with exact bytes
        assertTrue(file.exists())
        assertTrue(binaryData.contentEquals(file.readBytes()))

        // 2. Verify
        val verifyAction = AssertStorageAction()
        val verifyArgs =
            BackupDeviceActionArgs(
                mapOf(
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_FILES,
                    PopulateStorageAction.KEY_PATH to file.absolutePath,
                    PopulateStorageAction.KEY_VALUE to base64Encoded,
                    PopulateStorageAction.KEY_IS_BINARY to "true",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
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
                    PopulateStorageAction.KEY_STORAGE_TYPE to
                        PopulateStorageAction.STORAGE_TYPE_PREFS,
                    PopulateStorageAction.KEY_PREF_NAME to prefName,
                    PopulateStorageAction.KEY_PREF_KEY to key,
                    AssertStorageAction.KEY_EXPECT_NULL to "true",
                )
            )
        val verifyResult = verifyAction.execute(context, verifyArgs)
        assertEquals(
            PopulateStorageAction.STATUS_SUCCESS,
            verifyResult.payload[PopulateStorageAction.KEY_STATUS],
        )
    }
}
