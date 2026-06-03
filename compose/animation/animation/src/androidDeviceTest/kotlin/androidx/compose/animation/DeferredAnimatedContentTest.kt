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
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalDeferredTransitionApi::class, ExperimentalTransitionApi::class)
class DeferredAnimatedContentTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun waitToEnterTest() {
        val state = DeferredTransitionState("A")

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(100, easing = LinearEasing)) togetherWith
                        fadeOut(tween(100, easing = LinearEasing))
                }
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        // Initial state A
        rule.onNodeWithTag("content_A").assertExists()
        rule.waitForIdle()
        rule.onNodeWithTag("content_A").assertIsDisplayed()
        rule.onNodeWithTag("content_B").assertDoesNotExist()

        // Switch to B, but not ready
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        // A should still be displayed (Exit held)
        rule.onNodeWithTag("content_A").assertExists()

        // B should be composed but not displayed (Enter held at PreEnter)
        rule.onNodeWithTag("content_B").assertExists()

        // Make B ready
        rule.runOnIdle { state.animateTo("B") }
        rule.waitForIdle()

        // Wait for animation to finish
        rule.waitForIdle()

        // A gone, B displayed
        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertIsDisplayed()
    }

    @Test
    fun immediateEnterTest() {
        val state = DeferredTransitionState("A")

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(100, easing = LinearEasing)) togetherWith
                        fadeOut(tween(100, easing = LinearEasing))
                }
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Switch to B
        rule.runOnIdle { state.animateTo("B") }
        rule.waitForIdle()

        // Should transition immediately (B exists, A exists during transition)
        rule.onNodeWithTag("content_B").assertExists()

        // Wait for finish
        rule.waitForIdle()

        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertIsDisplayed()
    }

    @Test
    fun immediateExitTest() {
        val state = DeferredTransitionState("A")

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(1000, easing = LinearEasing)) togetherWith
                        fadeOut(tween(1000, easing = LinearEasing))
                }
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        // Initial state
        rule.waitForIdle()
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Trigger exit (A->B), but keep B not ready
        rule.runOnIdle { state.defer("B") }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeByFrame()

        // A should still be displayed, as its exit is held by isReady
        rule.onNodeWithTag("content_A").assertIsDisplayed()
        rule.onNodeWithTag("content_B").assertExists() // B is composed, but not displayed

        // Now make B ready, A's exit animation should start
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()

        // A should still be displayed (fading out)
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Advance time to finish animation
        rule.mainClock.advanceTimeBy(1000) // Finish fadeOut(1000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // A should be gone, B should be displayed
        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertIsDisplayed()
    }

    @Test
    fun sizeChangeDelayedUntilReady() {
        val state = DeferredTransitionState("A")
        var containerSize by mutableStateOf(IntSize.Zero)

        rule.setContent {
            Box(Modifier.onGloballyPositioned { containerSize = it.size }) {
                val transition = rememberTransition(state)
                transition.DeferredAnimatedContent(
                    transitionSpec = {
                        fadeIn(tween(100, easing = LinearEasing)) togetherWith
                            fadeOut(tween(100, easing = LinearEasing))
                    }
                ) { target ->
                    Box(
                        Modifier.size(if (target == "A") 100.dp else 200.dp)
                            .testTag("content_" + target)
                    )
                }
            }
        }

        rule.onNodeWithTag("content_A").assertIsDisplayed()
        val sizeA = with(rule.density) { 100.dp.roundToPx() }
        val sizeB = with(rule.density) { 200.dp.roundToPx() }

        // Initial check
        rule.runOnIdle {
            assertTrue(
                "Container size should be A ($sizeA) but was $containerSize",
                containerSize.width == sizeA && containerSize.height == sizeA,
            )
        }

        // Switch to B, but not ready
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        // Size should STILL be A (100)
        rule.runOnIdle {
            assertTrue(
                "Container size should remain A ($sizeA) while waiting, but was $containerSize",
                containerSize.width == sizeA && containerSize.height == sizeA,
            )
        }

        // Make B ready
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame() // Start animation

        // Now it should start animating towards B (size increasing)
        rule.runOnIdle {
            // After animation finishes
            assertTrue(
                "Container size should be B ($sizeB) after animation, but was $containerSize",
                containerSize.width == sizeB && containerSize.height == sizeB,
            )
        }
    }

    @Test
    fun verifyExitAnimationAfterGateOpens() {
        val state = DeferredTransitionState("A")

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(1000, easing = LinearEasing)) togetherWith
                        fadeOut(tween(1000, easing = LinearEasing))
                }
            ) { target ->
                Box(
                    Modifier.size(100.dp)
                        .testTag("content_$target")
                        .background(if (target == "A") Color.Red else Color.Green)
                )
            }
        }

        // Initial state A
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Switch to B, but not ready
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        // A should still be displayed
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Make B ready
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Advance clock by 500ms
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()

        // A should still be displayed (fading out)
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Finish animation
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertIsDisplayed()
    }

    @Test
    fun flappingTest() {
        val state = DeferredTransitionState("A")
        var containerSize by mutableStateOf(IntSize.Zero)

        rule.setContent {
            Box(Modifier.onGloballyPositioned { containerSize = it.size }) {
                val transition = rememberTransition(state)
                transition.DeferredAnimatedContent(
                    transitionSpec = {
                        // Use Linear easing for predictable progress
                        (fadeIn(tween(100, easing = LinearEasing)) togetherWith
                            fadeOut(tween(100, easing = LinearEasing))) using
                            SizeTransform { _, _ -> tween(100, easing = LinearEasing) }
                    }
                ) { target ->
                    Box(
                        Modifier.size(if (target == "A") 100.dp else 200.dp)
                            .testTag("content_$target")
                    )
                }
            }
        }

        // Initial A (100dp)
        rule.waitForIdle()
        val sizeA = with(rule.density) { 100.dp.roundToPx() }
        val sizeB = with(rule.density) { 200.dp.roundToPx() }

        rule.runOnIdle { assertTrue(containerSize.width == sizeA) }

        // Switch to B, ready. Start animation.
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.autoAdvance = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(50) // Halfway (50ms)

        // Size should be halfway between 100 and 200
        val midSize = (sizeA + sizeB) / 2
        rule.runOnIdle {
            // Check approximate size
            assertTrue(containerSize.width > sizeA && containerSize.width < sizeB)
        }

        val sizeAtFlap = containerSize

        // Flap: Not Ready!
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle() // Propagate state

        // Advance time - Should NOT pause because we already started
        rule.mainClock.advanceTimeBy(25)

        // If it paused, size would equal sizeAtFlap.
        // If it continued, size should be larger.
        rule.runOnIdle {
            assertTrue(
                "Animation should continue despite flapping unready",
                containerSize.width > sizeAtFlap.width,
            )
        }

        // Finish
        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle { assertTrue(containerSize.width == sizeB) }
    }

    @Test
    fun verifyCurrentStateRemainsDuringPreparation() {
        val state = DeferredTransitionState("A")
        lateinit var transition: Transition<String>

        rule.setContent {
            transition = rememberTransition(state)
            transition.DeferredAnimatedContent { target -> Box(Modifier.size(100.dp)) }
        }

        // Initial state
        rule.waitForIdle()
        assertTrue(transition.currentState == "A")
        assertTrue(transition.targetState == "A")

        // Switch to B, but not ready
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        // Verify we are "Held" at A
        // Target is B, but Current should still be A because preparation is not ready
        assertTrue("Target should still be A", transition.targetState == "A")
        assertTrue("Current should remain A during preparation", transition.currentState == "A")
        assertTrue("Pending Target should be B", state.pendingTargetState == "B")

        // Make ready
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Should settle
        assertTrue(transition.currentState == "B")
    }

    @Test
    fun doubleChangeTargetWhileNotReady() {
        val state = DeferredTransitionState(0)

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(100, easing = LinearEasing)) togetherWith
                        fadeOut(tween(100, easing = LinearEasing))
                }
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        // Initial state: content_0 is displayed.
        rule.onNodeWithTag("content_0").assertIsDisplayed()

        // Change target to 1, but content is not ready.
        rule.runOnIdle { state.defer(1) }
        rule.waitForIdle()

        // Content_0 should remain visible as the transition to 1 is held.
        rule.onNodeWithTag("content_0").assertIsDisplayed()
        // Content_1 should be composed but not yet displayed.
        rule.onNodeWithTag("content_1").assertExists()

        // Change target to 2, while content is still not ready.
        rule.runOnIdle { state.defer(2) }
        rule.waitForIdle()

        // Content_0 should still be displayed, as the transition to 2 is also held from 0.
        rule.onNodeWithTag("content_0").assertIsDisplayed()
        // Content_2 should be composed but not yet displayed.
        rule.onNodeWithTag("content_2").assertExists()
        // Content_1 should no longer be in the composition, as it was never truly "entered"
        // and has been superseded by content_2 as the new target.
        rule.onNodeWithTag("content_1").assertDoesNotExist()
    }

    @Test
    fun interruptionRespectedOnlyWhenReady() {
        val state = DeferredTransitionState("A")
        var size by mutableStateOf(IntSize.Zero)

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    (fadeIn(tween(100, easing = LinearEasing)) +
                        expandIn(tween(100, easing = LinearEasing))) togetherWith
                        (fadeOut(tween(100, easing = LinearEasing)) +
                            shrinkOut(tween(100, easing = LinearEasing)))
                },
                modifier = Modifier.onGloballyPositioned { size = it.size },
            ) { target ->
                // A: 100, B: 200, C: 100
                val sizeDp = if (target == "B") 200.dp else 100.dp
                Box(Modifier.size(sizeDp).testTag("content_$target"))
            }
        }

        // Initial A (100dp)
        rule.waitForIdle()
        val sizeA = with(rule.density) { 100.dp.roundToPx() }
        val sizeB = with(rule.density) { 200.dp.roundToPx() }

        // Start A -> B
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.autoAdvance = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(50) // Halfway

        // Size should be > sizeA
        assertTrue(size.width > sizeA)

        // Interrupt with C (100dp), but C is NOT ready.
        // Should continue towards B (200dp).
        rule.runOnIdle { state.defer("C") }

        // Trigger frame
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        val sizeAfterInterruption = size.width
        rule.mainClock.advanceTimeBy(20)

        // It should continue to expand (towards B), so size should increase
        assertTrue(
            "Size should increase (continue to B) but was $size.width vs $sizeAfterInterruption",
            size.width > sizeAfterInterruption,
        )

        // Make C ready. Now it should interrupt and start C (100dp).
        rule.runOnIdle { state.animateTo("C") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val sizeBeforeC = size.width
        rule.mainClock.advanceTimeBy(20)

        // Now it should shrink towards C
        assertTrue(
            "Size should decrease (towards C) but was $size.width vs $sizeBeforeC",
            size.width < sizeBeforeC,
        )

        // Just verify we eventually get to C
        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("content_C").assertIsDisplayed()
    }

    @Test
    fun interruptionRespectedOnlyWhenReady_verifyComposition() {
        val state = DeferredTransitionState("A")

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    (fadeIn(tween(100, easing = LinearEasing)) +
                        expandIn(tween(100, easing = LinearEasing))) togetherWith
                        (fadeOut(tween(100, easing = LinearEasing)) +
                            shrinkOut(tween(100, easing = LinearEasing)))
                }
            ) { target ->
                // A: 100, B: 200, C: 100
                val sizeDp = if (target == "B") 200.dp else 100.dp
                Box(Modifier.size(sizeDp).testTag("content_$target"))
            }
        }

        // Initial A
        rule.waitForIdle()
        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Start A -> B
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.autoAdvance = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(50) // Halfway

        // Verify A and B are present
        rule.onNodeWithTag("content_A").assertExists()
        rule.onNodeWithTag("content_B").assertExists()
        rule.onNodeWithTag("content_C").assertDoesNotExist()

        // Interrupt with C, C not ready
        rule.runOnIdle { state.defer("C") }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // KEY CHECK: C must enter composition immediately to prepare
        rule.onNodeWithTag("content_C").assertExists()

        // A and B should still exist as we are mid-transition and C is holding
        rule.onNodeWithTag("content_A").assertExists()
        rule.onNodeWithTag("content_B").assertExists()

        // Make C ready
        rule.runOnIdle { state.animateTo("C") }
        rule.mainClock.advanceTimeByFrame()

        // Finish
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithTag("content_C").assertIsDisplayed()
        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertDoesNotExist()
    }

    @Test
    fun predictiveBackMutableTransformDataTest() {
        val state = DeferredTransitionState("A")
        var enterFullSize = IntSize.Zero
        var exitFullSize = IntSize.Zero

        rule.setContent {
            val transition = rememberTransition(state)
            val mutableTransform = remember {
                MutableContentTransform {
                    targetContentTransform { fullSize ->
                        if (state.pendingTargetState != null && state.pendingTargetState != "A") {
                            enterFullSize = fullSize
                            offset = IntOffset.Zero
                        }
                    }
                    initialContentTransform { fullSize ->
                        if (state.pendingTargetState != null && state.pendingTargetState != "A") {
                            exitFullSize = fullSize
                            offset = IntOffset.Zero
                        }
                    }
                }
            }
            transition.DeferredAnimatedContent(mutableTransformSpec = { mutableTransform }) { target
                ->
                val size = if (target == "A") 100.dp else 200.dp
                Box(Modifier.size(size).testTag("content_$target"))
            }
        }

        rule.onNodeWithTag("content_A").assertIsDisplayed()

        // Switch to B, it will hold because we use defer
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        // Check if fullSize was captured in preparationData
        assertTrue(
            "Enter fullSize should be captured. Actually: $enterFullSize",
            enterFullSize.width > 0,
        )
        assertTrue(
            "Exit fullSize should be captured. Actually: $exitFullSize",
            exitFullSize.width > 0,
        )
    }

    @Test
    fun animatedContent_previewTransforms_handoffCorrectly() {
        val state = DeferredTransitionState("A")
        var previewing by mutableStateOf(false)
        var previewOffset by mutableStateOf(0)

        var enterX = 0
        var enterY = 0
        var exitX = 0
        var exitY = 0

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    slideInHorizontally(tween(160)) { 200 } togetherWith
                        slideOutHorizontally(tween(160)) { -200 }
                },
                mutableTransformSpec = {
                    if (previewing && targetState != "A") {
                        MutableContentTransform {
                            targetContentTransform { offset = IntOffset(previewOffset, 100) }
                            initialContentTransform { offset = IntOffset(-previewOffset, 100) }
                        }
                    } else {
                        null
                    }
                },
            ) { target ->
                Box(
                    Modifier.size(100.dp).testTag("content_$target").onGloballyPositioned { coords
                        ->
                        if (target == "A") {
                            exitX = coords.positionInRoot().x.toInt()
                            exitY = coords.positionInRoot().y.toInt()
                        } else if (target == "B") {
                            enterX = coords.positionInRoot().x.toInt()
                            enterY = coords.positionInRoot().y.toInt()
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertEquals(0, exitX)

        rule.mainClock.autoAdvance = false

        rule.runOnIdle {
            previewing = true
            state.defer("B")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle { previewOffset = 50 }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // slideIn starts at 200, preview adds 50 -> 250
        assertEquals(250, enterX)
        // slideOut starts at 0, preview adds -50 -> -50
        assertEquals(-50, exitX)

        assertEquals(100, enterY)
        assertEquals(100, exitY)

        rule.runOnIdle {
            previewing = false
            state.animateTo("B")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(80L)
        rule.mainClock.advanceTimeByFrame()

        // Enter is animating from 250 -> 0
        assertTrue(
            "Enter x offset should be between 0 and 250. Actually $enterX",
            enterX in 1..<250,
        )
        // Exit is animating from -50 -> -200
        assertTrue(
            "Exit x offset should be between -50 and -200. Actually $exitX",
            exitX < -50 && exitX > -200,
        )
        assertTrue("Enter Y offset should be between 0 and 100. Actually $exitX", enterY in 1..<100)
        assertTrue("Exit Y offset should be between 0 and 100. Actually $exitX", exitY in 1..<100)

        rule.mainClock.advanceTimeBy(1000L)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(0, enterX)
        rule.onNodeWithTag("content_A").assertDoesNotExist()
        rule.onNodeWithTag("content_B").assertIsDisplayed()
    }

    @Test
    fun previewScope_providesInitialAndTargetState() {
        val state = DeferredTransitionState("A")
        var capturedInitialState: String? = null
        var capturedTargetState: String? = null

        rule.setContent {
            val transition = rememberTransition(state)
            val mutableTransform = remember { MutableContentTransform() }
            if (state.pendingTargetState != null) {
                capturedInitialState = transition.targetState
                capturedTargetState = transition.pendingTargetState ?: state.pendingTargetState
            }
            transition.DeferredAnimatedContent(mutableTransformSpec = { mutableTransform }) { target
                ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        rule.waitForIdle()

        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        assertEquals("A", capturedInitialState)
        assertEquals("B", capturedTargetState)
    }

    @Test
    fun previewScope_providesCorrectStates_onInterruption() {
        val state = DeferredTransitionState("A")
        var capturedInitialState: String? = null
        var capturedTargetState: String? = null

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(100, easing = LinearEasing)) togetherWith
                        fadeOut(tween(100, easing = LinearEasing))
                },
                mutableTransformSpec = {
                    capturedInitialState = initialState
                    capturedTargetState = targetState
                    null
                },
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // Start transition to B
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()

        assertEquals(null, capturedInitialState)
        assertEquals(null, capturedTargetState)

        // Interrupt the transition to B and go back to A
        rule.runOnIdle { state.defer("A") }
        rule.mainClock.advanceTimeByFrame()

        assertEquals("B", capturedInitialState)
        assertEquals("A", capturedTargetState)
    }

    @Test
    fun previewScope_providesCorrectStates_onInterruptionDuringPreviewPhase() {
        val state = DeferredTransitionState("A")
        var capturedInitialState: String? = null
        var capturedTargetState: String? = null
        var previewInvocationCount: Int = 0

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    fadeIn(tween(100, easing = LinearEasing)) togetherWith
                        fadeOut(tween(100, easing = LinearEasing))
                },
                mutableTransformSpec = {
                    capturedInitialState = initialState
                    capturedTargetState = targetState
                    previewInvocationCount += 1
                    null
                },
            ) { target ->
                Box(Modifier.size(100.dp).testTag("content_$target"))
            }
        }

        rule.waitForIdle()

        // Start transition to B, but wait
        rule.runOnIdle { state.defer("B") }
        rule.waitForIdle()

        assertEquals("A", capturedInitialState)
        assertEquals("B", capturedTargetState)

        val invocationCountAfterFirstNavigation = previewInvocationCount

        // Interrupt the transition to B and go back to A
        rule.runOnIdle { state.defer("A") }
        rule.waitForIdle()
        // when interrupting during the preview phase, preview lambda is not called again
        assertEquals("A", capturedInitialState)
        assertEquals("B", capturedTargetState)
        assertEquals(invocationCountAfterFirstNavigation, previewInvocationCount)
    }

    @Test
    fun animatedContent_previewTransforms_handoffVelocity() {
        testTimeSource = { rule.mainClock.currentTime }

        val state = DeferredTransitionState("A")
        var previewScale by mutableStateOf(1f)

        var enterWidth = 0f
        var exitWidth = 0f

        rule.setContent {
            val transition = rememberTransition(state)
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    scaleIn(
                        spring(stiffness = Spring.StiffnessVeryLow),
                        initialScale = 0.2f,
                    ) togetherWith
                        scaleOut(spring(stiffness = Spring.StiffnessVeryLow), targetScale = 0.2f)
                },
                mutableTransformSpec = {
                    if (targetState != "A") {
                        MutableContentTransform {
                            targetContentTransform { scale = previewScale }
                            initialContentTransform { scale = previewScale }
                        }
                    } else {
                        null
                    }
                },
            ) { target ->
                Box(
                    Modifier.size(100.dp).testTag("content_$target").onGloballyPositioned { coords
                        ->
                        if (target == "A") {
                            exitWidth = coords.boundsInRoot().width
                        } else if (target == "B") {
                            enterWidth = coords.boundsInRoot().width
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        fun simulateGesture(isFast: Boolean): Pair<Float, Float> {
            rule.runOnIdle { state.defer("B") }
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

            val lastGestureEnterWidth = enterWidth
            val lastGestureExitWidth = exitWidth

            rule.runOnIdle { state.animateTo("B") }
            repeat(3) { rule.mainClock.advanceTimeByFrame() }
            rule.waitForIdle()

            return (lastGestureEnterWidth - enterWidth) to (lastGestureExitWidth - exitWidth)
        }

        // Scenario 1: Slow gesture
        val (enterDropSlow, exitDropSlow) = simulateGesture(isFast = false)

        // Reset
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { state.animateTo("A") }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        previewScale = 1f

        // Scenario 2: Fast gesture
        val (enterDropFast, exitDropFast) = simulateGesture(isFast = true)

        assertTrue(
            "Expected enterWidth drop with fast gesture ($enterDropFast) to be greater than with slow gesture ($enterDropSlow)",
            enterDropFast > enterDropSlow,
        )
        assertTrue(
            "Expected exitWidth drop with fast gesture ($exitDropFast) to be greater than with slow gesture ($exitDropSlow)",
            exitDropFast > exitDropSlow,
        )

        testTimeSource = null
    }
}
