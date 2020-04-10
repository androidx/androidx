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
import androidx.compose.emptyContent
import androidx.compose.remember
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.unit.IntPxSize
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
                container.setContent {
                    FillLayout(Modifier
                        .onPositioned { latch.countDown() })
                }
            }
        }

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

    @Test
    fun dispatchTouchEvent_pointerInputModifier_returnsTrue() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(Modifier
                        .consumeMovementGestureFilter()
                        .onPositioned { latch.countDown() })
                }
            }
        }

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

    private fun dispatchTouchEvent_movementConsumptionInCompose(
        consumeMovement: Boolean,
        callsRequestDisallowInterceptTouchEvent: Boolean
    ) {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(Modifier
                        .consumeMovementGestureFilter(consumeMovement)
                        .onPositioned { latch.countDown() })
                }
            }
        }

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

@Composable
fun Modifier.consumeMovementGestureFilter(consumeMovement: Boolean = false): Modifier {
    val filter = remember { ConsumeMovementGestureFilter(consumeMovement) }
    return this + PointerInputModifierImpl(filter)
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
            changes.map { it.consumePositionChange(it.positionChange().x, it.positionChange().y) }
        } else {
            changes
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
