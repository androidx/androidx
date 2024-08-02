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

package androidx.compose.material3.anchoredDraggable

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.material3.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.material3.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.material3.internal.AnchoredDraggableState
import androidx.compose.material3.internal.DraggableAnchors
import androidx.compose.material3.internal.anchoredDraggable
import androidx.compose.material3.internal.animateTo
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.snapTo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterial3Api::class)
class AnchoredDraggableStateTest {

    @get:Rule val rule = createComposeRule()

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_state_canSkipStateByFling() {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput { swipeDown() }

        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(C)
    }

    @Test
    fun anchoredDraggable_targetState_updatedOnSwipe() {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(endY = bottom * 0.45f)
        }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        // Assert that swipe below threshold upward settles at current state
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeUp(endY = bottom * 0.95f, durationMillis = 1000)
        }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        // Assert that swipe below threshold downward settles at current state
        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(endY = bottom * 0.05f)
        }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(endY = bottom * 0.9f)
        }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeUp(endY = top * 1.1f)
        }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_targetState_updatedWithAnimation() {
        rule.mainClock.autoAdvance = false
        val animationDuration = 300
        val frameLengthMillis = 16L
        val state =
            AnchoredDraggableState(
                initialValue = A,
                animationSpec = { tween(animationDuration, easing = LinearEasing) },
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = defaultVelocityThreshold
            )
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        scope.launch { state.animateTo(targetValue = B) }
        rule.mainClock.advanceTimeBy(1 * frameLengthMillis)

        assertWithMessage("Current state").that(state.currentValue).isEqualTo(A)
        assertWithMessage("Target state").that(state.targetValue).isEqualTo(B)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertWithMessage("Current state").that(state.currentValue).isEqualTo(B)
        assertWithMessage("Target state").that(state.targetValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_progress_matchesSwipePosition() {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.requiredSize(AnchoredDraggableBoxSize)
                            .testTag(AnchoredDraggableTestTag)
                            .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                            .onSizeChanged { layoutSize ->
                                state.updateAnchors(
                                    DraggableAnchors {
                                        A at 0f
                                        B at layoutSize.width / 2f
                                        C at layoutSize.width.toFloat()
                                    }
                                )
                            }
                            .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                            .background(Color.Red)
                    )
                }
            }
        }

        val anchorA = state.anchors.positionOf(A)
        val anchorB = state.anchors.positionOf(B)
        val almostAnchorB = anchorB * 0.9f
        var expectedProgress = almostAnchorB / (anchorB - anchorA)

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeDown(endY = almostAnchorB)
        }

        assertThat(state.targetValue).isEqualTo(B)
        assertThat(state.progress).isEqualTo(expectedProgress)

        val almostAnchorA = anchorA + ((anchorB - anchorA) * 0.1f)
        expectedProgress = 1 - (almostAnchorA / (anchorB - anchorA))

        rule.onNodeWithTag(AnchoredDraggableTestTag).performTouchInput {
            swipeUp(startY = anchorB, endY = almostAnchorA)
        }

        assertThat(state.targetValue).isEqualTo(A)
        assertThat(state.progress).isEqualTo(expectedProgress)
    }

    @Test
    fun anchoredDraggable_snapTo_updatesImmediately() = runBlocking {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        state.snapTo(C)
        assertThat(state.currentValue).isEqualTo(C)
    }

    @Test
    fun anchoredDraggable_rememberanchoredDraggableState_restored() {
        val restorationTester = StateRestorationTester(rule)

        val initialState = C
        val animationSpec = { tween<Float>(durationMillis = 1000) }
        val state =
            AnchoredDraggableState(
                initialValue = initialState,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = animationSpec
            )
        lateinit var scope: CoroutineScope

        restorationTester.setContent {
            SideEffect {
                state.updateAnchors(
                    DraggableAnchors {
                        A at 0f
                        B at 100f
                        C at 200f
                    }
                )
            }
            scope = rememberCoroutineScope()
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(state.currentValue).isEqualTo(initialState)
        assertThat(state.animationSpec).isEqualTo(animationSpec)

        scope.launch { state.animateTo(B) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)

        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_targetState_accessedInInitialComposition() {
        lateinit var targetState: AnchoredDraggableTestValue
        rule.setContent {
            val state = remember {
                AnchoredDraggableState(
                    initialValue = B,
                    positionalThreshold = defaultPositionalThreshold,
                    velocityThreshold = defaultVelocityThreshold,
                    animationSpec = defaultAnimationSpec
                )
            }
            LaunchedEffect(state.targetValue) { targetState = state.targetValue }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Horizontal)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        assertThat(targetState).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_progress_accessedInInitialComposition() {
        var progress = Float.NaN
        rule.setContent {
            val state = remember {
                AnchoredDraggableState(
                    initialValue = B,
                    positionalThreshold = defaultPositionalThreshold,
                    velocityThreshold = defaultVelocityThreshold,
                    animationSpec = defaultAnimationSpec
                )
            }
            LaunchedEffect(state.progress) { progress = state.progress }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Horizontal)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        assertThat(progress).isEqualTo(1f)
    }

    @Test
    @Ignore("Todo: Fix differences between tests and real code - this shouldn't work :)")
    fun anchoredDraggable_requireOffset_accessedInInitialComposition_throws() {
        var exception: Throwable? = null
        val state =
            AnchoredDraggableState(
                initialValue = B,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        var offset: Float? = null
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Horizontal)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.width / 2f
                                    C at layoutSize.width.toFloat()
                                }
                            )
                        }
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
            exception = runCatching { offset = state.requireOffset() }.exceptionOrNull()
        }

        assertThat(state.anchors.size).isNotEqualTo(0)
        assertThat(offset).isNull()
        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception).hasMessageThat().contains("offset")
    }

    @Test
    @Ignore("LaunchedEffects execute instantly in tests. How can we delay?")
    fun anchoredDraggable_requireOffset_accessedInEffect_doesntThrow() {
        var exception: Throwable? = null
        rule.setContent {
            val state = remember {
                AnchoredDraggableState(
                    initialValue = B,
                    positionalThreshold = defaultPositionalThreshold,
                    velocityThreshold = defaultVelocityThreshold,
                    animationSpec = defaultAnimationSpec
                )
            }
            LaunchedEffect(Unit) {
                exception = runCatching { state.requireOffset() }.exceptionOrNull()
            }
        }

        assertThat(exception).isNull()
    }

    @Test
    fun anchoredDraggable_animateTo_animatesBeyondBounds() {
        rule.mainClock.autoAdvance = false
        val minBound = 0f
        val maxBound = 500f
        val anchors = DraggableAnchors {
            A at minBound
            C at maxBound
        }

        val animationSpec = FloatSpringSpec(dampingRatio = Spring.DampingRatioHighBouncy)
        val animationDuration =
            animationSpec
                .getDurationNanos(
                    initialValue = minBound,
                    targetValue = maxBound,
                    initialVelocity = 0f
                )
                .let { TimeUnit.NANOSECONDS.toMillis(it) }

        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = { animationSpec }
            )
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            SideEffect { state.updateAnchors(anchors) }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                        .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                        .background(Color.Red)
                )
            }
        }

        scope.launch { state.animateTo(C) }
        var highestOffset = 0f
        for (i in 0..animationDuration step 16) {
            highestOffset = state.requireOffset()
            rule.mainClock.advanceTimeBy(16)
        }
        assertThat(highestOffset).isGreaterThan(anchors.positionOf(C))
    }

    @Test
    fun anchoredDraggable_targetNotInAnchors_animateTo_updatesCurrentValue() {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        assertThat(state.anchors.size).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.animateTo(B) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_targetNotInAnchors_snapTo_updatesCurrentValue() {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        assertThat(state.anchors.size).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.snapTo(B) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_updateAnchors_noOngoingDrag_shouldUpdateOffset() {
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )

        assertThat(anchoredDraggableState.currentValue).isEqualTo(A)
        assertThat(anchoredDraggableState.targetValue).isEqualTo(A)
        assertThat(anchoredDraggableState.offset).isNaN()

        val offsetAtB = 100f
        anchoredDraggableState.updateAnchors(
            newAnchors =
                DraggableAnchors {
                    A at 0f
                    B at offsetAtB
                },
            newTarget = B
        )
        assertThat(anchoredDraggableState.currentValue).isEqualTo(B)
        assertThat(anchoredDraggableState.targetValue).isEqualTo(B)
        assertThat(anchoredDraggableState.offset).isEqualTo(offsetAtB)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchoredDraggable_updateAnchors_ongoingDrag_shouldRestartDrag() = runTest {
        // Given an anchored draggable state
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = 1,
                defaultPositionalThreshold,
                defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )

        val anchorUpdates = Channel<DraggableAnchors<Int>>()
        val dragJob = launch {
            anchoredDraggableState.anchoredDrag { newAnchors ->
                anchorUpdates.send(newAnchors)
                suspendIndefinitely()
            }
        }

        val firstAnchors = anchorUpdates.receive()
        assertThat(firstAnchors.size).isEqualTo(0)

        // When the anchors change
        val newAnchors = DraggableAnchors {
            1 at 100f
            2 at 200f
        }
        Snapshot.withMutableSnapshot { anchoredDraggableState.updateAnchors(newAnchors) }

        // Then the block should be invoked with the new anchors
        assertThat(dragJob.isActive).isTrue()
        val secondAnchors = anchorUpdates.receive()
        assertThat(secondAnchors).isEqualTo(newAnchors)
        dragJob.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchoredDraggable_updateAnchors_anchoredDrag_invokedWithLatestAnchors() = runTest {
        // Given an anchored draggable state
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = 1,
                defaultPositionalThreshold,
                defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )

        val anchorUpdates = Channel<DraggableAnchors<Int>>()
        val dragJob =
            launch(Dispatchers.Unconfined) {
                anchoredDraggableState.anchoredDrag { newAnchors ->
                    anchorUpdates.send(newAnchors)
                    suspendIndefinitely()
                }
            }

        val firstAnchors = anchorUpdates.receive()
        assertThat(firstAnchors.size).isEqualTo(0)

        // When the anchors change
        val newAnchors = DraggableAnchors {
            1 at 100f
            2 at 200f
        }
        Snapshot.withMutableSnapshot { anchoredDraggableState.updateAnchors(newAnchors) }

        // Then the block should be invoked with the new anchors
        assertThat(dragJob.isActive).isTrue()
        val secondAnchors = anchorUpdates.receive()
        assertThat(secondAnchors).isEqualTo(newAnchors)
        dragJob.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchoredDraggable_updateAnchors_anchoredDrag_invokedWithLatestTarget() = runTest {
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = A,
                defaultPositionalThreshold,
                defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec,
            )
        anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                A at 0f
                B at 200f
            }
        )

        assertThat(anchoredDraggableState.targetValue).isEqualTo(A)

        val firstExpectedTarget = B
        val targetUpdates = Channel<AnchoredDraggableTestValue>()
        val dragJob =
            launch(Dispatchers.Unconfined) {
                anchoredDraggableState.anchoredDrag(firstExpectedTarget) { _, latestTarget ->
                    targetUpdates.send(latestTarget)
                    suspendIndefinitely()
                }
            }

        val firstTarget = targetUpdates.receive()
        assertThat(firstTarget).isEqualTo(firstExpectedTarget)

        // When the anchors and target change
        val newTarget = A
        val newAnchors = DraggableAnchors {
            A at 100f
            B at 200f
        }
        Snapshot.withMutableSnapshot { anchoredDraggableState.updateAnchors(newAnchors, newTarget) }

        // Then the block should be invoked with the new anchors
        val secondTarget = targetUpdates.receive()
        assertThat(secondTarget).isEqualTo(newTarget)
        dragJob.cancel()
    }

    @Test
    fun anchoredDraggable_anchoredDrag_doesNotUpdateOnConfirmValueChange() = runTest {
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = B,
                defaultPositionalThreshold,
                defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec,
                confirmValueChange = { false }
            )
        anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                A at 0f
                B at 200f
            }
        )

        assertThat(anchoredDraggableState.targetValue).isEqualTo(B)

        val unexpectedTarget = A
        val targetUpdates = Channel<Float>()
        val dragJob =
            launch(Dispatchers.Unconfined) {
                anchoredDraggableState.anchoredDrag(unexpectedTarget) { anchors, latestTarget ->
                    targetUpdates.send(anchors.positionOf(latestTarget))
                    suspendIndefinitely()
                }
            }

        val firstTarget = targetUpdates.receive()
        assertThat(firstTarget).isEqualTo(200f)
        dragJob.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchoredDraggable_dragCompletesExceptionally_cleansUp() = runTest {
        val anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = A,
                defaultPositionalThreshold,
                defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        val cancellationSignal = CompletableDeferred(false)
        val anchoredDragUpdates = Channel<Unit>()
        val dragJob = launch {
            anchoredDraggableState.anchoredDrag {
                anchoredDragUpdates.send(Unit)
                cancellationSignal.await()
                cancel()
            }
        }

        assertThat(dragJob.isActive).isTrue()
        assertThat(anchoredDragUpdates.receive()).isEqualTo(Unit)
        cancellationSignal.complete(true)
        dragJob.join()
        assertThat(dragJob.isCancelled).isTrue()
    }

    @Test
    fun anchoredDraggable_customDrag_updatesOffset() = runBlocking {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        val anchors = DraggableAnchors {
            A at 0f
            B at 200f
            C at 300f
        }

        state.updateAnchors(anchors)
        state.anchoredDrag { dragTo(150f) }

        assertThat(state.requireOffset()).isEqualTo(150f)

        state.anchoredDrag { dragTo(250f) }
        assertThat(state.requireOffset()).isEqualTo(250f)
    }

    @Test
    fun anchoredDraggable_customDrag_updatesVelocity() = runBlocking {
        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        val anchors = DraggableAnchors {
            A at 0f
            B at 200f
            C at 300f
        }

        state.updateAnchors(anchors)
        state.anchoredDrag { dragTo(150f, lastKnownVelocity = 454f) }
        assertThat(state.lastVelocity).isEqualTo(454f)
    }

    @Test
    fun anchoredDraggable_customDrag_targetValueUpdate() = runBlocking {
        val clock = HandPumpTestFrameClock()
        val dragScope = CoroutineScope(clock)

        val state =
            AnchoredDraggableState(
                initialValue = A,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        val anchors = DraggableAnchors {
            A at 0f
            B at 200f
            C at 300f
        }

        state.updateAnchors(anchors)
        dragScope.launch(start = CoroutineStart.UNDISPATCHED) {
            state.anchoredDrag(targetValue = C) { _, _ ->
                while (isActive) {
                    withFrameNanos { dragTo(200f) }
                }
            }
        }
        clock.advanceByFrame()
        assertThat(state.targetValue).isEqualTo(C)
        dragScope.cancel()
    }

    @Test
    fun anchoredDraggable_constructorWithAnchors_updatesAnchorsAndInitializes() {
        val initialValueOffset = 0f
        val anchors = DraggableAnchors {
            A at initialValueOffset
            B at 200f
        }
        val state =
            AnchoredDraggableState(
                initialValue = A,
                anchors = anchors,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        assertThat(state.anchors).isEqualTo(anchors)
        assertThat(state.offset).isEqualTo(initialValueOffset)
    }

    @Test
    fun anchoredDraggable_constructorWithAnchors_initialValueNotInAnchors_updatesCurrentValue() {
        val anchors = DraggableAnchors { B at 200f }
        val state =
            AnchoredDraggableState(
                initialValue = A,
                anchors = anchors,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        assertThat(state.anchors).isEqualTo(anchors)
        assertThat(state.offset).isNaN()
    }

    @Test
    fun anchoredDraggable_customDrag_settleOnInvalidState_shouldRespectConfirmValueChange() =
        runBlocking {
            var shouldBlockValueC = false
            val state =
                AnchoredDraggableState(
                    initialValue = B,
                    positionalThreshold = defaultPositionalThreshold,
                    velocityThreshold = defaultVelocityThreshold,
                    animationSpec = defaultAnimationSpec,
                    confirmValueChange = {
                        if (shouldBlockValueC) it != C // block state value C
                        else true
                    }
                )
            val anchors = DraggableAnchors {
                A at 0f
                B at 200f
                C at 300f
            }

            state.updateAnchors(anchors)
            state.anchoredDrag { dragTo(300f) }

            // confirm we can actually go to C
            assertThat(state.currentValue).isEqualTo(C)

            // go back to B
            state.anchoredDrag { dragTo(200f) }
            assertThat(state.currentValue).isEqualTo(B)

            // disallow C
            shouldBlockValueC = true

            state.anchoredDrag { dragTo(300f) }
            assertThat(state.currentValue).isNotEqualTo(C)
        }

    // Regression test for b/295536718
    @Test
    fun draggableAnchors_measuredInPlacementInLookahead_initializesOffset() {
        data class LayoutExecutionInfo(
            val phase: Int, // 0 = measure; 1 = place
            val isLookingAhead: Boolean
        )

        val innerLayoutExecutionOrder = mutableListOf<LayoutExecutionInfo>()
        val state =
            AnchoredDraggableState(
                initialValue = B,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = defaultAnimationSpec
            )
        rule.setContent {
            LookaheadScope {
                SubcomposeLayout { constraints ->
                    layout(0, 0) {
                        // Do all work in placement instead of measurement, so we run lookahead
                        // place before post-lookahead measure
                        val placeables =
                            subcompose("sheet") {
                                    Box(
                                        modifier =
                                            Modifier.layout { measurable, innerConstraints ->
                                                    innerLayoutExecutionOrder.add(
                                                        LayoutExecutionInfo(0, isLookingAhead)
                                                    )
                                                    layout(
                                                        innerConstraints.maxWidth,
                                                        innerConstraints.maxHeight
                                                    ) {
                                                        val placeable =
                                                            measurable.measure(innerConstraints)
                                                        innerLayoutExecutionOrder.add(
                                                            LayoutExecutionInfo(1, isLookingAhead)
                                                        )
                                                        placeable.place(0, 0)
                                                    }
                                                }
                                                // The offset should be initialized by
                                                // draggableAnchors in
                                                // lookahead measure. If lookahead place runs before
                                                // post-lookahead measure and we were not
                                                // initializing the
                                                // offset in lookahead measure, this would crash as
                                                // draggableAnchors uses requireOffset in placement.
                                                .draggableAnchors(state, Orientation.Vertical) {
                                                    size,
                                                    constraints ->
                                                    DraggableAnchors {
                                                        A at 0f
                                                        C at
                                                            constraints.maxHeight -
                                                                size.height.toFloat()
                                                    } to A
                                                }
                                    )
                                }
                                .map { it.measure(constraints) }
                        placeables.map { it.place(0, 0) }
                    }
                }
            }
        }
        assertThat(innerLayoutExecutionOrder)
            .containsExactly(
                LayoutExecutionInfo(0, true),
                LayoutExecutionInfo(1, true),
                LayoutExecutionInfo(0, false),
                LayoutExecutionInfo(1, false),
            )
    }

    @Test
    fun draggableAnchors_draggableOffsetTaggedAsMotionFrameOfReference() {
        var offset by mutableStateOf(IntOffset(0, 0))
        val offsets =
            listOf(
                IntOffset(0, 0),
                IntOffset(5, 20),
                IntOffset(25, 0),
                IntOffset(100, 10),
            )
        var coords: LayoutCoordinates? = null
        var rootCoords: LayoutCoordinates? = null
        val state =
            AnchoredDraggableState(
                initialValue = 0,
                positionalThreshold = defaultPositionalThreshold,
                velocityThreshold = defaultVelocityThreshold,
                animationSpec = { spring() }
            )
        var value by mutableIntStateOf(0)
        rule.setContent {
            Box(Modifier.onGloballyPositioned { rootCoords = it }.offset { offset }) {
                LaunchedEffect(value) { state.snapTo(value) }
                Box(
                    Modifier.draggableAnchors(state, Orientation.Vertical) { _, _ ->
                            DraggableAnchors { repeat(5) { it at it * 100f } } to 0
                        }
                        .fillMaxSize()
                ) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { coords = it })
                }
            }
        }

        repeat(5) {
            value = it
            rule.waitForIdle()

            repeat(4) {
                offset = offsets[it]
                rule.runOnIdle {
                    val excludeOffset =
                        rootCoords!!
                            .localPositionOf(coords!!, includeMotionFrameOfReference = false)
                            .round()
                    val includeOffset =
                        rootCoords!!
                            .localPositionOf(coords!!, includeMotionFrameOfReference = true)
                            .round()
                    assertEquals(
                        includeOffset - IntOffset(0, state.requireOffset().roundToInt()),
                        excludeOffset
                    )
                }
            }
        }
    }

    private suspend fun suspendIndefinitely() = suspendCancellableCoroutine<Unit> {}

    private class HandPumpTestFrameClock : MonotonicFrameClock {
        private val frameCh = Channel<Long>(1)

        suspend fun advanceByFrame() {
            frameCh.send(16_000_000L)
        }

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            return onFrame(frameCh.receive())
        }
    }

    private val defaultPositionalThreshold: (totalDistance: Float) -> Float = {
        with(rule.density) { 56.dp.toPx() }
    }

    private val defaultVelocityThreshold: () -> Float = { with(rule.density) { 125.dp.toPx() } }

    private val defaultAnimationSpec = { tween<Float>() }
}
