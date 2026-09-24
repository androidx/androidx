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
    fun testBackupDeviceActionArgsGet() {
        val args =
            BackupDeviceActionArgs(
                mapOf(BackupActionInputKeys.PREF_NAME to "prefs", "unrelated" to "ignored")
            )

        assertEquals("prefs", args[BackupActionInputKeys.PREF_NAME])
        assertNull(args[BackupActionInputKeys.PREF_KEY])
    }

    @Test
    fun testBackupDeviceActionResultPayload() {
        val payload =
            mapOf(
                BackupActionOutputKeys.STATUS to BackupActionValues.STATUS_SUCCESS,
                "custom" to "result",
            )
        val result = BackupDeviceActionResult(payload)

        assertEquals(2, result.payload.size)
        assertEquals(
            BackupActionValues.STATUS_SUCCESS,
            result.payload[BackupActionOutputKeys.STATUS],
        )
        assertEquals("result", result.payload["custom"])
        assertTrue(result.payload.containsKey(BackupActionOutputKeys.STATUS))
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
    fun testSuccessFactory() {
        val result = BackupDeviceActionResult.success()

        assertEquals(BackupActionValues.STATUS_SUCCESS, result.status)
        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
        assertEquals(1, result.payload.size)
    }

    @Test
    fun testSuccessFactoryCarriesExtraDataAndOwnsStatus() {
        val result =
            BackupDeviceActionResult.success(
                mapOf(
                    "rows" to "3",
                    BackupActionOutputKeys.STATUS to BackupActionValues.STATUS_FAILURE,
                )
            )

        assertEquals("3", result["rows"])
        assertEquals(BackupActionValues.STATUS_SUCCESS, result.status)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFailureFactory() {
        val result = BackupDeviceActionResult.failure("Expected 'a' but found 'b'")

        assertEquals(BackupActionValues.STATUS_FAILURE, result.status)
        assertFalse(result.isSuccess)
        assertEquals("Expected 'a' but found 'b'", result.errorMessage)
        assertEquals("Expected 'a' but found 'b'", result[BackupActionOutputKeys.ERROR])
    }

    @Test
    fun testFailureFactoryCarriesExtraData() {
        val result = BackupDeviceActionResult.failure("boom", mapOf("rows" to "0"))

        assertEquals("0", result["rows"])
        assertFalse(result.isSuccess)
        assertEquals("boom", result.errorMessage)
    }

    /** An action that does not report a status is treated as successful. */
    @Test
    fun testIsSuccessDefaultsToTrueWithoutStatus() {
        val result = BackupDeviceActionResult(mapOf("rows" to "1"))

        assertNull(result.status)
        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
    }

    @Test
    fun testIsSuccessIgnoresStatusCase() {
        assertTrue(BackupDeviceActionResult(mapOf("status" to "SUCCESS")).isSuccess)
        assertFalse(BackupDeviceActionResult(mapOf("status" to "FAILURE")).isSuccess)
    }

    /**
     * `status` is a generic key that custom actions use for their own vocabulary, so only the
     * recognized failure value marks a result as failed.
     *
     * A real custom action in a sample app reports `status=verified` to mean "all checks passed";
     * treating any non-success value as a failure broke it.
     */
    @Test
    fun testIsSuccessAcceptsUnrecognizedStatus() {
        assertTrue(BackupDeviceActionResult(mapOf("status" to "partial")).isSuccess)
        assertTrue(BackupDeviceActionResult(mapOf("status" to "verified")).isSuccess)
    }

    /**
     * [BackupDeviceActionResult.errorMessage] is reported only for a failed result.
     *
     * A successful action may carry its own `error` entry — the AddressBook sample returns an empty
     * `restore_credential_error` alongside a passing verification — and reporting that as the
     * failure message would be misleading.
     */
    @Test
    fun testErrorMessageIsNullForSuccessfulResult() {
        assertNull(BackupDeviceActionResult.success(mapOf("error" to "not a failure")).errorMessage)
        assertNull(BackupDeviceActionResult(mapOf("error" to "")).errorMessage)
        assertNull(
            BackupDeviceActionResult(mapOf("status" to "verified", "error" to "")).errorMessage
        )
    }

    @Test
    fun testErrorMessageIsReportedForFailedResult() {
        assertEquals("boom", BackupDeviceActionResult.failure("boom").errorMessage)
        assertEquals(
            "boom",
            BackupDeviceActionResult(mapOf("status" to "failure", "error" to "boom")).errorMessage,
        )
    }

    /**
     * An action that reports an error but forgets the status is still a failure.
     *
     * Hand-built payloads are legal — [BackupDeviceActionResult] takes a raw map — so a custom
     * action that only sets `error` must not be read as passing.
     */
    @Test
    fun testIsSuccessFailsOnErrorWithoutStatus() {
        val result = BackupDeviceActionResult(mapOf("error" to "Disk full"))

        assertNull(result.status)
        assertFalse(result.isSuccess)
        assertEquals("Disk full", result.errorMessage)
    }

    /**
     * An empty `error` without a status is not a failure.
     *
     * The AddressBook sample returns an empty `restore_credential_error` alongside a passing
     * verification, so the presence of the key alone cannot decide the outcome.
     */
    @Test
    fun testIsSuccessIgnoresEmptyErrorWithoutStatus() {
        val result = BackupDeviceActionResult(mapOf("error" to ""))

        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
    }

    /**
     * A reported status wins over an `error` entry.
     *
     * Only the recognized failure value marks a result as failed, so a success that carries a
     * diagnostic `error` of its own stays a success.
     */
    @Test
    fun testStatusOverridesErrorEntry() {
        assertTrue(
            BackupDeviceActionResult(mapOf("status" to "success", "error" to "stale")).isSuccess
        )
        assertTrue(
            BackupDeviceActionResult(mapOf("status" to "verified", "error" to "stale")).isSuccess
        )
    }

    @Test
    fun testActionPhaseConstants() {
        assertEquals(1, BackupDeviceAction.PHASE_POPULATE)
        assertEquals(2, BackupDeviceAction.PHASE_VERIFY)
        assertEquals(BackupDeviceAction.PHASE_POPULATE, BackupActionPhase.POPULATE)
        assertEquals(BackupDeviceAction.PHASE_VERIFY, BackupActionPhase.VERIFY)
    }
}
