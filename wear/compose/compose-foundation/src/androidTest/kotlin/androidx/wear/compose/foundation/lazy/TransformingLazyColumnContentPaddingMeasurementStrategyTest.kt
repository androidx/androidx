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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnContentPaddingMeasurementStrategyTest {
    private val screenHeight = 100
    private val screenWidth = 120
    private val density = Density(1f)

    private val containerConstraints =
        Constraints(
            minWidth = screenWidth,
            maxWidth = screenWidth,
            minHeight = screenHeight,
            maxHeight = screenHeight,
        )

    @Test
    fun emptyList_emptyResult() {
        val result = strategy.measure(listOf())

        assertThat(result.visibleItems).isEmpty()
    }

    @Test
    fun fullScreenItem_takesFullHeight() {
        val result = strategy.measure(listOf(screenHeight))

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight)
    }

    @Test
    fun fullScreenItem_scrollsBackToCenter() {
        val result =
            strategy.measure(
                listOf(screenHeight),
                // Scroll is ignored as the item constrained by the screen.
                scrollToBeConsumed = 25f,
            )

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight)
    }

    @Test
    fun halfScreenItem_takesHalfHeightAndTopAligned() {
        val result = strategy.measure(listOf(screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight / 2)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight / 2)
    }

    @Test
    fun twoItemsWithFirstTopAligned_measuredWithCorrectOffsets() {
        val result = strategy.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 2))
    }

    @Test
    fun twoItemsWithFirstTopAlignedWithPadding_measuredWithCorrectOffsets() {
        val topPadding = 5.dp
        val topPaddingPx = with(density) { topPadding.roundToPx() }
        val strategyWithTopPadding = measurementStrategy(PaddingValues(top = topPadding))

        val result = strategyWithTopPadding.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0 + topPaddingPx, screenHeight / 2 + topPaddingPx))
        assertThat(result.beforeContentPadding).isEqualTo(topPaddingPx)
    }

    @Test
    fun twoItemsWithLastOneAlignedWithPadding_measuredWithCorrectOffsets() {
        val bottomPadding = 5.dp
        val bottomPaddingPx = with(density) { bottomPadding.roundToPx() }
        val strategyWithBottomPadding = measurementStrategy(PaddingValues(bottom = bottomPadding))

        val result = strategyWithBottomPadding.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 2))
        assertThat(result.afterContentPadding).isEqualTo(bottomPaddingPx)
    }

    @Test
    fun threeHalfScreenItemsWithFirstOneTopAligned_pushesLastItemOffscreen() {
        val result =
            strategy.measure(
                listOf(
                    // Is centered.
                    screenHeight / 2,
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                )
            )

        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun threeItemsWithSecondOneCentered_measuredWithCorrectOffsets() {
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered.
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(-screenHeight / 4, screenHeight / 4, screenHeight * 3 / 4))
    }

    @Test
    fun threeItemsWithSecondOneCenteredAndOffset_measuredWithCorrectOffsets() {
        val tinyOffset = 5
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered with the offset.
                    screenHeight / 2,
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                anchorItemScrollOffset = tinyOffset,
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(
                listOf(
                    -screenHeight / 4 - tinyOffset,
                    screenHeight / 4 - tinyOffset,
                    screenHeight * 3 / 4 - tinyOffset,
                )
            )
    }

    @Test
    fun threeItemsWithSecondOneCenteredAndScrolled_measuredWithCorrectOffsets() {
        val scrollAmount = 5
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered with the scroll.
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                scrollToBeConsumed = scrollAmount.toFloat(),
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(
                listOf(
                    -screenHeight / 4 + scrollAmount,
                    screenHeight / 4 + scrollAmount,
                    screenHeight * 3 / 4 + scrollAmount,
                )
            )
    }

    @Test
    fun fullScreenItemWithTransformedHeight_takesHalfOfHeight() {
        val result =
            strategy.measure(
                listOf(
                    // Center item that appears half of the size.
                    screenHeight
                ),
                transformedHeight = { measuredHeight, _ -> measuredHeight / 2 },
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(1)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight / 2)
    }

    @Test
    fun renderContentSmallerThanTheScreen_hasNoScrolling() {
        val result =
            strategy.measure(
                listOf(
                    // Centered item.
                    screenHeight / 5,
                    screenHeight / 5,
                    screenHeight / 5,
                )
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(3)
    }

    @Test
    fun renderContentOnTopOfList_hasNoBackwardScrolling() {
        val result =
            strategy.measure(
                listOf(
                    // Centered item.
                    screenHeight / 2,
                    screenHeight / 2,
                    screenHeight / 2,
                )
            )

        assertThat(result.canScrollForward).isTrue()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun renderContentOnBottomOfList_hasNoForwardScrolling() {
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    screenHeight / 2,
                    // Centered item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 2,
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isTrue()
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun renderFullscreenContentOnTopOfList_hasNoBackwardScrolling() {
        val result = strategy.measure(listOf(screenHeight, screenHeight, screenHeight))

        assertThat(result.canScrollForward).isTrue()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(1)
    }

    @Test
    fun renderFullscreenContentOnBottomOfList_hasNoForwardScrolling() {
        val result =
            strategy.measure(listOf(screenHeight, screenHeight, screenHeight), anchorItemIndex = 2)

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isTrue()
        assertThat(result.visibleItems.size).isEqualTo(1)
    }

    @Test
    fun dynamicHeightItems_measuredWithCorrectOffsets() {
        val result =
            strategy.measure(
                listOf(
                    // Will be half of the size and therefore placed at 0 offset.
                    screenHeight / 2,
                    // Centered.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                transformedHeight = { measuredHeight, scrollProgression ->
                    if (scrollProgression.topOffsetFraction < 0.25f) {
                        measuredHeight / 2
                    } else measuredHeight
                },
            )

        assertThat(result.visibleItems.size).isEqualTo(2)
        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 4))
    }

    @Test
    fun flingBackwards_restoresLayoutCorrectly() {
        val itemSize = screenHeight / 4

        val result =
            strategy.measure(
                listOf(
                    // Items visible before the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                    // Items visible after the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                ),
                scrollToBeConsumed = -10 * screenHeight.toFloat(),
            )
        assertThat(result.visibleItems.map { it.index }).isEqualTo(listOf(4, 5, 6, 7))
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0, itemSize, 2 * itemSize, 3 * itemSize))
    }

    @Test
    fun flingForward_restoresLayoutCorrectly() {
        val itemSize = screenHeight / 4

        val result =
            strategy.measure(
                listOf(
                    // Items visible after the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                    // Items visible before the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                ),
                anchorItemIndex = 4,
                scrollToBeConsumed = 10 * screenHeight.toFloat(),
            )
        assertThat(result.visibleItems.map { it.index }).isEqualTo(listOf(0, 1, 2, 3))
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0, itemSize, 2 * itemSize, 3 * itemSize))
    }

    @Test
    fun initialLayout_contentFitScreen_restoresLayoutCorrectly() {
        val itemSize = screenHeight / 10

        val result = strategy.measure(listOf(itemSize, itemSize, itemSize, itemSize))
        assertThat(result.visibleItems.map { it.index }).isEqualTo(listOf(0, 1, 2, 3))
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0, itemSize, 2 * itemSize, 3 * itemSize))
    }

    @Test
    fun fullSizeBottomContentPadding_doesNotCrash() {
        val strategy =
            measurementStrategy(
                // Padding takes the full size.
                PaddingValues(bottom = with(density) { screenHeight.toDp() })
            )

        val itemSize = screenHeight / 4

        val result = strategy.measure(listOf(itemSize, itemSize))
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun fullSizeTopContentPadding_doesNotCrash() {
        val strategy =
            measurementStrategy(
                // Padding takes the full size.
                PaddingValues(top = with(density) { screenHeight.toDp() })
            )

        val itemSize = screenHeight / 4

        val result =
            strategy.measure(
                itemHeights = listOf(itemSize, itemSize),
                lastMeasuredAnchorItemHeight = itemSize,
            )
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun anchorRestoredByKey_whenItemBeforeAnchorIsRemoved() {
        val itemSize = screenHeight / 2
        val initialItems = listOf("A", "B", "Anchor", "D", "E")
        val finalItems = listOf("A", "Anchor", "D", "E")
        val anchorItemIndex = 2

        // 1. Measure the initial layout with item "Anchor" (index 2) as the anchor.
        val initialResult =
            strategy.measure(
                itemHeights = List(initialItems.size) { itemSize },
                keys = initialItems,
                // We don't set an anchor key but set an anchor index instead.
                anchorItemIndex = anchorItemIndex,
            )
        val anchorOffset = initialResult.visibleItems.first { it.key == "Anchor" }.offset
        // Assert that the visible items are B, Anchor and D, and we don't see any other items.
        assertThat(initialResult.visibleItems.map { it.key }).isEqualTo(listOf("B", "Anchor", "D"))

        // 2. Now, measure again after item "B" has been removed.
        // We pass the *key* of the last known anchor item, "Anchor".
        val finalResult =
            strategy.measure(
                itemHeights = List(finalItems.size) { itemSize },
                keys = finalItems,
                anchorItemKey = "Anchor",
                // anchorItemIndex should be ignored as the "Anchor" key is present.
                anchorItemIndex = anchorItemIndex,
            )

        // 3. Assert that the layout correctly found "Anchor" by its key,
        // updated its anchor index, and maintained its scroll offset.
        val finalAnchorOffset = finalResult.visibleItems.first { it.key == "Anchor" }.offset

        // The new anchor should remain the same, "Anchor" at its new index, 1.
        assertThat(finalResult.anchorItemIndex).isEqualTo(1)
        // The offset of the anchor after deletion should remain the same.
        assertThat(finalAnchorOffset).isEqualTo(anchorOffset)
        // The visible items should now be A, Anchor and D.
        assertThat(finalResult.visibleItems.map { it.key }).isEqualTo(listOf("A", "Anchor", "D"))
    }

    @Test
    fun anchorDoesntChange_whenItemAfterAnchorIsRemoved() {
        val itemSize = screenHeight / 2
        val initialItems = listOf("A", "B", "Anchor", "D", "E")
        val finalItems = listOf("A", "B", "Anchor", "E")
        val anchorItemIndex = 2

        // 1. Measure the initial layout with item "Anchor" (index 2) as the anchor.
        val initialResult =
            strategy.measure(
                itemHeights = List(initialItems.size) { itemSize },
                keys = initialItems,
                // We don't set an anchor key but set an anchor index instead.
                anchorItemIndex = anchorItemIndex,
            )
        val anchorOffset = initialResult.visibleItems.first { it.key == "Anchor" }.offset
        // Assert that the visible items are B, Anchor and D, and we don't see any other items.
        assertThat(initialResult.visibleItems.map { it.key }).isEqualTo(listOf("B", "Anchor", "D"))

        // 2. Now, measure again after item "D" has been removed.
        // We pass the *key* of the last known anchor item, "Anchor".
        val finalResult =
            strategy.measure(
                itemHeights = List(finalItems.size) { itemSize },
                keys = finalItems,
                anchorItemKey = "Anchor",
                // anchorItemIndex should be ignored as the "Anchor" key is present.
                anchorItemIndex = anchorItemIndex,
            )

        // 3. Assert that the layout correctly found "Anchor" by its key,
        // updated its anchor index, and maintained its scroll offset.
        val finalAnchorOffset = finalResult.visibleItems.first { it.key == "Anchor" }.offset

        // The new anchor should remain the same, "Anchor" at the same index, 2.
        assertThat(finalResult.anchorItemIndex).isEqualTo(2)
        // The offset of the anchor after deletion should remain the same.
        assertThat(finalAnchorOffset).isEqualTo(anchorOffset)
        // The visible items should now be B, Anchor and E.
        assertThat(finalResult.visibleItems.map { it.key }).isEqualTo(listOf("B", "Anchor", "E"))
    }

    @Test
    fun anchorMovesToNextItem_whenAnchorItselfIsDeleted() {
        val itemSize = screenHeight / 2
        val initialItems = listOf("A", "B", "Anchor", "D", "E")
        val finalItems = listOf("A", "B", "D", "E") // "Anchor" is removed.
        val anchorItemIndex = 2

        // 1. Measure the initial layout with "Anchor" centered.
        val initialResult =
            strategy.measure(
                itemHeights = List(initialItems.size) { itemSize },
                keys = initialItems,
                anchorItemIndex = anchorItemIndex,
            )
        // Assert that the visible items are B, Anchor, and D.
        assertThat(initialResult.visibleItems.map { it.key }).isEqualTo(listOf("B", "Anchor", "D"))

        // 2. Now, measure again after "Anchor" has been removed.
        // We pass the key of the deleted item. The strategy should select
        // the next item ("D") as the new anchor.
        val finalResult =
            strategy.measure(
                itemHeights = List(finalItems.size) { itemSize },
                keys = finalItems,
                anchorItemKey =
                    "Anchor", // Key of the deleted item. We shouldn't have it in the finalItems.
                anchorItemIndex = anchorItemIndex,
            )

        // 3. Assert that the new anchor is "D" (at its new index 2)
        assertThat(finalResult.anchorItemIndex).isEqualTo(2)
        assertThat(finalResult.visibleItems.map { it.key }).isEqualTo(listOf("B", "D", "E"))
    }

    @Test
    fun initialLayout_normalLayout_hasCorrectLogicalOffset() {
        val topPadding = 10.dp
        val strategy = measurementStrategy(PaddingValues(top = topPadding), reverseLayout = false)

        val result = strategy.measure(listOf(screenHeight / 2))

        // For a normal layout, the logical offset of the first item should be the top padding.
        val item = result.visibleItems.first()
        assertThat(item.offset).isEqualTo(with(density) { topPadding.roundToPx() })
    }

    @Test
    fun initialLayout_reverseLayout_hasCorrectLogicalOffset() {
        val bottomPadding = 15.dp
        val strategy =
            measurementStrategy(PaddingValues(bottom = bottomPadding), reverseLayout = true)

        val result =
            strategy.measure(
                itemHeights = listOf(screenHeight / 2),
                verticalArrangement = Arrangement.Bottom,
            )

        // For a reverse layout, the logical offset of the first item should still be the
        // `beforeContentPadding`, which is the bottom padding in this case.
        val item = result.visibleItems.first()
        assertThat(item.offset).isEqualTo(with(density) { bottomPadding.roundToPx() })
    }

    @Test
    fun contentPadding_reverseLayout_isAppliedCorrectly() {
        val topPadding = 5.dp
        val bottomPadding = 10.dp
        val topPaddingPx = with(density) { topPadding.roundToPx() }
        val bottomPaddingPx = with(density) { bottomPadding.roundToPx() }
        val strategyWithPadding =
            measurementStrategy(
                PaddingValues(top = topPadding, bottom = bottomPadding),
                reverseLayout = true,
            )

        val result = strategyWithPadding.measure(listOf(screenHeight / 2, screenHeight / 2))

        // In reverseLayout, 'before' padding is at the bottom, 'after' is at the top.
        assertThat(result.beforeContentPadding).isEqualTo(bottomPaddingPx)
        assertThat(result.afterContentPadding).isEqualTo(topPaddingPx)
    }

    @Test
    fun scrolling_reverseLayout_reportsCorrectCanScrollFlags() {
        val result =
            measurementStrategy(reverseLayout = true)
                .measure(listOf(screenHeight, screenHeight, screenHeight))

        // At the start of a reversed list (bottom), we can scroll forward (up) but not backward
        // (down).
        assertThat(result.canScrollForward).isTrue()
        assertThat(result.canScrollBackward).isFalse()
    }

    @Test
    fun minimumVerticalContentPadding_firstItemVisible_overridesInitialPadding() {
        val initialTop = 10.dp
        val requiredTop = 50.dp
        val requiredTopPx = with(density) { requiredTop.roundToPx() }
        val strategy = measurementStrategy(PaddingValues(top = initialTop))

        val result =
            strategy.measure(
                itemHeights = listOf(screenHeight / 2, screenHeight / 2),
                minimumVerticalContentPaddings =
                    listOf(MinimumVerticalContentPadding(top = requiredTop, bottom = 0.dp), null),
                anchorItemScrollOffset = -25,
            )

        assertThat(result.visibleItems.first().offset).isEqualTo(requiredTopPx)
        assertThat(result.beforeContentPadding).isEqualTo(requiredTopPx)
    }

    @Test
    fun minimumVerticalContentPadding_scrollPositionInMiddle_returnsInitialPadding() {
        val initialPadding = 10.dp
        val initialPaddingPx = with(density) { initialPadding.roundToPx() }
        val requiredPadding = 50.dp
        val strategy = measurementStrategy(PaddingValues(initialPadding)) // 10.dp all around

        val result =
            strategy.measure(
                itemHeights = List(5) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        MinimumVerticalContentPadding(requiredPadding),
                        null,
                        null,
                        null,
                        MinimumVerticalContentPadding(requiredPadding),
                    ),
                anchorItemIndex = 2,
            )

        assertThat(result.beforeContentPadding).isEqualTo(initialPaddingPx)
        assertThat(result.afterContentPadding).isEqualTo(initialPaddingPx)
    }

    @Test
    fun minimumVerticalContentPadding_firstItemVisibleWithLargeInitialPadding_returnsInitialPadding() {
        val initialTop = 100.dp
        val initialTopPx = with(density) { initialTop.roundToPx() }
        val requiredTop = 10.dp

        val strategy = measurementStrategy(PaddingValues(top = initialTop))

        val result =
            strategy.measure(
                itemHeights = List(3) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        MinimumVerticalContentPadding(top = requiredTop, bottom = 0.dp),
                        null,
                        null,
                    ),
                anchorItemIndex = 0,
            )

        assertThat(result.beforeContentPadding).isEqualTo(initialTopPx)
    }

    @Test
    fun minimumVerticalContentPadding_lastItemVisible_overridesInitialPadding() {
        val initialBottom = 10.dp
        val requiredBottom = 50.dp
        val requiredBottomPx = with(density) { requiredBottom.roundToPx() }

        val strategy = measurementStrategy(PaddingValues(bottom = initialBottom))

        val result =
            strategy.measure(
                itemHeights = List(3) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        null,
                        null,
                        MinimumVerticalContentPadding(top = 0.dp, bottom = requiredBottom),
                    ),
                anchorItemIndex = 2,
            )

        assertThat(result.afterContentPadding).isEqualTo(requiredBottomPx)
    }

    @Test
    fun minimumVerticalContentPadding_lastItemVisibleWithLargeInitialPadding_returnsInitialPadding() {
        val initialBottom = 100.dp
        val initialBottomPx = with(density) { initialBottom.roundToPx() }
        val requiredBottom = 10.dp

        val strategy = measurementStrategy(PaddingValues(bottom = initialBottom))

        val result =
            strategy.measure(
                itemHeights = List(3) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        null,
                        null,
                        MinimumVerticalContentPadding(top = 0.dp, bottom = requiredBottom),
                    ),
                anchorItemIndex = 2,
            )

        assertThat(result.afterContentPadding).isEqualTo(initialBottomPx)
    }

    @Test
    fun minimumVerticalContentPadding_reverseLayoutAndFirstItemVisible_usesBottomPadding() {
        val requiredBottom = 50.dp
        val requiredBottomPx = with(density) { requiredBottom.roundToPx() }
        val strategy = measurementStrategy(PaddingValues(0.dp), reverseLayout = true)

        val result =
            strategy.measure(
                itemHeights = List(3) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        MinimumVerticalContentPadding(top = 10.dp, bottom = requiredBottom),
                        null,
                        null,
                    ),
                anchorItemIndex = 0,
                reverseLayout = true,
            )

        assertThat(result.beforeContentPadding).isEqualTo(requiredBottomPx)
    }

    @Test
    fun minimumVerticalContentPadding_reverseLayoutAndLastItemVisible_usesTopPadding() {
        val requiredTop = 50.dp
        val requiredTopPx = with(density) { requiredTop.roundToPx() }
        val strategy = measurementStrategy(PaddingValues(0.dp), reverseLayout = true)

        val result =
            strategy.measure(
                itemHeights = List(3) { screenHeight / 2 },
                minimumVerticalContentPaddings =
                    listOf(
                        null,
                        null,
                        MinimumVerticalContentPadding(top = requiredTop, bottom = 10.dp),
                    ),
                anchorItemIndex = 2,
                reverseLayout = true,
            )

        assertThat(result.afterContentPadding).isEqualTo(requiredTopPx)
    }

    @Test
    fun minimumVerticalContentPadding_singleItem_calculatesBothPaddings() {
        val requiredTop = 13.dp
        val requiredBottom = 23.dp
        val requiredTopPx = with(density) { requiredTop.roundToPx() }
        val requiredBottomPx = with(density) { requiredBottom.roundToPx() }
        val strategy = measurementStrategy(PaddingValues(0.dp))

        val result =
            strategy.measure(
                itemHeights = listOf(screenHeight / 4),
                minimumVerticalContentPaddings =
                    listOf(MinimumVerticalContentPadding(requiredTop, requiredBottom)),
                verticalArrangement = Arrangement.Top,
                anchorItemIndex = 0,
            )

        assertThat(result.beforeContentPadding).isEqualTo(requiredTopPx)
        assertThat(result.afterContentPadding).isEqualTo(requiredBottomPx)
        assertThat(result.visibleItems.first().offset).isEqualTo(requiredTopPx)
    }

    private val mockGraphicContext =
        object : GraphicsContext {
            override fun createGraphicsLayer(): GraphicsLayer {
                TODO("Not yet implemented")
            }

            override fun releaseGraphicsLayer(layer: GraphicsLayer) {
                TODO("Not yet implemented")
            }
        }

    private val mockItemAnimator = LazyLayoutItemAnimator<TransformingLazyColumnMeasuredItem>()

    private fun measurementStrategy(
        contentPadding: PaddingValues = PaddingValues(),
        reverseLayout: Boolean = false,
    ) =
        TransformingLazyColumnContentPaddingMeasurementStrategy(
            contentPadding,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            mockGraphicContext,
            mockItemAnimator,
            isScrollInProgress = { false },
            reverseLayout = reverseLayout,
        )

    private val strategy = measurementStrategy(PaddingValues())

    private fun TransformingLazyColumnMeasurementStrategy.measure(
        itemHeights: List<Int>,
        keys: List<Any> = List(itemHeights.size) { it },
        transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)? = null,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        anchorItemKey: Any = Any(),
        anchorItemIndex: Int = 0,
        anchorItemScrollOffset: Int = 0,
        lastMeasuredAnchorItemHeight: Int = Int.MIN_VALUE,
        scrollToBeConsumed: Float = 0f,
        reverseLayout: Boolean = false,
        minimumVerticalContentPaddings: List<MinimumVerticalContentPadding?>? = null,
    ): TransformingLazyColumnMeasureResult =
        measure(
            itemsCount = itemHeights.size,
            measuredItemProvider =
                makeMeasuredItemProvider(
                    itemHeights,
                    keys,
                    transformedHeight,
                    reverseLayout,
                    minimumVerticalContentPaddings,
                ),
            keyIndexMap =
                object : LazyLayoutKeyIndexMap {
                    override fun getIndex(key: Any): Int = keys.indexOf(key)

                    override fun getKey(index: Int): Any? = keys[index]
                },
            verticalArrangement = verticalArrangement,
            containerConstraints = containerConstraints,
            anchorItemKey = anchorItemKey,
            anchorItemIndex = anchorItemIndex,
            anchorItemScrollOffset = anchorItemScrollOffset,
            lastMeasuredAnchorItemHeight = lastMeasuredAnchorItemHeight,
            scrollToBeConsumed = scrollToBeConsumed,
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = density,
            layout = { width, height, _ ->
                object : MeasureResult {
                    override val width = width
                    override val height = height
                    override val alignmentLines
                        get() = TODO("Not yet implemented")

                    override fun placeChildren() {}
                }
            },
        )

    private class EmptyPlaceable(
        width: Int,
        height: Int,
        val transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)?,
        val minimumTopContentPadding: Dp?,
        val minimumBottomContentPadding: Dp?,
    ) : Placeable() {
        init {
            measuredSize = IntSize(width, height)
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {}

        override val parentData: Any?
            get() =
                if (
                    transformedHeight != null ||
                        minimumTopContentPadding != null ||
                        minimumBottomContentPadding != null
                ) {
                    TransformingLazyColumnParentData(
                        heightProvider = transformedHeight,
                        minimumTopContentPadding = minimumTopContentPadding,
                        minimumBottomContentPadding = minimumBottomContentPadding,
                    )
                } else {
                    null
                }
    }

    private fun makeMeasuredItemProvider(
        itemHeights: List<Int>,
        keys: List<Any>,
        transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)? = null,
        reverseLayout: Boolean = false,
        minimumVerticalContentPaddings: List<MinimumVerticalContentPadding?>? = null,
    ) = MeasuredItemProvider { index, offset, measurementDirection, progressProvider ->
        val minimumVerticalContentPaddings = minimumVerticalContentPaddings?.getOrNull(index)
        TransformingLazyColumnMeasuredItem(
            index = index,
            offset = offset,
            placeable =
                EmptyPlaceable(
                    width = screenWidth,
                    height = itemHeights[index],
                    transformedHeight = transformedHeight,
                    minimumTopContentPadding = minimumVerticalContentPaddings?.top,
                    minimumBottomContentPadding = minimumVerticalContentPaddings?.bottom,
                ),
            containerConstraints = containerConstraints,
            spacing = 8,
            leftPadding = 0,
            rightPadding = 0,
            measureScrollProgress = progressProvider(itemHeights[index]),
            measurementDirection = measurementDirection,
            horizontalAlignment = Alignment.CenterHorizontally,
            layoutDirection = LayoutDirection.Ltr,
            key = keys[index],
            contentType = null,
            reverseLayout = reverseLayout,
        )
    }

    private data class MinimumVerticalContentPadding(val top: Dp, val bottom: Dp = top)
}
