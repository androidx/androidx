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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SingleProcessDatastoreTest {
    @get:Rule
    val benchmark = BenchmarkRule()

    @get:Rule
    val tmp = TemporaryFolder()
    private lateinit var testScope: TestScope
    private lateinit var dataStoreScope: TestScope

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    @LargeTest
    fun create() = testScope.runTest {
        benchmark.measureRepeated {
            val testFile = tmp.newFile()
            val store = DataStoreFactory.create(
                serializer = TestingSerializer(),
                scope = dataStoreScope
            ) { testFile }
            runWithTimingDisabled {
                Assert.assertNotNull(store)
            }
        }
    }

    @Test
    @MediumTest
    fun read() = testScope.runTest {
        val scope = this
        val testFile = tmp.newFile()
        val store = DataStoreFactory.create(
            serializer = TestingSerializer(),
            scope = dataStoreScope
        ) { testFile }
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
    fun update() = testScope.runTest {
        val scope = this
        val testFile = tmp.newFile()
        val store = DataStoreFactory.create(
            serializer = TestingSerializer(),
            scope = dataStoreScope
        ) { testFile }
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
}