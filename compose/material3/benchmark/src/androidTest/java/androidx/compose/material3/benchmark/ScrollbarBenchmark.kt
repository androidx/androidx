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
import androidx.compose.material3.interactiveScrollbar
import androidx.compose.material3.nonInteractiveScrollbar
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
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ScrollbarBenchmark(
    private val hasScrollbar: Boolean,
    private val isInteractive: Boolean,
    private val isFadeEnabled: Boolean,
) {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "hasScrollbar={0},isInteractive={1},isFadeEnabled={2}")
        fun parameters() =
            arrayOf(
                arrayOf(false, false, false), // Baseline test - scrollbar disabled
                arrayOf(true, false, true), // Non-interactive scrollbar with fade enabled
                arrayOf(true, false, false), // Non-interactive scrollbar with fade disabled
                arrayOf(true, true, true), // Interactive scrollbar with fade enabled
                arrayOf(true, true, false), // Interactive scrollbar with fade disabled
            )
    }

    private val scrollbarTestCaseFactory = {
        ScrollbarTestCase(
            hasScrollbar = hasScrollbar,
            isInteractive = isInteractive,
            isFadeEnabled = isFadeEnabled,
            // Unused: content is scrolled directly via toggleState(), so no callbacks fire.
            dispatchScrollEvents = false,
        )
    }

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

/**
 * A scrollable [Column] that optionally draws a scrollbar along its end edge.
 *
 * @param hasScrollbar whether a scrollbar is attached to the content at all.
 * @param isInteractive whether the scrollbar responds to gestures.
 * @param isFadeEnabled whether the scrollbar fades out once scrolling stops.
 * @param dispatchScrollEvents whether the scrollbar's callbacks actually scroll the content.
 *   Turning this off isolates the cost of the scrollbar itself from the cost of scrolling.
 */
internal class ScrollbarTestCase(
    private val hasScrollbar: Boolean,
    private val isInteractive: Boolean,
    private val isFadeEnabled: Boolean,
    private val dispatchScrollEvents: Boolean,
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val scrollState = ScrollState(initial = 0)
    private val scrollIndicatorState = scrollState.scrollIndicatorState
    private val baseModifier = Modifier.requiredHeight(ScrollbarViewportHeight).fillMaxWidth()
    private val itemModifier = Modifier.requiredSize(ScrollbarItemSize).background(Color.Red)

    /** The number of times the scrollbar has reported a thumb drag since the last reset. */
    var thumbDragCount: Int = 0

    /** The number of times the scrollbar has reported a track press since the last reset. */
    var trackPressCount: Int = 0

    /** The current scroll offset of the content, in pixels. */
    val scrollOffset: Int
        get() = scrollState.value

    @Composable
    override fun MeasuredContent() {
        val scrollbarModifier =
            if (hasScrollbar && scrollIndicatorState != null) {
                if (isInteractive) {
                    baseModifier.interactiveScrollbar(
                        state = scrollIndicatorState,
                        orientation = Orientation.Vertical,
                        onThumbDrag = { positionFraction ->
                            thumbDragCount++
                            scrollToPositionFraction(positionFraction)
                        },
                        onTrackPress = { positionFraction ->
                            trackPressCount++
                            scrollToPositionFraction(positionFraction)
                        },
                        isFadeEnabled = isFadeEnabled,
                    )
                } else {
                    baseModifier.nonInteractiveScrollbar(
                        state = scrollIndicatorState,
                        orientation = Orientation.Vertical,
                        isFadeEnabled = isFadeEnabled,
                    )
                }
            } else {
                baseModifier
            }

        val modifier = scrollbarModifier.verticalScroll(scrollState, overscrollEffect = null)

        Column(modifier = modifier) {
            repeat(ScrollbarItemCount) { index ->
                Box(modifier = itemModifier) { Text("Item $index") }
            }
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

    /**
     * Scrolls the content to [positionFraction], the position of the scrollbar thumb along its
     * track, normalized to `[0f, 1f]`.
     */
    private fun scrollToPositionFraction(positionFraction: Float) {
        if (!dispatchScrollEvents) return
        val state = scrollIndicatorState ?: return
        val maxScrollOffset = state.contentSize - state.viewportSize
        val targetPx = (positionFraction * maxScrollOffset).roundToInt()
        scrollState.dispatchRawDelta((targetPx - state.scrollOffset).toFloat())
    }
}

/** The height of the viewport that [ScrollbarTestCase] scrolls its content within. */
internal val ScrollbarViewportHeight = 400.dp

/** The height of a single item of [ScrollbarTestCase]'s content. */
private val ScrollbarItemSize = 50.dp

/** The number of items in [ScrollbarTestCase]'s content. */
private const val ScrollbarItemCount = 100
