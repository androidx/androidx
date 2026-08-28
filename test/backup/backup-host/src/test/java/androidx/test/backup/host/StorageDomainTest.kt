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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Test

class StorageDomainTest {

    @Test
    fun testPreference_validTypes() {
        // Valid SharedPreferences types should not throw
        StorageDomain.Preference("name", "key", "stringValue")
        StorageDomain.Preference("name", "key", 123)
        StorageDomain.Preference("name", "key", 123L)
        StorageDomain.Preference("name", "key", 1.23f)
        StorageDomain.Preference("name", "key", true)
        StorageDomain.Preference("name", "key", null)
    }

    @Test
    fun testPreference_doubleType_throws() {
        // SharedPreferences does not support Double, so it should throw
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Preference("name", "key", 1.23)
        }
    }

    @Test
    fun testPreference_invalidType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Preference("name", "key", listOf("invalid"))
        }
    }

    @Test
    fun testDatabase_validTypes() {
        // SQLite/Room supports both Float and Double
        StorageDomain.Database(
            "db",
            "table",
            "pk_col",
            "pk_val",
            mapOf("col1" to 123, "col2" to true, "col3" to null, "col4" to 1.23),
        )
    }

    @Test
    fun testDatabase_invalidPrimaryKey_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Database("db", "table", "pk_col", listOf("invalid"), mapOf("col1" to 123))
        }
    }

    @Test
    fun testDatabase_invalidColumnValue_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Database(
                "db",
                "table",
                "pk_col",
                123,
                mapOf("col1" to mapOf("nested" to "invalid")),
            )
        }
    }

    @Test
    fun testBinaryFile_immutabilityCloning() {
        val originalBytes = byteArrayOf(1, 2, 3)
        val binaryFile = StorageDomain.BinaryFile("files/data.bin", originalBytes)

        // Verify array contents match
        assertArrayEquals(originalBytes, binaryFile.content)

        // Verify defensive cloning prevents external mutation via getter
        val returnedBytes = binaryFile.content
        assertNotSame(originalBytes, returnedBytes)

        returnedBytes[0] = 99
        assertEquals(1.toByte(), binaryFile.content[0]) // Internal array remains unchanged
    }
}
