/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.paging.LoadType.REFRESH
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** @suppress */
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <Key : Any> PagedList.Config.toRefreshLoadParams(
    key: Key?
): PagingSource.LoadParams<Key> = PagingSource.LoadParams.Refresh(
    key,
    initialLoadSizeHint,
    enablePlaceholders,
)

/**
 * Base class for an abstraction of pageable static data from some source, where loading pages
 * of data is typically an expensive operation. Some examples of common [PagingSource]s might be
 * from network or from a database.
 *
 * An instance of a [PagingSource] is used to load pages of data for an instance of [PagingData].
 *
 * A [PagingData] can grow as it loads more data, but the data loaded cannot be updated. If the
 * underlying data set is modified, a new [PagingSource] / [PagingData] pair must be created to
 * represent an updated snapshot of the data.
 *
 * ### Loading Pages
 *
 * [PagingData] queries data from its [PagingSource] in response to loading hints generated as
 * the user scrolls in a `RecyclerView`.
 *
 * To control how and when a [PagingData] queries data from its [PagingSource], see [PagingConfig],
 * which defines behavior such as [PagingConfig.pageSize] and [PagingConfig.prefetchDistance].
 *
 * ### Updating Data
 *
 * A [PagingSource] / [PagingData] pair is a snapshot of the data set. A new [PagingData] /
 * [PagingData] must be created if an update occurs, such as a reorder, insert, delete, or content
 * update occurs. A [PagingSource] must detect that it cannot continue loading its snapshot
 * (for instance, when Database query notices a table being invalidated), and call [invalidate].
 * Then a new [PagingSource] / [PagingData] pair would be created to represent data from the new
 * state of the database query.
 *
 * ### Presenting Data to UI
 *
 * To present data loaded by a [PagingSource] to a `RecyclerView`, create an instance of [Pager],
 * which provides a stream of [PagingData] that you may collect from and submit to a
 * [PagingDataAdapter][androidx.paging.PagingDataAdapter].
 *
 * @param Key Type of key which define what data to load. E.g. [Int] to represent either a page
 * number or item position, or [String] if your network uses Strings as next tokens returned with
 * each response.
 * @param Value Type of data loaded in by this [PagingSource]. E.g., the type of data that will be
 * passed to a [PagingDataAdapter][androidx.paging.PagingDataAdapter] to be displayed in a
 * `RecyclerView`.
 *
 * @sample androidx.paging.samples.pageKeyedPagingSourceSample
 * @sample androidx.paging.samples.itemKeyedPagingSourceSample
 *
 * @see Pager
 */
public abstract class PagingSource<Key : Any, Value : Any> {

    /**
     * Params for a load request on a [PagingSource] from [PagingSource.load].
     */
    public sealed class LoadParams<Key : Any> constructor(
        /**
         * Requested number of items to load.
         *
         * Note: It is valid for [PagingSource.load] to return a [LoadResult] that has a different
         * number of items than the requested load size.
         */
        public val loadSize: Int,
        /**
         * From [PagingConfig.enablePlaceholders], true if placeholders are enabled and the load
         * request for this [LoadParams] should populate [LoadResult.Page.itemsBefore] and
         * [LoadResult.Page.itemsAfter] if possible.
         */
        public val placeholdersEnabled: Boolean,
    ) {
        /**
         * Key for the page to be loaded.
         *
         * [key] can be `null` only if this [LoadParams] is [Refresh], and either no `initialKey`
         * is provided to the [Pager] or [PagingSource.getRefreshKey] from the previous
         * [PagingSource] returns `null`.
         *
         * The value of [key] is dependent on the type of [LoadParams]:
         *  * [Refresh]
         *      * On initial load, the nullable `initialKey` passed to the [Pager].
         *      * On subsequent loads due to invalidation or refresh, the result of
         *      [PagingSource.getRefreshKey].
         *  * [Prepend] - [LoadResult.Page.prevKey] of the loaded page at the front of the list.
         *  * [Append] - [LoadResult.Page.nextKey] of the loaded page at the end of the list.
         */
        public abstract val key: Key?

        /**
         * Params for an initial load request on a [PagingSource] from [PagingSource.load] or a
         * refresh triggered by [invalidate].
         */
        public class Refresh<Key : Any> constructor(
            override val key: Key?,
            loadSize: Int,
            placeholdersEnabled: Boolean,
        ) : LoadParams<Key>(
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled,
        )

        /**
         * Params to load a page of data from a [PagingSource] via [PagingSource.load] to be
         * appended to the end of the list.
         */
        public class Append<Key : Any> constructor(
            override val key: Key,
            loadSize: Int,
            placeholdersEnabled: Boolean,
        ) : LoadParams<Key>(
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled,
        )

        /**
         * Params to load a page of data from a [PagingSource] via [PagingSource.load] to be
         * prepended to the start of the list.
         */
        public class Prepend<Key : Any> constructor(
            override val key: Key,
            loadSize: Int,
            placeholdersEnabled: Boolean,
        ) : LoadParams<Key>(
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled,
        )

        internal companion object {
            fun <Key : Any> create(
                loadType: LoadType,
                key: Key?,
                loadSize: Int,
                placeholdersEnabled: Boolean,
            ): LoadParams<Key> = when (loadType) {
                LoadType.REFRESH -> Refresh(
                    key = key,
                    loadSize = loadSize,
                    placeholdersEnabled = placeholdersEnabled,
                )
                LoadType.PREPEND -> Prepend(
                    loadSize = loadSize,
                    key = requireNotNull(key) {
                        "key cannot be null for prepend"
                    },
                    placeholdersEnabled = placeholdersEnabled,
                )
                LoadType.APPEND -> Append(
                    loadSize = loadSize,
                    key = requireNotNull(key) {
                        "key cannot be null for append"
                    },
                    placeholdersEnabled = placeholdersEnabled,
                )
            }
        }
    }

    /**
     * Result of a load request from [PagingSource.load].
     */
    public sealed class LoadResult<Key : Any, Value : Any> {
        /**
         * Error result object for [PagingSource.load].
         *
         * This return type indicates an expected, recoverable error (such as a network load
         * failure). This failure will be forwarded to the UI as a [LoadState.Error], and may be
         * retried.
         *
         * @sample androidx.paging.samples.pageKeyedPagingSourceSample
         */
        public data class Error<Key : Any, Value : Any>(
            val throwable: Throwable
        ) : LoadResult<Key, Value>()

        /**
         * Invalid result object for [PagingSource.load]
         *
         * This return type can be used to terminate future load requests on this [PagingSource]
         * when the [PagingSource] is not longer valid due to changes in the underlying dataset.
         *
         * For example, if the underlying database gets written into but the [PagingSource] does
         * not invalidate in time, it may return inconsistent results if its implementation depends
         * on the immutability of the backing dataset it loads from (e.g., LIMIT OFFSET style db
         * implementations). In this scenario, it is recommended to check for invalidation after
         * loading and to return LoadResult.Invalid, which causes Paging to discard any
         * pending or future load requests to this PagingSource and invalidate it.
         *
         * Returning [Invalid] will trigger Paging to [invalidate] this [PagingSource] and
         * terminate any future attempts to [load] from this [PagingSource]
         */
        public class Invalid<Key : Any, Value : Any> : LoadResult<Key, Value>()

        /**
         * Success result object for [PagingSource.load].
         *
         * @sample androidx.paging.samples.pageKeyedPage
         * @sample androidx.paging.samples.pageIndexedPage
         */
        public data class Page<Key : Any, Value : Any> constructor(
            /**
             * Loaded data
             */
            val data: List<Value>,
            /**
             * [Key] for previous page if more data can be loaded in that direction, `null`
             * otherwise.
             */
            val prevKey: Key?,
            /**
             * [Key] for next page if more data can be loaded in that direction, `null` otherwise.
             */
            val nextKey: Key?,
            /**
             * Optional count of items before the loaded data.
             */
            @IntRange(from = COUNT_UNDEFINED.toLong())
            val itemsBefore: Int = COUNT_UNDEFINED,
            /**
             * Optional count of items after the loaded data.
             */
            @IntRange(from = COUNT_UNDEFINED.toLong())
            val itemsAfter: Int = COUNT_UNDEFINED
        ) : LoadResult<Key, Value>() {

            /**
             * Success result object for [PagingSource.load].
             *
             * @param data Loaded data
             * @param prevKey [Key] for previous page if more data can be loaded in that direction,
             * `null` otherwise.
             * @param nextKey [Key] for next page if more data can be loaded in that direction,
             * `null` otherwise.
             */
            public constructor(
                data: List<Value>,
                prevKey: Key?,
                nextKey: Key?
            ) : this(data, prevKey, nextKey, COUNT_UNDEFINED, COUNT_UNDEFINED)

            init {
                require(itemsBefore == COUNT_UNDEFINED || itemsBefore >= 0) {
                    "itemsBefore cannot be negative"
                }

                require(itemsAfter == COUNT_UNDEFINED || itemsAfter >= 0) {
                    "itemsAfter cannot be negative"
                }
            }

            public companion object {
                public const val COUNT_UNDEFINED: Int = Int.MIN_VALUE

                @Suppress("MemberVisibilityCanBePrivate") // Prevent synthetic accessor generation.
                internal val EMPTY = Page(emptyList(), null, null, 0, 0)

                @Suppress("UNCHECKED_CAST") // Can safely ignore, since the list is empty.
                internal fun <Key : Any, Value : Any> empty() = EMPTY as Page<Key, Value>
            }
        }
    }

    /**
     * `true` if this [PagingSource] supports jumping, `false` otherwise.
     *
     * Override this to `true` if pseudo-fast scrolling via jumps is supported.
     *
     * A jump occurs when a `RecyclerView` scrolls through a number of placeholders defined by
     * [PagingConfig.jumpThreshold] and triggers a load with [LoadType] [REFRESH].
     *
     * [PagingSource]s that support jumps should override [getRefreshKey] to return a [Key] that
     * would load data fulfilling the viewport given a user's current [PagingState.anchorPosition].
     *
     * @see [PagingConfig.jumpThreshold]
     */
    public open val jumpingSupported: Boolean
        get() = false

    /**
     * `true` if this [PagingSource] expects to re-use keys to load distinct pages
     * without a call to [invalidate], `false` otherwise.
     */
    public open val keyReuseSupported: Boolean
        get() = false

    @VisibleForTesting
    internal val onInvalidatedCallbacks = CopyOnWriteArrayList<() -> Unit>()

    private val _invalid = AtomicBoolean(false)

    /**
     * Whether this [PagingSource] has been invalidated, which should happen when the data this
     * [PagingSource] represents changes since it was first instantiated.
     */
    public val invalid: Boolean
        get() = _invalid.get()

    /**
     * Signal the [PagingSource] to stop loading.
     *
     * This method is idempotent. i.e., If [invalidate] has already been called, subsequent calls to
     * this method should have no effect.
     */
    public fun invalidate() {
        if (_invalid.compareAndSet(false, true)) {
            onInvalidatedCallbacks.forEach { it.invoke() }
        }
    }

    /**
     * Add a callback to invoke when the [PagingSource] is first invalidated.
     *
     * Once invalidated, a [PagingSource] will not become valid again.
     *
     * A [PagingSource] will only invoke its callbacks once - the first time [invalidate] is called,
     * on that thread.
     *
     * @param onInvalidatedCallback The callback that will be invoked on thread that invalidates the
     * [PagingSource].
     */
    public fun registerInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        onInvalidatedCallbacks.add(onInvalidatedCallback)
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    public fun unregisterInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        onInvalidatedCallbacks.remove(onInvalidatedCallback)
    }

    /**
     * Loading API for [PagingSource].
     *
     * Implement this method to trigger your async load (e.g. from database or network).
     */
    public abstract suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value>

    /**
     * Provide a [Key] used for the initial [load] for the next [PagingSource] due to invalidation
     * of this [PagingSource]. The [Key] is provided to [load] via [LoadParams.key].
     *
     * The [Key] returned by this method should cause [load] to load enough items to
     * fill the viewport around the last accessed position, allowing the next generation to
     * transparently animate in. The last accessed position can be retrieved via
     * [state.anchorPosition][PagingState.anchorPosition], which is typically
     * the top-most or bottom-most item in the viewport due to access being triggered by binding
     * items as they scroll into view.
     *
     * For example, if items are loaded based on integer position keys, you can return
     * [state.anchorPosition][PagingState.anchorPosition].
     *
     * Alternately, if items contain a key used to load, get the key from the item in the page at
     * index [state.anchorPosition][PagingState.anchorPosition].
     *
     * @param state [PagingState] of the currently fetched data, which includes the most recently
     * accessed position in the list via [PagingState.anchorPosition].
     *
     * @return [Key] passed to [load] after invalidation used for initial load of the next
     * generation. The [Key] returned by [getRefreshKey] should load pages centered around
     * user's current viewport. If the correct [Key] cannot be determined, `null` can be returned
     * to allow [load] decide what default key to use.
     */
    public abstract fun getRefreshKey(state: PagingState<Key, Value>): Key?
}
