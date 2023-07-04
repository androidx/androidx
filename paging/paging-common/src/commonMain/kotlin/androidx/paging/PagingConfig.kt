/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import androidx.annotation.IntRange
import androidx.paging.PagingConfig.Companion.MAX_SIZE_UNBOUNDED
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED

/**
 * An object used to configure loading behavior within a [Pager], as it loads content from a
 * [PagingSource].
 */
public class PagingConfig @JvmOverloads public constructor(
    /**
     * Defines the number of items loaded at once from the [PagingSource].
     *
     * Should be several times the number of visible items onscreen.
     *
     * Configuring your page size depends on how your data is being loaded and used. Smaller
     * page sizes improve memory usage, latency, and avoid GC churn. Larger pages generally
     * improve loading throughput, to a point (avoid loading more than 2MB from SQLite at
     * once, since it incurs extra cost).
     *
     * If you're loading data for very large, social-media style cards that take up most of
     * a screen, and your database isn't a bottleneck, 10-20 may make sense. If you're
     * displaying dozens of items in a tiled grid, which can present items during a scroll
     * much more quickly, consider closer to 100.
     *
     * Note: [pageSize] is used to inform [PagingSource.LoadParams.loadSize], but is not enforced.
     * A [PagingSource] may completely ignore this value and still return a valid
     * [Page][PagingSource.LoadResult.Page].
     */
    @JvmField
    public val pageSize: Int,

    /**
     * Prefetch distance which defines how far from the edge of loaded content an access must be to
     * trigger further loading. Typically should be set several times the number of visible items
     * onscreen.
     *
     * E.g., If this value is set to 50, a [PagingData] will attempt to load 50 items in advance of
     * data that's already been accessed.
     *
     * A value of 0 indicates that no list items will be loaded until they are specifically
     * requested. This is generally not recommended, so that users don't observe a
     * placeholder item (with placeholders) or end of list (without) while scrolling.
     */
    @JvmField
    @IntRange(from = 0)
    public val prefetchDistance: Int = pageSize,

    /**
     * Defines whether [PagingData] may display `null` placeholders, if the [PagingSource]
     * provides them.
     *
     * [PagingData] will present `null` placeholders for not-yet-loaded content if two
     * conditions are met:
     *
     * 1) Its [PagingSource] can count all unloaded items (so that the number of nulls to
     * present is known).
     *
     * 2) [enablePlaceholders] is set to `true`
     */
    @JvmField
    public val enablePlaceholders: Boolean = true,

    /**
     * Defines requested load size for initial load from [PagingSource], typically larger than
     * [pageSize], so on first load data there's a large enough range of content loaded to cover
     * small scrolls.
     *
     * Note: [initialLoadSize] is used to inform [PagingSource.LoadParams.loadSize], but is not
     * enforced. A [PagingSource] may completely ignore this value and still return a valid initial
     * [Page][PagingSource.LoadResult.Page].
     */
    @JvmField
    @IntRange(from = 1)
    public val initialLoadSize: Int = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER,
    /**
     * Defines the maximum number of items that may be loaded into [PagingData] before pages should
     * be dropped.
     *
     * If set to [MAX_SIZE_UNBOUNDED], pages will never be dropped.
     *
     * This can be used to cap the number of items kept in memory by dropping pages. This value is
     * typically many pages so old pages are cached in case the user scrolls back.
     *
     * This value must be at least two times the [prefetchDistance] plus the [pageSize]). This
     * constraint prevent loads from being continuously fetched and discarded due to prefetching.
     *
     * [maxSize] is best effort, not a guarantee. In practice, if [maxSize] is many times
     * [pageSize], the number of items held by [PagingData] will not grow above this number.
     * Exceptions are made as necessary to guarantee:
     *  * Pages are never dropped until there are more than two pages loaded. Note that
     * a [PagingSource] may not be held strictly to [requested pageSize][PagingConfig.pageSize], so
     * two pages may be larger than expected.
     *  * Pages are never dropped if they are within a prefetch window (defined to be
     * `pageSize + (2 * prefetchDistance)`) of the most recent load.
     *
     * @see PagingConfig.MAX_SIZE_UNBOUNDED
     */
    @JvmField
    @IntRange(from = 2)
    public val maxSize: Int = MAX_SIZE_UNBOUNDED,

    /**
     * Defines a threshold for the number of items scrolled outside the bounds of loaded items
     * before Paging should give up on loading pages incrementally, and instead jump to the
     * user's position by triggering REFRESH via invalidate.
     *
     * Defaults to [COUNT_UNDEFINED], which disables invalidation due to scrolling large distances.
     *
     * Note: In order to allow [PagingSource] to resume from the user's current scroll position
     * after invalidation, [PagingSource.getRefreshKey] must be implemented.
     *
     * @see PagingSource.getRefreshKey
     * @see PagingSource.jumpingSupported
     */
    @JvmField
    public val jumpThreshold: Int = COUNT_UNDEFINED
) {
    init {
        if (!enablePlaceholders && prefetchDistance == 0) {
            throw IllegalArgumentException(
                "Placeholders and prefetch are the only ways" +
                    " to trigger loading of more data in PagingData, so either placeholders" +
                    " must be enabled, or prefetch distance must be > 0."
            )
        }
        if (maxSize != MAX_SIZE_UNBOUNDED && maxSize < pageSize + prefetchDistance * 2) {
            throw IllegalArgumentException(
                "Maximum size must be at least pageSize + 2*prefetchDist" +
                    ", pageSize=$pageSize, prefetchDist=$prefetchDistance" +
                    ", maxSize=$maxSize"
            )
        }

        require(jumpThreshold == COUNT_UNDEFINED || jumpThreshold > 0) {
            "jumpThreshold must be positive to enable jumps or COUNT_UNDEFINED to disable jumping."
        }
    }

    public companion object {
        /**
         * When [maxSize] is set to [MAX_SIZE_UNBOUNDED], the maximum number of items loaded is
         * unbounded, and pages will never be dropped.
         */
        @Suppress("MinMaxConstant")
        public const val MAX_SIZE_UNBOUNDED: Int = Int.MAX_VALUE
        internal const val DEFAULT_INITIAL_PAGE_MULTIPLIER = 3
    }
}