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

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfContentLayoutTouchListenerTest {
    private lateinit var listener: PdfContentLayoutTouchListener
    private lateinit var view: View
    private lateinit var context: Context

    private var gestureDownTime = 0L
    private var currentEventTimeInGesture = 0L

    private lateinit var wetStrokesViewEventTracker: TestTouchEventDispatcher
    private lateinit var pdfViewEventTracker: TestTouchEventDispatcher

    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        gestureDownTime = System.currentTimeMillis()
        currentEventTimeInGesture = gestureDownTime

        wetStrokesViewEventTracker = TestTouchEventDispatcher()
        pdfViewEventTracker = TestTouchEventDispatcher()

        listener =
            PdfContentLayoutTouchListener(context, wetStrokesViewEventTracker, pdfViewEventTracker)
    }

    @Test
    fun onTouch_actionDown_dispatchesToWetStrokesViewOnly() {
        val downEvent =
            createMotionEvent(MotionEvent.ACTION_DOWN, listOf(PointerInfo(0, PointF(10f, 10f))))
        listener.onTouch(view, downEvent)

        assertThat(wetStrokesViewEventTracker.callCount()).isEqualTo(1)
        assertThat(wetStrokesViewEventTracker.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_DOWN)
        assertThat(pdfViewEventTracker.wasCalled()).isFalse()
    }

    @Test
    fun onTouch_singleTouchMovePastSlop_commitsAndDispatchesMove() {
        listener.onTouch(
            view,
            createMotionEvent(MotionEvent.ACTION_DOWN, listOf(PointerInfo(0, PointF(10f, 10f)))),
        )
        wetStrokesViewEventTracker.reset()

        val moveEvent =
            createMotionEvent(
                MotionEvent.ACTION_MOVE,
                listOf(PointerInfo(0, PointF(10f + touchSlop + 1, 10f))),
            )
        listener.onTouch(view, moveEvent)

        assertThat(wetStrokesViewEventTracker.callCount()).isEqualTo(1)
        assertThat(wetStrokesViewEventTracker.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_MOVE)
        assertThat(pdfViewEventTracker.wasCalled()).isFalse()
    }

    @Test
    fun onTouch_pointerDownBeforeCommit_switchesToPdfView() {
        listener.onTouch(
            view,
            createMotionEvent(MotionEvent.ACTION_DOWN, listOf(PointerInfo(0, PointF(10f, 10f)))),
        )
        wetStrokesViewEventTracker.reset()

        val pointerDownEvent =
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                listOf(PointerInfo(0, PointF(10f, 10f)), PointerInfo(1, PointF(50f, 50f))),
                1,
            )
        listener.onTouch(view, pointerDownEvent)

        assertThat(wetStrokesViewEventTracker.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_CANCEL)

        assertThat(pdfViewEventTracker.callCount()).isEqualTo(2)
        assertThat(pdfViewEventTracker.receivedEvents[0].actionMasked)
            .isEqualTo(MotionEvent.ACTION_DOWN)
        assertThat(pdfViewEventTracker.receivedEvents[1].actionMasked)
            .isEqualTo(MotionEvent.ACTION_POINTER_DOWN)
    }

    @Test
    fun onTouch_pointerDownAfterCommit_staysOnWetStrokesView() {
        listener.onTouch(
            view,
            createMotionEvent(MotionEvent.ACTION_DOWN, listOf(PointerInfo(0, PointF(10f, 10f)))),
        )
        listener.onTouch(
            view,
            createMotionEvent(
                MotionEvent.ACTION_MOVE,
                listOf(PointerInfo(0, PointF(10f + touchSlop + 1, 10f))),
            ),
        )
        wetStrokesViewEventTracker.reset()
        pdfViewEventTracker.reset()

        val pointerDownEvent =
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                listOf(PointerInfo(0, PointF(10f, 10f)), PointerInfo(1, PointF(50f, 50f))),
                1,
            )
        listener.onTouch(view, pointerDownEvent)

        assertThat(wetStrokesViewEventTracker.callCount()).isEqualTo(1)
        assertThat(wetStrokesViewEventTracker.lastReceivedAction())
            .isEqualTo(MotionEvent.ACTION_POINTER_DOWN)
        assertThat(pdfViewEventTracker.wasCalled()).isFalse()
    }

    @Test
    fun onTouch_primaryPointerUpInCommittedDraw_sendsUpAndResets() {
        listener.onTouch(
            view,
            createMotionEvent(MotionEvent.ACTION_DOWN, listOf(PointerInfo(0, PointF(10f, 10f)))),
        )
        listener.onTouch(
            view,
            createMotionEvent(
                MotionEvent.ACTION_MOVE,
                listOf(PointerInfo(0, PointF(10f + touchSlop + 1, 10f))),
            ),
        )
        listener.onTouch(
            view,
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                listOf(PointerInfo(0, PointF(10f, 10f)), PointerInfo(1, PointF(50f, 50f))),
                1,
            ),
        )
        wetStrokesViewEventTracker.reset()

        val primaryUpEvent =
            createMotionEvent(
                MotionEvent.ACTION_POINTER_UP,
                listOf(PointerInfo(0, PointF(10f, 10f)), PointerInfo(1, PointF(50f, 50f))),
                0,
            )
        listener.onTouch(view, primaryUpEvent)

        assertThat(wetStrokesViewEventTracker.callCount()).isEqualTo(1)
        assertThat(wetStrokesViewEventTracker.lastReceivedAction()).isEqualTo(MotionEvent.ACTION_UP)
        assertThat(pdfViewEventTracker.wasCalled()).isFalse()
    }

    private fun createMotionEvent(
        action: Int,
        pointers: List<PointerInfo>,
        actionPointerIndex: Int = 0,
    ): MotionEvent {
        currentEventTimeInGesture += 10

        val pointerProperties =
            Array(pointers.size) { MotionEvent.PointerProperties().apply { id = pointers[it].id } }
        val pointerCoords =
            Array(pointers.size) {
                MotionEvent.PointerCoords().apply {
                    x = pointers[it].coords.x
                    y = pointers[it].coords.y
                }
            }

        val effectiveAction =
            when (action) {
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_POINTER_UP ->
                    action or (actionPointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                else -> action
            }

        return MotionEvent.obtain(
            gestureDownTime,
            currentEventTimeInGesture,
            effectiveAction,
            pointers.size,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1.0f,
            1.0f,
            0,
            0,
            android.view.InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )
    }

    private class TestTouchEventDispatcher : TouchEventDispatcher {
        val receivedEvents = mutableListOf<MotionEvent>()

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            receivedEvents.add(MotionEvent.obtain(event))
            return true
        }

        fun reset() {
            receivedEvents.forEach { it.recycle() }
            receivedEvents.clear()
        }

        fun lastReceivedAction(): Int? = receivedEvents.lastOrNull()?.actionMasked

        fun wasCalled(): Boolean = receivedEvents.isNotEmpty()

        fun callCount(): Int = receivedEvents.size
    }
}

private data class PointerInfo(val id: Int, val coords: PointF)
