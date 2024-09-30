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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.A
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.B
import androidx.compose.foundation.anchoredDraggable.AnchoredDraggableTestValue.C
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableMinFlingVelocity
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class AnchoredDraggableStateTest(testNewBehavior: Boolean) :
    AnchoredDraggableBackwardsCompatibleTest(testNewBehavior) {

    private val AnchoredDraggableTestTag = "dragbox"
    private val AnchoredDraggableBoxSize = 200.dp

    @Test
    fun anchoredDraggable_state_canSkipStateByFling() {
        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Vertical)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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
        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Vertical)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
                        .onSizeChanged { layoutSize ->
                            state.updateAnchors(
                                DraggableAnchors {
                                    A at 0f
                                    B at layoutSize.height / 2f
                                    C at layoutSize.height.toFloat()
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
        val snapAnimationSpec = tween<Float>(animationDuration, easing = LinearEasing)
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                orientation = Orientation.Vertical,
                snapAnimationSpec = snapAnimationSpec,
                positionalThreshold = { distance -> distance * 0.5f }
            )

        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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

        scope.launch { state.animateTo(targetValue = B, snapAnimationSpec) }
        rule.mainClock.advanceTimeBy(1 * frameLengthMillis)

        assertWithMessage("Current state").that(state.currentValue).isEqualTo(A)
        assertWithMessage("Target state").that(state.targetValue).isEqualTo(B)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertWithMessage("Current state").that(state.currentValue).isEqualTo(B)
        assertWithMessage("Target state").that(state.targetValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_targetState_updatedWithDeltaDispatch() {
        val state =
            createAnchoredDraggableState(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 200f
                        C at 400f
                    }
            )
        val flingBehavior = createAnchoredDraggableFlingBehavior(state, rule.density)

        val initialOffset = state.requireOffset()

        // Swipe towards B, close before threshold
        val aToBThreshold = abs(state.anchors.positionOf(A) - state.anchors.positionOf(B)) / 2f
        state.dispatchRawDelta(initialOffset + (aToBThreshold * 0.9f))

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        // Swipe towards B, close after threshold
        state.dispatchRawDelta(aToBThreshold * 0.2f)

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) { performFling(flingBehavior, state, 0f) }

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        // Swipe towards A, close before threshold
        state.dispatchRawDelta(-(aToBThreshold * 0.9f))

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        // Swipe towards A, close after threshold
        state.dispatchRawDelta(-(aToBThreshold * 0.2f))

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { performFling(flingBehavior, state, 0f) }

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun anchoredDraggable_currentValue_updatedWithDeltaDispatch() =
        runBlocking(AutoTestFrameClock()) {
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 20f
                            C at 40f
                        }
                )

            state.testProgression(from = A, to = B, valueUnderTest = { currentValue })
            state.testProgression(from = B, to = C, valueUnderTest = { currentValue })
            state.testProgression(from = C, to = B, valueUnderTest = { currentValue })
            state.testProgression(from = B, to = A, valueUnderTest = { currentValue })
        }

    @Test
    fun anchoredDraggable_progress() {
        rule.mainClock.autoAdvance = false
        val animationDuration = 320
        val frameLengthMillis = 16
        val amountOfFramesForAnimation = animationDuration / frameLengthMillis
        val snapAnimationSpec = tween<Float>(animationDuration, easing = LinearEasing)
        val (state, modifier) =
            createStateAndModifier(
                initialValue = A,
                snapAnimationSpec = snapAnimationSpec,
                orientation = Orientation.Vertical
            )
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
        assertThat(state.progress(from = A, to = B)).isEqualTo(0f)

        scope.launch { state.animateTo(B, snapAnimationSpec) }
        rule.mainClock.advanceTimeByFrame() // Start dispatching and running the animation

        repeat(amountOfFramesForAnimation) { frame ->
            val frameFraction = (frame / amountOfFramesForAnimation.toFloat())
            val hiddenToHalfExpandedProgress = state.progress(from = A, to = B)
            val hiddenToExpandedProgress = state.progress(from = A, to = C)
            assertThat(hiddenToHalfExpandedProgress).isWithin(0.001f).of(frameFraction)
            assertThat(hiddenToExpandedProgress).isWithin(0.001f).of(frameFraction / 2f)
            rule.mainClock.advanceTimeByFrame()
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        scope.launch { state.animateTo(A, snapAnimationSpec) }
        rule.mainClock.advanceTimeByFrame() // Start dispatching and running the animation

        repeat(amountOfFramesForAnimation) { frame ->
            val frameFraction = (frame / amountOfFramesForAnimation.toFloat())
            val aToBProgress = state.progress(from = A, to = B)
            val aToCProgress = state.progress(from = A, to = C)

            assertThat(aToBProgress).isWithin(0.001f).of(1 - frameFraction)
            assertThat(aToCProgress).isWithin(0.001f).of(0.5f - (frameFraction / 2f))
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun anchoredDraggable_snapTo_updatesImmediately() = runBlocking {
        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Vertical)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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
    fun anchoredDraggable_saver_restoresCurrentValue() {
        val restorationTester = StateRestorationTester(rule)

        val initialState = C
        lateinit var state: AnchoredDraggableState<AnchoredDraggableTestValue>
        lateinit var scope: CoroutineScope

        restorationTester.setContent {
            state =
                rememberSaveable(saver = AnchoredDraggableState.Saver()) {
                    createAnchoredDraggableState(initialState)
                }
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

        scope.launch { state.animateTo(B, DefaultSnapAnimationSpec) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)

        val stateBeforeSavedInstanceStateRestore = state
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(stateBeforeSavedInstanceStateRestore).isNotSameInstanceAs(state)
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_accessInInitialComposition_targetState() {
        lateinit var targetState: AnchoredDraggableTestValue
        rule.setContent {
            val (state, modifier) =
                remember {
                    createStateAndModifier(initialValue = B, orientation = Orientation.Horizontal)
                }
            targetState = state.targetValue
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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
            val (state, modifier) =
                remember {
                    createStateAndModifier(initialValue = B, orientation = Orientation.Horizontal)
                }
            progress = state.progress(from = A, to = B)
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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

    @Ignore("Todo: Fix differences between tests and real code - this shouldn't work :)")
    @Test
    fun anchoredDraggable_requireOffset_accessedInInitialComposition_throws() {
        var exception: Throwable? = null
        val (state, modifier) =
            createStateAndModifier(initialValue = B, orientation = Orientation.Horizontal)
        var offset: Float? = null
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(AnchoredDraggableBoxSize)
                        .testTag(AnchoredDraggableTestTag)
                        .then(modifier)
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
            SideEffect {
                exception = runCatching { offset = state.requireOffset() }.exceptionOrNull()
            }
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
                createAnchoredDraggableState(
                    initialValue = B,
                    anchors = DraggableAnchors { B at 100f }
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

        val snapAnimationSpec = FloatSpringSpec(dampingRatio = Spring.DampingRatioHighBouncy)
        val animationDuration =
            snapAnimationSpec
                .getDurationNanos(
                    initialValue = minBound,
                    targetValue = maxBound,
                    initialVelocity = 0f
                )
                .let { TimeUnit.NANOSECONDS.toMillis(it) }

        val (state, modifier) =
            createStateAndModifier(initialValue = A, orientation = Orientation.Vertical)
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            SideEffect { state.updateAnchors(anchors) }
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

        scope.launch { state.animateTo(C, snapAnimationSpec) }
        var highestOffset = 0f
        for (i in 0..animationDuration step 16) {
            highestOffset = state.requireOffset()
            rule.mainClock.advanceTimeBy(16)
        }
        assertThat(highestOffset).isGreaterThan(anchors.positionOf(C))
    }

    @Test
    fun anchoredDraggable_targetNotInAnchors_animateTo_updatesCurrentValue() {
        val state = createAnchoredDraggableState(initialValue = A)
        assertThat(state.anchors.size).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.animateTo(B, tween()) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_targetNotInAnchors_snapTo_updatesCurrentValue() {
        val state = createAnchoredDraggableState(initialValue = A)
        assertThat(state.anchors.size).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.snapTo(B) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun anchoredDraggable_updateAnchors_noOngoingDrag_shouldUpdateOffset() {
        val anchoredDraggableState = createAnchoredDraggableState(initialValue = A)

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

    @Test
    fun anchoredDraggable_updateAnchors_ongoingDrag_shouldRestartDrag() = runTest {
        // Given an anchored draggable state
        val anchoredDraggableState = createAnchoredDraggableState(initialValue = 1)

        val anchorUpdates = Channel<DraggableAnchors<Int>>()
        val dragJob = launch {
            anchoredDraggableState.anchoredDrag { newAnchors ->
                anchorUpdates.send(newAnchors)
                suspendIndefinitely()
            }
        }

        val firstAnchors = anchorUpdates.receive()
        assertThat(firstAnchors.size).isZero()

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

    @Test
    fun anchoredDraggable_updateAnchors_anchoredDrag_invokedWithLatestAnchors() = runTest {
        // Given an anchored draggable state
        val anchoredDraggableState = createAnchoredDraggableState(initialValue = 1)

        val anchorUpdates = Channel<DraggableAnchors<Int>>()
        val dragJob =
            launch(Dispatchers.Unconfined) {
                anchoredDraggableState.anchoredDrag { newAnchors ->
                    anchorUpdates.send(newAnchors)
                    suspendIndefinitely()
                }
            }

        val firstAnchors = anchorUpdates.receive()
        assertThat(firstAnchors.size).isZero()

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

    @Test
    fun anchoredDraggable_updateAnchors_anchoredDrag_invokedWithLatestTarget() = runTest {
        val anchoredDraggableState = createAnchoredDraggableState(initialValue = A)
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
    fun anchoredDraggable_dragCompletesExceptionally_cleansUp() = runTest {
        val anchoredDraggableState = createAnchoredDraggableState(initialValue = A)
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
    fun anchoredDraggable_customDrag_doesNotSnapToClosestAnchor() = runBlocking {
        val state =
            createAnchoredDraggableState(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 200f
                        C at 300f
                    }
            )

        state.anchoredDrag { dragTo(150f) }

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.requireOffset()).isEqualTo(150f)

        state.anchoredDrag { dragTo(260f) }
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.requireOffset()).isEqualTo(260f)
    }

    @Test
    fun anchoredDraggable_customDrag_updatesVelocity() = runBlocking {
        val state = createAnchoredDraggableState(initialValue = A)
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

        val state = createAnchoredDraggableState(initialValue = A)
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
        val state = createAnchoredDraggableState(initialValue = A, anchors = anchors)
        assertThat(state.anchors).isEqualTo(anchors)
        assertThat(state.offset).isEqualTo(initialValueOffset)
    }

    @Test
    fun anchoredDraggable_constructorWithAnchors_initialValueNotInAnchors_updatesCurrentValue() {
        val anchors = DraggableAnchors { B at 200f }
        val state = createAnchoredDraggableState(initialValue = A, anchors = anchors)
        assertThat(state.anchors).isEqualTo(anchors)
        assertThat(state.offset).isNaN()
    }

    @Test
    fun anchoredDraggable_customDrag_settleOnInvalidState_shouldRespectConfirmValueChange() =
        runBlocking {
            var shouldBlockValueC = false
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    confirmValueChange = {
                        if (shouldBlockValueC) it != C // block state value C
                        else true
                    },
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

    @Test
    fun anchoredDraggable_animateToTarget_zeroVelocity_usesSnapAnimationSpec() =
        runBlocking(AutoTestFrameClock()) {
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec = inspectDecayAnimationSpec.generateDecayAnimationSpec<Float>()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))

            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        }
                )

            assertThat(state.currentValue).isEqualTo(A)

            state.animateToWithDecay(
                targetValue = B,
                velocity = 0f,
                snapAnimationSpec = tweenAnimationSpec,
                decayAnimationSpec = decayAnimationSpec
            )

            assertThat(state.currentValue).isEqualTo(B)

            // since velocity is zero, target animation will be used
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_animateToTarget_canNotDecayToTarget_positiveVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        }
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(A)

            // dragging across the positionalThreshold to settle at anchor B
            val delta = distance * positionalThreshold * 1.1f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfA + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            val velocity = 500f // velocity not high enough to perform decay animation
            val projectedTarget =
                decayAnimationSpec.calculateTargetValue(state.requireOffset(), velocity)
            assertThat(projectedTarget).isLessThan(positionOfB)

            state.animateToWithDecay(B, velocity, tweenAnimationSpec, decayAnimationSpec)
            assertThat(state.currentValue).isEqualTo(B)

            // velocity is not enough to perform decay animation, target animation will be used
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_animateToTarget_canNotDecayToTarget_negativeVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        }
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(B)

            // dragging across the positionalThreshold to settle at anchor A
            val delta = -distance * positionalThreshold * 1.1f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfB + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            val velocity = -500f // velocity not high enough to perform decay animation
            val projectedTarget = decayAnimationSpec.calculateTargetValue(newOffset, velocity)
            assertThat(projectedTarget).isGreaterThan(positionOfA)

            state.animateToWithDecay(A, velocity, tweenAnimationSpec, decayAnimationSpec)
            assertThat(state.currentValue).isEqualTo(A)

            // velocity is not enough to perform decay animation, target animation will be used
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_animateToTarget_canDecayToTarget_positiveVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        }
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(A)

            // dragging across the positionalThreshold to settle at anchor B
            val delta = distance * positionalThreshold * 1.1f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfA + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            val velocity = 2000f // velocity high enough to perform decay animation
            val projectedTarget = decayAnimationSpec.calculateTargetValue(newOffset, velocity)
            assertThat(projectedTarget).isAtLeast(positionOfB)

            state.animateToWithDecay(B, velocity, tweenAnimationSpec, decayAnimationSpec)
            assertThat(state.currentValue).isEqualTo(B)

            // velocity is enough to perform decay animation
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(1)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(0)
        }

    @Test
    fun anchoredDraggable_animateToTarget_canDecayToTarget_negativeVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        }
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(B)

            // dragging across the positionalThreshold to settle at anchor A
            val delta = -distance * positionalThreshold * 1.1f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfB + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            val velocity = -2000f // velocity high enough to perform decay animation
            val projectedTarget = decayAnimationSpec.calculateTargetValue(newOffset, velocity)
            assertThat(projectedTarget).isAtMost(positionOfA)

            state.animateToWithDecay(A, velocity, tweenAnimationSpec, decayAnimationSpec)
            assertThat(state.currentValue).isEqualTo(A)

            // velocity is not enough to perform decay animation
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(1)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(0)
        }

    @Test
    fun anchoredDraggable_performFling_positiveOffset_positiveVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val velocityPx = with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
            val positionalThreshold = 0.5f
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        },
                    positionalThreshold = { it * positionalThreshold },
                    snapAnimationSpec = tweenAnimationSpec
                )

            val flingBehavior =
                createAnchoredDraggableFlingBehavior(
                    state,
                    rule.density,
                    positionalThreshold = { it * positionalThreshold },
                    snapAnimationSpec = tweenAnimationSpec
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(A)

            // dragging but not crossing positional threshold
            val delta = distance * positionalThreshold * 0.9f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfA + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            // target anchor did not change (still at A) since velocityThreshold and
            // positionalThreshold were not crossed.
            performFling(flingBehavior, state, velocityPx * 0.9f)

            assertThat(state.currentValue).isEqualTo(A)

            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_performFling_positiveOffset_negativeVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val velocityPx = with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 200f
                        },
                    positionalThreshold = { it * positionalThreshold },
                    decayAnimationSpec = decayAnimationSpec,
                    snapAnimationSpec = tweenAnimationSpec
                )
            val flingBehavior =
                createAnchoredDraggableFlingBehavior(
                    state,
                    density = rule.density,
                    positionalThreshold = { it * positionalThreshold },
                    snapAnimationSpec = tweenAnimationSpec
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(B)

            val delta = -distance * positionalThreshold * 0.9f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfB + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            // target anchor did not change (still at B) since velocityThreshold and
            // positionalThreshold were not crossed.
            performFling(flingBehavior, state, -velocityPx * 0.9f)

            assertThat(state.currentValue).isEqualTo(B)

            // assert that target animation is used, not decay animation because the target anchor
            // is not in the same direction of the drag (sign of velocity)
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_dragNotInTheSameDirectionOfTarget_negativeOffset_negativeVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val velocityPx = with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at -200f
                        },
                    positionalThreshold = { it * positionalThreshold },
                    decayAnimationSpec = decayAnimationSpec,
                    snapAnimationSpec = tweenAnimationSpec
                )
            val flingBehavior =
                createAnchoredDraggableFlingBehavior(
                    state,
                    rule.density,
                    positionalThreshold = { it * positionalThreshold },
                    snapAnimationSpec = tweenAnimationSpec
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(A)

            val delta = -distance * positionalThreshold * 0.9f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfA + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            // target anchor did not change (still at A) since velocityThreshold and
            // positionalThreshold were not crossed.
            performFling(flingBehavior, state, -velocityPx * 0.9f)

            assertThat(state.currentValue).isEqualTo(A)

            // assert that target animation is used, not decay animation because the target anchor
            // is not in the same direction of the drag (sign of velocity)
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Test
    fun anchoredDraggable_dragNotInTheSameDirectionOfTarget_negativeOffset_positiveVelocity() =
        runBlocking(AutoTestFrameClock()) {
            val velocityPx = with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
            val positionalThreshold = 0.5f
            val inspectDecayAnimationSpec =
                InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
            val decayAnimationSpec: DecayAnimationSpec<Float> =
                inspectDecayAnimationSpec.generateDecayAnimationSpec()
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = B,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at -200f
                        },
                    positionalThreshold = { it * positionalThreshold },
                    decayAnimationSpec = decayAnimationSpec,
                    snapAnimationSpec = tweenAnimationSpec
                )
            val flingBehavior =
                createAnchoredDraggableFlingBehavior(
                    state,
                    rule.density,
                    positionalThreshold = { it * positionalThreshold },
                    snapAnimationSpec = tweenAnimationSpec
                )

            val positionOfA = state.anchors.positionOf(A)
            val positionOfB = state.anchors.positionOf(B)
            val distance = abs(positionOfA - positionOfB)

            assertThat(state.currentValue).isEqualTo(B)

            val delta = distance * positionalThreshold * 0.9f
            state.dispatchRawDelta(delta)
            val newOffset = positionOfB + delta
            assertThat(state.requireOffset()).isEqualTo(newOffset)

            // target anchor did not change (still at B) since velocityThreshold and
            // positionalThreshold were not crossed.
            performFling(flingBehavior, state, velocityPx * 0.9f)

            assertThat(state.currentValue).isEqualTo(B)

            // assert that target animation is used, not decay animation because the target anchor
            // is not in the same direction of the drag (sign of velocity)
            assertThat(inspectDecayAnimationSpec.animationWasExecutions).isEqualTo(0)
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(1)
        }

    @Ignore("Clarify dispatchRawDelta contract b/344568351")
    @Test
    fun anchoredDraggable_settleWhenOffsetEqualsTargetOffset() =
        runBlocking(AutoTestFrameClock()) {
            val tweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))
            val state =
                createAnchoredDraggableState(
                    initialValue = A,
                    anchors =
                        DraggableAnchors {
                            A at 0f
                            B at 250f
                        }
                )
            val flingBehavior =
                createAnchoredDraggableFlingBehavior(
                    state,
                    rule.density,
                    snapAnimationSpec = tweenAnimationSpec
                )

            val positionA = state.anchors.positionOf(A)
            val positionB = state.anchors.positionOf(B)
            val distance = abs(positionA - positionB)

            assertThat(state.currentValue).isEqualTo(A)
            assertThat(state.offset).isEqualTo(positionA)

            state.dispatchRawDelta(distance)

            assertThat(state.offset).isEqualTo(positionB)

            performFling(flingBehavior, state, 1000f)

            // Assert that the component settled at positionB (anchor B)
            assertThat(state.offset).isEqualTo(positionB)

            // since offset == positionB, decay animation is used
            assertThat(tweenAnimationSpec.animationWasExecutions).isEqualTo(0)
        }

    @Test
    fun anchoredDraggable_animateTo_alreadyAtTarget_noOps() {
        val state =
            createAnchoredDraggableState(
                initialValue = B,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 200f
                        C at 300f
                    }
            )
        val clock = HandPumpTestFrameClock()
        val scope = CoroutineScope(clock)

        assertThat(state.offset).isEqualTo(200f)
        scope.launch { state.animateTo(B, DefaultSnapAnimationSpec) }
        runBlocking { clock.advanceByFrame() } // Advance only one frame, we should be done
        assertThat(state.offset).isEqualTo(200f)
    }

    @Test
    fun anchoredDraggable_animateToWithDecay_alreadyAtTarget_noOps() {
        val state =
            createAnchoredDraggableState(
                initialValue = B,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 200f
                        C at 300f
                    }
            )
        val clock = HandPumpTestFrameClock()
        val scope = CoroutineScope(clock)

        assertThat(state.offset).isEqualTo(200f)
        scope.launch {
            state.animateToWithDecay(
                targetValue = B,
                velocity = 100f,
                snapAnimationSpec = DefaultSnapAnimationSpec,
                decayAnimationSpec = DefaultDecayAnimationSpec
            )
        }
        runBlocking { clock.advanceByFrame() } // Advance only one frame, we should be done
        assertThat(state.offset).isEqualTo(200f)
    }

    @Test
    fun anchoredDraggable_fling_offsetPastHalfwayBetweenAnchors_beforePosThreshold_doesntAdvance() {
        val velocityThreshold = with(rule.density) { 125.dp.toPx() }
        val positionalThreshold: (Float) -> Float = { it * 0.9f }
        val state =
            createAnchoredDraggableState(
                initialValue = A,
                anchors =
                    DraggableAnchors {
                        A at 0f
                        B at 100f
                        C at 200f
                    },
                positionalThreshold = positionalThreshold
            )
        val flingBehavior =
            createAnchoredDraggableFlingBehavior(
                state = state,
                density = rule.density,
                positionalThreshold = positionalThreshold,
                snapAnimationSpec = tween()
            )

        state.dispatchRawDelta(80f)

        assertThat(state.offset).isEqualTo(80f)
        assertThat(state.settledValue).isEqualTo(A)
        assertThat(state.currentValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) {
            performFling(flingBehavior, state, velocityThreshold - 1f)
        }

        assertThat(state.offset).isEqualTo(0f)
        assertThat(state.settledValue).isEqualTo(A)
        assertThat(state.currentValue).isEqualTo(A)
    }

    /** Test the [valueUnderTest] progressively for each delta from [from] to [to]. */
    private suspend fun <T> AnchoredDraggableState<T>.testProgression(
        valueUnderTest: AnchoredDraggableState<T>.() -> Any,
        from: T,
        to: T
    ) {
        anchoredDrag { anchors ->
            val origin = anchors.positionOf(from).roundToInt()
            val destination = anchors.positionOf(to).roundToInt()
            val distance = abs(origin - destination)
            for (value in origin..destination) {
                dragTo(value.toFloat())
                val expectedCurrentValue = if (value < origin + (distance / 2)) from else to
                assertWithMessage(
                        "Going from $from@$origin to $to@$destination (distance $distance). " +
                            "Dragged to $value, offset is now $offset but value is unexpected."
                    )
                    .that(valueUnderTest())
                    .isEqualTo(expectedCurrentValue)
            }
        }
        settle(AnchoredDraggableDefaults.SnapAnimationSpec)
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

    private class InspectSplineAnimationSpec(
        private val splineBasedFloatDecayAnimationSpec: SplineBasedFloatDecayAnimationSpec
    ) : FloatDecayAnimationSpec by splineBasedFloatDecayAnimationSpec {

        private var valueFromNanosCalls = 0
        val animationWasExecutions: Int
            get() = valueFromNanosCalls / 2

        override fun getValueFromNanos(
            playTimeNanos: Long,
            initialValue: Float,
            initialVelocity: Float
        ): Float {

            if (playTimeNanos == 0L) {
                valueFromNanosCalls++
            }

            return splineBasedFloatDecayAnimationSpec.getValueFromNanos(
                playTimeNanos,
                initialValue,
                initialVelocity
            )
        }
    }

    private val DefaultSnapAnimationSpec = tween<Float>()

    private val DefaultDecayAnimationSpec: DecayAnimationSpec<Float> =
        SplineBasedFloatDecayAnimationSpec(rule.density).generateDecayAnimationSpec()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testNewBehavior={0}")
        fun params() = listOf(false, true)
    }
}
