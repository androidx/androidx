/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class PreferencesSerializerJavaTest {

    private val testIO = OkioTestIO()

    private lateinit var testFile: OkioPath
    private val preferencesSerializer: OkioSerializer<Preferences> = PreferencesSerializer
    private val fileSystem: FileSystem = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        testFile = testIO.newTempFile()
    }
    fun doTest(test: suspend TestScope.() -> Unit) {
        runTest(timeout = 10000.milliseconds) {
            test(this)
        }
    }

    @Test
    fun testThrowsCorruptionException() = doTest {
        // Not a valid proto - protos cannot start with a 0 byte.
        fileSystem.write(testFile.path) {
            this.write(byteArrayOf(0, 1, 2, 3, 4))
        }

        assertFailsWith<CorruptionException> {
            fileSystem.read(testFile.path) {
                preferencesSerializer.readFrom(this)
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testGetAllCantMutateInternalState() {
        val intKey = intPreferencesKey("int_key")
        val stringSetKey =
            stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(
            intKey to 123,
            stringSetKey to setOf("1", "2", "3")
        )

        val mutableAllPreferences = prefs.asMap() as MutableMap
        assertFailsWith<UnsupportedOperationException> {
            mutableAllPreferences[intKey] = 99999
        }
        assertFailsWith<UnsupportedOperationException> {
            (mutableAllPreferences[stringSetKey] as MutableSet<String>).clear()
        }

        assertEquals(123, prefs[intKey])
        assertEquals(setOf("1", "2", "3"), prefs[stringSetKey])
    }

    @Test
    fun testModifyingStringSetDoesntModifyInternalState() {
        val stringSetKey =
            stringSetPreferencesKey("string_set_key")

        val stringSet = mutableSetOf("1", "2", "3")

        val prefs = preferencesOf(stringSetKey to stringSet)

        stringSet.add("4") // modify the set passed into preferences

        // modify the returned set.
        val returnedSet: Set<String> = prefs[stringSetKey]!!
        val mutableReturnedSet: MutableSet<String> = returnedSet as MutableSet<String>

        assertFailsWith<UnsupportedOperationException> {
            mutableReturnedSet.clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            mutableReturnedSet.add("Original set does not contain this string")
        }

        assertEquals(setOf("1", "2", "3"), prefs[stringSetKey])
    }

    // TODO: This doesn't pass on native: https://youtrack.jetbrains.com/issue/KT-42903
    @Test
    @Suppress("UNUSED_VARIABLE")
    fun testWrongTypeThrowsClassCastException() {
        val stringKey = stringPreferencesKey("string_key")
        val intKey =
            intPreferencesKey("string_key") // long key of the same name as stringKey!
        val longKey = longPreferencesKey("string_key")

        val prefs = preferencesOf(intKey to 123456)

        assertTrue { prefs.contains(intKey) }
        assertTrue { prefs.contains(stringKey) } // TODO: I don't think we can prevent this

        // Trying to get a long where there is an Int value throws a ClassCastException.
        assertFailsWith<ClassCastException> {
            var unused = prefs[stringKey] // This only throws if it's assigned to a
            // variable
        }

        // Trying to get a Long where there is an Int value throws a ClassCastException.
        assertFailsWith<ClassCastException> {
            var unused = prefs[longKey] // This only throws if it's assigned to a
            // variable
        }
    }
}
