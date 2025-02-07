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

package androidx.compose.runtime.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class KotlinCoroutinesBenchmark(private val dispatcher: CoroutineDispatcher) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun createCoroutineScope_dispatcherOnly() {
        benchmarkRule.measureRepeated { CoroutineScope(dispatcher) }
    }

    @Test
    fun launch1() {
        benchmarkRule.measureRepeated {
            val coroutineScope = runWithMeasurementDisabled { CoroutineScope(dispatcher) }
            coroutineScope.launch { noopForever() }
            runWithMeasurementDisabled { coroutineScope.cancel() }
        }
    }

    @Test
    fun launch10() {
        benchmarkRule.measureRepeated {
            val coroutineScope = runWithMeasurementDisabled { CoroutineScope(dispatcher) }
            repeat(10) { coroutineScope.launch { noopForever() } }
            runWithMeasurementDisabled { coroutineScope.cancel() }
        }
    }

    @Test
    fun launch100() {
        benchmarkRule.measureRepeated {
            val coroutineScope = runWithMeasurementDisabled { CoroutineScope(dispatcher) }
            repeat(100) { coroutineScope.launch { noopForever() } }
            runWithMeasurementDisabled { coroutineScope.cancel() }
        }
    }

    @Test
    fun cancelJob() {
        benchmarkRule.measureRepeated {
            val job = runWithMeasurementDisabled {
                CoroutineScope(dispatcher).launch { noopForever() }
            }
            job.cancel()
        }
    }

    @Test
    fun cancelCoroutineScopeWithoutJobs() {
        benchmarkRule.measureRepeated {
            val scope = runWithMeasurementDisabled { CoroutineScope(dispatcher) }
            scope.cancel()
        }
    }

    @Test
    fun cancelCoroutineScopeWithJobs1() {
        benchmarkRule.measureRepeated {
            val scope = runWithMeasurementDisabled {
                CoroutineScope(dispatcher).apply {
                    launch { noopForever() }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                }
            }
            scope.cancel()
        }
    }

    @Test
    fun cancelCoroutineScopeWithJobs10() {
        benchmarkRule.measureRepeated {
            val scope = runWithMeasurementDisabled {
                CoroutineScope(dispatcher).apply {
                    repeat(10) { launch { noopForever() } }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                }
            }
            scope.cancel()
        }
    }

    @Test
    fun cancelCoroutineScopeWithJobs100() {
        benchmarkRule.measureRepeated {
            val scope = runWithMeasurementDisabled {
                CoroutineScope(dispatcher).apply {
                    repeat(100) { launch { noopForever() } }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                }
            }
            scope.cancel()
        }
    }

    private suspend fun noopForever() {
        delay(Int.MAX_VALUE.toLong())
    }

    companion object {
        @Parameterized.Parameters(name = "Dispatcher={0}")
        @JvmStatic
        fun parameters() =
            listOf<Array<Any?>>(
                arrayOf(Dispatchers.Main),
                arrayOf(Dispatchers.Unconfined),
            )
    }
}
