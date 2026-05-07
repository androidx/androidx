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

package androidx.compose.animation

import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalDeferredTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeferredAnimatedVisibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun visibility_enterGateClosed_defersAnimationUntilReady() {
        lateinit var state: DeferredTransitionState<Boolean>
        var composed by mutableStateOf(false)

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(100, easing = LinearEasing)),
                exit = fadeOut(tween(100, easing = LinearEasing)),
            ) {
                Box(Modifier.size(100.dp).testTag("content"))
                DisposableEffect(Unit) {
                    composed = true
                    onDispose { composed = false }
                }
            }
        }

        // Initially hidden
        rule.onNodeWithTag("content").assertDoesNotExist()
        assertFalse(composed)

        // Act: Trigger visibility but hold the gate
        rule.runOnIdle { state.defer(true) }
        rule.waitForIdle()

        // Assert: Content is composed (prepared) but animation hasn't started
        assertTrue("Content should be composed immediately for preparation", composed)
        rule.onNodeWithTag("content").assertExists()

        // Act: Open gate
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.advanceTimeBy(50)
        rule.onNodeWithTag("content").assertExists()

        rule.mainClock.advanceTimeBy(200)
        rule.onNodeWithTag("content").assertIsDisplayed()
    }

    @Test
    fun visibility_exitGateClosed_holdsContentInExitStateUntilReady() {
        lateinit var state: DeferredTransitionState<Boolean>
        var composed by mutableStateOf(false)

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(100)),
            ) {
                Box(Modifier.size(100.dp).testTag("content"))
                DisposableEffect(Unit) {
                    composed = true
                    onDispose { composed = false }
                }
            }
        }

        rule.waitForIdle()
        assertTrue(composed)

        // Act: Trigger exit but hold the gate closed
        rule.runOnIdle { state.defer(false) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500) // Advance past tween duration

        // Assert: Content should still be visible because gate is closed
        assertTrue("Content should stay composed while exit gate is held", composed)
        rule.onNodeWithTag("content").assertIsDisplayed()

        // Act: Release exit gate
        rule.runOnIdle { state.animateTo(false) }
        rule.mainClock.advanceTimeBy(200)

        // Assert: Content finally disposed
        rule.waitForIdle()
        assertFalse("Content should be disposed after gate opens and animation finishes", composed)
        rule.onNodeWithTag("content").assertIsNotDisplayed()
    }

    @Test
    fun visibility_enterGateFlapping_cannotBeClosedOnceStarted() {
        lateinit var state: DeferredTransitionState<Boolean>

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(1000, easing = LinearEasing)),
            ) {
                Box(Modifier.size(100.dp).testTag("content"))
            }
        }

        rule.runOnIdle { state.defer(true) }
        rule.waitForIdle()

        // Open gate to start (10% progress)
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.advanceTimeBy(100)

        // Act: Try to close gate mid-animation
        rule.runOnIdle { state.defer(true) }
        rule.mainClock.advanceTimeBy(1000)

        // Assert: Animation should complete regardless
        rule.onNodeWithTag("content").assertIsDisplayed()
    }

    @Test
    fun visibility_exitGateFlapping_cannotBeClosedOnceStarted() {
        lateinit var state: DeferredTransitionState<Boolean>

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(visible = { it }, exit = fadeOut(tween(100))) {
                Box(Modifier.size(100.dp).testTag("content"))
            }
        }

        rule.runOnIdle { state.defer(false) }
        rule.waitForIdle()

        // Open gate
        rule.runOnIdle { state.animateTo(false) }
        rule.mainClock.advanceTimeBy(100)

        // Act: Try to close gate mid-exit
        rule.runOnIdle { state.defer(false) }
        rule.mainClock.advanceTimeBy(1000)

        // Assert: Transition completes
        rule.onNodeWithTag("content").assertIsNotDisplayed()
    }

    @Test
    fun transition_childState_remainsInPreEnterDuringPreparation() {
        lateinit var state: DeferredTransitionState<Boolean>
        var currentState: EnterExitState? = null

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(visible = { it }) {
                currentState = this@DeferredAnimatedVisibility.transition.currentState
                Box(Modifier.size(100.dp).testTag("content"))
            }
        }

        // Trigger preparation
        rule.runOnIdle { state.defer(true) }
        rule.waitForIdle()

        assertEquals("Should be held at PreEnter", EnterExitState.PreEnter, currentState)

        // Start animation
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.advanceTimeBy(50)
        assertEquals(
            "Should remain PreEnter during active animation",
            EnterExitState.PreEnter,
            currentState,
        )

        // Settle
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        assertEquals("Should settle at Visible", EnterExitState.Visible, currentState)
    }

    @Test
    fun layout_size_isHeldAtInitialStateDuringPreparation() {
        lateinit var state: DeferredTransitionState<Boolean>
        var size by mutableStateOf(IntSize.Zero)

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = expandIn(tween(100, easing = LinearEasing)) { IntSize.Zero },
                modifier = Modifier.onGloballyPositioned { size = it.size },
            ) {
                Box(Modifier.size(100.dp))
            }
        }

        // Trigger visibility (Gate closed)
        rule.runOnIdle { state.defer(true) }
        rule.waitForIdle()

        // Assert size is held at the initial state (Zero) of expandIn
        assertEquals(IntSize.Zero, size)

        // Open gate and verify animation progress
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.autoAdvance = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(50)

        assertTrue("Size should be animating (>0)", size.width > 0 && size.height > 0)

        rule.mainClock.advanceTimeBy(100)
        val expectedPx = with(rule.density) { 100.dp.roundToPx() }
        assertEquals(IntSize(expectedPx, expectedPx), size)
    }

    @Test
    fun interruption_isDeferred_whenGateIsClosed() {
        lateinit var state: DeferredTransitionState<Boolean>
        var size by mutableStateOf(IntSize.Zero)
        var disposed by mutableStateOf(false)

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)
            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = expandIn(tween(100, easing = LinearEasing)) { IntSize.Zero },
                exit = shrinkOut(tween(100, easing = LinearEasing)) { IntSize.Zero },
                modifier = Modifier.onGloballyPositioned { size = it.size },
            ) {
                Box(Modifier.size(100.dp).testTag("content"))
                DisposableEffect(Unit) { onDispose { disposed = true } }
            }
        }

        // 1. Start Enter animation and go halfway
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.autoAdvance = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(50)
        val halfSize = size.width
        assertTrue(halfSize > 0)

        // 2. Interrupt (visible=false) but close the Exit gate
        rule.runOnIdle { state.defer(false) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // Assert: Instead of reversing (Exit), it should continue Enter because Exit is blocked
        rule.mainClock.advanceTimeBy(20)
        assertTrue("Should continue Enter: ${size.width} > $halfSize", size.width > halfSize)

        // 3. Open Exit gate
        rule.runOnIdle { state.animateTo(false) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Assert: Interruption now proceeds, size decreases
        val sizeBeforeExit = size.width
        rule.mainClock.advanceTimeBy(20)
        assertTrue("Size should now decrease", size.width < sizeBeforeExit)

        // 4. Finish
        rule.mainClock.advanceTimeBy(200)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("content").assertDoesNotExist()
        assertTrue(disposed)
    }

    @Test
    fun visibility_previewScaleSustained_whenNoScaleInTransitions() {
        testTimeSource = { rule.mainClock.currentTime }

        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)

        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                // NO scale in or out!
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()

        val initialWidth = measuredWidth
        assertTrue("Initial width should be > 0", initialWidth > 0f)

        // 1. Start gesture (preview phase)
        rule.runOnIdle {
            state.defer(false) // user triggered back gesture
        }
        rule.mainClock.advanceTimeByFrame()

        // 2. Manipulate scale during gesture
        rule.runOnIdle { previewScale = 0.5f }
        rule.waitForIdle()

        // Assert scale is applied by the preview
        assertEquals(initialWidth * 0.5f, measuredWidth, 1f)

        // 3. Complete gesture (handoff)
        rule.runOnIdle {
            state.animateTo(state.pendingTargetState ?: state.targetState) // gesture lifted
        }

        // 4. Run half of the animation
        rule.mainClock.advanceTimeBy(80L)
        rule.waitForIdle()

        // Assert that the scale is SUSTAINED at 0.5f (use delta of 5f due to velocity absorption)
        assertEquals(initialWidth * 0.5f, measuredWidth, 5f)

        // 5. Run to completion
        rule.mainClock.advanceTimeBy(1000L)
        rule.waitForIdle()

        testTimeSource = null
    }

    @Test
    fun visibility_previewOffsetSustained_whenNoOffsetInTransitions() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewOffset by mutableStateOf(0)

        var measuredX = 0

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                // NO slide in or out!
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                mutableTransform =
                    remember {
                        MutableTransform(offsetVelocityProvider = { Offset.Zero }) { _ ->
                            if (state.pendingTargetState != null) {
                                offset = IntOffset(previewOffset, 0)
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredX = coords.positionInRoot().x.toInt()
                    }
                )
            }
        }

        rule.waitForIdle()
        assertEquals(0, measuredX)

        // 1. Start gesture (preview phase)
        rule.runOnIdle {
            state.defer(false) // user triggered back gesture
        }
        rule.mainClock.advanceTimeByFrame()

        // 2. Manipulate offset during gesture
        rule.runOnIdle { previewOffset = 50 }
        rule.waitForIdle()

        // Assert offset is applied by the preview
        assertEquals(50, measuredX)

        // 3. Complete gesture (handoff)
        rule.runOnIdle {
            state.animateTo(state.pendingTargetState ?: state.targetState) // gesture lifted
        }

        // 4. Run half of the animation
        rule.mainClock.advanceTimeBy(80L)
        rule.waitForIdle()

        // Assert that the offset is SUSTAINED at 50
        assertEquals(50, measuredX)

        // 5. Run to completion
        rule.mainClock.advanceTimeBy(1000L)
        rule.waitForIdle()
    }

    @Test
    fun visibility_previewScale_handoffToExitTransition() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)

        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = scaleIn(tween(160)),
                exit = scaleOut(tween(160), targetScale = 0f),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()
        val initialWidth = measuredWidth

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(false) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle { previewScale = 0.5f }
        rule.waitForIdle()
        assertEquals(initialWidth * 0.5f, measuredWidth, 1f)

        rule.runOnIdle { state.animateTo(state.pendingTargetState ?: state.targetState) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(80L)
        rule.mainClock.advanceTimeByFrame()
        assertTrue(
            "Width should be animating down from 0.5 to 0. Actually: $measuredWidth",
            measuredWidth < initialWidth * 0.5f && measuredWidth > 0f,
        )

        rule.mainClock.advanceTimeBy(1000L)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("content", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun visibility_previewOffset_handoffToEnterTransition() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewOffset by mutableStateOf(0)

        var measuredX = 0

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = slideInHorizontally(tween(160)) { 200 },
                exit = slideOutHorizontally(tween(160)) { 200 },
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                offset = IntOffset(previewOffset, 0)
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).testTag("content").onGloballyPositioned { coords ->
                        measuredX = coords.positionInRoot().x.toInt()
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(true) }
        rule.waitForIdle()

        rule.runOnIdle { previewOffset = 100 }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        // slideIn starts at 200, preview adds 100, so it's 300
        assertEquals(300, measuredX)

        rule.runOnIdle { state.animateTo(state.pendingTargetState ?: state.targetState) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(80L)
        rule.mainClock.advanceTimeByFrame()

        // It should animate from 300 towards 0 (which is the natural resting state of enter
        // transitions)
        assertTrue(
            "Offset should be animating from 300 to 0. Actually: $measuredX",
            measuredX < 300 && measuredX > 0,
        )

        rule.mainClock.advanceTimeBy(1000L)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(0, measuredX)
    }

    @Test
    fun visibility_midAnimation_updateTargetState_combinesTransformAndTransition() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)

        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(false) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = scaleIn(tween(1000, easing = LinearEasing), initialScale = 0f),
                exit = scaleOut(tween(1000, easing = LinearEasing), targetScale = 0f),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // 1. target state updates to true
        rule.runOnIdle { state.animateTo(true) }
        rule.waitForIdle()

        // 2. animation starts, wait for half the animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()

        val expectedFullWidth = with(rule.density) { 100.dp.roundToPx().toFloat() }
        val midAnimationWidth = measuredWidth
        assertEquals(
            "Mid animation width should be roughly half of $expectedFullWidth. Was: $midAnimationWidth",
            expectedFullWidth / 2,
            midAnimationWidth,
            expectedFullWidth * 0.1f,
        )

        // 3. while animation is in progress, update target state back to false,
        // but hold it in non-ready for transition state
        rule.runOnIdle {
            previewScale = 0.5f
            state.defer(false)
        }

        // Advance a frame to allow the preview state and scale to be applied
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        // 4. verify that during this phase, both preview transform values and transition values get
        // applied. The underlying transition continues to grow, but is now visually scaled down by
        // the preview.
        val widthWithPreview1 = measuredWidth
        assertTrue(
            "Width should be scaled down by 0.5. midAnimationWidth=$midAnimationWidth, widthWithPreview1=$widthWithPreview1",
            widthWithPreview1 < midAnimationWidth,
        )

        // Advance another frame to prove the underlying transition is still continuing
        rule.mainClock.advanceTimeByFrame()
        val widthWithPreview2 = measuredWidth

        assertTrue(
            "Underlying transition should continue, causing width to grow. widthWithPreview1=$widthWithPreview1, widthWithPreview2=$widthWithPreview2",
            widthWithPreview2 > widthWithPreview1,
        )
    }

    @Test
    fun visibility_doublePreview_doesNotSnap() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)
        var applyScalePreview by mutableStateOf(true)

        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null && applyScalePreview) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()
        val fullWidth = measuredWidth

        rule.mainClock.autoAdvance = false

        // 1. First preview
        rule.runOnIdle {
            state.defer(false)
            previewScale = 0.5f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(fullWidth * 0.5f, measuredWidth, 1f)

        // 2. Commit first preview, transition starts
        rule.runOnIdle { state.animateTo(state.pendingTargetState ?: state.targetState) }
        rule.mainClock.advanceTimeByFrame()

        // Advance slightly so the transition is halfway
        rule.mainClock.advanceTimeBy(80L)
        rule.mainClock.advanceTimeByFrame()

        val widthDuringExit = measuredWidth
        assertEquals(
            "Should be sustaining at 0.5f during exit",
            fullWidth * 0.5f,
            widthDuringExit,
            1f,
        )

        // 3. Interrupt with second preview (without scale transform)
        rule.runOnIdle {
            applyScalePreview = false
            state.defer(true)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val widthAfterSecondPreview = measuredWidth
        assertEquals(
            "Should be sustaining at 0.5f during interrupted exit",
            fullWidth * 0.5f,
            widthDuringExit,
            1f,
        )

        // end preview and animate back to visible
        rule.runOnIdle { state.animateTo(state.pendingTargetState ?: state.targetState) }

        rule.mainClock.advanceTimeByFrame()
        assertTrue(
            "Scale should not snap to full width",
            widthAfterSecondPreview < fullWidth * 0.9f,
        )

        // verify scale animates back to 1f with the interrupted transition
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals("Should animate to fullWidth!", fullWidth, measuredWidth)
    }

    @Test
    fun visibility_previewScale_handoff_thenInterrupted_isSeamless() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)
        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                // Use linear easing and long duration to make progress predictable
                enter = scaleIn(tween(1000, easing = LinearEasing), initialScale = 0f),
                exit = scaleOut(tween(1000, easing = LinearEasing), targetScale = 0f),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()
        val fullWidth = measuredWidth
        rule.mainClock.autoAdvance = false

        // 1. Deferred phase (e.g. back gesture)
        rule.runOnIdle {
            state.defer(false)
            previewScale = 0.8f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(fullWidth * 0.8f, measuredWidth, 1f)

        // 2. Handoff to exit transition
        rule.runOnIdle { state.animateTo(false) }
        rule.mainClock.advanceTimeByFrame() // Handoff frame

        // 3. Let it animate for a bit (e.g. to 0.4f scale)
        // Transition goes from 1.0 to 0.0. Handoff scale is 0.8.
        // At 50% of the transition, scale should be 0.4.
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        val widthBeforeInterruption = measuredWidth
        assertEquals(fullWidth * 0.4f, widthBeforeInterruption, 5f)

        // 4. Interrupt mid-animation (e.g. user cancels back gesture)
        // This should clear the sustained 0.8f handoff value and start a new transition from 0.4f.
        rule.runOnIdle { state.animateTo(true) }
        rule.mainClock.advanceTimeByFrame() // Interruption frame
        rule.waitForIdle()

        // 5. Verify it is seamless (no snap back to 0.8f or jump to 1.0f)
        val widthAfterInterruption = measuredWidth
        assertEquals(
            "Width should not snap back or jump after interruption",
            widthBeforeInterruption,
            widthAfterInterruption,
            5f,
        )

        // 6. Verify it continues to animate towards full width
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()
        assertTrue(
            "Width should be increasing towards fullWidth. " +
                "Was $widthAfterInterruption, now $measuredWidth",
            measuredWidth > widthAfterInterruption,
        )

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(fullWidth, measuredWidth, 1f)
    }

    @Test
    fun visibility_previewScale_handoffVelocity() {
        testTimeSource = { rule.mainClock.currentTime }

        lateinit var state: DeferredTransitionState<Boolean>
        var previewScale by mutableStateOf(1f)

        var measuredWidth = 0f

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(160)),
                exit = scaleOut(spring(stiffness = Spring.StiffnessVeryLow)),
                mutableTransform =
                    remember {
                        MutableTransform { _ ->
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredWidth = coords.boundsInRoot().width
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        fun simulateGesture(isFast: Boolean): Float {
            rule.runOnIdle { state.defer(false) }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()

            val steps =
                if (isFast) {
                    listOf(0.9f, 0.7f, 0.5f)
                } else {
                    listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
                }
            for (s in steps) {
                previewScale = s
                rule.mainClock.advanceTimeByFrame()
                rule.waitForIdle()
            }

            val lastGestureWidth = measuredWidth

            rule.runOnIdle { state.animateTo(false) }
            repeat(3) { rule.mainClock.advanceTimeByFrame() }
            rule.waitForIdle()

            return lastGestureWidth - measuredWidth
        }

        // Scenario 1: Slow gesture
        val widthDropSlow = simulateGesture(isFast = false)

        // Reset
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { state.animateTo(true) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        previewScale = 1f

        // Scenario 2: Fast gesture
        val widthDropFast = simulateGesture(isFast = true)

        assertTrue(
            "Expected width drop with fast gesture ($widthDropFast) to be greater than with slow gesture ($widthDropSlow)",
            widthDropFast > widthDropSlow,
        )

        testTimeSource = null
    }

    @Test
    fun visibility_previewOffset_handoffVelocity() {
        lateinit var state: DeferredTransitionState<Boolean>
        var previewVelocity by mutableStateOf(Offset.Zero)
        var previewOffset = 100

        var measuredX = 0

        rule.setContent {
            state = remember { DeferredTransitionState(true) }
            val transition = rememberTransition(state)

            transition.DeferredAnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(160)),
                exit = slideOutHorizontally(spring(stiffness = Spring.StiffnessVeryLow)) { it },
                mutableTransform =
                    remember {
                        MutableTransform(
                            offsetVelocityProvider = {
                                Offset(previewVelocity.x, previewVelocity.y)
                            }
                        ) { _ ->
                            if (state.pendingTargetState != null) {
                                offset = IntOffset(previewOffset, 0)
                            }
                        }
                    },
            ) {
                Box(
                    Modifier.size(100.dp).onGloballyPositioned { coords ->
                        measuredX = coords.positionInRoot().x.toInt()
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // Scenario 1: Zero velocity
        rule.runOnIdle { state.defer(false) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(previewOffset, measuredX)

        rule.runOnIdle { state.animateTo(false) }
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()

        val xNoVelocity = measuredX

        // Reset
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { state.animateTo(true) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // Scenario 2: Large positive velocity
        rule.runOnIdle {
            previewVelocity = Offset(2000f, 0f) // Move right very fast
            state.defer(false)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(previewOffset, measuredX)

        rule.runOnIdle { state.animateTo(false) }
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()

        val xWithVelocity = measuredX

        assertTrue(
            "Expected X with positive velocity ($xWithVelocity) to be greater than with zero velocity ($xNoVelocity)",
            xWithVelocity > xNoVelocity,
        )
    }
}
