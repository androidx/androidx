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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * A key-index mapping used inside the [LazyLayoutItemProvider]. It might not contain all items
 * in the lazy layout as optimization, but it must cover items the provider is requesting
 * during layout pass.
 * See [NearestRangeKeyIndexMap] as sample implementation that samples items near current viewport.
 */
internal interface LazyLayoutKeyIndexMap {
    /**
     * @return current index for given [key] or `-1` if not found.
     */
    operator fun get(key: Any): Int

    /**
     * Empty map implementation, always returning `-1` for any key.
     */
    companion object Empty : LazyLayoutKeyIndexMap {
        @Suppress("AutoBoxing")
        override fun get(key: Any): Int = -1
    }
}

/**
 * State containing [LazyLayoutKeyIndexMap] precalculated for range of indexes near first visible
 * item.
 * It is optimized to return the same range for small changes in the firstVisibleItemIndex
 * value so we do not regenerate the map on each scroll.
 *
 * @param firstVisibleItemIndex Provider of the first item index currently visible on screen.
 * @param slidingWindowSize Number of items between current and `firstVisibleItem` until
 * [LazyLayoutKeyIndexMap] is regenerated.
 * @param extraItemCount The minimum amount of items in one direction near the first visible item
 * to calculate mapping for.
 * @param content Provider of [LazyLayoutIntervalContent] to generate key index mapping for.
 */
@ExperimentalFoundationApi
internal class NearestRangeKeyIndexMapState(
    firstVisibleItemIndex: () -> Int,
    slidingWindowSize: () -> Int,
    extraItemCount: () -> Int,
    content: () -> LazyLayoutIntervalContent<*>
) : State<LazyLayoutKeyIndexMap> {
    private val nearestRangeState by derivedStateOf(structuralEqualityPolicy()) {
        if (content().itemCount < extraItemCount() * 2 + slidingWindowSize()) {
            0 until content().itemCount
        } else {
            calculateNearestItemsRange(
                firstVisibleItemIndex(),
                slidingWindowSize(),
                extraItemCount()
            )
        }
    }

    override val value: LazyLayoutKeyIndexMap by derivedStateOf(referentialEqualityPolicy()) {
        NearestRangeKeyIndexMap(nearestRangeState, content())
    }
}

/**
 * Implementation of [LazyLayoutKeyIndexMap] indexing over given [IntRange] of items.
 * Items outside of given range are considered unknown, with null returned as the index.
 */
@ExperimentalFoundationApi
private class NearestRangeKeyIndexMap(
    nearestRange: IntRange,
    content: LazyLayoutIntervalContent<*>
) : LazyLayoutKeyIndexMap {
    private val map = generateKeyToIndexMap(nearestRange, content.intervals)

    override fun get(key: Any): Int = map.getOrElse(key) { -1 }

    companion object {
        /**
         * Traverses the interval [list] in order to create a mapping from the key to the index for all
         * the indexes in the passed [range].
         * The returned map will not contain the values for intervals with no key mapping provided.
         */
        private fun generateKeyToIndexMap(
            range: IntRange,
            list: IntervalList<LazyLayoutIntervalContent.Interval>
        ): Map<Any, Int> {
            val first = range.first
            check(first >= 0)
            val last = minOf(range.last, list.size - 1)
            return if (last < first) {
                emptyMap()
            } else {
                hashMapOf<Any, Int>().also { map ->
                    list.forEach(
                        fromIndex = first,
                        toIndex = last,
                    ) {
                        if (it.value.key != null) {
                            val keyFactory = requireNotNull(it.value.key)
                            val start = maxOf(first, it.startIndex)
                            val end = minOf(last, it.startIndex + it.size - 1)
                            for (i in start..end) {
                                map[keyFactory(i - it.startIndex)] = i
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a range of indexes which contains at least [extraItemCount] items near
 * the first visible item. It is optimized to return the same range for small changes in the
 * firstVisibleItem value so we do not regenerate the map on each scroll.
 */
private fun calculateNearestItemsRange(
    firstVisibleItem: Int,
    slidingWindowSize: Int,
    extraItemCount: Int
): IntRange {
    val slidingWindowStart = slidingWindowSize * (firstVisibleItem / slidingWindowSize)

    val start = maxOf(slidingWindowStart - extraItemCount, 0)
    val end = slidingWindowStart + slidingWindowSize + extraItemCount
    return start until end
}