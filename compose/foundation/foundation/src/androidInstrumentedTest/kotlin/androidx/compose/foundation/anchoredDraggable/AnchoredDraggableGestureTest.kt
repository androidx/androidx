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

package androidx.compose.foundation.anchoredDraggable

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalFoundationApi::class)
class AnchoredDraggableGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_swipe_horizontal() {
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        val anchors = DraggableAnchors {
            A at 0f
            B at AnchoredDraggableBoxSize.value / 2f
            C at AnchoredDraggableBoxSize.value
        }
        state.updateAnchors(anchors)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Horizontal
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeRight(endX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeRight(startX = right / 2, endX = right) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.positionOf(C))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeLeft(endX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeLeft(startX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.positionOf(A))
    }

    @Test
    fun anchoredDraggable_swipe_vertical() {
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        val anchors = DraggableAnchors {
            A at 0f
            B at AnchoredDraggableBoxSize.value / 2f
            C at AnchoredDraggableBoxSize.value
        }
        state.updateAnchors(anchors)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Vertical
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeDown(startY = top, endY = bottom / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeDown(startY = bottom / 2, endY = bottom) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.positionOf(C))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeUp(startY = bottom, endY = bottom / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeUp(startY = bottom / 2, endY = top) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.positionOf(A))
    }

    @Test
    fun anchoredDraggable_swipe_disabled_horizontal() {
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        state.updateAnchors(anchors)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Horizontal,
                                    enabled = false
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeRight(startX = left, endX = right) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun anchoredDraggable_swipe_disabled_vertical() {
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        state.updateAnchors(anchors)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Vertical,
                                    enabled = false
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeDown(startY = top, endY = bottom) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun anchoredDraggable_negative_offset_targetState() {
        val positionalThreshold = 0.5f
        val absThreshold = abs(positionalThreshold)
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { distance -> distance * positionalThreshold },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
                        )
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at -layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
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

        val positionOfA = state.anchors.positionOf(A)
        val positionOfB = state.anchors.positionOf(B)
        val distance = abs(positionOfA - positionOfB)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput { swipeLeft(startX = right, endX = left) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(distance * (absThreshold * 1.1f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_positionalThresholds_fractional_targetState() {
        val positionalThreshold = 0.5f
        val absThreshold = abs(positionalThreshold)
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { totalDistance -> totalDistance * positionalThreshold },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
                        )
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
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

        val positionOfA = state.anchors.positionOf(A)
        val positionOfB = state.anchors.positionOf(B)
        val distance = abs(positionOfA - positionOfB)
        state.dispatchRawDelta(positionOfA + distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        state.dispatchRawDelta(distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_positionalThresholds_fractional_negativeThreshold_targetState() {
        val positionalThreshold = -0.5f
        val absThreshold = abs(positionalThreshold)
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { totalDistance -> totalDistance * positionalThreshold },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
                        )
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
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

        val positionOfA = state.anchors.positionOf(A)
        val positionOfB = state.anchors.positionOf(B)
        val distance = abs(positionOfA - positionOfB)
        state.dispatchRawDelta(positionOfA + distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        state.dispatchRawDelta(distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_velocityThreshold_settle_velocityHigherThanThreshold_advances() =
        runBlocking(AutoTestFrameClock()) {
            val velocity = 100.dp
            val velocityPx = with(rule.density) { velocity.toPx() }
            val state = AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = DefaultPositionalThreshold,
                velocityThreshold = { velocityPx / 2f },
                snapAnimationSpec = tween(),
                decayAnimationSpec = DefaultDecayAnimationSpec
            )
            state.updateAnchors(
                DraggableAnchors {
                    A at 0f
                    B at 100f
                    C at 200f
                }
            )
            state.dispatchRawDelta(60f)
            state.settle(velocityPx)
            rule.waitForIdle()
            assertThat(state.currentValue).isEqualTo(B)
        }

    @Test
    fun anchoredDraggable_velocityThreshold_settle_velocityLowerThanThreshold_doesntAdvance() =
        runBlocking(AutoTestFrameClock()) {
            val velocity = 100.dp
            val velocityPx = with(rule.density) { velocity.toPx() }
            val state = AnchoredDraggableState(
                initialValue = A,
                velocityThreshold = { velocityPx },
                positionalThreshold = { Float.POSITIVE_INFINITY },
                snapAnimationSpec = tween(),
                decayAnimationSpec = DefaultDecayAnimationSpec
            )
            state.updateAnchors(
                DraggableAnchors {
                    A at 0f
                    B at 100f
                    C at 200f
                }
            )
            state.dispatchRawDelta(60f)
            state.settle(velocityPx / 2)
            assertThat(state.currentValue).isEqualTo(A)
        }

    @Test
    fun anchoredDraggable_dragAndSwipeBackWithVelocity_velocityHigherThanThreshold() =
        runBlocking(AutoTestFrameClock()) {
            val velocity = 100.dp
            val velocityPx = with(rule.density) { velocity.toPx() }
            val state = AnchoredDraggableState(
                initialValue = B,
                velocityThreshold = { velocityPx },
                positionalThreshold = { 0f },
                snapAnimationSpec = tween(),
                decayAnimationSpec = DefaultDecayAnimationSpec
            )
            state.updateAnchors(
                DraggableAnchors {
                    A at 0f
                    B at 200f
                }
            )

            // starting from anchor B, drag the component to the left and settle with a
            // positive velocity (higher than threshold). Result should be settling back to anchor B
            state.dispatchRawDelta(-60f)
            assertThat(state.requireOffset()).isEqualTo(140)
            state.settle(velocityPx)
            assertThat(state.currentValue).isEqualTo(B)

            state.animateTo(A)
            assertThat(state.currentValue).isEqualTo(A)

            // starting from anchor A, drag the component to the right and with a negative velocity
            // (higher than threshold). Result should be settling back to anchor A
            state.dispatchRawDelta(60f)
            assertThat(state.requireOffset()).isEqualTo(60)
            state.settle(-velocityPx)
            assertThat(state.currentValue).isEqualTo(A)
        }

    @Test
    fun anchoredDraggable_velocityThreshold_swipe_velocityHigherThanThreshold_advances() {
        val velocityThreshold = 100.dp
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = { with(rule.density) { velocityThreshold.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
                        )
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
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

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right / 2, 0f),
                    endVelocity = with(rule.density) { velocityThreshold.toPx() } * 1.1f
                )
            }

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_velocityThreshold_swipe_velocityLowerThanThreshold_doesntAdvance() {
        val velocityThreshold = 100.dp
        val state = AnchoredDraggableState(
            initialValue = A,
            velocityThreshold = { with(rule.density) { velocityThreshold.toPx() } },
            positionalThreshold = { Float.POSITIVE_INFINITY },
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
                        )
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
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

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right / 4, 0f),
                    endVelocity = with(rule.density) { velocityThreshold.toPx() } * 0.9f
                )
            }

        rule.waitForIdle()
        assertThat(state.settledValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_dragBeyondBounds_clampsAndSwipesBack() {
        val anchors = DraggableAnchors {
            A at 0f
            C at 500f
        }
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = { 0f },
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        state.updateAnchors(anchors)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Horizontal
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

        val overdrag = 100f
        val maxBound = state.anchors.positionOf(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset(0f, 0f))
                moveBy(Offset(x = maxBound + overdrag, y = 0f))
                moveBy(Offset(x = -overdrag, y = 0f))
            }

        rule.waitForIdle()

        // If we have not correctly coerced our drag deltas, its internal offset would be the
        // max bound + overdrag. If it is coerced correctly, it will not move past the max bound.
        // This means that once we swipe back by the amount of overdrag, we should end up at the
        // max bound - overdrag.
        assertThat(state.requireOffset()).isEqualTo(maxBound - overdrag)
    }

    @Test
    fun anchoredDraggable_animationCancelledByDrag_resetsTargetValueToClosest() {
        rule.mainClock.autoAdvance = false
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            anchors = anchors,
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        lateinit var scope: CoroutineScope
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                scope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal
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

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C) }

        rule.mainClock.advanceTimeUntil {
            state.requireOffset() > abs(state.requireOffset() - anchors.positionOf(B))
        } // Advance until our closest anchor is B
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset.Zero)
            }
        rule.waitForIdle()

        assertThat(state.targetValue).isEqualTo(B) // B is the closest now so we should target it
    }

    @Test
    fun anchoredDraggable_animationNotCancelledByDrag_startDragImmediatelyIsFalse() {
        rule.mainClock.autoAdvance = false
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        val state = AnchoredDraggableState(
            initialValue = A,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            anchors = anchors,
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        lateinit var scope: CoroutineScope
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                scope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                                startDragImmediately = false,
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
        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C) }

        rule.mainClock.advanceTimeUntil {
            state.requireOffset() > abs(state.requireOffset() - anchors.positionOf(B))
        } // Advance until our closest anchor is B
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset.Zero)
            }
        rule.waitForIdle()

        assertThat(state.targetValue).isEqualTo(C) // Animation will continue to C
    }

    @Test
    fun anchoredDraggable_updatesState() {
        val positionalThreshold = 0.5f
        val state1 = AnchoredDraggableState(
            initialValue = A,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
                C at 500f
            },
            positionalThreshold = { totalDistance -> totalDistance * positionalThreshold },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        val state2 = AnchoredDraggableState(
            initialValue = B,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
            },
            positionalThreshold = { totalDistance -> totalDistance * positionalThreshold },
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )

        var state by mutableStateOf(state1)

        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal
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

        val positionOfA = state.anchors.positionOf(A)
        val positionOfB = state.anchors.positionOf(B)
        val distance = abs(positionOfA - positionOfB)

        // dragging across the positional threshold to settle at anchor B
        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset(0f, 0f))
                moveBy(Offset(x = distance * positionalThreshold * 1.1f, y = 0f))
                up()
            }
        rule.waitForIdle()

        // assert that changes reflected on state1
        assertThat(state1.requireOffset()).isEqualTo(positionOfB)

        // attaching state2 instead of state1
        state = state2
        rule.waitForIdle()

        // dragging across the positional threshold to settle at anchor A
        rule.onNodeWithTag(AnchoredDraggableTestTag)
            .performTouchInput {
                down(Offset(0f, 0f))
                moveBy(Offset(x = -distance * positionalThreshold * 1.1f, y = 0f))
                up()
            }
        rule.waitForIdle()

        // assert that no more changes reflected on state1
        assertThat(state1.requireOffset()).isEqualTo(positionOfB)
        // assert that changes reflected on state2
        assertThat(state2.requireOffset()).isEqualTo(positionOfA)
    }

    @Test
    fun anchoredDraggable_reverseDirection_true_reversesDeltas() {
        val state = AnchoredDraggableState(
            initialValue = B,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
                C at 500f
            },
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                                reverseDirection = true
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

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(B), y = 0f),
                end = Offset(x = state.anchors.positionOf(A), y = 0f)
            )
        }

        assertThat(state.offset).isEqualTo(state.anchors.positionOf(C))
    }

    @Test
    fun anchoredDraggable_reverseDirection_defaultValue_reversesDeltasInRTL() {
        val state = AnchoredDraggableState(
            initialValue = A,
            anchors = DraggableAnchors {
                A at 0f
                B at 250f
                C at 500f
            },
            positionalThreshold = DefaultPositionalThreshold,
            velocityThreshold = DefaultVelocityThreshold,
            snapAnimationSpec = tween(),
            decayAnimationSpec = DefaultDecayAnimationSpec
        )
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .requiredSize(AnchoredDraggableBoxSize)
                                .testTag(AnchoredDraggableTestTag)
                                .anchoredDraggable(
                                    state = state,
                                    orientation = Orientation.Horizontal
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

        assertThat(layoutDirection).isEqualTo(LayoutDirection.Ltr)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(A), y = 0f),
                end = Offset(x = state.anchors.positionOf(B), y = 0f)
            )
        }
        assertThat(state.offset).isEqualTo(state.anchors.positionOf(B))

        layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(B), y = 0f),
                end = Offset(x = state.anchors.positionOf(A), y = 0f)
            )
        }
        assertThat(state.offset).isEqualTo(state.anchors.positionOf(C))
    }

    private val DefaultPositionalThreshold: (totalDistance: Float) -> Float = {
        with(rule.density) { 56.dp.toPx() }
    }

    private val DefaultVelocityThreshold: () -> Float = { with(rule.density) { 125.dp.toPx() } }

    private val DefaultDecayAnimationSpec: DecayAnimationSpec<Float> =
        SplineBasedFloatDecayAnimationSpec(rule.density).generateDecayAnimationSpec()
}

private val NoOpDensity = object : Density {
    override val density = 1f
    override val fontScale = 1f
}
