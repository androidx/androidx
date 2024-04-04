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

@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalAnimationApi::class)

package androidx.compose.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SharedTransitionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun transitionInterruption() {
        var visible by mutableStateOf(true)
        val boundsTransform = BoundsTransform { _, _ -> tween(500, easing = LinearEasing) }
        val positions = mutableListOf(
            Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero
        )
        val sizes = mutableListOf(
            IntSize(-1, -1), IntSize(-1, -1), IntSize.Zero, IntSize.Zero
        )
        var transitionScope: SharedTransitionScope? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    transitionScope = this
                    AnimatedVisibility(visible = visible) {
                        Column {
                            Box(Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "cat"),
                                    this@AnimatedVisibility,
                                    boundsTransform = boundsTransform
                                )
                                .onGloballyPositioned {
                                    positions[0] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[0] = it.size
                                }
                                .size(200.dp))
                            Box(Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "dog"),
                                    this@AnimatedVisibility,
                                    boundsTransform = boundsTransform
                                )
                                .onGloballyPositioned {
                                    positions[1] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[1] = it.size
                                }
                                .size(50.dp))
                        }
                    }
                    AnimatedVisibility(visible = !visible) {
                        Row {
                            Box(Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "dog"),
                                    this@AnimatedVisibility,
                                    boundsTransform = boundsTransform
                                )
                                .onGloballyPositioned {
                                    positions[2] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[2] = it.size
                                }
                                .size(50.dp))
                            Box(Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "cat"),
                                    this@AnimatedVisibility,
                                    boundsTransform = boundsTransform
                                )
                                .onGloballyPositioned {
                                    positions[3] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    sizes[3] = it.size
                                }
                                .size(200.dp))
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
        val positions = mutableListOf(
            Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero
        )
        val sizes = mutableListOf(IntSize(-1, -1), IntSize(-1, -1), IntSize.Zero, IntSize.Zero)
        var transitionScope: SharedTransitionScope? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout {
                    transitionScope = this
                    Column {
                        Box(Modifier
                            .sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = "cat"),
                                visible = visible,
                                boundsTransform = boundsTransform
                            )
                            .onGloballyPositioned {
                                positions[0] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                sizes[0] = it.size
                            }
                            .size(200.dp))
                        Box(Modifier
                            .sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = "dog"),
                                visible = visible,
                                boundsTransform = boundsTransform
                            )
                            .onGloballyPositioned {
                                positions[1] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                sizes[1] = it.size
                            }
                            .size(50.dp))
                    }
                    Row {
                        Box(Modifier
                            .sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = "dog"),
                                visible = !visible,
                                boundsTransform = boundsTransform
                            )
                            .onGloballyPositioned {
                                positions[2] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                sizes[2] = it.size
                            }
                            .size(50.dp))
                        Box(Modifier
                            .sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = "cat"),
                                visible = !visible,
                                boundsTransform = boundsTransform
                            )
                            .onGloballyPositioned {
                                positions[3] = lookaheadRoot.localPositionOf(it, Offset.Zero)
                                sizes[3] = it.size
                            }
                            .size(200.dp))
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
                Column {
                    Box(
                        Modifier
                            .sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = key1).also {
                                    set.add(it)
                                },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier
                            .sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(key = 2).also {
                                    set.add(it)
                                },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier
                            .sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = "cat").also {
                                    set.add(it)
                                },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                    Box(
                        Modifier
                            .sharedBoundsWithCallerManagedVisibility(
                                rememberSharedContentState(key = Unit).also {
                                    set.add(it)
                                },
                                visible = visible,
                            )
                            .size(200.dp)
                    )
                }
                if (showRow) {
                    Row {
                        Box(
                            Modifier
                                .sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = key1).also {
                                        set.add(it)
                                    },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier
                                .sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = 2).also {
                                        set.add(it)
                                    },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier
                                .sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "cat").also {
                                        set.add(it)
                                    },
                                    visible = !visible,
                                )
                                .size(50.dp)
                        )
                        Box(
                            Modifier
                                .sharedBoundsWithCallerManagedVisibility(
                                    rememberSharedContentState(key = Unit).also {
                                        set.add(it)
                                    },
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
        set.forEach {
            assertFalse(it.isMatchFound)
        }

        // Show row to add matched shared elements into composition
        showRow = true
        rule.runOnIdle {
            assertEquals(8, set.size)
            set.forEach {
                assertTrue(it.isMatchFound)
            }
        }
        visible = false
        rule.runOnIdle {
            set.forEach {
                assertTrue(it.isMatchFound)
            }
        }
        set.clear()
        showRow = false
        rule.runOnIdle {
            assertEquals(4, set.size)
        }
        rule.runOnIdle {
            set.forEach {
                assertFalse(it.isMatchFound)
            }
        }
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
                        1 -> Box(
                            Modifier
                                .sharedElement(
                                    state1, this
                                )
                                .size(200.dp)
                        ) {
                            DisposableEffect(key1 = Unit) {
                                set.add(state1)
                                onDispose {
                                    set.remove(state1)
                                }
                            }
                        }

                        2 -> Box(
                            Modifier
                                .sharedElement(
                                    state2,
                                    this
                                )
                                .size(600.dp)
                        ) {
                            DisposableEffect(key1 = Unit) {
                                set.add(state2)
                                onDispose {
                                    set.remove(state2)
                                }
                            }
                        }

                        else -> Box(Modifier.size(200.dp)) {
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
        set.forEach {
            assertFalse(it.isMatchFound)
        }

        // Show row to add matched shared elements into composition
        rule.runOnIdle {
            target = 2
        }
        rule.waitUntil {
            set.size == 2
        }

        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
        }
        assertEquals(2, set.size)

        // Now we expect two shared elements to be matched
        set.forEach {
            assertTrue(it.isMatchFound)
        }
        target = 3

        rule.waitUntil { !firstFrame }
        set.forEach {
            assertEquals(2, set.size)
            assertFalse(it.isMatchFound)
        }
        rule.mainClock.advanceTimeBy(50000L)
    }

    @SdkSuppress(minSdkVersion = 26)
    @OptIn(ExperimentalAnimationApi::class)
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
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("scope")
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        enterTransition = transition
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    fadeIn(tween),
                                    fadeOut(tween)
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Red)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        exitTransition = transition
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    fadeIn(tween),
                                    fadeOut(tween)
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
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
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("scope")
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible, enter = fadeIn(tween), exit = fadeOut(tween)
                    ) {
                        Box(
                            Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(Color.Red)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
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
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("scope")
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedElement(rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    boundsTransform = BoundsTransform { _, _ ->
                                        tween(100, easing = LinearEasing)
                                    })
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(Modifier
                            .fillMaxSize(0.5f)
                            .sharedElement(rememberSharedContentState(key = "child"),
                                this@AnimatedVisibility,
                                clipInOverlayDuringTransition = OverlayClip(CircleShape),
                                boundsTransform = BoundsTransform { _, _ ->
                                    tween(100, easing = LinearEasing)
                                })
                            .onGloballyPositioned {
                                size = it.size
                            }
                            .background(Color.Blue))
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
                    Color.White
                }
            }
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
                        val distanceToCenter = sqrt(
                            (it.x - width / 2) * (it.x - width / 2).toFloat() +
                                (it.y - height / 2) * (it.y - height / 2)
                        )
                        if (it.x < width / 2 &&
                            distanceToCenter < size!!.width / 2 - widthTolerance
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
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("scope")
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility
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
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition = OverlayClip(
                                        clipShape = CircleShape
                                    )
                                )
                                .fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility
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
            captureToImage().run {
                assertPixels {
                    Color.White
                }
            }
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
                        val distanceToCenter = sqrt(
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
                SharedTransitionLayout(
                    Modifier
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(0.5f)
                            .offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        exitTransition = this.transition
                        Box(
                            Modifier
                                .offset(20.dp)
                                .sharedBounds(rememberSharedContentState(key = "test"),
                                    this@AnimatedVisibility,
                                    boundsTransform = BoundsTransform { _, _ ->
                                        tween(100, easing = LinearEasing)
                                    })
                                .onGloballyPositioned {
                                    parentPosition =
                                        lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    parentSize = it.size
                                }) {
                            Box(Modifier
                                .offset(-20.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    boundsTransform = { initialBounds, targetBounds ->
                                        assertEquals(initialBounds, targetBounds)
                                        spring()
                                    }
                                )
                                .onGloballyPositioned {
                                    childPosition =
                                        lookaheadRoot.localPositionOf(it, Offset.Zero)
                                    childSize = it.size
                                }
                                .fillMaxSize()
                                .background(Color.Blue))
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
            val fraction =
                ((exitTransition!!.playTimeNanos / 1000_000L) / 100f).coerceIn(0f, 1f)
            val expectedSize = (50 * fraction + 100 * (1 - fraction)).roundToInt().let {
                IntSize(it, it)
            }
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
                SharedTransitionLayout(
                    Modifier
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .wrapContentSize()
                                .onGloballyPositioned {
                                    parent1Size = it.size
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .sharedElement(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility
                                    )
                                    .fillMaxSize(0.5f)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(Modifier
                            .onGloballyPositioned {
                                parent2Size = it.size
                            }
                            .offset(-20.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "child"),
                                this@AnimatedVisibility,
                                placeHolderSize = SharedTransitionScope
                                    .PlaceHolderSize { _, _ ->
                                        expectedSize
                                    }
                            )
                            .fillMaxSize()
                            .background(Color.Blue))
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
                    Modifier
                        .testTag("scope")
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility
                                )
                                .background(Color.Red)
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100, easing = LinearEasing)),
                        exit = ExitTransition.None
                    ) {
                        exit = transition
                        Box(
                            Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    renderInOverlayDuringTransition = false
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
            captureToImage().run {
                assertPixels {
                    Color.Red
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
            val fraction = ((exit!!.playTimeNanos / 1000_000L) / 100f).coerceIn(0f, 1f)
            rule.onNodeWithTag("scope").run {
                assertExists("Error: Node doesn't exist")
                captureToImage().run {
                    assertPixels {
                        Color.Blue.copy(alpha = fraction).compositeOver(Color.White)
                    }
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
                    Modifier
                        .testTag("scope")
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this

                    Box(
                        Modifier
                            .renderInSharedTransitionScopeOverlay(
                                zIndexInOverlay = greenZIndex
                            )
                            .background(Color.Green)
                            .fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(100, easing = LinearEasing)),
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                    zIndexInOverlay = redZIndex
                                )
                                .background(Color.Red)
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100, easing = LinearEasing)),
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                    zIndexInOverlay = blueZIndex
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
            captureToImage().run {
                assertPixels {
                    Color.Red
                }
            }
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
                captureToImage().run {
                    assertPixels {
                        expectedTopColor
                    }
                }
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
                    Modifier
                        .requiredSize(100.dp)
                        .testTag("scope")
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedElement(rememberSharedContentState(key = "child"),
                                    this@AnimatedVisibility,
                                    boundsTransform = BoundsTransform { _, _ ->
                                        tween(100, easing = LinearEasing)
                                    })
                                .fillMaxSize()
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxSize(),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(Modifier
                            .padding(25.dp)
                            .sharedElement(rememberSharedContentState(key = "child"),
                                this@AnimatedVisibility,
                                clipInOverlayDuringTransition = OverlayClip(CircleShape),
                                boundsTransform = BoundsTransform { _, _ ->
                                    tween(100, easing = LinearEasing)
                                })
                            .skipToLookaheadSize()
                            .onGloballyPositioned {
                                size = it.size
                            }
                            .background(Color.Blue))
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
                    Modifier
                        .testTag("scope")
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this
                    Box(
                        Modifier
                            .renderInSharedTransitionScopeOverlay(
                                zIndexInOverlay = 1f
                            )
                            .background(Color.Green)
                            .fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .wrapContentSize(align = Alignment.BottomEnd),
                        ) {
                            Box(
                                Modifier
                                    .sharedBounds(
                                        rememberSharedContentState(key = "child"),
                                        this@AnimatedVisibility
                                    )
                                    .fillMaxSize(0.5f)
                                    .background(Color.Red)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
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
                    assertPixels {
                        Color.Green
                    }
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
        val predefinedPath = Path().apply {
            addRect(Rect(5f, 5f, 6f, 6f))
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SharedTransitionLayout(
                    Modifier
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        parentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            childSharedContentState = it
                                        },
                                        this@AnimatedVisibility
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(0.5f)
                            .offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .offset(20.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        clippedParentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition = object :
                                        SharedTransitionScope.OverlayClip {
                                        override fun getClipPath(
                                            state: SharedTransitionScope.SharedContentState,
                                            bounds: Rect,
                                            layoutDirection: LayoutDirection,
                                            density: Density
                                        ): Path? {
                                            return predefinedPath
                                        }
                                    }
                                )) {
                            Box(
                                Modifier
                                    .offset(-20.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            clippedChildSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize(),
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
                SharedTransitionLayout(
                    Modifier
                        .requiredSize(100.dp)
                        .background(Color.White)
                ) {
                    transitionScope = this
                    AnimatedVisibility(
                        visible = visible,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        parentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.5f)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            childSharedContentState = it
                                        },
                                        this@AnimatedVisibility
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(0.5f)
                            .offset(x = 25.dp, y = 25.dp),
                        visible = !visible,
                        enter = fadeIn(tween(100)),
                        exit = ExitTransition.None
                    ) {
                        Box(
                            Modifier
                                .offset(20.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "parent").also {
                                        clippedParentSharedContentState = it
                                    },
                                    this@AnimatedVisibility,
                                )
                        ) {
                            Box(
                                Modifier
                                    .offset(-20.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = "child").also {
                                            clippedChildSharedContentState = it
                                        },
                                        this@AnimatedVisibility,
                                    )
                                    .fillMaxSize(),
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
                childSharedContentState!!.parentSharedContentState
            )
            assertNotNull(clippedParentSharedContentState)
            assertEquals(
                clippedParentSharedContentState,
                clippedChildSharedContentState!!.parentSharedContentState
            )

            assertTrue(parentSharedContentState!!.isMatchFound)
            assertTrue(childSharedContentState!!.isMatchFound)
            assertTrue(clippedParentSharedContentState!!.isMatchFound)
            assertTrue(clippedChildSharedContentState!!.isMatchFound)
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
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
