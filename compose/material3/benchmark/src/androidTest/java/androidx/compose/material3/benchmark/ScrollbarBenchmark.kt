/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.scrollbar
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayoutDraw
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ScrollbarBenchmark(private val hasScrollbar: Boolean, private val isFadeEnabled: Boolean) {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "hasScrollbar={0},isFadeEnabled={1}")
        fun parameters() =
            arrayOf(
                arrayOf(false, false), // Baseline test - scrollbar disabled
                arrayOf(true, true), // Scrollbar with fade enabled
                // arrayOf(true, false), // Scrollbar with fade disabled (for local evaluation)
            )
    }

    private val scrollbarTestCaseFactory = { ScrollbarTestCase(hasScrollbar, isFadeEnabled) }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(scrollbarTestCaseFactory)
    }

    @Test
    fun scrolling_recomposeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            caseFactory = scrollbarTestCaseFactory,
            requireRecomposition = false,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun successiveScroll_recomposeMeasureLayoutDraw() {
        benchmarkRule.runBenchmarkFor(scrollbarTestCaseFactory) {
            benchmarkRule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    doFramesUntilNoChangesPending()

                    // Dispatch the first unmeasured scroll and run exactly one frame. This forces
                    // the scrollbar to spin up its fade-in coroutine and transition to visible.
                    getTestCase().toggleState()
                    doFrame()
                }

                // Dispatch the second scroll in the next frame while the scrollbar is still active.
                getTestCase().toggleState()
                doFrame()

                runWithMeasurementDisabled { disposeContent() }
            }
        }
    }
}

internal class ScrollbarTestCase(
    private val hasScrollbar: Boolean,
    private val isFadeEnabled: Boolean,
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val scrollState = ScrollState(initial = 0)
    private val scrollIndicatorState = scrollState.scrollIndicatorState
    private val baseModifier = Modifier.requiredHeight(400.dp).fillMaxWidth()
    private val itemModifier = Modifier.requiredSize(50.dp).background(Color.Red)

    @Composable
    override fun MeasuredContent() {
        val scrollbarModifier =
            if (hasScrollbar) {
                baseModifier.scrollbar(
                    state = scrollIndicatorState,
                    orientation = Orientation.Vertical,
                    isFadeEnabled = isFadeEnabled,
                )
            } else {
                baseModifier
            }

        val modifier = scrollbarModifier.verticalScroll(scrollState, overscrollEffect = null)

        Column(modifier = modifier) {
            repeat(100) { index -> Box(modifier = itemModifier) { Text("Item $index") } }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        val amount = if (scrollState.value > 0) -100f else 100f
        scrollState.dispatchRawDelta(amount)
    }
}
