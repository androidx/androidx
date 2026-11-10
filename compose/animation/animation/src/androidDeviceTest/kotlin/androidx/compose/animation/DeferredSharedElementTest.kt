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

package androidx.compose.animation

import androidx.annotation.RequiresApi
import androidx.compose.animation.SharedTransitionScope.DeferredTransitionBehavior
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalSharedTransitionApi::class)
class DeferredSharedElementTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        testTimeSource = { rule.mainClock.currentTime }
    }

    @After
    fun tearDown() {
        testTimeSource = null
    }

    /**
     * Returns the bounds of the red pixels in the "scope" node, or [IntRect.Zero] if there are
     * none.
     */
    @RequiresApi(26)
    private fun getBounds(): IntRect {
        val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
        var minX = pixelMap.width
        var maxX = -1
        var minY = pixelMap.height
        var maxY = -1
        for (y in 0 until pixelMap.height) {
            for (x in 0 until pixelMap.width) {
                val color = pixelMap[x, y]
                if (color.red > 0.5f && color.green < 0.1f) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }
            }
        }
        return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDetachedPreview() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                scale = previewScale
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(
                                            "shared",
                                            SharedContentConfig(
                                                deferredTransitionBehavior =
                                                    DeferredTransitionBehavior.Static
                                            ),
                                        ),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState(
                                            "shared",
                                            SharedContentConfig(
                                                deferredTransitionBehavior =
                                                    DeferredTransitionBehavior.Static
                                            ),
                                        ),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // Switch to A, but hold in preview
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
        rule.waitForIdle()

        // Since the shared element is detached, it should NOT be scaled by the parent's preview
        // scale.
        // It should still be rendered at 200x200 (its unscaled approach size).
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 0 until 200 && pos.y in 0 until 200) {
                    Color.Red
                } else if (pos.x in 200 until 300 || pos.y in 200 until 300) {
                    Color.White
                } else null
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testAttachedPreview() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)
        var previewOffsetX by mutableStateOf(0)
        var previewOffsetY by mutableStateOf(0)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                scale = previewScale
                                offset = IntOffset(previewOffsetX, previewOffsetY)
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch to A, but hold in preview
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
        previewOffsetX = 10
        previewOffsetY = 20
        rule.waitForIdle()

        // Scale 0.5 with origin (150, 150):
        //   Top-left: (0, 0) -> ( (0 - 150) * 0.5 + 150, (0 - 150) * 0.5 + 150 ) = (75, 75)
        //   Bottom-right: (200, 200) -> ( (200 - 150) * 0.5 + 150, (200 - 150) * 0.5 + 150 ) =
        // (175, 175)
        // Offset (10, 20):
        //   Top-left: (75 + 10, 75 + 20) = (85, 95)
        //   Bottom-right: (175 + 10, 175 + 20) = (185, 195)
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 85 until 185 && pos.y in 95 until 195) {
                    Color.Red
                } else if (pos.x in 0 until 300 && pos.y in 0 until 300) {
                    Color.White
                } else null
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testAttachedPreviewWithOffCenterOrigin() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                // Scale origin at (1, 1), which is (300, 300) in px.
                                // This is outside the shared element's bounds (0, 0, 200, 200)
                                // but inside the parent's bounds (0, 0, 300, 300).
                                transformOrigin = TransformOrigin(1f, 1f)
                                scale = previewScale
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch to A, but hold in preview
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
        rule.waitForIdle()

        // Scale 0.5 with origin (300, 300):
        //   Top-left: (0, 0) -> ( (0 - 300) * 0.5 + 300, (0 - 300) * 0.5 + 300 ) = (150, 150)
        //   Bottom-right: (200, 200) -> ( (200 - 300) * 0.5 + 300, (200 - 300) * 0.5 + 300 ) =
        // (250, 250)
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 150 until 250 && pos.y in 150 until 250) {
                    Color.Red
                } else if (pos.x in 0 until 300 && pos.y in 0 until 300) {
                    Color.White
                } else null
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testHandoffPreview() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState) {
                        MutableContentTransform {
                            if (state.pendingTargetState != null) {
                                initialContentTransform {
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    scale = previewScale
                                }
                            }
                        }
                    }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    // Long transition to ensure we can catch the first frame after handoff
                    transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch to A, but hold in preview
        rule.runOnIdle {
            myState?.defer("A")
            previewScale = 0.5f // Scale the parent AnimatedContent down by half
        }
        rule.waitForIdle()

        // Scale 0.5 with origin (150, 150): (75, 75) to (175, 175)
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 75 until 175 && pos.y in 75 until 175) {
                    Color.Red
                } else if (pos.x in 0 until 300 && pos.y in 0 until 300) {
                    Color.White
                } else null
            }
        }

        // Commit the transition
        rule.mainClock.autoAdvance = false

        try {
            // Re-instantiate locally to avoid error (though it's bad test design, just to make it
            // compile)
            // Wait, state is actually declared as lateinit var, but inside setContent it's shadowed
            // or not accessible?
            // Actually, `val state = remember` was used inside `SharedTransitionLayout`.
            // Let's modify the file to use `state.animateTo` but let's declare `lateinit var
            // myState` outside setContent
            rule.runOnIdle {
                myState!!.animateTo(myState!!.pendingTargetState ?: myState!!.targetState)
            }
            rule.waitForIdle()

            // Advance just one frame to reach the handoff frame without progressing the animation
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()

            fun getBounds(): IntRect {
                val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
                var minX = pixelMap.width
                var maxX = -1
                var minY = pixelMap.height
                var maxY = -1
                for (y in 0 until pixelMap.height) {
                    for (x in 0 until pixelMap.width) {
                        val pixelColor = pixelMap[x, y]
                        if (
                            pixelColor.red > 0.9f &&
                                pixelColor.green < 0.1f &&
                                pixelColor.blue < 0.1f
                        ) {
                            minX = minOf(minX, x)
                            maxX = maxOf(maxX, x)
                            minY = minOf(minY, y)
                            maxY = maxOf(maxY, y)
                        }
                    }
                }

                return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
            }

            // In the handoff frame, the shared element should STILL be at the previewed position
            // (75, 75) to (175, 175)
            // because the animation starts FROM there.
            val handoffBounds = getBounds()
            val expectedHandoffBounds = IntRect(IntOffset(75, 75), IntSize(100, 100))
            if (handoffBounds != expectedHandoffBounds) {
                throw AssertionError(
                    "handoff preview check failed: expected $expectedHandoffBounds but was $handoffBounds."
                )
            }

            // One frame after handoff
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val tPlus1Bounds = getBounds()
            if (
                tPlus1Bounds.left >= handoffBounds.left ||
                    tPlus1Bounds.top >= handoffBounds.top ||
                    tPlus1Bounds.left < 0 ||
                    tPlus1Bounds.top < 0
            ) {
                throw AssertionError(
                    "tPlus1 check failed: expected bounds to be between $handoffBounds and (0, 0, 100, 100) but was $tPlus1Bounds"
                )
            }

            // Three frames after handoff
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val tPlus3Bounds = getBounds()
            if (
                tPlus3Bounds.left >= tPlus1Bounds.left ||
                    tPlus3Bounds.top >= tPlus1Bounds.top ||
                    tPlus3Bounds.left < 0 ||
                    tPlus3Bounds.top < 0
            ) {
                throw AssertionError(
                    "tPlus3 check failed: expected bounds to be between $tPlus1Bounds and (0, 0, 100, 100) but was $tPlus3Bounds"
                )
            }

            // End of animation
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()
            val endBounds = getBounds()
            val expectedEndBounds = IntRect(IntOffset(0, 0), IntSize(100, 100))
            if (endBounds != expectedEndBounds) {
                throw AssertionError(
                    "end check failed: expected $expectedEndBounds but was $endBounds"
                )
            }
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSimpleAttachedPreview() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"

        rule.setContent {
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                transition.DeferredAnimatedContent { state ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState("shared"),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(px200)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch state, hold in preview
        myState?.defer("A")
        rule.waitForIdle()

        // It should be rendered at (0, 0, 200, 200)
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 0 until 200 && pos.y in 0 until 200) {
                    Color.Red
                } else if (pos.x in 200 until 300 || pos.y in 200 until 300) {
                    Color.White
                } else null
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testAttachedPreviewWithOffset() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px400 = with(LocalDensity.current) { 400.toDp() }
            val px50 = with(LocalDensity.current) { 50.toDp() }

            SharedTransitionLayout(Modifier.size(px400).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState, previewScale) {
                        MutableContentTransform {
                            if (state.pendingTargetState != null) {
                                // Pivot at center of 400x400 parent = (200, 200)
                                initialContentTransform {
                                    transformOrigin = TransformOrigin.Center
                                    scale = previewScale
                                }
                            }
                        }
                    }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                // Offset the shared element from the parent's origin by (50, 50) px
                                .offset(px50, px50)
                                .sharedBounds(
                                    rememberSharedContentState("shared"),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(px100)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch state, hold in preview
        myState?.defer("A")
        previewScale = 0.5f
        rule.waitForIdle()

        // Parent is 400x400 (px), center is (200, 200).
        // Shared element at (50, 50) with size 100x100.
        // Bounds in root: (50, 50, 150, 150).
        // Scale 0.5 around pivot (200, 200):
        //   newTopLeft = (topLeft - pivot) * scale + pivot
        //   newTopLeft = ((50, 50) - (200, 200)) * 0.5 + (200, 200)
        //              = (-150, -150) * 0.5 + (200, 200)
        //              = (-75, -75) + (200, 200) = (125, 125)
        //   newBottomRight = ((150, 150) - (200, 200)) * 0.5 + (200, 200)
        //                  = (-50, -50) * 0.5 + (200, 200)
        //                  = (-25, -25) + (200, 200) = (175, 175)
        // Resulting Rect: (125, 125, 175, 175).

        rule.onNodeWithTag("scope").captureToImage().run {
            val pixelMap = toPixelMap()
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixelColor = pixelMap[x, y]
                    if (
                        pixelColor.red > 0.9f && pixelColor.green < 0.1f && pixelColor.blue < 0.1f
                    ) {
                        minX = minOf(minX, x)
                        maxX = maxOf(maxX, x)
                        minY = minOf(minY, y)
                        maxY = maxOf(maxY, y)
                    }
                }
            }

            val actualBounds =
                if (maxX == -1) "None"
                else "position ($minX, $minY), width ${maxX - minX + 1}, height ${maxY - minY + 1}"
            val expectedBounds = "position (125, 125), width 50, height 50"

            if (minX != 125 || minY != 125 || (maxX - minX + 1) != 50 || (maxY - minY + 1) != 50) {
                throw AssertionError(
                    "attached preview offset check failed: expected $expectedBounds but was $actualBounds"
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testHandoffVelocity() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        fun getBounds(): IntRect {
            val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
            var minX = pixelMap.width
            var maxX = -1
            var minY = pixelMap.height
            var maxY = -1
            for (y in 0 until pixelMap.height) {
                for (x in 0 until pixelMap.width) {
                    val pixelColor = pixelMap[x, y]
                    if (
                        pixelColor.red > 0.9f && pixelColor.green < 0.1f && pixelColor.blue < 0.1f
                    ) {
                        minX = minOf(minX, x)
                        maxX = maxOf(maxX, x)
                        minY = minOf(minY, y)
                        maxY = maxOf(maxY, y)
                    }
                }
            }

            return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
        }

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState) {
                        MutableContentTransform {
                            if (state.pendingTargetState != null) {
                                initialContentTransform { scale = previewScale }
                            }
                        }
                    }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith
                            fadeOut(
                                spring(
                                    stiffness = Spring.StiffnessVeryLow,
                                    visibilityThreshold = null,
                                )
                            )
                    },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.mainClock.autoAdvance = false

        fun simulateGesture(isFast: Boolean): IntRect {
            rule.runOnIdle { myState?.defer("A") }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()

            val steps =
                if (isFast) {
                    listOf(0.9f, 0.7f, 0.5f)
                } else {
                    listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
                }
            for (s in steps) {
                rule.mainClock.advanceTimeByFrame()
                rule.runOnIdle { previewScale = s }
                rule.waitForIdle()
            }

            rule.runOnIdle { myState!!.animateTo("A") }
            repeat(3) { rule.mainClock.advanceTimeByFrame() }
            rule.waitForIdle()

            return getBounds()
        }

        // Scenario 1: Slow gesture
        val boundsSlow = simulateGesture(isFast = false)

        // Reset state
        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            previewScale = 1f
            myState!!.animateTo("B")
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // Scenario 2: Fast gesture
        val boundsFast = simulateGesture(isFast = true)

        // With fast gesture (more negative velocity), it should be significantly smaller
        assertTrue(
            "Expected $boundsFast to be smaller than $boundsSlow",
            boundsFast.width < boundsSlow.width && boundsFast.height < boundsSlow.height,
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testCancelDeferredPhase_doesNotJumpToIncomingState() {
        var myState: DeferredTransitionState<String>? = null
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("A") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                scale = previewScale
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = {
                        if (targetState == "B") {
                            slideInHorizontally { it } togetherWith fadeOut(tween(100))
                        } else {
                            fadeIn(tween(100)) togetherWith slideOutHorizontally { it }
                        }
                    },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .testTag("shared")
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch to B, but hold in preview
        myState?.defer("B")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
        rule.waitForIdle()

        // Cancel the gesture (back to A)
        rule.mainClock.autoAdvance = false
        myState?.animateTo("A")

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val sharedElementBounds = rule.onNodeWithTag("shared").fetchSemanticsNode().boundsInWindow

        // If the shared element erroneously jumps to B's bounds due to the state inversion bug,
        // it will pick up B's slideInHorizontally offset (which is +300px).
        // Since B is scaled by 0.5 around the center (150, 150), its 200x200 box starts at 0, 0
        // local.
        // Global translation for B: offset +300 means it jumps far to the right.
        // We assert that the shared element's left bound is close to 0 (scaled down), and
        // definitely not > 150.
        assertTrue(
            "Shared element jumped to incoming screen's bounds. Left bound was: ${sharedElementBounds.left}",
            sharedElementBounds.left < 150f,
        )

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testInterruptionHandoff_noJump() {
        var myState: DeferredTransitionState<String>? = null
        var previewOffset by mutableStateOf(IntOffset.Zero)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px400 = with(LocalDensity.current) { 400.toDp() }

            SharedTransitionLayout(Modifier.size(px400).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("A") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState == "A") {
                                offset = previewOffset
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = {
                        slideInHorizontally(tween(2000)) { -it / 2 } togetherWith
                            slideOutHorizontally(tween(2000)) { it / 2 }
                    },
                ) { state ->
                    Box(Modifier.fillMaxSize()) {
                        if (state == "A") {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.offset(px200, px200)
                                    .sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        fun getBounds(): IntRect {
            val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
            var minX = pixelMap.width
            var maxX = -1
            var minY = pixelMap.height
            var maxY = -1
            for (y in 0 until pixelMap.height) {
                for (x in 0 until pixelMap.width) {
                    val color = pixelMap[x, y]
                    if (color.red > 0.5f && color.green < 0.1f) {
                        minX = minOf(minX, x)
                        maxX = maxOf(maxX, x)
                        minY = minOf(minY, y)
                        maxY = maxOf(maxY, y)
                    }
                }
            }
            return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
        }

        // 1. Start forward transition A -> B
        rule.mainClock.autoAdvance = false
        myState?.animateTo("B")

        // Advance 500ms
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()

        // 2. Interrupt with back gesture B -> A (Deferred)
        myState?.defer("A")
        previewOffset = IntOffset(100, 100)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val boundsDuringDeferred = getBounds()

        // 3. Commit back gesture B -> A
        myState?.animateTo("A")

        // Advance one frame for handoff
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val boundsAfterCommit = getBounds()

        // Verify no jump
        assertTrue(
            "Shared element jumped after commit! " +
                "Before: $boundsDuringDeferred, After: $boundsAfterCommit",
            boundsDuringDeferred == boundsAfterCommit,
        )

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDeferredTransition_withRenderInOverlayFalse_scalesWithParent() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }
            val px50 = with(LocalDensity.current) { 50.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                transformOrigin = TransformOrigin(0f, 0f) // Top-left origin
                                scale = previewScale
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.offset(px50, px50)
                                    .sharedBounds(
                                        rememberSharedContentState(
                                            "shared",
                                            SharedContentConfig(
                                                deferredTransitionBehavior =
                                                    DeferredTransitionBehavior.Static
                                            ),
                                        ),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                        renderInOverlayDuringTransition = false,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.offset(px50, px50)
                                    .sharedBounds(
                                        rememberSharedContentState(
                                            "shared",
                                            SharedContentConfig(
                                                deferredTransitionBehavior =
                                                    DeferredTransitionBehavior.Static
                                            ),
                                        ),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                        renderInOverlayDuringTransition = false,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // Switch to A, but hold in preview
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
        rule.waitForIdle()

        // Since renderInOverlayDuringTransition = false, the shared element is physically inside
        // the parent. It SHOULD scale down by 50% even though
        // deferredTransitionBehavior = Static.
        // It starts at size 200x200 with offset (50, 50). So unscaled bounds are (50, 50) to (250,
        // 250).
        // With 50% scale around (0,0), it should visually be 100x100 at (25, 25).
        rule.onNodeWithTag("scope").captureToImage().run {
            assertPixels { pos ->
                if (pos.x in 25 until 125 && pos.y in 25 until 125) {
                    Color.Red
                } else if (pos.x in 0 until 300 && pos.y in 0 until 300) {
                    Color.White
                } else null
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDeferredHandoff_withRenderInOverlayFalse_doesNotJump() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        fun getBounds(): IntRect {
            val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
            var minX = pixelMap.width
            var maxX = -1
            var minY = pixelMap.height
            var maxY = -1
            for (y in 0 until pixelMap.height) {
                for (x in 0 until pixelMap.width) {
                    val pixelColor = pixelMap[x, y]
                    if (
                        pixelColor.red > 0.9f && pixelColor.green < 0.1f && pixelColor.blue < 0.1f
                    ) {
                        minX = minOf(minX, x)
                        maxX = maxOf(maxX, x)
                        minY = minOf(minY, y)
                        maxY = maxOf(maxY, y)
                    }
                }
            }

            return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
        }

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }
            val px50 = with(LocalDensity.current) { 50.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState, previewScale) {
                        MutableContentTransform {
                            initialContentTransform {
                                if (state.pendingTargetState != null) {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scale = previewScale
                                }
                            }
                        }
                    }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.offset(px50, px50)
                                    .sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                        renderInOverlayDuringTransition = false,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.offset(px50, px50)
                                    .sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                        renderInOverlayDuringTransition = false,
                                    )
                                    .size(px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Switch to A, but hold in preview
        rule.runOnIdle {
            myState?.defer("A")
            previewScale = 0.5f // Scale the parent AnimatedContent down by half
        }
        rule.waitForIdle()

        val handoffBounds = getBounds()

        // Commit the transition
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { myState!!.animateTo("A") }
        rule.waitForIdle()

        // Advance just one frame to reach the handoff frame without progressing the animation
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // In the handoff frame, the shared element should exactly match the gesture bounds
        // (no jumping out of sync)
        val tPlus1Bounds = getBounds()

        if (handoffBounds != tPlus1Bounds) {
            throw AssertionError(
                "handoff check failed: bounds jumped! Expected $handoffBounds but was $tPlus1Bounds."
            )
        }

        // End of animation. Unscaled end position should be offset by 50.
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val endBounds = getBounds()
        val expectedEndBounds = IntRect(IntOffset(50, 50), IntSize(100, 100))
        if (endBounds != expectedEndBounds) {
            throw AssertionError("end check failed: expected $expectedEndBounds but was $endBounds")
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun incomingElement_alignsWithOutgoing_whenRenderingInPlace_atDeferredHandoff() {
        var myState: DeferredTransitionState<String>? = null
        val targetState = "B"
        var previewScale by mutableStateOf(1f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).background(Color.White)) {
                val state = remember { DeferredTransitionState(targetState) }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState, previewScale) {
                        MutableContentTransform {
                            if (state.pendingTargetState != null) {
                                initialContentTransform {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scale = previewScale
                                }
                            }
                        }
                    }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "A") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                        renderInOverlayDuringTransition = false,
                                    )
                                    .testTag("incoming")
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .testTag("outgoing")
                                    .size(px200)
                                    .background(Color.Blue)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { myState?.defer("A") }
        rule.mainClock.advanceTimeByFrame()

        previewScale = 0.5f
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        rule.runOnIdle { myState!!.animateTo("A") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val incomingBounds = rule.onNodeWithTag("incoming").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected bounds width to be 100.0 (half of outgoing size 200) at handoff, " +
                "but was ${incomingBounds.width}",
            incomingBounds.width == 100.0f,
        )
        assertTrue(
            "Expected bounds top left to be (0.0, 0.0), but was ${incomingBounds.topLeft}",
            incomingBounds.topLeft.x == 0.0f && incomingBounds.topLeft.y == 0.0f,
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDeferredTransition_cancellationDoesNotJump() {
        var myState: DeferredTransitionState<String>? = null

        fun getBounds(): IntRect {
            val pixelMap = rule.onNodeWithTag("scope").captureToImage().toPixelMap()
            var minX = pixelMap.width
            var maxX = -1
            var minY = pixelMap.height
            var maxY = -1
            for (y in 0 until pixelMap.height) {
                for (x in 0 until pixelMap.width) {
                    val pixelColor = pixelMap[x, y]
                    if (
                        pixelColor.red > 0.9f && pixelColor.green < 0.1f && pixelColor.blue < 0.1f
                    ) {
                        minX = minOf(minX, x)
                        maxX = maxOf(maxX, x)
                        minY = minOf(minY, y)
                        maxY = maxOf(maxY, y)
                    }
                }
            }

            return if (maxX == -1) IntRect.Zero else IntRect(minX, minY, maxX + 1, maxY + 1)
        }

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("Detail") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            // Detail screen transform
                            offset = IntOffset.Zero
                        }
                        targetContentTransform {
                            // Main screen transform (parallax offset to the right by 50px)
                            offset = IntOffset(50, 0)
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { state ->
                    if (state == "Detail") {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // 1. Start the back gesture: transition to "Main" but defer it
        myState?.defer("Main")
        rule.waitForIdle()

        // Verify that the shared element is positioned at Detail's position (no horizontal offset)
        // Detail is at (0, 0), and size is 100.
        val detailBoundsBeforeCancel = getBounds()
        assertTrue("Detail bounds width should be 100", detailBoundsBeforeCancel.width == 100)
        assertTrue("Detail bounds left should be 0", detailBoundsBeforeCancel.left == 0)

        // 2. Cancel the gesture: animate back to "Detail"
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState!!.animateTo("Detail") }
        rule.waitForIdle()

        // Advance just one frame to reach the handoff frame without progressing the animation
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // In the handoff frame, the shared element should NOT jump.
        // If it jumps to the right (matching Main's parallax offset of 50), left will be 50.
        // It should stay at 0.
        val detailBoundsAfterCancel = getBounds()
        assertTrue(
            "Detail bounds left after cancellation should be 0, but was ${detailBoundsAfterCancel.left}",
            detailBoundsAfterCancel.left == 0,
        )
    }

    @Test
    fun testSharedElementNestedUnderAnimateEnterExit_preservesDeferredTransformAtHandoff() {
        val state = DeferredTransitionState("A")
        val scale = mutableFloatStateOf(1f)

        rule.setContent {
            val px20 = with(LocalDensity.current) { 20.toDp() }
            val px40 = with(LocalDensity.current) { 40.toDp() }
            val px60 = with(LocalDensity.current) { 60.toDp() }
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }

            val transition = rememberDeferredTransition(state)
            SharedTransitionLayout(Modifier.size(px200)) {
                transition.DeferredAnimatedContent(
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    mutableTransformSpec = {
                        MutableContentTransform {
                            initialContentTransform { this.scale = scale.floatValue }
                        }
                    },
                ) { target ->
                    Box(
                        Modifier.fillMaxSize()
                            .animateEnterExit(fadeIn(tween(300)), fadeOut(tween(300)))
                    ) {
                        Box(
                            Modifier.offset(
                                    x = if (target == "A") px20 else px60,
                                    y = if (target == "A") px60 else px100,
                                )
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("shared"),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                    boundsTransform = { _, _ -> tween(300) },
                                )
                                .size(px40)
                                .testTag("shared-$target")
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // 1. Enter deferred phase and scale outgoing content to 0.5x around center (100, 100).
        // Untransformed A is at (20, 60) with size 40x40:
        //   left = (20 - 100) * 0.5 + 100 = 60
        //   top = (60 - 100) * 0.5 + 100 = 80
        //   width = 40 * 0.5 = 20
        rule.runOnIdle {
            state.defer("B")
            scale.floatValue = 0.5f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val deferredBoundsA = rule.onNodeWithTag("shared-A").fetchSemanticsNode().boundsInRoot
        assertEquals(60f, deferredBoundsA.left, 1f)
        assertEquals(80f, deferredBoundsA.top, 1f)
        assertEquals(20f, deferredBoundsA.width, 1f)

        // 2. Commit transition to B and verify handoff starts from the transformed bounds (60, 80)
        // with width 20, rather than snapping back to untransformed (20, 60) with width 40.
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val commitBoundsA = rule.onNodeWithTag("shared-A").fetchSemanticsNode().boundsInRoot
        val commitBoundsB = rule.onNodeWithTag("shared-B").fetchSemanticsNode().boundsInRoot
        assertEquals(60f, commitBoundsA.left, 1f)
        assertEquals(80f, commitBoundsA.top, 1f)
        assertEquals(20f, commitBoundsA.width, 1f)
        assertEquals(60f, commitBoundsB.left, 1f)
        assertEquals(80f, commitBoundsB.top, 1f)
        assertEquals(20f, commitBoundsB.width, 1f)
    }

    @Test
    fun testSharedElementNestedUnderSharedBounds_preservesDeferredTransformAtHandoff() {
        val state = DeferredTransitionState("A")
        val scale = mutableFloatStateOf(1f)

        rule.setContent {
            val px20 = with(LocalDensity.current) { 20.toDp() }
            val px40 = with(LocalDensity.current) { 40.toDp() }
            val px60 = with(LocalDensity.current) { 60.toDp() }
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }

            val transition = rememberDeferredTransition(state)
            SharedTransitionLayout(Modifier.size(px200)) {
                transition.DeferredAnimatedContent(
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    mutableTransformSpec = {
                        MutableContentTransform {
                            initialContentTransform { this.scale = scale.floatValue }
                        }
                    },
                ) { target ->
                    Box(
                        Modifier.fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("bounds"),
                                animatedVisibilityScope = this@DeferredAnimatedContent,
                                enter = fadeIn(tween(300)),
                                exit = fadeOut(tween(300)),
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            )
                    ) {
                        Box(
                            Modifier.offset(
                                    x = if (target == "A") px20 else px60,
                                    y = if (target == "A") px60 else px100,
                                )
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("shared"),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                    boundsTransform = { _, _ -> tween(300) },
                                )
                                .size(px40)
                                .testTag("shared-$target")
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // 1. Enter deferred phase and scale outgoing content to 0.5x around center (100, 100).
        // Untransformed A is at (20, 60) with size 40x40:
        //   left = (20 - 100) * 0.5 + 100 = 60
        //   top = (60 - 100) * 0.5 + 100 = 80
        //   width = 40 * 0.5 = 20
        rule.runOnIdle {
            state.defer("B")
            scale.floatValue = 0.5f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val deferredBoundsA = rule.onNodeWithTag("shared-A").fetchSemanticsNode().boundsInRoot
        assertEquals(60f, deferredBoundsA.left, 1f)
        assertEquals(80f, deferredBoundsA.top, 1f)
        assertEquals(20f, deferredBoundsA.width, 1f)

        // 2. Commit transition to B and verify handoff starts from the transformed bounds (60, 80)
        // with width 20, rather than snapping back to untransformed (20, 60) with width 40.
        rule.runOnIdle { state.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val commitBoundsA = rule.onNodeWithTag("shared-A").fetchSemanticsNode().boundsInRoot
        val commitBoundsB = rule.onNodeWithTag("shared-B").fetchSemanticsNode().boundsInRoot
        assertEquals(60f, commitBoundsA.left, 1f)
        assertEquals(80f, commitBoundsA.top, 1f)
        assertEquals(20f, commitBoundsA.width, 1f)
        assertEquals(60f, commitBoundsB.left, 1f)
        assertEquals(80f, commitBoundsB.top, 1f)
        assertEquals(20f, commitBoundsB.width, 1f)
    }

    private class IncomingMovableContentTestState {
        var deferredState: DeferredTransitionState<String>? = null
        var useMutableTransform by mutableStateOf(false)
        var previewScale by mutableFloatStateOf(1f)
        var isTransitionActive = false
    }

    /**
     * Sets up a deferred transition from B to A. The shared element is 100x100 at x = 100..200 in B
     * and 50x50 at x = 0..50 in A, followed by "sibling-A". The shared element's content is a
     * [movableContentOf] that is only composed in the incoming (or visible) content. The shared
     * element opts out of deferred transforms (`DeferredTransitionBehavior.Static`).
     */
    private fun setUpIncomingMovableContent(): IncomingMovableContentTestState {
        val testState = IncomingMovableContentTestState()
        rule.setContent {
            val px50 = with(LocalDensity.current) { 50.toDp() }
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            val movableContent = remember {
                movableContentOf { Box(Modifier.fillMaxSize().background(Color.Red)) }
            }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("B") }
                testState.deferredState = state
                val transition = rememberDeferredTransition(state)
                testState.isTransitionActive = isTransitionActive
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                scale = testState.previewScale
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = {
                        if (testState.useMutableTransform) mutableTransform else null
                    },
                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                ) { target ->
                    val isIncomingOrVisible =
                        (this.transition.pendingTargetState ?: this.transition.targetState) ==
                            EnterExitState.Visible
                    Row(Modifier.fillMaxSize()) {
                        if (target == "B") {
                            Box(Modifier.size(px100))
                        }
                        Box(
                            Modifier.sharedElement(
                                    rememberSharedContentState(
                                        "shared",
                                        SharedContentConfig(
                                            deferredTransitionBehavior =
                                                SharedTransitionScope.DeferredTransitionBehavior
                                                    .Static
                                        ),
                                    ),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(if (target == "B") px100 else px50)
                        ) {
                            if (isIncomingOrVisible) {
                                movableContent()
                            }
                        }
                        if (target == "A") {
                            Box(Modifier.weight(1f).fillMaxSize().testTag("sibling-A"))
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        return testState
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElement_deferredPhase_doesNotActivateTransitionScope() {
        val testState = setUpIncomingMovableContent()

        rule.runOnIdle { testState.deferredState?.defer("A") }
        rule.waitForIdle()
        assertFalse(
            "Expected SharedTransitionScope.isTransitionActive to be false during deferred phase " +
                "when mutableTransformSpec is null",
            testState.isTransitionActive,
        )

        rule.runOnIdle {
            testState.useMutableTransform = true
            testState.previewScale = 0.5f
        }
        rule.waitForIdle()
        assertFalse(
            "Expected SharedTransitionScope.isTransitionActive to remain false during deferred " +
                "phase when deferredTransitionBehavior is Static",
            testState.isTransitionActive,
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElement_rendersIncomingMovableContentAtOutgoingBoundsDuringDeferredPhase() {
        val testState = setUpIncomingMovableContent()

        // Defer transition from B to A, initially with mutableTransformSpec returning null.
        rule.runOnIdle { testState.deferredState?.defer("A") }
        rule.waitForIdle()

        // 1. Incoming sibling in A should already be laid out next to A's 50px placeholder
        // (left = 50, width = 250), rather than taking up the full 300px viewport.
        val siblingBoundsDuringDefer =
            rule.onNodeWithTag("sibling-A").fetchSemanticsNode().boundsInRoot
        assertEquals(50f, siblingBoundsDuringDefer.left, 1f)
        assertEquals(250f, siblingBoundsDuringDefer.width, 1f)

        // 2. Even when mutableTransformSpec is null, the incoming content renders in the overlay
        // with outgoing B's position and size (x = 100..200, y = 0..100), rather than with A's
        // 50x50 bounds or vanishing inside A's hidden PreEnter container.
        assertEquals(IntRect(100, 0, 200, 100), getBounds())

        // 3. Enable mutableTransform scaling and verify the shared element still remains at
        // outgoing B's unscaled bounds.
        rule.runOnIdle {
            testState.useMutableTransform = true
            testState.previewScale = 0.5f
        }
        rule.waitForIdle()
        assertEquals(IntRect(100, 0, 200, 100), getBounds())

        // 4. Commit to A and verify it settles at A's bounds (x = 0..50, y = 0..50) with no
        // sibling layout jump.
        rule.runOnIdle { testState.deferredState?.animateTo("A") }
        rule.waitForIdle()

        val siblingBoundsAfterCommit =
            rule.onNodeWithTag("sibling-A").fetchSemanticsNode().boundsInRoot
        assertEquals(50f, siblingBoundsAfterCommit.left, 1f)
        assertEquals(250f, siblingBoundsAfterCommit.width, 1f)
        assertEquals(IntRect(0, 0, 50, 50), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testInterruptionHandoff_holdUntilForwardFinishesAndRepeat_noJump() {
        var myState: DeferredTransitionState<String>? = null
        var previewOffset by mutableStateOf(IntOffset.Zero)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px400 = with(LocalDensity.current) { 400.toDp() }

            SharedTransitionLayout(Modifier.size(px400).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("A") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState == "A") {
                                offset = previewOffset
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = {
                        slideInHorizontally(tween(300)) { -it / 2 } togetherWith
                            slideOutHorizontally(tween(300)) { it / 2 }
                    },
                ) { state ->
                    Box(Modifier.fillMaxSize()) {
                        if (state == "A") {
                            Box(
                                Modifier.sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        } else {
                            Box(
                                Modifier.offset(px200, px200)
                                    .sharedBounds(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // First gesture cycle: navigate to B, then defer and commit back to A so B is disposed
        rule.runOnIdle { myState?.animateTo("B") }
        rule.waitForIdle()
        rule.runOnIdle {
            myState?.defer("A")
            previewOffset = IntOffset(50, 50)
        }
        rule.waitForIdle()
        rule.runOnIdle { myState?.animateTo("A") }
        rule.waitForIdle()
        rule.runOnIdle { previewOffset = IntOffset.Zero }

        // Second gesture cycle: start forward transition A -> B, interrupt mid-flight with
        // defer("A"), hold until A -> B finishes, then commit animateTo("A").
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState?.animateTo("B") }
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        rule.runOnIdle {
            myState?.defer("A")
            previewOffset = IntOffset(100, 100)
        }
        // Hold deferred gesture while forward A -> B transition finishes
        rule.mainClock.advanceTimeBy(2000)
        rule.waitForIdle()

        val boundsAtEndOfHold = getBounds()
        assertEquals(IntRect(300, 300, 400, 400), boundsAtEndOfHold)

        // Commit back gesture B -> A and verify no jump on the handoff frame
        rule.runOnIdle { myState?.animateTo("A") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val boundsAfterCommit = getBounds()
        assertEquals(boundsAtEndOfHold, boundsAfterCommit)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 100, 100), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElement_verticalScrollDuringDeferredPhaseAndCancel_tracksMfrOffset() {
        var myState: DeferredTransitionState<String>? = null
        var scrollState: ScrollState? = null
        var previewOffset by mutableStateOf(IntOffset.Zero)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px400 = with(LocalDensity.current) { 400.toDp() }
            val px600 = with(LocalDensity.current) { 600.toDp() }

            val scroll = rememberScrollState(initial = 40)
            scrollState = scroll

            SharedTransitionLayout(Modifier.size(px400).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("B") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        initialContentTransform {
                            if (state.pendingTargetState == "A") {
                                offset = previewOffset
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                ) { target ->
                    if (target == "B") {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                            Spacer(Modifier.size(px200))
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                            Spacer(Modifier.size(px600))
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.sharedElement(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Initial position on B with scroll = 40: y = 200 - 40 = 160..260
        assertEquals(IntRect(0, 160, 100, 260), getBounds())

        // Start deferred transition to A while already scrolled (initial MFR = -40)
        rule.runOnIdle {
            myState?.defer("A")
            previewOffset = IntOffset(20, 0)
        }
        rule.waitForIdle()
        assertEquals(IntRect(20, 160, 120, 260), getBounds())

        // Continue scrolling B's column during the deferred phase (scroll 40 -> 90, delta = +50)
        rule.runOnIdle { scrollState?.dispatchRawDelta(50f) }
        rule.waitForIdle()
        assertEquals(90, scrollState?.value)
        // Overlay should immediately reflect the 50px MFR scroll delta: y = 160 - 50 = 110..210
        assertEquals(IntRect(20, 110, 120, 210), getBounds())

        // Cancel the gesture back to B and verify there is no vertical jump from MFR mismatch
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState?.animateTo("B") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val boundsOnCancelFrame = getBounds()
        assertEquals(110, boundsOnCancelFrame.top)
        assertEquals(210, boundsOnCancelFrame.bottom)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntRect(0, 110, 100, 210), getBounds())

        // Defer again, scroll during the deferred phase (scroll 90 -> 140, delta = +50), then
        // commit to A and verify the handoff frame does not double-apply the MFR delta.
        rule.runOnIdle {
            myState?.defer("A")
            previewOffset = IntOffset(20, 0)
        }
        rule.waitForIdle()
        rule.runOnIdle { scrollState?.dispatchRawDelta(50f) }
        rule.waitForIdle()
        assertEquals(IntRect(20, 60, 120, 160), getBounds())

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState?.animateTo("A") }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(IntRect(20, 60, 120, 160), getBounds())

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 100, 100), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDetachedSeekingWithProgress() {
        var myState: DeferredTransitionState<String>? = null
        var previewProgress by mutableFloatStateOf(0f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("B") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        sharedElementProgress { previewProgress }
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                scale = 0.5f
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                ) { target ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(
                                        "shared",
                                        SharedContentConfig(
                                            deferredTransitionBehavior =
                                                DeferredTransitionBehavior.SeekToTarget
                                        ),
                                    ),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(if (target == "A") px100 else px200)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        // Start deferred transition B (200x200) -> A (100x100) and seek to 25% -> 175x175
        rule.runOnIdle {
            myState?.defer("A")
            previewProgress = 0.25f
        }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 175, 175), getBounds())

        // Seek to 50% on a subsequent frame -> 150x150 (must not compound-lerp)
        rule.runOnIdle { previewProgress = 0.5f }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 150, 150), getBounds())

        // Cancel back to B and verify it returns to B's resting bounds (200x200)
        rule.runOnIdle { myState?.animateTo("B") }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 200, 200), getBounds())

        // Defer again to A, seek to 50%, then commit to A and verify handoff starts from 150x150
        rule.runOnIdle {
            myState?.defer("A")
            previewProgress = 0.5f
        }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 150, 150), getBounds())

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState?.animateTo("A") }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 150, 150), getBounds())

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 100, 100), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testStaticBehaviorIgnoresSharedElementProgress() {
        var myState: DeferredTransitionState<String>? = null
        var previewProgress by mutableFloatStateOf(0f)

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("B") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        sharedElementProgress { previewProgress }
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                scale = 0.5f
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                ) { target ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.sharedBounds(
                                    rememberSharedContentState(
                                        "shared",
                                        SharedContentConfig(
                                            deferredTransitionBehavior =
                                                DeferredTransitionBehavior.Static
                                        ),
                                    ),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(if (target == "A") px100 else px200)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        // Start deferred transition B (200x200) -> A (100x100) with progress = 0.5f.
        // Because deferredTransitionBehavior is Static (not SeekToTarget), it must stay at 200x200.
        rule.runOnIdle {
            myState?.defer("A")
            previewProgress = 0.5f
        }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 200, 200), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElementSeeking_changesBothSizeAndPosition() {
        var myState: DeferredTransitionState<String>? = null
        var previewProgress by mutableFloatStateOf(0f)

        rule.setContent {
            val px0 = with(LocalDensity.current) { 0.toDp() }
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("B") }
                myState = state
                val transition = rememberDeferredTransition(state)
                val mutableTransform = remember {
                    MutableContentTransform {
                        sharedElementProgress { previewProgress }
                        initialContentTransform {
                            if (state.pendingTargetState != null) {
                                scale = 0.5f
                            }
                        }
                    }
                }
                transition.DeferredAnimatedContent(
                    mutableTransformSpec = { mutableTransform },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                ) { target ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.offset(
                                    x = if (target == "A") px100 else px0,
                                    y = if (target == "A") px100 else px0,
                                )
                                .sharedElement(
                                    rememberSharedContentState(
                                        "shared",
                                        SharedContentConfig(
                                            deferredTransitionBehavior =
                                                DeferredTransitionBehavior.SeekToTarget
                                        ),
                                    ),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                )
                                .size(if (target == "A") px100 else px200)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 200, 200), getBounds())

        // Start deferred transition B ((0, 0) 200x200) -> A ((100, 100) 100x100) and seek to 25%:
        // topLeft = (25, 25), size = 175x175 -> IntRect(25, 25, 200, 200)
        rule.runOnIdle {
            myState?.defer("A")
            previewProgress = 0.25f
        }
        rule.waitForIdle()
        assertEquals(IntRect(25, 25, 200, 200), getBounds())

        // Seek to 50% on a subsequent frame -> topLeft = (50, 50), size = 150x150
        rule.runOnIdle { previewProgress = 0.5f }
        rule.waitForIdle()
        assertEquals(IntRect(50, 50, 200, 200), getBounds())

        // Cancel back to B and verify it returns to B's resting bounds ((0, 0) 200x200)
        rule.runOnIdle { myState?.animateTo("B") }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 200, 200), getBounds())

        // Defer again to A, seek to 50%, then commit to A and verify handoff starts from
        // (50, 50) 150x150 and settles at (100, 100) 100x100
        rule.runOnIdle {
            myState?.defer("A")
            previewProgress = 0.5f
        }
        rule.waitForIdle()
        assertEquals(IntRect(50, 50, 200, 200), getBounds())

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { myState?.animateTo("A") }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(IntRect(50, 50, 200, 200), getBounds())

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(IntRect(100, 100, 200, 200), getBounds())
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testDetachedSeekingHandoffVelocity() {
        var myState: DeferredTransitionState<String>? = null
        var previewProgress by mutableFloatStateOf(0f)

        try {
            rule.setContent {
                val px0 = with(LocalDensity.current) { 0.toDp() }
                val px50 = with(LocalDensity.current) { 50.toDp() }
                val px100 = with(LocalDensity.current) { 100.toDp() }
                val px200 = with(LocalDensity.current) { 200.toDp() }
                val px300 = with(LocalDensity.current) { 300.toDp() }

                SharedTransitionLayout(
                    Modifier.size(px300).testTag("scope").background(Color.White)
                ) {
                    val state = remember { DeferredTransitionState("B") }
                    myState = state
                    val transition = rememberDeferredTransition(state)
                    val mutableTransform = remember {
                        MutableContentTransform { sharedElementProgress { previewProgress } }
                    }
                    transition.DeferredAnimatedContent(
                        mutableTransformSpec = { mutableTransform },
                        transitionSpec = {
                            fadeIn(tween(1000)) togetherWith
                                fadeOut(
                                    spring(
                                        stiffness = Spring.StiffnessVeryLow,
                                        visibilityThreshold = null,
                                    )
                                )
                        },
                    ) { target ->
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.offset(
                                        x = if (target == "A") px50 else px0,
                                        y = if (target == "A") px50 else px0,
                                    )
                                    .sharedBounds(
                                        rememberSharedContentState(
                                            "shared",
                                            SharedContentConfig(
                                                deferredTransitionBehavior =
                                                    DeferredTransitionBehavior.SeekToTarget
                                            ),
                                        ),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(if (target == "A") px100 else px200)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.autoAdvance = false

            fun simulateSeekGesture(isFast: Boolean): IntRect {
                rule.runOnIdle { myState?.defer("A") }
                rule.mainClock.advanceTimeByFrame()
                rule.waitForIdle()

                val steps =
                    if (isFast) {
                        listOf(0.1f, 0.3f, 0.5f)
                    } else {
                        listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
                    }
                for (p in steps) {
                    previewProgress = p
                    rule.mainClock.advanceTimeByFrame()
                    rule.waitForIdle()
                }

                rule.runOnIdle { myState!!.animateTo("A") }
                repeat(3) { rule.mainClock.advanceTimeByFrame() }
                rule.waitForIdle()

                return getBounds()
            }

            // Scenario 1: Slow seek gesture from B (0, 0, 200, 200) toward A (50, 50, 150, 150)
            val boundsSlow = simulateSeekGesture(isFast = false)

            // Reset state back to B
            rule.mainClock.autoAdvance = true
            rule.runOnIdle { myState!!.animateTo("B") }
            rule.waitForIdle()
            rule.mainClock.autoAdvance = false
            previewProgress = 0f

            // Scenario 2: Fast seek gesture
            val boundsFast = simulateSeekGesture(isFast = true)

            // With higher progress velocity toward A (50, 50, 150, 150), all four edges
            // should have moved further toward A's bounds at t+3 frames.
            assertTrue(
                "Expected $boundsFast to be further toward (50, 50, 150, 150) than $boundsSlow",
                boundsFast.left > boundsSlow.left &&
                    boundsFast.top > boundsSlow.top &&
                    boundsFast.right < boundsSlow.right &&
                    boundsFast.bottom < boundsSlow.bottom,
            )
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun testSharedElementInPlace_deferredScaleUpdate_doesNotDoubleApplyTransform() {
        val state = DeferredTransitionState("A")
        val scale = mutableFloatStateOf(1f)
        val placedPositionsInParent = mutableListOf<Offset>()

        rule.setContent {
            val px20 = with(LocalDensity.current) { 20.toDp() }
            val px40 = with(LocalDensity.current) { 40.toDp() }
            val px60 = with(LocalDensity.current) { 60.toDp() }
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }

            val transition = rememberDeferredTransition(state)
            SharedTransitionLayout(Modifier.size(px200)) {
                transition.DeferredAnimatedContent(
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    mutableTransformSpec = {
                        MutableContentTransform {
                            initialContentTransform { this.scale = scale.floatValue }
                        }
                    },
                ) { target ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.offset(
                                    x = if (target == "A") px20 else px60,
                                    y = if (target == "A") px60 else px100,
                                )
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("shared"),
                                    animatedVisibilityScope = this@DeferredAnimatedContent,
                                    boundsTransform = { _, _ -> tween(300) },
                                )
                                .onPlaced { coords ->
                                    if (target == "A") {
                                        placedPositionsInParent.add(coords.positionInParent())
                                    }
                                }
                                .size(px40)
                                .testTag("shared-$target")
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        placedPositionsInParent.clear()
        rule.runOnIdle {
            state.defer("B")
            scale.floatValue = 0.5f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // While rendered in place inside the 0.5x scaled container, the shared element's local
        // position inside the container must stay at (20, 60) on every placement pass so the
        // container's graphics layer scales it once to (60, 80) in root coordinates (rather than
        // placing it at local (60, 80) against a stale 1.0x layer and scaling it twice).
        assertTrue(placedPositionsInParent.isNotEmpty())
        for (pos in placedPositionsInParent) {
            assertEquals(20f, pos.x, 1f)
            assertEquals(60f, pos.y, 1f)
        }

        val boundsInRoot = rule.onNodeWithTag("shared-A").fetchSemanticsNode().boundsInRoot
        assertEquals(60f, boundsInRoot.left, 1f)
        assertEquals(80f, boundsInRoot.top, 1f)
        assertEquals(20f, boundsInRoot.width, 1f)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun testSharedElement_interruptedDeferralToUnmatchedState_remainsVisible() {
        var deferredState: DeferredTransitionState<String>? = null

        rule.setContent {
            val px100 = with(LocalDensity.current) { 100.toDp() }
            val px200 = with(LocalDensity.current) { 200.toDp() }
            val px300 = with(LocalDensity.current) { 300.toDp() }

            SharedTransitionLayout(Modifier.size(px300).testTag("scope").background(Color.White)) {
                val state = remember { DeferredTransitionState("A") }
                deferredState = state
                val transition = rememberDeferredTransition(state)
                transition.DeferredAnimatedContent(
                    transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }
                ) { target ->
                    Box(Modifier.fillMaxSize()) {
                        // C has no matching shared element.
                        if (target != "C") {
                            val offsetDp = if (target == "B") px200 else 0.dp
                            Box(
                                Modifier.offset(offsetDp, offsetDp)
                                    .sharedElement(
                                        rememberSharedContentState("shared"),
                                        animatedVisibilityScope = this@DeferredAnimatedContent,
                                    )
                                    .size(px100)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(IntRect(0, 0, 100, 100), getBounds())

        try {
            rule.mainClock.autoAdvance = false
            rule.runOnIdle { deferredState?.animateTo("B") }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()
            val boundsMidFlight = getBounds()
            assertTrue(
                "Expected $boundsMidFlight to be in flight between A and B",
                boundsMidFlight.left in 1..199,
            )

            // Deferring to C (no matching shared element) while A -> B is in flight must keep the
            // outgoing B entry rendering and continuing its bounds animation toward B rather than
            // disappearing or snapping to its final position.
            rule.runOnIdle { deferredState?.defer("C") }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val boundsDuringDefer = getBounds()
            assertTrue(
                "Expected $boundsDuringDefer to remain visible and mid-flight from $boundsMidFlight",
                boundsDuringDefer != IntRect.Zero &&
                    boundsDuringDefer.left in boundsMidFlight.left..199,
            )

            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()
            val boundsLaterInDefer = getBounds()
            assertTrue(
                "Expected $boundsLaterInDefer to continue animating toward B from $boundsDuringDefer",
                boundsLaterInDefer.left > boundsDuringDefer.left &&
                    boundsLaterInDefer.top > boundsDuringDefer.top,
            )
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.runOnIdle { deferredState?.animateTo("B") }
        rule.waitForIdle()
        assertEquals(IntRect(200, 200, 300, 300), getBounds())
    }
}
