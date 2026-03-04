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

package androidx.compose.runtime.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.SnapshotFlowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

/**
 * Benchmarks that test the performance characteristics of [snapshotFlow]s.
 *
 * In these benchmarks:
 * 1) State objects and [snapshotFlow]s are initialized
 * 2) The value of each state object is changed one-by-one, and the [snapshotFlow]s are forced to
 *    respond to each change
 */
@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SnapshotFlowBenchmark(
    private val snapshotFlowManagerKind: SnapshotFlowManagerSharing,
    private val n: Int,
) {
    enum class SnapshotFlowManagerSharing {
        /**
         * Make each [snapshotFlow] in the benchmark be backed by a distinct [SnapshotFlowManager].
         */
        NONE,

        /** Make all [snapshotFlow]s in the benchmark share the same [SnapshotFlowManager]. */
        MAXIMAL,
    }

    @get:Rule val benchmarkRule = BenchmarkRule()

    /**
     * A test in which there are [n] [snapshotFlow]s that each watch one of [n] distinct state
     * objects.
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun eachSnapshotFlowWatchesOneStateObject() {
        benchmarkRule.measureRepeated {
            runTest {
                val stateObjects = runWithMeasurementDisabled { List(n) { mutableStateOf(false) } }
                var count = 0
                val jobs = runWithMeasurementDisabled { Array<Job?>(n) { null } }

                val manager =
                    if (snapshotFlowManagerKind == SnapshotFlowManagerSharing.MAXIMAL)
                        SnapshotFlowManager()
                    else null
                for (i in 0 until n) {
                    val flow =
                        snapshotFlowFactory(manager) { stateObjects[i].value }.onEach { count++ }
                    runWithMeasurementDisabled { jobs[i] = flow.launchIn(this) }
                }

                // This test uses a `runTest` single-threaded dispatcher, which means that changes
                // aren't flushed to `snapshotFlow`s until we `yield()` intentionally.
                runWithMeasurementDisabled {
                    testScheduler.advanceUntilIdle()
                    assertEquals(n, count)
                    Snapshot.notifyObjectsInitialized()
                }

                lateinit var snapshot: MutableSnapshot
                stateObjects.forEach {
                    runWithMeasurementDisabled {
                        snapshot = Snapshot.takeMutableSnapshot()
                        snapshot.enter { it.value = true }
                    }

                    snapshot.apply()
                }

                testScheduler.advanceUntilIdle()

                runWithMeasurementDisabled {
                    assertEquals(n * 2, count)
                    manager?.dispose()
                    jobs.forEach { it!!.cancel() }
                    testScheduler.advanceUntilIdle()
                }
            }
        }
    }

    /**
     * A test with [n] [snapshotFlow]s and [n] distinct state objects, in which each [snapshotFlow]
     * watches 10 state objects, and each state object is watched by 10 [snapshotFlow]s.
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun eachSnapshotFlowWatchesTenStateObjects() {
        benchmarkRule.measureRepeated {
            runTest {
                val stateObjects = runWithMeasurementDisabled { List(n) { mutableStateOf(false) } }
                var count = 0
                val jobs = runWithMeasurementDisabled { Array<Job?>(n) { null } }

                val manager =
                    if (snapshotFlowManagerKind == SnapshotFlowManagerSharing.MAXIMAL)
                        SnapshotFlowManager()
                    else null
                for (i in 0 until n) {
                    val flow =
                        snapshotFlowFactory(manager) {
                                stateObjects[i].value
                                stateObjects[(i + 1) % n].value
                                stateObjects[(i + 2) % n].value
                                stateObjects[(i + 3) % n].value
                                stateObjects[(i + 4) % n].value
                                stateObjects[(i + 5) % n].value
                                stateObjects[(i + 6) % n].value
                                stateObjects[(i + 7) % n].value
                                stateObjects[(i + 8) % n].value
                                stateObjects[(i + 9) % n].value
                            }
                            .onEach { count++ }
                    runWithMeasurementDisabled { jobs[i] = flow.launchIn(this) }
                }

                // This test uses a `runTest` single-threaded dispatcher, which means that changes
                // aren't flushed to `snapshotFlow`s until we `yield()` intentionally.
                runWithMeasurementDisabled {
                    testScheduler.advanceUntilIdle()
                    assertEquals(n, count)
                }

                lateinit var snapshot: MutableSnapshot
                stateObjects.forEach {
                    runWithMeasurementDisabled {
                        snapshot = Snapshot.takeMutableSnapshot()
                        snapshot.enter { it.value = true }
                    }

                    snapshot.apply()
                }

                testScheduler.advanceUntilIdle()

                runWithMeasurementDisabled {
                    assertEquals(n * 2, count)
                    manager?.dispose()
                    jobs.forEach { it!!.cancel() }
                    testScheduler.advanceUntilIdle()
                }
            }
        }
    }

    companion object {
        // Like `snapshotFlow`, but with a nullable `manager` parameter.
        @OptIn(ExperimentalComposeRuntimeApi::class)
        fun <T> snapshotFlowFactory(manager: SnapshotFlowManager?, block: () -> T): Flow<T> {
            return if (manager == null) {
                snapshotFlow(block)
            } else {
                snapshotFlow(manager, block)
            }
        }

        @Parameterized.Parameters(name = "snapshotFlowManagerSharing={0}, n={1}")
        @JvmStatic
        fun parameters() =
            listOf<Array<Any?>>(
                arrayOf(SnapshotFlowManagerSharing.NONE, 10),
                arrayOf(SnapshotFlowManagerSharing.MAXIMAL, 10),
                arrayOf(SnapshotFlowManagerSharing.NONE, 100),
                arrayOf(SnapshotFlowManagerSharing.MAXIMAL, 100),
            )
    }
}
