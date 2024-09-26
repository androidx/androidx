/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.anchoredDraggable

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class AnchoredDraggableOverscrollTest(testNewBehavior: Boolean) :
    AnchoredDraggableBackwardsCompatibleTest(testNewBehavior) {

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_scrollOutOfBounds_haveDeltaForOverscroll() {
        val overscrollEffect = TestOverscrollEffect()
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                    },
                orientation = Orientation.Horizontal,
                overscrollEffect = overscrollEffect
            )
        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize().overscroll(overscrollEffect)) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .then(modifier)
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                    )
                }
            }
        }

        val positionOfA = state.anchors.positionOf(A)
        val maxBound = state.anchors.positionOf(B)
        val overDrag = Offset(x = 100f, y = 0f)

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionOfA)
        assertThat(overscrollEffect.scrollOverscrollDelta).isEqualTo(Offset.Zero)

        // drag to positionB + overDrag
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            down(Offset(0f, 0f))
            moveBy(Offset(x = maxBound + overDrag.x, y = 0f))
            up()
        }
        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(maxBound)

        // assert that applyToScroll was called and there is a remaining delta because of dragging
        // out of bounds
        assertThat(overscrollEffect.applyToScrollCalledCount).isEqualTo(1)
        assertThat(overscrollEffect.scrollOverscrollDelta).isEqualTo(overDrag)
    }

    private fun testDispatchesToOverscrollInOrientationOnlyWhenDraggedOutOfBounds(
        orientation: Orientation
    ) {
        val overscrollEffect = TestOverscrollEffect()
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                    },
                orientation = orientation,
                overscrollEffect = overscrollEffect
            )
        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize().overscroll(overscrollEffect)) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .then(modifier)
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                    )
                }
            }
        }

        val positionOfA = state.anchors.positionOf(A)
        val maxBound = state.anchors.positionOf(B)
        val overDrag = 100f

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionOfA)
        assertThat(overscrollEffect.scrollOverscrollDelta).isEqualTo(Offset.Zero)

        // drag to positionB + overDrag
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            down(Offset(0f, 0f))
            moveBy(Offset(x = maxBound + overDrag, y = maxBound + overDrag))
            up()
        }
        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(maxBound)

        // assert that applyToScroll was called and there is a remaining delta because of dragging
        // out of bounds
        assertThat(overscrollEffect.applyToScrollCalledCount).isEqualTo(1)
        assertWithMessage("overscrollDelta.x for orientation $orientation")
            .that(overscrollEffect.scrollOverscrollDelta.x)
            .isEqualTo(if (orientation == Orientation.Horizontal) overDrag else 0f)
        assertWithMessage("overscrollDelta.y for orientation $orientation")
            .that(overscrollEffect.scrollOverscrollDelta.y)
            .isEqualTo(if (orientation == Orientation.Vertical) overDrag else 0f)
    }

    @Test
    fun anchoredDraggable_scrollOutOfBounds_dispatchesToOverscroll_inOrientationOnly_horizontal() {
        testDispatchesToOverscrollInOrientationOnlyWhenDraggedOutOfBounds(
            orientation = Orientation.Horizontal
        )
    }

    @Test
    fun anchoredDraggable_scrollOutOfBounds_dispatchesToOverscroll_inOrientationOnly_vertical() {
        testDispatchesToOverscrollInOrientationOnlyWhenDraggedOutOfBounds(
            orientation = Orientation.Vertical
        )
    }

    private fun testSwipeWithVelocityDispatchesToOverscroll(orientation: Orientation) {
        val endVelocity = 4000f
        val overscrollEffect = TestOverscrollEffect()
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                    },
                orientation = orientation,
                overscrollEffect = overscrollEffect,
                decayAnimationSpec =
                    SplineBasedFloatDecayAnimationSpec(rule.density).generateDecayAnimationSpec()
            )

        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize().overscroll(overscrollEffect)) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .then(modifier)
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                            .background(Color.Red)
                    )
                }
            }
        }

        val positionA = state.anchors.positionOf(A)
        val positionB = state.anchors.positionOf(B)

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionA)
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(0)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            val endPointX = if (orientation == Orientation.Horizontal) right else 0f
            val endPointY = if (orientation == Orientation.Vertical) bottom else 0f
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(endPointX, endPointY),
                endVelocity = endVelocity
            )
        }

        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(positionB)

        // assert that applyToFling was called and there is a remaining velocity because of flinging
        // with a high velocity towards the max anchor
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(1)
        // [flingOverscrollVelocity] would be slightly less than [endVelocity] as one animation
        // frame would be executed, which consumes some velocity
        assertWithMessage("flingOverscrollVelocity.x for orientation $orientation")
            .that(abs(overscrollEffect.flingOverscrollVelocity.x))
            .isWithin(endVelocity * 0.005f)
            .of(if (orientation == Orientation.Horizontal) endVelocity else 0f)
        assertWithMessage("flingOverscrollVelocity.y for orientation $orientation")
            .that(abs(overscrollEffect.flingOverscrollVelocity.y))
            .isWithin(endVelocity * 0.005f)
            .of(if (orientation == Orientation.Vertical) endVelocity else 0f)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_haveVelocityForOverscroll_horizontal() {
        testSwipeWithVelocityDispatchesToOverscroll(Orientation.Horizontal)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_haveVelocityForOverscroll_vertical() {
        testSwipeWithVelocityDispatchesToOverscroll(Orientation.Vertical)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_notAtBounds_noOverscroll() {
        val overscrollEffect = TestOverscrollEffect()

        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                        C at 500f
                    },
                orientation = Orientation.Horizontal,
                overscrollEffect = overscrollEffect
            )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize().overscroll(overscrollEffect)) {
                        Box(
                            Modifier.requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .then(modifier)
                                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                                .background(Color.Red)
                        )
                    }
                }
            }
        }

        val positionA = state.anchors.positionOf(A)
        val positionB = state.anchors.positionOf(B)

        val distance = abs(positionB - positionA)
        val delta = distance * 0.55f

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionA)
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(0)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(start = Offset(0f, 0f), end = Offset(delta, 0f), endVelocity = 4000f)
        }

        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(positionB)

        // assert that applyToFling was called but there is no remaining velocity for overscroll
        // because flinging was not towards the min/max anchors
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(1)
        assertThat(abs(overscrollEffect.flingOverscrollVelocity.x)).isWithin(1f).of(0f)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_notEnoughVelocityForOverscroll() {
        val overscrollEffect = TestOverscrollEffect()

        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                    },
                orientation = Orientation.Horizontal,
                overscrollEffect = overscrollEffect
            )

        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize().overscroll(overscrollEffect)) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .then(modifier)
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                            .background(Color.Red)
                    )
                }
            }
        }

        val positionA = state.anchors.positionOf(A)
        val positionB = state.anchors.positionOf(B)

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionA)
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(0)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(start = Offset(left, 0f), end = Offset(right, 0f), endVelocity = 0f)
        }

        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(positionB)

        // assert that applyToFling was called but there is no remaining velocity for overscroll
        // because velocity is small
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(1)
        assertThat(abs(overscrollEffect.flingOverscrollVelocity.x)).isWithin(1f).of(0f)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testNewBehavior={0}")
        fun params() = listOf(false, true)
    }
}

private class TestOverscrollEffect : OverscrollEffect {
    var applyToScrollCalledCount: Int = 0
        private set

    var applyToFlingCalledCount: Int = 0
        private set

    var scrollOverscrollDelta: Offset = Offset.Zero
        private set

    var flingOverscrollVelocity: Velocity = Velocity.Zero
        private set

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        applyToScrollCalledCount++
        val consumed = performScroll(delta)
        scrollOverscrollDelta = delta - consumed
        return Offset.Zero
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        applyToFlingCalledCount++
        val consumed = performFling(velocity)
        flingOverscrollVelocity = velocity - consumed
    }

    override val isInProgress: Boolean = false
    override val effectModifier: Modifier = Modifier
}

private val NoOpDensity =
    object : Density {
        override val density = 1f
        override val fontScale = 1f
    }
