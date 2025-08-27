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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableMinFlingVelocity
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
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
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class AnchoredDraggableGestureTest(val testNewBehavior: Boolean) :
    AnchoredDraggableBackwardsCompatibleTest(testNewBehavior) {

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_swipe_horizontal() {
        val (state, modifier) = createStateAndModifier(initialValue = A, Orientation.Horizontal)
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeRight(endX = right / 2)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeRight(startX = right / 2, endX = right)
        }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.positionOf(C))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeLeft(endX = right / 2)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeLeft(startX = right / 2)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.positionOf(A))
    }

    @Test
    fun anchoredDraggable_swipe_vertical() {
        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Vertical)
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(startY = top, endY = bottom / 2)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(startY = bottom / 2, endY = bottom)
        }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.positionOf(C))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeUp(startY = bottom, endY = bottom / 2)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.positionOf(B))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeUp(startY = bottom / 2, endY = top)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.positionOf(A))
    }

    @Test
    fun anchoredDraggable_swipe_disabled_horizontal() {
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                orientation = Orientation.Horizontal,
                enabled = false,
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeRight(startX = left, endX = right)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun anchoredDraggable_swipe_disabled_vertical() {
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                orientation = Orientation.Vertical,
                enabled = false,
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

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(startY = top, endY = bottom)
        }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun anchoredDraggable_velocityThreshold_settle_velocityHigherThanThreshold_advances() =
        runBlocking(AutoTestFrameClock()) {
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 100f
                            C at 200f
                        },
                )
            val flingBehavior = createAnchoredDraggableFlingBehavior(state, rule.density)
            state.dispatchRawDelta(60f)
            performFling(flingBehavior, state, AnchoredDraggableMinFlingVelocityPx + 1)
            rule.waitForIdle()
            assertThat(state.currentValue).isEqualTo(B)
        }

    @Test
    fun anchoredDraggable_velocityThreshold_settle_velocityLowerThanThreshold_doesntAdvance() =
        runBlocking(AutoTestFrameClock()) {
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    DraggableAnchors {
                        A at 0f
                        B at 100f
                        C at 200f
                    },
                )
            val flingBehavior = createAnchoredDraggableFlingBehavior(state, rule.density)

            state.dispatchRawDelta(40f)
            performFling(flingBehavior, state, AnchoredDraggableMinFlingVelocityPx * 0.9f)
            assertThat(state.currentValue).isEqualTo(A)
        }

    @Test
    fun anchoredDraggable_dragAndSwipeBackWithVelocity_velocityHigherThanThreshold() =
        runBlocking(AutoTestFrameClock()) {
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    DraggableAnchors {
                        A at 0f
                        B at 200f
                    },
                )
            val flingBehavior = createAnchoredDraggableFlingBehavior(state, rule.density)

            // starting from anchor B, drag the component to the left and settle with a
            // positive velocity (higher than threshold). Result should be settling back to anchor B
            state.dispatchRawDelta(-60f)
            assertThat(state.requireOffset()).isEqualTo(140)
            performFling(flingBehavior, state, AnchoredDraggableMinFlingVelocityPx + 1)
            assertThat(state.currentValue).isEqualTo(B)

            state.animateTo(A, AnchoredDraggableDefaults.SnapAnimationSpec)
            assertThat(state.currentValue).isEqualTo(A)

            // starting from anchor A, drag the component to the right and with a negative velocity
            // (higher than threshold). Result should be settling back to anchor A
            state.dispatchRawDelta(60f)
            assertThat(state.requireOffset()).isEqualTo(60)
            performFling(flingBehavior, state, -AnchoredDraggableMinFlingVelocityPx)
            assertThat(state.currentValue).isEqualTo(A)
        }

    @Test
    fun anchoredDraggable_velocityThreshold_swipe_velocityHigherThanThreshold_advances() {
        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Horizontal)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(right / 2, 0f),
                endVelocity = AnchoredDraggableMinFlingVelocityPx * 1.1f,
            )
        }

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_velocityThreshold_swipe_velocityLowerThanThreshold_doesntAdvance() {
        val (state, modifier) = createStateAndModifier(initialValue = A, Orientation.Horizontal)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
                        .onSizeChanged { layoutSize ->
                            val anchors = DraggableAnchors {
                                A at 0f
                                B at layoutSize.width / 2f
                                C at layoutSize.width.toFloat()
                            }
                            state.updateAnchors(anchors)
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(right / 4, 0f),
                endVelocity = AnchoredDraggableMinFlingVelocityPx * 0.9f,
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
        val (state, modifier) = createStateAndModifier(initialValue = A, Orientation.Horizontal)
        state.updateAnchors(anchors)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        val overdrag = 100f
        val maxBound = state.anchors.positionOf(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
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
    fun anchoredDraggable_targetValue_animationCancelledResetsTargetValueToClosest() = runBlocking {
        rule.mainClock.autoAdvance = false
        lateinit var scope: CoroutineScope
        rule.setContent { scope = rememberCoroutineScope() }

        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        val state = createAnchoredDraggableState(initialValue = A, anchors = anchors)

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C, DefaultSnapAnimationSpec) }

        // Advance until our closest anchor is B
        while (state.requireOffset() < anchors.positionOf(B)) {
            rule.mainClock.advanceTimeByFrame()
        }
        assertThat(state.targetValue).isEqualTo(C)

        // Take over the state to cancel the ongoing animation
        state.anchoredDrag {}
        rule.mainClock.advanceTimeByFrame()

        // B is the closest now so we should target it
        assertThat(state.targetValue).isEqualTo(B)
    }

    // TODO(b/360835763): Remove when removing the old overload
    @Test
    fun anchoredDraggable_startDragImmediately_false_animationNotCancelledByDrag() {
        rule.mainClock.autoAdvance = false
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors = anchors,
                orientation = Orientation.Horizontal,
                startDragImmediately = false,
            )
        lateinit var scope: CoroutineScope
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                scope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize()) {
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
        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C, DefaultSnapAnimationSpec) }

        rule.mainClock.advanceTimeUntil {
            state.requireOffset() > abs(state.requireOffset() - anchors.positionOf(B))
        } // Advance until our closest anchor is B
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput { down(Offset.Zero) }
        rule.waitForIdle()

        assertThat(state.targetValue).isEqualTo(C) // Animation will continue to C
    }

    @Test
    fun anchoredDraggable_startDragImmediately_default_processesWithoutSlopWhileAnimating() {
        rule.mainClock.autoAdvance = false
        val anchors = DraggableAnchors {
            A at 0f
            B at 250f
            C at 500f
        }
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                anchors = anchors,
                orientation = Orientation.Horizontal,
            )
        lateinit var scope: CoroutineScope
        rule.setContent {
            WithTouchSlop(touchSlop = 5000f) {
                scope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize()) {
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
        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C) }

        rule.mainClock.advanceTimeUntil { state.requireOffset() > 10 }
        val offsetBeforeTouch = state.requireOffset()

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            down(Offset.Zero)
            moveBy(Offset(x = 15f, y = 0f))
        }
        // rule.mainClock.advanceTimeByFrame()
        assertThat(state.requireOffset()).isEqualTo(offsetBeforeTouch + 15f)
        rule.waitForIdle()
    }

    @Test
    fun anchoredDraggable_updatesState() {
        val state1 =
            createAnchoredDraggableState(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                        C at 500f
                    },
            )
        val state2 =
            createAnchoredDraggableState(
                initialValue = B,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                    },
            )
        var state by mutableStateOf(state1)

        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .then(createAnchoredDraggableModifier(state, Orientation.Horizontal))
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                            .background(Color.Red)
                    )
                }
            }
        }

        val positionOfA = state.anchors.positionOf(A)
        val positionOfB = state.anchors.positionOf(B)
        val distance = abs(positionOfA - positionOfB)

        // dragging across the positional threshold to settle at anchor B
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            down(Offset(0f, 0f))
            moveBy(Offset(x = distance * 0.55f, y = 0f))
            up()
        }
        rule.waitForIdle()

        // assert that changes reflected on state1
        assertThat(state1.requireOffset()).isEqualTo(positionOfB)

        // attaching state2 instead of state1
        state = state2
        rule.waitForIdle()

        // dragging across the positional threshold to settle at anchor A
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            down(Offset(0f, 0f))
            moveBy(Offset(x = -distance * 0.55f, y = 0f))
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
        val (state, modifier) =
            createStateAndModifier(
                initialValue = B,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 250f
                        C at 500f
                    },
                orientation = Orientation.Horizontal,
                reverseDirection = true,
            )
        rule.setContent {
            WithTouchSlop(0f) {
                Box(Modifier.fillMaxSize()) {
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

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(B), y = 0f),
                end = Offset(x = state.anchors.positionOf(A), y = 0f),
            )
        }

        assertThat(state.offset).isEqualTo(state.anchors.positionOf(C))
    }

    @Test
    fun anchoredDraggable_reverseDirection_defaultValue_reversesDeltasInRTL() {
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
            )
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                WithTouchSlop(0f) {
                    Box(Modifier.fillMaxSize()) {
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

        assertThat(layoutDirection).isEqualTo(LayoutDirection.Ltr)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(A), y = 0f),
                end = Offset(x = state.anchors.positionOf(B), y = 0f),
            )
        }
        assertThat(state.offset).isEqualTo(state.anchors.positionOf(B))

        layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipe(
                start = Offset(x = state.anchors.positionOf(B), y = 0f),
                end = Offset(x = state.anchors.positionOf(A), y = 0f),
            )
        }
        assertThat(state.offset).isEqualTo(state.anchors.positionOf(C))
    }

    @Test
    fun anchoredDraggable_onDensityChanges_swipeWithVelocityHigherThanThreshold() {
        if (!testNewBehavior) return
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                orientation = Orientation.Horizontal,
                shouldCreateFling = false,
            )

        var density by mutableStateOf(rule.density)
        val originalThreshold = AnchoredDraggableMinFlingVelocityPx * 1.1f
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalDensity provides density) {
                    Box(
                        Modifier.requiredSize(400.dp)
                            .testTag(AnchoredDraggableTestTag)
                            .then(modifier)
                            .onSizeChanged { layoutSize ->
                                val anchors = DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                                state.updateAnchors(anchors)
                            }
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                            .background(Color.Red)
                    )
                }
            }
        }
        var offsetDisplaced = 0.0f
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            offsetDisplaced = right / 2
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(offsetDisplaced, 0f),
                endVelocity = originalThreshold,
            )
        }

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)

        rule.runOnIdle {
            density = Density(density = density.density * 2f) // now threshold is higher
        }

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(offsetDisplaced, 0f),
                endVelocity = originalThreshold,
            )
        }

        // will not advance, threshold grew because of density change
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)

        // now use the new threshold
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeWithVelocity(
                start = Offset(left, 0f),
                end = Offset(offsetDisplaced, 0f),
                endVelocity = with(density) { AnchoredDraggableMinFlingVelocity.toPx() } * 1.1f,
            )
        }

        // will advance correctly
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
    }

    @Test
    fun anchoredDraggable_fling_confirmValueChange_returnsFalse_returnsToSettledAnchor() {
        val inspectAnimationSpec = InspectSpringAnimationSpec(DefaultSnapAnimationSpec)
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                Orientation.Horizontal,
                confirmValueChange = { it != B },
                snapAnimationSpec = inspectAnimationSpec,
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

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.settledValue).isEqualTo(A)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeRight(endX = right / 2)
        }
        assertThat(state.offset).isWithin(0.5f).of(anchors.positionOf(B))
        rule.waitForIdle()

        assertThat(inspectAnimationSpec.animationWasExecutions).isEqualTo(1)
        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.settledValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.positionOf(A))
    }

    private val DefaultSnapAnimationSpec = tween<Float>()

    private class InspectSpringAnimationSpec(private val animation: AnimationSpec<Float>) :
        AnimationSpec<Float> {

        var animationWasExecutions = 0

        override fun <V : AnimationVector> vectorize(
            converter: TwoWayConverter<Float, V>
        ): VectorizedAnimationSpec<V> {
            animationWasExecutions++
            return animation.vectorize(converter)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testNewBehavior={0}")
        fun params() = listOf(false, true)
    }
}

private val NoOpDensity =
    object : Density {
        override val density = 1f
        override val fontScale = 1f
    }
