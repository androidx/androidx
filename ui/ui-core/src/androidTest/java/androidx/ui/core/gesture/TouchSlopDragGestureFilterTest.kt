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
package androidx.ui.core.gesture

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.layout.Stack
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(JUnit4::class)
class TouchSlopDragGestureFilterTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var dragObserver: DragObserver
    private lateinit var view: View
    private var touchSlop: Float = Float.NEGATIVE_INFINITY

    private val TinyNum = .01f

    @Test
    fun ui_pointerMovementWithinTouchSlop_noCallbacksCalled() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop - TinyNum, 50f))
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop - TinyNum, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_pointerDownMovementBeyondTouchSlopUp_correctCallbacksInOrder() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f))
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }

        dragObserver.inOrder {
            verify().onStart(PxPosition(50.ipx, 50.ipx))
            // Twice because RawDragGestureDetector calls the callback on both postUp and postDown
            // and nothing consumes the drag distance.
            verify(dragObserver, times(2)).onDrag(any())
            verify().onStop(any())
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_pointerDownMovementBeyondTouchSlopCancel_correctCallbacksInOrder() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f))
        )
        val cancel = MotionEvent(
            30,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(cancel)
        }

        dragObserver.inOrder {
            verify().onStart(PxPosition(50.ipx, 50.ipx))
            // Twice because RawDragGestureDetector calls the callback on both postUp and postDown
            // and nothing consumes the drag distance.
            verify(dragObserver, times(2)).onDrag(any())
            verify().onCancel()
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_startDragImmediatelyTrueDown_onStartOnlyCalled() {
        setup(true)

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
        }

        dragObserver.inOrder {
            verify().onStart(PxPosition(50.ipx, 50.ipx))
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_movement_onDragCalledWithCorrectValue() {
        setup(false)

        // Guaranteed to be over slop
        val movement = (touchSlop + 1 * 2).toInt()

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + movement, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
        }

        verify(dragObserver, times(2)).onDrag(PxPosition(movement.toFloat(), 0.0f))
    }

    private fun setup(startDragImmediately: Boolean) {
        dragObserver = spy(MyDragObserver())

        val activity = activityTestRule.activity
        assertTrue(activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Stack {
                    touchSlop = with(DensityAmbient.current) { TouchSlop.toPx() }
                    Layout(
                        modifier = Modifier.dragGestureFilter(
                            dragObserver,
                            startDragImmediately = startDragImmediately
                        ),
                        measureBlock = { _, _, _ ->
                            layout(100.ipx, 100.ipx) {
                                setupLatch.countDown()
                            }
                        }, children = {}
                    )
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue(setupLatch.await(1000, TimeUnit.SECONDS))
    }
}

@Suppress("RedundantOverride")
open class MyDragObserver : DragObserver {
    override fun onStart(downPosition: PxPosition) {
        super.onStart(downPosition)
    }

    override fun onDrag(dragDistance: PxPosition): PxPosition {
        return super.onDrag(dragDistance)
    }

    override fun onStop(velocity: PxPosition) {
        super.onStop(velocity)
    }
}
