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

package androidx.ui.core

import android.app.Activity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class AndroidPointerInputTest {
    @get:Rule
    val rule = ActivityTestRule<AndroidPointerInputTestActivity>(
        AndroidPointerInputTestActivity::class.java
    )

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: ViewGroup

    @Before
    fun setup() {
        val activity = rule.activity
        container = spy(FrameLayout(activity)).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        rule.runOnUiThread {
            activity.setContentView(container)
        }
    }

    @Test
    fun dispatchTouchEvent_noPointerInputModifiers_returnsFalse() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent(Recomposer.current()) {
                    FillLayout(Modifier
                        .onPositioned { latch.countDown() })
                }
            }
        }

        rule.runOnUiThread {
            androidComposeView = container.getChildAt(0) as AndroidComposeView

            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(0f, 0f))
            )

            // Act
            val actual = androidComposeView.dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Test
    fun dispatchTouchEvent_pointerInputModifier_returnsTrue() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent(Recomposer.current()) {
                    FillLayout(Modifier
                        .consumeMovementGestureFilter()
                        .onPositioned { latch.countDown() })
                }
            }
        }

        rule.runOnUiThread {

            androidComposeView = container.getChildAt(0) as AndroidComposeView

            val locationInWindow = IntArray(2).also {
                androidComposeView.getLocationInWindow(it)
            }

            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(locationInWindow[0].toFloat(), locationInWindow[1].toFloat()))
            )

            // Act
            val actual = androidComposeView.dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isTrue()
        }
    }

    @Test
    fun dispatchTouchEvent_movementNotConsumed_requestDisallowInterceptTouchEventNotCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = false,
            callsRequestDisallowInterceptTouchEvent = false
        )
    }

    @Test
    fun dispatchTouchEvent_movementConsumed_requestDisallowInterceptTouchEventCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = true,
            callsRequestDisallowInterceptTouchEvent = true
        )
    }

    @Test
    fun dispatchTouchEvent_notMeasuredLayoutsAreMeasuredFirst() {
        val size = mutableStateOf(10)
        val latch = CountDownLatch(1)
        var consumedDownPosition: PxPosition? = null
        rule.runOnUiThread {
            container.setContent(Recomposer.current()) {
                Layout(
                    {},
                    Modifier
                        .consumeDownGestureFilter {
                            consumedDownPosition = it
                        }
                        .onPositioned {
                            latch.countDown()
                        }
                ) { _, _, _ ->
                    val sizePx = size.value.ipx
                    layout(sizePx, sizePx) {}
                }
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        rule.runOnUiThread {
            androidComposeView = container.getChildAt(0) as AndroidComposeView

            // we update size from 10 to 20 pixels
            size.value = 20
            // this call will synchronously mark the LayoutNode as needs remeasure
            FrameManager.nextFrame()

            val ownerPosition = androidComposeView.calculatePosition()
            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(ownerPosition.x.value + 15f, ownerPosition.y.value + 15f))
            )

            // we expect it to first remeasure and only then process
            androidComposeView.dispatchTouchEvent(motionEvent)

            assertThat(consumedDownPosition).isEqualTo(PxPosition(15f, 15f))
        }
    }

    private fun dispatchTouchEvent_movementConsumptionInCompose(
        consumeMovement: Boolean,
        callsRequestDisallowInterceptTouchEvent: Boolean
    ) {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent(Recomposer.current()) {
                    FillLayout(Modifier
                        .consumeMovementGestureFilter(consumeMovement)
                        .onPositioned { latch.countDown() })
                }
            }
        }

        rule.runOnUiThread {

            androidComposeView = container.getChildAt(0) as AndroidComposeView
            val (x, y) = IntArray(2).let { array ->
                androidComposeView.getLocationInWindow(array)
                array.map { item -> item.toFloat() }
            }

            val down = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(x, y))
            )

            val move = MotionEvent(
                0,
                MotionEvent.ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(x + 1, y))
            )

            androidComposeView.dispatchTouchEvent(down)

            // Act
            androidComposeView.dispatchTouchEvent(move)

            // Assert
            if (callsRequestDisallowInterceptTouchEvent) {
                verify(container).requestDisallowInterceptTouchEvent(true)
            } else {
                verify(container, never()).requestDisallowInterceptTouchEvent(any())
            }
        }
    }
}

fun Modifier.consumeMovementGestureFilter(consumeMovement: Boolean = false): Modifier = composed {
    val filter = remember(consumeMovement) { ConsumeMovementGestureFilter(consumeMovement) }
    PointerInputModifierImpl(filter)
}

fun Modifier.consumeDownGestureFilter(onDown: (PxPosition) -> Unit): Modifier = composed {
    val filter = remember { ConsumeDownChangeFilter() }
    filter.onDown = onDown
    this + PointerInputModifierImpl(filter)
}

private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

private class ConsumeMovementGestureFilter(val consumeMovement: Boolean) : PointerInputFilter() {
    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ) =
        if (consumeMovement) {
            changes.map { it.consumePositionChange(
                it.positionChange().x,
                it.positionChange().y)
            }
        } else {
            changes
        }

    override fun onCancel() {}
}

private class ConsumeDownChangeFilter : PointerInputFilter() {
    var onDown by mutableStateOf<(PxPosition) -> Unit>({})
    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ) = changes.map {
        if (it.changedToDown()) {
            onDown(it.current.position!!)
            it.consumeDownChange()
        } else {
            it
        }
    }

    override fun onCancel() {}
}

@Composable
fun FillLayout(modifier: Modifier = Modifier) {
    Layout(emptyContent(), modifier) { _, constraints, _ ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

fun countDown(block: (CountDownLatch) -> Unit) {
    val countDownLatch = CountDownLatch(1)
    block(countDownLatch)
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
}

class AndroidPointerInputTestActivity : Activity()
