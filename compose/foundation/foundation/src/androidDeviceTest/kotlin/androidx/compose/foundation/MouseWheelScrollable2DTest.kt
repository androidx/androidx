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

import android.app.Activity
import androidx.compose.foundation.gestures.DifferentialVelocityTracker
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.platformScrollConfig
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.Velocity
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import org.junit.Test

@LargeTest
class MouseWheelScrollable2DTest : Scrollable2DInputTest() {
    override val testFlingBehavior: Boolean
        get() = false

    override val flingVelocityComparisonFactor: Float
        get() = error("Fling is not supported for mouse wheel")

    override fun SemanticsNodeInteraction.performScrollGestureWithVelocity(
        start: InjectionScope.() -> Offset,
        delta: InjectionScope.() -> Offset,
        endVelocity: Float,
        durationMillis: Long?,
        also: InjectionScope.() -> Unit,
    ) {
        error("Mouse wheel cannot be scrolled with velocity")
    }

    private val scrollConfig by lazy {
        @Suppress("UNCHECKED_CAST")
        val context = (rule as AndroidComposeTestRule<*, *>).activity as Activity
        platformScrollConfig(context)
    }

    override fun SemanticsNodeInteraction.performScrollGesture(
        start: InjectionScope.() -> Offset,
        delta: InjectionScope.() -> Offset,
        durationMillis: Long,
        preventFling: Boolean,
        also: InjectionScope.() -> Unit,
    ) {
        performMouseInput { performMouseScrollGesture(delta = delta, also = also) }
    }

    private fun MouseInjectionScope.performMouseScrollGesture(
        delta: InjectionScope.() -> Offset,
        also: InjectionScope.() -> Unit = {},
    ) {
        val verticalFactor = with(scrollConfig) { getVerticalScrollFactor() }
        val horizontalFactor = with(scrollConfig) { getHorizontalScrollFactor() }
        val delta = this.delta()
        val deltaToScroll = Offset(x = -delta.x / horizontalFactor, y = -delta.y / verticalFactor)
        this.scroll(deltaToScroll)
        also()
    }

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
            Modifier.pointerInput(Unit) { saveScrollInputEvents(tracker, scrollConfig, this) }
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
            performMouseInput {
                // generate various move events
                repeat(30) {
                    performMouseScrollGesture(delta = { Offset(delta, delta) })
                    previousScrollValue += delta.toInt()
                }
            }
            Offset(previousScrollValue, previousScrollValue)
        }
}
