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

package androidx.pdf.ink

import android.graphics.Color
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.pdf.FakeEditablePdfDocument
import androidx.pdf.ink.state.AnnotationDrawingMode
import androidx.pdf.ink.util.TestTouchEventDispatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AnnotationsTouchEventDispatcherTest {
    private lateinit var annotationsViewDispatcher: TestTouchEventDispatcher
    private lateinit var inkViewDispatcher: TestTouchEventDispatcher
    private lateinit var annotationsTouchEventDispatcher: AnnotationsTouchEventDispatcher

    private var gestureDownTime = 0L
    private var currentEventTimeInGesture = 0L

    @Before
    fun setUp() {
        annotationsViewDispatcher = TestTouchEventDispatcher()
        inkViewDispatcher = TestTouchEventDispatcher()
        annotationsTouchEventDispatcher =
            AnnotationsTouchEventDispatcher(annotationsViewDispatcher, inkViewDispatcher)
    }

    @Test
    fun dispatchTouchEvent_noDrawingMode_returnsFalseAndDoesNotDispatch() {
        annotationsTouchEventDispatcher.drawingMode = null
        val event = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)

        val result = annotationsTouchEventDispatcher.dispatchTouchEvent(event)

        assertThat(result).isFalse()
        assertThat(inkViewDispatcher.wasCalled()).isFalse()
        assertThat(annotationsViewDispatcher.wasCalled()).isFalse()
    }

    @Test
    fun dispatchTouchEvent_inPenMode_sendsEventsToInkDispatcherOnly() {
        annotationsTouchEventDispatcher.drawingMode = penMode

        // Dispatch DOWN, MOVE, UP events
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)

        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent)

        val upEvent = createMotionEvent(MotionEvent.ACTION_UP, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(upEvent)

        // Verify ink dispatcher received all events
        assertThat(inkViewDispatcher.callCount()).isEqualTo(3)
        assertThat(inkViewDispatcher.receivedEvents[0].actionMasked)
            .isEqualTo(MotionEvent.ACTION_DOWN)
        assertThat(inkViewDispatcher.receivedEvents[1].actionMasked)
            .isEqualTo(MotionEvent.ACTION_MOVE)
        assertThat(inkViewDispatcher.receivedEvents[2].actionMasked)
            .isEqualTo(MotionEvent.ACTION_UP)

        // Verify annotation dispatcher received no events
        assertThat(annotationsViewDispatcher.wasCalled()).isFalse()
    }

    @Test
    fun dispatchTouchEvent_inHighlighterMode_sendsDownAndUpToBothDispatchers() {
        annotationsTouchEventDispatcher.drawingMode = highlighterMode

        // Dispatch DOWN event
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)

        // Verify both dispatchers received the DOWN event
        assertThat(inkViewDispatcher.callCount()).isEqualTo(1)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)
        assertThat(inkViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_DOWN)
        assertThat(annotationsViewDispatcher.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_DOWN)

        // Dispatch UP event
        val upEvent = createMotionEvent(MotionEvent.ACTION_UP, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(upEvent)

        // Verify both dispatchers received the UP event
        assertThat(inkViewDispatcher.callCount()).isEqualTo(2)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(2)
        assertThat(inkViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_UP)
        assertThat(annotationsViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_UP)
    }

    @Test
    fun dispatchTouchEvent_inHighlighterMode_sendsMoveToInkDispatcherOnlyInitially() {
        annotationsTouchEventDispatcher.drawingMode = highlighterMode

        // Dispatch MOVE event
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent)

        // Verify only ink dispatcher received the MOVE event
        assertThat(inkViewDispatcher.callCount()).isEqualTo(1)
        assertThat(inkViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_MOVE)
        assertThat(annotationsViewDispatcher.wasCalled()).isFalse()
    }

    fun dispatchTouchEvent_inHighlighterMode_switchesToAnnotationDispatcherMidGesture() {
        annotationsTouchEventDispatcher.drawingMode = highlighterMode

        // ACTION_DOWN is sent to both dispatchers.
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)
        assertThat(inkViewDispatcher.callCount()).isEqualTo(1)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)

        // The initial ACTION_MOVE is sent only to the ink dispatcher for the "wet" stroke.
        val moveEvent1 = createMotionEvent(MotionEvent.ACTION_MOVE, 15f, 15f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent1)
        assertThat(inkViewDispatcher.callCount()).isEqualTo(2)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)

        // A "confirm" call happens mid-gesture, switching the active dispatcher to annotations.
        annotationsTouchEventDispatcher.switchActiveDispatcher(
            annotationsViewDispatcher,
            PointF(15f, 15f),
        )

        // Verify inkView received a CANCEL event to stop the wet stroke.
        assertThat(inkViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_CANCEL)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)

        // Subsequent MOVE events go only to the now-active annotation dispatcher.
        val moveEvent2 = createMotionEvent(MotionEvent.ACTION_MOVE, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent2)
        assertThat(inkViewDispatcher.callCount()).isEqualTo(3)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(2)
        assertThat(annotationsViewDispatcher.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_MOVE)
    }

    @Test
    fun dispatchTouchEvent_inEraserMode_sendsEventsToAnnotationDispatcherOnly() {
        annotationsTouchEventDispatcher.drawingMode = eraserMode

        // Dispatch DOWN, MOVE, UP events
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)

        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent)

        val upEvent = createMotionEvent(MotionEvent.ACTION_UP, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(upEvent)

        // Verify annotation dispatcher received all events
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(3)
        assertThat(annotationsViewDispatcher.receivedEvents[0].actionMasked)
            .isEqualTo(MotionEvent.ACTION_DOWN)
        assertThat(annotationsViewDispatcher.receivedEvents[1].actionMasked)
            .isEqualTo(MotionEvent.ACTION_MOVE)
        assertThat(annotationsViewDispatcher.receivedEvents[2].actionMasked)
            .isEqualTo(MotionEvent.ACTION_UP)

        // Verify ink dispatcher received no events
        assertThat(inkViewDispatcher.wasCalled()).isFalse()
    }

    @Test
    fun switchToActiveDispatcher_sendsCancelAndForwardsEvents() {
        annotationsTouchEventDispatcher.drawingMode = highlighterMode

        // Start with a DOWN event - both should receive it
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)
        assertThat(inkViewDispatcher.callCount()).isEqualTo(1)
        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)
        inkViewDispatcher.reset()
        annotationsViewDispatcher.reset()

        // Switch the active dispatcher to the annotationsViewDispatcher
        annotationsTouchEventDispatcher.switchActiveDispatcher(
            annotationsViewDispatcher,
            PointF(15f, 15f),
        )

        // Verify that the inkViewDispatcher received a CANCEL event
        assertThat(inkViewDispatcher.callCount()).isEqualTo(1)
        assertThat(inkViewDispatcher.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_CANCEL)
        assertThat(annotationsViewDispatcher.wasCalled()).isFalse()

        // Now, dispatch a MOVE event. It should only go to the new active dispatcher.
        inkViewDispatcher.reset()
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 20f, 20f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(moveEvent)

        assertThat(annotationsViewDispatcher.callCount()).isEqualTo(1)
        assertThat(annotationsViewDispatcher.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_MOVE)
        assertThat(inkViewDispatcher.wasCalled()).isFalse()
    }

    @Test
    fun switchToActiveDispatcher_doesNothingIfAlreadyActive() {
        annotationsTouchEventDispatcher.drawingMode = penMode // inkViewDispatcher is active

        // Dispatch down to set active dispatcher
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
        annotationsTouchEventDispatcher.dispatchTouchEvent(downEvent)
        inkViewDispatcher.reset() // Clear the down event for clean verification

        // Attempt to switch to the already active dispatcher
        annotationsTouchEventDispatcher.switchActiveDispatcher(inkViewDispatcher, PointF(15f, 15f))

        // No events should have been dispatched
        assertThat(inkViewDispatcher.wasCalled()).isFalse()
        assertThat(annotationsViewDispatcher.wasCalled()).isFalse()
    }

    private fun createMotionEvent(
        action: Int,
        x: Float,
        y: Float,
        metaState: Int = 0,
    ): MotionEvent {
        if (action == MotionEvent.ACTION_DOWN) {
            gestureDownTime = SystemClock.uptimeMillis()
            currentEventTimeInGesture = gestureDownTime
        } else {
            currentEventTimeInGesture += 10
        }

        return MotionEvent.obtain(
            gestureDownTime,
            currentEventTimeInGesture,
            action,
            x,
            y,
            metaState,
        )
    }

    companion object {
        private val eraserMode = AnnotationDrawingMode.EraserMode
        private val penMode = AnnotationDrawingMode.PenMode(5f, Color.BLACK)
        private val highlighterMode =
            AnnotationDrawingMode.HighlighterMode(10f, Color.YELLOW, FakeEditablePdfDocument())
    }
}
