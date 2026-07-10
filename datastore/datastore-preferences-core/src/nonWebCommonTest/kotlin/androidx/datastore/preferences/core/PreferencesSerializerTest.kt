/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.preferences.core

import androidx.datastore.OkioPath
import androidx.datastore.OkioTestIO
import androidx.datastore.core.okio.OkioSerializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.SYSTEM

@OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    kotlinx.coroutines.ObsoleteCoroutinesApi::class,
    kotlinx.coroutines.FlowPreview::class,
)
class PreferencesSerializerTest {

    private val testIO = OkioTestIO()

    private lateinit var testFile: OkioPath
    private val preferencesSerializer: OkioSerializer<Preferences> = PreferencesSerializer
    private val fileSystem: FileSystem = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        testFile = testIO.newTempFile()
        fileSystem.createDirectories(testFile.path.parent!!)
    }

    fun doTest(test: suspend TestScope.() -> Unit) {
        runTest(timeout = 10000.milliseconds) { test(this) }
    }

    @Test
    fun testWriteAndReadString() = doTest {
        val stringKey = stringPreferencesKey("string_key")

        val prefs = preferencesOf(stringKey to "string1")

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadStringSet() = doTest {
        val stringSetKey = stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(stringSetKey to setOf("string1", "string2", "string3"))

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadLong() = doTest {
        val longKey = longPreferencesKey("long_key")

        val prefs = preferencesOf(longKey to (1L shr 50))

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadInt() = doTest {
        val intKey = intPreferencesKey("int_key")

        val prefs = preferencesOf(intKey to 3)

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadBoolean() = doTest {
        val booleanKey = booleanPreferencesKey("boolean_key")

        val prefs = preferencesOf(booleanKey to true)

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadFloat() = doTest {
        val floatKey = floatPreferencesKey("float_key")

        val prefs = preferencesOf(floatKey to 3.0f)

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadDouble() = doTest {
        val maxDouble = doublePreferencesKey("max_double_key")
        val minDouble = doublePreferencesKey("min_double_key")

        val prefs = preferencesOf(maxDouble to Double.MAX_VALUE, minDouble to Double.MIN_VALUE)

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadByteArray() = doTest {
        val byteArrayKey = byteArrayPreferencesKey("byteArray")

        val prefs = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))

        fileSystem.write(testFile.path) { preferencesSerializer.writeTo(prefs, this) }

        val readPrefs = fileSystem.read(testFile.path) { preferencesSerializer.readFrom(this) }

        assertEquals(prefs, readPrefs)
    }
}
