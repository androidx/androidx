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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class AnimateBoundsTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun animatePosition() =
        with(rule.density) {
            val frames = 14 // Even number to reliably test at half duration
            val durationMillis = frames * 16
            val rootSizePx = 100
            val boxSizePX = 20

            var boxPosition = IntOffset.Zero

            var isAtStart by mutableStateOf(true)

            rule.setContent {
                Box(modifier = Modifier.size(rootSizePx.toDp())) {
                    LookaheadScope {
                        Box(
                            modifier =
                                Modifier.align(
                                        if (isAtStart) Alignment.TopStart else Alignment.BottomEnd
                                    )
                                    .size(boxSizePX.toDp())
                                    .animateBounds(
                                        lookaheadScope = this@LookaheadScope,
                                        boundsTransform = { _, _ ->
                                            tween(durationMillis, easing = LinearEasing)
                                        }
                                    )
                                    .drawBehind { drawRect(Color.LightGray) }
                                    .onGloballyPositioned {
                                        boxPosition = it.positionInParent().round()
                                    }
                        )
                    }
                }
            }
            rule.waitForIdle()

            // At TopStart (0, 0)
            assertEquals(IntOffset.Zero, boxPosition)

            // AutoAdvance off to test animation at different points
            rule.mainClock.autoAdvance = false
            isAtStart = false
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Advance to the middle of the animation
            rule.mainClock.advanceTimeBy(durationMillis / 2L)

            val expectedPosPx = (rootSizePx - boxSizePX) * 0.5f
            val expectedIntOffset = Offset(expectedPosPx, expectedPosPx).round()
            assertEquals(expectedIntOffset, boxPosition)

            // AutoAdvance ON to finish the animation
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            // At BottomEnd (parentSize - boxSize, parentSize - boxSize)
            val expectedFinalPos = rootSizePx - boxSizePX
            assertEquals(IntOffset(expectedFinalPos, expectedFinalPos), boxPosition)
        }

    @Test
    fun animateSize() =
        with(rule.density) {
            val frameTime = 16 // milliseconds
            val frames = 14 // Even number to reliable test at half duration
            val durationMillis = frames * frameTime
            val rootSizePx = 400
            val boxSizeSmallPx = rootSizePx * 0.25f
            val boxSizeLargePx = rootSizePx * 0.5f

            val expectedLargeSize = Size(boxSizeLargePx, boxSizeLargePx)
            val expectedSmallSize = Size(boxSizeSmallPx, boxSizeSmallPx)

            var boxSize = IntSize.Zero

            var isExpanded by mutableStateOf(false)

            rule.setContent {
                Box(Modifier.size(rootSizePx.toDp())) {
                    LookaheadScope {
                        Box(
                            Modifier.size(
                                    if (isExpanded) boxSizeLargePx.toDp() else boxSizeSmallPx.toDp()
                                )
                                .animateBounds(
                                    lookaheadScope = this,
                                    boundsTransform = { _, _ ->
                                        tween(
                                            durationMillis = durationMillis,
                                            easing = LinearEasing
                                        )
                                    }
                                )
                                .drawBehind { drawRect(Color.LightGray) }
                                .onGloballyPositioned { boxSize = it.size }
                        )
                    }
                }
            }
            rule.waitForIdle()
            assertEquals(expectedSmallSize.round(), boxSize)

            // AutoAdvance off to test animation at different points
            rule.mainClock.autoAdvance = false
            isExpanded = true
            rule.waitForIdle()

            // Wait until first animated frame, for test stability
            do {
                rule.mainClock.advanceTimeByFrame()
            } while (expectedSmallSize.round() == boxSize)

            // Advance to approx. the middle of the animation (minus the first animated frame)
            rule.mainClock.advanceTimeBy(durationMillis / 2L - frameTime)

            val expectedMidIntSize = (expectedLargeSize + expectedSmallSize).times(0.5f).round()
            assertEquals(expectedMidIntSize, boxSize)

            // AutoAdvance ON to finish the animation
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            assertEquals(expectedLargeSize.round(), boxSize)
        }

    @Test
    fun animateBounds() =
        with(rule.density) {
            val frames = 14 // Even number to reliable test at half duration
            val durationMillis = frames * 16
            val rootSizePx = 400
            val boxSizeSmallPx = rootSizePx * 0.25f
            val boxSizeLargePx = rootSizePx * 0.5f

            val expectedLargeSize = Size(boxSizeLargePx, boxSizeLargePx)
            val expectedSmallSize = Size(boxSizeSmallPx, boxSizeSmallPx)
            val expectedFinalPos = rootSizePx - boxSizeLargePx

            var boxBounds = Rect(Offset.Zero, Size.Zero)

            var toggle by mutableStateOf(false)

            rule.setContent {
                Box(Modifier.size(rootSizePx.toDp())) {
                    LookaheadScope {
                        Box(
                            Modifier.then(
                                    if (toggle) {
                                        Modifier.align(Alignment.BottomEnd)
                                            .size(boxSizeLargePx.toDp())
                                    } else {
                                        Modifier.align(Alignment.TopStart)
                                            .size(boxSizeSmallPx.toDp())
                                    }
                                )
                                .animateBounds(
                                    lookaheadScope = this,
                                    boundsTransform = { _, _ ->
                                        tween(
                                            durationMillis = durationMillis,
                                            easing = LinearEasing
                                        )
                                    }
                                )
                                .drawBehind { drawRect(Color.Yellow) }
                                .onGloballyPositioned { boxBounds = it.boundsInParent() }
                        )
                    }
                }
            }
            rule.waitForIdle()
            assertEquals(Rect(Offset.Zero, expectedSmallSize), boxBounds)

            // AutoAdvance off to test animation at different points
            rule.mainClock.autoAdvance = false
            toggle = true
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Advance to the middle of the animation
            rule.mainClock.advanceTimeBy(durationMillis / 2L)
            rule.waitForIdle()

            // Calculate expected bounds
            val expectedMidSize = (expectedLargeSize + expectedSmallSize).times(0.5f)
            val expectedMidPosition = (rootSizePx - boxSizeLargePx) * 0.5f
            val expectedMidOffset = Offset(expectedMidPosition, expectedMidPosition)
            val expectedMidBounds = Rect(expectedMidOffset, expectedMidSize)

            assertEquals(expectedMidBounds, boxBounds)

            // AutoAdvance ON to finish the animation
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            assertEquals(
                Rect(Offset(expectedFinalPos, expectedFinalPos), expectedLargeSize),
                boxBounds
            )
        }

    @Test
    fun animateBounds_withIntermediateModifier() =
        with(rule.density) {
            val durationMillis = 10 * 16

            var toggleAnimation by mutableStateOf(true)

            val rootWidthPx = 100
            val padding1Px = 10
            val padding2Px = 20

            var positionA = IntOffset(-1, -1)
            var positionB = IntOffset(-1, 1)

            // Change the padding on state change to trigger the animation
            fun Modifier.applyPadding(): Modifier =
                this.padding(
                    horizontal =
                        if (toggleAnimation) {
                            padding1Px.toDp()
                        } else {
                            padding2Px.toDp()
                        }
                )

            rule.setContent {
                // Based on sample `AnimateBounds_withLayoutModifier`
                LookaheadScope {
                    Column(Modifier.width(rootWidthPx.toDp())) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    modifier = Modifier.applyPadding(),
                                    boundsTransform = { _, _,
                                        ->
                                        tween(durationMillis, easing = LinearEasing)
                                    }
                                )
                            ) {
                                Box(
                                    Modifier.onGloballyPositioned {
                                        positionA = it.positionInRoot().round()
                                    }
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.animateBounds(
                                        lookaheadScope = this@LookaheadScope,
                                        boundsTransform = { _, _,
                                            ->
                                            tween(durationMillis, easing = LinearEasing)
                                        }
                                    )
                                    .applyPadding()
                            ) {
                                Box(
                                    Modifier.onGloballyPositioned {
                                        positionB = it.positionInRoot().round()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()

            assertEquals(positionA, IntOffset(padding1Px, 0))
            assertEquals(positionB, IntOffset(padding1Px, 0))

            rule.mainClock.autoAdvance = false
            toggleAnimation = !toggleAnimation
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // We measure at the first animated frame
            rule.mainClock.advanceTimeByFrame()

            // Box A has a continuous change in value from having the Modifier as a parameter
            val expectedPosA =
                lerp(padding1Px.toFloat(), padding2Px.toFloat(), 16f / durationMillis)
            assertEquals(positionA, IntOffset(expectedPosA.fastRoundToInt(), 0))
            // Box B has an immediate change in value from chaining the Modifier
            assertEquals(positionB, IntOffset(padding2Px, 0))
        }

    @Test
    fun animateBounds_usingMovableContent() =
        with(rule.density) {
            val frames = 14 // Even number to reliable test at half duration
            val durationMillis = frames * 16

            val itemASizePx = 30
            val itemAOffset = IntOffset(70, 70)

            val itemBSizePx = 50
            val itemBOffset = IntOffset(110, 110)

            var isBoxAtSlotA by mutableStateOf(true)

            var boxPosition = IntOffset.Zero
            var boxSize = IntSize.Zero

            rule.setContent {
                val movableBox = remember {
                    movableContentWithReceiverOf<LookaheadScope> {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .animateBounds(
                                        lookaheadScope = this,
                                        boundsTransform = { _, _ ->
                                            tween(
                                                durationMillis = durationMillis,
                                                easing = LinearEasing
                                            )
                                        }
                                    )
                                    .onGloballyPositioned {
                                        boxPosition = it.positionInRoot().round()
                                        boxSize = it.size
                                    }
                        )
                    }
                }

                LookaheadScope {
                    Box {
                        Box(Modifier.offset { itemAOffset }.size(itemASizePx.toDp())) {
                            // Slot A
                            if (isBoxAtSlotA) {
                                movableBox()
                            }
                        }
                        Box(Modifier.offset { itemBOffset }.size(itemBSizePx.toDp())) {
                            // Slot B
                            if (!isBoxAtSlotA) {
                                movableBox()
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()

            // Initial conditions
            assertEquals(itemAOffset, boxPosition)
            assertEquals(IntSize(itemASizePx, itemASizePx), boxSize)

            // AutoAdvance off to test animation at different points
            rule.mainClock.autoAdvance = false
            isBoxAtSlotA = false
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Advance to the middle of the animation
            rule.mainClock.advanceTimeBy(durationMillis / 2L)
            rule.waitForIdle()

            // Evaluate with expected values at half the animation
            val sizeAtHalfDuration = (itemASizePx + itemBSizePx) / 2
            assertEquals((itemAOffset + itemBOffset).div(2f), boxPosition)
            assertEquals(IntSize(sizeAtHalfDuration, sizeAtHalfDuration), boxSize)

            // AutoAdvance ON to finish the animation
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            assertEquals(itemBOffset, boxPosition)
            assertEquals(IntSize(itemBSizePx, itemBSizePx), boxSize)
        }

    @Test
    fun animateBounds_scrollBehavior() =
        with(rule.density) {
            val itemSizePx = 30f
            val keyFrameOffset = itemSizePx * 5

            var isAnimateScroll by mutableStateOf(false)
            val scrollState = ScrollState(0)

            var item0Position = IntOffset(-1, -1)

            rule.setContent {
                LookaheadScope {
                    Column(Modifier.size(itemSizePx.toDp()).verticalScroll(scrollState)) {
                        repeat(2) { index ->
                            Box(
                                modifier =
                                    Modifier.size(itemSizePx.toDp())
                                        .animateBounds(
                                            lookaheadScope = this@LookaheadScope,
                                            boundsTransform = { initial, _ ->
                                                // Drive the start position to a specific value, by
                                                // default
                                                // the animation should not happen, and so we should
                                                // never
                                                // be able to read that value.
                                                keyframes {
                                                    Rect(Offset(0f, keyFrameOffset), initial.size)
                                                        .at(0)
                                                        .using(LinearEasing)
                                                }
                                            },
                                            animateMotionFrameOfReference = isAnimateScroll
                                        )
                                        .onGloballyPositioned {
                                            if (index == 0) {
                                                item0Position = it.positionInRoot().round()
                                            }
                                        }
                            )
                        }
                    }
                }
            }
            // First test without animating scroll, note that we still handle the clock, as to allow
            // any animation to play after we change the scroll.
            rule.waitForIdle()
            rule.mainClock.autoAdvance = false

            runBlocking { scrollState.scrollBy(itemSizePx) }
            rule.waitForIdle()

            // Let animations play for the first frame
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Expected position should immediately reflect scroll changes since we are not
            // animating it
            assertEquals(IntOffset(0, -itemSizePx.fastRoundToInt()), item0Position)

            // Finish any pending animations
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            // Enable scroll animation
            isAnimateScroll = true
            rule.waitForIdle()
            rule.mainClock.autoAdvance = false

            runBlocking {
                // Scroll back into starting position
                scrollState.scrollBy(-itemSizePx)
            }
            rule.waitForIdle()

            rule.mainClock.advanceTimeByFrame()

            // Position should correspond to the exaggerated keyframe offset.
            // Note that the keyframe is actually defined around the item's center
            assertEquals(
                Offset(
                        // Center position at x = 0
                        x = 0f,
                        // keyframeOffset - (previousScrollOffset) + itemCenterY
                        y = keyFrameOffset
                    )
                    .round(),
                item0Position
            )
        }

    private fun Size.round(): IntSize = IntSize(width.roundToInt(), height.roundToInt())

    private operator fun Size.plus(other: Size) = Size(width + other.width, height + other.height)

    private operator fun Size.minus(other: Size) = Size(width - other.width, height - other.height)

    private operator fun IntSize.minus(other: IntSize) =
        IntSize(width - other.width, height - other.height)

    private operator fun Rect.minus(other: Rect) =
        Rect(offset = this.topLeft - other.topLeft, size = this.size - other.size)
}
