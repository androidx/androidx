/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.recomposeUntilNoChangesPending
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for Wear Compose ScalingLazyColumn.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ScalingLazyColumnBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val scalingLazyColumnCaseFactory = { ScalingLazyColumnTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstScalingLazyColumnMeasure(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstScalingLazyColumnLayout(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstScalingLazyColumnDraw(scalingLazyColumnCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(scalingLazyColumnCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(scalingLazyColumnCaseFactory)
    }
}

internal class ScalingLazyColumnTestCase : LayeredComposeTestCase() {
    private var itemSizeDp: Dp = 10.dp
    private var defaultItemSpacingDp: Dp = 4.dp

    @Composable
    override fun MeasuredContent() {
        ScalingLazyColumn(
            state = rememberScalingLazyListState(),
            modifier = Modifier.requiredSize(
                itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
            ),
        ) {
            items(10) { it ->
                Box(Modifier.requiredSize(itemSizeDp)) {
                    BasicText(text = "Item $it",
                        Modifier
                            .background(Color.White)
                            .padding(2.dp),
                        TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp,
                        )
                    )
                }
            }
        }
    }
}

// TODO (b/210654937): Should be able to get rid of this workaround in the future once able to call
// LaunchedEffect directly on underlying LazyColumn rather than via a 2-stage initialization via
// onGloballyPositioned().
fun ComposeBenchmarkRule.benchmarkFirstScalingLazyColumnMeasure(
    caseFactory: () -> LayeredComposeTestCase
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
            }

            measure()
            recomposeUntilNoChangesPending()

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

// TODO (b/210654937): Should be able to get rid of this workaround in the future once able to call
// LaunchedEffect directly on underlying LazyColumn rather than via a 2-stage initialization via
// onGloballyPositioned().
fun ComposeBenchmarkRule.benchmarkFirstScalingLazyColumnLayout(
    caseFactory: () -> LayeredComposeTestCase
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
                measure()
            }

            layout()
            recomposeUntilNoChangesPending()

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

// TODO (b/210654937): Should be able to get rid of this workaround in the future once able to call
// LaunchedEffect directly on underlying LazyColumn rather than via a 2-stage initialization via
// onGloballyPositioned().
fun ComposeBenchmarkRule.benchmarkFirstScalingLazyColumnDraw(
    caseFactory: () -> LayeredComposeTestCase
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()
            drawFinish()
            recomposeUntilNoChangesPending()

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

private class LayeredCaseAdapter(private val innerCase: LayeredComposeTestCase) : ComposeTestCase {

    companion object {
        fun of(caseFactory: () -> LayeredComposeTestCase): () -> LayeredCaseAdapter = {
            LayeredCaseAdapter(caseFactory())
        }
    }

    var isComposed by mutableStateOf(false)

    @Composable
    override fun Content() {
        innerCase.ContentWrappers {
            if (isComposed) {
                innerCase.MeasuredContent()
            }
        }
    }

    fun addMeasuredContent() {
        Assert.assertTrue(!isComposed)
        isComposed = true
    }
}
