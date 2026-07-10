/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.junit.Test

@LargeTest
class TouchScrollable2DTest : Scrollable2DInputTest() {
    override val testFlingBehavior: Boolean
        get() = true

    override val flingVelocityComparisonFactor: Float
        get() = 0.005f

    override fun SemanticsNodeInteraction.performScrollGesture(
        start: InjectionScope.() -> Offset,
        delta: InjectionScope.() -> Offset,
        durationMillis: Long,
        preventFling: Boolean,
        also: InjectionScope.() -> Unit,
    ) {
        performTouchInput {
            val start = this.start()
            val delta = this.delta()
            if (preventFling) {
                down(start)
                moveBy(delta)
                advanceEventTime(3000L) // Prevents fling
                up()
            } else {
                this.swipe(start = start, end = start + delta, durationMillis = durationMillis)
            }

            also()
        }
    }

    override fun SemanticsNodeInteraction.performScrollGestureWithVelocity(
        start: InjectionScope.() -> Offset,
        delta: InjectionScope.() -> Offset,
        endVelocity: Float,
        durationMillis: Long?,
        also: InjectionScope.() -> Unit,
    ) {
        performTouchInput {
            val start = this.start()
            val delta = this.delta()
            if (durationMillis == null) {
                swipeWithVelocity(start = start, end = start + delta, endVelocity = endVelocity)
            } else {
                swipeWithVelocity(
                    start = start,
                    end = start + delta,
                    endVelocity = endVelocity,
                    durationMillis = durationMillis,
                )
            }

            also()
        }
    }

    @Test
    fun scrollable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        setScrollable2DContent {
            Modifier.scrollable2D(interactionSource = interactionSource, state = scrollable2DState)
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        rule.setContentAndGetScope {
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollable2DBoxTag)
                                .size(100.dp)
                                .scrollable2D(
                                    interactionSource = interactionSource,
                                    state = scrollable2DState,
                                )
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose scrollable
        rule.runOnIdle { emitScrollableBox = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    /**
     * This test is not in [Scrollable2DInputTest] because trackpad needs its own implementation due
     * to never becoming idle during a pan gesture. See
     * [TrackpadScrollable2DTest.scrollable_flingBehaviourCalled_correctScope].
     */
    @Test
    fun scrollable_flingBehaviourCalled_correctScope() {
        var total = Offset.Zero
        var returned = 0f
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    returned = scrollBy(123f)
                    return 0f
                }
            }
        setScrollable2DContent {
            Modifier.scrollable2D(state = scrollable2DState, flingBehavior = flingBehaviour)
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 100f))
        }

        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                total
            }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            moveBy(Offset(x = 100f, y = 100f))
            up()
        }

        rule.runOnIdle {
            assertThat(total.x).isWithin(1f).of(prevTotal.x + (123 * 0.7f) + 100f)
            assertThat(total.y).isWithin(1f).of(prevTotal.y + (123 * 0.7f) + 100f)
            assertThat(returned.roundToInt()).isEqualTo(123)
        }
    }

    /**
     * This test is not in [Scrollable2DInputTest] because trackpad and mouse have a different
     * velocity tracker and a different way to collect input events.
     *
     * See
     * [MouseWheelScrollable2DTest.scrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker]
     * and
     * [TrackpadScrollable2DTest.scrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker]
     */
    @Test
    fun scrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection =
            object : NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    velocity += available
                    return Velocity.Zero
                }
            }
        val scrollable2DState = Scrollable2DState { _ -> Offset.Zero }

        setScrollable2DContent {
            Modifier.pointerInput(Unit) { savePointerInputEvents(tracker, this) }
                .nestedScroll(capturingScrollConnection)
                .scrollable2D(scrollable2DState)
        }

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performScrollLeftGesture()

        // assert
        rule.runOnIdle {
            val diff = (velocity - tracker.calculateVelocity()).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performScrollRightGesture()

        // assert
        rule.runOnIdle {
            val diff = (velocity - tracker.calculateVelocity()).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease() =
        scrollable_noMomentum_shouldChangeScrollStateAfterRelease { delta ->
            var previousScrollValue = 0f
            performTouchInput {
                down(center)
                // generate various move events
                repeat(30) {
                    moveBy(Offset(delta, delta), delayMillis = 8L)
                    previousScrollValue += delta.toInt()
                }
                advanceEventTime(3000L) // Prevent fling gesture.
                up()
            }
            Offset(previousScrollValue, previousScrollValue)
        }
}
