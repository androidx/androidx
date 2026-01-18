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

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.WebStorage
import androidx.datastore.core.okio.WebStorageType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

private val preferencesSerializer: OkioSerializer<Preferences> = PreferencesSerializer

// TODO(b/441511612): Add more testing once OPFS is supported.
class PreferencesSerializerWebTest {
    private val testSessionStorageName = "test_session_storage"
    private val testLocalStorageName = "test_local_storage"

    @AfterTest
    fun tearDown() {
        sessionStorage.removeItem(testSessionStorageName)
        localStorage.removeItem(testLocalStorageName)
    }

    @Test
    fun testSessionStorage_writeThenRead() = runTest {
        val storage =
            WebStorage(
                name = testSessionStorageName,
                serializer = preferencesSerializer,
                storageType = WebStorageType.SESSION,
            )
        val dataStore = DataStoreFactory.create(storage)
        val floatKey = floatPreferencesKey("float_key")
        val dataToWrite = preferencesOf(floatKey to 120f)

        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testLocalStorage_writeThenRead() = runTest {
        val storage =
            WebStorage(
                name = testLocalStorageName,
                serializer = preferencesSerializer,
                storageType = WebStorageType.LOCAL,
            )
        val dataStore = DataStoreFactory.create(storage)
        val floatKey = floatPreferencesKey("float_key")
        val dataToWrite = preferencesOf(floatKey to 120f)

        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }
}
