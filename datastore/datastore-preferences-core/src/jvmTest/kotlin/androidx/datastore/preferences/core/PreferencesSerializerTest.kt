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

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class PreferencesSerializerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private val preferencesSerializer = PreferencesSerializer

    @Before
    fun setUp() {
        testFile = tmp.newFile()
    }

    @Test
    fun testWriteAndReadString() = runTest {
        val stringKey = stringPreferencesKey("string_key")

        val prefs = preferencesOf(
            stringKey to "string1"
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadStringSet() = runTest {
        val stringSetKey =
            stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(
            stringSetKey to setOf("string1", "string2", "string3")
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadLong() = runTest {
        val longKey = longPreferencesKey("long_key")

        val prefs = preferencesOf(
            longKey to (1L shr 50)
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        val intKey = intPreferencesKey("int_key")

        val prefs = preferencesOf(
            intKey to 3
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadBoolean() = runTest {
        val booleanKey = booleanPreferencesKey("boolean_key")

        val prefs = preferencesOf(
            booleanKey to true
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadFloat() = runTest {
        val floatKey = floatPreferencesKey("float_key")

        val prefs = preferencesOf(
            floatKey to 3.0f
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadDouble() = runTest {
        val maxDouble = doublePreferencesKey("max_double_key")
        val minDouble = doublePreferencesKey("min_double_key")

        val prefs = preferencesOf(
            maxDouble to Double.MAX_VALUE,
            minDouble to Double.MIN_VALUE
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testThrowsCorruptionException() = runTest {
        // Not a valid proto - protos cannot start with a 0 byte.
        testFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        assertFailsWith<CorruptionException> {
            testFile.inputStream().use {
                preferencesSerializer.readFrom(it)
            }
        }
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        val byteArrayKey = byteArrayPreferencesKey("byteArray")

        val prefs = preferencesOf(
            byteArrayKey to byteArrayOf(1, 2, 3, 4)
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }
}