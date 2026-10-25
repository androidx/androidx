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

import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesProtoCodecTest {
    @get:Rule val folder = TemporaryFolder()

    /**
     * A Preferences DataStore file with one preference of each type, as `protoc --encode` writes it
     * from the `PreferenceMap` message of DataStore's `preferences.proto`.
     */
    private val dataStoreFile =
        ("0a0d0a07626f6f6c65616e120208000a0e0a05666c6f6174120515000020400a120a03696e74120b18d6ff" +
                "ffffffffffffff010a0e0a046c6f6e67120620eaadc0e5240a120a06737472696e6712082a0668c3a96c" +
                "6c6f0a0f0a03736574120832060a01610a01620a130a06646f75626c65120939182d4454fb2109400a0e" +
                "0a0562797465731205420300807f")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    @Test
    fun readsAndWritesTheFileFormatOfPreferencesDataStore() {
        val file = File(folder.root, "datastore/prefs.preferences_pb")
        file.parentFile!!.mkdirs()
        file.writeBytes(dataStoreFile)

        val preferences = readPreferencesDataStore(file)

        assertEquals(false, preferences["boolean"])
        assertEquals(2.5f, preferences["float"])
        assertEquals(-42, preferences["int"])
        assertEquals(9876543210L, preferences["long"])
        assertEquals("héllo", preferences["string"])
        assertEquals(setOf("a", "b"), preferences["set"])
        assertEquals(3.141592653589793, preferences["double"])
        assertArrayEquals(byteArrayOf(0, -128, 127), preferences["bytes"] as ByteArray)
        writePreferencesDataStore(file, preferences)
        assertArrayEquals(dataStoreFile, file.readBytes())
    }

    @Test
    fun rejectsACorruptFileWithAnIOException() {
        val file = File(folder.root, "corrupt.preferences_pb")
        for (content in
            listOf(
                dataStoreFile.copyOf(dataStoreFile.size - 1),
                // A field that claims Int.MAX_VALUE bytes, which must fail before any allocation.
                byteArrayOf(0x0a, -1, -1, -1, -1, 0x07),
            )) {
            file.writeBytes(content)

            assertThrows(IOException::class.java) { readPreferencesDataStore(file) }
        }
    }
}
