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

package androidx.datastore.preferences

import androidx.datastore.CorruptionException
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
    fun testWriteAndReadString() {
        val stringKey = "string_key"

        val prefs = Preferences.Builder()
            .setString(stringKey, "string1")
            .build()

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertThat(readPrefs).isEqualTo(prefs)
    }

    @Test
    fun testWriteAndReadStringSet() {
        val stringSetKey = "string_set_key"

        val prefs = Preferences.Builder()
            .setStringSet(stringSetKey, setOf("string1", "string2", "string3"))
            .build()

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertThat(readPrefs).isEqualTo(prefs)
    }

    @Test
    fun testWriteAndReadLong() {
        val longKey = "long_key"

        val prefs = Preferences.Builder()
            .setLong(longKey, 1 shr 50)
            .build()

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertThat(readPrefs).isEqualTo(prefs)
    }

    @Test
    fun testWriteAndReadInt() {
        val intKey = "int_key"

        val prefs = Preferences.Builder()
            .setInt(intKey, 3)
            .build()

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertThat(readPrefs).isEqualTo(prefs)
    }

    @Test
    fun testWriteAndReadBoolean() {
        val booleanKey = "boolean_key"

        val prefs = Preferences.Builder()
            .setBoolean(booleanKey, true)
            .build()

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertThat(readPrefs).isEqualTo(prefs)
    }

    @Test
    fun testThrowsCorruptionException() {
        // Not a valid proto - protos cannot start with a 0 byte.
        testFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        assertThrows<CorruptionException> {
            testFile.inputStream().use {
                preferencesSerializer.readFrom(it)
            }
        }
    }
}