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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.pager.PagerState

public object OneHandedGestureDefaults {
    /**
     * A scroll implementation tailored for use with [TransformingLazyColumnState].
     *
     * This logic handles one-handed gesture by first attempting to scroll the list by half the
     * viewport. If the list cannot scroll further forward, it scrolls back to the start.
     *
     * Sample demonstrating gesture handling with [TransformingLazyColumnState]:
     *
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
     * @param scrollState The scroll state associated with a transforming lazy column.
     */
    public suspend fun scrollDown(scrollState: TransformingLazyColumnState) {
        if (!scrollState.canScrollForward) {
            scrollState.animateScrollToItem(0)
            return
        }

        scrollState.animateScrollBy(scrollState.layoutInfo.viewportSize.height * 0.5f)
    }

    /**
     * A scroll implementation tailored for use with [ScalingLazyListState].
     *
     * This logic handles one-handed gesture by first attempting to scroll the list by half the
     * viewport. If the list cannot scroll further forward, it scrolls back to the start.
     *
     * Sample demonstrating gesture handling with [ScalingLazyListState]:
     *
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnSample
     * @param scrollState The scroll state associated with a scaling lazy column.
     */
    public suspend fun scrollDown(scrollState: ScalingLazyListState) {
        if (!scrollState.canScrollForward) {
            scrollState.animateScrollToItem(0)
            return
        }

        scrollState.animateScrollBy(scrollState.layoutInfo.viewportSize.height * 0.5f)
    }

    /**
     * A scroll implementation tailored for use with [TransformingLazyColumnState].
     *
     * This logic handles one-handed gesture by first attempting to scroll to the next item in the
     * list (or scrolling through the current item if it exceeds the viewport size). If the list
     * cannot scroll further forward, it scrolls back to the start.
     *
     * Sample demonstrating gesture handling with [TransformingLazyColumnState]:
     *
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnScrollToNextItemSample
     * @param scrollState The scroll state associated with a transforming lazy column.
     */
    public suspend fun scrollToNextItem(scrollState: TransformingLazyColumnState) {
        if (!scrollState.canScrollForward) {
            scrollState.animateScrollToItem(0)
            return
        }

        val layoutInfo = scrollState.layoutInfo
        val anchorIndex = scrollState.anchorItemIndex
        val targetIndex =
            if (scrollState.anchorItemScrollOffset < 0) anchorIndex else anchorIndex + 1

        val targetItem = layoutInfo.visibleItems.fastFirstOrNull { it.index == targetIndex }

        if (targetItem != null && targetItem.measuredHeight < layoutInfo.viewportSize.height) {
            scrollState.animateScrollBy(
                targetItem.offset + targetItem.measuredHeight / 2f -
                    layoutInfo.viewportSize.height.toFloat() / 2f
            )
        } else {
            scrollState.animateScrollBy(layoutInfo.viewportSize.height * 0.5f)
        }
    }

    /**
     * A scroll implementation tailored for use with [ScalingLazyListState].
     *
     * This logic handles one-handed gesture by first attempting to scroll to the next item in the
     * list (or scrolling through the current item if it exceeds the viewport size). If the list
     * cannot scroll further forward, it scrolls back to the start.
     *
     * Sample demonstrating gesture handling with [ScalingLazyListState]:
     *
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnScrollToNextItemSample
     * @param scrollState The scroll state associated with a scaling lazy column.
     */
    public suspend fun scrollToNextItem(scrollState: ScalingLazyListState) {
        if (!scrollState.canScrollForward) {
            scrollState.animateScrollToItem(0)
            return
        }

        val layoutInfo = scrollState.layoutInfo
        val anchorIndex = scrollState.centerItemIndex

        var itemCenterOffset = scrollState.centerItemScrollOffset.toFloat()
        if (layoutInfo.anchorType == ScalingLazyListAnchorType.ItemStart) {
            val centerItem =
                layoutInfo.visibleItemsInfo.fastFirstOrNull {
                    it.index == scrollState.centerItemIndex
                }
            itemCenterOffset += (centerItem?.size ?: 0) / 2.0f
        }

        val targetIndex = if (itemCenterOffset < 0) anchorIndex else anchorIndex + 1

        val targetItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }

        if (targetItem != null && targetItem.size < layoutInfo.viewportSize.height) {
            var offsetFromItemCenter = targetItem.offset.toFloat()
            if (layoutInfo.anchorType == ScalingLazyListAnchorType.ItemStart) {
                offsetFromItemCenter += targetItem.size / 2.0f
            }

            scrollState.animateScrollBy(offsetFromItemCenter)
        } else {
            scrollState.animateScrollBy(layoutInfo.viewportSize.height * 0.5f)
        }
    }

    /**
     * Automatically animates the [pagerState] to the next available page.
     *
     * This function triggers a smooth scroll transition to the next page index. If the current page
     * is the last page in the pager, the animation will wrap around to the first page (index 0).
     *
     * Samples demonstrating gesture handling with horizontal and vertical pagers:
     *
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
     * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
     * @param pagerState The state of the pager to be animated.
     */
    public suspend fun scrollToNextPage(pagerState: PagerState) {
        pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
    }
}
