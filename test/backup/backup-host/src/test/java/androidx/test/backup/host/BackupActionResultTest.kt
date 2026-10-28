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

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupActionResultTest {

    @Test
    fun testSuccessDefaultConstructor() {
        val success = BackupActionResult.Success()
        assertTrue(success.isSuccess)
        assertTrue(success.data.isEmpty())
        assertEquals("Success(data={})", success.toString())
    }

    @Test
    fun testSuccessCustomData() {
        val map = mapOf("key1" to "val1", "key2" to "val2")
        val success = BackupActionResult.Success(map)
        assertTrue(success.isSuccess)
        assertEquals(map, success.data)
        assertEquals("val1", success.data["key1"])
        assertEquals("Success(data=$map)", success.toString())
    }

    @Test
    fun testSuccessEqualityAndHashCode() {
        val success1 = BackupActionResult.Success(mapOf("a" to "b"))
        val success2 = BackupActionResult.Success(mapOf("a" to "b"))
        val success3 = BackupActionResult.Success(mapOf("x" to "y"))

        assertEquals(success1, success2)
        assertNotEquals(success1, success3)
        assertEquals(success1.hashCode(), success2.hashCode())
        assertNotEquals(success1.hashCode(), success3.hashCode())
    }

    @Test
    fun testFailureConstructor() {
        val failure = BackupActionResult.Failure("An error occurred")
        assertFalse(failure.isSuccess)
        assertEquals("An error occurred", failure.errorMessage)
        assertNull(failure.stackTrace)
        assertTrue(failure.data.isEmpty())
        assertEquals(
            "Failure(errorMessage=An error occurred, stackTrace=null, data={})",
            failure.toString(),
        )
    }

    @Test
    fun testFailureWithStackTrace() {
        val failure = BackupActionResult.Failure("Crash", "at some.Method(File.kt:12)")
        assertFalse(failure.isSuccess)
        assertEquals("Crash", failure.errorMessage)
        assertEquals("at some.Method(File.kt:12)", failure.stackTrace)
        assertEquals(
            "Failure(errorMessage=Crash, stackTrace=at some.Method(File.kt:12), data={})",
            failure.toString(),
        )
    }

    @Test
    fun testFailureKeepsTheDataOfTheAction() {
        val failure =
            BackupActionResult.Failure("Wrong key", data = mapOf("credential_id" to "abc"))

        assertEquals(mapOf("credential_id" to "abc"), failure.data)
        assertEquals(
            "Failure(errorMessage=Wrong key, stackTrace=null, data={credential_id=abc})",
            failure.toString(),
        )
    }

    @Test
    fun testFailureEqualityAndHashCode() {
        val failure1 = BackupActionResult.Failure("Fail", "trace")
        val failure2 = BackupActionResult.Failure("Fail", "trace")
        val failure3 = BackupActionResult.Failure("Fail", null)
        val failure4 = BackupActionResult.Failure("Crash", "trace")
        val failure5 = BackupActionResult.Failure("Fail", "trace", mapOf("a" to "b"))

        assertEquals(failure1, failure2)
        assertNotEquals(failure1, failure3)
        assertNotEquals(failure1, failure4)
        assertNotEquals(failure1, failure5)
        assertEquals(failure1.hashCode(), failure2.hashCode())
        assertNotEquals(failure1.hashCode(), failure3.hashCode())
        assertNotEquals(failure1.hashCode(), failure4.hashCode())
        assertNotEquals(failure1.hashCode(), failure5.hashCode())
    }

    @Test
    fun testResultsKeepAnUnmodifiableCopyOfTheirData() {
        val data = mutableMapOf("key" to "value")
        val results =
            listOf(
                BackupActionResult.Success(data),
                BackupActionResult.Failure("Fail", data = data),
            )

        data["other"] = "value"

        for (result in results) {
            assertEquals(mapOf("key" to "value"), result.data)
            assertThrows(UnsupportedOperationException::class.java) {
                (result.data as MutableMap<String, String>)["other"] = "value"
            }
        }
    }

    @Test
    fun testGetOrThrowReturnsTheDataOfASuccess() {
        assertEquals(
            mapOf("id" to "1"),
            BackupActionResult.Success(mapOf("id" to "1")).getOrThrow(),
        )
    }

    @Test
    fun testGetOrThrowThrowsTheErrorMessageAndStackTraceOfAFailure() {
        fun messageOf(failure: BackupActionResult.Failure) =
            assertThrows(IOException::class.java) { failure.getOrThrow() }.message

        assertEquals("Wrong key", messageOf(BackupActionResult.Failure("Wrong key")))
        // The runner reports a blank stack trace for an action that returned its failure.
        assertEquals("Wrong key", messageOf(BackupActionResult.Failure("Wrong key", " \n")))
        assertEquals(
            "Crash\nat some.Method(File.kt:12)",
            messageOf(BackupActionResult.Failure("Crash", "at some.Method(File.kt:12)")),
        )
    }
}
