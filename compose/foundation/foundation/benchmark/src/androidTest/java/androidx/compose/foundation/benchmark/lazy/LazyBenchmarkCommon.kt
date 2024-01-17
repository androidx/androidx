/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.lazy

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeExecutionControl
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import kotlinx.coroutines.runBlocking

internal object NoFlingBehavior : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return 0f
    }
}

data class LazyItem(val index: Int)

/**
 * Helper for dispatching simple [MotionEvent]s to a [view] for use in scrolling benchmarks.
 */
class MotionEventHelper(private val view: View) {
    private var time = 0L
    private var lastCoord: Offset? = null

    fun sendEvent(
        action: Int,
        delta: Offset,
        timeDelta: Long = 10L
    ) {
        time += timeDelta

        val coord = delta + (lastCoord ?: Offset.Zero)

        lastCoord = if (action == MotionEvent.ACTION_UP) {
            null
        } else {
            coord
        }

        val locationOnScreen = IntArray(2) { 0 }
        view.getLocationOnScreen(locationOnScreen)

        val motionEvent = MotionEvent.obtain(
            0,
            time,
            action,
            1,
            arrayOf(MotionEvent.PointerProperties()),
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = locationOnScreen[0] + coord.x.coerceAtLeast(1f)
                    y = locationOnScreen[1] + coord.y.coerceAtLeast(1f)
                }
            ),
            0,
            0,
            0f,
            0f,
            0,
            0,
            0,
            0
        ).apply {
            offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
        }

        view.dispatchTouchEvent(motionEvent)
    }
}

// TODO(b/169852102 use existing public constructs instead)
internal fun ComposeBenchmarkRule.toggleStateBenchmark(
    caseFactory: () -> LazyBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFramesUntilNoChangesPending()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                assertNoPendingRecompositionMeasureOrLayout()
                getTestCase().beforeToggle()
                if (hasPendingChanges() || hasPendingMeasureOrLayout()) {
                    doFrame()
                }
                assertNoPendingRecompositionMeasureOrLayout()
            }
            performToggle(getTestCase())
            runWithTimingDisabled {
                assertNoPendingRecompositionMeasureOrLayout()
                getTestCase().afterToggle()
                assertNoPendingRecompositionMeasureOrLayout()
            }
        }
    }
}

// we extract this function so it is easier to differentiate this work  in the traces from the work
// we are not measuring, like beforeToggle() and afterToggle().
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeExecutionControl.performToggle(testCase: LazyBenchmarkTestCase) {
    testCase.toggle()
    if (hasPendingChanges()) {
        recompose()
    }
    if (hasPendingMeasureOrLayout()) {
        getViewRoot().measureAndLayoutForTest()
    }
}

private fun ComposeExecutionControl.assertNoPendingRecompositionMeasureOrLayout() {
    if (hasPendingChanges() || hasPendingMeasureOrLayout()) {
        throw AssertionError("Expected no pending changes but there were some.")
    }
}

private fun ComposeExecutionControl.hasPendingMeasureOrLayout(): Boolean {
    return getViewRoot().hasPendingMeasureOrLayout
}

private fun ComposeExecutionControl.getViewRoot(): ViewRootForTest =
    getHostView() as ViewRootForTest

// TODO(b/169852102 use existing public constructs instead)
internal fun ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> LazyBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread {
            doFrame()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                // reset the state and draw
                getTestCase().beforeToggle()
                measure()
                layout()
                drawPrepare()
                draw()
                drawFinish()
                // toggle and prepare measuring draw
                getTestCase().toggle()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                getTestCase().afterToggle()
                drawFinish()
            }
        }
    }
}

abstract class LazyBenchmarkTestCase(
    private val isVertical: Boolean,
    private val usePointerInput: Boolean
) : ComposeTestCase {

    lateinit var scrollingHelper: ScrollingHelper

    fun beforeToggle() {
        setUp()
        scrollingHelper.onBeforeScroll()
        beforeToggleCheck()
    }

    fun toggle() {
        scrollingHelper.onScroll()
    }

    fun afterToggle() {
        afterToggleCheck()
        scrollingHelper.onAfterScroll()
        tearDown()
    }

    @Composable
    fun InitializeScrollHelper(scrollAmount: Int) {
        val view = LocalView.current
        val touchSlop = LocalViewConfiguration.current.touchSlop

        if (!::scrollingHelper.isInitialized) scrollingHelper = ScrollingHelper(
            view,
            MotionEventHelper(view),
            touchSlop,
            scrollAmount,
            isVertical,
            usePointerInput,
            ::programmaticScroll
        )
    }

    abstract fun beforeToggleCheck()
    abstract fun afterToggleCheck()

    abstract suspend fun programmaticScroll(amount: Int)

    // first instruction to run in the before toggle cycle
    abstract fun setUp()

    // last instruction to run at the end of the after toggle cycke
    abstract fun tearDown()
}

class ScrollingHelper(
    private val view: View,
    private val motionEventHelper: MotionEventHelper,
    private val touchSlop: Float,
    val scrollAmount: Int,
    private val isVertical: Boolean,
    private val usePointerInput: Boolean,
    private val programmaticScroll: suspend (scrollAmount: Int) -> Unit
) {

    fun onBeforeScroll() {
        if (!usePointerInput) return
        val size = if (isVertical) view.measuredHeight else view.measuredWidth
        motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, (size / 2f).toSingleAxisOffset())
        motionEventHelper.sendEvent(MotionEvent.ACTION_MOVE, touchSlop.toSingleAxisOffset())
    }

    fun onScroll() {
        if (usePointerInput) {
            motionEventHelper
                .sendEvent(MotionEvent.ACTION_MOVE, -scrollAmount.toFloat().toSingleAxisOffset())
        } else {
            runBlocking { programmaticScroll.invoke(scrollAmount) }
        }
    }

    fun onAfterScroll() {
        if (!usePointerInput) return
        motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
    }

    private fun Float.toSingleAxisOffset(): Offset =
        Offset(x = if (isVertical) 0f else this, y = if (isVertical) this else 0f)
}
