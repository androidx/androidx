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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDeviceActionTest {

    @Test
    fun testBackupDeviceActionArgsPayload() {
        val payload = mapOf("key1" to "val1", "key2" to "val2")
        val args = BackupDeviceActionArgs(payload)

        assertEquals(2, args.payload.size)
        assertEquals("val1", args.payload["key1"])
        assertEquals("val2", args.payload["key2"])
        assertTrue(args.payload.containsKey("key1"))
        assertFalse(args.payload.containsKey("key3"))
        assertNull(args.payload["key3"])
    }

    @Test
    fun testBackupDeviceActionArgsEquality() {
        val args1 = BackupDeviceActionArgs(mapOf("a" to "b"))
        val args2 = BackupDeviceActionArgs(mapOf("a" to "b"))
        val args3 = BackupDeviceActionArgs(mapOf("a" to "c"))

        assertEquals(args1, args2)
        assertEquals(args1.hashCode(), args2.hashCode())
        assertNotEquals(args1, args3)
        assertEquals("BackupDeviceActionArgs(payload={a=b})", args1.toString())
    }

    @Test
    fun testBackupDeviceActionResultPayload() {
        val payload =
            mapOf(
                BackupDeviceAction.KEY_STATUS to BackupDeviceAction.STATUS_SUCCESS,
                "custom" to "result",
            )
        val result = BackupDeviceActionResult(payload)

        assertEquals(2, result.payload.size)
        assertEquals(
            BackupDeviceAction.STATUS_SUCCESS,
            result.payload[BackupDeviceAction.KEY_STATUS],
        )
        assertEquals("result", result.payload["custom"])
        assertTrue(result.payload.containsKey(BackupDeviceAction.KEY_STATUS))
    }

    @Test
    fun testBackupDeviceActionResultEquality() {
        val res1 = BackupDeviceActionResult(mapOf("status" to "success"))
        val res2 = BackupDeviceActionResult(mapOf("status" to "success"))
        val res3 = BackupDeviceActionResult(mapOf("status" to "failure"))

        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
        assertNotEquals(res1, res3)
        assertEquals("BackupDeviceActionResult(payload={status=success})", res1.toString())
    }

    @Test
    fun testActionPhaseConstants() {
        assertEquals(1, BackupDeviceAction.PHASE_POPULATE)
        assertEquals(2, BackupDeviceAction.PHASE_VERIFY)
        assertEquals(BackupDeviceAction.PHASE_POPULATE, ActionPhase.POPULATE)
        assertEquals(BackupDeviceAction.PHASE_VERIFY, ActionPhase.VERIFY)
    }

    @Test
    fun testBackupDeviceActionConstants() {
        assertEquals("storage_type", BackupDeviceAction.KEY_STORAGE_TYPE)
        assertEquals("PREFS", BackupDeviceAction.STORAGE_TYPE_PREFS)
        assertEquals("DATABASE", BackupDeviceAction.STORAGE_TYPE_DATABASE)
        assertEquals("FILES", BackupDeviceAction.STORAGE_TYPE_FILES)
        assertEquals("pref_name", BackupDeviceAction.KEY_PREF_NAME)
        assertEquals("pref_key", BackupDeviceAction.KEY_PREF_KEY)
        assertEquals("value", BackupDeviceAction.KEY_VALUE)
        assertEquals("value_type", BackupDeviceAction.KEY_VALUE_TYPE)
        assertEquals("db_name", BackupDeviceAction.KEY_DB_NAME)
        assertEquals("table", BackupDeviceAction.KEY_TABLE)
        assertEquals("values", BackupDeviceAction.KEY_VALUES)
        assertEquals("path", BackupDeviceAction.KEY_PATH)
        assertEquals("is_binary", BackupDeviceAction.KEY_IS_BINARY)
        assertEquals("is_device_protected", BackupDeviceAction.KEY_IS_DEVICE_PROTECTED)
        assertEquals("key_col", BackupDeviceAction.KEY_KEY_COL)
        assertEquals("key_val", BackupDeviceAction.KEY_KEY_VAL)
        assertEquals("expected_col", BackupDeviceAction.KEY_EXPECTED_COL)
        assertEquals("expected_val", BackupDeviceAction.KEY_EXPECTED_VAL)
        assertEquals("expected", BackupDeviceAction.KEY_EXPECTED)
        assertEquals("expect_null", BackupDeviceAction.KEY_EXPECT_NULL)
        assertEquals("status", BackupDeviceAction.KEY_STATUS)
        assertEquals("success", BackupDeviceAction.STATUS_SUCCESS)
        assertEquals("failure", BackupDeviceAction.STATUS_FAILURE)
        assertEquals("error", BackupDeviceAction.KEY_ERROR)
    }
}
