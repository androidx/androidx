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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyGridItemSpanScope
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridSpanLayoutProvider(private val itemsProvider: LazyGridItemsProvider) {
    class LineConfiguration(val firstItemIndex: Int, val spans: List<GridItemSpan>)

    /** Caches the index of the first item on lines 0, [bucketSize], 2 * [bucketSize], etc. */
    private val bucketStartItemIndex = ArrayList<Int>().apply { add(0) }
    /**
     * Caches the span of the first item on lines 0, [bucketSize], 2 * [bucketSize], etc., but
     * only if this item caused "line break".
     */
    private val bucketStartKnownSpan = ArrayList<Int>().apply { add(0) }
    /**
     * The interval at each we will store the starting element of lines. These will be then
     * used to calculate the layout of arbitrary lines, by starting from the closest
     * known "bucket start". The smaller the bucketSize, the smaller cost for calculating layout
     * of arbitrary lines but the higher memory usage for [bucketStartItemIndex].
     */
    private val bucketSize get() = sqrt(1.0 * totalSize / slotsPerLine).toInt() + 1
    /** Caches the last calculated line index, useful when scrolling in main axis direction. */
    private var lastLineIndex = 0
    /** Caches the starting item index on [lastLineIndex]. */
    private var lastLineStartItemIndex = 0
    /** Caches the span of [lastLineStartItemIndex], if this was already calculated. */
    private var lastLineStartKnownSpan = 0
    /**
     * Caches a calculated bucket, this is useful when scrolling in reverse main axis
     * direction. We cannot only keep the last element, as we would not know previous max span.
     */
    private var cachedBucketIndex = -1
    /**
     * Caches layout of [cachedBucketIndex], this is useful when scrolling in reverse main axis
     * direction. We cannot only keep the last element, as we would not know previous max span.
     */
    private val cachedBucket = mutableListOf<Int>()
    /**
     * List of 1x1 spans if we do not have custom spans.
     */
    private var previousDefaultSpans = emptyList<GridItemSpan>()
    private fun getDefaultSpans(currentSlotsPerLine: Int) =
        if (currentSlotsPerLine == previousDefaultSpans.size) {
            previousDefaultSpans
        } else {
            List(currentSlotsPerLine) { GridItemSpan(1) }.also { previousDefaultSpans = it }
        }

    val totalSize get() = itemsProvider.itemsCount

    /** The number of slots on one grid line e.g. the number of columns of a vertical grid. */
    var slotsPerLine = 0
        set(value) {
            if (value != field) {
                field = value
                invalidateCache()
            }
        }

    fun getLineConfiguration(lineIndex: Int): LineConfiguration {
        if (!itemsProvider.hasCustomSpans) {
            // Quick return when all spans are 1x1 - in this case we can easily calculate positions.
            val firstItemIndex = lineIndex * slotsPerLine
            return LineConfiguration(
                firstItemIndex,
                getDefaultSpans(slotsPerLine.coerceAtMost(totalSize - firstItemIndex)
                    .coerceAtLeast(0))
            )
        }

        val bucket = lineIndex / bucketSize
        // We can calculate the items on the line from the closest cached bucket start item.
        var currentLine = min(bucket, bucketStartItemIndex.size - 1) * bucketSize
        var currentItemIndex = bucketStartItemIndex[min(bucket, bucketStartItemIndex.size - 1)]
        var knownCurrentItemSpan = bucketStartKnownSpan[min(bucket, bucketStartItemIndex.size - 1)]
        // ... but try using the more localised cached values.
        if (lastLineIndex in currentLine..lineIndex) {
            // The last calculated value is a better start point. Common when scrolling main axis.
            currentLine = lastLineIndex
            currentItemIndex = lastLineStartItemIndex
            knownCurrentItemSpan = lastLineStartKnownSpan
        } else if (bucket == cachedBucketIndex && lineIndex - currentLine < cachedBucket.size) {
            // It happens that the needed line start is fully cached. Common when scrolling in
            // reverse main axis, as we decided to cacheThisBucket previously.
            currentItemIndex = cachedBucket[lineIndex - currentLine]
            currentLine = lineIndex
            knownCurrentItemSpan = 0
        }

        val cacheThisBucket = currentLine % bucketSize == 0 &&
            lineIndex - currentLine in 2 until bucketSize
        if (cacheThisBucket) {
            cachedBucketIndex = bucket
            cachedBucket.clear()
        }

        check(currentLine <= lineIndex)

        while (currentLine < lineIndex && currentItemIndex < totalSize) {
            if (cacheThisBucket) {
                cachedBucket.add(currentItemIndex)
            }

            var spansUsed = 0
            while (spansUsed < slotsPerLine && currentItemIndex < totalSize) {
                val span = if (knownCurrentItemSpan == 0) {
                    spanOf(currentItemIndex, slotsPerLine - spansUsed)
                } else {
                    knownCurrentItemSpan.also { knownCurrentItemSpan = 0 }
                }
                if (spansUsed + span > slotsPerLine) {
                    knownCurrentItemSpan = span
                    break
                }

                currentItemIndex++
                spansUsed += span
            }
            ++currentLine
            if (currentLine % bucketSize == 0 && currentItemIndex < totalSize) {
                val currentLineBucket = currentLine / bucketSize
                // This should happen, as otherwise this should have been used as starting point.
                check(bucketStartItemIndex.size == currentLineBucket)
                bucketStartItemIndex.add(currentItemIndex)
                bucketStartKnownSpan.add(knownCurrentItemSpan)
            }
        }

        lastLineIndex = lineIndex
        lastLineStartItemIndex = currentItemIndex
        lastLineStartKnownSpan = knownCurrentItemSpan

        val firstItemIndex = currentItemIndex
        val spans = mutableListOf<GridItemSpan>()

        var spansUsed = 0
        while (spansUsed < slotsPerLine && currentItemIndex < totalSize) {
            val span = if (knownCurrentItemSpan == 0) {
                spanOf(currentItemIndex, slotsPerLine - spansUsed)
            } else {
                knownCurrentItemSpan.also { knownCurrentItemSpan = 0 }
            }
            if (spansUsed + span > slotsPerLine) break

            currentItemIndex++
            spans.add(GridItemSpan(span))
            spansUsed += span
        }
        return LineConfiguration(firstItemIndex, spans)
    }

    /**
     * Calculate the line of index [itemIndex].
     */
    fun getLineIndexOfItem(itemIndex: Int): LineIndex {
        if (totalSize <= 0) {
            return LineIndex(0)
        }
        require(itemIndex < totalSize)
        if (!itemsProvider.hasCustomSpans) {
            return LineIndex(itemIndex / slotsPerLine)
        }

        val lowerBoundBucket = bucketStartItemIndex.binarySearch { it - itemIndex }.let {
            if (it >= 0) it else -it - 2
        }
        var currentLine = lowerBoundBucket * bucketSize
        var currentItemIndex = bucketStartItemIndex[lowerBoundBucket]

        require(currentItemIndex <= itemIndex)
        var spansUsed = 0
        while (currentItemIndex < itemIndex) {
            val span = spanOf(currentItemIndex++, slotsPerLine - spansUsed)
            if (spansUsed + span < slotsPerLine) {
                spansUsed += span
            } else if (spansUsed + span == slotsPerLine) {
                ++currentLine
                spansUsed = 0
            } else {
                // spansUsed + span > slotsPerLine
                ++currentLine
                spansUsed = span
            }
            if (currentLine % bucketSize == 0) {
                val currentLineBucket = currentLine / bucketSize
                if (currentLineBucket >= bucketStartItemIndex.size) {
                    bucketStartItemIndex.add(currentItemIndex - if (spansUsed > 0) 1 else 0)
                }
            }
        }
        if (spansUsed + spanOf(itemIndex, slotsPerLine - spansUsed) > slotsPerLine) {
            ++currentLine
        }

        return LineIndex(currentLine)
    }

    private fun spanOf(itemIndex: Int, maxSpan: Int) = with(itemsProvider) {
        with(LazyGridItemSpanScopeImpl) {
            maxCurrentLineSpan = maxSpan
            maxLineSpan = slotsPerLine

            getSpan(itemIndex).currentLineSpan.coerceIn(1, slotsPerLine)
        }
    }

    private fun invalidateCache() {
        bucketStartItemIndex.clear()
        bucketStartItemIndex.add(0)
        lastLineIndex = 0
        lastLineStartItemIndex = 0
        cachedBucketIndex = -1
        cachedBucket.clear()
    }

    private object LazyGridItemSpanScopeImpl : LazyGridItemSpanScope {
        override var maxCurrentLineSpan = 0
        override var maxLineSpan = 0
    }
}