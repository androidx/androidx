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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.DifferentialVelocityTracker
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.pan
import androidx.compose.ui.test.panWithVelocity
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.unit.Velocity
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import org.junit.Test

@LargeTest
class TrackpadScrollable2DTest : Scrollable2DInputTest() {
    override val testFlingBehavior: Boolean
        get() = true

    override val flingVelocityComparisonFactor: Float
        get() = 0.05f

    override fun SemanticsNodeInteraction.performScrollGesture(
        start: InjectionScope.() -> Offset,
        delta: InjectionScope.() -> Offset,
        durationMillis: Long,
        preventFling: Boolean,
        also: InjectionScope.() -> Unit,
    ) {
        performTrackpadInput {
            val delta = this.delta()
            if (preventFling) {
                panStart()
                panMoveBy(delta = delta, delayMillis = durationMillis)
                panEnd(3000) // Prevents fling
            } else {
                val durationFloat = durationMillis.toFloat()
                pan(
                    curve = { lerp(Offset.Zero, delta, it / durationFloat) },
                    durationMillis = durationMillis,
                )
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
        performTrackpadInput {
            val offset = this.delta()
            if (durationMillis == null) {
                panWithVelocity(offset = offset, endVelocity = endVelocity)
            } else {
                panWithVelocity(
                    offset = offset,
                    endVelocity = endVelocity,
                    durationMillis = durationMillis,
                )
            }
            also()
        }
    }

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

        rule.onNodeWithTag(scrollable2DBoxTag).performTrackpadInput {
            panStart()
            panMoveBy(Offset(x = 100f, y = 100f))
        }

        // Trackpad processing in TrackpadScrollingLogic uses Channel.busyReceive() which
        // prevents the state from being idle, so we can't waitForIdle
        assertThat(total.x).isGreaterThan(0f)
        assertThat(total.y).isGreaterThan(0f)
        val prevTotal = total

        // Can't wait for idle, as explained above
        rule.runWithoutImplicitWait {
            rule.onNodeWithTag(scrollable2DBoxTag).performTrackpadInput {
                panMoveBy(Offset(x = 100f, y = 100f))
                panEnd()
            }
        }

        rule.runOnIdle {
            assertThat(total.x).isWithin(1f).of(prevTotal.x + (123 * 0.7f) + 100f)
            assertThat(total.y).isWithin(1f).of(prevTotal.y + (123 * 0.7f) + 100f)
            assertThat(returned.roundToInt()).isEqualTo(123)
        }
    }

    // Trackpad events are only supported from API 34.
    // In earlier APIs they are converted to touch events, but this is already covered by a similar
    // test in TouchScrollable2DTest.
    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun scrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = DifferentialVelocityTracker()
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
            Modifier.pointerInput(Unit) { saveTrackpadInputEvents(tracker, this) }
                .nestedScroll(capturingScrollConnection)
                .scrollable2D(scrollable2DState)
        }

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performScrollLeftGesture()

        // assert
        rule.runOnIdle {
            val outsideVelocity = -tracker.calculateVelocity()
            val diff = (velocity - outsideVelocity).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performScrollRightGesture()

        // assert
        rule.runOnIdle {
            val outsideVelocity = -tracker.calculateVelocity()
            val diff = (velocity - outsideVelocity).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease() =
        scrollable_noMomentum_shouldChangeScrollStateAfterRelease { delta ->
            var previousScrollValue = 0f
            performTrackpadInput {
                panStart()
                // generate various move events
                repeat(30) {
                    panMoveBy(Offset(delta, delta), delayMillis = 8L)
                    previousScrollValue += delta.toInt()
                }
                advanceEventTime(3000L) // Prevent fling gesture.
                panEnd()
            }
            Offset(previousScrollValue, previousScrollValue)
        }
}
