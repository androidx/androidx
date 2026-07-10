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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

/**
 * These benchmarks measure the amount of time needed to make multiple calls to
 * [Snapshot.sendApplyNotifications] from one thread while [snapshotFlow]s on a different thread
 * emit values. The aim of these benchmarks is to test the impact of lock contention between the
 * apply observer registered by a [SnapshotFlowManager] and the logic in its managed [snapshotFlow]s
 * that handles emitting values.
 *
 * These benchmarks were copied from [SnapshotFlowBenchmark] and adapted to be multithreaded.
 */
@LargeTest
@RunWith(Parameterized::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SnapshotFlowLockContentionBenchmark(private val n: Int) {
    @get:Rule val benchmarkRule = BenchmarkRule()

    /**
     * A test in which there are [n] [snapshotFlow]s managed by a single [SnapshotFlowManager], and
     * each [snapshotFlow] watch one of [n] distinct state objects.
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun eachSnapshotFlowWatchesOneStateObject() {
        val stateObjects = List(n) { mutableStateOf(false) }
        val jobs = Array<Job?>(n) { null }

        val manager = SnapshotFlowManager()
        var latch = CountDownLatch(n)

        for (i in 0 until n) {
            val flow = snapshotFlow(manager) { stateObjects[i].value }.onEach { latch.countDown() }
            jobs[i] = flow.launchIn(CoroutineScope(Dispatchers.Main))
        }

        latch.await()

        benchmarkRule.measureRepeated {
            latch = CountDownLatch(n)
            (0 until n).forEach { i ->
                runWithMeasurementDisabled { stateObjects[i].value = !stateObjects[i].value }
                Snapshot.sendApplyNotifications()
            }
            latch.await()
        }

        manager.dispose()
        jobs.forEach { it!!.cancel() }
    }

    companion object {
        @Parameterized.Parameters(name = "n={0}")
        @JvmStatic
        fun parameters() = listOf<Array<Any?>>(arrayOf(10), arrayOf(100))
    }
}
