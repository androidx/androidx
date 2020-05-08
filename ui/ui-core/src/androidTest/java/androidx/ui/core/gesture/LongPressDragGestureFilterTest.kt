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
import androidx.compose.emptyContent
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// TODO(shepshapard): Figure out how to test composite gesture detectors better.  There should be
//  more tests, but hopefully they can also be easier to write then what is currently available.
//  Should possibly wait till we have host side testing that does not require the Android runtime.

@LargeTest
@RunWith(JUnit4::class)
class LongPressDragGestureFilterTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var longPressDragObserver: LongPressDragObserver
    private lateinit var longPressCountDownLatch: CountDownLatch
    private lateinit var view: View

    @Before
    fun setup() {
        longPressDragObserver = spy(MyLongPressDragObserver {
            longPressCountDownLatch.countDown()
        })

        val activity = activityTestRule.activity
        assertTrue("timed out waiting for activity focus",
            activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Stack {
                    Layout(
                        modifier = Modifier.longPressDragGestureFilter(longPressDragObserver),
                        measureBlock = { _, _, _ ->
                            layout(100.ipx, 100.ipx) {
                                setupLatch.countDown()
                            }
                        }, children = emptyContent()
                    )
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue("timed out waiting for setup completion",
            setupLatch.await(1000, TimeUnit.SECONDS))
    }

    @Test
    fun ui_downMoveUpBeforeLongPressTimeout_noCallbacksCalled() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        val up = MotionEvent(
            0,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }

        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downMoveCancelBeforeLongPressTimeout_noCallbacksCalled() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val move = MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        val cancel = MotionEvent(
            0,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(cancel)
        }

        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPress_onlyOnLongPressCalled() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }

        verify(longPressDragObserver).onLongPress(any())
        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPressMove_callbacksCorrect() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }
        val move = MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(move)

        inOrder(longPressDragObserver) {
            verify(longPressDragObserver).onLongPress(any())
            verify(longPressDragObserver).onDragStart()
            // Twice because DragGestureDetector dispatches onDrag during 2 passes.
            verify(longPressDragObserver, times(2)).onDrag(any())
        }
        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPressUp_callbacksCorrect() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }
        val up = MotionEvent(
            0,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(up)

        // Assert.
        inOrder(longPressDragObserver) {
            verify(longPressDragObserver).onLongPress(any())
            verify(longPressDragObserver).onStop(any())
        }
        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPressMoveUp_callbacksCorrect() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }
        val move = MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(move)
        val up = MotionEvent(
            0,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(up)

        inOrder(longPressDragObserver) {
            verify(longPressDragObserver).onLongPress(any())
            verify(longPressDragObserver).onDragStart()
            // Twice because DragGestureDetector dispatches onDrag during 2 passes.
            verify(longPressDragObserver, times(2)).onDrag(any())
            verify(longPressDragObserver).onStop(any())
        }
        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPressCancel_callbacksCorrect() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }
        val cancel = MotionEvent(
            0,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        view.dispatchTouchEvent(cancel)

        inOrder(longPressDragObserver) {
            verify(longPressDragObserver).onLongPress(any())
            verify(longPressDragObserver).onCancel()
        }
        verifyNoMoreInteractions(longPressDragObserver)
    }

    @Test
    fun ui_downWaitForLongPressMoveCancel_callbacksCorrect() {

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        waitForLongPress {
            view.dispatchTouchEvent(down)
        }
        val move = MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(move)
        val cancel = MotionEvent(
            0,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(51f, 50f))
        )
        view.dispatchTouchEvent(cancel)

        inOrder(longPressDragObserver) {
            verify(longPressDragObserver).onLongPress(any())
            verify(longPressDragObserver).onDragStart()
            // Twice because DragGestureDetector dispatches onDrag during 2 passes.
            verify(longPressDragObserver, times(2)).onDrag(any())
            verify(longPressDragObserver).onCancel()
        }
        verifyNoMoreInteractions(longPressDragObserver)
    }

    private fun waitForLongPress(block: () -> Unit) {
        longPressCountDownLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR(block)
        assertTrue("timed out waiting for long press",
            longPressCountDownLatch.await(750, TimeUnit.MILLISECONDS))
    }
}

@Suppress("RedundantOverride")
open class MyLongPressDragObserver(val onLongPress: () -> Unit) : LongPressDragObserver {
    override fun onLongPress(pxPosition: PxPosition) {
        onLongPress()
    }

    override fun onDragStart() {}

    override fun onDrag(dragDistance: PxPosition): PxPosition {
        return super.onDrag(dragDistance)
    }

    override fun onStop(velocity: PxPosition) {}
}