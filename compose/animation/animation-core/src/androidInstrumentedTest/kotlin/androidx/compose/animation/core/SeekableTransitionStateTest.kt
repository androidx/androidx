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
package androidx.compose.animation.core

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class SeekableTransitionStateTest {
    @get:Rule
    val rule = createComposeRule()

    private enum class AnimStates {
        From,
        To
    }

    @Test
    fun seekFraction() {
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)
        var animatedValue by mutableIntStateOf(-1)

        rule.setContent {
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
        }
        rule.runOnIdle {
            assertEquals(0, animatedValue)
            runBlocking {
                seekableTransitionState.snapToFraction(0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.snapToFraction(1f)
                assertEquals(1f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            runBlocking {
                seekableTransitionState.snapToFraction(0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.snapToFraction(0f)
                assertEquals(0f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(0, animatedValue)
        }
    }

    @Test
    fun changeTarget() {
        var animatedValue by mutableIntStateOf(-1)
        var duration by mutableLongStateOf(0)
        var fromState by mutableStateOf(AnimStates.From)
        var toState by mutableStateOf(AnimStates.To)
        lateinit var seekableTransitionState: SeekableTransitionState<AnimStates>

        rule.setContent {
            seekableTransitionState = remember(fromState, toState) {
                SeekableTransitionState(fromState, toState)
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(label = "Value") { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
            duration = transition.totalDurationNanos
        }

        rule.runOnIdle {
            assertEquals(0, animatedValue)
            fromState = AnimStates.To
            toState = AnimStates.From
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            runBlocking {
                seekableTransitionState.snapToFraction(0.5f)
            }
        }

        rule.runOnIdle {
            assertTrue(animatedValue > 0)
            assertTrue(animatedValue < 1000)
            fromState = AnimStates.From
            toState = AnimStates.To
        }
        rule.runOnIdle {
            assertEquals(0, animatedValue)
        }
    }

    @Test
    fun animateToTarget() {
        var animatedValue by mutableIntStateOf(-1)
        var duration by mutableLongStateOf(0)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)
        lateinit var coroutineContext: CoroutineContext
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            coroutineContext = coroutineScope.coroutineContext
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
            duration = transition.totalDurationNanos
        }

        val deferred1 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToTargetState()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        var progressFraction = 0f
        rule.runOnIdle {
            assertTrue(seekableTransitionState.fraction > 0f)
            progressFraction = seekableTransitionState.fraction
        }

        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(seekableTransitionState.fraction > progressFraction)
            progressFraction = seekableTransitionState.fraction
        }

        // interrupt the progress

        runBlocking {
            seekableTransitionState.snapToFraction(0.5f)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertTrue(deferred1.isCancelled)
            // We've stopped animating after seeking
            assertEquals(0.5f, seekableTransitionState.fraction)
            assertEquals(500, animatedValue)
        }

        // continue from the same place
        val deferred2 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToTargetState()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            // We've stopped animating after seeking
            assertTrue(seekableTransitionState.fraction > 0.5f)
            assertTrue(seekableTransitionState.fraction < 1f)
        }

        rule.mainClock.advanceTimeBy(duration / 1000_000L)

        rule.runOnIdle {
            assertTrue(deferred2.isCompleted)
            assertEquals(1f, seekableTransitionState.fraction, 0f)
            assertEquals(1000, animatedValue)
        }
    }

    @Test
    fun animateToCurrent() {
        var animatedValue by mutableIntStateOf(-1)
        var duration by mutableLongStateOf(0)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)
        lateinit var coroutineContext: CoroutineContext
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            coroutineContext = coroutineScope.coroutineContext
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
            duration = transition.totalDurationNanos
        }

        runBlocking {
            // Go to the end
            seekableTransitionState.snapToFraction(1f)
        }

        val deferred1 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToCurrentState()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        var progressFraction = 0f
        rule.runOnIdle {
            assertTrue(seekableTransitionState.fraction < 1f)
            progressFraction = seekableTransitionState.fraction
        }

        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(seekableTransitionState.fraction < progressFraction)
            progressFraction = seekableTransitionState.fraction
        }

        // interrupt the progress
        runBlocking {
            seekableTransitionState.snapToFraction(0.5f)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertTrue(deferred1.isCancelled)
            // We've stopped animating after seeking
            assertEquals(0.5f, seekableTransitionState.fraction)
            assertEquals(500, animatedValue)
        }

        // continue from the same place
        val deferred2 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToCurrentState()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            // We've stopped animating after seeking
            assertTrue(seekableTransitionState.fraction < 0.5f)
            assertTrue(seekableTransitionState.fraction > 0f)
        }

        rule.mainClock.advanceTimeBy(duration / 1000_000L)

        rule.runOnIdle {
            assertTrue(deferred2.isCompleted)
            assertEquals(0f, seekableTransitionState.fraction, 0f)
            assertEquals(0, animatedValue)
        }
    }

    @Test
    fun updatedTransition() {
        var animatedValue by mutableIntStateOf(-1)
        var duration = -1L
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)

        rule.setContent {
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 200, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
            transition.AnimatedContent(transitionSpec = {
                fadeIn(tween(durationMillis = 1000, easing = LinearEasing)) togetherWith
                    fadeOut(tween(durationMillis = 1000, easing = LinearEasing))
            }) { state ->
                if (state == AnimStates.To) {
                    Box(Modifier.size(100.dp))
                }
            }
            duration = transition.totalDurationNanos
        }

        rule.runOnIdle {
            assertEquals(1000_000_000L, duration)
            assertEquals(0f, seekableTransitionState.fraction, 0f)
        }

        runBlocking {
            // Go to the middle
            seekableTransitionState.snapToFraction(0.5f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(0.5f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go to the end
            seekableTransitionState.snapToFraction(1f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(1f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go back to part way through the animatedValue
            seekableTransitionState.snapToFraction(0.1f)
        }

        rule.runOnIdle {
            assertEquals(500, animatedValue)
            assertEquals(0.1f, seekableTransitionState.fraction)
        }
    }

    @Test
    fun repeatAnimate() {
        var animatedValue by mutableIntStateOf(-1)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)
        lateinit var coroutineContext: CoroutineContext
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            coroutineContext = coroutineScope.coroutineContext
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
        }

        val deferred1 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToTargetState()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        // Running the same animation again should cancel the existing one
        val deferred2 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToTargetState()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred1.isCancelled)
        assertFalse(deferred2.isCancelled)

        // Changing the direction should cancel also
        val deferred3 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToCurrentState()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred2.isCancelled)
        assertFalse(deferred3.isCancelled)

        // Change direction the other way should cancel also
        val deferred4 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateToTargetState()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred3.isCancelled)
        assertFalse(deferred4.isCancelled)
    }

    @Test
    fun segmentInitialized() {
        var animatedValue by mutableIntStateOf(-1)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From, AnimStates.To)
        lateinit var segment: Transition.Segment<AnimStates>

        rule.setContent {
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = {
                    if (initialState == targetState) {
                        snap()
                    } else {
                        tween(easing = LinearEasing)
                    }
                }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                }
            }.value
            segment = transition.segment
        }

        rule.runOnIdle {
            assertEquals(AnimStates.From, segment.initialState)
            assertEquals(AnimStates.To, segment.targetState)
        }
    }
}
