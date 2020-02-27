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

package androidx.ui.test

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.PointerInput
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.util.PointerInputRecorder
import androidx.ui.test.util.assertOnlyLastEventIsUp
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.unit.dp
import com.google.common.collect.Ordering
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SendSwipeTest {

    private val tag = "widget"

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private lateinit var recorder: PointerInputRecorder

    @Before
    fun setup() {
        recorder = PointerInputRecorder()
    }

    @Composable
    fun Ui(alignment: Modifier) {
        Stack(LayoutSize.Fill + alignment) {
            Semantics(container = true, properties = { testTag = tag }) {
                PointerInput(
                    pointerInputHandler = recorder::onPointerInput,
                    cancelHandler = {}
                ) {
                    Box(LayoutSize(100.dp), backgroundColor = Color.Yellow)
                }
            }
        }
    }

    @Test
    fun swipeUp() {
        composeTestRule.setContent { Ui(LayoutAlign.TopStart) }
        findByTag(tag).doGesture { sendSwipeUp() }
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsUp()
            }
        }
    }

    @Test
    fun swipeDown() {
        composeTestRule.setContent { Ui(LayoutAlign.TopEnd) }
        findByTag(tag).doGesture { sendSwipeDown() }
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsDown()
            }
        }
    }

    @Test
    fun swipeLeft() {
        composeTestRule.setContent { Ui(LayoutAlign.BottomEnd) }
        findByTag(tag).doGesture { sendSwipeLeft() }
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsLeft()
            }
        }
    }

    @Test
    fun swipeRight() {
        composeTestRule.setContent { Ui(LayoutAlign.BottomStart) }
        findByTag(tag).doGesture { sendSwipeRight() }
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsRight()
            }
        }
    }
}

private fun PointerInputRecorder.assertSwipeIsUp() {
    // Must have at least two events to have a direction
    assertThat(events.size).isAtLeast(2)
    // Last event must be above first event
    assertThat(events.last().position.y).isLessThan(events.first().position.y)
    // All events in between only move up
    events.map { it.position.x.value }.assertSame(tolerance = 0.001f)
    events.map { it.position.y }.assertDecreasing()
}

private fun PointerInputRecorder.assertSwipeIsDown() {
    // Must have at least two events to have a direction
    assertThat(events.size).isAtLeast(2)
    // Last event must be below first event
    assertThat(events.last().position.y).isGreaterThan(events.first().position.y)
    // All events in between only move down
    events.map { it.position.x.value }.assertSame(tolerance = 0.001f)
    events.map { it.position.y }.assertIncreasing()
}

private fun PointerInputRecorder.assertSwipeIsLeft() {
    // Must have at least two events to have a direction
    assertThat(events.size).isAtLeast(2)
    // Last event must be to the left of first event
    assertThat(events.last().position.x).isLessThan(events.first().position.x)
    // All events in between only move to the left
    events.map { it.position.x }.assertDecreasing()
    events.map { it.position.y.value }.assertSame(tolerance = 0.001f)
}

private fun PointerInputRecorder.assertSwipeIsRight() {
    // Must have at least two events to have a direction
    assertThat(events.size).isAtLeast(2)
    // Last event must be to the right of first event
    assertThat(events.last().position.x).isGreaterThan(events.first().position.x)
    // All events in between only move to the right
    events.map { it.position.x }.assertIncreasing()
    events.map { it.position.y.value }.assertSame(tolerance = 0.001f)
}

private fun List<Float>.assertSame(tolerance: Float = 0f) {
    if (size <= 1) {
        return
    }
    val baseValue = first()
    assertThat(min()).isWithin(tolerance).of(baseValue)
    assertThat(max()).isWithin(tolerance).of(baseValue)
}

private fun <E : Comparable<E>> List<E>.assertIncreasing() {
    assertThat(this).isInOrder(Ordering.natural<E>())
}

private fun <E : Comparable<E>> List<E>.assertDecreasing() {
    assertThat(this).isInOrder(Ordering.natural<E>().reverse<E>())
}
