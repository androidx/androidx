/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.slidingpanelayout.widget

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DraggableDividerHandlerTest {
    @Test
    fun userResizeLifecycleEvents() {
        userResizeLifecycleEvents(cancelAtEnd = false)
        userResizeLifecycleEvents(cancelAtEnd = true)
    }

    private fun userResizeLifecycleEvents(cancelAtEnd: Boolean) =
        with(ExpectCounter()) {
            object : SlidingPaneLayout.AbsDraggableDividerHandler(0) {
                    override fun dividerBoundsContains(x: Int, y: Int): Boolean = true

                    override fun onUserResizeStarted() {
                        expect(2)
                        assertWithMessage("isDragging when started").that(isDragging).isTrue()
                        assertWithMessage("drag position when started")
                            .that(dragPositionX)
                            .isEqualTo(10)
                    }

                    override fun onUserResizeProgress() {
                        expect(4)
                        assertWithMessage("onUserResizeProgress isDragging")
                            .that(isDragging)
                            .isTrue()
                        assertWithMessage("onUserResizeProgress position")
                            .that(dragPositionX)
                            .isEqualTo(5)
                    }

                    override fun onUserResizeComplete(wasCancelled: Boolean) {
                        expect(6)
                        assertWithMessage("onUserResizeComplete isDragging")
                            .that(isDragging)
                            .isFalse()
                        assertWithMessage("onUserResizeComplete position still had final value")
                            .that(dragPositionX)
                            .isEqualTo(5)
                        assertWithMessage("onUserResizeComplete wasCancelled")
                            .that(wasCancelled)
                            .isEqualTo(cancelAtEnd)
                    }
                }
                .test {
                    expect(1)
                    down(10f, 10f)
                    expect(3)
                    moveTo(5f, 10f)
                    expect(5)
                    if (cancelAtEnd) cancel() else up()
                    expect(7)
                }
        }

    @Test
    fun requiresDividerBoundsCheckOnDown() {
        object : SlidingPaneLayout.AbsDraggableDividerHandler(0) {
                override fun dividerBoundsContains(x: Int, y: Int): Boolean = false

                override fun onUserResizeStarted() {
                    fail("unexpected user resize event")
                }
            }
            .test {
                expectOnTouchReturns = false
                down(0f, 0f)
            }
    }

    @Test
    fun clampDragPosition() {
        object : SlidingPaneLayout.AbsDraggableDividerHandler(0) {
                override fun dividerBoundsContains(x: Int, y: Int): Boolean = true

                override fun clampDraggingDividerPosition(proposedPositionX: Int): Int =
                    proposedPositionX.coerceIn(5, 10)
            }
            .test {
                down(7f, 10f)
                moveTo(0f, 10f)
                assertWithMessage("position after move to 0, 10")
                    .that(draggableDividerHandler.dragPositionX)
                    .isEqualTo(5)
                moveTo(15f, 10f)
                assertWithMessage("position after move to 15, 10")
                    .that(draggableDividerHandler.dragPositionX)
                    .isEqualTo(10)
            }
    }

    @Test
    fun ignoreInvalidEventStreams() {
        ExpectNoResizeEventsDividerHandler().test {
            expectOnTouchReturns = false
            moveTo(5f, 5f)
            up()
            cancel()
        }
    }

    @Test
    fun touchSlop() {
        ExpectNoResizeEventsDividerHandler(10).test {
            down(25f, 0f)
            moveTo(34f, 0f)
            up()
        }

        object : SlidingPaneLayout.AbsDraggableDividerHandler(10) {
                override fun dividerBoundsContains(x: Int, y: Int): Boolean = true
            }
            .test {
                down(25f, 0f)
                moveTo(34f, 0f)
                assertWithMessage("isDragging before slop")
                    .that(draggableDividerHandler.isDragging)
                    .isFalse()
                moveTo(35f, 0f)
                assertWithMessage("isDragging after slop")
                    .that(draggableDividerHandler.isDragging)
                    .isTrue()
                up()

                down(25f, 0f)
                moveTo(16f, 0f)
                assertWithMessage("isDragging before slop")
                    .that(draggableDividerHandler.isDragging)
                    .isFalse()
                moveTo(15f, 0f)
                assertWithMessage("isDragging after slop")
                    .that(draggableDividerHandler.isDragging)
                    .isTrue()
            }
    }

    @Test
    fun interceptTouchEvents() {
        object : SlidingPaneLayout.AbsDraggableDividerHandler(10) {
                override fun dividerBoundsContains(x: Int, y: Int): Boolean = true
            }
            .test {
                performInterceptTouchEvent(downEvent(25f, 0f), false)
                val interceptedMove = moveEvent(35f, 0f)
                performInterceptTouchEvent(interceptedMove, true)
                assertWithMessage("isDragging after onInterceptTouchEvent move")
                    .that(isDragging)
                    .isTrue()
            }
    }
}

private open class ExpectNoResizeEventsDividerHandler(touchSlop: Int = 0) :
    SlidingPaneLayout.AbsDraggableDividerHandler(touchSlop) {
    override fun dividerBoundsContains(x: Int, y: Int): Boolean = true

    override fun onUserResizeStarted() {
        fail("started user resize")
    }

    override fun onUserResizeProgress() {
        fail("user resize progress")
    }

    override fun onUserResizeComplete(wasCancelled: Boolean) {
        fail("user resize complete")
    }
}

private class ExpectCounter {
    var count: Int = 0
        private set

    fun expect(expectedCount: Int) {
        assertWithMessage("Ordered operation $expectedCount").that(expectedCount).isEqualTo(++count)
    }
}

/** Create a test [MotionEvent]; this will have bogus time values, no history */
private fun motionEvent(
    action: Int,
    x: Float,
    y: Float,
) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

private fun downEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_DOWN, x, y)

private fun moveEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_MOVE, x, y)

private fun upEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_UP, x, y)

private fun cancelEvent() = motionEvent(MotionEvent.ACTION_CANCEL, 0f, 0f)

private inline fun SlidingPaneLayout.AbsDraggableDividerHandler.test(
    block: DraggableDividerHandlerTester.() -> Unit
) {
    DraggableDividerHandlerTester(this).apply(block)
}

@SuppressLint("NewApi") // requires 19; migration in progress
private class DraggableDividerHandlerTester(
    val draggableDividerHandler: SlidingPaneLayout.AbsDraggableDividerHandler
) {
    var expectOnTouchReturns = true

    var lastAction: Int = -1
        private set

    fun lastActionToString() = MotionEvent.actionToString(lastAction)

    var lastX: Float = Float.NaN
        private set

    var lastY: Float = Float.NaN
        private set

    val isDragging: Boolean
        get() = draggableDividerHandler.isDragging

    fun down(x: Float, y: Float) {
        performTouchEvent(downEvent(x, y))
    }

    fun moveTo(x: Float, y: Float) {
        performTouchEvent(moveEvent(x, y))
    }

    fun up(x: Float, y: Float) {
        performTouchEvent(upEvent(x, y))
    }

    fun up() = up(lastX, lastY)

    fun cancel() {
        performTouchEvent(cancelEvent())
    }

    fun performInterceptTouchEvent(event: MotionEvent, expectReturn: Boolean) {
        assertWithMessage("onInterceptTouchEvent(${MotionEvent.actionToString(event.action)})")
            .that(draggableDividerHandler.onInterceptTouchEvent(event))
            .isEqualTo(expectReturn)
    }

    fun performTouchEvent(event: MotionEvent) {
        lastAction = event.action
        lastX = event.x
        lastY = event.y
        assertWithMessage("onTouchEvent(${MotionEvent.actionToString(event.action)}) return value")
            .that(draggableDividerHandler.onTouchEvent(event))
            .isEqualTo(expectOnTouchReturns)
    }
}
