/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.datastore.TestFile
import androidx.datastore.TestIO
import androidx.datastore.TestingSerializerConfig
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

abstract class CloseDownstreamOnCloseTest<F : TestFile<F>>(private val testIO: TestIO<F, *>) {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val datastoreScope = testScope.backgroundScope
    private lateinit var store: DataStore<Byte>

    @BeforeTest
    fun createDataStore() {
        val testFile: F = testIO.newTempFile()
        store =
            testIO.getStore(
                serializerConfig = TestingSerializerConfig(),
                scope = datastoreScope,
                coordinatorProducer = { createSingleProcessCoordinator(testFile.path()) },
            ) {
                testFile
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun closeWhileCollecting() =
        testScope.runTest {
            val collector = async { store.data.toList().map { it.toInt() } }
            runCurrent()
            store.updateData { 1 }
            datastoreScope.cancel()
            dispatcher.scheduler.advanceUntilIdle()
            assertThat(collector.await()).isEqualTo(listOf(0, 1))
        }

    @Test
    fun closeBeforeCollecting() =
        testScope.runTest {
            datastoreScope.cancel()
            assertThrows(CancellationException::class) { store.data.toList() }
        }
}
