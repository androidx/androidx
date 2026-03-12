/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 26)
@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
@RunWith(AndroidJUnit4::class)
class LookaheadAnimationVisualDebugHelperTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testIsNotEnabled() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        val testTag = "is_not_enabled_test"
        val overallColor = Color.Gray

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(false) {
                    SharedTransitionLayout(
                        Modifier.requiredSize(20.dp).background(overallColor).testTag(testTag)
                    ) {
                        transitionScope = this
                        AnimatedContent(
                            targetState = visible,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                (EnterTransition.None togetherWith ExitTransition.None).using(
                                    SizeTransform(clip = false)
                                )
                            },
                        ) { isScreenA ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!isScreenA) {
                                    Box(
                                        modifier =
                                            Modifier.sharedElement(
                                                    rememberSharedContentState(key = "key"),
                                                    this@AnimatedContent,
                                                    boundsTransform = { _, _ -> tween(50) },
                                                )
                                                .size(10.dp)
                                                .background(overallColor)
                                                .align(Alignment.CenterEnd)
                                    )
                                } else {
                                    Box(
                                        modifier =
                                            Modifier.sharedElement(
                                                    rememberSharedContentState(key = "key"),
                                                    this@AnimatedContent,
                                                    boundsTransform = { _, _ -> tween(50) },
                                                )
                                                .size(20.dp)
                                                .background(overallColor)
                                                .align(Alignment.CenterStart)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).captureToImage().run { assertPixels { overallColor } }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testPartiallyEnabled() {
        var transitionScopeEnabled: SharedTransitionScope? = null
        var transitionScopeDisabled: SharedTransitionScope? = null
        var currentCenter: Offset? = null
        var visible by mutableStateOf(false)
        val enabledTestTag = "enabled_layout"
        val disabledTestTag = "disabled_layout"
        val enabledLayoutColor = Color.Gray
        val debugColor = Color.Red
        val disabledLayoutColor = Color.Black

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(debugColor) {
                        Column {
                            SharedTransitionLayout(
                                Modifier.size(50.dp)
                                    .background(enabledLayoutColor)
                                    .testTag(enabledTestTag)
                            ) {
                                transitionScopeEnabled = this
                                AnimatedContent(
                                    targetState = visible,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = {
                                        (EnterTransition.None togetherWith ExitTransition.None)
                                            .using(SizeTransform(clip = false))
                                    },
                                ) { isScreenB ->
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (!isScreenB) {
                                            Box(
                                                modifier =
                                                    Modifier.onGloballyPositioned {
                                                            currentCenter = it.boundsInRoot().center
                                                        }
                                                        .sharedElement(
                                                            rememberSharedContentState(
                                                                key = "key1"
                                                            ),
                                                            this@AnimatedContent,
                                                            boundsTransform = { _, _ -> tween(50) },
                                                        )
                                                        .size(30.dp)
                                                        .background(
                                                            enabledLayoutColor
                                                        ) // Match layout bg
                                                        .align(Alignment.CenterStart)
                                            )
                                        } else {
                                            Box(
                                                modifier =
                                                    Modifier.onGloballyPositioned {
                                                            currentCenter = it.boundsInRoot().center
                                                        }
                                                        .sharedElement(
                                                            rememberSharedContentState(
                                                                key = "key1"
                                                            ),
                                                            this@AnimatedContent,
                                                            boundsTransform = { _, _ -> tween(50) },
                                                        )
                                                        .size(40.dp)
                                                        .background(
                                                            enabledLayoutColor
                                                        ) // Match layout bg
                                                        .align(Alignment.CenterEnd)
                                            )
                                        }
                                    }
                                }
                            }

                            // Disabled
                            LookaheadAnimationVisualDebugging(false) {
                                SharedTransitionLayout(
                                    Modifier.size(50.dp)
                                        .background(disabledLayoutColor)
                                        .testTag(disabledTestTag)
                                ) {
                                    transitionScopeDisabled = this
                                    AnimatedContent(
                                        targetState = visible,
                                        modifier = Modifier.fillMaxSize(),
                                        transitionSpec = {
                                            (EnterTransition.None togetherWith ExitTransition.None)
                                                .using(SizeTransform(clip = false))
                                        },
                                    ) { isScreenB ->
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (!isScreenB) {
                                                Box(
                                                    modifier =
                                                        Modifier.sharedElement(
                                                                rememberSharedContentState(
                                                                    key = "key2"
                                                                ),
                                                                this@AnimatedContent,
                                                                boundsTransform = { _, _ ->
                                                                    tween(50)
                                                                },
                                                            )
                                                            .size(30.dp)
                                                            .background(
                                                                disabledLayoutColor
                                                            ) // Match layout bg
                                                            .align(Alignment.CenterStart)
                                                )
                                            } else {
                                                Box(
                                                    modifier =
                                                        Modifier.sharedElement(
                                                                rememberSharedContentState(
                                                                    key = "key2"
                                                                ),
                                                                this@AnimatedContent,
                                                                boundsTransform = { _, _ ->
                                                                    tween(50)
                                                                },
                                                            )
                                                            .size(40.dp)
                                                            .background(
                                                                disabledLayoutColor
                                                            ) // Match layout bg
                                                            .align(Alignment.CenterEnd)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (
            !(transitionScopeEnabled?.isTransitionActive == true &&
                transitionScopeDisabled?.isTransitionActive == true)
        ) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val center = currentCenter!!

        rule.onNodeWithTag(disabledTestTag).captureToImage().assertPixels { disabledLayoutColor }

        rule.onNodeWithTag(enabledTestTag).captureToImage().assertPixels {
            // Check if the pixel is on the center
            if (it.x == center.x.roundToInt() - 1 && it.y == center.y.roundToInt() - 1) {
                debugColor
            } else null
        }
    }

    @Test
    fun testCurrentBorderSharedElement() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var currentBounds: Rect? by mutableStateOf(null)
        val testTag = "current_border_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        currentBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ ->
                                                            spring(Spring.DampingRatioNoBouncy)
                                                        },
                                                    )
                                                    .size(30.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        currentBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ ->
                                                            spring(Spring.DampingRatioNoBouncy)
                                                        },
                                                    )
                                                    .size(40.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val pixelMap = toPixelMap()

            val bounds = currentBounds!!

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val leftEdge =
                        x == bounds.left.roundToInt() &&
                            y >= bounds.top.roundToInt() &&
                            y <= bounds.bottom.roundToInt()
                    val rightEdge =
                        x == bounds.right.roundToInt() &&
                            y >= bounds.top.roundToInt() &&
                            y <= bounds.bottom.roundToInt()
                    val topEdge =
                        y == bounds.top.roundToInt() &&
                            x >= bounds.left.roundToInt() &&
                            x <= bounds.right.roundToInt()
                    val bottomEdge =
                        y == bounds.bottom.roundToInt() &&
                            x >= bounds.left.roundToInt() &&
                            x <= bounds.right.roundToInt()

                    val currentBorderPixel = leftEdge || rightEdge || topEdge || bottomEdge

                    if (currentBorderPixel) {
                        val pixelColor = pixelMap[x, y]
                        if (pixelColor == backgroundColor || pixelColor == contentColor) {
                            throw AssertionError(
                                "Expected a bounds color on the border at ($x, $y), " +
                                    "but found background or content color."
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testCurrentBorderAnimateBounds() {
        var visible by mutableStateOf(false)
        var currentBounds: Rect? by mutableStateOf(null)
        val testTag = "current_border_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        Box(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            LookaheadScope {
                                Box(
                                    Modifier.then(
                                            if (visible) {
                                                Modifier.align(Alignment.BottomEnd).size(40.dp)
                                            } else {
                                                Modifier.align(Alignment.TopStart).size(20.dp)
                                            }
                                        )
                                        .animateBounds(
                                            lookaheadScope = this,
                                            boundsTransform = { _, _ ->
                                                spring(Spring.DampingRatioNoBouncy)
                                            },
                                        )
                                        .drawBehind { drawRect(Color.Yellow) }
                                        .onGloballyPositioned {
                                            currentBounds = it.boundsInParent()
                                        }
                                        .background(contentColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        for (i in 1..(100 / 16.67).roundToInt()) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()

            rule.onNodeWithTag(testTag).captureToImage().run {
                val pixelMap = toPixelMap()

                val bounds = currentBounds!!

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val leftEdge =
                            x == bounds.left.roundToInt() &&
                                y >= bounds.top.roundToInt() &&
                                y <= bounds.bottom.roundToInt()
                        val rightEdge =
                            x == bounds.right.roundToInt() &&
                                y >= bounds.top.roundToInt() &&
                                y <= bounds.bottom.roundToInt()
                        val topEdge =
                            y == bounds.top.roundToInt() &&
                                x >= bounds.left.roundToInt() &&
                                x <= bounds.right.roundToInt()
                        val bottomEdge =
                            y == bounds.bottom.roundToInt() &&
                                x >= bounds.left.roundToInt() &&
                                x <= bounds.right.roundToInt()

                        val currentBorderPixel = leftEdge || rightEdge || topEdge || bottomEdge

                        if (currentBorderPixel) {
                            val pixelColor = pixelMap[x, y]
                            if (pixelColor == backgroundColor || pixelColor == contentColor) {
                                throw AssertionError(
                                    "Expected a bounds color on the border at ($x, $y), " +
                                        "but found background or content color."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testTargetBorderSharedElement() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var target: Rect? = null
        val testTag = "target_border_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, targetBounds ->
                                                            target = targetBounds
                                                            tween(50)
                                                        },
                                                    )
                                                    .size(30.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, targetBounds ->
                                                            target = targetBounds
                                                            tween(50)
                                                        },
                                                    )
                                                    .size(40.dp)
                                                    .padding(10.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val bounds = target!!

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).captureToImage().run {
                val pixelMap = toPixelMap()

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val leftEdge =
                            x == bounds.left.roundToInt() &&
                                y >= bounds.top.roundToInt() &&
                                y <= bounds.bottom.roundToInt()
                        val rightEdge =
                            x == bounds.right.roundToInt() &&
                                y >= bounds.top.roundToInt() &&
                                y <= bounds.bottom.roundToInt()
                        val topEdge =
                            y == bounds.top.roundToInt() &&
                                x >= bounds.left.roundToInt() &&
                                x <= bounds.right.roundToInt()
                        val bottomEdge =
                            y == bounds.bottom.roundToInt() &&
                                x >= bounds.left.roundToInt() &&
                                x <= bounds.right.roundToInt()

                        val targetBorderPixel = leftEdge || rightEdge || topEdge || bottomEdge

                        if (targetBorderPixel) {
                            val pixelColor = pixelMap[x, y]
                            if (pixelColor == backgroundColor || pixelColor == contentColor) {
                                throw AssertionError(
                                    "Expected a bounds color on the border at ($x, $y), " +
                                        "but found background or content color."
                                )
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testTargetBorderAnimateBounds() {
        var visible by mutableStateOf(false)
        var target: Rect? = null
        val testTag = "target_border_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        Box(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            LookaheadScope {
                                Box(
                                    Modifier.then(
                                            if (visible) {
                                                Modifier.align(Alignment.BottomEnd).size(40.dp)
                                            } else {
                                                Modifier.align(Alignment.TopStart).size(20.dp)
                                            }
                                        )
                                        .animateBounds(
                                            lookaheadScope = this,
                                            boundsTransform = { initialBounds, targetBounds ->
                                                target = targetBounds
                                                tween(50)
                                            },
                                        )
                                        .drawBehind { drawRect(Color.Yellow) }
                                        .background(contentColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(50)

        val bounds = target!!

        rule.onNodeWithTag(testTag).captureToImage().run {
            val pixelMap = toPixelMap()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val leftEdge =
                        x == bounds.left.roundToInt() &&
                            y >= bounds.top.roundToInt() &&
                            y <= bounds.bottom.roundToInt()
                    val rightEdge =
                        x == bounds.right.roundToInt() &&
                            y >= bounds.top.roundToInt() &&
                            y <= bounds.bottom.roundToInt()
                    val topEdge =
                        y == bounds.top.roundToInt() &&
                            x >= bounds.left.roundToInt() &&
                            x <= bounds.right.roundToInt()
                    val bottomEdge =
                        y == bounds.bottom.roundToInt() &&
                            x >= bounds.left.roundToInt() &&
                            x <= bounds.right.roundToInt()

                    val targetBorderPixel = leftEdge || rightEdge || topEdge || bottomEdge

                    if (targetBorderPixel) {
                        val pixelColor = pixelMap[x, y]
                        if (pixelColor == backgroundColor || pixelColor == contentColor) {
                            throw AssertionError(
                                "Expected a bounds color on the border at ($x, $y), " +
                                    "but found background or content color."
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testPathSharedElement() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var bounds: Rect? by mutableStateOf(null)
        val testTag = "path_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        bounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(30.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        bounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(40.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        val initialBounds = bounds!!

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val targetBounds = bounds!!
        rule.onNodeWithTag(testTag).captureToImage().run {
            val pixelMap = toPixelMap()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Check if the pixel is on the center of the target position
                    val targetPixel =
                        x == targetBounds.center.x.roundToInt() &&
                            y == targetBounds.center.y.roundToInt()

                    // Check if the pixel is on the initial position
                    val initialPixel =
                        x == initialBounds.center.x.roundToInt() &&
                            y == initialBounds.center.y.roundToInt()

                    if (targetPixel || initialPixel) {
                        val pixelColor = pixelMap[x, y]
                        if (pixelColor == backgroundColor || pixelColor == contentColor) {
                            throw AssertionError(
                                "Expected a path color on the path at ($x, $y), " +
                                    "but found background or content color."
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testPathAnimateBounds() {
        var visible by mutableStateOf(false)
        var bounds: Rect? by mutableStateOf(null)
        val testTag = "path_test"
        val borderColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(borderColor) {
                        Box(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            LookaheadScope {
                                Box(
                                    Modifier.then(
                                            if (visible) {
                                                Modifier.align(Alignment.BottomEnd).size(40.dp)
                                            } else {
                                                Modifier.align(Alignment.TopStart).size(20.dp)
                                            }
                                        )
                                        .animateBounds(
                                            lookaheadScope = this,
                                            boundsTransform = { _, _ -> tween(50) },
                                        )
                                        .drawBehind { drawRect(Color.Yellow) }
                                        .onGloballyPositioned { bounds = it.boundsInParent() }
                                        .background(contentColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        val initialBounds = bounds!!

        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(testTag).captureToImage().run {
            val pixelMap = toPixelMap()
            val targetBounds = bounds!!

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Check if the pixel is on the center of the target position
                    val targetPixel =
                        x == targetBounds.center.x.roundToInt() &&
                            y == targetBounds.center.y.roundToInt()

                    // Check if the pixel is on the initial position
                    val initialPixel =
                        x == initialBounds.center.x.roundToInt() &&
                            y == initialBounds.center.y.roundToInt()

                    if (targetPixel || initialPixel) {
                        val pixelColor = pixelMap[x, y]
                        if (pixelColor == backgroundColor || pixelColor == contentColor) {
                            throw AssertionError(
                                "Expected a path color on the path at ($x, $y), " +
                                    "but found background or content color."
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testOverlay() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var currentBounds: Rect? = null
        val testTag = "overlay_test"
        val overlayColor = Color(0x8034A853)
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    overlayColor,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(Color.Transparent) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        currentBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(20.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        currentBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(40.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        val bounds = currentBounds!!

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val pixelMap = toPixelMap()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Check if the pixel is inside the element's bounds
                    val isInBounds =
                        x > bounds.left && x < bounds.right && y > bounds.top && y < bounds.bottom

                    // Check if the pixel is outside of the element's bounds
                    val isOutBounds =
                        (x < bounds.left || x > bounds.right) &&
                            (y < bounds.top || y > bounds.bottom)

                    // Check if the pixel is on the border
                    val border =
                        (x in 0..3) ||
                            (x in width - 3..<width) ||
                            (y in 0..3) ||
                            (y in height - 3..<height)

                    val pixelColor = pixelMap[x, y]

                    if (isInBounds && !border) {
                        if (pixelColor != contentColor) {
                            throw AssertionError(
                                "Expected content color on the element at ($x, $y), " +
                                    "but found background, debug, or overlay color."
                            )
                        }
                    } else if (isOutBounds && !border) {
                        if (pixelColor == contentColor || pixelColor == backgroundColor) {
                            throw AssertionError(
                                "Expected overlay color at ($x, $y), " +
                                    "but found content or background color."
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testProgressIndicatorSharedElement() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        val testTag = "progress_indicator_test"
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    false,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(Color.Transparent) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(100.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(50.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(80.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).captureToImage().run {
                val pixelMap = toPixelMap()

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val isBorderPixel = x == 0 || x == width - 1 || y == 0 || y == height - 1
                        if (isBorderPixel) {
                            val pixelColor = pixelMap[x, y]
                            if (pixelColor == backgroundColor || pixelColor == contentColor) {
                                throw AssertionError(
                                    "Expected a progress indicator color on the border at ($x, $y), " +
                                        "but found background or content color."
                                )
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testUnmatched() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var targetBounds: Rect? = null
        val testTag = "unmatched_test"
        val unmatchedColor = Color.Red
        val matchedColor = Color.Green
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    unmatchedColor,
                    true,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(matchedColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(100.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        targetBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(
                                                            key = "unmatch1"
                                                        ),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(70.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "match"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(10.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.TopStart)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        targetBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(
                                                            key = "unmatch2"
                                                        ),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(90.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "match"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(20.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.TopStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val bounds = targetBounds!!

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).run {
                // Check for 0 matches messages
                assertExists("unmatch1: 0uFE0F⃣ matches")
                assertExists("unmatch2: 0uFE0F⃣ matches")

                captureToImage().run {
                    assertPixels { pos ->
                        // Check if the pixel is on the element's bounds
                        val currentBorderPixel =
                            pos.x == bounds.left.roundToInt() &&
                                pos.x == bounds.right.roundToInt() &&
                                pos.y == bounds.top.roundToInt() &&
                                pos.y == bounds.bottom.roundToInt()

                        if (currentBorderPixel) {
                            unmatchedColor
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testMultipleMatches() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        var targetBounds: Rect? = null
        val testTag = "multiple_matches_test"
        val multipleMatchesColor = Color.Red
        val matchedColor = Color.Green
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    multipleMatchesColor,
                    Color.Transparent,
                    true,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(matchedColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(100.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        targetBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(
                                                            key = "duplicate"
                                                        ),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(70.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        targetBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(
                                                            key = "duplicate"
                                                        ),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(90.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                        Box(
                                            modifier =
                                                Modifier.onGloballyPositioned {
                                                        targetBounds = it.boundsInRoot()
                                                    }
                                                    .sharedElement(
                                                        rememberSharedContentState(
                                                            key = "duplicate"
                                                        ),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(90.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        val bounds = targetBounds!!

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).run {
                // Check for multiple matches message
                assertExists("duplicate: 2\uFE0F⃣ matches")

                captureToImage().run {
                    assertPixels { pos ->
                        // Check if the pixel is on the element's bounds
                        val currentBorderPixel =
                            pos.x == bounds.left.roundToInt() &&
                                pos.x == bounds.right.roundToInt() &&
                                pos.y == bounds.top.roundToInt() &&
                                pos.y == bounds.bottom.roundToInt()

                        if (currentBorderPixel) {
                            multipleMatchesColor
                        } else null
                    }
                }
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun testIsShowKeyLabelEnabled() {
        var transitionScope: SharedTransitionScope? = null
        var visible by mutableStateOf(false)
        val testTag = "show_keys_test"
        val animationColor = Color.Red
        val backgroundColor = Color.Gray
        val contentColor = Color.Blue

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadAnimationVisualDebugging(
                    true,
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent,
                    true,
                ) {
                    CustomizedLookaheadAnimationVisualDebugging(animationColor) {
                        SharedTransitionLayout(
                            Modifier.requiredSize(50.dp)
                                .background(backgroundColor)
                                .testTag(testTag)
                        ) {
                            transitionScope = this
                            AnimatedContent(
                                targetState = visible,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    (EnterTransition.None togetherWith ExitTransition.None).using(
                                        SizeTransform(clip = false)
                                    )
                                },
                            ) { isScreenA ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isScreenA) {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(20.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterEnd)
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier.sharedElement(
                                                        rememberSharedContentState(key = "key"),
                                                        this@AnimatedContent,
                                                        boundsTransform = { _, _ -> tween(50) },
                                                    )
                                                    .size(40.dp)
                                                    .background(contentColor)
                                                    .align(Alignment.CenterStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { visible = true }

        while (transitionScope?.isTransitionActive != true) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        while (transitionScope?.isTransitionActive != false) {
            rule.onNodeWithTag(testTag).run {
                // Check for displayed key
                assertExists("key")
            }
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }
    }
}
