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

package androidx.datastore.core

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class DataStoreFactoryTest {
    // TODO add timeout
//    @get:Rule
//    val timeout = Timeout(10, TimeUnit.SECONDS)

    val testIO = TestIO()

    private val dataStoreScope: TestScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun testNewInstance() = dataStoreScope.runTest {
        val store = DataStoreFactory.create(
            storage = testIO.newFileStorage(TestingSerializer()),
            scope = dataStoreScope
        )
        val expectedByte = 123.toByte()

        assertThat(
            store.updateData {
                expectedByte
            }
        ).isEqualTo(expectedByte)
        assertThat(store.data.first()).isEqualTo(expectedByte)
    }

    @Test
    fun testCorruptionHandlerInstalled() = runTest {
        val valueToReplace = 123.toByte()

        val store = DataStoreFactory.create(
            storage = testIO.newFileStorage(
                TestingSerializer(failReadWithCorruptionException = true)),
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> {
                valueToReplace
            },
            scope = dataStoreScope
        )

        assertThat(store.data.first()).isEqualTo(valueToReplace)
    }

    @Test
    fun testMigrationsInstalled() = runTest {
        val migratedByte = 1

        val migratePlus2 = object : DataMigration<Byte> {
            override suspend fun shouldMigrate(currentData: Byte) = true
            override suspend fun migrate(currentData: Byte) = currentData.inc().inc()
            override suspend fun cleanUp() {}
        }
        val migrateMinus1 = object : DataMigration<Byte> {
            override suspend fun shouldMigrate(currentData: Byte) = true

            override suspend fun migrate(currentData: Byte) = currentData.dec()

            override suspend fun cleanUp() {}
        }

        val store = DataStoreFactory.create(
            storage = testIO.newFileStorage(TestingSerializer()),
            migrations = listOf(migratePlus2, migrateMinus1),
            scope = dataStoreScope
        )

        assertThat(store.data.first()).isEqualTo(migratedByte.toByte())
    }
}