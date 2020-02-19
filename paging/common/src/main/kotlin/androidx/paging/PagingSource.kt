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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <Key : Any> PagedList.Config.toRefreshLoadParams(key: Key?): PagingSource.LoadParams<Key> =
    PagingSource.LoadParams(
        LoadType.REFRESH,
        key,
        initialLoadSizeHint,
        enablePlaceholders,
        pageSize
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
 * <h4>Loading Pages</h4>
 *
 * [PagingData] queries data from its [PagingSource] in response to loading hints generated as
 * the user scrolls in a `RecyclerView`.
 *
 * To control how and when a [PagingData] queries data from its [PagingSource], see [PagingConfig],
 * which defines behavior such as [PagingConfig.pageSize] and [PagingConfig.prefetchDistance].
 *
 * <h4>Updating Data</h4>
 *
 * A [PagingSource] / [PagingData] pair is a snapshot of the data set. A new [PagingData] /
 * [PagingData] must be created if an update occurs, such as a reorder, insert, delete, or content
 * update occurs. A [PagingSource] must detect that it cannot continue loading its snapshot
 * (for instance, when Database query notices a table being invalidated), and call [invalidate].
 * Then a new [PagingSource] / [PagingData] pair would be created to represent data from the new
 * state of the database query.
 *
 * @param Key Type for unique identifier for items loaded from [PagingSource]. E.g., [Int] to
 * represent an item's position in a [PagingSource] that is keyed by item position. Note that this
 * is distinct from e.g. Room's `<Value>` type loaded by the [PagingSource].
 * @param Value Type of data loaded in by this [PagingSource]. E.g., the type of data that will be
 * passed to a [androidx.paging.PagingDataAdapter] to be displayed in a `RecyclerView`.
 */
@Suppress("KDocUnresolvedReference")
abstract class PagingSource<Key : Any, Value : Any> {

    /**
     * Params for a load request on a [PagingSource] from [PagingSource.load].
     */
    data class LoadParams<Key : Any>(
        /**
         * [LoadType] of this load request.
         *
         * May be one of the following values:
         * * [LoadType.REFRESH] - initial load or a refresh triggered by [invalidate].
         * * [LoadType.START] - load a page of data to be prepended to the start of the list.
         * * [LoadType.END] - load a page of data to be appended to the end of the list.
         */
        val loadType: LoadType,
        /**
         * Key for the page to be loaded.
         */
        val key: Key?,
        /**
         * Requested number of items to load.
         *
         * Note: It is valid for [PagingSource.load] to return a [LoadResult] that has a different
         * number of items than the requested load size.
         */
        val loadSize: Int,
        /**
         * From [PagingConfig.enablePlaceholders], true if placeholders are enabled and the load
         * request for this [LoadParams] should populate [LoadResult.Page.itemsBefore] and
         * [LoadResult.Page.itemsAfter] if possible.
         */
        val placeholdersEnabled: Boolean,
        /**
         * From [PagingConfig.pageSize], the configured page size.
         */
        val pageSize: Int
    )

    /**
     * Result of a load request from [PagingSource.load].
     */
    sealed class LoadResult<Key : Any, Value : Any> {
        /**
         * Recoverable error used to update [LoadState] in a [PagingData] that may be retried via
         * [PagingDataDiffer.retry].
         */
        data class Error<Key : Any, Value : Any>(
            val throwable: Throwable
        ) : LoadResult<Key, Value>()

        /**
         * Success result object for [PagingSource.load].
         */
        data class Page<Key : Any, Value : Any>(
            /**
             * Loaded data
             */
            val data: List<Value>,
            /**
             * Key for previous page if more data can be loaded in that direction, null otherwise.
             */
            val prevKey: Key?,
            /**
             * Key for next page if more data can be loaded in that direction, null otherwise.
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
            init {
                require(itemsBefore == COUNT_UNDEFINED || itemsBefore >= 0) {
                    "itemsBefore cannot be negative"
                }

                require(itemsAfter == COUNT_UNDEFINED || itemsAfter >= 0) {
                    "itemsAfter cannot be negative"
                }
            }

            companion object {
                const val COUNT_UNDEFINED = Int.MIN_VALUE

                @Suppress("MemberVisibilityCanBePrivate") // Prevent synthetic accessor generation.
                private val EMPTY = Page(emptyList(), null, null, 0, 0)

                @Suppress("UNCHECKED_CAST") // Can safely ignore, since the list is empty.
                internal fun <Key : Any, Value : Any> empty() = EMPTY as Page<Key, Value>
            }
        }
    }

    /**
     * Request a refresh key given the current [PagingState] of the associated [PagingData] used to
     * present loaded data from this [PagingSource].
     *
     * The [Key] returned by this method is used to populate the [LoadParams.key] for load requests
     * of type [LoadType.REFRESH].
     *
     * For example, if items are loaded based on position, and keys are positions, [getRefreshKey]
     * should return the position of the item.
     *
     * Alternately, if items contain a key used to load, get the key from the item in the page at
     * index [PagingState.anchorPosition].
     *
     * If this operation cannot be supported (generally, because keys cannot be reused across
     * refresh) return `null` - this is the default behavior.
     *
     * Note: This method is guaranteed to only be called if the initial load succeeds and the
     * list of loaded pages is not empty. In the case where a refresh is triggered before the
     * initial load succeeds or it errors out, the initial key passed into the creation of the
     * [PagingData] stream (typically from [PagingDataFlow]) will be used.
     */
    open fun getRefreshKey(state: PagingState<Key, Value>): Key? = null

    private val onInvalidatedCallbacks = CopyOnWriteArrayList<() -> Unit>()

    private val _invalid = AtomicBoolean(false)
    /**
     * Whether this [PagingSource] has been invalidated, which should happen when the data this
     * [PagingSource] represents changes since it was first instantiated.
     */
    val invalid: Boolean
        get() = _invalid.get()

    /**
     * Signal the [PagingSource] to stop loading.
     *
     * This method is idempotent. i.e., If [invalidate] has already been called, subsequent calls to
     * this method should have no effect.
     */
    open fun invalidate() {
        // TODO(b/137971356): Investigate making this not open when able to remove
        //  LegacyPagingSource.
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
    fun registerInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        onInvalidatedCallbacks.add(onInvalidatedCallback)
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    fun unregisterInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        onInvalidatedCallbacks.remove(onInvalidatedCallback)
    }

    /**
     * Loading API for [PagingSource].
     *
     * Implement this method to trigger your async load (e.g. from database or network).
     */
    abstract suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value>
}
