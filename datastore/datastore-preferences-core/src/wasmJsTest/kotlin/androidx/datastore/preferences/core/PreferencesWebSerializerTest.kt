/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.WebStorage
import androidx.datastore.core.okio.WebStorageType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

// TODO(b/441511612): Add more testing once LocalStorage and OPFS is supported.
class PreferencesWebSerializerTest {
    private val testSessionStorageName = "test_session_storage"
    private lateinit var sessionDataStore: DataStore<Preferences>
    private val preferencesSerializer: OkioSerializer<Preferences> = PreferencesSerializer

    private val floatKey = floatPreferencesKey("float_key")
    private val intKey = intPreferencesKey("int_key")
    private val booleanKey = booleanPreferencesKey("boolean_key")
    private val doubleKey = doublePreferencesKey("double_key")
    private val stringKey = stringPreferencesKey("string_key")
    private val stringSetKey = stringSetPreferencesKey("string_set_key")
    private val longKey = longPreferencesKey("long_key")
    private val byteArrayKey = byteArrayPreferencesKey("byte_array_key")

    private val floatActual = 3.14f
    private val intActual = 123
    private val booleanActual = false
    private val doubleActual = 3.1415
    private val stringActual = "abc"
    private val stringSetActual = setOf("1", "2", "3")
    private val longActual = 10000000000L
    private val byteArrayActual = byteArrayOf(1, 2, 3, 4)

    @BeforeTest
    fun setUp() {
        val sessionWebStorage =
            WebStorage(
                name = testSessionStorageName,
                serializer = preferencesSerializer,
                storageType = WebStorageType.SESSION,
            )
        sessionDataStore = DataStoreFactory.create(sessionWebStorage)
    }

    @AfterTest
    fun tearDown() {
        sessionStorage.removeItem(testSessionStorageName)
    }

    @Test
    fun testWriteAndReadString() = runTest {
        val dataToWrite = preferencesOf(stringKey to stringActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadStringSet() = runTest {
        val dataToWrite = preferencesOf(stringSetKey to stringSetActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadLong() = runTest {
        val dataToWrite = preferencesOf(longKey to longActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        val dataToWrite = preferencesOf(intKey to intActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadBoolean() = runTest {
        val dataToWrite = preferencesOf(booleanKey to booleanActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadFloat() = runTest {
        val dataToWrite = preferencesOf(floatKey to floatActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadDouble() = runTest {
        val dataToWrite = preferencesOf(doubleKey to doubleActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        val dataToWrite = preferencesOf(byteArrayKey to byteArrayActual)
        sessionDataStore.updateData { dataToWrite }
        val readData = sessionDataStore.data.first()
        assertEquals(dataToWrite, readData)
    }
}
