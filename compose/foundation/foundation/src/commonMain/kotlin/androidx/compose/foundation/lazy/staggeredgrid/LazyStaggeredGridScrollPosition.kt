/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutNearestRangeState
import androidx.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.util.fastFirstOrNull

@ExperimentalFoundationApi
internal class LazyStaggeredGridScrollPosition(
    initialIndices: IntArray,
    initialOffsets: IntArray,
    private val fillIndices: (targetIndex: Int, laneCount: Int) -> IntArray
) {
    var indices = initialIndices
        private set
    var index by mutableIntStateOf(calculateFirstVisibleIndex(initialIndices))
        private set
    var scrollOffsets = initialOffsets
        private set
    var scrollOffset by mutableIntStateOf(
        calculateFirstVisibleScrollOffset(initialIndices, initialOffsets)
    )
        private set

    private fun calculateFirstVisibleIndex(indices: IntArray): Int {
        var minIndex = Int.MAX_VALUE
        indices.forEach { index ->
            // index array can contain -1, indicating lane being empty (cell number > itemCount)
            // if any of the lanes are empty, we always on 0th item index
            if (index <= 0) return 0
            if (minIndex > index) minIndex = index
        }
        return if (minIndex == Int.MAX_VALUE) 0 else minIndex
    }

    private fun calculateFirstVisibleScrollOffset(indices: IntArray, offsets: IntArray): Int {
        var minOffset = Int.MAX_VALUE
        val smallestIndex = calculateFirstVisibleIndex(indices)
        for (lane in offsets.indices) {
            if (indices[lane] == smallestIndex) {
                minOffset = minOf(minOffset, offsets[lane])
            }
        }
        return if (minOffset == Int.MAX_VALUE) 0 else minOffset
    }

    private var hadFirstNotEmptyLayout = false

    /** The last know key of the item at lowest of [indices] position. */
    private var lastKnownFirstItemKey: Any? = null

    val nearestRangeState = LazyLayoutNearestRangeState(
        initialIndices.minOrNull() ?: 0,
        NearestItemsSlidingWindowSize,
        NearestItemsExtraItemCount
    )

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: LazyStaggeredGridMeasureResult) {
        val firstVisibleIndex = calculateFirstVisibleIndex(measureResult.firstVisibleItemIndices)

        lastKnownFirstItemKey = measureResult.visibleItemsInfo
            .fastFirstOrNull { it.index == firstVisibleIndex }
            ?.key
        nearestRangeState.update(firstVisibleIndex)
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real items. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.totalItemsCount > 0) {
            hadFirstNotEmptyLayout = true
            Snapshot.withoutReadObservation {
                update(
                    measureResult.firstVisibleItemIndices,
                    measureResult.firstVisibleItemScrollOffsets
                )
            }
        }
    }

    fun updateScrollOffset(scrollOffsets: IntArray) {
        this.scrollOffsets = scrollOffsets
        this.scrollOffset = calculateFirstVisibleScrollOffset(indices, scrollOffsets)
    }

    /**
     * Updates the scroll position - the passed values will be used as a start position for
     * composing the items during the next measure pass and will be updated by the real
     * position calculated during the measurement. This means that there is no guarantee that
     * exactly this index and offset will be applied as it is possible that:
     * a) there will be no item at this index in reality
     * b) item at this index will be smaller than the asked scrollOffset, which means we would
     * switch to the next item
     * c) there will be not enough items to fill the viewport after the requested index, so we
     * would have to compose few elements before the asked index, changing the first visible item.
     */
    fun requestPositionAndForgetLastKnownKey(index: Int, scrollOffset: Int) {
        val newIndices = fillIndices(index, indices.size)
        val newOffsets = IntArray(newIndices.size) { scrollOffset }
        update(newIndices, newOffsets)
        nearestRangeState.update(index)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [updateScrollPositionIfTheFirstItemWasMoved] shouldn't override this.
        lastKnownFirstItemKey = null
    }

    /**
     * In addition to keeping the first visible item index we also store the key of this item.
     * When the user provided custom keys for the items this mechanism allows us to detect when
     * there were items added or removed before our current first visible item and keep this item
     * as the first visible one even given that its index has been changed.
     */
    @ExperimentalFoundationApi
    fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: LazyLayoutItemProvider,
        indices: IntArray
    ): IntArray {
        val newIndex = itemProvider.findIndexByKey(
            key = lastKnownFirstItemKey,
            lastKnownIndex = indices.getOrNull(0) ?: 0
        )
        return if (newIndex !in indices) {
            nearestRangeState.update(newIndex)
            val newIndices = Snapshot.withoutReadObservation { fillIndices(newIndex, indices.size) }
            this.indices = newIndices
            this.index = calculateFirstVisibleIndex(newIndices)
            newIndices
        } else {
            indices
        }
    }

    private fun update(indices: IntArray, offsets: IntArray) {
        this.indices = indices
        this.index = calculateFirstVisibleIndex(indices)
        this.scrollOffsets = offsets
        this.scrollOffset = calculateFirstVisibleScrollOffset(indices, offsets)
    }
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private const val NearestItemsSlidingWindowSize = 90

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private const val NearestItemsExtraItemCount = 200
