/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Activity
import android.view.View
import androidx.activity.compose.setContent
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MetricCapture
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.TimeCapture
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule

private const val GROUP_METRIC_NAME = "Groups"
private const val GROUP_METRIC_INDEX = 0
private const val SLOT_METRIC_NAME = "Slots"
private const val SLOT_METRIC_INDEX = 1

private var compositionTables: MutableSet<CompositionData>? = null
private var groupsCount: Long = 0
private var slotsCount: Long = 0

private fun countGroupsAndSlots(table: CompositionData, tables: Set<CompositionData>) {
    for (group in table.compositionGroups) {
        groupsCount += group.groupSize
        slotsCount += group.slotsSize
    }

    for (subTable in tables) {
        for (group in subTable.compositionGroups) {
            groupsCount += group.groupSize
            slotsCount += group.slotsSize
        }
    }
}

@Composable
private fun CountGroupsAndSlots(content: @Composable () -> Unit) {
    val data = currentComposer.compositionData
    currentComposer.disableSourceInformation()
    CompositionLocalProvider(LocalInspectionTables provides compositionTables, content = content)
    SideEffect { compositionTables?.let { countGroupsAndSlots(data, it) } }
}

@OptIn(ExperimentalBenchmarkConfigApi::class)
abstract class ComposeBenchmarkBase {
    @get:Rule
    val benchmarkRule =
        BenchmarkRule(
            MicrobenchmarkConfig(
                metrics =
                    listOf(
                        TimeCapture(),
                        object : MetricCapture(listOf(GROUP_METRIC_NAME, SLOT_METRIC_NAME)) {
                            override fun captureStart(timeNs: Long) {
                                compositionTables = mutableSetOf()
                                groupsCount = 0
                                slotsCount = 0
                            }

                            override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
                                output[offset + GROUP_METRIC_INDEX] = groupsCount
                                output[offset + SLOT_METRIC_INDEX] = slotsCount
                                compositionTables = null
                            }

                            override fun capturePaused() {
                                // Unsupported for now
                            }

                            override fun captureResumed() {
                                // Unsupported for now
                            }
                        }
                    ),
            )
        )

    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComposeActivity::class.java)
    val activityScenario
        get() = activityScenarioRule.scenario

    // Here and elsewhere in this file, this is intentionally not OptIn, because we want to
    // communicate to consumers that by using this API, they're also transitively getting all the
    // experimental risk of using the experimental API in the kotlinx testing library.
    // DO NOT MAKE OPT-IN!
    @ExperimentalCoroutinesApi
    @ExperimentalTestApi
    suspend fun TestScope.measureCompose(block: @Composable () -> Unit) = coroutineScope {
        val recomposer = Recomposer(coroutineContext)

        try {
            val activity = activityScenario.getCurrentActivity()

            benchmarkRule.measureRepeatedOnMainThread {
                activity.setContent(recomposer) { CountGroupsAndSlots(block) }

                runWithMeasurementDisabled {
                    activity.setContentView(View(activity))
                    testScheduler.advanceUntilIdle()
                }
            }
        } finally {
            activityScenario.onActivity { activity -> activity.setContentView(View(activity)) }
            testScheduler.advanceUntilIdle()
            recomposer.cancel()
        }
    }

    @ExperimentalCoroutinesApi
    @ExperimentalTestApi
    suspend fun TestScope.measureComposeFocused(block: @Composable () -> Unit) = coroutineScope {
        val recomposer = Recomposer(coroutineContext)

        try {
            val activity = activityScenario.getCurrentActivity()
            benchmarkRule.measureRepeatedOnMainThread {
                val benchmarkState = benchmarkRule.getState()
                benchmarkState.pauseTiming()

                activity.setContent(recomposer) {
                    CountGroupsAndSlots {
                        trace("Benchmark focus") {
                            benchmarkState.resumeTiming()
                            block()
                            benchmarkState.pauseTiming()
                        }
                    }
                }
                benchmarkState.resumeTiming()

                runWithMeasurementDisabled {
                    activity.setContentView(View(activity))
                    testScheduler.advanceUntilIdle()
                }
            }
        } finally {
            activityScenario.onActivity { activity -> activity.setContentView(View(activity)) }
            testScheduler.advanceUntilIdle()
            recomposer.cancel()
        }
    }

    @ExperimentalCoroutinesApi
    @ExperimentalTestApi
    suspend fun TestScope.measureRecompose(block: RecomposeReceiver.() -> Unit) {
        val receiver = RecomposeReceiver()
        receiver.block()

        val recomposer = Recomposer(currentCoroutineContext())
        val coroutineScope = CoroutineScope(currentCoroutineContext())

        activityScenario.onActivity { activity ->
            coroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }
            activity.setContent(recomposer) { CountGroupsAndSlots(receiver.composeCb) }
        }

        var iterations = 0
        benchmarkRule.measureRepeatedOnMainThread {
            runWithMeasurementDisabled {
                receiver.updateModelCb()
                Snapshot.sendApplyNotifications()
                assertTrue(
                    "recomposer does not have invalidations for frame",
                    recomposer.hasPendingWork
                )
            }

            testScheduler.advanceUntilIdle()

            runWithMeasurementDisabled {
                assertFalse("recomposer has invalidations for frame", recomposer.hasPendingWork)
                receiver.resetCb()
                Snapshot.sendApplyNotifications()
                testScheduler.advanceUntilIdle()
                iterations++
            }
        }

        activityScenario.onActivity { activity -> activity.setContentView(View(activity)) }
        recomposer.cancel()
    }

    private suspend fun currentCoroutineContext() = coroutineContext

    private fun <A : Activity> ActivityScenario<A>.getCurrentActivity(): A {
        lateinit var activity: A
        onActivity { activity = it }
        return activity
    }
}

@ExperimentalCoroutinesApi
@ExperimentalTestApi
fun runBlockingTestWithFrameClock(
    context: CoroutineContext = EmptyCoroutineContext,
    testBody: suspend TestScope.() -> Unit
): Unit =
    runTest(UnconfinedTestDispatcher() + context) {
        withContext(TestMonotonicFrameClock(this)) { testBody() }
    }

fun ControlledComposition.performRecompose(
    readObserver: (Any) -> Unit,
    writeObserver: (Any) -> Unit
): Boolean {
    val snapshot = Snapshot.takeMutableSnapshot(readObserver, writeObserver)
    val result = snapshot.enter { recompose().also { applyChanges() } }
    snapshot.apply().check()
    return result
}

class RecomposeReceiver {
    var composeCb: @Composable () -> Unit = @Composable {}
    var updateModelCb: () -> Unit = {}
    var resetCb: () -> Unit = {}

    fun compose(block: @Composable () -> Unit) {
        composeCb = block
    }

    fun reset(block: () -> Unit) {
        resetCb = block
    }

    fun update(block: () -> Unit) {
        updateModelCb = block
    }
}

private inline fun trace(name: String, block: () -> Unit) {
    android.os.Trace.beginSection(name)
    try {
        block()
    } finally {
        android.os.Trace.endSection()
    }
}
