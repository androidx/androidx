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

package androidx.xr.glimmer.stack

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class VerticalStackMaskingTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun largerNextItem_clipsNextItemToTopItemShape() {
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
            }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val topItemTop = with(rule.density) { topItemBounds.top.roundToPx() }
            // The top of the top item is between the top and bottom of the next item
            assertThat(topItemTop).isGreaterThan(0)
            assertThat(topItemTop).isLessThan(pixels.height - 1)

            // The part of the next item above the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in 0 until topItemTop) {
                    assertWithMessage("Pixel at ($x, $y) should not have the next item's color")
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Green)
                }
            }

            // The bottom of the next item is visible below the top item
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            val x = (pixels.width / 2)
            val y = nextItemBottom - 1
            val nextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.red)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.green)
                .isGreaterThan(0.3f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.blue)
                .isLessThan(0.2f)
        }
    }

    @Test
    fun largerNextNextItem_clipsNextNextItemToNextItemShape() = runTest {
        var topItemHeight = 0
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    ) {
                        topItemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Blue),
                    )
                }
            }
        }

        // Scroll almost fully to the next item to reveal the next-next item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(topItemHeight * 0.99f) }
        rule.waitForIdle()

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()

            val topItemBottom = with(rule.density) { topItemBounds.bottom.roundToPx() }
            // The bottom of the top item is visible at the top of the next-next item bounds
            assertThat(topItemBottom).isGreaterThan(0)

            val nextItemTop = with(rule.density) { nextItemBounds.top.roundToPx() }
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            // The top of the next item is between the top and bottom of the next-next item
            assertThat(nextItemTop).isGreaterThan(0)
            assertThat(nextItemTop).isLessThan(nextItemBottom)
            // The top of the next item is below the bottom of the top item
            assertThat(nextItemTop).isGreaterThan(topItemBottom)

            // The part of the next-next item above the next item and below the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in topItemBottom + 1 until nextItemTop) {
                    assertWithMessage(
                            "Pixel at ($x, $y) should not have the next-next item's color"
                        )
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Blue)
                }
            }

            // The bottom of the next-next item is visible below the next item
            val nextNextItemBottom = with(rule.density) { nextNextItemBounds.bottom.roundToPx() }
            assertThat(nextNextItemBottom).isGreaterThan(nextItemBottom)
            val x = (pixels.width / 2)
            val y = nextNextItemBottom - 1
            val nextNextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.red)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.green)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.blue)
                .isGreaterThan(0.3f)
        }
    }

    @Test
    fun narrowDecoration_doesNotClip() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a narrow decoration, which does not mask Item 1
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                    }
                }
                item {
                    // Item 1: Blue, should not be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
            val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Item 1 outside narrow decoration should not be clipped")
                .that(pixels[pixels.width / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun narrowDecorationBecomesWide_clipsAfterWidening() {
        var widthFraction by mutableStateOf(0.5f)
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a narrow decoration initially.
                    Box(
                        Modifier.focusable()
                            .fillMaxWidth(widthFraction)
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red)
                    )
                }
                item {
                    // Item 1: Blue, should not be clipped by Item 0 initially.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should not be clipped")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }

        rule.runOnIdle { widthFraction = 1f }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should now be clipped")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isNotEqualTo(Color.Blue)
        }
    }

    @Test
    fun decorationHeightDecreases_updatesMask() {
        var heightFraction by mutableStateOf(0.6f)
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a large decoration initially.
                    Box(
                        Modifier.focusable()
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red)
                    )
                }
                item { StackItem("Item 1", Modifier.background(Color.Blue)) }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 0 is in the middle of the stack viewport")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }

        rule.runOnIdle { heightFraction = 0.1f }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should now be clipped further")
                .that(pixels[pixels.width / 2, (pixels.height * 0.8f).toInt()].toOpaque())
                .isNotIn(listOf(Color.Red, Color.Blue))
        }
    }

    @Test
    fun decorationNodeReused_updatesMask() {
        val stackHeight = 100.dp
        val stackHeightPx = stackHeight.toPx()
        rule.setContent {
            Box(Modifier.background(Color.Red)) {
                VerticalStack(modifier = Modifier.size(stackHeight).testTag("stack")) {
                    items(10) {
                        Box(
                            Modifier.focusable()
                                .fillMaxWidth()
                                .fillMaxHeight(0.1f)
                                .itemDecoration(RectangleShape)
                                .background(Color.Green)
                        )
                    }
                    item {
                        StackItem(
                            "Item 1",
                            Modifier.itemDecoration(RectangleShape).background(Color.Blue),
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("The center pixel should have the outer Box color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }

        repeat(9) {
            // Scroll the stack sufficiently to trigger a modifier node reuse.
            performIndirectSwipe(stackHeightPx)
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("The center pixel should have the outer Box color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }
    }

    @Test
    fun multipleDecorations_clipsToWidest() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val wideDecorationHeight = 10.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Contains two decorations.
                    // 1. Narrow but tall (Red)
                    // 2. Wide (Green)
                    // The clip should align to the Wide one (Green).
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                        Box(
                            Modifier.padding(top = narrowDecorationHeight)
                                .fillMaxWidth()
                                .height(wideDecorationHeight)
                                .itemDecoration(RectangleShape)
                                .background(Color.Green)
                        )
                    }
                }
                item {
                    // Item 1: Blue, should be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
            val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }
            val wideDecorationHeightPx = with(rule.density) { wideDecorationHeight.roundToPx() }
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Item 1 outside narrow decoration but above clip line should be clipped"
                )
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isNotEqualTo(Color.Blue)

            val belowY = with(rule.density) { 50.dp.roundToPx() }
            assertWithMessage("Item 1 should be visible below the clip line)")
                .that(
                    pixels[
                            pixels.width / 2,
                            narrowDecorationHeightPx + wideDecorationHeightPx + belowY]
                        .toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun multipleEqualWidthDecorations_clipsAboveTopMost() {
        val topOffset = 100.dp
        val decorationHeight = 10.dp
        val interDecorationDistance = 100.dp
        val state = StackState()
        rule.setContent {
            Box(Modifier.background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        Column(Modifier.padding(top = topOffset).fillMaxSize().focusable()) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .height(decorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                            Spacer(Modifier.height(interDecorationDistance))
                            Box(
                                Modifier.fillMaxWidth()
                                    .height(decorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Large item", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val offsetX = pixels.width / 2
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val interDecorationDistancePx = interDecorationDistance.toPx()

            assertWithMessage("Next item is masked above the first decoration")
                .that(pixels[offsetX, topOffsetPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Next item is not masked between the two decorations")
                .that(
                    pixels[
                            offsetX,
                            topOffsetPx + decorationHeightPx + interDecorationDistancePx / 2]
                        .toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun removeDecoration_updatesClip() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val wideDecorationHeight = 10.dp
        val state = StackState()
        var showWideDecoration by mutableStateOf(true)
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Contains two decorations.
                    // 1. Narrow but tall (Red) -- always present
                    // 2. Wide (Green) -- gets removed
                    // The clip should initially align to the green one and then the red one.
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                        if (showWideDecoration) {
                            Box(
                                Modifier.padding(top = narrowDecorationHeight)
                                    .fillMaxWidth()
                                    .height(wideDecorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                }
                item {
                    // Item 1: Blue, should be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }
        val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
        val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }

        // Initially the clip line is aligned with the top of the wide decoration.
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Item 1 outside narrow decoration but above the wide decoration should be clipped"
                )
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isNotEqualTo(Color.Blue)
        }

        rule.runOnIdle { showWideDecoration = false }

        // Now the clip line is aligned with the top of the narrow decoration.
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Item 1 outside narrow decoration should now be visible")
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun changeDecorationOffset_updatesClip() {
        val state = StackState()
        val initialOffset = 200.dp
        val decorationHeight = 10.dp
        val shadowOffset = 50.dp
        var offsetDp by mutableStateOf(initialOffset)
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.padding(top = offsetDp)
                                .fillMaxWidth()
                                .height(decorationHeight)
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                    }
                }
                item { StackItem("Item 1", Modifier.background(Color.Green)) }
            }
        }
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val centerX = pixels.width / 2
            assertWithMessage("Initially clips above the initial offset")
                .that(pixels[centerX, initialOffset.toPx() / 2].toOpaque())
                .isNotIn(listOf(Color.Red, Color.Green))
            assertWithMessage("Item 1 is visible right below Item 0")
                .that(
                    pixels[
                            centerX,
                            initialOffset.toPx() + decorationHeight.toPx() + shadowOffset.toPx()]
                        .toOpaque()
                )
                .isEqualTo(Color.Green)
        }

        rule.runOnIdle { offsetDp = initialOffset + decorationHeight + shadowOffset * 2 }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val centerX = pixels.width / 2
            assertWithMessage("Item 1 is clipped where it was visible before")
                .that(
                    pixels[
                            centerX,
                            initialOffset.toPx() + decorationHeight.toPx() + shadowOffset.toPx()]
                        .toOpaque()
                )
                .isNotIn(listOf(Color.Red, Color.Green))
        }
    }

    @Test
    fun roundedShape_clipsAtTopRadius() {
        val topOffset = 100.dp
        val cornerRadius = 100.dp
        val shape = RoundedCornerShape(cornerRadius)
        val state = StackState()
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        // Item 0: Rounded Rect with the widest point at the 'cornerRadius' Y offset
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(cornerRadius * 2)
                                    .clip(shape)
                                    .itemDecoration(shape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val cornerRadiusPx = cornerRadius.toPx()
            val cornerRadiusLegPx = cornerRadiusPx / sqrt(2f)
            val offsetPx = (cornerRadiusPx - cornerRadiusLegPx).toInt()

            assertWithMessage("Pixels outside of the top-left rounded corner should be clipped")
                .that(pixels[offsetPx, topOffsetPx + offsetPx - 2])
                .isEqualTo(Color.Red)
            assertWithMessage("Pixels inside of the rounded corner should have the Item 0 color")
                .that(pixels[offsetPx, topOffsetPx + offsetPx + 2])
                .isEqualTo(Color.Green)
            assertWithMessage(
                    "Pixels outside of the bottom-left rounded corner should have the Item 1 color"
                )
                .that(pixels[offsetPx, topOffsetPx + cornerRadiusPx * 2 - offsetPx + 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun genericShape_selectsWidestPoint() {
        val topOffset = 100.dp
        val decorationHeight = 100.dp
        // Shape definition:
        // - Starts at 60% decoration height (40% top padding).
        // - Widest point is at 80% of the total decoration height.
        // - Ends at 100% decoration height.
        // Shape bounds: top=0.6, bottom=1.0, height=0.4.
        // Widest point relative to shape bounds: at 0.2 (which is 0.8 absolute - 0.6 top).
        val shiftedDiamondShape = GenericShape { size, _ ->
            moveTo(size.width / 2f, size.height * 0.6f) // Start 60% down
            lineTo(size.width, size.height * 0.8f) // Widest point at 80%
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height * 0.8f) // Widest point at 80%
            close()
        }

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = StackState(), modifier = Modifier.testTag("stack")) {
                    item {
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(decorationHeight)
                                    .clip(shiftedDiamondShape)
                                    .itemDecoration(shiftedDiamondShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val offsetXPx = pixels.width / 6

            assertWithMessage("Pixels above the widest point should be clipped")
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.5f).toInt()].toOpaque()
                )
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Pixels at 70% (between calculated relative height and absolute height) should be clipped"
                )
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.7f).toInt()].toOpaque()
                )
                .isEqualTo(Color.Red)

            assertWithMessage("Pixels below the widest point should have Item 1 color")
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.95f).toInt()].toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun genericShape_picksTopMostWidestPoint() {
        val state = StackState()
        val topOffset = 100.dp
        val decorationHeight = 100.dp
        // Widest points are at 0.3 (top) and 0.7 (bottom).
        // The masking logic should pick the top widest line.
        val indentedRhombusShape = GenericShape { size, _ ->
            apply {
                moveTo(size.width * 0.5f, 0f)
                lineTo(size.width, size.height * 0.3f)
                lineTo(size.width * 0.6f, size.height * 0.5f)
                lineTo(size.width, size.height * 0.7f)
                lineTo(size.width * 0.5f, size.height)
                lineTo(0f, size.height * 0.7f)
                lineTo(size.width * 0.4f, size.height * 0.5f)
                lineTo(0f, size.height * 0.3f)
                close()
            }
        }
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(decorationHeight)
                                    .clip(indentedRhombusShape)
                                    .itemDecoration(indentedRhombusShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val offsetXPx = pixels.width / 4
            assertWithMessage("Pixels above the top widest point should be clipped")
                .that(pixels[offsetXPx, topOffsetPx + 5].toOpaque())
                .isEqualTo(Color.Red)
            assertWithMessage("Pixels in the indented area should have Item 1 color")
                .that(pixels[offsetXPx, topOffsetPx + decorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
            assertWithMessage("Pixels below the widest bottom point should have Item 1 color")
                .that(pixels[offsetXPx, topOffsetPx + decorationHeightPx - 5].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Composable
    private fun StackItem(
        text: String,
        modifier: Modifier = Modifier,
        onHeightChanged: (Int) -> Unit = {},
    ) {
        Box(
            modifier
                .onSizeChanged { onHeightChanged(it.height) }
                .fillMaxSize()
                .focusable()
                .testTag(text)
        ) {
            Text(text)
        }
    }

    private fun performIndirectSwipe(distancePx: Int, durationMillis: Long = 200L) {
        require(distancePx != 0)
        rule
            .onRoot()
            .performIndirectSwipe(rule, distancePx.toFloat(), moveDuration = durationMillis)
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    private fun Color.toOpaque(): Color = copy(alpha = 1.0f)

    private fun Dp.toPx(): Int = with(rule.density) { roundToPx() }
}
