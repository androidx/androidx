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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.CacheWindowScope
import androidx.compose.foundation.lazy.layout.InvalidIndex
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.MultiLaneCacheWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridCacheWindowLogic(
    override val cacheWindow: LazyLayoutCacheWindow,
    laneInfo: LazyStaggeredGridLaneInfo,
    laneCount: () -> Int,
    prefetchState: LazyLayoutPrefetchState,
    isRequestHighPriority: () -> Boolean,
) : CacheWindowLogic by MultiLaneCacheWindow(cacheWindow = cacheWindow, laneCount = laneCount) {
    internal val cacheWindowScope =
        LazyStaggeredGridCacheWindowScope(
            laneInfo = laneInfo,
            prefetchState = prefetchState,
            isRequestHighPriority = isRequestHighPriority,
        )

    fun onScroll(delta: Float, layoutInfo: LazyStaggeredGridMeasureResult) {
        applyWindowScope(layoutInfo) { onScroll(delta) }
    }

    fun onVisibleItemsUpdated(layoutInfo: LazyStaggeredGridMeasureResult) {
        applyWindowScope(layoutInfo) { onVisibleItemsUpdated() }
    }

    private inline fun applyWindowScope(
        layoutInfo: LazyStaggeredGridMeasureResult,
        crossinline block: CacheWindowScope.() -> Unit,
    ) {
        cacheWindowScope.layoutInfo = layoutInfo
        block(cacheWindowScope)
    }
}

internal class LazyStaggeredGridCacheWindowScope(
    private val prefetchState: LazyLayoutPrefetchState,
    private val isRequestHighPriority: () -> Boolean,
    private val laneInfo: LazyStaggeredGridLaneInfo,
) : CacheWindowScope {
    lateinit var layoutInfo: LazyStaggeredGridMeasureResult

    override val totalItemsCount: Int
        get() = layoutInfo.totalItemsCount

    override val visibleItemsCount: Int
        get() = layoutInfo.visibleItemsInfo.size

    override val hasVisibleItems: Boolean
        get() = layoutInfo.visibleItemsInfo.isNotEmpty()

    override val firstVisibleItemIndex: Int
        get() = layoutInfo.visibleItemsInfo.first().index

    override val density: Density
        get() = layoutInfo.density

    override val lastVisibleItemIndex: Int
        get() = layoutInfo.visibleItemsInfo.last().index

    override val mainAxisViewportSize: Int
        get() = layoutInfo.singleAxisViewportSize

    override fun updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace: IntArray) {
        layoutInfo.firstVisibleItemScrollOffsets.forEachIndexed { lane, scrollOffset ->
            perLaneMainAxisExtraStartSpace[lane] = scrollOffset
        }
    }

    private var _reusableScratchBuffer: IntArray? = null

    private val reusableScratchBuffer: IntArray
        get() =
            _reusableScratchBuffer?.takeIf { it.size == layoutInfo.laneCount }
                ?: IntArray(layoutInfo.laneCount).also { _reusableScratchBuffer = it }

    override fun updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace: IntArray) {
        layoutInfo.lastVisibleItemIndexesAndEndOffsets(
            reusableScratchBuffer,
            perLaneMainAxisExtraEndSpace,
        )
        perLaneMainAxisExtraEndSpace.apply {
            forEachIndexed { lane, lastItemIndexOffset ->
                this[lane] =
                    if (lastItemIndexOffset == Int.MIN_VALUE) {
                        0
                    } else {
                        (lastItemIndexOffset + layoutInfo.mainAxisItemSpacing -
                                layoutInfo.viewportEndOffset)
                            .coerceAtLeast(0)
                    }
            }
        }
    }

    override fun updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex: IntArray) {
        layoutInfo.firstVisibleItemIndices.forEachIndexed { lane, itemIndex ->
            perLaneFirstVisibleItemIndex[lane] = itemIndex
        }
    }

    override fun updatePerLaneLastVisibleItemIndexes(perLaneLastVisibleItemIndexes: IntArray) {
        layoutInfo.lastVisibleItemIndexesAndEndOffsets(
            perLaneLastVisibleItemIndexes,
            reusableScratchBuffer,
        )
    }

    override fun schedulePrefetch(
        lane: Int,
        itemIndex: Int,
        onItemPrefetched: (itemSize: Int) -> Unit,
    ): List<LazyLayoutPrefetchState.PrefetchHandle> =
        layoutInfo.schedulePrefetch(
            prefetchState = prefetchState,
            lane = lane,
            itemIndex = itemIndex,
            isRequestHighPriority = isRequestHighPriority(),
            onItemPrefetched = onItemPrefetched,
        )

    override fun getVisibleItemSize(indexInVisibleItems: Int): Int =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].size.run {
            if (layoutInfo.orientation == Orientation.Vertical) height else width
        }

    override fun getVisibleItemIndex(indexInVisibleItems: Int) =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].index

    override fun getVisibleItemKey(indexInVisibleItems: Int) =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].key

    override fun getVisibleItemLane(indexInVisibleItems: Int) =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].lane

    override fun lastItemIndexInLine(currentItemIndex: Int) = currentItemIndex

    override fun getLastItemIndex() = (totalItemsCount - 1).coerceAtLeast(0)

    override fun getNextEndItemIndexInLane(lane: Int, currentItemIndex: Int) =
        with(laneInfo) {
            val nextItemIndex = findNextItemIndex(currentItemIndex, lane)
            if (nextItemIndex >= upperBound()) {
                setLane(
                    nextItemIndex,
                    if (isSpanItem(nextItemIndex)) {
                        LazyStaggeredGridLaneInfo.LaneFullSpan
                    } else {
                        lane
                    },
                )
            } else {
                if (isSpanItem(nextItemIndex)) {
                    setLane(nextItemIndex, LazyStaggeredGridLaneInfo.LaneFullSpan)
                } else {
                    if (getLane(nextItemIndex) == LazyStaggeredGridLaneInfo.LaneUnset) {
                        setLane(nextItemIndex, lane)
                    }
                }
            }
            return@with nextItemIndex
        }

    override fun getNextStartItemIndexInLane(lane: Int, currentItemIndex: Int) =
        with(laneInfo) {
            if (currentItemIndex < layoutInfo.laneCount) return -1
            val previousItemIndex = findPreviousItemIndex(currentItemIndex, lane)
            return@with if (previousItemIndex == -1) {
                val calculatedPreviousIndex = (currentItemIndex - 1).coerceAtLeast(0)
                if (isSpanItem(calculatedPreviousIndex)) {
                    setLane(calculatedPreviousIndex, LazyStaggeredGridLaneInfo.LaneFullSpan)
                } else {
                    if (getLane(calculatedPreviousIndex) == LazyStaggeredGridLaneInfo.LaneUnset) {
                        setLane(calculatedPreviousIndex, lane)
                    }
                }
                calculatedPreviousIndex
            } else {
                if (isSpanItem(previousItemIndex)) {
                    setLane(previousItemIndex, LazyStaggeredGridLaneInfo.LaneFullSpan)
                } else {
                    if (getLane(previousItemIndex) == LazyStaggeredGridLaneInfo.LaneUnset) {
                        setLane(previousItemIndex, lane)
                    }
                }
                previousItemIndex
            }
        }

    override fun isSpanItem(itemIndex: Int) = layoutInfo.spanProvider.isFullSpan(itemIndex)
}

internal fun LazyStaggeredGridMeasureResult.lastVisibleItemIndexesAndEndOffsets(
    perLaneLastVisibleItemIndexes: IntArray,
    perLaneMainAxisExtraEndSpace: IntArray,
) {
    perLaneLastVisibleItemIndexes.fill(InvalidIndex)
    perLaneMainAxisExtraEndSpace.fill(Int.MIN_VALUE)

    visibleItemsInfo.fastForEach { item ->
        val lane = item.lane
        val itemEndOffset =
            item.run {
                if (orientation == Orientation.Vertical) {
                    offset.y + size.height
                } else {
                    offset.x + size.width
                }
            }

        val laneCurrentMaxEndOffset = perLaneMainAxisExtraEndSpace[lane]

        if (itemEndOffset > laneCurrentMaxEndOffset) {
            perLaneLastVisibleItemIndexes[lane] = item.index
            perLaneMainAxisExtraEndSpace[lane] = itemEndOffset
        }
    }
}

internal fun LazyStaggeredGridMeasureResult.schedulePrefetch(
    prefetchState: LazyLayoutPrefetchState,
    lane: Int,
    itemIndex: Int,
    isRequestHighPriority: Boolean,
    onItemPrefetched: (size: Int) -> Unit,
): List<LazyLayoutPrefetchState.PrefetchHandle> =
    with(prefetchState) {
        val isFullSpan = spanProvider.isFullSpan(itemIndex)
        val slot = if (isFullSpan) 0 else lane
        val span = if (isFullSpan) laneCount else 1
        val crossAxisSize =
            if (span == 1) {
                slots.sizes[slot]
            } else {
                val start = slots.positions[slot]
                val endSlot = slot + span - 1
                val end = slots.run { positions[endSlot] + sizes[endSlot] }
                end - start
            }
        val constraints =
            if (orientation == Orientation.Vertical) {
                Constraints.fixedWidth(crossAxisSize)
            } else {
                Constraints.fixedHeight(crossAxisSize)
            }
        val handle =
            schedulePrecompositionAndPremeasure(itemIndex, constraints, isRequestHighPriority) {
                var itemMainAxisSize = 0
                repeat(placeablesCount) {
                    itemMainAxisSize +=
                        if (orientation == Orientation.Vertical) {
                            getSize(it).height
                        } else {
                            getSize(it).width
                        }
                }
                onItemPrefetched(itemMainAxisSize)
            }
        return listOf(handle)
    }
