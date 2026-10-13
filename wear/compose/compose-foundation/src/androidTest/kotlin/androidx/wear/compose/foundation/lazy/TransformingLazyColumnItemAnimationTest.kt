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

package androidx.wear.compose.foundation.lazy

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnItemAnimationTest {

    @get:Rule val rule = createComposeRule()

    private val containerSize = 200.dp
    private val itemSize = 80.dp
    private val frameDuration = 16L
    private val animationDuration = 160L // 10 frames for easy math
    private val listTag = "list"
    private val tolerancePx = 1.5f
    private val tolerance: Dp
        get() = maxOf(0.5.dp, with(rule.density) { tolerancePx.toDp() })

    private val animationSpec =
        tween<IntOffset>(durationMillis = animationDuration.toInt(), easing = LinearEasing)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun reorder_reverseLayout_animatesCorrectly() {
        // Set up 2 items in reverse layout (Item 0 at bottom 80dp, Item 1 at top 0dp)
        var list by mutableStateOf(listOf(0, 1))
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(80.dp)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(0.dp)

        // Trigger reorder
        rule.runOnUiThread { list = listOf(1, 0) }

        // Verify frame-by-frame smooth transitions
        onAnimationFrame { fraction ->
            val expectedY0 = 80.dp - (80.dp * fraction)
            val expectedY1 = 0.dp + (80.dp * fraction)
            rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(expectedY0)
            rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(expectedY1)
        }
    }

    @Test
    fun insertion_reverseLayout_heightTransformsSmoothly() {
        // Set up 1 item that scales dynamically based on scroll progress
        var list by mutableStateOf(listOf(0))
        val containerHeight = 300.dp
        val baseHeight = 100.dp
        val heightProvider: (Int, TransformingLazyColumnItemScrollProgress) -> Int =
            { height, progress ->
                val fraction = progress.topOffsetFraction
                if (fraction.isNaN()) height
                else {
                    val scale = 0.6f + 0.4f * (1f - 2f * abs(fraction - 0.5f))
                    (height * scale).roundToInt()
                }
            }
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(width = 100.dp, height = containerHeight),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.transformedHeight(heightProvider) // Outer (handles layout spacing)
                            .requiredSize(width = 100.dp, height = baseHeight) // Inner (base size)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.waitForIdle()

        // Insert Item 1 at the top, pushing Item 0 downwards
        rule.runOnUiThread { list = listOf(1, 0) }

        // Verify that Item 0's position transitions smoothly without bouncing
        var previousY = Dp.Unspecified
        onAnimationFrame { fraction ->
            val y =
                rule.onNodeWithTag("item_0").fetchSemanticsNode().positionInRoot.y.let {
                    with(rule.density) { it.toDp() }
                }
            if (previousY != Dp.Unspecified) {
                val yDelta = abs(y.value - previousY.value)
                // A threshold of 15dp per frame (at 60Hz/16ms) is a heuristic that safely exceeds
                // the maximum expected velocity of a smooth spring animation (typically <
                // 8dp/frame),
                // while unequivocally catching any sudden layout leaps or "pops" (which exceed
                // 20dp+).
                assertWithMessage("Position jumped abruptly by $yDelta dp at fraction $fraction")
                    .that(yDelta)
                    .isLessThan(15f)
            }
            previousY = y
        }
    }

    @Test
    fun deletion_offscreenItem_scrollProgressKeepsAnimating() {
        // Set up 4 items where the boundary Item 2 is scaled down and partially offscreen
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val heightProvider: (Int, TransformingLazyColumnItemScrollProgress) -> Int =
            { height, progress ->
                val fraction = progress.topOffsetFraction
                if (fraction.isNaN()) height
                else {
                    val scale = 0.5f + 0.5f * (1f - fraction)
                    (height * scale).roundToInt()
                }
            }
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.transformedHeight(heightProvider)
                            .requiredSize(width = 80.dp, height = itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(80.dp)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(144.dp)

        // Delete Item 0, forcing Item 1, 2, and 3 to slide up
        rule.runOnUiThread { list = listOf(1, 2, 3) }

        // Verify that incoming Item 3's position transitions smoothly (proving Item 2 scaled
        // smoothly)
        var previousY3 = Dp.Unspecified
        onAnimationFrame { fraction ->
            if (fraction > 0f) {
                val y3 =
                    rule.onNodeWithTag("item_3").fetchSemanticsNode().positionInRoot.y.let {
                        with(rule.density) { it.toDp() }
                    }
                if (previousY3 != Dp.Unspecified) {
                    val y3Delta = abs(y3.value - previousY3.value)
                    // A threshold of 15dp per frame (at 60Hz/16ms) is a heuristic that safely
                    // exceeds
                    // the maximum expected velocity of a smooth spring animation (typically <
                    // 8dp/frame),
                    // while unequivocally catching any sudden layout leaps or "pops" (which exceed
                    // 20dp+).
                    assertWithMessage("Item 3 position jumped by $y3Delta dp at fraction $fraction")
                        .that(y3Delta)
                        .isLessThan(15f)
                }
                previousY3 = y3
            }
        }
    }

    @Test
    fun reorder_multipleItems_animatesSmoothly() {
        // Set up 4 items in standard layout
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val localItemSize = 50.dp
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = 80.dp, height = localItemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(0.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(50.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(100.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_3").assertTopPositionInRootIsEqualTo(150.dp, tolerance = 1.5.dp)

        // Trigger massive multi-item reorder (complete reverse)
        rule.runOnUiThread { list = listOf(3, 2, 1, 0) }

        // Verify frame-by-frame smooth transitions for all 4 items
        onAnimationFrame { fraction ->
            val expectedY0 = 0.dp + (150.dp * fraction)
            val expectedY1 = 50.dp + (50.dp * fraction)
            val expectedY2 = 100.dp - (50.dp * fraction)
            val expectedY3 = 150.dp - (150.dp * fraction)
            rule
                .onNodeWithTag("item_0")
                .assertTopPositionInRootIsEqualTo(expectedY0, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_1")
                .assertTopPositionInRootIsEqualTo(expectedY1, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_2")
                .assertTopPositionInRootIsEqualTo(expectedY2, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_3")
                .assertTopPositionInRootIsEqualTo(expectedY3, tolerance = 1.5.dp)
        }
    }

    @Test
    fun reorder_multipleItemsInReverseLayout_animatesSmoothly() {
        // Set up 4 items in reverse layout (Item 0 at bottom, Item 3 at top)
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val localItemSize = 50.dp
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = 80.dp, height = localItemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(150.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(100.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(50.dp, tolerance = 1.5.dp)
        rule.onNodeWithTag("item_3").assertTopPositionInRootIsEqualTo(0.dp, tolerance = 1.5.dp)

        // Trigger massive multi-item reorder (complete reverse)
        rule.runOnUiThread { list = listOf(3, 2, 1, 0) }

        // Verify frame-by-frame smooth transitions in reverse layout
        onAnimationFrame { fraction ->
            val expectedY0 = 150.dp - (150.dp * fraction)
            val expectedY1 = 100.dp - (50.dp * fraction)
            val expectedY2 = 50.dp + (50.dp * fraction)
            val expectedY3 = 0.dp + (150.dp * fraction)
            rule
                .onNodeWithTag("item_0")
                .assertTopPositionInRootIsEqualTo(expectedY0, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_1")
                .assertTopPositionInRootIsEqualTo(expectedY1, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_2")
                .assertTopPositionInRootIsEqualTo(expectedY2, tolerance = 1.5.dp)
            rule
                .onNodeWithTag("item_3")
                .assertTopPositionInRootIsEqualTo(expectedY3, tolerance = 1.5.dp)
        }
    }

    @Test
    fun multipleAdditionsAndDeletions_standardLayout_animatesSmoothly() {
        // Set up 3 items in standard layout
        var list by mutableStateOf(listOf(0, 1, 2))
        val localItemSize = 50.dp
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = 80.dp, height = localItemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        // Ensure the tolerance is larger than 1px, 0.5px is too small at densities below 2.0f
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(0.dp, tolerance)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(50.dp, tolerance)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(100.dp, tolerance)

        // Delete Item 0 & 1, insert Item 3 at the top
        rule.runOnUiThread { list = listOf(3, 2) }

        // Verify that Item 2 slides up smoothly to fill the gap
        onAnimationFrame { fraction ->
            val expectedY2 = 100.dp - (50.dp * fraction)
            rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(expectedY2, tolerance)
        }
    }

    @Test
    fun multipleAdditionsAndDeletions_reverseLayout_animatesSmoothly() {
        // Set up 3 items bottom-aligned in reverse layout
        var list by mutableStateOf(listOf(0, 1, 2))
        val localItemSize = 50.dp
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                reverseLayout = true,
                verticalArrangement = Arrangement.Bottom,
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = 80.dp, height = localItemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(150.dp, tolerance = 1.dp)
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(100.dp, tolerance = 1.dp)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(50.dp, tolerance = 1.dp)

        // Delete Item 0 & 1, insert Item 3 at the top
        rule.runOnUiThread { list = listOf(3, 2) }

        // Verify that Item 2 slides down smoothly to fill the gap
        onAnimationFrame { fraction ->
            val expectedY2 = 50.dp + (50.dp * fraction)
            rule
                .onNodeWithTag("item_2")
                .assertTopPositionInRootIsEqualTo(expectedY2, tolerance = 1.dp)
        }
    }

    @Test
    fun dispatchRawDelta_doesNotAnimatePlacement() {
        val state = TransformingLazyColumnState()
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(5, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(80.dp, tolerance)

        // Scroll without a scroll {} block, as done by nested scrolling (onPostScroll), so
        // isScrollInProgress stays false.
        rule.runOnUiThread {
            assertThat(state.isScrollInProgress).isFalse()
            val consumed = state.dispatchRawDelta(with(rule.density) { 40.dp.toPx() })
            assertThat(consumed).isNonZero()
        }

        // The scroll offset change must be applied immediately and not be animated.
        onAnimationFrame { _ ->
            rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(40.dp, tolerance)
            rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(120.dp, tolerance)
        }
    }

    @Test
    fun dispatchRawDelta_thenDeletion_animatesSmoothly() {
        val state = TransformingLazyColumnState()
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 40.dp.toPx() }) }
        rule.waitForIdle()
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(120.dp, tolerance)

        // Delete Item 1, Item 2 should slide up into its place.
        rule.runOnUiThread { list = listOf(0, 2, 3, 4) }

        onAnimationFrame { fraction ->
            val expectedY2 = 120.dp - (80.dp * fraction)
            rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(expectedY2, tolerance)
        }
    }

    @Test
    fun dispatchRawDelta_duringPlacementAnimation_keepsAnimating() {
        val state = TransformingLazyColumnState()
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(160.dp, tolerance)

        // Delete Item 1, Item 2 starts sliding up from 160dp to 80dp.
        rule.runOnUiThread { list = listOf(0, 2, 3, 4) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(animationDuration / 2)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(120.dp, tolerance)

        // Scroll by 20dp in the middle of the animation. The scroll is applied immediately and the
        // running animation continues towards the scrolled target (80dp - 20dp).
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 20.dp.toPx() }) }
        rule.waitForIdle()
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(100.dp, tolerance)

        rule.mainClock.advanceTimeBy(animationDuration / 2)
        rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(60.dp, tolerance)
    }

    @Test
    fun dispatchRawDelta_revealedItem_isNotAnimated() {
        val state = TransformingLazyColumnState()
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(6, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_3").assertDoesNotExist()

        // Scroll Item 0 out and Item 3 in.
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 100.dp.toPx() }) }

        onAnimationFrame { _ ->
            rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo((-20).dp, tolerance)
            rule.onNodeWithTag("item_2").assertTopPositionInRootIsEqualTo(60.dp, tolerance)
            rule.onNodeWithTag("item_3").assertTopPositionInRootIsEqualTo(140.dp, tolerance)
        }
    }

    @Test
    fun dispatchRawDelta_doesNotRedrawAnimatedItems() {
        rule.mainClock.autoAdvance = true
        val state = TransformingLazyColumnState()
        var draws = 0
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(10, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .drawBehind { if (key == 1) draws++ }
                    )
                }
            }
        }
        rule.waitForIdle()
        draws = 0

        // Nested scrolling dispatches a raw delta for every pointer move. Moving an item should
        // only move its layer and not invalidate its content.
        val step = with(rule.density) { 2.dp.toPx() }
        repeat(10) {
            rule.runOnUiThread { state.dispatchRawDelta(step) }
            rule.waitForIdle()
        }
        assertThat(draws).isEqualTo(0)
    }

    @Test
    fun dispatchRawDelta_reverse_revealedItem_isNotAnimated() {
        val state = TransformingLazyColumnState()
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(6, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        // Scroll Item 0 out and let any animation settle.
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 100.dp.toPx() }) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationDuration * 2)
        rule.waitForIdle()
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo((-20).dp, tolerance)

        // Scroll back, revealing Item 0 at the top.
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { -100.dp.toPx() }) }

        onAnimationFrame { _ ->
            rule.onNodeWithTag("item_0").assertTopPositionInRootIsEqualTo(0.dp, tolerance)
            rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(80.dp, tolerance)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun dispatchRawDelta_scrolledOutItem_isNotAnimatedAway() {
        val state = TransformingLazyColumnState()
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize).testTag(listTag),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(6, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = containerSize, height = itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .background(if (key == 0) Color.Red else Color.Blue)
                    )
                }
            }
        }

        // Scroll Item 0 (red) fully out. Item 1 (blue) is now at [-20dp, 60dp].
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 100.dp.toPx() }) }

        // Item 0 must not be drawn sliding out on top of Item 1.
        onAnimationFrame { _ -> assertThat(colorAt(10.dp)).isEqualTo(Color.Blue) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun dispatchRawDelta_duringFadeOut_movesFadingItemWithContent() {
        val state = TransformingLazyColumnState()
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val fadeOutDuration = 1000
        rule.setContent {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(containerSize).testTag(listTag),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(width = containerSize, height = itemSize)
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = null,
                                fadeOutSpec = tween(fadeOutDuration, easing = LinearEasing),
                            )
                            .background(if (key == 1) Color.Red else Color.Blue)
                    )
                }
            }
        }

        // Delete Item 1 (red). It fades out at [80dp, 160dp] while the other items snap.
        rule.runOnUiThread { list = listOf(0, 2, 3, 4, 5) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(fadeOutDuration / 2L)
        rule.waitForIdle()
        assertThat(colorAt(140.dp)).isNotEqualTo(Color.Blue)

        // Scroll by 40dp during the fade out. The fading item moves with the content to
        // [40dp, 120dp].
        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 40.dp.toPx() }) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        assertThat(colorAt(140.dp)).isEqualTo(Color.Blue)
        assertThat(colorAt(60.dp)).isNotEqualTo(Color.Blue)
    }

    @Test
    fun deletion_firstItemAtTop_animatesSmoothly() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(list, key = { it }) { key ->
                    Box(
                        Modifier.requiredSize(itemSize)
                            .animateItem(placementSpec = animationSpec)
                            .testTag("item_$key")
                    )
                }
            }
        }
        rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(80.dp, tolerance)

        rule.runOnUiThread { list = listOf(1, 2, 3, 4) }

        onAnimationFrame { fraction ->
            val expectedY1 = 80.dp - (80.dp * fraction)
            rule.onNodeWithTag("item_1").assertTopPositionInRootIsEqualTo(expectedY1, tolerance)
        }
    }

    @Test
    fun dispatchRawDelta_withTransformedHeights_doesNotAnimate() {
        val state = TransformingLazyColumnState()
        rule.setContent { TransformedItemsList(state) }
        rule.waitForIdle()

        rule.runOnUiThread { state.dispatchRawDelta(with(rule.density) { 30.dp.toPx() }) }

        assertPositionsDoNotChangeOverAnimation()
    }

    @Test
    fun scrollBy_withTransformedHeights_doesNotAnimateAfterScrollEnds() {
        val state = TransformingLazyColumnState()
        rule.setContent { TransformedItemsList(state) }
        rule.waitForIdle()

        rule.runOnIdle { runBlocking { state.scrollBy(with(rule.density) { 30.dp.toPx() }) } }

        assertPositionsDoNotChangeOverAnimation()
    }

    @Composable
    private fun TransformedItemsList(state: TransformingLazyColumnState) {
        val heightProvider: (Int, TransformingLazyColumnItemScrollProgress) -> Int =
            { height, progress ->
                val fraction = progress.topOffsetFraction
                if (fraction.isNaN()) height
                else {
                    val scale = 0.6f + 0.4f * (1f - 2f * abs(fraction - 0.5f)).coerceIn(0f, 1f)
                    (height * scale).roundToInt()
                }
            }
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.requiredSize(width = 100.dp, height = containerSize),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(10, key = { it }) { key ->
                Box(
                    Modifier.transformedHeight(heightProvider)
                        .requiredSize(width = 100.dp, height = 60.dp)
                        .animateItem(placementSpec = animationSpec)
                        .testTag("item_$key")
                )
            }
        }
    }

    private fun assertPositionsDoNotChangeOverAnimation() {
        val tags = (0 until 10).map { "item_$it" }
        fun positions() = tags.map { tag ->
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()?.positionInRoot?.y
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        val initial = positions()
        onAnimationFrame { fraction ->
            positions().forEachIndexed { index, y ->
                val expected = initial[index]
                if (y != null && expected != null) {
                    assertWithMessage("item_$index at fraction $fraction")
                        .that(abs(y - expected))
                        .isAtMost(tolerancePx)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun colorAt(y: Dp): Color {
        val pixels = rule.onNodeWithTag(listTag).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, with(rule.density) { y.roundToPx() }]
    }

    private fun onAnimationFrame(
        duration: Long = animationDuration,
        onFrame: (fraction: Float) -> Unit,
    ) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step frameDuration) {
            val fraction = i / duration.toFloat()
            onFrame(fraction)
            if (i < duration) {
                rule.mainClock.advanceTimeBy(frameDuration)
                expectedTime += frameDuration
                assertThat(rule.mainClock.currentTime).isEqualTo(expectedTime)
            }
        }
    }
}
