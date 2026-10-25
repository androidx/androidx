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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
    fun testDatabase_primaryKeyColumnMatchingPrimaryKeyVal_isAccepted() {
        StorageDomain.Database("db", "users", "id", 42, mapOf("id" to 42, "name" to "Ann"))
        // Values are sent as strings, so an Int and a Long of the same number match.
        StorageDomain.Database("db", "users", "id", 42L, mapOf("ID" to 42))
    }

    @Test
    fun testDatabase_primaryKeyColumnConflictingWithPrimaryKeyVal_throws() {
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                StorageDomain.Database("db", "users", "id", 42, mapOf("id" to 7, "name" to "Ann"))
            }
        assertEquals(
            "Column 'id' is the primary key column, so its value '7' must match " +
                "primaryKeyVal '42'.",
            e.message,
        )
    }

    /** SQLite column names are case-insensitive, so `ID` is the primary key column too. */
    @Test
    fun testDatabase_primaryKeyColumnIsMatchedCaseInsensitively() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Database("db", "users", "id", 42, mapOf("ID" to 7))
        }
    }

    @Test
    fun testDatabase_nullPrimaryKeyColumn_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Database("db", "users", "id", 42, mapOf("id" to null))
        }
    }

    @Test
    fun testDatabase_emptyColumnValues_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Database("db", "users", "id", 42, emptyMap())
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

    @Test
    fun testTextFile_toStringShowsContentUpToTheBound() {
        val atTheBound = "a".repeat(64)

        assertEquals(
            "TextFile(path='notes.txt', content='hello')",
            StorageDomain.TextFile("notes.txt", "hello").toString(),
        )
        assertEquals(
            "TextFile(path='notes.txt', content='$atTheBound')",
            StorageDomain.TextFile("notes.txt", atTheBound).toString(),
        )
    }

    @Test
    fun testTextFile_toStringCutsLongerContent() {
        val content = "a".repeat(64) + "b".repeat(36)

        assertEquals(
            "TextFile(path='big.txt', content='${"a".repeat(64)}...' (100 chars))",
            StorageDomain.TextFile("big.txt", content).toString(),
        )
    }

    @Test
    fun testTextFile_toStringDoesNotCutASurrogatePair() {
        val content = "a".repeat(63) + "\uD83D\uDE00" + "b".repeat(36)

        assertEquals(
            "TextFile(path='big.txt', content='${"a".repeat(63)}...' (101 chars))",
            StorageDomain.TextFile("big.txt", content).toString(),
        )
    }

    @Test
    fun testPreference_toStringShowsAShortValue() {
        assertEquals(
            "Preference(prefName='name', key='key', value=hello)",
            StorageDomain.Preference("name", "key", "hello").toString(),
        )
        assertEquals(
            "Preference(prefName='name', key='key', value=[a, b])",
            StorageDomain.Preference("name", "key", setOf("a", "b")).toString(),
        )
        assertEquals(
            "Preference(prefName='name', key='key', value=null)",
            StorageDomain.Preference("name", "key").toString(),
        )
    }

    @Test
    fun testPreference_toStringCutsALongValue() {
        val value = "a".repeat(64) + "b".repeat(100_000)

        assertEquals(
            "Preference(prefName='name', key='key', value=${"a".repeat(64)}... (100064 chars))",
            StorageDomain.Preference("name", "key", value).toString(),
        )
        assertEquals(
            "Preference(prefName='name', key='key', value=[${"a".repeat(63)}... (100066 chars))",
            StorageDomain.Preference("name", "key", setOf(value)).toString(),
        )
    }

    @Test
    fun testDatabase_toStringCutsEachLongValue() {
        val long = "a".repeat(64) + "b".repeat(100_000)

        assertEquals(
            "Database(dbName='db', table='notes', primaryKeyCol='id', " +
                "primaryKeyVal=${"a".repeat(64)}... (100064 chars), " +
                "columnValues={note=${"a".repeat(64)}... (100064 chars), count=1, tag=null})",
            StorageDomain.Database(
                    "db",
                    "notes",
                    "id",
                    long,
                    linkedMapOf("note" to long, "count" to 1, "tag" to null),
                )
                .toString(),
        )
    }

    @Test
    fun testPreference_acceptsASetOfStrings() {
        val preference = StorageDomain.Preference("name", "key", setOf("a", "b"))

        assertEquals(setOf("a", "b"), preference.value)
        assertEquals(StorageDomain.Preference("name", "key", setOf("b", "a")), preference)
        assertEquals(
            StorageDomain.Preference("name", "key", setOf("b", "a")).hashCode(),
            preference.hashCode(),
        )
    }

    @Test
    fun testPreference_rejectsASetWithAnElementThatIsNotAString() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Preference("name", "key", setOf("a", 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StorageDomain.Preference("name", "key", setOf("a", null))
        }
    }

    /** A preference keeps its own copy of a set, so later changes to the set don't affect it. */
    @Test
    fun testPreference_keepsACopyOfASet() {
        val values = mutableSetOf("a")
        val preference = StorageDomain.Preference("name", "key", values)

        values += "b"

        assertEquals(setOf("a"), preference.value)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST") (preference.value as MutableSet<String>).add("c")
        }
    }

    @Test
    fun testDomainsAreInTheDefaultStorageOfTheAppByDefault() {
        assertEquals(false, StorageDomain.Preference("name", "key", "v").isDeviceProtectedStorage)
        assertEquals(false, database().isDeviceProtectedStorage)
        assertEquals(false, StorageDomain.TextFile("a.txt", "c").isDeviceProtectedStorage)
        assertEquals(
            false,
            StorageDomain.BinaryFile("a.bin", byteArrayOf(1)).isDeviceProtectedStorage,
        )
    }

    @Test
    fun testDomainsInDeviceProtectedStorageDifferFromTheOthers() {
        val pairs =
            listOf(
                StorageDomain.Preference("name", "key", "v") to
                    StorageDomain.Preference("name", "key", "v", isDeviceProtectedStorage = true),
                database() to database(isDeviceProtectedStorage = true),
                StorageDomain.TextFile("a.txt", "c") to
                    StorageDomain.TextFile("a.txt", "c", isDeviceProtectedStorage = true),
                StorageDomain.BinaryFile("a.bin", byteArrayOf(1)) to
                    StorageDomain.BinaryFile(
                        "a.bin",
                        byteArrayOf(1),
                        isDeviceProtectedStorage = true,
                    ),
            )

        for ((inDefaultStorage, deviceProtected) in pairs) {
            assertNotEquals(inDefaultStorage, deviceProtected)
            assertNotEquals(inDefaultStorage.toString(), deviceProtected.toString())
            assertTrue(deviceProtected.toString().endsWith(", isDeviceProtectedStorage=true)"))
            assertFalse(inDefaultStorage.toString().contains("isDeviceProtectedStorage"))
        }
    }

    @Test
    fun testCustomData_equalityAndToString() {
        val action = StorageDomain.CustomData(POPULATE, VERIFY, mapOf("rp_id" to "example.com"))
        val sameAction = StorageDomain.CustomData(POPULATE, VERIFY, mapOf("rp_id" to "example.com"))

        assertEquals(sameAction, action)
        assertEquals(sameAction.hashCode(), action.hashCode())
        assertNotEquals(StorageDomain.CustomData(POPULATE, VERIFY), action)
        assertEquals(
            "CustomData(populateAction='$POPULATE', verifyAction='$VERIFY', " +
                "args={rp_id=example.com})",
            action.toString(),
        )
    }

    @Test
    fun testCustomData_rejectsArgsThatTheRunnerReadsItself() {
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                StorageDomain.CustomData(POPULATE, VERIFY, mapOf("actionClass" to "x"))
            }

        assertEquals(
            "CustomData args must not use the keys [actionClass], which the runner reads itself.",
            e.message,
        )
    }

    /**
     * Custom data and a database keep their own copy of a map, so later changes don't affect it.
     */
    @Test
    fun testMapsOfDomainsAreCopies() {
        val args = mutableMapOf("rp_id" to "example.com")
        val columns = mutableMapOf<String, Any?>("a" to "b")
        val customData = StorageDomain.CustomData(POPULATE, VERIFY, args)
        val database = StorageDomain.Database("db", "table", "id", 1, columns)

        args["user"] = "sarah"
        columns["c"] = "d"

        assertEquals(mapOf("rp_id" to "example.com"), customData.args)
        assertEquals(mapOf("a" to "b"), database.columnValues)
        assertThrows(UnsupportedOperationException::class.java) {
            (customData.args as MutableMap<String, String>)["user"] = "sarah"
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (database.columnValues as MutableMap<String, Any?>)["c"] = "d"
        }
    }

    @Test
    fun testExcludedData_acceptsDataThatCanBeSeeded() {
        for (domain in
            listOf(
                StorageDomain.Preference("name", "key", "v"),
                database(),
                StorageDomain.TextFile("a.txt", "c"),
                StorageDomain.BinaryFile("a.bin", byteArrayOf(1)),
            )) {
            val excluded = StorageDomain.ExcludedData(domain)

            assertEquals(domain, excluded.domain)
            assertEquals(StorageDomain.ExcludedData(domain), excluded)
            assertEquals(StorageDomain.ExcludedData(domain).hashCode(), excluded.hashCode())
            assertEquals("ExcludedData($domain)", excluded.toString())
            assertNotEquals(domain, excluded)
            assertNotEquals(domain.hashCode(), excluded.hashCode())
        }
    }

    /**
     * A preference without a value is seeded by removing it, and custom data is checked by its own
     * action itself, so neither can be expected to be absent.
     */
    @Test
    fun testExcludedData_rejectsDataThatItCannotExpectAbsent() {
        for (domain in
            listOf(
                StorageDomain.Preference("name", "key"),
                StorageDomain.CustomData(POPULATE, VERIFY),
                StorageDomain.ExcludedData(StorageDomain.TextFile("a.txt", "c")),
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                StorageDomain.ExcludedData(domain)
            }
        }
    }

    private fun database(isDeviceProtectedStorage: Boolean = false) =
        StorageDomain.Database("db", "table", "id", 1, mapOf("a" to "b"), isDeviceProtectedStorage)

    private companion object {
        const val POPULATE = "com.example.backup.CreateKeyAction"
        const val VERIFY = "com.example.backup.VerifyKeyAction"
    }
}
