/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutNearestRangeState
import androidx.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToLong

/**
 * Contains the current scroll position represented by the first visible page  and the first
 * visible page scroll offset.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PagerScrollPosition(
    currentPage: Int = 0,
    currentPageOffsetFraction: Float = 0.0f,
    val state: PagerState
) {
    var currentPage by mutableIntStateOf(currentPage)
        private set

    var currentPageOffsetFraction by mutableFloatStateOf(currentPageOffsetFraction)
        private set

    private var hadFirstNotEmptyLayout = false

    /** The last know key of the page at [currentPage] position. */
    private var lastKnownCurrentPageKey: Any? = null

    val nearestRangeState = LazyLayoutNearestRangeState(
        currentPage,
        NearestItemsSlidingWindowSize,
        NearestItemsExtraItemCount
    )

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: PagerMeasureResult) {
        lastKnownCurrentPageKey = measureResult.currentPage?.key
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real pages. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.visiblePagesInfo.isNotEmpty()) {
            hadFirstNotEmptyLayout = true

            update(
                measureResult.currentPage?.index ?: 0,
                measureResult.currentPageOffsetFraction
            )
        }
    }

    /**
     * Updates the scroll position - the passed values will be used as a start position for
     * composing the pages during the next measure pass and will be updated by the real
     * position calculated during the measurement. This means that there is no guarantee that
     * exactly this index and offset will be applied as it is possible that:
     * a) there will be no page at this index in reality
     * b) page at this index will be smaller than the asked scrollOffset, which means we would
     * switch to the next page
     * c) there will be not enough pages to fill the viewport after the requested index, so we
     * would have to compose few elements before the asked index, changing the first visible page.
     */
    fun requestPosition(index: Int, offsetFraction: Float) {
        update(index, offsetFraction)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [checkIfFirstVisibleItemWasMoved] shouldn't override this.
        lastKnownCurrentPageKey = null
    }

    fun matchPageWithKey(
        itemProvider: PagerLazyLayoutItemProvider,
        index: Int
    ): Int {
        val newIndex = itemProvider.findIndexByKey(lastKnownCurrentPageKey, index)
        if (index != newIndex) {
            currentPage = newIndex
            nearestRangeState.update(index)
        }
        return newIndex
    }

    private fun update(page: Int, offsetFraction: Float) {
        currentPage = page
        nearestRangeState.update(page)
        currentPageOffsetFraction = offsetFraction
    }

    fun updateCurrentPageOffsetFraction(offsetFraction: Float) {
        currentPageOffsetFraction = offsetFraction
    }

    fun currentAbsoluteScrollOffset(): Long {
        val currentPageOffset = currentPage.toLong() * state.pageSizeWithSpacing
        val offsetFraction = (currentPageOffsetFraction * state.pageSizeWithSpacing).roundToLong()
        return currentPageOffset + offsetFraction
    }

    fun applyScrollDelta(delta: Int) {
        debugLog { "Applying Delta=$delta" }
        val fractionUpdate = if (state.pageSizeWithSpacing == 0) {
            0.0f
        } else {
            delta / state.pageSizeWithSpacing.toFloat()
        }
        currentPageOffsetFraction += fractionUpdate
    }
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
internal const val NearestItemsSlidingWindowSize = 30

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
internal const val NearestItemsExtraItemCount = 100

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.ScrollPosition) {
        println("PagerScrollPosition: ${generateMsg()}")
    }
}
