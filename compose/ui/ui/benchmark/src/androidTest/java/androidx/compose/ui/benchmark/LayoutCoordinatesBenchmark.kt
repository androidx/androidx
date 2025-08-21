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

package androidx.compose.ui.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LayoutCoordinatesBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun localPositionOfWithLayer() {
        benchmarkRule.runBenchmarkFor({ LayoutCoordinatesTestCase(true) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                testCase.coordinates1.localPositionOf(testCase.coordinates2)
                testCase.coordinates2.localPositionOf(testCase.coordinates1)
            }
        }
    }

    @Test
    fun localPositionOfNoLayer() {
        benchmarkRule.runBenchmarkFor({ LayoutCoordinatesTestCase(false) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                testCase.coordinates1.localPositionOf(testCase.coordinates2)
                testCase.coordinates2.localPositionOf(testCase.coordinates1)
            }
        }
    }

    @Test
    fun grandParentMoves_gettingPositionInRoot_onPlaced() {
        benchmarkRule.toggleStateBenchmark(
            MovingGrandParentTestCase(Modifier.onPlaced { it.positionInRoot() })
        )
    }

    @Test
    fun grandParentMoves_gettingPositionInRoot_onGloballyPositioned() {
        benchmarkRule.toggleStateBenchmark(
            MovingGrandParentTestCase(Modifier.onGloballyPositioned() { it.positionInRoot() })
        )
    }

    @Test
    fun grandParentMoves_gettingPositionInRoot_onLayoutRectChanged() {
        benchmarkRule.toggleStateBenchmark(
            MovingGrandParentTestCase(Modifier.onLayoutRectChanged { it.positionInRoot })
        )
    }

    @Test
    fun grandParentMoves_gettingPositionInRoot_placementBlock() {
        benchmarkRule.toggleStateBenchmark(
            MovingGrandParentTestCase(
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        coordinates?.positionInRoot()
                        placeable.place(0, 0)
                    }
                }
            )
        )
    }

    @Test
    fun grandParentMoves_noCoordinatesUsage() {
        benchmarkRule.toggleStateBenchmark(MovingGrandParentTestCase(Modifier))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun grandParentMoves_noCoordinatesUsage_noRectTracking() {
        ComposeUiFlags.isRectTrackingEnabled = false
        benchmarkRule.toggleStateBenchmark(MovingGrandParentTestCase(Modifier))
        ComposeUiFlags.isRectTrackingEnabled = true
    }

    private class LayoutCoordinatesTestCase(val useLayer: Boolean) : ComposeTestCase {
        lateinit var coordinates1: LayoutCoordinates
        lateinit var coordinates2: LayoutCoordinates

        @Composable
        private fun NestedContent(depth: Int, isFirst: Boolean) {
            if (depth == 0) {
                Box(
                    Modifier.fillMaxSize().onPlaced {
                        if (isFirst) coordinates1 = it else coordinates2 = it
                    }
                )
            } else {
                val modifier = if (useLayer) Modifier.graphicsLayer {} else Modifier
                Box(modifier.padding(1.dp)) { NestedContent(depth - 1, isFirst) }
            }
        }

        @Composable
        override fun Content() {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { NestedContent(10, true) }
                Box(Modifier.weight(1f).fillMaxWidth()) { NestedContent(10, false) }
            }
        }
    }

    private class MovingGrandParentTestCase(private val leafModifier: Modifier) :
        ComposeTestCase, ToggleableTestCase {
        private var offset by mutableStateOf(0.dp)

        @Composable
        private fun NestedContent(depth: Int) {
            if (depth == 0) {
                Box(Modifier.fillMaxSize().then(leafModifier))
            } else {
                Box(Modifier.padding(1.dp)) { NestedContent(depth - 1) }
            }
        }

        @Composable
        override fun Content() {
            Box(Modifier.offset(offset)) { NestedContent(10) }
        }

        override fun toggleState() {
            offset = if (offset == 0.dp) 1.dp else 0.dp
        }
    }

    private fun ComposeBenchmarkRule.toggleStateBenchmark(case: MovingGrandParentTestCase) {
        runBenchmarkFor({ case }) {
            runOnUiThread { doFramesUntilNoChangesPending() }

            measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().toggleState()
                    recompose()
                    requestLayout()
                    measure()
                }
                layout()
            }
        }
    }
}
