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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        To,
        Other,
    }

    @Test
    fun seekFraction() {
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue by mutableIntStateOf(-1)

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }.value
        }
        rule.runOnIdle {
            assertEquals(0, animatedValue)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 1f)
                assertEquals(1f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0f)
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
                SeekableTransitionState(fromState)
            }
            LaunchedEffect(seekableTransitionState, toState) {
                seekableTransitionState.snapTo(toState)
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(label = "Value") { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
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
                seekableTransitionState.snapTo(fraction = 0.5f)
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
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var coroutineContext: CoroutineContext
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
            coroutineScope = rememberCoroutineScope()
            coroutineContext = coroutineScope.coroutineContext
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }.value
            duration = transition.totalDurationNanos
        }

        val deferred1 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateTo()
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
            seekableTransitionState.snapTo(fraction = 0.5f)
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
            seekableTransitionState.animateTo()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            // We've stopped animating after seeking
            assertTrue(seekableTransitionState.fraction > 0.5f)
            assertTrue(seekableTransitionState.fraction < 1f)
        }

        rule.mainClock.advanceTimeBy(5000L)

        rule.runOnIdle {
            assertTrue(deferred2.isCompleted)
            assertEquals(0f, seekableTransitionState.fraction, 0f)
            assertEquals(1000, animatedValue)
        }
    }

    @Test
    fun updatedTransition() {
        var animatedValue by mutableIntStateOf(-1)
        var duration = -1L
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 200, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
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
            seekableTransitionState.snapTo(fraction = 0.5f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(0.5f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go to the end
            seekableTransitionState.snapTo(fraction = 1f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(1f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go back to part way through the animatedValue
            seekableTransitionState.snapTo(fraction = 0.1f)
        }

        rule.runOnIdle {
            assertEquals(500, animatedValue)
            assertEquals(0.1f, seekableTransitionState.fraction)
        }
    }

    @Test
    fun repeatAnimate() {
        var animatedValue by mutableIntStateOf(-1)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var coroutineContext: CoroutineContext
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
            coroutineScope = rememberCoroutineScope()
            coroutineContext = coroutineScope.coroutineContext
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            animatedValue = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }.value
        }

        val deferred1 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateTo()
        }
        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        // Running the same animation again should cancel the existing one
        val deferred2 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateTo()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred1.isCancelled)
        assertFalse(deferred2.isCancelled)

        // seeking should cancel the animation
        val deferred3 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.snapTo(fraction = 0.25f)
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred2.isCancelled)
        assertFalse(deferred3.isCancelled)
        assertTrue(deferred3.isCompleted)

        // start the animation again
        val deferred4 = coroutineScope.async(coroutineContext) {
            seekableTransitionState.animateTo()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertFalse(deferred4.isCancelled)
    }

    @Test
    fun segmentInitialized() {
        var animatedValue by mutableIntStateOf(-1)
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var segment: Transition.Segment<AnimStates>

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
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
                    else -> 1000
                }
            }.value
            segment = transition.segment
        }

        rule.runOnIdle {
            assertEquals(AnimStates.From, segment.initialState)
            assertEquals(AnimStates.To, segment.targetState)
        }
    }

    // In the middle of seeking from From to To, seek to Other
    @Test
    fun seekThirdState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        var animatedValue3 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.snapTo(AnimStates.To)
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.Other -> 1000
                    else -> 0
                }
            }
            val val3 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                        animatedValue3 = val3.value
                    })
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // Check initial values
            assertEquals(0, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(0, animatedValue3)
            // Seek half way
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // Check half way values
            assertEquals(500, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(500, animatedValue3)
        }
        // Start seek to new state. It won't complete until the initial state is
        // animated to "To"
        val seekTo = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.Other)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // First frame, nothing has changed. We've only gathered the first frame of the
            // animation since it was not previously animating
            assertEquals(500, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(500, animatedValue3)
        }

        // Continue the initial value animation. It should use a spring spec to animate the
        // value since no animation spec was previously used
        rule.mainClock.advanceTimeBy(75L)
        rule.runOnIdle {
            assertTrue(animatedValue1 > 500)
            assertTrue(animatedValue1 < 1000)
            assertEquals(0, animatedValue2)
            assertTrue(animatedValue3 > 500)
            assertTrue(animatedValue3 < 1000)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(500, animatedValue2)
        }

        // Advance to the end of the seekTo() animation
        rule.mainClock.advanceTimeBy(5_000)
        runBlocking { seekTo.await() }
        rule.runOnIdle {
            // The initial values should be 1000/0/1000
            // Target values should be 1000, 1000, 2000
            // The seek is 0.5
            assertEquals(1000, animatedValue1)
            assertEquals(500, animatedValue2)
            assertEquals(1500, animatedValue3)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 1f)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // Should be at the target values now
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
            assertEquals(2000, animatedValue3)
        }
    }

    // In the middle of animating from From to To, seek to Other
    @Test
    fun interruptAnimationWithSeekThirdState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        var animatedValue3 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.animateTo(AnimStates.To, tween(easing = LinearEasing))
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.Other -> 1000
                    else -> 0
                }
            }
            val val3 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                        animatedValue3 = val3.value
                    })
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation start time
        rule.runOnIdle {
            assertEquals(0f, seekableTransitionState.fraction, 0.01f)
            // Check initial values
            assertEquals(0, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(0, animatedValue3)
        }
        // Advance around half way through the animation
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            // should be 160/300 = 0.5333f
            assertEquals(0.53f, seekableTransitionState.fraction, 0.01f)

            // Check values at that fraction
            assertEquals(533f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(533f, animatedValue3.toFloat(), 1f)
        }

        val seekTo = coroutineScope.async {
            // seek to Other. This won't finish until the animation finishes
            seekableTransitionState.snapTo(AnimStates.Other)
        }

        rule.runOnIdle {
            // Nothing will have changed yet. The initial value should continue to animate
            // after this
            assertEquals(533f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(533f, animatedValue3.toFloat(), 1f)
        }

        // Advance time by two more frames
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle {
            // should be 192/300 = 0.64 through animation
            assertEquals(640f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(640f, animatedValue3.toFloat(), 1f)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue2)
        }

        // Advance to the end of the seekTo() animation
        rule.mainClock.advanceTimeBy(5_000)
        assertTrue(seekTo.isCompleted)
        rule.runOnIdle {
            // The initial values should be 1000/0/1000
            // Target values should be 1000, 1000, 2000
            // The seek is 0.5
            assertEquals(1000, animatedValue1)
            assertEquals(500, animatedValue2)
            assertEquals(1500, animatedValue3)
            runBlocking {
                seekableTransitionState.snapTo(fraction = 1f)
                assertEquals(1f, seekableTransitionState.fraction, 0f)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // Should be at the target values now
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
            assertEquals(2000, animatedValue3)
        }
    }

    // In the middle of animating from From to To, seek to Other
    @Test
    fun interruptAnimationWithAnimateToThirdState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        var animatedValue3 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.animateTo(AnimStates.To, tween(easing = LinearEasing))
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.Other -> 1000
                    else -> 0
                }
            }
            val val3 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                        animatedValue3 = val3.value
                    })
        }
        rule.waitForIdle() // let the launched effect start
        rule.mainClock.advanceTimeByFrame() // lock in the animation start time
        rule.runOnIdle {
            assertEquals(0f, seekableTransitionState.fraction, 0.01f)
            // Check initial values
            assertEquals(0, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(0, animatedValue3)
        }
        // Advance around half way through the animation
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            // should be 160/300 = 0.5333f
            assertEquals(0.53f, seekableTransitionState.fraction, 0.01f)

            // Check values at that fraction
            assertEquals(533f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(533f, animatedValue3.toFloat(), 1f)
        }
        val animateToOther = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.Other, tween(easing = LinearEasing))
        }

        rule.runOnIdle {
            // Nothing will have changed yet. The initial value should continue to animate
            // after this
            assertEquals(533f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(533f, animatedValue3.toFloat(), 1f)
        }

        // Lock in the animation for the animation to Other, but advance animation to To
        rule.mainClock.advanceTimeBy(16)
        rule.runOnIdle {
            // initial should be 176/300 = 0.587 through animation
            assertEquals(586.7f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(586.7f, animatedValue3.toFloat(), 1f)
        }

        // Advance time by two more frames
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle {
            // initial should be 208/300 = 0.693 through animation
            // other should be 32/300 = 0.107 through the animation
            assertEquals(693f, animatedValue1.toFloat(), 1f)
            assertEquals(107f, animatedValue2.toFloat(), 1f)
            assertEquals(693f + ((2000f - 693f) * 0.107f), animatedValue3.toFloat(), 2f)
        }

        // Advance to the end of the animation
        rule.mainClock.advanceTimeBy(5_000)
        assertTrue(animateToOther.isCompleted)
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
            assertEquals(2000, animatedValue3)
        }
    }

    // In the middle of animating from From to To, seek to Other
    @Test
    fun cancelAnimationWithAnimateToThirdStateW() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        var animatedValue3 by mutableIntStateOf(-1)
        var targetState by mutableStateOf(AnimStates.To)

        rule.setContent {
            LaunchedEffect(seekableTransitionState, targetState) {
                seekableTransitionState.animateTo(targetState, tween(easing = LinearEasing))
            }
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.Other -> 1000
                    else -> 0
                }
            }
            val val3 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                        animatedValue3 = val3.value
                    })
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation start time
        rule.runOnIdle {
            assertEquals(0f, seekableTransitionState.fraction, 0.01f)
            // Check initial values
            assertEquals(0, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(0, animatedValue3)
        }
        // Advance around half way through the animation
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            // should be 160/300 = 0.5333f
            assertEquals(0.53f, seekableTransitionState.fraction, 0.01f)

            // Check values at that fraction
            assertEquals(533f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(533f, animatedValue3.toFloat(), 1f)
            targetState = AnimStates.Other
        }

        // Advance the clock so that the LaunchedEffect can run
        rule.mainClock.advanceTimeBy(16)

        rule.runOnIdle {
            assertEquals(AnimStates.Other, seekableTransitionState.targetState)

            // The time is advanced first, so the values are updated, then the
            // LaunchedEffect cancels the animation
            assertEquals(586, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(586, animatedValue3)
        }

        // Lock in the animation for initial animation and target animation
        rule.mainClock.advanceTimeBy(16)

        rule.runOnIdle {
            // Nothing will have changed yet, but both animations will start on the next frame
            assertEquals(586, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(586, animatedValue3)
        }

        var previousAnimatedValue1 = 586

        // Advance one frame
        rule.mainClock.advanceTimeBy(16)
        rule.runOnIdle {
            // initial value should be animated by a spring at this point
            // target animation should be 16/300 = 0.05333
            assertTrue(animatedValue1 > previousAnimatedValue1)
            assertEquals(53, animatedValue2)
            assertEquals(
                animatedValue1 + (0.053333f * (2000f - animatedValue1)),
                animatedValue3.toFloat(),
                2f
            )
            previousAnimatedValue1 = animatedValue1
        }

        // Advance time by two more frames
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle {
            // initial should continue to be animated by a spring
            // other should be 48/300 = 0.160 through the animation
            assertTrue(animatedValue1 > previousAnimatedValue1)
            assertEquals(160, animatedValue2)
            assertEquals(
                animatedValue1 + ((2000f - animatedValue1) * 0.16f),
                animatedValue3.toFloat(),
                2f
            )
        }

        // Advance to the end of the animation
        rule.mainClock.advanceTimeBy(5_000)
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
            assertEquals(2000, animatedValue3)
        }
    }

    @Test
    fun interruptInterruption() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        var animatedValue3 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.Other -> 1000
                    else -> 0
                }
            }
            val val3 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                        animatedValue3 = val3.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.snapTo(AnimStates.To) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.snapTo(fraction = 0.5f) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.snapTo(AnimStates.Other) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // lock in the initial value animation start time
        coroutineScope.launch { seekableTransitionState.snapTo(fraction = 0.5f) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.snapTo(AnimStates.From) }
        rule.waitForIdle()

        // Now we have two initial value animations running. One is for animating
        // from From -> To, one from To -> Other
        // The From -> To animation should affect animatedValue1 and animatedValue2
        // The To -> Other animation should affect animatedValue3

        // Holding the value here, the animations should move the values to 1000, 1000, 2000
        rule.mainClock.advanceTimeBy(5_000L)
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
            assertEquals(2000, animatedValue3)
        }
    }

    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun delayedTransition() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var coroutineScope: CoroutineScope
        lateinit var transition: Transition<AnimStates>

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            transition = rememberTransition(seekableTransitionState, label = "Test")
            transition.AnimatedVisibility(
                visible = { it != AnimStates.To },
                enter = fadeIn(tween(300, 0, LinearEasing)),
                exit = fadeOut(tween(300, 0, LinearEasing))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(Color.Red) }
                )
            }
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.snapTo(AnimStates.To, 0.5f)
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // must wait for a composition and draw
        rule.runOnIdle {
            assertEquals(150L * MillisToNanos, transition.playTimeNanos)
        }
    }

    @Test
    fun seekAfterAnimating() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val deferred = coroutineScope.async { seekableTransitionState.animateTo(AnimStates.To) }
        rule.mainClock.advanceTimeBy(10_000L) // complete the animation
        rule.waitForIdle()
        assertTrue(deferred.isCompleted)
        assertEquals(1000, animatedValue1)

        // seeking after the animation has completed should not change any value
        coroutineScope.launch { seekableTransitionState.snapTo(fraction = 0.5f) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1000, animatedValue1)
    }
}
