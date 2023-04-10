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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

/**
 * Common parts backing the interval-based content of lazy layout defined through `item` DSL.
 */
@ExperimentalFoundationApi
abstract class LazyLayoutIntervalContent<Interval : LazyLayoutIntervalContent.Interval> {
    abstract val intervals: IntervalList<Interval>

    val itemCount: Int get() = intervals.size

    fun getKey(index: Int): Any =
        withLocalIntervalIndex(index) { localIndex, content ->
            content.key?.invoke(localIndex) ?: getDefaultLazyLayoutKey(index)
        }

    fun getContentType(index: Int): Any? =
        withLocalIntervalIndex(index) { localIndex, content ->
            content.type.invoke(localIndex)
        }

    private inline fun <T> withLocalIntervalIndex(
        index: Int,
        block: (localIndex: Int, content: Interval) -> T
    ): T {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return block(localIntervalIndex, interval.value)
    }

    /**
     * Common content of individual intervals in `item` DSL of lazy layouts.
     */
    @ExperimentalFoundationApi
    interface Interval {
        /**
         * Returns item key based on a local index for the current interval.
         */
        val key: ((index: Int) -> Any)? get() = null

        /**
         * Returns item type based on a local index for the current interval.
         */
        val type: ((index: Int) -> Any?) get() = { null }
    }
}

/**
 * Defines a composable content of item in a lazy layout to support focus pinning.
 * See [LazyLayoutPinnableItem] for more details.
 */
@ExperimentalFoundationApi
@Composable
fun <T : LazyLayoutIntervalContent.Interval> LazyLayoutIntervalContent<T>.PinnableItem(
    index: Int,
    pinnedItemList: LazyLayoutPinnedItemList,
    content: @Composable T.(index: Int) -> Unit
) {
    val interval = intervals[index]
    val localIndex = index - interval.startIndex
    LazyLayoutPinnableItem(
        interval.value.key?.invoke(localIndex),
        index,
        pinnedItemList
    ) {
        interval.value.content(localIndex)
    }
}