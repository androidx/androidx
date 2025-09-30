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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.max
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyGridScrollIndicatorTest(private val orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    private val LazyGridTag = "LazyGridTag"

    @Test
    fun scrollIndicatorState_emptyGrid() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = crossAxisSlots,
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {}
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_whenContentFits() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = with(rule.density) { 30.toDp() }
        val totalItems = 4 // Results in 4/2 = 2 lines
        val linesInViewport = 3 // Viewport is larger than content
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = crossAxisSlots,
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize = expectedContentLines * itemMainSizePx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_uniformSizeItems() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val totalItems = 10 // Results in 10/2 = 5 lines
        val linesInViewport = 3
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = crossAxisSlots,
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize = expectedContentLines * itemMainSizePx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_variableItemSizes() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizesPx = listOf(50, 80, 60, 90, 70)
        val itemCrossSize = 30.dp
        val totalItems = 10 // Results in 10/2 = 5 lines
        val containerMainAxisSizePx = 200
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {
                items(totalItems) { index ->
                    val itemMainSize =
                        with(rule.density) { itemMainSizesPx[index % itemMainSizesPx.size].toDp() }
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize =
            state.layoutInfo.visibleLinesAverageMainAxisSize() * expectedContentLines

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withMainAxisSpacing() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val mainAxisSpacingPx = 10
        val mainAxisSpacing = with(rule.density) { mainAxisSpacingPx.toDp() }
        val totalItems = 10 // Results in 10/2 = 5 lines
        val containerMainAxisSizePx = 150
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
                mainAxisSpacedBy = mainAxisSpacing,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize =
            (expectedContentLines * itemMainSizePx) +
                ((expectedContentLines - 1) * mainAxisSpacingPx)

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withContentPadding() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 10 // Results in 10/2 = 5 lines
        val containerMainAxisSizePx = 150
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        val contentPadding =
            if (orientation == Orientation.Vertical) {
                PaddingValues(top = startPadding, bottom = endPadding)
            } else {
                PaddingValues(start = startPadding, end = endPadding)
            }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
                contentPadding = contentPadding,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize =
            (expectedContentLines * itemMainSizePx) + startPaddingPx + endPaddingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withReverseLayout() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val totalItems = 10 // Results in 10/2 = 5 lines
        val linesInViewport = 3
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
                reverseLayout = true,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize = expectedContentLines * itemMainSizePx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_onScrollByOffset() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val totalItems = 20 // Results in 10/2 = 5 lines
        val linesInViewport = 3
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }
        val scrollAmountPx = (itemMainSizePx * 1.5f).toInt() // Scroll 1.5 items
        val scrollAmount = with(rule.density) { scrollAmountPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(itemCrossSize))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollMainAxisBy(scrollAmount)

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize = expectedContentLines * itemMainSizePx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(scrollAmountPx)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_onScrollToItem() {
        lateinit var state: LazyGridState
        val crossAxisSlots = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val itemCrossSize = 30.dp
        val totalItems = 20 // Results in 10/2 = 5 lines
        val linesInViewport = 3
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }
        val scrollToItemIndex = 7 // Belongs to line 3 (0-indexed lines)

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = GridCells.Fixed(crossAxisSlots),
                modifier = Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyGridTag),
                state = state,
            ) {
                items(totalItems) { index ->
                    Spacer(
                        Modifier.mainAxisSize(itemMainSize)
                            .crossAxisSize(itemCrossSize)
                            .testTag("item$index")
                    )
                }
            }
        }

        state.scrollTo(scrollToItemIndex)

        val expectedScrolledLine = scrollToItemIndex / crossAxisSlots
        val expectedScrollOffset = expectedScrolledLine * itemMainSizePx

        val expectedContentLines = ceil(totalItems.toFloat() / crossAxisSlots).toInt()
        val expectedContentSize = expectedContentLines * itemMainSizePx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(expectedScrollOffset)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    internal fun LazyGridLayoutInfo.visibleLinesAverageMainAxisSize(): Int {
        val isVertical = orientation == Orientation.Vertical
        val visibleItems = visibleItemsInfo
        if (visibleItems.isEmpty()) return 0

        fun lineOf(index: Int): Int =
            if (isVertical) visibleItemsInfo[index].row else visibleItemsInfo[index].column

        var totalLinesMainAxisSize = 0
        var linesCount = 0

        var lineStartIndex = 0
        while (lineStartIndex < visibleItems.size) {
            val currentLine = lineOf(lineStartIndex)
            if (currentLine == -1) {
                // Filter out exiting items.
                ++lineStartIndex
                continue
            }

            var lineMainAxisSize = 0
            var lineEndIndex = lineStartIndex
            while (lineEndIndex < visibleItems.size && lineOf(lineEndIndex) == currentLine) {
                lineMainAxisSize =
                    max(
                        lineMainAxisSize,
                        if (isVertical) {
                            visibleItems[lineEndIndex].size.height
                        } else {
                            visibleItems[lineEndIndex].size.width
                        },
                    )
                ++lineEndIndex
            }

            totalLinesMainAxisSize += lineMainAxisSize
            ++linesCount

            lineStartIndex = lineEndIndex
        }

        return totalLinesMainAxisSize / linesCount + mainAxisItemSpacing
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
