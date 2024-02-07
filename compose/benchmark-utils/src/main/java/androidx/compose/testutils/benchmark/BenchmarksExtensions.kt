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

package androidx.compose.testutils.benchmark

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeExecutionControl
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges
import androidx.compose.testutils.setupContent
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs

/**
 * Measures measure and layout performance of the given test case by toggling measure constraints.
 */
fun ComposeBenchmarkRule.benchmarkLayoutPerf(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        val measureSpecs = arrayOf(0, 1, 2, 3)

        runOnUiThread {
            doFramesUntilNoChangesPending()

            val width = measuredWidth
            val height = measuredHeight

            measureSpecs[0] = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            measureSpecs[1] = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            measureSpecs[2] = View.MeasureSpec.makeMeasureSpec(width - 10, View.MeasureSpec.EXACTLY)
            measureSpecs[3] =
                View.MeasureSpec.makeMeasureSpec(height - 10, View.MeasureSpec.EXACTLY)

            requestLayout()
            measureWithSpec(measureSpecs[0], measureSpecs[1])
            layout()
        }

        var offset = 0
        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                // toggle between 0 and 2
                offset = abs(2 - offset)
                requestLayout()
            }
            measureWithSpec(measureSpecs[offset], measureSpecs[offset + 1])
            layout()
        }
    }
}

fun AndroidBenchmarkRule.benchmarkLayoutPerf(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        val measureSpecs = arrayOf(0, 1, 2, 3)

        runOnUiThread {
            doFrame()

            val width = measuredWidth
            val height = measuredHeight

            measureSpecs[0] = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            measureSpecs[1] = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            measureSpecs[2] = View.MeasureSpec.makeMeasureSpec(width - 10, View.MeasureSpec.EXACTLY)
            measureSpecs[3] =
                View.MeasureSpec.makeMeasureSpec(height - 10, View.MeasureSpec.EXACTLY)

            requestLayout()
            measureWithSpec(measureSpecs[0], measureSpecs[1])
            layout()
        }

        var offset = 0
        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                // toggle between 0 and 2
                offset = abs(2 - offset)
                requestLayout()
            }
            measureWithSpec(measureSpecs[offset], measureSpecs[offset + 1])
            layout()
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun AndroidBenchmarkRule.benchmarkDrawPerf(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFrame()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun ComposeBenchmarkRule.benchmarkDrawPerf(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first recomposition without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkRecompose(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true,
    requireRecomposition: Boolean = true,
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }
        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            if (requireRecomposition) {
                recomposeAssertHadChanges()
            } else {
                recompose()
            }
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first remeasure without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }
        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            measure()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first relayout without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkLayout(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                measure()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            layout()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first redraw without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFrame()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            measure()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkLayout(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFrame()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                measure()
            }
            layout()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFrame()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures recompose, measure and layout time after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first recompose, remeasure and relayout without any pending
 * animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true,
    requireRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }
        measureRepeatedOnUiThread {
            getTestCase().toggleState()
            if (requireRecomposition) {
                recomposeAssertHadChanges()
            } else {
                recompose()
            }
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
            measure()
            layout()
            runWithTimingDisabled {
                drawPrepare()
                draw()
                drawFinish()
            }
        }
    }
}

/**
 *  Measures measure and layout time after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first remeasure and relayout without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasureLayout(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            measure()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 * Runs a reuse benchmark for the given [content].
 * @param content The Content to be benchmarked.
 */
fun ComposeBenchmarkRule.benchmarkReuseFor(
    content: @Composable () -> Unit
) {
    val testCase = { SubcomposeLayoutReuseTestCase(reusableSlots = 1, content) }
    runBenchmarkFor(testCase) {
        runOnUiThread {
            setupContent()
            doFramesUntilIdle()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().clearContent()
                doFramesUntilIdle()
                assertNoPendingChanges()
            }

            getTestCase().initContent()
            doFramesUntilIdle()
        }
    }
}

private fun ComposeExecutionControl.doFramesUntilIdle() {
    do {
        doFrame()
    } while (hasPendingChanges() || hasPendingMeasureOrLayout())
}

/**
 * A [ComposeTestCase] to emulate content reuse.
 *
 * @param reusableSlots The max number of slots that will be kept for use. For instance, if
 * reusableSlots=0 the content will be always disposed.
 * @param content The composable content that will be benchmarked
 */
class SubcomposeLayoutReuseTestCase(
    private val reusableSlots: Int = 0,
    private val content: @Composable () -> Unit
) : ComposeTestCase {
    private var active by mutableStateOf(true)

    @Composable
    override fun Content() {
        SubcomposeLayout(
            SubcomposeLayoutState(SubcomposeSlotReusePolicy(reusableSlots))
        ) { constraints ->
            val measurables = if (active) {
                subcompose(Unit) { content() }
            } else {
                null
            }

            val placeable = measurables?.single()?.measure(constraints)
            layout(placeable?.width ?: 0, placeable?.height ?: 0) {
                placeable?.place(IntOffset.Zero)
            }
        }
    }

    fun clearContent() {
        active = false
    }

    fun initContent() {
        active = true
    }
}

@VisibleForTesting
private fun ComposeExecutionControl.hasPendingMeasureOrLayout(): Boolean {
    return (getHostView() as ViewRootForTest).hasPendingMeasureOrLayout
}
