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

package androidx.datastore

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class DataStoreFactoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope
    private lateinit var context: Context

    @Before
    fun setUp() {
        testFile = tmp.newFile()
        dataStoreScope = TestCoroutineScope()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCreateWithContextAndName() = runBlockingTest {
        val byte = 1

        var store = context.createDataStore(
            serializer = TestingSerializer(),
            fileName = "my_settings.byte",
            scope = dataStoreScope
        )
        store.updateData { 1 }

        // Create it again and confirm it's still there
        store = context.createDataStore(
            serializer = TestingSerializer(),
            fileName = "my_settings.byte",
            scope = dataStoreScope
        )
        assertThat(store.data.first()).isEqualTo(byte)

        // Check that the file name is context.filesDir + fileName
        store = DataStoreFactory.create(
            serializer = TestingSerializer(),
            scope = dataStoreScope
        ) {
            File(context.filesDir, "datastore/my_settings.byte")
        }
        assertThat(store.data.first()).isEqualTo(byte)
    }
}