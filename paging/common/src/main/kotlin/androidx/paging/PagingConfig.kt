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

/**
 * Configures how to load content from a [PagedSource].
 *
 * Use [PagingConfig.Builder] to construct and define custom loading behavior, such as
 * [pageSize], which defines number of items loaded at a time.
 */
class PagingConfig(
    /**
     * Defines the number of items loaded at once from the [PagedSource].
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
     */
    @JvmField
    val pageSize: Int,
    /**
     * Prefetch distance which defines how far ahead to load.
     *
     * If this value is set to 50, the paged list will attempt to load 50 items in advance of
     * data that's already been accessed.
     *
     * @see PagedList.loadAround
     */
    @JvmField
    val prefetchDistance: Int = pageSize,
    /**
     * Defines whether the [PagedList] may display null placeholders, if the [PagedSource]
     * provides them.
     */
    @JvmField
    val enablePlaceholders: Boolean = true,

    /**
     * Size requested size for initial load of from [PagedSource], generally larger than a regular
     * page.
     */
    @JvmField
    val initialLoadSize: Int = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER,
    /**
     * Defines the maximum number of items that may be loaded into this pagedList before pages
     * should be dropped.
     *
     * If set to [MAX_SIZE_UNBOUNDED], pages will never be dropped.
     *
     * @see MAX_SIZE_UNBOUNDED
     * @see Builder.setMaxSize
     */
    @JvmField
    val maxSize: Int = MAX_SIZE_UNBOUNDED
) {
    init {
        if (!enablePlaceholders && prefetchDistance == 0) {
            throw IllegalArgumentException(
                "Placeholders and prefetch are the only ways" +
                        " to trigger loading of more data in the PagedList, so either" +
                        " placeholders must be enabled, or prefetch distance must be > 0."
            )
        }
        if (maxSize != MAX_SIZE_UNBOUNDED && maxSize < pageSize + prefetchDistance * 2) {
            throw IllegalArgumentException(
                "Maximum size must be at least pageSize + 2*prefetchDist" +
                        ", pageSize=$pageSize, prefetchDist=$prefetchDistance" +
                        ", maxSize=$maxSize"
            )
        }
    }
    companion object {
        /**
         * When [maxSize] is set to [MAX_SIZE_UNBOUNDED], the maximum number of items loaded is
         * unbounded, and pages will never be dropped.
         */
        const val MAX_SIZE_UNBOUNDED = Int.MAX_VALUE
        internal const val DEFAULT_INITIAL_PAGE_MULTIPLIER = 3
    }

    /**
     * Builder class for [PagingConfig] for Java callers.
     *
     * Kotlin callers should use the Config constructor directly.
     */
    class Builder(
        private val pageSize: Int
    ) {
        private var prefetchDistance = -1
        private var initialLoadSizeHint = -1
        private var enablePlaceholders = true
        private var maxSize = MAX_SIZE_UNBOUNDED

        /**
         * Defines how far from the edge of loaded content an access must be to trigger further
         * loading.
         *
         * Should be several times the number of visible items onscreen.
         *
         * If not set, defaults to page size.
         *
         * A value of 0 indicates that no list items will be loaded until they are specifically
         * requested. This is generally not recommended, so that users don't observe a
         * placeholder item (with placeholders) or end of list (without) while scrolling.
         *
         * @param prefetchDistance Distance the [PagedList] should prefetch.
         * @return this
         */
        fun setPrefetchDistance(@IntRange(from = 0) prefetchDistance: Int) = apply {
            this.prefetchDistance = prefetchDistance
        }

        /**
         * Pass false to disable null placeholders in [PagedList]s using this [PagingConfig].
         *
         * If not set, defaults to true.
         *
         * A [PagedList] will present null placeholders for not-yet-loaded content if two
         * conditions are met:
         *
         * 1) Its [PagedSource] can count all unloaded items (so that the number of nulls to
         * present is known).
         *
         * 2) placeholders are not disabled on the [PagingConfig].
         *
         * Call `setEnablePlaceholders(false)` to ensure the receiver of the PagedList
         * (often a [androidx.paging.PagedDataAdapter]) doesn't need to account for null items.
         *
         * If placeholders are disabled, not-yet-loaded content will not be present in the list.
         * Paging will still occur, but as items are loaded or removed, they will be signaled
         * as inserts to the [PagedList.Callback].
         *
         * [PagedList.Callback.onChanged] will not be issued as part of loading, though a
         * [androidx.paging.PagedDataAdapter] may still receive change events as a result of
         * [PagedList] diffing.
         *
         * @param enablePlaceholders `false` if null placeholders should be disabled.
         * @return this
         */
        fun setEnablePlaceholders(enablePlaceholders: Boolean) = apply {
            this.enablePlaceholders = enablePlaceholders
        }

        /**
         * Defines how many items to load when first load occurs.
         *
         * This value is typically larger than page size, so on first load data there's a large
         * enough range of content loaded to cover small scrolls.
         *
         * If not set, defaults to three times page size.
         *
         * @param initialLoadSizeHint Number of items to load while initializing the [PagedList]
         * @return this
         */
        fun setInitialLoadSizeHint(@IntRange(from = 1) initialLoadSizeHint: Int) = apply {
            this.initialLoadSizeHint = initialLoadSizeHint
        }

        /**
         * Defines how many items to keep loaded at once.
         *
         * This can be used to cap the number of items kept in memory by dropping pages. This
         * value is typically many pages so old pages are cached in case the user scrolls back.
         *
         * This value must be at least two times the [prefetchDistance] plus the [pageSize]).
         * This constraint prevent loads from being continuously fetched and discarded due to
         * prefetching.
         *
         * The max size specified here best effort, not a guarantee. In practice, if [maxSize]
         * is many times the page size, the number of items held by the [PagedList] will not
         * grow above this number. Exceptions are made as necessary to guarantee:
         *  * Pages are never dropped until there are more than two pages loaded. Note that
         * a [PagedSource] may not be held strictly to [requested pageSize][PagingConfig.pageSize], so
         * two pages may be larger than expected.
         *  * Pages are never dropped if they are within a prefetch window (defined to be
         * `pageSize + (2 * prefetchDistance)`) of the most recent load.
         *
         * If not set, defaults to [MAX_SIZE_UNBOUNDED], which disables page dropping.
         *
         * @param maxSize Maximum number of items to keep in memory, or [MAX_SIZE_UNBOUNDED] to
         * disable page dropping.
         * @return this
         *
         * @see PagingConfig.MAX_SIZE_UNBOUNDED
         * @see PagingConfig.maxSize
         */
        fun setMaxSize(@IntRange(from = 2) maxSize: Int) = apply {
            this.maxSize = maxSize
        }

        /**
         * Creates a [PagingConfig] with the given parameters.
         *
         * @return A new [PagingConfig].
         */
        fun build(): PagingConfig {
            if (prefetchDistance < 0) {
                prefetchDistance = pageSize
            }
            if (initialLoadSizeHint < 0) {
                initialLoadSizeHint = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER
            }

            return PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholders,
                initialLoadSizeHint,
                maxSize
            )
        }
    }
}