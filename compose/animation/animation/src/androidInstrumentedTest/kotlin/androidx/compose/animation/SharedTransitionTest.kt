/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class,
)

package androidx.compose.animation

import android.annotation.SuppressLint
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.animatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.lookaheadScopeCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.lang.Thread.sleep
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SharedTransitionTest {
    val rule = createComposeRule()

    // Detect leaks BEFORE and AFTER compose rule work
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    @Test
    fun transitionInterruption() {
        var visible by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(500, easing = LinearEasing) }
        val positions = mutableListOf(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero)
        val sizes = mutableListOf(IntSize(-1, -1), IntSize(-1, -1), IntSize.Zero, IntSize.Zero)
        var transitionScope: SharedTransitionScope? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    transitionScope = this@SharedTransitionLayout as SharedTransitionScopeImpl
                    AnimatedVisibility(visible = visible) {
                        Column {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "cat"),
                                        this@AnimatedVisibility,
                                        boundsTransform = boundsTransform,
                                    )
                                    .onGloballyPositioned {
                                        positions[0] =
                                            lookaheadRoot.localPositionOf(it, Offset.Zero)
                                        sizes[0] = it.size
                                    }
                                    .size(200.dp)
                            )
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "dog"),
                                        this@AnimatedVisibility,
                                        boundsTransform = boundsTransform,
                                    )
                                    .onGloballyPositioned {
                                        positions[1] =
                                            lookaheadRoot.localPositionOf(it, Offset.Zero)
                                        sizes[1] = it.size
                                    }
                                    .size(50.dp)
                            )
                        }
                    }
                    AnimatedVisibility(visible = !visible) {
                        Row {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "dog"),
                                        this@AnimatedVisibility,
                                        boundsTransform = boundsTransform,
                                    )
                                    .onGloballyPositioned {
                                        positions[2] =
                                            lookaheadRoot.localPositionOf(it, Offset.Zero)
                                        sizes[2] = it.size
                                    }
                                    .size(50.dp)
                            )
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "cat"),
                                        this@AnimatedVisibility,
                                        boundsTransform = boundsTransform,
                                    )
                                    .onGloballyPositioned {
                                        positions[3] =
                                            lookaheadRoot.localPositionOf(it, Offset.Zero)
                                        sizes[3] = it.size
                                    }
                                    .size(200.dp)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(transitionScope!!.isTransitionActive)
            assertEquals(IntSize(200, 200), sizes[0])
            assertEquals(IntSize(50, 50), sizes[1])
            assertEquals(Offset(0f, 0f), positions[0])
            assertEquals(Offset(0f, 200f), positions[1])
        }

        rule.mainClock.autoAdvance = false
        visible = false

        repeat(20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val offsetTolerance = Offset(5f, 5f)
        val tolerance = IntSize(5, 5)

        assertEquals(positions[0], positions[3], offsetTolerance)
        assertEquals(positions[1], positions[2], offsetTolerance)

        assertEquals(sizes[0], sizes[3], tolerance)
        assertEquals(sizes[1], sizes[2], tolerance)

        assertTrue(transitionScope!!.isTransitionActive)

        // Interrupt
        visible = true
        val lastSizes = mutableListOf<IntSize>().also { it.addAll(sizes) }
        val lastPositions = mutableListOf<Offset>().also { it.addAll(positions) }

        while (transitionScope?.isTransitionActive != false) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()

            // Shared bounds are in sync with each other's bounds
            assertEquals(positions[0], positions[3], offsetTolerance)
            assertEquals(positions[1], positions[2], offsetTolerance)

            assertEquals(sizes[0], sizes[3], tolerance)
            assertEquals(sizes[1], sizes[2], tolerance)

            // Expect size[0] to grow and size[1] to shrink from the point of interruption
            // And that size always changes continuously
            assertTrue(sizes[0].width >= lastSizes[0].width)
            assertTrue(sizes[0].height >= lastSizes[0].height)
            assertEquals(sizes[0], lastSizes[0], IntSize(10, 10))

            assertTrue(sizes[1].width <= lastSizes[1].width)
            assertTrue(sizes[1].height <= lastSizes[1].height)
            assertEquals(sizes[1], lastSizes[1], IntSize(10, 10))

            // Expect positions to change gradually.
            assertEquals(positions[0], lastPositions[0], Offset(20f, 20f))
            assertEquals(positions[1], lastPositions[1], Offset(40f, 40f))
            assertEquals(0f, positions[0].y)
            assertEquals(0f, positions[1].x)

            lastSizes.clear()
            lastSizes.addAll(sizes)
            lastPositions.clear()
            lastPositions.addAll(positions)
        }

        // Animation finished
        assertEquals(IntSize(200, 200), sizes[0])
        assertEquals(IntSize(50, 50), sizes[1])
        assertEquals(Offset(0f, 0f), positions[0])
        assertEquals(Offset(0f, 200f), positions[1])
    }

    @Test
    fun transitionInterruptionSelfManagedVisibility() {
        var visible by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(500, easing = LinearEasing) }
        val positions = mutableListOf(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero)
        val sizes = mutableListOf(IntSize(-1, -1), IntSize(-1, -1), IntSize.Zero, IntSize.Zero)
        var transitionScope: SharedTransitionScopeImpl? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    transitionScope = this@SharedTransitionLayout as SharedTransitionScopeImpl
                    Column {
                        Box(
                            Modifier.sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "cat"),
                                    visible = visible,
                                    boundsTransform = boundsTransform,
                                )
                                .onGloballyPositioned {
                                    positions[0] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[0] = it.size
                                }
                                .size(200.dp)
                        )
                        Box(
                            Modifier.sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "dog"),
                                    visible = visible,
                                    boundsTransform = boundsTransform,
                                )
                                .onGloballyPositioned {
                                    positions[1] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[1] = it.size
                                }
                                .size(50.dp)
                        )
                    }
                    Row {
                        Box(
                            Modifier.sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "dog"),
                                    visible = !visible,
                                    boundsTransform = boundsTransform,
                                )
                                .onGloballyPositioned {
                                    positions[2] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[2] = it.size
                                }
                                .size(50.dp)
                        )
                        Box(
                            Modifier.sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "cat"),
                                    visible = !visible,
                                    boundsTransform = boundsTransform,
                                )
                                .onGloballyPositioned {
                                    positions[3] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[3] = it.size
                                }
                                .size(200.dp)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(transitionScope!!.isTransitionActive)
            assertEquals(IntSize(200, 200), sizes[0])
            assertEquals(IntSize(50, 50), sizes[1])
            assertEquals(Offset(0f, 0f), positions[0])
            assertEquals(Offset(0f, 200f), positions[1])
        }

        rule.mainClock.autoAdvance = false
        visible = false

        repeat(20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val offsetTolerance = Offset(5f, 5f)
        val tolerance = IntSize(5, 5)

        assertEquals(positions[0], positions[3], offsetTolerance)
        assertEquals(positions[1], positions[2], offsetTolerance)

        assertEquals(sizes[0], sizes[3], tolerance)
        assertEquals(sizes[1], sizes[2], tolerance)

        assertTrue(transitionScope!!.isTransitionActive)

        // Interrupt
        visible = true
        val lastSizes = mutableListOf<IntSize>().also { it.addAll(sizes) }
        val lastPositions = mutableListOf<Offset>().also { it.addAll(positions) }

        while (transitionScope?.isTransitionActive != false) {

            // Shared bounds are in sync with each other's bounds
            assertEquals(positions[0], positions[3], offsetTolerance)
            assertEquals(positions[1], positions[2], offsetTolerance)

            assertEquals(sizes[0], sizes[3], tolerance)
            assertEquals(sizes[1], sizes[2], tolerance)

            // Expect size[0] to grow and size[1] to shrink from the point of interruption
            // And that size always changes continuously
            assertTrue(sizes[0].width >= lastSizes[0].width)
            assertTrue(sizes[0].height >= lastSizes[0].height)
            assertEquals(sizes[0], lastSizes[0], IntSize(10, 10))

            assertTrue(sizes[1].width <= lastSizes[1].width)
            assertTrue(sizes[1].height <= lastSizes[1].height)
            assertEquals(sizes[1], lastSizes[1], IntSize(10, 10))

            // Expect positions to change gradually.
            assertEquals(positions[0], lastPositions[0], Offset(20f, 20f))
            assertEquals(positions[1], lastPositions[1], Offset(40f, 40f))
            assertEquals(0f, positions[0].y)
            assertEquals(0f, positions[1].x)

            lastSizes.clear()
            lastSizes.addAll(sizes)
            lastPositions.clear()
            lastPositions.addAll(positions)

            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Animation finished
        assertEquals(IntSize(200, 200), sizes[0])
        assertEquals(IntSize(50, 50), sizes[1])
        assertEquals(Offset(0f, 0f), positions[0])
        assertEquals(Offset(0f, 200f), positions[1])
    }

    @Test
    fun testKeyMatch() {
        val key1 = Any()
        val set = mutableSetOf<SharedTransitionScope.SharedContentState>()
        var showRow by mutableStateOf(false)
        var visible by mutableStateOf(true)
        rule.setContent {
            SharedTransitionLayout {
                this@SharedTransitionLayout as SharedTransitionScopeImpl
                Column {
                    Box(
                        Modifier.sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = key1).also { set.add(it) },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier.sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = 2).also { set.add(it) },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier.sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = "cat").also { set.add(it) },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier.sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = Unit).also { set.add(it) },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                }
                if (showRow) {
                    Row {
                        Box(
                            Modifier.sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = key1).also { set.add(it) },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier.sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = 2).also { set.add(it) },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier.sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "cat").also { set.add(it) },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier.sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = Unit).also { set.add(it) },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(4, set.size)
        set.forEach { assertFalse(it.isMatchFound) }

        // Show row to add matched shared elements into composition
        showRow = true
        rule.runOnIdle {
            assertEquals(8, set.size)
            set.forEach { assertTrue(it.isMatchFound) }
        }
        visible = false
        rule.runOnIdle { set.forEach { assertTrue(it.isMatchFound) } }
        set.clear()
        showRow = false
        rule.runOnIdle { assertEquals(4, set.size) }
        rule.runOnIdle { set.forEach { assertFalse(it.isMatchFound) } }
    }

    @Test
    fun testMatchFoundUpdatedPromptly() {
        val key1 = Any()
        val set = mutableSetOf<SharedTransitionScope.SharedContentState>()
        val seekableTransition = SeekableTransitionState(1)
        var target by mutableStateOf(1)
        var firstFrame = true
        rule.setContent {
            LaunchedEffect(target) {
                if (seekableTransition.currentState != target) {
                    seekableTransition.animateTo(target)
                }
            }
            val transition = rememberTransition(transitionState = seekableTransition)
            SharedTransitionLayout {
                val state1 = rememberSharedContentState(key = key1)
                val state2 = rememberSharedContentState(key = key1)
                transition.AnimatedContent {
                    when (it) {
                        1 ->
                            Box(Modifier.sharedElement(state1, this).size(200.dp)) {
                                DisposableEffect(key1 = Unit) {
                                    set.add(state1)
                                    onDispose { set.remove(state1) }
                                }
                            }
                        2 ->
                            Box(Modifier.sharedElement(state2, this).size(600.dp)) {
                                DisposableEffect(key1 = Unit) {
                                    set.add(state2)
                                    onDispose { set.remove(state2) }
                                }
                            }
                        else ->
                            Box(Modifier.size(200.dp)) {
                                if (firstFrame) {
                                    firstFrame = false
                                }
                            }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, set.size)
        set.forEach { assertFalse(it.isMatchFound) }

        // Show row to add matched shared elements into composition
        rule.runOnIdle { target = 2 }
        rule.waitUntil { set.size == 2 }

        repeat(5) { rule.mainClock.advanceTimeByFrame() }
        assertEquals(2, set.size)

        // Now we expect two shared elements to be matched
        set.forEach { assertTrue(it.isMatchFound) }
        target = 3

        rule.waitUntil { !firstFrame }
        set.forEach {
            assertEquals(2, set.size)
            assertFalse(it.isMatchFound)
        }
        rule.mainClock.advanceTimeBy(50000L)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testBothContentShowing() {
        var visible by mutableStateOf(false)
        val tween = tween<Float>(100, easing = LinearEasing)
        var transitionScope: SharedTransitionScope? = null
        var exitTransition: Transition<*>? = null
        var enterTransition: Transition<*>? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        enterTransition = transition
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    fadeIn(tween),
                                    fadeOut(tween),
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Red)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        exitTransition = transition
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    fadeIn(tween),
                                    fadeOut(tween),
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Blue)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.mainClock.autoAdvance = false
        visible = true

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            val playTime = exitTransition!!.playTimeNanos / 1000_000L
            val fraction = (playTime.toFloat() / 100f).coerceIn(0f, 1f)

            val enterPlayTime = enterTransition!!.playTimeNanos / 1000_000L
            val enterFraction = (enterPlayTime.toFloat() / 100f).coerceIn(0f, 1f)

            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    assertPixels {
                        if (it.x < width / 2) {
                            Color.Red.copy(alpha = enterFraction).compositeOver(Color.White)
                        } else if (it.x > width / 2) {
                            Color.Blue.copy(alpha = 1f - fraction).compositeOver(Color.White)
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testObserverScopeClearedAfterDisposing() {
        var visible by mutableStateOf(false)
        val tween = tween<Float>(100, easing = LinearEasing)
        var transitionScope: SharedTransitionScopeImpl? = null
        var shouldDispose by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                if (!shouldDispose) {
                    SharedTransitionLayout(
                        Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                    ) {
                        transitionScope = this as SharedTransitionScopeImpl
                        AnimatedVisibility(
                            visible = visible,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this@AnimatedVisibility,
                                        fadeIn(tween),
                                        fadeOut(tween),
                                    )
                                    .fillMaxSize()
                            ) {
                                Box(
                                    Modifier.fillMaxHeight()
                                        .fillMaxWidth(0.5f)
                                        .background(Color.Red)
                                        .align(Alignment.CenterStart)
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = !visible,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this@AnimatedVisibility,
                                        fadeIn(tween),
                                        fadeOut(tween),
                                    )
                                    .fillMaxSize()
                            ) {
                                Box(
                                    Modifier.fillMaxHeight()
                                        .fillMaxWidth(0.5f)
                                        .background(Color.Blue)
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        visible = true
        rule.waitForIdle()

        shouldDispose = true
        rule.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = 26)
    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testOnlyVisibleContentShowingInSharedElement() {
        var visible by mutableStateOf(false)
        val tween = tween<Float>(100, easing = LinearEasing)
        var transitionScope: SharedTransitionScope? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween),
                        exit = fadeOut(tween),
                    ) {
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Red)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Blue)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run {
                assertPixels {
                    if (it.x < width / 2 - 2) {
                        Color.White
                    } else if (it.x > width / 2 + 2) {
                        Color.Blue
                    } else null
                }
            }
        }

        rule.mainClock.autoAdvance = false
        visible = true

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    assertPixels {
                        if (it.x < width / 2 - 2) {
                            Color.Red
                        } else if (it.x > width / 2 + 2) {
                            Color.White
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testOverlayClip() {
        // Set a clip shape on the shared element that is change both size and position, and check
        // that the shape is being updated per frame.
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var size: IntSize? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    boundsTransform =
                                        BoundsTransform { _, _ ->
                                            tween(100, easing = LinearEasing)
                                        },
                                )
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.fillMaxSize(0.5f)
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition = OverlayClip(CircleShape),
                                    boundsTransform =
                                        BoundsTransform { _, _ ->
                                            tween(100, easing = LinearEasing)
                                        },
                                )
                                .onGloballyPositioned { size = it.size }
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.White } }
        }

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Set aside 5 pixel width for anti-aliasing
        val widthTolerance = 4
        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    // Check clipping
                    assertPixels {
                        val distanceToCenter =
                            sqrt(
                                (it.x - width / 2) * (it.x - width / 2).toFloat() +
                                    (it.y - height / 2) * (it.y - height / 2)
                            )
                        if (
                            it.x < width / 2 && distanceToCenter < size!!.width / 2 - widthTolerance
                        ) {
                            Color.Blue
                        } else if (distanceToCenter > size!!.width / 2 + widthTolerance) {
                            Color.White
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testOverlayClipInheritedByChildren() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition =
                                        OverlayClip(clipShape = CircleShape),
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .align(Alignment.CenterStart)
                                    .background(Color.Blue)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.White } }
        }

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Set aside 5 pixel width for anti-aliasing
        val widthTolerance = 5
        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    // Check clipping
                    assertPixels {
                        val distanceToCenter =
                            sqrt(
                                (it.x - width / 2) * (it.x - width / 2).toFloat() +
                                    (it.y - height / 2) * (it.y - height / 2)
                            )
                        if (it.x < width / 2 && distanceToCenter < width / 2 - widthTolerance) {
                            Color.Blue
                        } else if (
                            it.x > width / 2 + 5 || distanceToCenter > width / 2 + widthTolerance
                        ) {
                            Color.White
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testBoundsTransform() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var parentSize: IntSize? = null
        var parentPosition: Offset? = null
        var childSize: IntSize? = null
        var childPosition: Offset? = null
        var exitTransition: Transition<*>? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.requiredSize(100.dp).background(Color.White)) {
                    transitionScope = this@SharedTransitionLayout as SharedTransitionScopeImpl
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(0.5f).offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        exitTransition = this.transition
                        Box(
                            Modifier.offset(20.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    fadeIn(),
                                    fadeOut(),
                                    boundsTransform =
                                        BoundsTransform { _, _ ->
                                            tween(100, easing = LinearEasing)
                                        },
                                    resizeMode = RemeasureToBounds,
                                )
                                .onGloballyPositioned {
                                    parentPosition = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    parentSize = it.size
                                }
                        ) {
                            Box(
                                Modifier.offset(-20.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                        boundsTransform = { initialBounds, targetBounds ->
                                            assertEquals(initialBounds, targetBounds)
                                            spring()
                                        },
                                    )
                                    .onGloballyPositioned {
                                        childPosition =
                                            lookaheadRoot.localPositionOf(it, Offset.Zero)
                                        childSize = it.size
                                    }
                                    .fillMaxSize()
                                    .background(Color.Blue)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            // Expect size (100, 100) -> (50, 50), position -> (0, 0) -> (45, 45)
            val fraction = ((exitTransition!!.playTimeNanos / 1000_000L) / 100f).coerceIn(0f, 1f)
            val expectedSize =
                (50 * fraction + 100 * (1 - fraction)).roundToInt().let { IntSize(it, it) }
            val expectedPosition = Offset(45f * fraction, 25f * fraction)
            assertEquals(expectedSize, parentSize!!, IntSize(3, 3))
            assertEquals(expectedPosition, parentPosition!!, Offset(3f, 3f))

            // Child is expected to hold in place throughout the transition
            assertEquals(IntSize(50, 50), childSize!!)
            assertEquals(Offset(25f, 25f), childPosition!!)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testPlaceHolderSize() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var parent1Size: IntSize? = null
        var parent2Size: IntSize? = null
        var expectedSize by mutableStateOf(IntSize.Zero)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.requiredSize(100.dp).background(Color.White)) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.fillMaxSize().wrapContentSize().onGloballyPositioned {
                                parent1Size = it.size
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize(0.5f)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.onGloballyPositioned { parent2Size = it.size }
                                .offset(-20.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    placeHolderSize =
                                        SharedTransitionScope.PlaceHolderSize { _, _ ->
                                            expectedSize
                                        },
                                )
                                .fillMaxSize()
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            // Expect parent1 to stay at contentSize and parent2 to change size
            assertEquals(IntSize(50, 50), parent1Size!!)
            assertEquals(expectedSize, parent2Size!!)

            expectedSize = IntSize(Random.nextInt(0, 100), Random.nextInt(0, 100))
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testRenderInOverlayEqualsFalse() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var exit: Transition<*>? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.testTag("scope").requiredSize(100.dp).background(Color.White)
                ) {
                    transitionScope = this

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                )
                                .background(Color.Red)
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100, easing = LinearEasing)),
                        exit = ExitTransition.None,
                    ) {
                        exit = transition
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    renderInOverlayDuringTransition = false,
                                )
                                .background(Color.Blue)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.Red } }
        }

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            val fraction = ((exit!!.playTimeNanos / 1000_000L) / 100f).coerceIn(0f, 1f)
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    assertPixels { Color.Blue.copy(alpha = fraction).compositeOver(Color.White) }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testZIndexInOverlay() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var greenZIndex by mutableStateOf(0f)
        var redZIndex by mutableStateOf(0f)
        var blueZIndex by mutableStateOf(0f)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.testTag("scope").requiredSize(100.dp).background(Color.White)
                ) {
                    transitionScope = this

                    Box(
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = greenZIndex)
                            .background(Color.Green)
                            .fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(100, easing = LinearEasing)),
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                    zIndexInOverlay = redZIndex,
                                )
                                .background(Color.Red)
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100, easing = LinearEasing)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                    zIndexInOverlay = blueZIndex,
                                )
                                .background(Color.Blue)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.Red } }
        }

        rule.mainClock.autoAdvance = false
        visible = false
        greenZIndex = 0f
        redZIndex = 0f
        blueZIndex = 1f
        var expectedTopColor = Color.Blue
        var i = 0

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run { assertPixels { expectedTopColor } }
            }

            greenZIndex = i.toFloat()
            redZIndex = i.toFloat()
            blueZIndex = i.toFloat()
            when (i) {
                0 -> {
                    redZIndex++
                    expectedTopColor = Color.Red
                }
                1 -> {
                    greenZIndex++
                    expectedTopColor = Color.Green
                }
                2 -> {
                    blueZIndex++
                    expectedTopColor = Color.Blue
                }
            }
            i = (i + 1) % 3
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testSkipToLookahead() {
        // Set a clip shape on the shared element that is change both size and position, and check
        // that the shape is being updated per frame.
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var size: IntSize? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.requiredSize(100.dp).testTag("scope").background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    boundsTransform =
                                        BoundsTransform { _, _ ->
                                            tween(100, easing = LinearEasing)
                                        },
                                )
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.padding(25.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition = OverlayClip(CircleShape),
                                    boundsTransform =
                                        BoundsTransform { _, _ ->
                                            tween(100, easing = LinearEasing)
                                        },
                                )
                                .skipToLookaheadSize()
                                .onGloballyPositioned { size = it.size }
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        assertNull(size)

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            // Check that child size has skipped to lookahead size
            assertEquals(IntSize(50, 50), size)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testRenderInOverlay() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.testTag("scope").requiredSize(100.dp).background(Color.White)
                ) {
                    transitionScope = this
                    Box(
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                            .background(Color.Green)
                            .fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(Modifier.fillMaxSize().wrapContentSize(align = Alignment.BottomEnd)) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize(0.5f)
                                    .background(Color.Red)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(0.5f)
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)
        // tolerance due to AA
        val tolerance = 2
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run {
                assertPixels {
                    if (it.x > width / 2 + tolerance && it.y > height / 2 + tolerance) {
                        Color.Red
                    } else if (it.x < width / 2 - tolerance || it.y < height / 2 - tolerance) {
                        Color.Green
                    } else null
                }
            }
        }

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    // Check clipping
                    assertPixels { Color.Green }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testSharedContentStateClipPathInOverlay() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var parentSharedContentState: SharedTransitionScope.SharedContentState? = null
        var childSharedContentState: SharedTransitionScope.SharedContentState? = null

        var clippedParentSharedContentState: SharedTransitionScope.SharedContentState? = null
        var clippedChildSharedContentState: SharedTransitionScope.SharedContentState? = null
        val predefinedPath = Path().apply { addRect(Rect(5f, 5f, 6f, 6f)) }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.requiredSize(100.dp).background(Color.White)) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        parentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            childSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(0.5f).offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.offset(20.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        clippedParentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition =
                                        object : SharedTransitionScope.OverlayClip {
                                            override fun getClipPath(
                                                sharedContentState:
                                                    SharedTransitionScope.SharedContentState,
                                                bounds: Rect,
                                                layoutDirection: LayoutDirection,
                                                density: Density,
                                            ): Path? {
                                                return predefinedPath
                                            }
                                        },
                                )
                        ) {
                            Box(
                                Modifier.offset(-20.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            clippedChildSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Pulse another frame to ensure the rendering has happened
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            // Check that the custom clip is picked up by both parent and child shared states
            assertEquals(predefinedPath, clippedParentSharedContentState!!.clipPathInOverlay)
            assertEquals(predefinedPath, clippedChildSharedContentState!!.clipPathInOverlay)
            assertEquals(null, parentSharedContentState!!.clipPathInOverlay)
            assertEquals(null, childSharedContentState!!.clipPathInOverlay)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testParentSharedContentState() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(true)
        var parentSharedContentState: SharedTransitionScope.SharedContentState? = null
        var childSharedContentState: SharedTransitionScope.SharedContentState? = null

        var clippedParentSharedContentState: SharedTransitionScope.SharedContentState? = null
        var clippedChildSharedContentState: SharedTransitionScope.SharedContentState? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.requiredSize(100.dp).background(Color.White)) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        parentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            childSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(0.5f).offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None,
                    ) {
                        Box(
                            Modifier.offset(20.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        clippedParentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                        ) {
                            Box(
                                Modifier.offset(-20.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            clippedChildSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertFalse(transitionScope!!.isTransitionActive)

        rule.mainClock.autoAdvance = false
        visible = false

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Pulse another frame to ensure the rendering has happened
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        // Now shared bounds transition started
        while (transitionScope?.isTransitionActive != false) {
            // Check that the custom clip is picked up by both parent and child shared states
            assertNotNull(parentSharedContentState)
            assertEquals(
                parentSharedContentState,
                childSharedContentState!!.parentSharedContentState,
            )
            assertNotNull(clippedParentSharedContentState)
            assertEquals(
                clippedParentSharedContentState,
                clippedChildSharedContentState!!.parentSharedContentState,
            )

            assertTrue(parentSharedContentState!!.isMatchFound)
            assertTrue(childSharedContentState!!.isMatchFound)
            assertTrue(clippedParentSharedContentState!!.isMatchFound)
            assertTrue(clippedChildSharedContentState!!.isMatchFound)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testScaleTransition() {
        var isSquare by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(100, easing = LinearEasing) }
        var transition: Transition<*>? = null

        rule.setContent {
            SharedTransitionLayout(Modifier.testTag("scope").background(Color.White)) {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    AnimatedContent(
                        isSquare,
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { isSquare ->
                        if (this.transition.targetState == EnterExitState.Visible) {
                            transition = this.transition
                        }
                        if (isSquare) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        enter = EnterTransition.None,
                                        exit = ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.Fit),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(80.dp)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        enter = EnterTransition.None,
                                        exit = ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.Fit),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(40.dp, 160.dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.Red } }
        }
        rule.mainClock.autoAdvance = false
        isSquare = false

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        // Anti alias tolerance
        val tolerance = 2

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 80f + fraction * 40f
                val expectedBoundsHeight = (1f - fraction) * 80f + fraction * 160f
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { (x, y) ->
                            val greyMin = width / 2 - height / 8
                            val greyMax = width / 2 + height / 8
                            if (x > greyMin + tolerance && x < greyMax - tolerance) {
                                Color.Gray
                            } else if (x < greyMin - tolerance || x > greyMax + tolerance) {
                                // This should be either red or white depending on height
                                if (
                                    y > height / 2f - width / 2f + tolerance &&
                                        y < height / 2f + width / 2f - tolerance
                                ) {
                                    Color.Red
                                } else null
                            } else null
                        }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testScaleTransitionFillHeightAndFillWidth() {
        var isSquare by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(100, easing = LinearEasing) }
        var transition: Transition<*>? = null

        rule.setContent {
            SharedTransitionLayout(Modifier.testTag("scope").background(Color.White)) {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    AnimatedContent(
                        isSquare,
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { isSquare ->
                        if (this.transition.targetState == EnterExitState.Visible) {
                            transition = this.transition
                        }
                        if (isSquare) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.FillHeight),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(80.dp)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.FillWidth),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(40.dp, 160.dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.Red } }
        }
        rule.mainClock.autoAdvance = false
        isSquare = false

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 80f + fraction * 40f
                val expectedBoundsHeight = (1f - fraction) * 80f + fraction * 160f
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { Color.Gray }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }

        rule.waitForIdle()

        isSquare = true

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 40f + fraction * 80f
                val expectedBoundsHeight = (1f - fraction) * 160f + fraction * 80f
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { Color.Red }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testScaleTransitionAlignment() {
        var isSquare by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(100, easing = LinearEasing) }
        var transition: Transition<*>? = null

        rule.setContent {
            SharedTransitionLayout(Modifier.testTag("scope").background(Color.White)) {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    AnimatedContent(
                        isSquare,
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { isSquare ->
                        if (this.transition.targetState == EnterExitState.Visible) {
                            transition = this.transition
                        }
                        if (isSquare) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode =
                                            scaleToBounds(ContentScale.Fit, Alignment.TopStart),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(80.dp)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode =
                                            scaleToBounds(ContentScale.Fit, Alignment.BottomStart),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(40.dp, 160.dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run { assertPixels { Color.Red } }
        }
        rule.mainClock.autoAdvance = false
        isSquare = false

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        // Anti alias tolerance
        val tolerance = 2

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 80f + fraction * 40f
                val expectedBoundsHeight = (1f - fraction) * 80f + fraction * 160f
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { (x, y) ->
                            val greyMax = height / 4
                            if (x >= 0 && x < greyMax - tolerance) {
                                Color.Gray
                            } else if (x > greyMax + tolerance) {
                                // This should be either red or white depending on height
                                if (y > 0 + tolerance && y < width - tolerance) {
                                    Color.Red
                                } else if (y > width + tolerance) {
                                    Color.White
                                } else null
                            } else null
                        }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testScaleTransitionCrop() {
        var isSquare by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(100, easing = LinearEasing) }
        var transition: Transition<*>? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier.testTag("scope").background(Color.White).padding(20.dp)
                ) {
                    AnimatedContent(
                        isSquare,
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { isSquare ->
                        if (this.transition.targetState == EnterExitState.Visible) {
                            transition = this.transition
                        }
                        if (isSquare) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.Crop),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(80.dp)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "test"),
                                        this,
                                        EnterTransition.None,
                                        ExitTransition.None,
                                        resizeMode = scaleToBounds(ContentScale.FillWidth),
                                        boundsTransform = boundsTransform,
                                        placeHolderSize = animatedSize,
                                    )
                                    .size(40.dp, 160.dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("scope").run {
            assertExists("Error: Node doesn't exist")
            captureToImage().run {
                assertPixels { (x, y) ->
                    if (x > 20 && x < width - 20 && y > 20 && y < height - 20) {
                        Color.Red
                    } else if (x < 20 || x > width - 20 || y < 20 && y > height - 20) {
                        Color.White
                    } else null
                }
            }
        }
        rule.mainClock.autoAdvance = false
        isSquare = false

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 80f + fraction * 40f + 40
                val expectedBoundsHeight = (1f - fraction) * 80f + fraction * 160f + 40
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { (x, y) ->
                            if (x > 20 && x < width - 20) {
                                // Expect gray to draw in the padding area since it does not crop
                                val verticalDistTimes2 = abs(2 * y - height)
                                if (verticalDistTimes2 > (width - 40) * 4) {
                                    Color.White
                                } else if (verticalDistTimes2 < (width - 40) * 4) {
                                    Color.Gray
                                } else null
                            } else if (x < 20 || x > width + 20) {
                                Color.White
                            } else null
                        }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }

        rule.waitForIdle()

        isSquare = true

        // Wait for transition to start
        while (transition!!.targetState == transition!!.currentState) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        while (transition!!.targetState != transition!!.currentState) {
            // animating from 80, 80 -> 40, 160
            rule.onNodeWithTag("scope").run {
                val fraction = (transition!!.playTimeNanos / 1000_000L) / 100f
                val expectedBoundsWidth = (1f - fraction) * 40f + fraction * 80f + 40
                val expectedBoundsHeight = (1f - fraction) * 160f + fraction * 80f + 40
                if (fraction > 0) {
                    captureToImage().run {
                        assertEquals(expectedBoundsWidth.roundToInt(), width)
                        assertEquals(expectedBoundsHeight.roundToInt(), height)
                        assertPixels { (x, y) ->
                            if (x > 20 && x < width - 20) {
                                // Excluded left & right padding
                                if (y < 20 || y > height - 20) {
                                    // expect grey to be rendered out of bounds since it doesn't
                                    // clip and it's fillWidth
                                    // Expect gray to draw in the padding area since it does not
                                    // crop
                                    val verticalDistTimes2 = abs(2 * y - height)
                                    if (verticalDistTimes2 > (width - 40) * 4) {
                                        Color.White
                                    } else if (verticalDistTimes2 < (width - 40) * 4) {
                                        Color.Gray
                                    } else null
                                } else if (y > 20 && y < height - 20) {
                                    Color.Red
                                } else null
                            } else if (x < 20 || x > width - 20) {
                                // Left and right padding
                                Color.White
                            } else null
                        }
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    @Test
    fun testScaleTransitionInterruption() {
        var showList by mutableStateOf(false)
        var selected by mutableIntStateOf(1)
        val scaleX = FloatArray(5) { 0f }
        val scaleY = FloatArray(5) { 0f }
        var detailScaleX: Float = 0f
        var detailScaleY: Float = 0f
        val boundsTransform = BoundsTransform { _, _ -> tween(160, easing = LinearEasing) }
        rule.setContent {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = showList,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using null },
                ) {
                    if (it) {
                        LazyColumn {
                            repeat(5) { id ->
                                item {
                                    Box(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier.sharedBounds(
                                                    rememberSharedContentState(key = id),
                                                    this@AnimatedContent,
                                                    boundsTransform = boundsTransform,
                                                    resizeMode = scaleToBounds(ContentScale.Fit),
                                                )
                                                .size(200.dp, 50.dp)
                                                .background(
                                                    if (selected == id) Color.Red else Color.White
                                                )
                                                .onPlaced {
                                                    val matrix = Matrix()
                                                    it.transformFrom(
                                                        it.parentLayoutCoordinates!!,
                                                        matrix,
                                                    )
                                                    scaleX[id] = matrix.values[Matrix.ScaleX]
                                                    scaleY[id] = matrix.values[Matrix.ScaleY]
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = selected),
                                        this@AnimatedContent,
                                        boundsTransform = boundsTransform,
                                        resizeMode = scaleToBounds(ContentScale.Fit),
                                    )
                                    .size(100.dp)
                                    .onPlaced {
                                        val matrix = Matrix()
                                        it.transformFrom(it.parentLayoutCoordinates!!, matrix)
                                        detailScaleX = matrix.values[Matrix.ScaleX]
                                        detailScaleY = matrix.values[Matrix.ScaleY]
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1f, detailScaleX)
            assertEquals(1f, detailScaleY)
        }
        rule.mainClock.autoAdvance = false
        // detail -> list -> detail interruption
        showList = true
        while (detailScaleX == 1f) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        var lastDetailScaleX = detailScaleX
        var lastListScaleX = scaleX[selected]

        // Move forward 3 frames
        repeat(6) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            assertEquals(scaleX[selected], scaleY[selected])
            assertEquals(detailScaleX, detailScaleY)

            // Gradually decreasing with limited change
            assertTrue(lastListScaleX - scaleX[selected] > 0.05f)
            assertTrue(lastListScaleX - scaleX[selected] < 0.2f)
            lastListScaleX = scaleX[selected]

            // Gradually increasing with limited change
            assertTrue(detailScaleX - lastDetailScaleX > 0.05f)
            assertTrue(detailScaleX - lastDetailScaleX < 0.2f)
            lastDetailScaleX = detailScaleX
        }

        // Interruption
        selected = 3
        showList = false

        while (scaleX[selected] == 1f) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        lastListScaleX = scaleX[selected]
        lastDetailScaleX = detailScaleX

        repeat(6) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            assertEquals(scaleX[selected], scaleY[selected])
            assertEquals(detailScaleX, detailScaleY)

            // Gradually increasing with limited change
            assertTrue(scaleX[selected] - lastListScaleX > 0.05f)
            assertTrue(scaleX[selected] - lastListScaleX < 0.2f)
            lastListScaleX = scaleX[selected]

            // Gradually decreasing with limited change
            assertTrue(lastDetailScaleX - detailScaleX > 0.05f)
            assertTrue(lastDetailScaleX - detailScaleX < 0.2f)
            lastDetailScaleX = detailScaleX
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(1f, detailScaleX)
        assertEquals(1f, detailScaleY)
    }

    @Test
    fun testFoundMatchBeforeSharedContentComposed() {
        var isSquare by mutableStateOf(true)
        var box2Composed: Boolean = false
        rule.setContent {
            SharedTransitionLayout(
                Modifier.testTag("scope").background(Color.White).padding(20.dp)
            ) {
                AnimatedContent(
                    isSquare,
                    transitionSpec = {
                        EnterTransition.None togetherWith ExitTransition.None using null
                    },
                ) { isSquare ->
                    if (isSquare) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(key = "test").also {
                                        if (transition.currentState == transition.targetState) {
                                            assertFalse(it.isMatchFound)
                                        } else {
                                            assertEquals(box2Composed, it.isMatchFound)
                                        }
                                    },
                                    this,
                                )
                                .size(80.dp)
                                .background(Color.Red)
                        )
                    } else {
                        val key = rememberSharedContentState(key = "test")
                        if (transition.currentState != transition.targetState) {
                            // isMatchFound returns false when created, and next frame return true
                            assertEquals(box2Composed, key.isMatchFound)
                        }
                        box2Composed = true
                        Box(
                            Modifier.sharedBounds(key, this)
                                .size(40.dp, 160.dp)
                                .background(Color.Gray)
                        )
                    }
                }
            }
        }
        isSquare = !isSquare
        rule.waitForIdle()
    }

    sealed class Screen {
        abstract val key: Int

        object List : Screen() {
            override val key = 1
        }

        data class Details(val item: Int) : Screen() {
            override val key = 2
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testRandomScrollingWithInterruption() {
        @Suppress("PrimitiveInCollection")
        val colors =
            listOf(Color(0xffff6f69), Color(0xffffcc5c), Color(0xff2a9d84), Color(0xff264653))
        var state by mutableStateOf<Screen>(Screen.List)
        val lazyListState = LazyListState()
        rule.setContent {
            SharedTransitionLayout(modifier = Modifier.fillMaxWidth().height(800.dp)) {
                AnimatedContent(
                    state,
                    label = "",
                    contentKey = { it.key },
                    transitionSpec = {
                        if (initialState == Screen.List) {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                        } else {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                        }
                    },
                ) {
                    when (it) {
                        Screen.List -> {
                            LazyColumn(state = lazyListState) {
                                items(50) { item ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier =
                                                Modifier.size(100.dp)
                                                    .then(
                                                        Modifier.sharedElement(
                                                            rememberSharedContentState(
                                                                key = "item-image$item"
                                                            ),
                                                            this@AnimatedContent,
                                                        )
                                                    )
                                                    .background(colors[item % 4])
                                        )
                                        Spacer(Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                        is Screen.Details -> {
                            val item = it.item
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier =
                                        Modifier.sharedElement(
                                                rememberSharedContentState(key = "item-image$item"),
                                                this@AnimatedContent,
                                            )
                                            .background(colors[item % 4])
                                            .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        state = Screen.Details(5)
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        state = Screen.List
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        repeat(3) {
            repeat(5) {
                rule.runOnIdle { runBlocking { lazyListState.scrollToItem(40 - it * 10) } }
            }
            rule.mainClock.advanceTimeByFrame()
            repeat(10) { rule.runOnIdle { runBlocking { lazyListState.scrollToItem(it * 5) } } }
            rule.mainClock.advanceTimeByFrame()
            rule.runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
        }
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun vigorouslyScrollingSharedElementsInLazyList() {
        var state by mutableStateOf<Screen>(Screen.List)
        val lazyListState = LazyListState()

        @Suppress("PrimitiveInCollection")
        val colors =
            listOf(Color(0xffff6f69), Color(0xffffcc5c), Color(0xff2a9d84), Color(0xff264653))
        rule.setContent {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = lazyListState) {
                    items(50) { item ->
                        AnimatedVisibility(visible = (state as? Screen.Details)?.item != item) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier =
                                        Modifier.size(100.dp)
                                            .then(
                                                Modifier.sharedElement(
                                                    rememberSharedContentState(
                                                        key = "item-image$item"
                                                    ),
                                                    this@AnimatedVisibility,
                                                )
                                            )
                                            .background(colors[item % 4])
                                )
                                Spacer(Modifier.size(15.dp))
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = state is Screen.Details) {
                    var item: Int? by remember { mutableStateOf(null) }
                    if (state is Screen.Details) {
                        item = (state as Screen.Details).item
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier =
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "item-image$item"),
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxWidth()
                                    .background(colors[item!! % 4])
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        state = Screen.Details(5)
        repeat(5) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        state = Screen.List
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        repeat(5) {
            rule.runOnIdle { runBlocking { lazyListState.scrollToItem(it + 1) } }
            rule.mainClock.advanceTimeByFrame()
        }
        repeat(20) {
            rule.runOnIdle {
                val id = Random.nextInt(0, 20)
                runBlocking { lazyListState.scrollToItem(id) }
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testPlaceHolderLogicSkippedWhenNoMatch() {
        var parentSize: IntSize? = null
        val changeInProgress = true
        var testSize by mutableStateOf(IntSize.Zero)
        rule.setContent {
            AnimatedVisibility(visible = true) {
                SharedTransitionLayout {
                    Box(
                        Modifier.onSizeChanged { parentSize = it }
                            .sharedBounds(
                                rememberSharedContentState("test"),
                                this@AnimatedVisibility,
                            )
                    ) {
                        Box(
                            Modifier.approachLayout(
                                    isMeasurementApproachInProgress = { changeInProgress }
                                ) { measurable, constraints ->
                                    measurable.measure(constraints).run {
                                        layout(testSize.width, testSize.height) { place(0, 0) }
                                    }
                                }
                                .requiredSize(40.dp, 40.dp)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(testSize, parentSize)

        testSize = IntSize(20, 25)
        rule.waitForIdle()
        assertEquals(testSize, parentSize)

        testSize = IntSize(35, 10)
        rule.waitForIdle()
        assertEquals(testSize, parentSize)
    }

    @Test
    fun testUserModifierInSharedTransitionLayout() {
        var scope: SharedTransitionScope? = null
        rule.setContent {
            SharedTransitionLayout(Modifier.offset { IntOffset(65, 536) }) {
                scope = this
                Box(Modifier.fillMaxSize())
            }
        }
        rule.runOnIdle {
            val pos = (scope as SharedTransitionScopeImpl).root.positionInWindow()
            assertEquals(Offset(65f, 536f), pos)
        }
    }

    @Test
    fun testNoAdditionalPlacementWhenNoMatch() {
        val state = LazyListState()
        val placementCount = Array(20) { 0 }
        val controlState = LazyListState()
        val controlPlacementCount = Array(20) { 0 }
        rule.setContent {
            Row {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    AnimatedVisibility(true) {
                        SharedTransitionLayout {
                            LazyColumn(Modifier.size(100.dp), state) {
                                repeat(20) { id ->
                                    item(id) {
                                        Box(
                                            Modifier.sharedElement(
                                                    rememberSharedContentState("$id"),
                                                    this@AnimatedVisibility,
                                                )
                                                .onPlaced { placementCount[id]++ }
                                                .size(10.dp)
                                                .background(Color.Black)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(Modifier.size(100.dp), controlState) {
                        repeat(20) { id ->
                            item(id) {
                                Box(
                                    Modifier.onPlaced { controlPlacementCount[id]++ }
                                        .size(10.dp)
                                        .background(Color.Black)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            repeat(20) {
                runBlocking {
                    state.scrollBy(5f)
                    controlState.scrollBy(5f)
                }
            }
        }
        rule.runOnIdle {
            repeat(20) { assertEquals(controlPlacementCount[it], placementCount[it]) }
        }
    }

    // Verify that lookahead placement is not affected when skipToLookahead is used (indirectly
    // via sharedBounds) by child returning a different size than
    // parent when measured with the same lookahead constraints.
    @Test
    fun testLookaheadPositionInSkipToLookaheadSize() {
        var target by mutableStateOf(true)
        var targetPos: MutableList<Offset?> = mutableListOf()
        var scope: SharedTransitionScope? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier) {
                    scope = this@SharedTransitionLayout
                    AnimatedContent(
                        target,
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    ) { state ->
                        Box(
                            Modifier.requiredSizeIn(maxWidth = 400.dp, maxHeight = 400.dp)
                                .sharedBounds(
                                    rememberSharedContentState("test"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                )
                                .layout { m, c ->
                                    m.measure(c).run {
                                        val w = if (isLookingAhead) width else 200
                                        val h = if (isLookingAhead) height else 300
                                        layout(w, h) {
                                            if (
                                                isLookingAhead && state == target && target == false
                                            ) {
                                                // Check that in spite of returning a different
                                                // size in approach, it doesn't affect the
                                                // lookahead placement.
                                                targetPos.add(
                                                    0,
                                                    coordinates?.let { it.positionInParent() },
                                                )
                                            }
                                            place(0, 0)
                                        }
                                    }
                                }
                                .background(Color.Red)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.Black))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        target = !target
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        assertTrue(scope?.isTransitionActive!!)
        targetPos.forEach {
            if (it != null) {
                assertEquals(Offset.Zero, it)
            }
        }
    }

    @Test
    fun testScaleToBoundsCaching() {
        val alignments =
            listOf(
                TopStart,
                TopCenter,
                TopEnd,
                CenterStart,
                Center,
                CenterEnd,
                BottomStart,
                BottomCenter,
                BottomEnd,
            )

        val contentScales =
            listOf(
                ContentScale.FillWidth,
                ContentScale.FillHeight,
                ContentScale.FillBounds,
                ContentScale.Fit,
                ContentScale.Crop,
                ContentScale.None,
                ContentScale.Inside,
            )
        var prev: SharedTransitionScope.ResizeMode? = null
        alignments.forEach { alignment ->
            contentScales.forEach { contentScale ->
                assertTrue(
                    scaleToBounds(contentScale, alignment) ===
                        scaleToBounds(contentScale, alignment)
                )
                assertFalse(prev === scaleToBounds(contentScale, alignment))
                prev = scaleToBounds(contentScale, alignment)
            }
        }

        val customAlignment =
            object : Alignment {
                override fun align(
                    size: IntSize,
                    space: IntSize,
                    layoutDirection: LayoutDirection,
                ): IntOffset {
                    return Alignment.Center.align(size, space, layoutDirection)
                }
            }

        val customContentScale =
            object : ContentScale {
                override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
                    return ContentScale.Crop.computeScaleFactor(srcSize, dstSize)
                }
            }

        assertFalse(scaleToBounds(customContentScale) === scaleToBounds(customContentScale))
        assertFalse(
            scaleToBounds(alignment = customAlignment) ===
                scaleToBounds(alignment = customAlignment)
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElementsDroppedFromOverlayAfterTransition() {
        // Test that shared elements are dropped from overlay after transition **even if their
        // match is still in the tree**.
        val duration = 500
        var showOverlay by mutableStateOf(false)
        rule.setContent {
            SharedTransitionLayout(Modifier.requiredSize(120.dp).testTag("root")) {
                Box(
                    modifier =
                        Modifier.sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState("box"),
                                visible = !showOverlay,
                                boundsTransform = BoundsTransform { _, _ -> tween(duration) },
                            )
                            .background(Color.LightGray)
                            .fillMaxSize()
                )
                Box(
                    modifier =
                        Modifier.sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState("box"),
                                visible = showOverlay,
                                boundsTransform = BoundsTransform { _, _ -> tween(duration) },
                            )
                            .background(Color.LightGray)
                            .size(110.dp)
                )
                Box(
                    modifier =
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                            .size(100.dp)
                            .background(Color.Black)
                )
            }
        }

        rule.runOnIdle {
            rule.mainClock.autoAdvance = false
            showOverlay = !showOverlay
        }
        rule.waitForIdle()

        repeat(6) {
            rule.mainClock.advanceTimeBy(100)
            rule.onNodeWithTag("root").captureToImage().assertContainsColor(Color.Black)
        }

        rule.mainClock.autoAdvance = true

        rule.runOnIdle {
            rule.mainClock.autoAdvance = false
            showOverlay = !showOverlay
        }
        rule.waitForIdle()

        repeat(6) {
            rule.mainClock.advanceTimeBy(100)
            rule.onNodeWithTag("root").captureToImage().assertContainsColor(Color.Black)
        }
    }

    // Regression test for b/347520198, SharedTransitionLayout onDraw would not get invalidated
    // in some cases.
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedTransitionScopeIsInvalidated() {
        var state by mutableIntStateOf(0)

        val animDurationMillis = 500

        val parentTag = "STL"
        val clickTarget = "click-target"

        rule.setContent {
            SharedTransitionLayout(Modifier.size(100.dp).testTag(parentTag)) {
                // This outer AnimatedContent doesn't do anything, and the issue only triggers
                // when it's present
                AnimatedContent(targetState = true) {
                    @Suppress("UNUSED_EXPRESSION")
                    it // Need to reference the unused outer AnimatedContent's target state

                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            // Add a delay to the animation just so that it takes a known time to
                            // complete
                            fadeIn(snap()).togetherWith(fadeOut(snap(animDurationMillis)))
                        },
                    ) { currentState ->
                        val innerAnimatedContentScope = this
                        Box(
                            // This will cycle from Green -> Blue -> Red -> Blue -> Red...
                            Modifier.testTag(clickTarget)
                                .clickable(
                                    // Don't let the clickable paint anything, it may interfere with
                                    // the test.
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    state =
                                        when (currentState) {
                                            0 -> 2
                                            1 -> 2
                                            else -> 1
                                        }
                                }
                                .fillMaxSize()
                        ) {
                            val color =
                                when (currentState) {
                                    0 -> Color.Green
                                    1 -> Color.Red
                                    else -> Color.Blue
                                }
                            Box(
                                Modifier
                                    // Using shared bounds so that we control when the item enters
                                    // and leaves in every case. Particularly, we want the target to
                                    // show immediately
                                    .sharedBounds(
                                        rememberSharedContentState(
                                            key =
                                                if (currentState == 0) "no match"
                                                else "matching key"
                                        ),
                                        animatedVisibilityScope = innerAnimatedContentScope,
                                        enter = fadeIn(snap()),
                                        exit = fadeOut(snap()),
                                    )
                                    .background(color)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        // Start off with a Green box
        rule.onNodeWithTag(parentTag).captureToImage().assertContainsColor(Color.Green)

        fun clickAndAssertColorDuringTransition(color: Color) {
            rule.mainClock.autoAdvance = false
            rule.onNodeWithTag(clickTarget).performClick()

            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(animDurationMillis / 2L)

            rule.onNodeWithTag(parentTag).captureToImage().assertContainsColor(color)

            rule.mainClock.autoAdvance = true
            rule.waitForIdle()
        }

        // Transition into a Blue box
        clickAndAssertColorDuringTransition(Color.Blue)

        // Transition into a Red box
        clickAndAssertColorDuringTransition(Color.Red)
    }

    @Test
    fun foundMatchedElementButNeverMeasured() {
        var target by mutableStateOf(true)
        rule.setContent {
            SharedTransitionLayout {
                AnimatedContent(target) {
                    SubcomposeLayout {
                        subcompose(0) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("test"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    )
                                    .size(200.dp)
                                    .background(Color.Red)
                            )
                        }
                        // Skip measure and return size
                        layout(200, 200) {}
                    }
                    Box(Modifier.size(200.dp).background(Color.Black))
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        target = !target
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    @Test
    fun intrinsicsQueryComingFromAboveLookaheadRoot() {
        var intrinsicWidth = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Layout(
                    content = {
                        SharedTransitionLayout { Box(Modifier.skipToLookaheadSize().size(100.dp)) }
                    }
                ) { measurables, constraints ->
                    val measurable = measurables[0]
                    intrinsicWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
                    val placeable = measurable.measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(100, intrinsicWidth)
    }

    @Test
    fun SharedElementWithStructuralChangesAmidAnimation() {
        var selectFirst by mutableStateOf(true)
        // The alignment will be changed amid animation.
        var alignment by mutableStateOf(TopStart)
        var positionInTransition: Offset? = null
        var selectFirstPositionInTransition: Offset? = null
        var scope: SharedTransitionScope? = null
        rule.setContent {
            val key = remember { Any() }
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.size(400.dp)) {
                    scope = this
                    AnimatedContent(selectFirst) { selectFirst ->
                        if (!selectFirst) {
                            Box(Modifier.fillMaxSize()) {
                                Box(
                                    Modifier.align(alignment)
                                        .sharedBounds(
                                            rememberSharedContentState(key = key),
                                            this@AnimatedContent,
                                        )
                                        .background(Color.Red)
                                        .onGloballyPositioned {
                                            positionInTransition = it.positionInRoot()
                                        }
                                        .size(100.dp)
                                ) {
                                    Text("false", color = Color.White)
                                }
                            }
                        } else {
                            Box(
                                Modifier.offsetWithMFR(IntOffset(10, 180))
                                    .sharedBounds(rememberSharedContentState(key = key), this)
                                    .onGloballyPositioned {
                                        selectFirstPositionInTransition = it.positionInRoot()
                                    }
                                    .alpha(0.5f)
                                    .background(Color.Blue)
                                    .size(180.dp)
                            ) {
                                Text("true", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { selectFirst = false }
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        assert(positionInTransition != null)
        assertTrue(scope?.isTransitionActive == true)
        val lastPosition = positionInTransition
        rule.runOnIdle { alignment = Alignment.BottomCenter }
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        // Assert that the alignment change is causing the animation to turn around and animate
        // towards the bottom center of the screen
        assert(positionInTransition!!.y > lastPosition!!.y)
        assert(positionInTransition!!.x > lastPosition!!.x)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntOffset(150, 300), positionInTransition!!.round())

        // Trigger transition again in the other direction and change the alignment during
        // transition.
        rule.mainClock.autoAdvance = false
        selectFirstPositionInTransition = null
        rule.runOnIdle { selectFirst = true }
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        assert(selectFirstPositionInTransition != null)
        assertTrue(scope?.isTransitionActive == true)
        rule.runOnIdle { alignment = Alignment.TopStart }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntOffset(10, 180), selectFirstPositionInTransition!!.round())
    }

    private fun Modifier.offsetWithMFR(offset: IntOffset) =
        this.layout { m, c ->
            m.measure(c).run {
                layout(width, height) {
                    withMotionFrameOfReferencePlacement { place(offset.x, offset.y) }
                }
            }
        }

    // Source and destination have different amount of MFR offset, and structural offset. But
    // they total up to the same amount. Test that the MFR offset change is handled correctly
    // and animating back and forth creates no change in the animated position.
    @Test
    fun TestSharedElementWithFixedMfrOffset() {
        var selectFirst by mutableStateOf(true)
        var position1: Offset? = null
        var position2: Offset? = null
        var scope: SharedTransitionScope? = null
        rule.setContent {
            val key = remember { Any() }
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    scope = this
                    AnimatedContent(
                        selectFirst,
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { selectFirst ->
                        if (!selectFirst) {
                            Box(
                                Modifier.layout { m, c ->
                                        m.measure(c).run {
                                            layout(width, height) {
                                                withMotionFrameOfReferencePlacement {
                                                    place(80, 80)
                                                }
                                            }
                                        }
                                    }
                                    .sharedBounds(
                                        rememberSharedContentState(key = key),
                                        this@AnimatedContent,
                                    )
                                    .onGloballyPositioned { position1 = it.positionInRoot() }
                                    .background(Color.Red)
                                    .size(100.dp)
                            ) {
                                Text("false", color = Color.White)
                            }
                        } else {
                            Box(
                                Modifier.offset { IntOffset(180, 180) }
                                    .layout { m, c ->
                                        m.measure(c).run {
                                            layout(width, height) {
                                                withMotionFrameOfReferencePlacement {
                                                    place(-100, -100)
                                                }
                                            }
                                        }
                                    }
                                    .sharedBounds(rememberSharedContentState(key = key), this)
                                    .onGloballyPositioned { position2 = it.positionInRoot() }
                                    .alpha(0.5f)
                                    .background(Color.Blue)
                                    .size(180.dp)
                            ) {
                                Text("true", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        selectFirst = false
        while (!scope!!.isTransitionActive) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        assertEquals(Offset(80f, 80f), position1)
        assertEquals(Offset(80f, 80f), position2)

        // Interrupts the animation
        selectFirst = true
        while (!scope!!.isTransitionActive) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        while (scope.isTransitionActive) {
            assertEquals(Offset(80f, 80f), position1)
            assertEquals(Offset(80f, 80f), position2)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    // Test that when shared element is being scrolled during the animation, the scroll delta
    // is directly applied.
    @Test
    fun SharedElementWithChangingMfrOffset() {
        var target by mutableStateOf(true)
        var scope: SharedTransitionScope? = null
        var position1: Offset? = null
        var position2: Offset? = null
        val state = LazyListState()
        var scrollPosition by mutableStateOf(0)
        rule.setContent {
            val key = remember { Any() }
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    scope = this
                    AnimatedContent(
                        target,
                        Modifier.fillMaxSize(),
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        },
                    ) { target ->
                        if (target) {
                            Box {
                                Box(
                                    Modifier.layout { m, c ->
                                            m.measure(c).run {
                                                layout(width, height) {
                                                    withMotionFrameOfReferencePlacement {
                                                        place(80, 80)
                                                    }
                                                }
                                            }
                                        }
                                        .offset(x = -40.dp)
                                        // After the offsets, the expected target offset is (40, 80)
                                        .sharedElement(
                                            rememberSharedContentState(key = key),
                                            this@AnimatedContent,
                                        )
                                        .onGloballyPositioned { position1 = it.positionInRoot() }
                                        .background(Color.Red)
                                        .size(300.dp)
                                ) {
                                    Text("false", color = Color.White)
                                }
                            }
                        } else {
                            LazyColumn(Modifier.padding(40.dp, 80.dp), state) {
                                repeat(5) { id ->
                                    item {
                                        Box(
                                            Modifier.size(100.dp)
                                                .padding(10.dp)
                                                .background(Color.Gray)
                                        )
                                    }
                                }
                                item { // item id 5
                                    Box(
                                        Modifier.sharedElement(
                                                rememberSharedContentState(key = key),
                                                this@AnimatedContent,
                                            )
                                            .onGloballyPositioned {
                                                position2 = it.positionInRoot()
                                            }
                                            .background(Color.Blue)
                                            .size(100.dp)
                                    ) {
                                        Text("true", color = Color.White)
                                    }
                                }
                                repeat(30) {
                                    item {
                                        Box(
                                            Modifier.size(100.dp)
                                                .padding(10.dp)
                                                .background(Color.Gray)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            rule.mainClock.autoAdvance = false
            target = false
        }
        rule.waitForIdle()

        while (scope?.isTransitionActive != true) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        assertNotNull(position2)
        val positionBeforeScrolling = position2
        repeat(4) {
            rule.runOnIdle { runBlocking { state.scrollBy(100f) } }
            rule.waitForIdle()
            val expectedPosition =
                positionBeforeScrolling!!.copy(y = positionBeforeScrolling.y - 100f * (it + 1))
            assertEquals(expectedPosition, position2)
        }
        rule.runOnIdle { runBlocking { state.scrollBy(99f) } }
        rule.waitForIdle()
        val expectedPosition = positionBeforeScrolling!!.copy(y = positionBeforeScrolling.y - 499f)
        assertEquals(expectedPosition, position2)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // Change the direction of the animation. Animate back with the lazy list scrolled very
        // close to the target position in the destination.
        rule.mainClock.autoAdvance = false
        target = true

        while (scope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        assertEquals(40f, position1?.x)
        assert(position1!!.y >= 80f)
        assert(position1!!.y <= 81f)

        // Test that throughout the animation, the position never goes out of the 1 px range. i.e.
        // no jump.
        while (scope?.isTransitionActive == true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()

            assertEquals(40f, position1?.x)
            assert(position1!!.y >= 80f)
            assert(position1!!.y <= 81f)
        }
    }

    val noContentTransform: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        EnterTransition.None togetherWith ExitTransition.None using null
    }

    @Test
    fun testIsEnabledOnlyInOneDirection() {
        var target by mutableStateOf(true)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    transitionCreated = transition
                    transition.AnimatedContent(transitionSpec = noContentTransform) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState("test"),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else {
                                Box(
                                    Modifier.offset(20.dp, 20.dp)
                                        .sharedElement(
                                            rememberSharedContentState(
                                                "test",
                                                config =
                                                    SharedContentConfig {
                                                        // Only enable shared element when going
                                                        // from
                                                        // true
                                                        // to
                                                        // false
                                                        !transition.targetState
                                                    },
                                            ),
                                            this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ -> tween(160) },
                                        )
                                        .onPlaced {
                                            sizes2.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets2.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = false }
        // Expect animation from true to false
        while (transitionCreated?.currentState != false) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        sizes2.forEachIndexed { i, size ->
            if (i == 0) {
                assertTrue(size.width >= 20)
                assertTrue(size.height >= 20)
            } else {
                val prevSize = sizes2[i - 1]
                assertTrue(
                    "Error: shared element width is not monotonically increasing",
                    size.width >= prevSize.width,
                )
                assertTrue(
                    "Error: shared element height is not monotonically increasing",
                    size.height >= prevSize.height,
                )
            }
        }
        assertEquals(IntSize(20, 20), sizes2.first())
        assertEquals(IntSize(180, 180), sizes2.last())

        offsets2.forEachIndexed { i, offset ->
            if (i == 0) {
                assertTrue(offset.x >= 0)
                assertTrue(offset.y >= 0)
            } else {
                val prevOffset = offsets2[i - 1]
                assertTrue(
                    "Error: shared element offset is not monotonically increasing",
                    offset.x >= prevOffset.x,
                )
                assertTrue(
                    "Error: shared element offset is not monotonically increasing",
                    offset.y >= prevOffset.y,
                )
            }
        }
        assertEquals(Offset(0f, 0f), offsets2.first())
        assertEquals(Offset(20f, 20f), offsets2.last())

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect no animation
        rule.mainClock.autoAdvance = false
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle { target = true }

        // Expect no animation from true to false
        while (transitionCreated?.currentState != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Since we don't expect animations on the way back, the sizes & positions should only
        // contain one value (or duplicates of the same value) each.
        sizes1.forEach { assertEquals(IntSize(20, 20), it) }
        sizes2.forEach { assertEquals(IntSize(180, 180), it) }
        offsets1.forEach { assertEquals(Offset(0f, 0f), it) }
        offsets2.forEach { assertEquals(Offset(20f, 20f), it) }
    }

    @Test
    // The shared element transition is only enabled in one direction, but before the transition
    // finishes it is interrupted. Expect the not-enabled direction to handle the interruption
    // with animation in this test.
    fun testIsEnabledOnlyInOneDirectionWithInterruption() {
        var target by mutableStateOf(true)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    transitionCreated = transition
                    transition.AnimatedContent(transitionSpec = noContentTransform) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState("test"),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else {
                                Box(
                                    Modifier.offset(20.dp, 20.dp)
                                        .sharedElement(
                                            rememberSharedContentState(
                                                "test",
                                                config =
                                                    SharedContentConfig {
                                                        // Only enable shared element when going
                                                        // from
                                                        // true
                                                        // to
                                                        // false
                                                        !transition.targetState
                                                    },
                                            ),
                                            this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ -> tween(160) },
                                        )
                                        .onPlaced {
                                            sizes2.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets2.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = false }
        // Expect animation from true to false
        while (sizes1.last().width == 20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Now the animation has started. Run 3 frames
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Verify through the values that the animation has started, but not yet finished.
        assertTrue(
            "Error: shared element width is ${sizes1.last().width}, not within the" +
                " valid range for a running animation",
            sizes1.last().width > 20 && sizes1.last().width < 180,
        )
        assertTrue(
            "Error: shared element height is ${sizes1.last().height}, not within the" +
                " valid range for a running animation",
            sizes1.last().height > 20 && sizes1.last().height < 180,
        )

        assertTrue(
            "Error: shared element x is ${offsets1.last().x}, not within the" +
                " valid range for a running animation",
            offsets1.last().x > 0 && offsets1.last().x < 20,
        )
        assertTrue(
            "Error: shared element y is ${offsets1.last().y}, not within the" +
                " valid range for a running animation",
            offsets1.last().y > 0 && offsets1.last().y < 20,
        )

        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect animation because the config
        // states disable unless animation is ongoing.
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle { target = true }

        // Expect animation since `accountForAnimation` has not be overridden from true to false
        while (transitionCreated?.currentState != true || transitionCreated?.targetState != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        sizes2.forEachIndexed { i, size ->
            if (i > 3) {
                val prevSize = sizes2[i - 1]
                assertTrue(
                    "Error: shared element width is not monotonically decreasing," +
                        " from ${prevSize.width} to ${size.width}",
                    prevSize.width >= size.width,
                )
                assertTrue(
                    "Error: shared element height is not monotonically decreasing",
                    prevSize.height >= size.height,
                )
            } else if (i > 0) {
                val prevSize = sizes2[i - 1]
                // Expect at least 3 frames of changing value
                assertTrue(
                    "Error: shared element width is not monotonically decreasing," +
                        " from ${prevSize.width} to ${size.width}",
                    prevSize.width >= size.width,
                )
                assertTrue(
                    "Error: shared element height is not monotonically decreasing",
                    prevSize.height >= size.height,
                )
            }
        }

        // Also check that there are more than just initial and target values from offsets, to
        // confirm that animation has been run.
        assertTrue(offsets2.distinct().size > 2)
    }

    @Test
    // The shared element transition is only enabled in one direction, but before the transition
    // finishes it is interrupted. Expect the not-enabled direction to have no animation, since
    // user explicitly configures the shared element to disable even when animating.
    fun testIsEnabledOnlyInOneDirectionExplicitlyNoInterruptionHandling() {
        var target by mutableStateOf(true)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    val disableInSpiteOfAnimationConfig = remember {
                        object : SharedTransitionScope.SharedContentConfig {
                            override val shouldKeepEnabledForOngoingAnimation: Boolean
                                get() = false

                            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                                get() = !transition.targetState
                        }
                    }
                    transitionCreated = transition
                    transition.AnimatedContent(transitionSpec = noContentTransform) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState(
                                                "test",
                                                config = disableInSpiteOfAnimationConfig,
                                            ),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else {
                                Box(
                                    Modifier.offset(20.dp, 20.dp)
                                        .sharedElement(
                                            rememberSharedContentState(
                                                "test",
                                                config = disableInSpiteOfAnimationConfig,
                                            ),
                                            this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ -> tween(160) },
                                        )
                                        .onPlaced {
                                            sizes2.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets2.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = false }
        // Expect animation from true to false
        while (sizes1.last().width == 20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Now the animation has started. Run 3 frames
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Verify through the values that the animation has started, but not yet finished.
        assertTrue(
            "Error: shared element width is ${sizes1.last().width}, not within the" +
                " valid range for a running animation",
            sizes1.last().width > 20 && sizes1.last().width < 180,
        )
        assertTrue(
            "Error: shared element height is ${sizes1.last().height}, not within the" +
                " valid range for a running animation",
            sizes1.last().height > 20 && sizes1.last().height < 180,
        )

        assertTrue(
            "Error: shared element x is ${offsets1.last().x}, not within the" +
                " valid range for a running animation",
            offsets1.last().x > 0 && offsets1.last().x < 20,
        )
        assertTrue(
            "Error: shared element y is ${offsets1.last().y}, not within the" +
                " valid range for a running animation",
            offsets1.last().y > 0 && offsets1.last().y < 20,
        )

        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect no animation
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle { target = true }

        // Expect animation since `accountForAnimation` has not be overridden from true to false
        while (transitionCreated?.currentState != true || transitionCreated?.targetState != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Since we don't expect animations on the way back, the sizes & positions should only
        // contain one value (or duplicates of the same value) each.
        sizes1.forEach { assertEquals(IntSize(20, 20), it) }
        sizes2.forEach { assertEquals(IntSize(180, 180), it) }
        offsets1.forEach { assertEquals(Offset(0f, 0f), it) }
        offsets2.forEach { assertEquals(Offset(20f, 20f), it) }
    }

    @Test
    fun removeTargetWithoutExplicitAlternativeTarget() {
        // Expect animation to stop right away
        var target by mutableStateOf(true)
        var removeTarget by mutableStateOf(false)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    transitionCreated = transition
                    val configWithoutAlternativeTarget = remember {
                        object : SharedTransitionScope.SharedContentConfig {
                            override fun SharedTransitionScope.SharedContentState
                                .alternativeTargetBoundsInTransitionScopeAfterRemoval(
                                targetBoundsBeforeRemoval: Rect,
                                sharedTransitionLayoutSize: Size,
                            ): Rect? = null
                        }
                    }
                    transition.AnimatedContent(
                        transitionSpec = {
                            // Intentionally make the animation longer than the bounds transform
                            // so we'll get the last frame of the bounds transform.
                            fadeIn(tween(2000)) togetherWith fadeOut(tween(2000)) using null
                        }
                    ) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState("test"),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else {
                                Box(Modifier.size(200.dp))
                                if (!removeTarget) {
                                    Box(
                                        Modifier.offset(20.dp, 20.dp)
                                            .sharedElement(
                                                rememberSharedContentState(
                                                    "test",
                                                    config = configWithoutAlternativeTarget,
                                                ),
                                                this@AnimatedContent,
                                                boundsTransform =
                                                    BoundsTransform { _, _ -> tween(160) },
                                            )
                                            .onPlaced {
                                                sizes2.add(it.size)
                                                val lookaheadScopeCoords =
                                                    it.lookaheadScopeCoordinates(
                                                        this@SharedTransitionLayout
                                                    )
                                                offsets2.add(
                                                    lookaheadScopeCoords.localPositionOf(it)
                                                )
                                            }
                                            .size(180.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = false }
        // Expect animation from true to false
        while (sizes1.last().width == 20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Now the animation has started. Run 3 frames
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Verify through the values that the animation has started, but not yet finished.
        assertTrue(
            "Error: shared element width is ${sizes1.last().width}, not within the" +
                " valid range for a running animation",
            sizes1.last().width > 20 && sizes1.last().width < 180,
        )
        assertTrue(
            "Error: shared element height is ${sizes1.last().height}, not within the" +
                " valid range for a running animation",
            sizes1.last().height > 20 && sizes1.last().height < 180,
        )

        assertTrue(
            "Error: shared element x is ${offsets1.last().x}, not within the" +
                " valid range for a running animation",
            offsets1.last().x > 0 && offsets1.last().x < 20,
        )
        assertTrue(
            "Error: shared element y is ${offsets1.last().y}, not within the" +
                " valid range for a running animation",
            offsets1.last().y > 0 && offsets1.last().y < 20,
        )

        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect animation because the config
        // states disable unless animation is ongoing.
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle { removeTarget = true }

        while (transitionCreated?.currentState != false) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Expect no animation since the alternative target is not specified.
        sizes1.forEach { assertEquals(IntSize(20, 20), it) }
        offsets1.forEach { assertEquals(Offset(0f, 0f), it) }

        assertTrue(sizes2.isEmpty())
        assertTrue(offsets2.isEmpty())
    }

    // Expect animation to stop right away since the disable logic explicitly ignores animation.
    @Test
    fun disableIgnoreAnimationWithAlternativeTargetOnDisabledContent() {

        var target by mutableStateOf(false)
        var enableTarget by mutableStateOf(true)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    transitionCreated = transition
                    val configWithAlternativeTarget = remember {
                        object : SharedTransitionScope.SharedContentConfig {
                            override val shouldKeepEnabledForOngoingAnimation: Boolean
                                get() = false

                            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                                get() = enableTarget

                            override fun SharedTransitionScope.SharedContentState
                                .alternativeTargetBoundsInTransitionScopeAfterRemoval(
                                targetBoundsBeforeRemoval: Rect,
                                sharedTransitionLayoutSize: Size,
                            ): Rect? {
                                return Rect(Offset(30f, 40f), Size(50f, 60f))
                            }
                        }
                    }
                    transition.AnimatedContent(
                        transitionSpec = {
                            // Intentionally make the animation longer than the bounds transform
                            // so we'll get the last frame of the bounds transform.
                            fadeIn(tween(2000)) togetherWith fadeOut(tween(2000)) using null
                        }
                    ) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState("test"),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else { // targetState == false
                                Box(Modifier.size(200.dp))
                                Box(
                                    Modifier.offset(20.dp, 20.dp)
                                        .sharedElement(
                                            rememberSharedContentState(
                                                "test",
                                                config = configWithAlternativeTarget,
                                            ),
                                            this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ -> tween(160) },
                                        )
                                        .onPlaced {
                                            sizes2.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets2.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = true }
        // Expect animation from false to true
        while (sizes2.last().width == 180) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Now the animation has started. Run 3 frames
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Verify through the values that the animation has started, but not yet finished.
        assertTrue(
            "Error: shared element width is ${sizes1.last().width}, not within the" +
                " valid range for a running animation",
            sizes1.last().width > 20 && sizes1.last().width < 180,
        )
        assertTrue(
            "Error: shared element height is ${sizes1.last().height}, not within the" +
                " valid range for a running animation",
            sizes1.last().height > 20 && sizes1.last().height < 180,
        )

        assertTrue(
            "Error: shared element x is ${offsets1.last().x}, not within the" +
                " valid range for a running animation",
            offsets1.last().x > 0 && offsets1.last().x < 20,
        )
        assertTrue(
            "Error: shared element y is ${offsets1.last().y}, not within the" +
                " valid range for a running animation",
            offsets1.last().y > 0 && offsets1.last().y < 20,
        )

        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect animation because the config
        // states disable unless animation is ongoing.
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle {
            // Change target during the animation, while disabling the target state shared element
            enableTarget = false
            target = false
        }

        // Expect no animation.
        while (
            transitionCreated?.targetState != false || transitionCreated?.currentState != false
        ) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        sizes1.forEach { assertEquals(IntSize(20, 20), it) }
        offsets1.forEach { assertEquals(Offset(0f, 0f), it) }
        sizes2.forEach { assertEquals(IntSize(180, 180), it) }
        offsets2.forEach { assertEquals(Offset(20f, 20f), it) }
    }

    @Test
    fun testRemoveTargetWithAlternativeTargetSpecified() {
        var target by mutableStateOf(true)
        var removeTarget by mutableStateOf(false)
        // Track sizes & positions in state == true
        val sizes1 = mutableListOf<IntSize>()
        val offsets1 = mutableListOf<Offset>()
        // Track sizes & positions in state == false
        val sizes2 = mutableListOf<IntSize>()
        val offsets2 = mutableListOf<Offset>()

        var transitionCreated: Transition<*>? = null
        // Test that when going from state true -> false there's shared element, otherwise no.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    val transition = updateTransition(target)
                    transitionCreated = transition
                    val configWithAlternativeTarget = remember {
                        object : SharedTransitionScope.SharedContentConfig {
                            override fun SharedTransitionScope.SharedContentState
                                .alternativeTargetBoundsInTransitionScopeAfterRemoval(
                                targetBoundsBeforeRemoval: Rect,
                                sharedTransitionLayoutSize: Size,
                            ): Rect? {
                                return Rect(Offset(30f, 40f), Size(50f, 60f))
                            }
                        }
                    }
                    transition.AnimatedContent(
                        transitionSpec = {
                            // Intentionally make the animation longer than the bounds transform
                            // so we'll get the last frame of the bounds transform.
                            fadeIn(tween(2000)) togetherWith fadeOut(tween(2000)) using null
                        }
                    ) { targetState ->
                        Box(Modifier.size(200.dp)) {
                            if (targetState) {
                                Box(
                                    Modifier.sharedElement(
                                            rememberSharedContentState("test"),
                                            boundsTransform =
                                                BoundsTransform { _, _ -> tween(160) },
                                            animatedVisibilityScope = this@AnimatedContent,
                                        )
                                        .onPlaced {
                                            sizes1.add(it.size)
                                            val lookaheadScopeCoords =
                                                it.lookaheadScopeCoordinates(
                                                    this@SharedTransitionLayout
                                                )
                                            offsets1.add(lookaheadScopeCoords.localPositionOf(it))
                                        }
                                        .size(20.dp)
                                )
                            } else {
                                Box(Modifier.size(200.dp))
                                if (!removeTarget) {
                                    Box(
                                        Modifier.offset(20.dp, 20.dp)
                                            .sharedElement(
                                                rememberSharedContentState(
                                                    "test",
                                                    config = configWithAlternativeTarget,
                                                ),
                                                this@AnimatedContent,
                                                boundsTransform =
                                                    BoundsTransform { _, _ -> tween(160) },
                                            )
                                            .onPlaced {
                                                sizes2.add(it.size)
                                                val lookaheadScopeCoords =
                                                    it.lookaheadScopeCoordinates(
                                                        this@SharedTransitionLayout
                                                    )
                                                offsets2.add(
                                                    lookaheadScopeCoords.localPositionOf(it)
                                                )
                                            }
                                            .size(180.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { target = false }
        // Expect animation from true to false
        while (sizes1.last().width == 20) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
        // Now the animation has started. Run 3 frames
        repeat(3) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Verify through the values that the animation has started, but not yet finished.
        assertTrue(
            "Error: shared element width is ${sizes1.last().width}, not within the" +
                " valid range for a running animation",
            sizes1.last().width > 20 && sizes1.last().width < 180,
        )
        assertTrue(
            "Error: shared element height is ${sizes1.last().height}, not within the" +
                " valid range for a running animation",
            sizes1.last().height > 20 && sizes1.last().height < 180,
        )

        assertTrue(
            "Error: shared element x is ${offsets1.last().x}, not within the" +
                " valid range for a running animation",
            offsets1.last().x > 0 && offsets1.last().x < 20,
        )
        assertTrue(
            "Error: shared element y is ${offsets1.last().y}, not within the" +
                " valid range for a running animation",
            offsets1.last().y > 0 && offsets1.last().y < 20,
        )

        rule.waitForIdle()

        // Now go back the other way (i.e. false -> true), expect animation because the config
        // states disable unless animation is ongoing.
        offsets2.clear()
        offsets1.clear()
        sizes2.clear()
        sizes1.clear()

        rule.runOnIdle { removeTarget = true }

        // Expect animation to animate to the alternative target
        while (transitionCreated?.currentState != false) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        // Animation has finished
        assertEquals(0, sizes2.size)
        assertEquals(0, offsets2.size)

        // Verify that by the end of the animation, the size & position reached the alternative
        // target.
        assertEquals(IntSize(50, 60), sizes1.last())
        assertEquals(Offset(30f, 40f), offsets1.last())

        // Also check that there are more than just initial and target values from offsets, to
        // confirm that animation has been run.
        assertTrue(offsets1.distinct().size > 2)
        assertTrue(sizes1.distinct().size > 2)
    }

    @Test
    fun testAndVerifyObserveIsNotDoneOnNonUIThread() {
        var visible by mutableStateOf(false)
        var isEnabled by mutableStateOf(false)
        var scope: SharedTransitionScopeImpl? = null
        var show2ndBox by mutableStateOf(true)
        var threadId by mutableStateOf(-1L)
        var testBlockInvocationCount = 0
        rule.setContent {
            SharedTransitionLayout(Modifier.requiredSize(120.dp).testTag("root")) {
                scope = this@SharedTransitionLayout as SharedTransitionScopeImpl
                Box(
                    modifier =
                        Modifier.sharedElementWithCallerManagedVisibility(
                                sharedContentState =
                                    rememberSharedContentState(
                                        "box",
                                        SharedContentConfig { isEnabled },
                                    ),
                                visible = !visible,
                                boundsTransform = BoundsTransform { _, _ -> tween() },
                            )
                            .background(Color.LightGray)
                            .fillMaxSize()
                )
                if (show2ndBox) {
                    Box(
                        modifier =
                            Modifier.sharedElementWithCallerManagedVisibility(
                                    sharedContentState = rememberSharedContentState("box"),
                                    visible = visible,
                                    boundsTransform = BoundsTransform { _, _ -> tween() },
                                )
                                .background(Color.LightGray)
                                .size(110.dp)
                    )
                }
            }
        }
        scope!!.testBlockToRun = {
            // Check that the test block is never ran on the custom thread
            assertNotEquals(threadId, Thread.currentThread().id)
            testBlockInvocationCount++
        }
        rule.waitForIdle()
        val thread =
            @SuppressLint("BanThreadSleep")
            thread(start = true) {
                threadId = Thread.currentThread().id
                repeat(100) {
                    Snapshot.withMutableSnapshot { isEnabled = !isEnabled }
                    sleep(1)
                }
            }
        repeat(10) {
            // Triggers observations on the UI thread
            show2ndBox = !show2ndBox
            rule.waitForIdle()
        }
        thread.join()
        assertTrue(testBlockInvocationCount >= 10)
    }

    @Test
    fun NewlyAddedSharedElementWithCallerManagedVisibilityTriggersAnimation() {
        var state by mutableStateOf(State.Start)
        val targetSizes = mutableListOf<IntSize>()
        val initialSizes = mutableListOf<IntSize>()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(modifier = Modifier.fillMaxSize(), targetState = state) {
                        currentState ->
                        when (currentState) {
                            State.Start ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier =
                                            Modifier.align(Alignment.TopEnd)
                                                .sharedElementWithCallerManagedVisibility(
                                                    sharedContentState =
                                                        rememberSharedContentState(
                                                            State
                                                                .SharedElementWithUserManagedVisibility
                                                        ),
                                                    visible =
                                                        transition.targetState ==
                                                            EnterExitState.Visible,
                                                    boundsTransform =
                                                        BoundsTransform { _, _ ->
                                                            tween(160, easing = LinearEasing)
                                                        },
                                                )
                                                .onGloballyPositioned { initialSizes.add(it.size) }
                                                .background(Color.Blue)
                                                .size(60.dp)
                                    )
                                }

                            State.SharedElementWithUserManagedVisibility ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier =
                                            Modifier.sharedElementWithCallerManagedVisibility(
                                                    sharedContentState =
                                                        rememberSharedContentState(
                                                            State
                                                                .SharedElementWithUserManagedVisibility
                                                        ),
                                                    visible =
                                                        transition.targetState ==
                                                            EnterExitState.Visible,
                                                    boundsTransform =
                                                        BoundsTransform { _, _ ->
                                                            tween(160, easing = LinearEasing)
                                                        },
                                                )
                                                .onGloballyPositioned { targetSizes.add(it.size) }
                                                .background(Color.Blue)
                                                .size(160.dp)
                                    )
                                }
                        }
                    }
                }
            }
        }
        rule.runOnIdle { state = State.SharedElementWithUserManagedVisibility }
        rule.waitForIdle()

        // Check sizes
        val start = targetSizes.indexOfFirst { it.width > 60 } - 1
        for (i in start until targetSizes.size) {
            val frameCount = i - start
            if (frameCount <= 10) {
                assertEquals(IntSize(60 + frameCount * 10, 60 + frameCount * 10), targetSizes[i])
            }
        }
        /// Assert that all 10 frames are run.
        assertTrue(targetSizes.size - start > 10)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    private enum class State {
        Start,
        SharedElementWithUserManagedVisibility,
    }
}

private fun assertEquals(a: IntSize, b: IntSize, delta: IntSize) {
    assertEquals(a.width.toFloat(), b.width.toFloat(), delta.width.toFloat())
    assertEquals(a.height.toFloat(), b.height.toFloat(), delta.height.toFloat())
}

private fun assertEquals(a: Offset, b: Offset, delta: Offset) {
    assertEquals(a.x, b.x, delta.x)
    assertEquals(a.y, b.y, delta.y)
}
