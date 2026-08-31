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

import androidx.annotation.VisibleForTesting
import androidx.collection.mutableScatterMapOf
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import kotlin.math.min
import kotlin.math.sqrt

internal class LazyGridSpanLayoutProvider(private val gridContent: LazyGridIntervalContent) {
    class LineConfiguration(val firstItemIndex: Int, val spans: List<GridItemSpan>)

    /**
     * Maps each snapshot state object read during custom span queries to its value at the time the
     * bucket cache was populated.
     *
     * Entries share the lifetime of [buckets] (and are cleared together in [invalidateCache])
     * because any state read while walking items to populate [buckets] affects all subsequent
     * cached bucket start indices.
     */
    private val observedStates = mutableScatterMapOf<Any, Any?>()

    /**
     * Records snapshot state reads during item span calculation walks.
     *
     * The read observer records every snapshot read inside the layout walk (including transitive
     * dependencies read when evaluating a `DerivedState`). This is safe because [buckets],
     * [totalSize], and [slotsPerLine] are plain properties, so only the user's span lambdas read
     * snapshot state.
     */
    private val spanReadObserver: (Any) -> Unit = { state ->
        if (state !in observedStates) {
            observedStates[state] = Snapshot.withoutReadObservation { readStateValue(state) }
        }
    }

    @get:VisibleForTesting
    internal val hasObservedStates: Boolean
        get() = observedStates.isNotEmpty()

    /**
     * Set by [getLineIndexOfItem] at the start of a measure pass (which runs inside
     * [Snapshot.withoutReadObservation] in `LazyGrid`) so that the first [getLineConfiguration]
     * call inside `measureLazyGrid` reads [observedStates] once under `SubcomposeLayout`'s
     * `SnapshotStateObserver`, while subsequent [getLineConfiguration] calls in the same line loop
     * skip it.
     */
    private var needsMeasureObservation = false

    /**
     * Stored line/item index arguments for [computeLineConfigurationBlock] and
     * [computeLineIndexOfItemBlock].
     *
     * Passing these via fields avoids allocating a lambda on every query. This is safe because:
     * 1. Reentrancy is impossible: [computeLineConfiguration] and [computeLineIndexOfItem] never
     *    call back into [getLineConfiguration] or [getLineIndexOfItem].
     * 2. Measure, layout, and prefetch run on the main UI thread (Android's `LazyLayoutPrefetcher`
     *    is Choreographer-driven).
     */
    private var pendingLineIndex = 0
    private val computeLineConfigurationBlock: () -> LineConfiguration = {
        computeLineConfiguration(pendingLineIndex)
    }

    private var pendingItemIndex = 0
    private val computeLineIndexOfItemBlock: () -> Int = {
        computeLineIndexOfItem(pendingItemIndex)
    }

    /** Caches the bucket info on lines 0, [bucketSize], 2 * [bucketSize], etc. */
    private val buckets = ArrayList<Bucket>().apply { add(Bucket(0)) }

    /**
     * The interval at each we will store the starting element of lines. These will be then used to
     * calculate the layout of arbitrary lines, by starting from the closest known "bucket start".
     * The smaller the bucketSize, the smaller cost for calculating layout of arbitrary lines but
     * the higher memory usage for [buckets].
     */
    private val bucketSize
        get() = sqrt(1.0 * totalSize / slotsPerLine).toInt() + 1

    /** Caches the last calculated line index, useful when scrolling in main axis direction. */
    private var lastLineIndex = 0

    /** Caches the starting item index on [lastLineIndex]. */
    private var lastLineStartItemIndex = 0

    /** Caches the span of [lastLineStartItemIndex], if this was already calculated. */
    private var lastLineStartKnownSpan = 0

    /**
     * Caches a calculated bucket, this is useful when scrolling in reverse main axis direction. We
     * cannot only keep the last element, as we would not know previous max span.
     */
    private var cachedBucketIndex = -1

    /**
     * Caches layout of [cachedBucketIndex], this is useful when scrolling in reverse main axis
     * direction. We cannot only keep the last element, as we would not know previous max span.
     */
    private val cachedBucket = mutableListOf<Int>()

    /** List of 1x1 spans if we do not have custom spans. */
    private var previousDefaultSpans = emptyList<GridItemSpan>()

    private fun getDefaultSpans(currentSlotsPerLine: Int) =
        if (currentSlotsPerLine == previousDefaultSpans.size) {
            previousDefaultSpans
        } else {
            List(currentSlotsPerLine) { GridItemSpan(1) }.also { previousDefaultSpans = it }
        }

    val totalSize
        get() = gridContent.intervals.size

    /** The number of slots on one grid line e.g. the number of columns of a vertical grid. */
    var slotsPerLine = 0
        set(value) {
            if (value != field) {
                field = value
                invalidateCache()
            }
        }

    /**
     * Returns the [LineConfiguration] for [lineIndex].
     *
     * If the grid has custom spans, the calculation is wrapped in [Snapshot.observe] to record any
     * [State] read by the span lambdas into [observedStates].
     */
    fun getLineConfiguration(lineIndex: Int): LineConfiguration {
        if (!gridContent.hasCustomSpans) {
            // Quick return when all spans are 1x1 - in this case we can easily calculate positions.
            val firstItemIndex = lineIndex * slotsPerLine
            return LineConfiguration(
                firstItemIndex,
                getDefaultSpans(
                    slotsPerLine.coerceAtMost(totalSize - firstItemIndex).coerceAtLeast(0)
                ),
            )
        }

        if (needsMeasureObservation) {
            needsMeasureObservation = false
            verifyAndReadObservedStates()
        }
        pendingLineIndex = lineIndex
        return Snapshot.observe(
            readObserver = spanReadObserver,
            block = computeLineConfigurationBlock,
        )
    }

    private fun computeLineConfiguration(lineIndex: Int): LineConfiguration {
        val bucketIndex = min(lineIndex / bucketSize, buckets.size - 1)
        // We can calculate the items on the line from the closest cached bucket start item.
        var currentLine = bucketIndex * bucketSize
        var currentItemIndex = buckets[bucketIndex].firstItemIndex
        var knownCurrentItemSpan = buckets[bucketIndex].firstItemKnownSpan
        // ... but try using the more localised cached values.
        if (lastLineIndex in currentLine..lineIndex) {
            // The last calculated value is a better start point. Common when scrolling main axis.
            currentLine = lastLineIndex
            currentItemIndex = lastLineStartItemIndex
            knownCurrentItemSpan = lastLineStartKnownSpan
        } else if (
            bucketIndex == cachedBucketIndex && lineIndex - currentLine < cachedBucket.size
        ) {
            // It happens that the needed line start is fully cached. Common when scrolling in
            // reverse main axis, as we decided to cacheThisBucket previously.
            currentItemIndex = cachedBucket[lineIndex - currentLine]
            currentLine = lineIndex
            knownCurrentItemSpan = 0
        }

        val cacheThisBucket =
            currentLine % bucketSize == 0 && lineIndex - currentLine in 2 until bucketSize
        if (cacheThisBucket) {
            cachedBucketIndex = bucketIndex
            cachedBucket.clear()
        }

        checkPrecondition(currentLine <= lineIndex) {
            "currentLine ($currentLine) > lineIndex ($lineIndex)"
        }

        while (currentLine < lineIndex && currentItemIndex < totalSize) {
            if (cacheThisBucket) {
                cachedBucket.add(currentItemIndex)
            }

            var spansUsed = 0
            while (spansUsed < slotsPerLine && currentItemIndex < totalSize) {
                val span =
                    if (knownCurrentItemSpan == 0) {
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
                checkPrecondition(buckets.size == currentLineBucket) { "invalid starting point" }
                buckets.add(Bucket(currentItemIndex, knownCurrentItemSpan))
            }
        }

        lastLineIndex = lineIndex
        lastLineStartItemIndex = currentItemIndex
        lastLineStartKnownSpan = knownCurrentItemSpan

        val firstItemIndex = currentItemIndex
        val spans = mutableListOf<GridItemSpan>()

        var spansUsed = 0
        while (spansUsed < slotsPerLine && currentItemIndex < totalSize) {
            val span =
                if (knownCurrentItemSpan == 0) {
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

    /** Calculate the line of index [itemIndex]. */
    fun getLineIndexOfItem(itemIndex: Int): Int {
        if (totalSize <= 0) {
            return 0
        }
        requirePrecondition(itemIndex < totalSize) { "ItemIndex > total count" }
        if (!gridContent.hasCustomSpans) {
            return itemIndex / slotsPerLine
        }

        verifyAndReadObservedStates()
        needsMeasureObservation = true
        pendingItemIndex = itemIndex
        return Snapshot.observe(
            readObserver = spanReadObserver,
            block = computeLineIndexOfItemBlock,
        )
    }

    private fun computeLineIndexOfItem(itemIndex: Int): Int {
        val lowerBoundBucket =
            buckets
                .binarySearch { it.firstItemIndex - itemIndex }
                .let { if (it >= 0) it else -it - 2 }
        var currentLine = lowerBoundBucket * bucketSize
        var currentItemIndex = buckets[lowerBoundBucket].firstItemIndex

        requirePrecondition(currentItemIndex <= itemIndex) { "currentItemIndex > itemIndex" }
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
                if (currentLineBucket >= buckets.size) {
                    buckets.add(Bucket(currentItemIndex - if (spansUsed > 0) 1 else 0))
                }
            }
        }
        if (spansUsed + spanOf(itemIndex, slotsPerLine - spansUsed) > slotsPerLine) {
            ++currentLine
        }

        return currentLine
    }

    fun spanOf(itemIndex: Int, maxSpan: Int): Int =
        with(LazyGridItemSpanScopeImpl) {
            maxCurrentLineSpan = maxSpan
            maxLineSpan = slotsPerLine

            val interval = gridContent.intervals[itemIndex]
            val localIntervalIndex = itemIndex - interval.startIndex
            val span = interval.value.span.invoke(this, localIntervalIndex)
            return span.currentLineSpan
        }

    private fun readStateValue(state: Any): Any? =
        when (state) {
            is State<*> -> state.value
            is SnapshotStateList<*> -> state.toList()
            is SnapshotStateMap<*, *> -> state.toMap()
            is SnapshotStateSet<*> -> state.toSet()
            else -> state
        }

    /**
     * Reads all accumulated [observedStates] via their public getters (so an enclosing measure
     * `SnapshotStateObserver` retains subscriptions to states from cached buckets and
     * [getLineIndexOfItem]) and invalidates the bucket cache if any observed state value changed.
     */
    private fun verifyAndReadObservedStates() {
        if (observedStates.isEmpty()) return
        var invalidated = false
        // Read every entry without short-circuiting so that when called inside an active
        // SnapshotStateObserver scope, all accumulated states are recorded by the observer.
        observedStates.forEach { state, cachedValue ->
            if (readStateValue(state) != cachedValue) {
                invalidated = true
            }
        }
        if (invalidated) {
            invalidateCache()
        }
    }

    /** Clears all cached line bookmarks, calculated intervals, and tracked states. */
    private fun invalidateCache() {
        buckets.clear()
        buckets.add(Bucket(0))
        lastLineIndex = 0
        lastLineStartItemIndex = 0
        lastLineStartKnownSpan = 0
        cachedBucketIndex = -1
        cachedBucket.clear()
        observedStates.clear()
        needsMeasureObservation = false
    }

    private class Bucket(
        /** Index of the first item in the bucket */
        val firstItemIndex: Int,
        /** Known span of the first item. Not zero only if this item caused "line break". */
        val firstItemKnownSpan: Int = 0,
    )

    private object LazyGridItemSpanScopeImpl : LazyGridItemSpanScope {
        override var maxCurrentLineSpan = 0
        override var maxLineSpan = 0
    }
}
