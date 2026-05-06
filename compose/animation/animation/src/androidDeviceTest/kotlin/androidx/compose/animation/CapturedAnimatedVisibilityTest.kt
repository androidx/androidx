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

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.test.StandardTestDispatcher
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CapturedAnimatedVisibilityTest {
    val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    @Test
    fun testCapturedAnimatedVisibility_disposesContentImmediatelyOnExit() {
        var visible by mutableStateOf(true)
        var isContentPresent = false

        rule.setContent {
            CapturedAnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                DisposableEffect(Unit) {
                    isContentPresent = true
                    onDispose { isContentPresent = false }
                }
                Box(Modifier.size(100.dp))
            }
        }

        assertTrue("Content should initially be present in composition", isContentPresent)

        rule.mainClock.autoAdvance = false
        // Trigger exit
        visible = false
        rule.mainClock.advanceTimeByFrame()

        assertFalse(
            "Content should be disposed immediately when visible becomes false in CapturedAnimatedVisibility",
            isContentPresent,
        )

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCapturedAnimatedVisibility_fadingOutPixelVerification() {
        var visible by mutableStateOf(true)
        var isContentPresent = false
        rule.mainClock.autoAdvance = false

        rule.setContent {
            Box(Modifier.size(40.dp).testTag("container").background(Color.White)) {
                CapturedAnimatedVisibility(
                    visible = visible,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(160, easing = LinearEasing)),
                ) {
                    DisposableEffect(Unit) {
                        isContentPresent = true
                        onDispose { isContentPresent = false }
                    }
                    Box(Modifier.size(40.dp).background(Color.Red))
                }
            }
        }

        // Verify initial state: content present and rendered full Red
        rule.mainClock.advanceTimeByFrame()
        assertTrue("Content should be in composition initially", isContentPresent)
        rule.onNodeWithTag("container").captureToImage().assertPixels { Color.Red }

        // Start exit animation
        visible = false
        rule.mainClock.advanceTimeByFrame()

        // Content must be immediately disposed from composition tree
        assertFalse("Content should be disposed immediately upon exit", isContentPresent)

        // Advance clock to 50% opacity (80ms animation progress)
        rule.mainClock.advanceTimeBy(96)
        rule.onNodeWithTag("container").captureToImage().assertPixels {
            Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
        }

        // Advance past end of transition (total > 160ms)
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()
        rule.onNodeWithTag("container").captureToImage().assertPixels { Color.White }
    }

    @Test
    fun testCapturedAnimatedVisibility_mutableTransitionState() {
        val visibleState = MutableTransitionState(false)
        var isContentPresent = false

        rule.setContent {
            CapturedAnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                DisposableEffect(Unit) {
                    isContentPresent = true
                    onDispose { isContentPresent = false }
                }
                Box(Modifier.size(100.dp))
            }
        }

        assertFalse("Content should initially not be in composition", isContentPresent)
        assertTrue(visibleState.isIdle)
        assertFalse(visibleState.currentState)
        assertFalse(visibleState.targetState)

        // Trigger enter
        visibleState.targetState = true
        rule.waitForIdle()

        assertTrue("Content should be in composition after entering", isContentPresent)
        assertTrue(visibleState.isIdle)
        assertTrue(visibleState.currentState)

        rule.mainClock.autoAdvance = false
        // Trigger exit
        visibleState.targetState = false
        rule.mainClock.advanceTimeByFrame()

        assertFalse(
            "Content should be disposed immediately upon setting targetState = false",
            isContentPresent,
        )

        // Finish exit
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()

        assertTrue(visibleState.isIdle)
        assertFalse(visibleState.currentState)
        assertFalse(visibleState.targetState)
    }

    @Test
    fun testCapturedAnimatedVisibility_mutableTransitionState_initialTrue() {
        val visibleState = MutableTransitionState(true)
        var isContentPresent = false

        rule.setContent {
            CapturedAnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                DisposableEffect(Unit) {
                    isContentPresent = true
                    onDispose { isContentPresent = false }
                }
                Box(Modifier.size(100.dp))
            }
        }

        assertTrue(
            "Content should initially be in composition when initialState = true",
            isContentPresent,
        )
        assertTrue(visibleState.isIdle)
        assertTrue(visibleState.currentState)
        assertTrue(visibleState.targetState)
    }

    @Test
    fun testCapturedAnimatedVisibility_interruptExitWithEnter() {
        val visibleState = MutableTransitionState(true)
        var isContentPresent = false

        rule.setContent {
            CapturedAnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                DisposableEffect(Unit) {
                    isContentPresent = true
                    onDispose { isContentPresent = false }
                }
                Box(Modifier.size(100.dp))
            }
        }

        assertTrue(isContentPresent)

        rule.mainClock.autoAdvance = false
        // Start exit
        visibleState.targetState = false
        rule.mainClock.advanceTimeByFrame()

        assertFalse("Content should be disposed immediately upon exit", isContentPresent)
        assertFalse(visibleState.isIdle)

        // Advance 80ms into exit
        rule.mainClock.advanceTimeBy(80)
        assertFalse("Content remains uncomposed during exit", isContentPresent)

        // Interrupt exit with enter
        visibleState.targetState = true
        rule.mainClock.advanceTimeByFrame()

        assertTrue(
            "Content must recompose immediately when interrupting exit with enter",
            isContentPresent,
        )
        assertFalse(visibleState.isIdle)

        // Complete enter
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertTrue(visibleState.isIdle)
        assertTrue(visibleState.currentState)
        assertTrue(visibleState.targetState)
        assertTrue(isContentPresent)
    }

    @Test
    fun testCapturedAnimatedVisibility_interruptEnterWithExit() {
        val visibleState = MutableTransitionState(false)
        var isContentPresent = false

        rule.setContent {
            CapturedAnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                DisposableEffect(Unit) {
                    isContentPresent = true
                    onDispose { isContentPresent = false }
                }
                Box(Modifier.size(100.dp))
            }
        }

        assertFalse(isContentPresent)

        rule.mainClock.autoAdvance = false
        // Start enter
        visibleState.targetState = true
        rule.mainClock.advanceTimeByFrame()

        assertTrue("Content is composed during enter", isContentPresent)

        // Advance 80ms into enter
        rule.mainClock.advanceTimeBy(80)
        assertTrue(isContentPresent)
        assertFalse(visibleState.isIdle)

        // Interrupt enter with exit
        visibleState.targetState = false
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        assertFalse(
            "Content must be disposed immediately when interrupting enter with exit",
            isContentPresent,
        )

        // Complete exit
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()

        assertTrue(visibleState.isIdle)
        assertFalse(visibleState.currentState)
        assertFalse(visibleState.targetState)
        assertFalse(isContentPresent)
    }

    @Test
    fun testCapturedAnimatedVisibility_inLookaheadScope() {
        val lookaheadSizes = mutableListOf<IntSize>()
        var visible by mutableStateOf(true)
        var isContentPresent = false

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            measurable.measure(constraints).run {
                                if (isLookingAhead) {
                                    lookaheadSizes.add(IntSize(width, height))
                                }
                                layout(width, height) { place(0, 0) }
                            }
                        }
                    ) {
                        CapturedAnimatedVisibility(
                            visible = visible,
                            enter = expandVertically(tween(160)),
                            exit = shrinkVertically(tween(160)),
                        ) {
                            DisposableEffect(Unit) {
                                isContentPresent = true
                                onDispose { isContentPresent = false }
                            }
                            Box(Modifier.size(200.dp, 100.dp))
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertTrue(visible)
            assertTrue(isContentPresent)
            assertTrue(lookaheadSizes.isNotEmpty())
            lookaheadSizes.forEach { assertEquals(IntSize(200, 100), it) }
            lookaheadSizes.clear()

            rule.mainClock.autoAdvance = false
            visible = false
        }

        rule.mainClock.advanceTimeByFrame()

        assertFalse("Content should be disposed immediately upon exit", isContentPresent)

        rule.runOnIdle {
            assertFalse(visible)
            assertTrue(lookaheadSizes.isNotEmpty())
            lookaheadSizes.forEach { assertEquals(IntSize.Zero, it) }
        }
    }

    @Test
    fun testCapturedAnimatedVisibility_siblingLookaheadInColumn() {
        var visible by mutableStateOf(true)
        var siblingLookaheadPosition = Offset.Unspecified
        var siblingApproachPosition = Offset.Unspecified

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Column {
                        CapturedAnimatedVisibility(
                            visible = visible,
                            enter = expandVertically(tween(160, easing = LinearEasing)),
                            exit = shrinkVertically(tween(160, easing = LinearEasing)),
                        ) {
                            Box(Modifier.size(200.dp, 100.dp))
                        }
                        Box(
                            Modifier.size(200.dp, 50.dp).layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val lookingAhead = isLookingAhead
                                layout(placeable.width, placeable.height) {
                                    if (lookingAhead) {
                                        siblingLookaheadPosition =
                                            lookaheadScopeCoordinates.localLookaheadPositionOf(
                                                coordinates!!
                                            )
                                    } else {
                                        siblingApproachPosition =
                                            lookaheadScopeCoordinates.localPositionOf(
                                                coordinates!!,
                                                Offset.Zero,
                                            )
                                    }
                                    placeable.place(0, 0)
                                }
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(IntOffset(0, 100), siblingLookaheadPosition.round())
            assertEquals(IntOffset(0, 100), siblingApproachPosition.round())
        }

        rule.mainClock.autoAdvance = false
        visible = false

        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Sibling lookahead target position should instantly jump to 0 (destination state)
        assertEquals(IntOffset(0, 0), siblingLookaheadPosition.round())

        // Advance 80ms (50% midpoint of 160ms shrink animation)
        rule.mainClock.advanceTimeBy(80)

        // Sibling approach position animates halfway to 50
        assertEquals(IntOffset(0, 50), siblingApproachPosition.round())

        // Complete exit
        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(IntOffset(0, 0), siblingLookaheadPosition.round())
        assertEquals(IntOffset(0, 0), siblingApproachPosition.round())
    }

    @Test
    fun testCapturedAnimatedVisibility_interruptExitWithNewSize() {
        var visible by mutableStateOf(true)
        var contentHeight by mutableStateOf(100.dp)
        var measuredHeight = 0
        var isContentPresent = false

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        measuredHeight = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                ) {
                    CapturedAnimatedVisibility(
                        visible = visible,
                        enter = expandVertically(tween(160, easing = LinearEasing)),
                        exit = shrinkVertically(tween(160, easing = LinearEasing)),
                    ) {
                        DisposableEffect(Unit) {
                            isContentPresent = true
                            onDispose { isContentPresent = false }
                        }
                        Box(Modifier.size(100.dp, contentHeight))
                    }
                }
            }
        }

        rule.runOnIdle {
            assertTrue(isContentPresent)
            assertEquals(100, measuredHeight)
        }

        rule.mainClock.autoAdvance = false
        // Start exit animation
        visible = false
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertFalse("Content should be disposed immediately upon exit", isContentPresent)

        // Advance 80ms (50% progress of 100 height = 50 height)
        rule.mainClock.advanceTimeBy(80)
        assertEquals(50, measuredHeight)

        // Interrupt exit: set visible = true AND simultaneously change content height to 200.dp
        visible = true
        contentHeight = 200.dp
        rule.mainClock.advanceTimeByFrame()

        assertTrue("Content must recompose immediately with new size", isContentPresent)

        // Complete enter transition to new target size (200.dp)
        rule.mainClock.advanceTimeBy(200)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertTrue(isContentPresent)
        assertEquals(200, measuredHeight)
    }
}
