/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates a [ScalingLazyListState] that is remembered across compositions.
 */
@Composable
public fun rememberScalingLazyListState(): ScalingLazyListState {
    return rememberSaveable(saver = ScalingLazyListState.Saver) {
        ScalingLazyListState()
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 * TODO (b/193792848): Add scrolling and snap support.
 *
 * In most cases, this will be created via [rememberScalingLazyListState].
 */
@Stable
public class ScalingLazyListState : ScrollableState {
    internal var lazyListState: LazyListState = LazyListState(0, 0)
    internal val extraPaddingInPixels = mutableStateOf<Int?>(null)
    internal val scalingParams = mutableStateOf<ScalingParams?>(null)
    internal val gapBetweenItemsPx = mutableStateOf<Int?>(null)
    internal val viewportHeightPx = mutableStateOf<Int?>(null)
    internal val reverseLayout = mutableStateOf<Boolean?>(null)

    /**
     * The object of [ScalingLazyListLayoutInfo] calculated during the last layout pass. For
     * example, you can use it to calculate what items are currently visible.
     */
    public val layoutInfo: ScalingLazyListLayoutInfo by derivedStateOf {
        if (extraPaddingInPixels.value == null || scalingParams.value == null ||
            gapBetweenItemsPx.value == null || viewportHeightPx.value == null ||
            reverseLayout.value == null
        ) {
            EmptyScalingLazyListLayoutInfo
        } else {
            val visibleItemsInfo = mutableListOf<ScalingLazyListItemInfo>()
            var centralItemIndex = -1

            if (lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val verticalAdjustment =
                    lazyListState.layoutInfo.viewportStartOffset + extraPaddingInPixels.value!!

                // Find the item in the middle of the viewport
                val centralItem =
                    findItemNearestCenter(viewportHeightPx.value!!, verticalAdjustment)!!

                // Place the center item
                val centerItemInfo = createItemInfo(
                    centralItem.offset,
                    centralItem,
                    verticalAdjustment,
                    viewportHeightPx.value!!,
                    scalingParams.value!!,
                )
                visibleItemsInfo.add(
                    centerItemInfo
                )
                // Go Up
                centralItemIndex = centralItem.index
                var nextItemBottomNoPadding = centerItemInfo.offset - gapBetweenItemsPx.value!!
                val minIndex =
                    lazyListState.layoutInfo.visibleItemsInfo.minOf { it.index }
                (centralItemIndex - 1 downTo minIndex).forEach { ix ->
                    val currentItem =
                        lazyListState.layoutInfo.visibleItemsInfo.find { it.index == ix }!!
                    val itemInfo = createItemInfo(
                        nextItemBottomNoPadding - currentItem.size,
                        currentItem,
                        verticalAdjustment,
                        viewportHeightPx.value!!,
                        scalingParams.value!!,
                    )
                    // If the item is visible in the viewport insert it at the start of the
                    // list
                    if ((itemInfo.offset + itemInfo.size) > verticalAdjustment) {
                        // Insert the item info at the front of the list
                        visibleItemsInfo.add(0, itemInfo)
                    }
                    nextItemBottomNoPadding = itemInfo.offset - gapBetweenItemsPx.value!!
                }
                // Go Down
                var nextItemTopNoPadding =
                    centerItemInfo.offset + centerItemInfo.size +
                        gapBetweenItemsPx.value!!
                val maxIndex =
                    lazyListState.layoutInfo.visibleItemsInfo.maxOf { it.index }
                (centralItemIndex + 1..maxIndex).forEach { ix ->
                    val currentItem =
                        lazyListState.layoutInfo.visibleItemsInfo.find { it.index == ix }!!
                    val itemInfo = createItemInfo(
                        nextItemTopNoPadding,
                        currentItem,
                        verticalAdjustment,
                        viewportHeightPx.value!!,
                        scalingParams.value!!,
                    )
                    // If the item is visible in the viewport insert it at the end of the
                    // list
                    if ((itemInfo.offset - verticalAdjustment) < viewportHeightPx.value!!) {
                        visibleItemsInfo.add(itemInfo)
                    }
                    nextItemTopNoPadding =
                        itemInfo.offset + itemInfo.size + gapBetweenItemsPx.value!!
                }
            }
            DefaultScalingLazyListLayoutInfo(
                visibleItemsInfo = visibleItemsInfo,
                totalItemsCount = lazyListState.layoutInfo.totalItemsCount,
                viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset +
                    extraPaddingInPixels.value!!,
                viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset -
                    extraPaddingInPixels.value!!,
                centralItemIndex = centralItemIndex
            )
        }
    }

    private fun findItemNearestCenter(
        viewportHeightPx: Int,
        verticalAdjustment: Int
    ): LazyListItemInfo? {
        val centerLine = viewportHeightPx / 2
        var result: LazyListItemInfo? = null
        // Find the item in the middle of the viewport
        for (item in lazyListState.layoutInfo.visibleItemsInfo) {
            val rawItemStart = item.offset - verticalAdjustment
            val rawItemEnd = rawItemStart + item.size
            result = item
            if (rawItemEnd > centerLine) {
                break
            }
        }
        return result
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScalingLazyListState].
         */
        val Saver = listSaver<ScalingLazyListState, Int>(
            save = {
                listOf(
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset,
                )
            },
            restore = {
                val scalingLazyColumnState = ScalingLazyListState()
                scalingLazyColumnState.lazyListState = LazyListState(
                    firstVisibleItemIndex = it[0],
                    firstVisibleItemScrollOffset = it[1],
                )
                scalingLazyColumnState
            }
        )
    }

    override val isScrollInProgress: Boolean
        get() {
            return lazyListState.isScrollInProgress
        }

    override fun dispatchRawDelta(delta: Float): Float {
        return lazyListState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        lazyListState.scroll(scrollPriority = scrollPriority, block = block)
    }
}

private object EmptyScalingLazyListLayoutInfo : ScalingLazyListLayoutInfo {
    override val visibleItemsInfo = emptyList<ScalingLazyListItemInfo>()
    override val viewportStartOffset = 0
    override val viewportEndOffset = 0
    override val totalItemsCount = 0
    override val centralItemIndex = -1
}
