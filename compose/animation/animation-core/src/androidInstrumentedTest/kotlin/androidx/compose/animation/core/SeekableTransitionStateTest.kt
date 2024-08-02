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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
                seekableTransitionState.seekTo(0f, targetState = AnimStates.To)
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
                seekableTransitionState.seekTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.seekTo(fraction = 1f)
                assertEquals(1f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            runBlocking {
                seekableTransitionState.seekTo(fraction = 0.5f)
                assertEquals(0.5f, seekableTransitionState.fraction)
            }
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue)
            runBlocking {
                seekableTransitionState.seekTo(fraction = 0f)
                assertEquals(0f, seekableTransitionState.fraction)
            }
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
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.seekTo(0f, targetState = AnimStates.To)
            }
            coroutineScope = rememberCoroutineScope()
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

        rule.mainClock.advanceTimeByFrame() // wait for composition after seekTo()
        val deferred1 = coroutineScope.async {
            seekableTransitionState.animateTo()
        }
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
            seekableTransitionState.seekTo(fraction = 0.5f)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertTrue(deferred1.isCancelled)
            // We've stopped animating after seeking
            assertEquals(0.5f, seekableTransitionState.fraction)
            assertEquals(500, animatedValue)
        }

        // continue from the same place
        val deferred2 = coroutineScope.async {
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
                seekableTransitionState.seekTo(0f, targetState = AnimStates.To)
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
            seekableTransitionState.seekTo(fraction = 0.5f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(0.5f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go to the end
            seekableTransitionState.seekTo(fraction = 1f)
        }

        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertEquals(1f, seekableTransitionState.fraction)
        }

        runBlocking {
            // Go back to part way through the animatedValue
            seekableTransitionState.seekTo(fraction = 0.1f)
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
        lateinit var coroutineScope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
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

        val deferred1 = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.mainClock.advanceTimeByFrame()

        // Running the same animation again should cancel the existing one
        val deferred2 = coroutineScope.async {
            seekableTransitionState.animateTo()
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred1.isCancelled)
        assertFalse(deferred2.isCancelled)

        // seeking should cancel the animation
        val deferred3 = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0.25f)
        }

        rule.waitForIdle() // wait for coroutine to run
        rule.mainClock.advanceTimeByFrame()

        assertTrue(deferred2.isCancelled)
        assertFalse(deferred3.isCancelled)
        assertTrue(deferred3.isCompleted)

        // start the animation again
        val deferred4 = coroutineScope.async {
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
                seekableTransitionState.seekTo(0f, targetState = AnimStates.To)
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
                seekableTransitionState.seekTo(0f, targetState = AnimStates.To)
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
        rule.mainClock.advanceTimeByFrame() // let seekTo() run
        rule.runOnIdle {
            // Check initial values
            assertEquals(0, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(0, animatedValue3)
            // Seek half way
            runBlocking {
                seekableTransitionState.seekTo(fraction = 0.5f)
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
            seekableTransitionState.seekTo(0f, targetState = AnimStates.Other)
        }
        rule.mainClock.advanceTimeByFrame() // must recompose to Other
        rule.runOnIdle {
            assertEquals(AnimStates.Other, seekableTransitionState.targetState)
            // First frame, nothing has changed. We've only gathered the first frame of the
            // animation since it was not previously animating
            assertEquals(500, animatedValue1)
            assertEquals(0, animatedValue2)
            assertEquals(500, animatedValue3)
        }

        // Continue the initial value animation. It should use a linear animation.
        rule.mainClock.advanceTimeBy(80L) // 4 frames of animation
        rule.runOnIdle {
            assertEquals(500 + (500f * 80f / 150f), animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(500 + (500f * 80f / 150f), animatedValue3.toFloat(), 1f)
        }
        val seekToFraction = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0.5f)
            assertEquals(0.5f, seekableTransitionState.fraction)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            val expected1Value = 500 + (500f * 96f / 150f)
            assertEquals(expected1Value, animatedValue1.toFloat(), 1f)
            assertEquals(500, animatedValue2)
            assertEquals(
                expected1Value + 0.5f * (2000 - expected1Value),
                animatedValue3.toFloat(),
                1f
            )
        }

        // Advance to the end of the seekTo() animation
        rule.mainClock.advanceTimeBy(5_000)
        runBlocking { seekToFraction.await() }
        assertTrue(seekTo.isCancelled)
        rule.runOnIdle {
            // The initial values should be 1000/0/1000
            // Target values should be 1000, 1000, 2000
            // The seek is 0.5
            assertEquals(1000, animatedValue1)
            assertEquals(500, animatedValue2)
            assertEquals(1500, animatedValue3)
            runBlocking {
                seekableTransitionState.seekTo(fraction = 1f)
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
                seekableTransitionState.animateTo(AnimStates.To)
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
            seekableTransitionState.seekTo(0f, targetState = AnimStates.Other)
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
        }
        val seekToHalf = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0.5f)
            assertEquals(0.5f, seekableTransitionState.fraction)
        }
        rule.runOnIdle {
            assertEquals(500, animatedValue2)
        }

        // Advance to the end of the seekTo() animation
        rule.mainClock.advanceTimeBy(5_000)
        assertTrue(seekToHalf.isCompleted)
        assertTrue(seekTo.isCancelled)
        rule.runOnIdle {
            // The initial values should be 1000/0/1000
            // Target values should be 1000, 1000, 2000
            // The seek is 0.5
            assertEquals(1000, animatedValue1)
            assertEquals(500, animatedValue2)
            assertEquals(1500, animatedValue3)
        }
        coroutineScope.launch {
            seekableTransitionState.seekTo(fraction = 1f)
            assertEquals(1f, seekableTransitionState.fraction, 0f)
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
                seekableTransitionState.animateTo(AnimStates.To)
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
        val animateToOther = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.Other)
        }

        rule.mainClock.advanceTimeBy(16) // composition after animateTo()

        rule.runOnIdle {
            // initial should be 176/300 = 0.587 through animation
            assertEquals(586.7f, animatedValue1.toFloat(), 1f)
            assertEquals(0, animatedValue2)
            assertEquals(586.7f, animatedValue3.toFloat(), 1f)
        }

        // Lock in the animation for the animation to Other, but advance animation to To
        rule.mainClock.advanceTimeBy(16)
        rule.runOnIdle {
            // initial should be 192/300 = 0.640 through animation
            // target should be 16/300 = 0.053
            assertEquals(640f, animatedValue1.toFloat(), 1f)
            assertEquals(53.3f, animatedValue2.toFloat(), 1f)
            assertEquals(640f + ((2000f - 640f) * 0.053f), animatedValue3.toFloat(), 1f)
        }

        // Advance time by two more frames
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle {
            // initial should be 224/300 = 0.746.7 through animation
            // other should be 48/300 = 0.160 through the animation
            assertEquals(746.7f, animatedValue1.toFloat(), 1f)
            assertEquals(160f, animatedValue2.toFloat(), 1f)
            assertEquals(746.7f + ((2000f - 746.7f) * 0.160f), animatedValue3.toFloat(), 2f)
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
                seekableTransitionState.animateTo(targetState)
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

        // Compose the change
        rule.mainClock.advanceTimeBy(16)

        rule.runOnIdle {
            // The previous animation's start time can be used, so continue the animation
            assertEquals(640, animatedValue1)
            assertEquals(0, animatedValue2) // animation hasn't started yet
            assertEquals(640, animatedValue3) // animation hasn't started yet
        }

        // Advance one frame
        rule.mainClock.advanceTimeBy(16)
        rule.runOnIdle {
            assertEquals(693, animatedValue1)
            assertEquals(53, animatedValue2)
            assertEquals(693 + ((2000 - 693) * 16 / 300), animatedValue3)
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
        coroutineScope.launch { seekableTransitionState.seekTo(0f, targetState = AnimStates.To) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.seekTo(fraction = 0.5f) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.seekTo(0f, targetState = AnimStates.Other) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // lock in the initial value animation start time
        coroutineScope.launch { seekableTransitionState.seekTo(fraction = 0.5f) }
        rule.waitForIdle()
        coroutineScope.launch { seekableTransitionState.seekTo(0f, targetState = AnimStates.From) }
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
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(0.5f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
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
        coroutineScope.launch { seekableTransitionState.seekTo(fraction = 0.5f) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1000, animatedValue1)
    }

    @Test
    fun animateToWithSpec() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 100, easing = LinearEasing) }
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
        coroutineScope.launch {
            seekableTransitionState.seekTo(0.5f, AnimStates.To)
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        val deferred = coroutineScope.async {
            seekableTransitionState.animateTo(animationSpec = tween(1000, 0, LinearEasing))
        }
        rule.mainClock.advanceTimeByFrame() // lock in the start time
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle {
            // should be 500 + 500 * 64/1000 = 532
            assertEquals(532, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(192)
        rule.runOnIdle {
            // should be 500 + 500 * 256/1000 = 628
            assertEquals(628, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(256)
        rule.runOnIdle {
            // should be 500 + 500 * 512/1000 = 756
            assertEquals(756, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(512)
        rule.runOnIdle {
            assertTrue(deferred.isCompleted)
            assertEquals(1000, animatedValue1)
        }
    }

    @Test
    fun seekToFollowedByAnimation() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
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
        coroutineScope.launch {
            seekableTransitionState.seekTo(1f, AnimStates.To)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // let the composition happen after seekTo
        rule.runOnIdle { // seekTo() should run now, setting the animated value
            assertEquals(1000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation clock
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(984, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(968, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun conflictingSeekTo() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val defer1 = coroutineScope.async {
            seekableTransitionState.seekTo(1f, AnimStates.To)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        val defer2 = coroutineScope.async {
            seekableTransitionState.seekTo(1f, AnimStates.Other)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // let the composition happen after seekTo
        rule.runOnIdle {
            assertTrue(defer1.isCancelled)
            assertFalse(defer2.isCancelled)
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation clock
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(1968, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(0, animatedValue1)
            assertTrue(defer2.isCompleted)
        }
    }

    @Test
    fun conflictingSnapTo() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val defer1 = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.To)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        val defer2 = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.Other)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // let the composition happen after seekTo
        rule.runOnIdle {
            assertTrue(defer1.isCancelled)
            assertFalse(defer2.isCancelled)
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation clock
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(1968, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(0, animatedValue1)
            assertTrue(defer2.isCompleted)
        }
    }

    /**
     * Here, the first seekTo() doesn't do anything since the target is the same as the current
     * value. It only changes the fraction.
     */
    @Test
    fun conflictingSeekTo2() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.seekTo(1f, AnimStates.From)
            seekableTransitionState.animateTo(AnimStates.To)
        }
        coroutineScope.launch {
            seekableTransitionState.seekTo(1f, AnimStates.Other)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // let the composition happen after seekTo
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation clock
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(1968, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(0, animatedValue1)
        }
    }

    /**
     * Here, the first seekTo() doesn't do anything since the target is the same as the current
     * value. It only changes the fraction.
     */
    @Test
    fun conflictingSnapTo2() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.snapTo(AnimStates.From)
            seekableTransitionState.animateTo(AnimStates.To)
        }
        coroutineScope.launch {
            seekableTransitionState.snapTo(AnimStates.Other)
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // let the composition happen after snapTo
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the animation clock
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(1968, animatedValue1)
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun snapToStopsAllAnimations() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.seekTo(1f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val animation = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.Other)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val snapTo = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(animation.isCancelled)
            assertTrue(snapTo.isCompleted)
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun snapToSameTargetState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(0.5f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val snapTo = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(snapTo.isCompleted)
            assertEquals(1000, animatedValue1)
        }
    }

    @Test
    fun snapToSameCurrentState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(0.5f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val snapTo = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(snapTo.isCompleted)
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun snapToExistingState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val snapTo = coroutineScope.async {
            seekableTransitionState.snapTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(snapTo.isCompleted)
            assertEquals(0, animatedValue1)
        }
        val seekAndSnap = coroutineScope.async {
            seekableTransitionState.seekTo(0.5f, AnimStates.To)
            seekableTransitionState.snapTo(AnimStates.From)
            seekableTransitionState.snapTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // seekTo
        rule.mainClock.advanceTimeByFrame() // snapTo
        rule.runOnIdle {
            assertTrue(seekAndSnap.isCompleted)
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun animateAndContinueAnimation() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0f, targetState = AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after seekTo
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val animateTo = coroutineScope.async {
            seekableTransitionState.animateTo()
        }
        rule.mainClock.advanceTimeByFrame() // lock animation clock
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            assertEquals(160, animatedValue1)
        }

        val animateTo2 = coroutineScope.async {
            seekableTransitionState.animateTo()
        }

        rule.runOnIdle {
            assertTrue(animateTo.isCancelled)
        }

        rule.mainClock.advanceTimeByFrame() // continue the animation

        rule.runOnIdle {
            assertEquals(176, animatedValue1)
        }

        rule.mainClock.advanceTimeBy(900)

        rule.runOnIdle {
            assertTrue(animateTo2.isCompleted)
            assertEquals(1000, animatedValue1)
        }
    }

    @Test
    fun continueAnimationWithNewSpec() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0f, targetState = AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after seekTo
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val animateTo = coroutineScope.async {
            seekableTransitionState.animateTo()
        }
        rule.mainClock.advanceTimeByFrame() // lock animation clock
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            assertEquals(160, animatedValue1)
        }

        val animateTo2 = coroutineScope.async {
            seekableTransitionState.animateTo(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearEasing
                )
            )
        }

        rule.runOnIdle {
            assertTrue(animateTo.isCancelled)
        }

        rule.mainClock.advanceTimeByFrame() // continue the animation

        rule.runOnIdle {
            // 160 + (840 * 16/200) = 227.2
            assertEquals(227.2f, animatedValue1.toFloat(), 1f)
        }

        rule.mainClock.advanceTimeBy(200)

        rule.runOnIdle {
            assertTrue(animateTo2.isCompleted)
            assertEquals(1000, animatedValue1)
        }
    }

    @Test
    fun continueAnimationUsesInitialVelocity() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0f, targetState = AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after seekTo
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val animateTo = coroutineScope.async {
            seekableTransitionState.animateTo()
        }
        rule.mainClock.advanceTimeByFrame() // lock animation clock
        rule.mainClock.advanceTimeBy(800) // half way
        rule.runOnIdle {
            assertEquals(500, animatedValue1)
        }

        coroutineScope.launch {
            seekableTransitionState.animateTo(
                animationSpec = spring(
                    visibilityThreshold = 0.01f,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        }

        rule.runOnIdle {
            assertTrue(animateTo.isCancelled)
        }

        rule.mainClock.advanceTimeByFrame() // continue the animation

        rule.runOnIdle {
            // The velocity should be similar to what it was before after only one frame
            // 500 / 800 = 0.625 pixels per ms * 16 = 10 pixels
            assertEquals(510f, animatedValue1.toFloat(), 2f)
        }
    }

    @Test
    fun continueAnimationNewSpecUsesInitialVelocity() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(fraction = 0f, targetState = AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after seekTo
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
        }
        val springSpec = spring<Float>(dampingRatio = 2f)
        val vecSpringSpec = springSpec.vectorize(Float.VectorConverter)
        val animateTo = coroutineScope.async {
            seekableTransitionState.animateTo(animationSpec = springSpec)
        }
        rule.mainClock.advanceTimeByFrame() // lock animation clock

        // find how long it takes to get to about half way:
        var halfDuration = 16L
        val zeroVector = AnimationVector1D(0f)
        val oneVector = AnimationVector1D(1f)
        while (vecSpringSpec.getValueFromMillis(
                playTimeMillis = halfDuration,
                start = zeroVector,
                end = oneVector,
                startVelocity = zeroVector
            )[0] < 0.5f
        ) {
            halfDuration += 16L
        }
        rule.mainClock.advanceTimeBy(halfDuration) // ~half way
        val halfValue = vecSpringSpec.getValueFromMillis(
            playTimeMillis = halfDuration,
            start = zeroVector,
            end = oneVector,
            startVelocity = zeroVector
        )[0] * 1000
        rule.runOnIdle {
            assertEquals(halfValue, animatedValue1.toFloat(), 1f)
        }

        val velocityAtHalfWay = vecSpringSpec.getVelocityFromNanos(
            playTimeNanos = halfDuration * MillisToNanos,
            initialValue = zeroVector,
            targetValue = oneVector,
            initialVelocity = zeroVector
        )[0]

        coroutineScope.launch {
            seekableTransitionState.animateTo(
                animationSpec = spring(
                    visibilityThreshold = 0.01f,
                    stiffness = Spring.StiffnessVeryLow,
                    dampingRatio = Spring.DampingRatioHighBouncy
                )
            )
        }

        rule.runOnIdle {
            assertTrue(animateTo.isCancelled)
        }

        rule.mainClock.advanceTimeByFrame() // continue the animation

        rule.runOnIdle {
            // The velocity should be similar to what it was before after only one frame
            assertEquals(halfValue + (velocityAtHalfWay * 16f), animatedValue1.toFloat(), 2f)
        }
    }

    @Test
    fun animationCompletionHasNoInitialValueAnimation() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                val target = when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
                target
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeBy(1700)
        coroutineScope.launch {
            seekableTransitionState.animateTo(AnimStates.From)
        }
        rule.mainClock.advanceTimeByFrame() // lock in the clock
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(990, animatedValue1)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(980, animatedValue1)
        }
    }

    @Test
    fun animationDurationWorksOnInitialStateChange() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.Other -> 2000
                    else -> 1000
                }
            }
            val val2 = transition.animateInt(
                label = "Value2",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
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
                        animatedValue2 = val2.value
                    })
        }
        rule.waitForIdle()
        coroutineScope.launch {
            seekableTransitionState.animateTo(
                AnimStates.To,
                animationSpec = tween(durationMillis = 160, easing = LinearEasing)
            )
        }
        rule.mainClock.advanceTimeByFrame() // lock in the clock
        coroutineScope.launch {
            seekableTransitionState.animateTo(AnimStates.Other)
        }
        rule.mainClock.advanceTimeByFrame() // advance one frame toward To and compose to Other
        rule.runOnIdle {
            assertEquals(100, animatedValue1)
            assertEquals(100, animatedValue2)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // 200 + (1800 * 16/1600) = 218
            assertEquals(218, animatedValue1)
            // continue the animatedValue2 animation
            assertEquals(200, animatedValue2)
        }
        rule.mainClock.advanceTimeBy(128)
        rule.runOnIdle {
            // 1000 + (1000 * 144/1600) = 1090
            assertEquals(1090, animatedValue1)
            assertEquals(1000, animatedValue2)
        }
        rule.mainClock.advanceTimeBy(1600)
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
            assertEquals(1000, animatedValue2)
        }
    }

    @Test
    fun animationAlreadyAtTarget() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
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
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(1f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
            assertEquals(1000, animatedValue1)
        }
        val anim = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // compose to current state = target state
        rule.runOnIdle {
            assertTrue(anim.isCompleted)
            assertEquals(AnimStates.To, seekableTransitionState.currentState)
            assertEquals(AnimStates.To, seekableTransitionState.targetState)
        }
    }

    @Test
    fun seekCurrentEqualsTarget() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
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
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(0.5f)
        }
        rule.runOnIdle {
            assertTrue(seekTo.isCompleted)
            assertEquals(0, animatedValue1)
        }
    }

    @Test
    fun animateToAtEndCurrentInitialValueAnimations() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    AnimStates.Other -> 2000
                }
            }
            val val2 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
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
                        animatedValue2 = val2.value
                    })
        }
        rule.waitForIdle()
        val seekTo = coroutineScope.async {
            seekableTransitionState.seekTo(0f, AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // compose to To
        assertTrue(seekTo.isCompleted)
        val seekOther = coroutineScope.async {
            seekableTransitionState.seekTo(1f, AnimStates.Other)
        }
        rule.mainClock.advanceTimeByFrame() // compose to Other
        assertFalse(seekOther.isCompleted) // should be animating animatedValue2
        val animateOther = coroutineScope.async {
            // already at the end (1f), but it should continue the animatedValue2 animation
            seekableTransitionState.animateTo(AnimStates.Other)
        }
        assertTrue(seekOther.isCancelled)
        assertTrue(animateOther.isActive)
        rule.mainClock.advanceTimeByFrame() // advance the animation
        rule.runOnIdle {
            assertTrue(animateOther.isActive)
            assertEquals(2000, animatedValue1)
            assertEquals(1000 * 16 / 1600, animatedValue2)
        }
    }

    @Test
    fun changingDuration() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }
            val val2 = if (val1.value < 500) {
                mutableFloatStateOf(0f)
            } else {
                transition.animateFloat(
                    label = "Value2",
                    transitionSpec = { tween(durationMillis = 3200, easing = LinearEasing) }
                ) { state ->
                    when (state) {
                        AnimStates.From -> 0f
                        else -> 1000f
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value.roundToInt()
                    })
        }
        rule.waitForIdle()
        val anim = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition
        rule.mainClock.advanceTimeBy(800) // half way through
        rule.runOnIdle {
            assertEquals(500, animatedValue1)
            assertEquals(0, animatedValue2)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(510, animatedValue1)
            assertEquals(5, animatedValue2)
        }
        rule.mainClock.advanceTimeBy(784)
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
            assertEquals(250, animatedValue2)
            assertFalse(anim.isCompleted)
        }
        rule.mainClock.advanceTimeBy(2400)
        rule.runOnIdle {
            assertEquals(1000, animatedValue1)
            assertEquals(1000, animatedValue2)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition
        assertTrue(anim.isCompleted)
    }

    @Test
    fun changingAnimationWithAnimateToThirdState() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        var animatedValue2 by mutableFloatStateOf(-1f)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    AnimStates.To -> 1000
                    else -> 2000
                }
            }
            val val2 = if (val1.value < 500) {
                mutableFloatStateOf(0f)
            } else {
                transition.animateFloat(
                    label = "Value2",
                    transitionSpec = { tween(durationMillis = 3200, easing = LinearEasing) }
                ) { state ->
                    when (state) {
                        AnimStates.From -> 0f
                        AnimStates.To -> 1000f
                        else -> 2000f
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        animatedValue1 = val1.value
                        animatedValue2 = val2.value
                    })
        }
        rule.waitForIdle()
        val animateTo = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition
        rule.mainClock.advanceTimeBy(800) // half way through

        rule.runOnIdle {
            // Won't have advanced the value to Other, but will continue advance to To
            assertEquals(1000 * 800 / 1600, animatedValue1)
            assertEquals(1000 * 0 / 3200, animatedValue2.roundToInt())
        }
        rule.mainClock.advanceTimeByFrame() // one frame past recomposition, so animation is running

        rule.runOnIdle {
            // Won't have advanced the value to Other, but will continue advance to To
            assertEquals(1000 * 816 / 1600, animatedValue1)
            assertEquals(1000 * 16 / 3200, animatedValue2.roundToInt())
        }

        // now seek to third state
        val animateOther = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.Other)
        }
        assertTrue(animateTo.isCancelled)
        rule.mainClock.advanceTimeByFrame() // wait for composition
        rule.runOnIdle {
            // Won't have advanced the value to Other, but will continue advance to To
            assertEquals(1000 * 832 / 1600, animatedValue1)
            assertEquals(1000 * 32 / 3200, animatedValue2.roundToInt())
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            // Continues the advance to Other
            val anim1Value1 = 1000 * 848 / 1600
            val anim1Value2 = 1000 * 48 / 3200
            assertEquals(anim1Value1 + ((2000 - anim1Value1) * 16 / 1600), animatedValue1)
            assertEquals(anim1Value2 + ((2000f - anim1Value2) * 16 / 3200), animatedValue2)
        }

        rule.mainClock.advanceTimeBy(752)
        rule.runOnIdle {
            val anim1Value1 = 1000
            val anim1Value2 = 1000 * 800 / 3200
            assertEquals(anim1Value1 + ((2000 - anim1Value1) * 768 / 1600), animatedValue1)
            assertEquals(anim1Value2 + ((2000f - anim1Value2) * 768 / 3200), animatedValue2)
            assertFalse(animateOther.isCompleted)
        }

        rule.mainClock.advanceTimeBy(832)
        rule.runOnIdle {
            val anim1Value2 = 1000 * 1632 / 3200
            assertEquals(2000, animatedValue1)
            assertEquals(anim1Value2 + ((2000f - anim1Value2) * 1600 / 3200), animatedValue2)
            assertFalse(animateOther.isCompleted)
        }

        rule.mainClock.advanceTimeBy(1600)
        rule.runOnIdle {
            assertEquals(2000, animatedValue1)
            assertEquals(2000f, animatedValue2)
            assertFalse(animateOther.isCompleted)
        }

        rule.mainClock.advanceTimeByFrame() // composition after the current value changes
        rule.runOnIdle {
            assertTrue(animateOther.isCompleted)
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun animateAfterSeekToZero() {
        rule.mainClock.autoAdvance = false
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        var animatedValue1 by mutableIntStateOf(-1)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(seekableTransitionState, label = "Test")
            val val1 = transition.animateInt(
                label = "Value",
                transitionSpec = { tween(durationMillis = 1600, easing = LinearEasing) }
            ) { state ->
                when (state) {
                    AnimStates.From -> 0
                    else -> 1000
                }
            }

            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("AV_parent")
                        .drawBehind {
                            animatedValue1 = val1.value
                            drawRect(Color.White)
                        }) {
                    transition.AnimatedVisibility({ it == AnimStates.To }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        val initialAnimateAndSeek = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
            seekableTransitionState.seekTo(0.5f, targetState = AnimStates.From)
            seekableTransitionState.seekTo(0f, targetState = AnimStates.From)
        }
        rule.mainClock.advanceTimeBy(5000)
        rule.runOnIdle {
            assertTrue(initialAnimateAndSeek.isCompleted)
            assertEquals(1000, animatedValue1)
        }
        rule.onNodeWithTag("AV_parent").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run {
                assertEquals(100, width)
                assertEquals(100, height)
                assertPixels { _ ->
                    Color.Red
                }
            }
        }
        val secondAnimate = coroutineScope.async {
            seekableTransitionState.animateTo(AnimStates.To)
        }
        rule.waitForIdle()
        // This waits for the initial state animation to finish, since we changed the initial state
        // when going from seeking to animating.
        rule.mainClock.advanceTimeBy(5000)
        rule.runOnIdle {
            assertTrue(secondAnimate.isCompleted)
            assertEquals(1000, animatedValue1)
        }
        rule.onNodeWithTag("AV_parent").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run {
                assertEquals(100, width)
                assertEquals(100, height)
                assertPixels { _ ->
                    Color.Red
                }
            }
        }
    }

    @Test
    fun isRunningDuringAnimateTo() {
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var transition: Transition<AnimStates>
        var animatedValue by mutableIntStateOf(-1)

        rule.mainClock.autoAdvance = false

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                seekableTransitionState.animateTo(AnimStates.To)
            }
            transition = rememberTransition(seekableTransitionState, label = "Test")
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
            assertFalse(transition.isRunning)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after animateTo()
        rule.mainClock.advanceTimeByFrame() // one frame to set the start time
        rule.runOnIdle {
            assertTrue(animatedValue > 0)
            assertTrue(transition.isRunning)
        }
        rule.mainClock.advanceTimeBy(5000)
        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertFalse(transition.isRunning)
        }
    }

    @Test
    fun isRunningFalseAfterSnapTo() {
        val seekableTransitionState = SeekableTransitionState(AnimStates.From)
        lateinit var transition: Transition<AnimStates>
        var animatedValue by mutableIntStateOf(-1)

        rule.mainClock.autoAdvance = false

        rule.setContent {
            LaunchedEffect(seekableTransitionState) {
                awaitFrame() // Not sure why this is needed. Animated val doesn't change without it.
                seekableTransitionState.snapTo(AnimStates.To)
            }
            transition = rememberTransition(seekableTransitionState, label = "Test")
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
            assertFalse(transition.isRunning)
        }
        rule.mainClock.advanceTimeByFrame() // wait for composition after animateTo()
        rule.mainClock.advanceTimeByFrame() // one frame to snap
        rule.mainClock.advanceTimeByFrame() // one frame for LaunchedEffect's awaitFrame()
        rule.runOnIdle {
            assertEquals(1000, animatedValue)
            assertFalse(transition.isRunning)
        }
    }

    @Test
    fun testCleanupAfterDispose() {
        fun isObserving(): Boolean {
            var active = false
            SeekableStateObserver.clearIf {
                active = true
                false
            }
            return active
        }

        var seekableState: SeekableTransitionState<*>?
        var disposed by mutableStateOf(false)

        rule.setContent {
            seekableState = remember { SeekableTransitionState(true) }

            if (!disposed) {
                rememberTransition(transitionState = seekableState!!)
            }
        }
        rule.waitForIdle()
        assertTrue(isObserving())

        disposed = true
        rule.waitForIdle()
        assertFalse(isObserving())
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun quickAddAndRemove() {
        @Stable
        class ScreenState(
            val label: String,
            removing: Boolean = false,
        ) {
            var removing by mutableStateOf(removing)
        }

        var labelIndex = 1
        val screenStates = mutableStateListOf(ScreenState("1"))
        val seekableScreenTransitionState = SeekableTransitionState(screenStates.toList())

        rule.setContent {
            val screenTransition = rememberTransition(seekableScreenTransitionState)
            LaunchedEffect(Unit) {
                snapshotFlow { screenStates.toList().filter { !it.removing } }
                    .collectLatest { capturedScreenStates ->
                        seekableScreenTransitionState.animateTo(capturedScreenStates)
                        // Done animating
                        screenStates.fastForEachReversed {
                            if (it.removing) {
                                screenStates.remove(it)
                            }
                        }
                    }
            }

            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    var lastVisibleIndex = screenStates.size - 1
                    while (lastVisibleIndex >= 0 && screenStates[lastVisibleIndex].removing) {
                        lastVisibleIndex--
                    }

                    screenStates.forEach { screenState ->
                        key(screenState) {
                            val visibleTransition =
                                screenTransition.createChildTransition {
                                    screenState === it.lastOrNull() && !screenState.removing
                                }
                            visibleTransition.AnimatedVisibility(
                                visible = { it },
                            ) {
                                Text(
                                    "Hello ${screenState.label}",
                                    Modifier.testTag(screenState.label)
                                )
                            }
                        }
                    }
                    Text(
                        "screenStates:\n${
                            screenStates.reversed().joinToString("\n") {
                                it.label +
                                    if (it.removing) " (removing)" else ""
                            }
                        }",
                        Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }
        fun removeState() {
            rule.runOnUiThread { screenStates.last { !it.removing }.removing = true }
        }
        fun addState() {
            rule.runOnUiThread { screenStates += ScreenState(label = "${++labelIndex}") }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        addState()
        rule.mainClock.advanceTimeBy(50)
        removeState()
        rule.mainClock.advanceTimeBy(50)
        addState()
        rule.mainClock.advanceTimeBy(50)
        removeState()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsNotDisplayed()
        rule.onNodeWithTag("3").assertIsNotDisplayed()
    }
}
