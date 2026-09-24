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

package androidx.test.backup.host

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the host half of the action wire protocol to its literal strings.
 *
 * This host library is a Kotlin/JVM artifact and cannot link against the device-side
 * `androidx.test.backup.BackupActionInputKeys`, so the two copies are kept honest by this test and
 * its device-side twin, `BackupActionKeysTest`. If one of them is edited alone, the build fails
 * rather than a device run silently receiving arguments it does not recognize.
 */
class BackupActionWireProtocolTest {

    @Test
    fun inputKeysMatchTheDeviceLibrary() {
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
    fun outputKeysMatchTheDeviceLibrary() {
        assertEquals("status", BackupActionOutputKeys.STATUS)
        assertEquals("error", BackupActionOutputKeys.ERROR)
    }

    @Test
    fun valuesMatchTheDeviceLibrary() {
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
    }
}
