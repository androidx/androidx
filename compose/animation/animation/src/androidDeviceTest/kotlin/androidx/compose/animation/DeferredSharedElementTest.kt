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

import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalDeferredTransitionApi::class, ExperimentalSharedTransitionApi::class)
class DeferredSharedElementTest {
    @get:Rule val rule = createComposeRule()

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
                val transition = rememberTransition(state)
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
                                                permitTransformDuringDeferredTransition = false
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
                                                permitTransformDuringDeferredTransition = false
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
                val transition = rememberTransition(state)
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
                val transition = rememberTransition(state)
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
                val transition = rememberTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState, previewScale) {
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
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
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
                val transition = rememberTransition(state)
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
                val transition = rememberTransition(state)
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
        testTimeSource = { rule.mainClock.currentTime }

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
                val transition = rememberTransition(state)
                val mutableTransform =
                    remember(state.pendingTargetState, previewScale) {
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
                previewScale = s
                rule.mainClock.advanceTimeByFrame()
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
        rule.runOnIdle { myState!!.animateTo("B") }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        previewScale = 1f

        // Scenario 2: Fast gesture
        val boundsFast = simulateGesture(isFast = true)

        // With fast gesture (more negative velocity), it should be significantly smaller
        assertTrue(
            "Expected $boundsFast to be smaller than $boundsSlow",
            boundsFast.width < boundsSlow.width && boundsFast.height < boundsSlow.height,
        )

        testTimeSource = null
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
                val transition = rememberTransition(state)
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
                val transition = rememberTransition(state)
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
                val transition = rememberTransition(state)
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
                                                permitTransformDuringDeferredTransition = false
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
                                                permitTransformDuringDeferredTransition = false
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
        // permitTransformDuringDeferredTransition = false.
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
                val transition = rememberTransition(state)
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
        myState?.defer("A")
        previewScale = 0.5f // Scale the parent AnimatedContent down by half
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
                val transition = rememberTransition(state)
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
}
