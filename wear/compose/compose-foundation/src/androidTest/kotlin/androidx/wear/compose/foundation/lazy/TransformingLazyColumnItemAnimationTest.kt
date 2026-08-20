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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
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
        val tolerance = maxOf(0.5.dp, with(rule.density) { 1.5f.toDp() })
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
