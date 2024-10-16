/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MultiProcessDataStoreTest {
    @get:Rule val benchmark = BenchmarkRule()

    @get:Rule val tmp = TemporaryFolder()

    @Test
    @MediumTest
    fun create() =
        runTest(UnconfinedTestDispatcher()) {
            // create a new scope for each instance and cancel it to avoid hoarding memory
            var newScope = TestScope(UnconfinedTestDispatcher())
            var testFile = tmp.newFile()

            benchmark.measureRepeated {
                val store =
                    MultiProcessDataStoreFactory.create(
                        serializer = TestingSerializer(),
                        scope = newScope
                    ) {
                        testFile
                    }
                runWithTimingDisabled {
                    Assert.assertNotNull(store)
                    newScope.cancel()
                    newScope = TestScope(UnconfinedTestDispatcher())
                    testFile = tmp.newFile()
                }
            }
        }

    @Test
    @MediumTest
    fun coldRead() {
        lateinit var store: DataStore<Byte>
        lateinit var job: Job

        suspend fun reinitDataStore() {
            job = Job()
            val dataFile = tmp.newFile()
            dataFile.writeBytes(byteArrayOf(1))
            store =
                MultiProcessDataStoreFactory.create(
                    serializer = TestingSerializer(),
                    scope = CoroutineScope(job),
                    produceFile = { dataFile }
                )
        }

        runBlocking { reinitDataStore() }
        benchmark.measureRepeated {
            runBlocking {
                val result = store.data.first()

                runWithTimingDisabled {
                    assertEquals(1, result)
                    job.cancelAndJoin()
                    reinitDataStore()
                }
            }
        }
    }

    @Test
    @MediumTest
    fun read() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = this
            val testFile = tmp.newFile()
            val store =
                MultiProcessDataStoreFactory.create(
                    serializer = TestingSerializer(),
                    scope = TestScope(UnconfinedTestDispatcher())
                ) {
                    testFile
                }
            store.updateData { 1 }
            benchmark.measureRepeated {
                runBlocking(scope.coroutineContext) {
                    val data = store.data.first()
                    runWithTimingDisabled {
                        val exp: Byte = 1
                        Assert.assertEquals(exp, data)
                    }
                }
            }
        }

    @Test
    @MediumTest
    fun update_withoutValueChange() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = this
            val testFile = tmp.newFile()
            val store =
                MultiProcessDataStoreFactory.create(
                    serializer = TestingSerializer(),
                    scope = TestScope(UnconfinedTestDispatcher())
                ) {
                    testFile
                }
            benchmark.measureRepeated {
                runBlocking(scope.coroutineContext) {
                    store.updateData { 1 }
                    val data = store.data.first()
                    runWithTimingDisabled {
                        val exp: Byte = 1
                        Assert.assertEquals(exp, data)
                    }
                }
            }
        }

    @Test
    @MediumTest
    fun update_withValueChange() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = this
            val testFile = tmp.newFile()
            val store =
                MultiProcessDataStoreFactory.create(
                    serializer = TestingSerializer(),
                    scope = TestScope(UnconfinedTestDispatcher())
                ) {
                    testFile
                }
            var counter = 0
            benchmark.measureRepeated {
                runBlocking(scope.coroutineContext) {
                    val newValue = (++counter).toByte()
                    store.updateData { newValue }
                    val data = store.data.first()
                    runWithTimingDisabled {
                        val exp: Byte = newValue
                        Assert.assertEquals(exp, data)
                    }
                }
            }
        }
}
