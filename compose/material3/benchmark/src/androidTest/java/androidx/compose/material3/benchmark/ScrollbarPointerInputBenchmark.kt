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

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.material3.InteractiveScrollbarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmarks for the pointer input handling of [androidx.compose.material3.interactiveScrollbar].
 *
 * Every benchmark in this class dispatches synthetic **mouse** [MotionEvent]s directly to the host
 * view, so the measurements cover the whole pointer input pipeline (Android event dispatch, Compose
 * hit testing, and the scrollbar's own gesture handling).
 *
 * Each gesture is run in every [Mode]; see there for what each one measures.
 */
@LargeTest
@RunWith(Parameterized::class)
class ScrollbarPointerInputBenchmark(private val mode: Mode) {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "mode={0}") fun parameters() = Mode.entries
    }

    /** What a benchmark measures, depending on whether a scrollbar is attached and scrolls. */
    enum class Mode(val hasScrollbar: Boolean, val dispatchScrollEvents: Boolean) {
        /**
         * The scrollbar handles the gesture, but its callbacks don't scroll the content. This
         * isolates the cost of the scrollbar's gesture handling.
         */
        GestureOnly(hasScrollbar = true, dispatchScrollEvents = false),

        /**
         * The scrollbar handles the gesture and scrolls the content, and a frame is rendered. This
         * measures the full cost of the gesture, including moving the thumb.
         */
        GestureAndScroll(hasScrollbar = true, dispatchScrollEvents = true),

        /**
         * Baseline only, not a scrollbar measurement. Dispatches the same events to the same
         * content with no scrollbar attached, so subtracting this from [GestureOnly] gives the
         * scrollbar's own cost. The content's `verticalScroll` ignores mouse drags, so nothing
         * scrolls here either.
         */
        NoScrollbar(hasScrollbar = false, dispatchScrollEvents = false),
    }

    /** Measures a mouse drag of the scrollbar thumb. */
    @Test
    fun performMouseThumbDrag() {
        benchmarkRule.runBenchmarkFor({ ScrollbarPointerInputTestCase(mode) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                getTestCase().performThumbDrag()
                if (mode.dispatchScrollEvents) {
                    // Render the scrolled content, so that the thumb bounds used to hit test the
                    // next gesture reflect the new scroll position.
                    doFrame()
                }
                runWithMeasurementDisabled { getTestCase().assertThumbDragged() }
            }
        }
    }

    /** Measures a mouse press on the scrollbar track, outside of the thumb. */
    @Test
    fun performMouseTrackPress() {
        benchmarkRule.runBenchmarkFor({ ScrollbarPointerInputTestCase(mode) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                getTestCase().performTrackPress()
                if (mode.dispatchScrollEvents) {
                    // Render the scrolled content, so that the thumb bounds used to hit test the
                    // next gesture reflect the new scroll position.
                    doFrame()
                }
                runWithMeasurementDisabled { getTestCase().assertTrackPressed() }
            }
        }
    }
}

/**
 * A [ScrollbarTestCase] configured for [mode], that can perform mouse gestures on its scrollbar and
 * verify that they landed where intended.
 *
 * When scroll events are dispatched the content follows each gesture, so gestures alternate between
 * scrolling forward along the track and back to its start. This keeps each gesture landing on the
 * intended part of the scrollbar, and makes the content oscillate between two scroll offsets.
 * Otherwise the thumb stays at the start of the track, and every gesture scrolls forward.
 */
private class ScrollbarPointerInputTestCase(private val mode: ScrollbarPointerInputBenchmark.Mode) :
    LayeredComposeTestCase() {
    private val content =
        ScrollbarTestCase(
            hasScrollbar = mode.hasScrollbar,
            isInteractive = true,
            isFadeEnabled = false,
            dispatchScrollEvents = mode.dispatchScrollEvents,
        )

    private lateinit var mouse: ScrollbarMouseMotionEventHelper
    private lateinit var scrollbar: ScrollbarCoordinates

    /** Whether the content is scrolled forward, rather than resting at the start of the track. */
    private var isScrolledForward = false

    /** The offsets the content settles at when resting at the start, and when scrolled forward. */
    private val settledScrollOffsets = intArrayOf(UnsetScrollOffset, UnsetScrollOffset)

    @Composable
    override fun MeasuredContent() {
        content.MeasuredContent()
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        val view = LocalView.current
        val density = LocalDensity.current
        mouse = remember(view) { ScrollbarMouseMotionEventHelper(view) }
        scrollbar = remember(view, density) { ScrollbarCoordinates(view, density) }
        this.content.ContentWrappers(content)
    }

    /**
     * Grabs the thumb and drags it between the start of the track and [DragTrackFraction] of the
     * way along it, using one `ACTION_DOWN`, [DragMoveEventCount] `ACTION_MOVE`s and one
     * `ACTION_UP`.
     */
    fun performThumbDrag() {
        val atStart = scrollbar.pointInThumbStartingAt(0f)
        val scrolledForward = scrollbar.pointInThumbStartingAt(DragTrackFraction)
        if (isScrolledForward) drag(scrolledForward, atStart) else drag(atStart, scrolledForward)
        onGestureFinished()
    }

    /**
     * Presses the track outside of the thumb, using one `ACTION_DOWN` and one `ACTION_UP`: past the
     * thumb when it rests at the start of the track, and before it when it has scrolled forward.
     */
    fun performTrackPress() {
        val fraction = if (isScrolledForward) UpperTrackPressFraction else LowerTrackPressFraction
        val point = scrollbar.pointOnTrack(fraction)
        mouse.down(point)
        mouse.up(point)
        onGestureFinished()
    }

    fun assertThumbDragged() {
        assertCallbackCounts(
            expectedThumbDragCount = DragMoveEventCount,
            expectedTrackPressCount = 0,
        )
        assertScrollOffset()
    }

    fun assertTrackPressed() {
        assertCallbackCounts(expectedThumbDragCount = 0, expectedTrackPressCount = 1)
        assertScrollOffset()
    }

    private fun drag(from: Offset, to: Offset) {
        mouse.down(from)
        repeat(DragMoveEventCount) { move ->
            mouse.moveTo(lerp(from, to, (move + 1f) / DragMoveEventCount))
        }
        mouse.up(to)
    }

    private fun onGestureFinished() {
        if (mode.dispatchScrollEvents) isScrolledForward = !isScrolledForward
    }

    private fun assertCallbackCounts(expectedThumbDragCount: Int, expectedTrackPressCount: Int) {
        if (mode.hasScrollbar) {
            assertEquals(expectedThumbDragCount, content.thumbDragCount)
            assertEquals(expectedTrackPressCount, content.trackPressCount)
        } else {
            assertEquals(0, content.thumbDragCount)
            assertEquals(0, content.trackPressCount)
        }
        content.thumbDragCount = 0
        content.trackPressCount = 0
    }

    /**
     * When scroll events are dispatched, asserts that the content settled at the same offset as it
     * did the last time it was in the same position. The first time each position is reached, its
     * offset is recorded instead, and checked to differ from the other position's so that a
     * benchmark that has stopped scrolling altogether is caught too.
     *
     * When scroll events are not dispatched, asserts that the content did not scroll at all.
     */
    private fun assertScrollOffset() {
        if (!mode.dispatchScrollEvents) {
            assertEquals(0, content.scrollOffset, "Expected the content not to scroll")
            return
        }

        val index = if (isScrolledForward) 1 else 0
        val expectedScrollOffset = settledScrollOffsets[index]
        if (expectedScrollOffset != UnsetScrollOffset) {
            assertEquals(
                expectedScrollOffset,
                content.scrollOffset,
                "Expected every gesture to the same position to scroll to the same offset",
            )
            return
        }

        settledScrollOffsets[index] = content.scrollOffset
        assertTrue(
            settledScrollOffsets[1 - index] != content.scrollOffset,
            "Expected the gesture to scroll the content",
        )
    }
}

private class ScrollbarCoordinates(private val view: View, density: Density) {
    private val trackStartPx: Float
    private val trackLengthPx: Float
    private val centerInsetPx: Float
    private val thumbGrabInsetPx: Float

    init {
        with(density) {
            trackStartPx = InteractiveScrollbarDefaults.MainAxisTrackInset.toPx()
            trackLengthPx = ScrollbarViewportHeight.toPx() - 2 * trackStartPx
            centerInsetPx =
                (InteractiveScrollbarDefaults.CrossAxisTrackInset +
                        InteractiveScrollbarDefaults.Thickness / 2f)
                    .toPx()
            thumbGrabInsetPx = ThumbGrabInset.toPx()
        }
    }

    private val centerXPx: Float
        get() = view.width - centerInsetPx

    fun pointOnTrack(fraction: Float): Offset =
        Offset(centerXPx, trackStartPx + fraction * trackLengthPx)

    fun pointInThumbStartingAt(fraction: Float): Offset =
        pointOnTrack(fraction) + Offset(0f, thumbGrabInsetPx)
}

/**
 * Builds and dispatches synthetic mouse [MotionEvent]s to [view], at positions relative to [view].
 *
 * The pointer properties and coordinates are allocated once and reused, and every event is recycled
 * after it has been dispatched, to avoid some overhead and keep the measurement focused on the
 * actual logic.
 */
private class ScrollbarMouseMotionEventHelper(private val view: View) {
    private val pointerProperties =
        arrayOf(MotionEvent.PointerProperties().apply { toolType = MotionEvent.TOOL_TYPE_MOUSE })
    private val pointerCoords = arrayOf(MotionEvent.PointerCoords())
    private var eventTime = SystemClock.uptimeMillis()
    private var downTime = eventTime

    /** Presses the primary mouse button at [position], starting a new gesture. */
    fun down(position: Offset) {
        eventTime = SystemClock.uptimeMillis()
        downTime = eventTime
        dispatch(MotionEvent.ACTION_DOWN, position, MotionEvent.BUTTON_PRIMARY)
    }

    /** Moves the pointer to [position] with the primary mouse button still held down. */
    fun moveTo(position: Offset) {
        eventTime += EventIntervalMillis
        dispatch(MotionEvent.ACTION_MOVE, position, MotionEvent.BUTTON_PRIMARY)
    }

    /** Releases the primary mouse button at [position], ending the current gesture. */
    fun up(position: Offset) {
        eventTime += EventIntervalMillis
        dispatch(MotionEvent.ACTION_UP, position, buttonState = 0)
    }

    private fun dispatch(action: Int, position: Offset, buttonState: Int) {
        pointerCoords[0].x = position.x
        pointerCoords[0].y = position.y

        val motionEvent =
            MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                /* pointerCount= */ 1,
                pointerProperties,
                pointerCoords,
                /* metaState= */ 0,
                buttonState,
                /* xPrecision= */ 1f,
                /* yPrecision= */ 1f,
                /* deviceId= */ 0,
                /* edgeFlags= */ 0,
                InputDevice.SOURCE_MOUSE,
                /* flags= */ 0,
            )
        try {
            view.dispatchTouchEvent(motionEvent)
        } finally {
            motionEvent.recycle()
        }
    }
}

/** The number of `ACTION_MOVE` events dispatched over the course of a drag gesture. */
private const val DragMoveEventCount = 10

/** The fraction of the track that a drag gesture moves the thumb along. */
private const val DragTrackFraction = 0.5f

/** The fractions of the track that are pressed by the track press benchmarks. */
private const val UpperTrackPressFraction = 0.25f
private const val LowerTrackPressFraction = 0.75f

/** Sentinel for a scroll offset that has not been recorded yet. */
private const val UnsetScrollOffset = -1

/** The time between two consecutive events of a gesture. */
private const val EventIntervalMillis = 10L

/** How far past its start edge the thumb is grabbed by a drag gesture. */
private val ThumbGrabInset = 2.dp
