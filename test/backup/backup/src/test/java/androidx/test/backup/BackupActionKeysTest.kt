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

package androidx.test.backup

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the device half of the action wire protocol to its literal strings.
 *
 * The host library `androidx.test.backup.host` is a Kotlin/JVM artifact and cannot link against
 * these constants, so it keeps a mirror in `BackupActionWireProtocol` guarded by an equivalent
 * test. Renaming a constant is free; changing the string it holds breaks every host that has
 * already shipped, so both tests must be updated deliberately and together.
 */
class BackupActionKeysTest {

    @Test
    fun inputKeysMatchTheWireProtocol() {
        assertEquals("storage_type", BackupActionInputKeys.STORAGE_TYPE)
        assertEquals("is_device_protected", BackupActionInputKeys.IS_DEVICE_PROTECTED)
        assertEquals("pref_name", BackupActionInputKeys.PREF_NAME)
        assertEquals("pref_key", BackupActionInputKeys.PREF_KEY)
        assertEquals("value", BackupActionInputKeys.VALUE)
        assertEquals("value_type", BackupActionInputKeys.VALUE_TYPE)
        assertEquals("db_name", BackupActionInputKeys.DB_NAME)
        assertEquals("table", BackupActionInputKeys.TABLE)
        assertEquals("values", BackupActionInputKeys.VALUES)
        assertEquals("key_col", BackupActionInputKeys.KEY_COL)
        assertEquals("key_val", BackupActionInputKeys.KEY_VAL)
        assertEquals("path", BackupActionInputKeys.PATH)
        assertEquals("is_binary", BackupActionInputKeys.IS_BINARY)
        assertEquals("expected", BackupActionInputKeys.EXPECTED)
        assertEquals("expected_col", BackupActionInputKeys.EXPECTED_COL)
        assertEquals("expected_val", BackupActionInputKeys.EXPECTED_VAL)
        assertEquals("expect_null", BackupActionInputKeys.EXPECT_NULL)
    }

    @Test
    fun outputKeysMatchTheWireProtocol() {
        assertEquals("status", BackupActionOutputKeys.STATUS)
        assertEquals("error", BackupActionOutputKeys.ERROR)
    }

    @Test
    fun valuesMatchTheWireProtocol() {
        assertEquals("PREFS", BackupActionValues.STORAGE_TYPE_PREFS)
        assertEquals("DATABASE", BackupActionValues.STORAGE_TYPE_DATABASE)
        assertEquals("FILES", BackupActionValues.STORAGE_TYPE_FILES)
        assertEquals("INT", BackupActionValues.VALUE_TYPE_INT)
        assertEquals("LONG", BackupActionValues.VALUE_TYPE_LONG)
        assertEquals("FLOAT", BackupActionValues.VALUE_TYPE_FLOAT)
        assertEquals("BOOLEAN", BackupActionValues.VALUE_TYPE_BOOLEAN)
        assertEquals("STRING", BackupActionValues.VALUE_TYPE_STRING)
        assertEquals("success", BackupActionValues.STATUS_SUCCESS)
        assertEquals("failure", BackupActionValues.STATUS_FAILURE)
        assertEquals("default_prefs", BackupActionValues.DEFAULT_PREF_NAME)
    }

    /** Input and output vocabularies are disjoint, so a key is never ambiguous. */
    @Test
    fun inputAndOutputKeysDoNotOverlap() {
        val inputKeys =
            setOf(
                BackupActionInputKeys.STORAGE_TYPE,
                BackupActionInputKeys.IS_DEVICE_PROTECTED,
                BackupActionInputKeys.PREF_NAME,
                BackupActionInputKeys.PREF_KEY,
                BackupActionInputKeys.VALUE,
                BackupActionInputKeys.VALUE_TYPE,
                BackupActionInputKeys.DB_NAME,
                BackupActionInputKeys.TABLE,
                BackupActionInputKeys.VALUES,
                BackupActionInputKeys.KEY_COL,
                BackupActionInputKeys.KEY_VAL,
                BackupActionInputKeys.PATH,
                BackupActionInputKeys.IS_BINARY,
                BackupActionInputKeys.EXPECTED,
                BackupActionInputKeys.EXPECTED_COL,
                BackupActionInputKeys.EXPECTED_VAL,
                BackupActionInputKeys.EXPECT_NULL,
            )
        assertEquals(17, inputKeys.size)
        val outputKeys = setOf(BackupActionOutputKeys.STATUS, BackupActionOutputKeys.ERROR)
        assertEquals(emptySet<String>(), inputKeys intersect outputKeys)
    }
}
