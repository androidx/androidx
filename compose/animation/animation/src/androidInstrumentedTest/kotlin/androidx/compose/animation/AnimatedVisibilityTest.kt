/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(InternalAnimationApi::class)
class AnimatedVisibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private val frameDuration = 16

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun animateVisibilityExpandShrinkTest() {
        val testModifier by mutableStateOf(TestModifier())
        var visible by mutableStateOf(false)
        var offset by mutableStateOf(Offset(0f, 0f))
        var disposed by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AnimatedVisibility(
                    visible, testModifier,
                    enter = expandIn(
                        tween(160, easing = LinearOutSlowInEasing),
                        Alignment.BottomEnd
                    ) { fullSize ->
                        IntSize(fullSize.width / 4, fullSize.height / 2)
                    },

                    exit = shrinkOut(
                        tween(160, easing = FastOutSlowInEasing),
                        Alignment.CenterStart
                    ) { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 5) },
                ) {
                    Box(
                        Modifier
                            .requiredSize(100.dp, 100.dp)
                            .onGloballyPositioned {
                                offset = it.localToRoot(Offset.Zero)
                            }
                    ) {
                        DisposableEffect(Unit) {
                            onDispose {
                                disposed = true
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            visible = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val startWidth = 100 / 4f
        val startHeight = 100 / 2f
        val fullSize = 100f
        assertFalse(disposed)

        for (i in 0..160 step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / 160f)
            val animWidth = lerp(startWidth, fullSize, fraction)
            val animHeight = lerp(startHeight, fullSize, fraction)
            // Check size
            assertEquals(animWidth, testModifier.width.toFloat(), 2f)
            assertEquals(animHeight, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(animWidth - fullSize, offset.x, 2f)
            assertEquals(animHeight - fullSize, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }

        rule.runOnIdle {
            visible = false
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val endWidth = 100 / 10f
        val endHeight = 100 / 5f
        for (i in 0..160 step frameDuration) {
            val fraction = FastOutSlowInEasing.transform(i / 160f)
            val animWidth = lerp(fullSize, endWidth, fraction)
            val animHeight = lerp(fullSize, endHeight, fraction)
            // Check size
            assertEquals(animWidth, testModifier.width.toFloat(), 2f)
            assertEquals(animHeight, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(0f, offset.x, 2f)
            assertEquals((animHeight - fullSize) / 2f, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }

        // Check that the composable children in AnimatedVisibility are skipped after exit animation
        rule.mainClock.autoAdvance = true
        rule.waitUntil { disposed }
        rule.mainClock.autoAdvance = false

        // Make it visible again, and test that it behaves the same as before
        rule.runOnIdle {
            visible = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        for (i in 0..160 step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / 160f)
            val animWidth = lerp(startWidth, fullSize, fraction)
            val animHeight = lerp(startHeight, fullSize, fraction)
            // Check size
            assertEquals(animWidth, testModifier.width.toFloat(), 2f)
            assertEquals(animHeight, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(animWidth - fullSize, offset.x, 2f)
            assertEquals(animHeight - fullSize, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun animateVisibilitySlideTest() {
        val testModifier by mutableStateOf(TestModifier())
        var visible by mutableStateOf(false)
        var density = 0f
        var offset by mutableStateOf(Offset(0f, 0f))
        var disposed by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AnimatedVisibility(
                visible, testModifier,
                enter = slideIn(
                    tween(160, easing = LinearOutSlowInEasing)
                ) { fullSize -> IntOffset(fullSize.width / 4, -fullSize.height / 2) },
                exit = slideOut(
                    tween(160, easing = FastOutSlowInEasing)
                ) { fullSize -> IntOffset(-fullSize.width / 10, fullSize.height / 5) },
            ) {
                Box(
                    Modifier
                        .requiredSize(100.dp, 100.dp)
                        .onGloballyPositioned {
                            offset = it.localToRoot(Offset.Zero)
                        }
                ) {
                    DisposableEffect(Unit) {
                        onDispose {
                            disposed = true
                        }
                    }
                }
            }
            density = LocalDensity.current.density
        }

        rule.runOnIdle {
            visible = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val startX = density * 100 / 4f
        val startY = -density * 100 / 2f
        val fullSize = density * 100
        assertFalse(disposed)

        for (i in 0..160 step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / 160f)
            val animX = lerp(startX, 0f, fraction)
            val animY = lerp(startY, 0f, fraction)
            // Check size
            assertEquals(fullSize, testModifier.width.toFloat(), 2f)
            assertEquals(fullSize, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(animX, offset.x, 2f)
            assertEquals(animY, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }

        rule.runOnIdle {
            visible = false
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val endX = -density * 100 / 10f
        val endY = density * 100 / 5f
        for (i in 0..160 step frameDuration) {
            val fraction = FastOutSlowInEasing.transform(i / 160f)
            val animX = lerp(0f, endX, fraction)
            val animY = lerp(0f, endY, fraction)
            // Check size
            assertEquals(fullSize, testModifier.width.toFloat(), 2f)
            assertEquals(fullSize, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(animX, offset.x, 2f)
            assertEquals(animY, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }

        // Check that the composable children in AnimatedVisibility are skipped after exit animation
        rule.mainClock.autoAdvance = true
        rule.waitUntil { disposed }
        rule.mainClock.autoAdvance = false

        // Make it visible again, and test that it behaves the same as before
        rule.runOnIdle {
            visible = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        for (i in 0..160 step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / 160f)
            val animX = lerp(startX, 0f, fraction)
            val animY = lerp(startY, 0f, fraction)
            // Check size
            assertEquals(fullSize, testModifier.width.toFloat(), 2f)
            assertEquals(fullSize, testModifier.height.toFloat(), 2f)
            // Check offset
            assertEquals(animX, offset.x, 2f)
            assertEquals(animY, offset.y, 2f)
            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun animateVisibilityContentSizeChangeTest() {
        val size = mutableStateOf(40.dp)
        val testModifier by mutableStateOf(TestModifier())
        var visible by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AnimatedVisibility(visible, testModifier) {
                    Box(modifier = Modifier.size(size = size.value))
                }
            }
        }
        rule.runOnIdle {
            assertEquals(40, testModifier.height)
            assertEquals(40, testModifier.width)
            size.value = 60.dp
        }
        rule.runOnIdle {
            assertEquals(60, testModifier.height)
            assertEquals(60, testModifier.width)
        }
        rule.runOnIdle {
            visible = false
        }
        rule.runOnIdle {
            visible = true
        }
        rule.runOnIdle {
            assertEquals(60, testModifier.height)
            assertEquals(60, testModifier.width)
            size.value = 30.dp
        }
        rule.runOnIdle {
            assertEquals(30, testModifier.height)
            assertEquals(30, testModifier.width)
        }
    }

    // Test different animations for fade in and fade out, in a complete run without interruptions
    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun animateVisibilityFadeTest() {
        var visible by mutableStateOf(false)
        val easing = FastOutLinearInEasing
        val easingOut = FastOutSlowInEasing
        var alpha by mutableStateOf(0f)
        rule.setContent {
            AnimatedVisibility(
                visible,
                enter = fadeIn(animationSpec = tween(500, easing = easing)),
                exit = fadeOut(animationSpec = tween(300, easing = easingOut)),
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 20.dp)
                        .background(Color.White)
                )
                LaunchedEffect(visible) {
                    var exit = false
                    val enterExit = transition
                    while (true) {
                        withFrameNanos {
                            if (enterExit.targetState == Visible) {
                                alpha = enterExit.animations.firstOrNull {
                                    it.label.contains("alpha", true)
                                }?.value as Float
                                val fraction =
                                    (enterExit.playTimeNanos / 1_000_000) / 500f
                                if (enterExit.currentState != Visible) {
                                    assertEquals(easing.transform(fraction), alpha, 0.01f)
                                } else {
                                    // When currentState = targetState, the playTime will be reset
                                    // to 0. So compare alpha against expected visible value.
                                    assertEquals(1f, alpha)
                                    exit = true
                                }
                            } else if (enterExit.targetState == PostExit) {
                                alpha = enterExit.animations.firstOrNull {
                                    it.label.contains("alpha", true)
                                }?.value as Float
                                val fraction =
                                    (enterExit.playTimeNanos / 1_000_000) / 300f
                                if (enterExit.currentState != PostExit) {
                                    assertEquals(
                                        1f - easingOut.transform(fraction),
                                        alpha,
                                        0.01f
                                    )
                                } else {
                                    // When currentState = targetState, the playTime will be reset
                                    // to 0. So compare alpha against expected invisible value.
                                    assertEquals(0f, alpha)
                                    exit = true
                                }
                            } else {
                                exit = enterExit.currentState == enterExit.targetState
                            }
                        }
                        if (exit) break
                    }
                }
            }
        }
        rule.runOnIdle {
            visible = true
        }
        rule.runOnIdle {
            // At this point fade in has finished, expect alpha = 1
            assertEquals(1f, alpha)
            visible = false
        }
        rule.runOnIdle {
            // At this point fade out has finished, expect alpha = 0
            assertEquals(0f, alpha)
        }
    }

    // Test different animations for scale in and scale out, in a complete run without interruptions
    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun animateVisibilityScaleTest() {
        var visible by mutableStateOf(false)
        val easing = FastOutLinearInEasing
        val easingOut = FastOutSlowInEasing
        var scale by mutableStateOf(0f)
        rule.setContent {
            AnimatedVisibility(
                visible,
                enter = scaleIn(animationSpec = tween(500, easing = easing)),
                exit = scaleOut(animationSpec = tween(300, easing = easingOut)),
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 20.dp)
                        .background(Color.White)
                )
                LaunchedEffect(visible) {
                    var exit = false
                    val enterExit = transition
                    while (true) {
                        withFrameNanos {
                            if (enterExit.targetState == Visible) {
                                scale = enterExit.animations.firstOrNull {
                                    it.label.contains("scale", true)
                                }?.value as Float
                                val fraction =
                                    (enterExit.playTimeNanos / 1_000_000) / 500f
                                if (enterExit.currentState != Visible) {
                                    assertEquals(easing.transform(fraction), scale, 0.01f)
                                } else {
                                    // When currentState = targetState, the playTime will be reset
                                    // to 0. So compare scale against expected visible value.
                                    assertEquals(1f, scale)
                                    exit = true
                                }
                            } else if (enterExit.targetState == PostExit) {
                                scale = enterExit.animations.firstOrNull {
                                    it.label.contains("scale", true)
                                }?.value as Float
                                val fraction =
                                    (enterExit.playTimeNanos / 1_000_000) / 300f
                                if (enterExit.currentState != PostExit) {
                                    assertEquals(
                                        1f - easingOut.transform(fraction),
                                        scale,
                                        0.01f
                                    )
                                } else {
                                    // When currentState = targetState, the playTime will be reset
                                    // to 0. So compare scale against expected invisible value.
                                    assertEquals(0f, scale)
                                    exit = true
                                }
                            } else {
                                exit = enterExit.currentState == enterExit.targetState
                            }
                        }
                        if (exit) break
                    }
                }
            }
        }
        rule.runOnIdle {
            visible = true
        }
        rule.runOnIdle {
            // At this point fade in has finished, expect alpha = 1
            assertEquals(1f, scale)
            visible = false
        }
        rule.runOnIdle {
            // At this point fade out has finished, expect scale = 0
            assertEquals(0f, scale)
        }
    }

    @Test
    fun testEnterTransitionNoneAndExitTransitionNone() {
        val testModifier by mutableStateOf(TestModifier())
        val visible = MutableTransitionState(false)
        var disposed by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AnimatedVisibility(
                    visible, testModifier,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    Box(Modifier.requiredSize(100.dp, 100.dp)) {
                        DisposableEffect(Unit) {
                            onDispose {
                                disposed = true
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, testModifier.width)
            assertEquals(0, testModifier.height)
            visible.targetState = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(100, testModifier.width)
            assertEquals(100, testModifier.height)
            assertFalse(disposed)
            visible.targetState = false
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(disposed)
        }
    }

    private enum class TestState { State1, State2, State3 }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testTransitionExtensionAnimatedVisibility() {
        val testModifier by mutableStateOf(TestModifier())
        val testState = mutableStateOf(TestState.State1)
        var currentState = TestState.State1
        var disposed by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = updateTransition(testState.value)
                currentState = transition.currentState
                transition.AnimatedVisibility(
                    // Only visible in State2
                    visible = { it == TestState.State2 },
                    modifier = testModifier.testTag("content"),
                    // Must use LinearEasing otherwise the target size will be reached before the
                    // animation finishes running.
                    enter = expandIn(animationSpec = tween(100, easing = LinearEasing)),
                    exit = shrinkOut(animationSpec = tween(100, easing = LinearEasing))
                ) {
                    Box(Modifier.requiredSize(100.dp, 100.dp)) {
                        DisposableEffect(Unit) {
                            onDispose {
                                disposed = true
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertThat(testModifier.width).isEqualTo(0)
            assertThat(testModifier.height).isEqualTo(0)
            testState.value = TestState.State2
        }
        while (currentState != TestState.State2) {
            assertThat(testModifier.width).isLessThan(100)
            rule.mainClock.advanceTimeByFrame()
        }
        rule.runOnIdle {
            assertThat(testModifier.width).isEqualTo(100)
            assertThat(testModifier.height).isEqualTo(100)
            testState.value = TestState.State3
        }
        while (currentState != TestState.State3) {
            assertThat(testModifier.width).isGreaterThan(0)
            rule.onNodeWithTag("content").assertExists()
            assertThat(disposed).isFalse()
            rule.mainClock.advanceTimeByFrame()
        }
        // When the hide animation finishes, it will never get measured with size 0 because the
        // animation will remove it from the composition instead.
        rule.onNodeWithTag("content").assertDoesNotExist()
        rule.runOnIdle {
            assertThat(disposed).isTrue()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testSeekingAnimatedVisibility() {
        fun <T> spec() = tween<T>(200, easing = LinearEasing)
        var rootTransition: Transition<Boolean>? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                updateTransition(false, label = "test").apply {
                    rootTransition = this
                }.AnimatedVisibility(
                    { it },
                    enter = fadeIn(animationSpec = spec()) +
                        slideInVertically(animationSpec = spec()) { 200 },
                    exit = scaleOut(animationSpec = spec()) +
                        shrinkHorizontally(animationSpec = spec())
                ) {
                    Box(Modifier.size(200.dp))
                }
            }
        }
        rule.waitForIdle()

        val transition = rootTransition!!
        var playTimeMs = 0
        while (true) {
            // Seeking the enter transition, and check alpha & slide
            transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                false, true, playTimeMs * 1_000_000L
            )
            rule.waitForIdle()
            assertEquals(200_000_000L, transition.totalDurationNanos)

            val alpha = transition.transitions[0].animations.firstOrNull {
                it.label.contains("alpha")
            }?.value
            val slide = transition.transitions[0].animations.firstOrNull {
                it.label.contains("slide")
            }?.value
            if (playTimeMs * 1_000_000L > transition.totalDurationNanos) {
                // Finished. Check some end condition
                assertEquals(1f, alpha)
                assertEquals(IntOffset(0, 0), slide)
                break
            } else {
                assertEquals(playTimeMs / 200f, alpha as Float, 0.01f)
                assertEquals(IntOffset(0, 200 - playTimeMs), slide)
            }
            playTimeMs += 20
        }

        playTimeMs = 0

        while (true) {
            // Seeking the exit transition, and check scale & shrink
            transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                true, false, playTimeMs * 1_000_000L
            )
            rule.waitForIdle()
            assertEquals(200_000_000L, transition.totalDurationNanos)

            val scale = transition.transitions[0].animations.firstOrNull {
                it.label.contains("scale")
            }?.value
            val shrink = transition.transitions[0].animations.firstOrNull {
                it.label.contains("shrink")
            }?.value
            if (playTimeMs * 1_000_000L > transition.totalDurationNanos) {
                // Finished. Check some end condition
                assertEquals(0f, scale)
                assertEquals(IntSize(0, 200), shrink)
                break
            } else {
                assertEquals(1f - playTimeMs / 200f, scale as Float, 0.01f)
                assertEquals(IntSize(200 - playTimeMs, 200), shrink)
            }
            playTimeMs += 20
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testAnimatedVisibilityInLookaheadScope() {
        val lookaheadSizes = mutableListOf<IntSize>()
        var visible by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(Modifier.layout { measurable, constraints ->
                        measurable.measure(constraints).run {
                            if (isLookingAhead) {
                                lookaheadSizes.add(IntSize(width, height))
                            }
                            layout(width, height) { place(0, 0) }
                        }
                    }) {
                        AnimatedVisibility(visible = visible) {
                            Box(Modifier.size(200.dp, 100.dp))
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertTrue(visible)
            assertTrue(lookaheadSizes.isNotEmpty())
            lookaheadSizes.forEach {
                assertEquals(IntSize(200, 100), it)
            }
            lookaheadSizes.clear()
            visible = !visible
        }
        rule.runOnIdle {
            assertFalse(visible)
            assertTrue(lookaheadSizes.isNotEmpty())
            lookaheadSizes.forEach {
                assertEquals(IntSize.Zero, it)
            }
            lookaheadSizes.clear()
        }
    }

    @Test
    fun interruptedExitAnimationUsesCorrectTransition() = with(rule.density) {
        var visible by mutableStateOf(false)

        val duration = 16 * 20 // 20 frames
        val animation = { tween<IntOffset>(duration, easing = LinearEasing) }

        val boxSizePx = 100
        val enterDistance = 180
        val undesiredExitDistance = -200
        val expectedExitDistance = 200

        var boxPosition = IntOffset.Zero

        rule.setContent {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(animation()) { enterDistance },
                exit = if (visible) {
                    slideOutHorizontally(animation()) { undesiredExitDistance }
                } else {
                    // Only this transition should apply
                    slideOutHorizontally(animation()) { expectedExitDistance }
                }
            ) {
                Box(
                    Modifier
                        .requiredSize(boxSizePx.toDp())
                        .background(Color.Red)
                        .onGloballyPositioned {
                            boxPosition = it
                                .positionInRoot()
                                .round()
                        }
                )
            }
        }
        rule.waitForIdle()

        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            visible = true
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Animate towards half of the transition and interrupt it by toggling the state
        rule.mainClock.advanceTimeBy(duration / 2L)
        rule.runOnIdle {
            // Verify the position corresponds to half of the animation
            assertEquals(IntOffset(enterDistance / 2, 0), boxPosition)
            visible = false
        }
        val positionAtInterruption = boxPosition

        // Run the animation for a few steps/frames to guarantee each frame is rendered, since a
        // spring is used after interruption it's unknown how many more frames we need till the end
        // of the animation, but we don't need to run the entire animation.
        // We also need to run more than one step, since springs keep their initial momentum (in
        // this case, momentum towards the left), so the next frame might still move the box towards
        // the left.
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        // After a few frames, we can check the position of the box. If it used the expected
        // transition, its current position should be offset to the right.
        assertEquals(0, boxPosition.y)
        assertThat(boxPosition.x).isGreaterThan(positionAtInterruption.x)
    }
}
