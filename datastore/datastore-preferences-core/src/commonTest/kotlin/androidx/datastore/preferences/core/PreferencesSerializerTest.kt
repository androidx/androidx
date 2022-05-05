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
import androidx.datastore.core.OutputStream
import androidx.datastore.core.InputStream
import androidx.datastore.core.TestFile
import androidx.datastore.core.TestIO
import androidx.datastore.core.kmp.asBufferedSink
import androidx.datastore.core.kmp.asBufferedSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.use


@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class PreferencesSerializerTest {

    private lateinit var testFile: TestFile
    private lateinit var testIO: TestIO
    private lateinit var testScope: TestScope
    private val preferencesSerializer = getSerializer()

    @BeforeTest
    fun setUp() {
        testIO = TestIO()
        testFile = testIO.createTempFile("test")
        testScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    fun testWriteAndReadString() = testScope.runTest {
        val stringKey = stringPreferencesKey("string_key")

        val prefs = preferencesOf(
            stringKey to "string1"
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadStringSet() = testScope.runTest {
        val stringSetKey =
            stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(
            stringSetKey to setOf("string1", "string2", "string3")
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadLong() = testScope.runTest {
        val longKey = longPreferencesKey("long_key")

        val prefs = preferencesOf(
            longKey to (1L shr 50)
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadInt() = testScope.runTest {
        val intKey = intPreferencesKey("int_key")

        val prefs = preferencesOf(
            intKey to 3
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadBoolean() = testScope.runTest {
        val booleanKey = booleanPreferencesKey("boolean_key")

        val prefs = preferencesOf(
            booleanKey to true
        )
        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadFloat() = testScope.runTest {
        val floatKey = floatPreferencesKey("float_key")

        val prefs = preferencesOf(
            floatKey to 3.0f
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadDouble() = testScope.runTest {
        val maxDouble = doublePreferencesKey("max_double_key")
        val minDouble = doublePreferencesKey("min_double_key")

        val prefs = preferencesOf(
            maxDouble to Double.MAX_VALUE,
            minDouble to Double.MIN_VALUE
        )


        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testThrowsCorruptionException() = testScope.runTest {
        // Not a valid proto or json - protos cannot start with a 0 byte. Also invalid JSON.
        testIO.outputStream(testFile).asBufferedSink().use {
            it.write(byteArrayOf(0, 1, 2, 3, 4))
        }

        assertFailsWith<CorruptionException> {
            testIO.inputStream(testFile).use {
                preferencesSerializer.readFrom(it)
            }
        }
    }

    @Test
    fun testWriteAndReadByteArray() = testScope.runTest {
        val byteArrayKey = byteArrayPreferencesKey("byteArray")

        val prefs = preferencesOf(
            byteArrayKey to byteArrayOf(1, 2, 3, 4)
        )

        testIO.outputStream(testFile).use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testIO.inputStream(testFile).use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }
}

private inline fun InputStream.use(block: (InputStream) -> Preferences): Preferences {
    this.asBufferedSource().use {
        return block(this)
    }
}

private inline fun OutputStream.use(block: (OutputStream) -> Unit) {
    this.asBufferedSink().use {
        return block(this)
    }
}
