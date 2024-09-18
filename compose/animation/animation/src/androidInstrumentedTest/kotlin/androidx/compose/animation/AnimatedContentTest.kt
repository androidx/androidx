/*
 * Copyright 2021 The Android Open Source Project
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

@file:OptIn(ExperimentalAnimationApi::class)

package androidx.compose.animation

import android.annotation.SuppressLint
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AnimatedContentTest {
    val rule = createComposeRule()
    // Detect leaks BEFORE and AFTER compose rule work
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    @Test
    fun AnimatedContentSizeTransformTest() {
        val size1 = 40
        val size2 = 200
        val testModifier by mutableStateOf(TestModifier())
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = rememberTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    transitionSpec = {
                        if (true isTransitioningTo false) {
                            fadeIn() togetherWith
                                fadeOut() using
                                SizeTransform { initialSize, targetSize ->
                                    keyframes {
                                        durationMillis = 320
                                        IntSize(targetSize.width, initialSize.height) at
                                            160 using
                                            LinearEasing
                                        targetSize at 320 using LinearEasing
                                    }
                                }
                        } else {
                            fadeIn() togetherWith
                                fadeOut() using
                                SizeTransform { _, _ ->
                                    tween(durationMillis = 80, easing = LinearEasing)
                                }
                        }
                    }
                ) {
                    if (it) {
                        Box(modifier = Modifier.size(size = size1.dp))
                    } else {
                        Box(modifier = Modifier.size(size = size2.dp))
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(40, testModifier.height)
            assertEquals(40, testModifier.width)
            assertTrue(transitionState.targetState)
            transitionState.targetState = false
        }

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                if (playTimeMillis <= 160) {
                    assertEquals(playTimeMillis + 40, testModifier.width)
                    assertEquals(40, testModifier.height)
                } else {
                    assertEquals(200, testModifier.width)
                    assertEquals(playTimeMillis - 120, testModifier.height)
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }

        rule.runOnIdle {
            assertEquals(200, testModifier.width)
            assertEquals(200, testModifier.height)
            transitionState.targetState = true
        }

        // Transition from item2 to item1 in 80ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                if (playTimeMillis <= 80) {
                    assertEquals(200 - playTimeMillis * 2, testModifier.width)
                    assertEquals(200 - playTimeMillis * 2, testModifier.height)
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun AnimatedContentSizeTransformEmptyComposableTest() {
        val size1 = 160
        val testModifier by mutableStateOf(TestModifier())
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = rememberTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    transitionSpec = {
                        EnterTransition.None togetherWith
                            ExitTransition.None using
                            SizeTransform { _, _ ->
                                tween(durationMillis = 160, easing = LinearEasing)
                            }
                    }
                ) {
                    if (it) {
                        Box(modifier = Modifier.size(size = size1.dp))
                    }
                    // Empty composable for it == false
                }
            }
        }
        rule.runOnIdle {
            assertEquals(160, testModifier.height)
            assertEquals(160, testModifier.width)
            assertTrue(transitionState.targetState)
            transitionState.targetState = false
        }

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                assertEquals(160 - playTimeMillis, testModifier.width)
                assertEquals(160 - playTimeMillis, testModifier.height)
            }
            rule.mainClock.advanceTimeByFrame()
        }

        // Now there's only an empty composable
        rule.runOnIdle {
            assertEquals(0, testModifier.width)
            assertEquals(0, testModifier.height)
            transitionState.targetState = true
        }

        // Transition from item2 to item1 in 80ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                assertEquals(playTimeMillis, testModifier.width)
                assertEquals(playTimeMillis, testModifier.height)
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun AnimatedContentContentAlignmentTest() {
        val size1 = IntSize(80, 80)
        val size2 = IntSize(160, 240)
        val testModifier by mutableStateOf(TestModifier())
        var offset1 by mutableStateOf(Offset.Zero)
        var offset2 by mutableStateOf(Offset.Zero)
        var playTimeMillis by mutableStateOf(0)
        val transitionState = MutableTransitionState(true)
        val alignment =
            listOf(
                Alignment.TopStart,
                Alignment.BottomStart,
                Alignment.Center,
                Alignment.BottomEnd,
                Alignment.TopEnd
            )
        var contentAlignment by mutableStateOf(Alignment.TopStart)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = rememberTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    contentAlignment = contentAlignment,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 80)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 80)) using
                            SizeTransform { _, _ ->
                                tween(durationMillis = 80, easing = LinearEasing)
                            }
                    }
                ) {
                    if (it) {
                        Box(
                            modifier =
                                Modifier.onGloballyPositioned { offset1 = it.positionInRoot() }
                                    .size(size1.width.dp, size1.height.dp)
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier.onGloballyPositioned { offset2 = it.positionInRoot() }
                                    .size(size2.width.dp, size2.height.dp)
                        )
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        alignment.forEach {
            rule.runOnIdle {
                assertEquals(80, testModifier.height)
                assertEquals(80, testModifier.width)
                assertTrue(transitionState.targetState)
                contentAlignment = it
            }

            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.runOnIdle { transitionState.targetState = false }

            // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
            // then full height in the next 160ms
            while (transitionState.currentState != transitionState.targetState) {
                rule.runOnIdle {
                    val space = IntSize(testModifier.width, testModifier.height)
                    val position1 = it.align(size1, space, LayoutDirection.Ltr)
                    val position2 = it.align(size2, space, LayoutDirection.Ltr)
                    if (playTimeMillis < 80) {
                        // This gets removed when the animation is finished at 80ms
                        assertEquals(
                            position1,
                            IntOffset(offset1.x.roundToInt(), offset1.y.roundToInt())
                        )
                    }
                    if (playTimeMillis > 0) {
                        assertEquals(
                            position2,
                            IntOffset(offset2.x.roundToInt(), offset2.y.roundToInt())
                        )
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }

            rule.runOnIdle {
                assertEquals(size2.width, testModifier.width)
                assertEquals(size2.height, testModifier.height)
                // After the animation the size should be the same as parent, offset should be 0
                assertEquals(offset2, Offset.Zero)
                transitionState.targetState = true
            }

            // Transition from item2 to item1 in 80ms
            while (transitionState.currentState != transitionState.targetState) {
                rule.runOnIdle {
                    val space = IntSize(testModifier.width, testModifier.height)
                    val position1 = it.align(size1, space, LayoutDirection.Ltr)
                    val position2 = it.align(size2, space, LayoutDirection.Ltr)
                    if (playTimeMillis > 0) {
                        assertEquals(
                            position1,
                            IntOffset(offset1.x.roundToInt(), offset1.y.roundToInt())
                        )
                    }
                    if (playTimeMillis < 80) {
                        assertEquals(
                            position2,
                            IntOffset(offset2.x.roundToInt(), offset2.y.roundToInt())
                        )
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }

            rule.runOnIdle {
                assertEquals(size1.width, testModifier.width)
                assertEquals(size1.height, testModifier.height)
                // After the animation the size should be the same as parent, offset should be 0
                assertEquals(offset1, Offset.Zero)
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun AnimatedContentSlideInAndOutOfContainerTest() {
        val transitionState = MutableTransitionState(true)
        // LinearEasing is required to ensure the animation doesn't reach final values before the
        // duration.
        val animSpec = tween<IntOffset>(200, easing = LinearEasing)
        lateinit var trueTransition: Transition<EnterExitState>
        lateinit var falseTransition: Transition<EnterExitState>
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                @Suppress("UpdateTransitionLabel")
                val rootTransition = rememberTransition(transitionState)
                rootTransition.AnimatedContent(
                    transitionSpec = {
                        if (true isTransitioningTo false) {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animSpec
                            ) togetherWith
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animSpec
                                )
                        } else {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animSpec
                            ) togetherWith
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animSpec
                                )
                        }
                    }
                ) { target ->
                    if (target) {
                        trueTransition = transition
                    } else {
                        falseTransition = transition
                    }
                    Box(Modifier.requiredSize(200.dp).testTag(target.toString()))
                }
            }
        }

        // Kick off the first animation.
        transitionState.targetState = false
        // The initial composition creates the transition…
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("true").assertExists()
        rule.onNodeWithTag("false").assertExists()
        // …but the animation won't actually start until one frame later.
        rule.mainClock.advanceTimeByFrame()
        assertThat(trueTransition.animations).isNotEmpty()
        assertThat(falseTransition.animations).isNotEmpty()

        // Loop to ensure the content is offset correctly at each frame.
        var trueAnim = trueTransition.animations[0]
        var falseAnim = falseTransition.animations[0]
        assertThat(transitionState.currentState).isTrue()
        while (transitionState.currentState) {
            // True is leaving: it should start at 0 and slide out to -200.
            assertThat(trueAnim.value).isEqualTo(IntOffset(-trueTransition.playTimeMillis, 0))
            // False is entering: it should start at 200 and slide in to 0.
            assertThat(falseAnim.value)
                .isEqualTo(IntOffset(200 - falseTransition.playTimeMillis, 0))
            rule.mainClock.advanceTimeByFrame()
        }
        // The animation should remove the newly-hidden node from the composition.
        rule.onNodeWithTag("true").assertDoesNotExist()

        // Kick off the second transition.
        transitionState.targetState = true
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("true").assertExists()
        rule.onNodeWithTag("false").assertExists()
        rule.mainClock.advanceTimeByFrame()
        assertThat(trueTransition.animations).isNotEmpty()

        trueAnim = trueTransition.animations[0]
        falseAnim = falseTransition.animations[0]
        assertThat(transitionState.currentState).isFalse()
        while (!transitionState.currentState) {
            // True is entering, it should start at -200 and slide in to 0.
            assertThat(trueAnim.value).isEqualTo(IntOffset(trueTransition.playTimeMillis - 200, 0))
            // False is leaving, it should start at 0 and slide out to 200.
            assertThat(falseAnim.value).isEqualTo(IntOffset(falseTransition.playTimeMillis, 0))
            rule.mainClock.advanceTimeByFrame()
        }
        rule.onNodeWithTag("false").assertDoesNotExist()
    }

    @Test
    fun AnimatedContentWithContentKey() {
        var targetState by mutableStateOf(1)
        var actualIncomingPosition: Offset? = null
        var actualOutgoingPosition: Offset? = null
        var targetPosition: Offset? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AnimatedContent(
                    targetState,
                    Modifier.onGloballyPositioned { targetPosition = it.positionInRoot() },
                    transitionSpec = {
                        slideInHorizontally { -200 } togetherWith
                            slideOutHorizontally(snap()) { 200 } + fadeOut(tween(200))
                    },
                    contentKey = { it > 3 }
                ) { target ->
                    Box(
                        Modifier.requiredSize(200.dp).onGloballyPositioned {
                            if (target == targetState) {
                                actualIncomingPosition = it.localToRoot(Offset.Zero)
                            } else {
                                actualOutgoingPosition = it.localToRoot(Offset.Zero)
                            }
                        }
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            repeat(3) {
                // Check that no animation happens until the content key changes
                assertEquals(targetPosition, actualIncomingPosition)
                assertNotNull(actualIncomingPosition)
                assertNull(actualOutgoingPosition)
                targetState++
            }
        }

        rule.runOnIdle {
            // Check that animation happened because targetState going from 3 to 4 caused the
            // resulting key to change
            assertEquals(targetPosition, actualIncomingPosition)
            assertNotNull(actualIncomingPosition)
            assertEquals(
                targetPosition!!.copy(x = targetPosition!!.x + 200),
                actualOutgoingPosition
            )
        }
    }

    @Test
    fun LookaheadWithMinMaxIntrinsics() {
        rule.setContent {
            LookaheadScope {
                Scaffold(
                    Modifier.fillMaxSize().testTag(""),
                    topBar = {},
                    floatingActionButton = {}
                ) {
                    Surface() {
                        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
                            val tabRowWidth = constraints.maxWidth
                            val tabMeasurables =
                                subcompose("Tabs") {
                                    repeat(15) { Text(it.toString(), Modifier.width(100.dp)) }
                                }
                            val tabCount = tabMeasurables.size
                            var tabWidth = 0
                            if (tabCount > 0) {
                                tabWidth = (tabRowWidth / tabCount)
                            }
                            val tabRowHeight =
                                tabMeasurables.fold(initial = 0) { max, curr ->
                                    maxOf(curr.maxIntrinsicHeight(tabWidth), max)
                                }

                            val tabPlaceables =
                                tabMeasurables.map {
                                    it.measure(
                                        constraints.copy(
                                            minWidth = tabWidth,
                                            maxWidth = tabWidth,
                                            minHeight = tabRowHeight,
                                            maxHeight = tabRowHeight,
                                        )
                                    )
                                }

                            repeat(tabCount) { index ->
                                var contentWidth =
                                    minOf(
                                            tabMeasurables[index].maxIntrinsicWidth(tabRowHeight),
                                            tabWidth
                                        )
                                        .toDp()
                                contentWidth -= 32.dp
                            }

                            layout(tabRowWidth, tabRowHeight) {
                                tabPlaceables.forEachIndexed { index, placeable ->
                                    placeable.placeRelative(index * tabWidth, 0)
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.Blue)) { Text(text = "test") }
            }
        }
        rule.waitForIdle()
    }

    // This test uses a Scaffold around a TabRow setup to reproduce a scenario where tabs' lookahead
    // measurements will be invalidated right before placement, to ensure the correctness of the
    // impl that lookahead remeasures children right before layout.
    @Test
    fun AnimatedContentWithSubcomposition() {
        var target by mutableStateOf(true)
        rule.setContent {
            AnimatedContent(target) {
                if (it) {
                    Scaffold(
                        Modifier.fillMaxSize().testTag(""),
                        topBar = {},
                        floatingActionButton = {}
                    ) {
                        TabRow(selectedTabIndex = 0) {
                            repeat(15) { Text(it.toString(), Modifier.width(100.dp)) }
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Color.Blue)) { Text(text = "test") }
                } else {
                    Box(Modifier.size(200.dp))
                }
            }
        }

        rule.runOnIdle { target = !target }
        rule.waitForIdle()
        rule.runOnIdle { target = !target }
        rule.waitForIdle()
    }

    @Test
    fun AnimatedContentWithKeysTest() {
        var targetState by mutableStateOf(1)
        val list = mutableListOf<Int>()
        rule.setContent {
            val transition = updateTransition(targetState)
            val holder = rememberSaveableStateHolder()
            transition.AnimatedContent(contentKey = { it > 2 }) {
                if (it <= 2) {
                    holder.SaveableStateProvider(11) {
                        var count by rememberSaveable { mutableStateOf(0) }
                        LaunchedEffect(Unit) { list.add(++count) }
                    }
                }
                Box(Modifier.requiredSize(200.dp))
            }
            LaunchedEffect(Unit) {
                assertFalse(transition.isRunning)
                targetState = 2
                withFrameMillis {
                    assertFalse(transition.isRunning)
                    assertEquals(1, transition.currentState)
                    assertEquals(1, transition.targetState)

                    // This state change should now cause an animation
                    targetState = 3
                }
                withFrameMillis { assertTrue(transition.isRunning) }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(1, list.size)
            assertEquals(1, list[0])
            targetState = 1
        }

        rule.runOnIdle {
            // Check that save worked
            assertEquals(2, list.size)
            assertEquals(1, list[0])
            assertEquals(2, list[1])
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun AnimatedContentWithInterruption() {
        var flag by mutableStateOf(true)
        var rootCoords: LayoutCoordinates? = null
        rule.setContent {
            AnimatedContent(
                targetState = flag,
                modifier = Modifier.onGloballyPositioned { rootCoords = it },
                transitionSpec = {
                    if (targetState) {
                        fadeIn(tween(2000)) togetherWith
                            slideOut(tween(2000)) { fullSize ->
                                IntOffset(0, fullSize.height / 2)
                            } + fadeOut(tween(2000))
                    } else {
                        fadeIn(tween(2000)) togetherWith fadeOut(tween(2000))
                    }
                }
            ) { state ->
                if (state) {
                    Box(
                        modifier =
                            Modifier.onGloballyPositioned {
                                    assertEquals(
                                        Offset.Zero,
                                        rootCoords!!.localPositionOf(it, Offset.Zero)
                                    )
                                }
                                .fillMaxSize()
                                .background(Color.Green)
                    )
                } else {
                    LaunchedEffect(key1 = Unit) {
                        delay(200)
                        assertFalse(flag)
                        assertTrue(transition.isRunning)
                        // Interrupt
                        flag = true
                    }
                    Box(
                        modifier =
                            Modifier.onGloballyPositioned {
                                    assertEquals(
                                        Offset.Zero,
                                        rootCoords!!.localPositionOf(it, Offset.Zero)
                                    )
                                }
                                .fillMaxSize()
                                .background(Color.Red)
                    )
                }
            }
        }
        rule.runOnIdle { flag = false }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testExitHold() {
        var target by mutableStateOf(true)
        var box1Disposed = false
        var box2EnterFinished = false
        rule.setContent {
            AnimatedContent(
                targetState = target,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith
                        fadeOut(tween(5)) + ExitTransition.KeepUntilTransitionsFinished
                }
            ) {
                if (it) {
                    Box(Modifier.size(200.dp)) {
                        DisposableEffect(key1 = Unit) { onDispose { box1Disposed = true } }
                    }
                } else {
                    Box(Modifier.size(200.dp)) {
                        box2EnterFinished =
                            transition.targetState == transition.currentState &&
                                transition.targetState == EnterExitState.Visible
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = !target }

        rule.waitForIdle()
        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            assertFalse(box1Disposed)
            assertFalse(box2EnterFinished)
        }

        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertEquals(box1Disposed, box2EnterFinished)
        }

        assertTrue(box1Disposed)
        assertTrue(box2EnterFinished)
    }

    @Test
    fun testRightEnterExitTransitionIsChosenDuringInterruption() {
        var flag by mutableStateOf(false)
        var fixedPosition: Offset? = null
        var slidePosition: Offset? = null
        rule.setContent {
            AnimatedContent(
                targetState = flag,
                label = "",
                transitionSpec = {
                    if (false isTransitioningTo true) {
                        ContentTransform(
                            targetContentEnter = EnterTransition.None,
                            initialContentExit =
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(durationMillis = 500)
                                ),
                            targetContentZIndex = -1.0f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    } else {
                        ContentTransform(
                            targetContentEnter =
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End
                                ),
                            initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
                            targetContentZIndex = 0.0f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { flag ->
                Spacer(
                    modifier =
                        Modifier.wrapContentSize(Alignment.Center)
                            .size(256.dp)
                            .onGloballyPositioned {
                                if (flag) {
                                    fixedPosition = it.positionInRoot()
                                } else {
                                    slidePosition = it.positionInRoot()
                                }
                            }
                )
            }
        }

        rule.runOnIdle { flag = true }
        rule.waitUntil { fixedPosition != null }
        val initialFixedPosition = fixedPosition
        // Advance 10 frames
        repeat(10) {
            val lastSlidePos = slidePosition
            rule.waitUntil { slidePosition != lastSlidePos }
            assertEquals(initialFixedPosition, fixedPosition)
        }

        // Change the target state amid transition, creating an interruption
        flag = false
        // Advance 10 frames
        repeat(10) {
            val lastSlidePos = slidePosition
            rule.waitUntil { slidePosition != lastSlidePos }
            assertEquals(initialFixedPosition, fixedPosition)
        }
        rule.waitForIdle()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testExitHoldDefersUntilAllFinished() {
        var target by mutableStateOf(true)
        var box1Disposed = false
        var box2EnterFinished = false
        var transitionFinished = false
        rule.setContent {
            val outerTransition = updateTransition(targetState = target)
            transitionFinished = !outerTransition.targetState && !outerTransition.currentState
            outerTransition.AnimatedContent(
                transitionSpec = {
                    fadeIn(tween(160)) togetherWith
                        fadeOut(tween(5)) + ExitTransition.KeepUntilTransitionsFinished using
                        SizeTransform { _, _ -> tween(300) }
                }
            ) {
                if (it) {
                    Box(Modifier.size(200.dp)) {
                        DisposableEffect(key1 = Unit) { onDispose { box1Disposed = true } }
                    }
                } else {
                    Box(Modifier.size(400.dp)) {
                        box2EnterFinished =
                            transition.targetState == transition.currentState &&
                                transition.targetState == EnterExitState.Visible
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = !target }

        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            assertFalse(box1Disposed)
            assertFalse(box2EnterFinished)
        }

        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertTrue(box2EnterFinished)
            // Enter finished, but box1 is only disposed when the transition is completely finished,
            // which includes enter, exit & size change.
            assertFalse(box1Disposed)
            assertFalse(transitionFinished)
        }

        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertTrue(box2EnterFinished)
            // Enter finished, but box1 is only disposed when the transition is completely finished,
            // which includes enter, exit & size change.
            assertEquals(box1Disposed, transitionFinished)
        }

        assertTrue(box1Disposed)
        assertTrue(box2EnterFinished)
    }

    @Test
    fun AnimatedContentLookaheadTest() {
        // Test that AnimatedContent's lookahead size is its target content's lookahead size.
        // Also test that the lookahead placement for content is correct.
        val size1 = 400
        val size2 = 20
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        val testModifier = TestModifier()
        var lookaheadPosition: Offset? = null
        var approachPosition: Offset? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(testModifier) {
                        val transition = rememberTransition(transitionState)
                        playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                        transition.AnimatedContent(
                            transitionSpec = {
                                if (true isTransitioningTo false) {
                                    fadeIn() togetherWith
                                        fadeOut() using
                                        SizeTransform { _, _ ->
                                            tween(durationMillis = 80, easing = LinearEasing)
                                        }
                                } else {
                                    fadeIn() togetherWith
                                        fadeOut() using
                                        SizeTransform { _, _ ->
                                            tween(durationMillis = 80, easing = LinearEasing)
                                        }
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (it) {
                                Box(modifier = Modifier.size(size = size1.dp))
                            } else {
                                Box(
                                    modifier =
                                        Modifier.layout { m, c ->
                                                m.measure(c).run {
                                                    layout(width, height) {
                                                        if (isLookingAhead) {
                                                            with(this@LookaheadScope) {
                                                                lookaheadPosition =
                                                                    lookaheadScopeCoordinates
                                                                        .localLookaheadPositionOf(
                                                                            coordinates!!
                                                                        )
                                                            }
                                                        } else {
                                                            approachPosition =
                                                                lookaheadScopeCoordinates
                                                                    .localPositionOf(
                                                                        coordinates!!,
                                                                        Offset.Zero
                                                                    )
                                                        }
                                                        place(0, 0)
                                                    }
                                                }
                                            }
                                            .size(size = size2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertTrue(transitionState.targetState)
            assertEquals(IntSize(size1, size1), testModifier.lookaheadSize)
            transitionState.targetState = false
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                assertEquals(IntSize(size2, size2), testModifier.lookaheadSize)
                assertNotNull(approachPosition)
                assertNotNull(lookaheadPosition)
                assertOffsetEquals(Offset(0f, 0f), lookaheadPosition!!)
            }
            rule.mainClock.advanceTimeByFrame()
        }
        rule.waitForIdle()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Test
    fun testSizeTransformAlwaysContinuous() {
        var large by mutableStateOf(false)
        var currentWidth: Int = 0
        var currentHeight: Int = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(Modifier.clickable { large = !large }) {
                        AnimatedContent(
                            label = "Test",
                            modifier =
                                Modifier.animateBounds(
                                        this@LookaheadScope,
                                        if (!large) Modifier.size(200.dp) else Modifier.size(300.dp)
                                    )
                                    .layout { m, c ->
                                        m.measure(
                                                c.copy(
                                                    maxWidth = Constraints.Infinity,
                                                    maxHeight = Constraints.Infinity
                                                )
                                            )
                                            .run {
                                                if (!isLookingAhead) {
                                                    currentWidth = width
                                                    currentHeight = height
                                                }
                                                layout(width, height) { place(0, 0) }
                                            }
                                    }
                                    .background(Color.Gray),
                            targetState = large,
                            contentKey = { true },
                            transitionSpec = { fadeIn().togetherWith(fadeOut()) }
                        ) {
                            Box(Modifier.background(Color.Black).size(200.dp, 100.dp))
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        large = true

        assertEquals(200, currentWidth)
        assertEquals(200, currentHeight)
        // Expect size to grow
        var lastWidth: Int = 200
        var lastHeight: Int = 200

        fun doFrame() {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertTrue(currentWidth >= lastWidth)
            assertTrue(currentHeight >= lastHeight)
            lastWidth = currentWidth
            lastHeight = currentHeight
        }

        repeat(5) { doFrame() }

        while (currentWidth != 300 || currentHeight != 300) {
            doFrame()
        }
    }

    @Test
    fun testTargetChangeLookaheadPlacement() {
        var lookaheadPosition1: Offset? = null
        var lookaheadPosition2: Offset? = null
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        rule.setContent {
            LookaheadScope {
                val transition = rememberTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    contentAlignment = Alignment.Center,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using null }
                ) {
                    if (it) {
                        Box(
                            Modifier.layout { measurable, constraints ->
                                    measurable.measure(constraints).run {
                                        layout(width, height) {
                                            if (isLookingAhead) {
                                                lookaheadPosition1 =
                                                    lookaheadScopeCoordinates
                                                        .localLookaheadPositionOf(coordinates!!)
                                            }
                                        }
                                    }
                                }
                                .fillMaxSize()
                                .background(Color.Blue)
                        )
                    } else {
                        Box(
                            Modifier.layout { measurable, constraints ->
                                    measurable.measure(constraints).run {
                                        layout(width, height) {
                                            if (isLookingAhead) {
                                                lookaheadPosition2 =
                                                    lookaheadScopeCoordinates
                                                        .localLookaheadPositionOf(coordinates!!)
                                            }
                                        }
                                    }
                                }
                                .size(100.dp)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            assertTrue(transitionState.targetState)
            assertTrue(transitionState.currentState)
            transitionState.targetState = false
        }
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            assertNotNull(lookaheadPosition1)
            assertOffsetEquals(Offset(0f, 0f), lookaheadPosition1!!)
            transitionState.targetState = false
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        repeat(3) {
            assertNotEquals(transitionState.currentState, transitionState.targetState)
            rule.runOnIdle {
                assertNotNull(lookaheadPosition2)
                assertOffsetEquals(Offset(0f, 0f), lookaheadPosition2!!)
            }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        // Check that the lookahead position for the outgoing content changed
        assertNotEquals(0f, lookaheadPosition1!!.x)
        assertNotEquals(0f, lookaheadPosition1!!.y)
        // Interruption during animation
        transitionState.targetState = true
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        rule.runOnIdle {
            assertNotNull(lookaheadPosition1)
            // Check that after the target state change, the new incoming content has
            // a 0, 0 lookahead offset.
            assertOffsetEquals(Offset(0f, 0f), lookaheadPosition1!!)
        }
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.00001f)
        assertEquals(expected.y, actual.y, 0.00001f)
    }

    private val Transition<*>.playTimeMillis
        get() = (playTimeNanos / 1_000_000L).toInt()
}
