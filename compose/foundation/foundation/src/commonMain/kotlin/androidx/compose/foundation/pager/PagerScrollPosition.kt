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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Contains the current scroll position represented by the first visible page  and the first
 * visible page scroll offset.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PagerScrollPosition(
    initialPage: Int = 0,
    initialScrollOffset: Int = 0
) {
    var firstVisiblePage by mutableStateOf(initialPage)
    var currentPage by mutableStateOf(initialPage)

    var scrollOffset by mutableStateOf(initialScrollOffset)
        private set

    private var hadFirstNotEmptyLayout = false

    /** The last know key of the page at [firstVisiblePage] position. */
    private var lastKnownFirstPageKey: Any? = null

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: PagerMeasureResult) {
        lastKnownFirstPageKey = measureResult.firstVisiblePage?.key
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real pages. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.pagesCount > 0) {
            hadFirstNotEmptyLayout = true
            val scrollOffset = measureResult.firstVisiblePageOffset
            check(scrollOffset >= 0f) { "scrollOffset should be non-negative ($scrollOffset)" }

            Snapshot.withoutReadObservation {
                update(
                    measureResult.firstVisiblePage?.index ?: 0,
                    scrollOffset
                )
                measureResult.closestPageToSnapPosition?.index?.let {
                    if (it != this.currentPage) {
                        this.currentPage = it
                    }
                }
            }
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
    fun requestPosition(index: Int, scrollOffset: Int) {
        update(index, scrollOffset)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [checkIfFirstVisibleItemWasMoved] shouldn't override this.
        lastKnownFirstPageKey = null
    }

    private fun update(index: Int, scrollOffset: Int) {
        require(index >= 0f) { "Index should be non-negative ($index)" }
        if (index != this.firstVisiblePage) {
            this.firstVisiblePage = index
        }
        if (scrollOffset != this.scrollOffset) {
            this.scrollOffset = scrollOffset
        }
    }
}