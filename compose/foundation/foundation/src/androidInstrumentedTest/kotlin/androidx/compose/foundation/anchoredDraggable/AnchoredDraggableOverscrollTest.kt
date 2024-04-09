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
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalFoundationApi::class)
class AnchoredDraggableOverscrollTest {

    @get:Rule
    val rule = createComposeRule()

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_scrollOutOfBounds_haveDeltaForOverscroll() {
        val overscrollEffect = TestOverscrollEffect()
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
            },
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            WithTouchSlop(0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .overscroll(overscrollEffect)
                ) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                                overscrollEffect = overscrollEffect
                            )
                            .offset {
                                IntOffset(
                                    state
                                        .requireOffset()
                                        .roundToInt(), 0
                                )
                            }
                    )
                }
            }
        }

        val positionOfA = state.anchors.positionOf(A)
        val maxBound = state.anchors.positionOf(B)
        val overDrag = 100f

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionOfA)
        assertThat(overscrollEffect.scrollOverscrollDelta.x).isEqualTo(0f)

        // drag to positionB + overDrag
        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset(0f, 0f))
                moveBy(Offset(x = maxBound + overDrag, y = 0f))
                up()
            }
        rule.waitForIdle()

        // assert the component settled at anchor B
        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(maxBound)

        // assert that applyToScroll was called and there is a remaining delta because of dragging
        // out of bounds
        assertThat(overscrollEffect.applyToScrollCalledCount).isEqualTo(1)
        assertThat(abs(overscrollEffect.scrollOverscrollDelta.x)).isEqualTo(overDrag)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_haveVelocityForOverscroll() {
        val endVelocity = 4000f
        val overscrollEffect = TestOverscrollEffect()
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
            }
        )

        rule.setContent {
            WithTouchSlop(0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .overscroll(overscrollEffect)
                ) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                                overscrollEffect = overscrollEffect
                            )
                            .offset {
                                IntOffset(
                                    state
                                        .requireOffset()
                                        .roundToInt(), 0
                                )
                            }
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

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right / 2, 0f),
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
        assertThat(abs(overscrollEffect.flingOverscrollVelocity.x))
            .isWithin(endVelocity * 0.005f).of(endVelocity)
    }

    @Test
    fun anchoredDraggable_swipeWithVelocity_notAtBounds_noOverscroll() {
        val positionalThreshold = 0.5f
        val absThreshold = abs(positionalThreshold)
        val overscrollEffect = TestOverscrollEffect()
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { distance -> absThreshold * distance },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
                C at 500f
            }
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .overscroll(overscrollEffect)
                    ) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Horizontal,
                                    overscrollEffect = overscrollEffect
                                )
                                .offset {
                                    IntOffset(
                                        state
                                            .requireOffset()
                                            .roundToInt(), 0
                                    )
                                }
                                .background(Color.Red)
                        )
                    }
                }
            }
        }

        val positionA = state.anchors.positionOf(A)
        val positionB = state.anchors.positionOf(B)

        val distance = abs(positionB - positionA)
        val delta = distance * absThreshold * 1.1f

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(positionA)
        assertThat(overscrollEffect.applyToFlingCalledCount).isEqualTo(0)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(0f, 0f),
                    end = Offset(delta, 0f),
                    endVelocity = 4000f
                )
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun anchoredDraggable_swipeWithVelocity_notEnoughVelocityForOverscroll() {
        val overscrollEffect = TestOverscrollEffect()
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
            }
        )

        rule.setContent {
            WithTouchSlop(0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .overscroll(overscrollEffect)
                ) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                                overscrollEffect = overscrollEffect
                            )
                            .offset {
                                IntOffset(
                                    state
                                        .requireOffset()
                                        .roundToInt(), 0
                                )
                            }
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

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right, 0f),
                    endVelocity = 0f
                )
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

    private val DefaultPositionalThreshold: (totalDistance: Float) -> Float = {
        with(rule.density) { 56.dp.toPx() }
    }

    private val DefaultVelocityThreshold: () -> Float = { with(rule.density) { 125.dp.toPx() } }

    private val DefaultDecayAnimationSpec: DecayAnimationSpec<Float> =
        SplineBasedFloatDecayAnimationSpec(rule.density).generateDecayAnimationSpec()
}

@OptIn(ExperimentalFoundationApi::class)
private class TestOverscrollEffect : OverscrollEffect {
    var applyToScrollCalledCount: Int = 0
        private set

    var applyToFlingCalledCount: Int = 0
        private set

    var scrollOverscrollDelta: Offset = Offset.Zero
        private set

    var flingOverscrollVelocity: Velocity = Velocity.Zero
        private set

    @ExperimentalFoundationApi
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

private val NoOpDensity = object : Density {
    override val density = 1f
    override val fontScale = 1f
}
