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

package androidx.compose.animation.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@OptIn(ExperimentalTransitionApi::class, ExperimentalDeferredTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeferredTransitionTest {
    private val rule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    private enum class TestStates {
        A,
        B,
        C,
    }

    @Test
    fun deferredTransition_initialStateSetImmediately_updatesOnlyWhenReady() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
        }

        // 1. Verify Initial state (Initial state cannot be deferred)
        rule.runOnIdle {
            assertEquals(TestStates.A, transition.targetState)
            assertNull(transition.pendingTargetState)
        }

        // 2. Update target while content is NOT ready
        rule.runOnIdle { state.defer(TestStates.B) }

        rule.runOnIdle {
            assertEquals(TestStates.A, transition.currentState)
            assertEquals(TestStates.A, transition.targetState)
            assertEquals(TestStates.B, transition.pendingTargetState)
        }

        // 3. Make content ready and verify update
        rule.runOnIdle { state.animateTo(TestStates.B) }

        rule.runOnIdle {
            assertEquals(TestStates.B, transition.targetState)
            assertNull(transition.pendingTargetState)
        }
    }

    @Test
    fun deferredTransition_pendingState_isOverriddenByNewUpdates() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
        }

        // Update target to B (Deferred)
        rule.runOnIdle { state.defer(TestStates.B) }
        rule.runOnIdle { assertEquals(TestStates.B, transition.pendingTargetState) }

        // Update target to C (Should override pending B)
        rule.runOnIdle { state.defer(TestStates.C) }

        rule.runOnIdle {
            assertEquals(TestStates.A, transition.currentState)
            assertEquals(TestStates.A, transition.targetState)
            assertEquals(TestStates.C, transition.pendingTargetState)
        }
    }

    @Test
    fun deferredTransition_interruption_continuesCurrentAnimationUntilReady() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>
        var animatedValue by mutableIntStateOf(-1)

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
            animatedValue =
                transition
                    .animateInt(
                        label = "val",
                        transitionSpec = { tween(1000, easing = LinearEasing) },
                    ) { s ->
                        if (s == TestStates.A) 0 else 1000
                    }
                    .value
        }

        rule.mainClock.autoAdvance = false
        rule.waitForIdle()

        // 1. Start animating A -> B
        rule.runOnIdle { state.animateTo(TestStates.B) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(500) // Advance to midpoint
        rule.waitForIdle()
        assertEquals(500f, animatedValue.toFloat(), 20f)

        // 2. Interrupt back to A, but defer it
        rule.runOnIdle { state.defer(TestStates.A) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Verify animation still targets B because A isn't ready
        assertEquals(TestStates.A, transition.pendingTargetState)
        assertEquals(TestStates.B, transition.targetState)

        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()
        assertEquals(700f, animatedValue.toFloat(), 20f) // Continues toward 1000

        // 3. Release A
        rule.runOnIdle { state.animateTo(TestStates.A) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // 4. Verify redirection toward A
        assertEquals(TestStates.A, transition.targetState)
        rule.mainClock.advanceTimeBy(2000)
        rule.waitForIdle()
        assertEquals(0, animatedValue)
    }

    @Test
    fun deferredTransition_childTransition_defersSimultaneouslyWithParent() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>
        lateinit var childTransition: Transition<Boolean>

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
            childTransition = transition.createChildTransition { it == TestStates.B }
        }

        // Defer target change
        rule.runOnIdle { state.defer(TestStates.B) }

        rule.runOnIdle {
            assertEquals(TestStates.B, transition.pendingTargetState)
            assertEquals(TestStates.A, transition.targetState)
            assertFalse("Child should not have updated yet", childTransition.targetState)
        }

        // Release
        rule.runOnIdle { state.animateTo(TestStates.B) }

        rule.runOnIdle {
            assertEquals(TestStates.B, transition.targetState)
            assertTrue("Child should update to B (true)", childTransition.targetState)
        }
    }

    @Test
    fun deferredTransition_animateFloat_respectsTransitionSpecAfterRelease() {
        lateinit var state: DeferredTransitionState<TestStates>
        var value by mutableStateOf(0f)

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            val transition = rememberTransition(state)
            value =
                transition
                    .animateFloat(
                        transitionSpec = {
                            if (TestStates.A isTransitioningTo TestStates.B) {
                                tween(100, easing = LinearEasing)
                            } else {
                                snap()
                            }
                        }
                    ) {
                        if (it == TestStates.B) 1f else 0f
                    }
                    .value
        }

        rule.mainClock.autoAdvance = false
        rule.waitForIdle()

        // Defer change to B
        rule.runOnIdle { state.defer(TestStates.B) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(0f, value)

        // Release and check if tween(100) is used
        rule.runOnIdle { state.animateTo(TestStates.B) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        rule.mainClock.advanceTimeBy(50)
        assertTrue("Value should be mid-animation", value > 0f && value < 1f)

        rule.mainClock.advanceTimeBy(100)
        assertEquals(1f, value)
    }

    @Test
    fun deferredTransition_childTransition_inheritsPendingState() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>
        lateinit var childTransition: Transition<Boolean>

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
            childTransition =
                transition.createChildTransition(label = "ChildTransition") { it == TestStates.B }
        }

        // 1. Initial state (No pending state)
        rule.runOnIdle {
            assertNull(transition.pendingTargetState)
            assertNull(childTransition.pendingTargetState)
        }

        // 2. Set parent to pending state
        rule.runOnIdle { state.defer(TestStates.B) }

        rule.runOnIdle {
            assertEquals(TestStates.B, transition.pendingTargetState)
            // The child's pendingTargetState should now be its mapped targetState (true)
            // since the parent is pending B. child target state should remain false.
            assertEquals(true, childTransition.pendingTargetState)
            assertEquals(false, childTransition.targetState)
        }

        // 4. Release parent
        rule.runOnIdle { state.animateTo(TestStates.B) }

        rule.runOnIdle {
            assertNull(transition.pendingTargetState)
            assertNull(childTransition.pendingTargetState)
            assertTrue(childTransition.targetState)
        }
    }

    @Test
    fun deferredTransition_animateToThirdState_overridesPendingState() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
        }

        // 1. Defer A -> B
        rule.runOnIdle { state.defer(TestStates.B) }
        rule.runOnIdle { assertEquals(TestStates.B, transition.pendingTargetState) }

        // 2. animateTo(C) - bypassing B
        rule.runOnIdle { state.animateTo(TestStates.C) }

        // 3. Verify targetState is C and pendingTargetState is null
        rule.runOnIdle {
            assertEquals(TestStates.C, transition.targetState)
            assertNull(transition.pendingTargetState)
        }
    }

    @Test
    fun deferredTransition_deferNewStateWhileAnimating() {
        lateinit var transition: DeferredTransition<TestStates>
        lateinit var state: DeferredTransitionState<TestStates>
        var animatedValue by mutableIntStateOf(-1)

        rule.setContent {
            state = remember { DeferredTransitionState(TestStates.A) }
            transition = rememberTransition(state)
            animatedValue =
                transition
                    .animateInt(
                        label = "val",
                        transitionSpec = { tween(1000, easing = LinearEasing) },
                    ) { s ->
                        if (s == TestStates.A) 0 else 1000
                    }
                    .value
        }

        rule.mainClock.autoAdvance = false
        rule.waitForIdle()

        // 1. Start animating A -> B
        rule.runOnIdle { state.animateTo(TestStates.B) }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        assertEquals(500f, animatedValue.toFloat(), 20f)

        // 2. Call defer(C) mid-animation
        rule.runOnIdle { state.defer(TestStates.C) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // 3. Verify targetState remains B (animation continues) and pendingTargetState is C
        assertEquals(TestStates.B, transition.targetState)
        assertEquals(TestStates.C, transition.pendingTargetState)

        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()
        assertEquals(600f, animatedValue.toFloat(), 20f) // Still moving toward B
    }
}
