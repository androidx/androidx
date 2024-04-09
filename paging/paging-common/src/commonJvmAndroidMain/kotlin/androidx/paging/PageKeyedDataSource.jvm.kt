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

import androidx.arch.core.util.Function
import androidx.paging.DataSource.KeyType.PAGE_KEYED
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Incremental data loader for page-keyed content, where requests return keys for next/previous
 * pages.
 *
 * Implement a [DataSource] using [PageKeyedDataSource] if you need to use data from page `N - 1` to
 * load page `N`. This is common, for example, in network APIs that include a next/previous link or
 * key with each page load.
 *
 * The `InMemoryByPageRepository` in the
 * [PagingWithNetworkSample](https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md)
 * shows how to implement a network PageKeyedDataSource using
 * [Retrofit](https://square.github.io/retrofit/), while
 * handling swipe-to-refresh, network errors, and retry.
 *
 * @param Key Type of data used to query Value types out of the [DataSource].
 * @param Value Type of items being loaded by the [DataSource].
 */
@Deprecated(
    message = "PageKeyedDataSource is deprecated and has been replaced by PagingSource",
    replaceWith = ReplaceWith(
        "PagingSource<Key, Value>",
        "androidx.paging.PagingSource"
    )
)
public abstract class PageKeyedDataSource<Key : Any, Value : Any> : DataSource<Key, Value>(
    PAGE_KEYED
) {

    /**
     * Holder object for inputs to [loadInitial].
     *
     * @param Key Type of data used to query pages.
     * @param requestedLoadSize Requested number of items to load.
     *
     * Note that this may be larger than available data.
     * @param placeholdersEnabled Defines whether placeholders are enabled, and whether the
     * loaded total count will be ignored.
     */
    public open class LoadInitialParams<Key : Any>(
        @JvmField public val requestedLoadSize: Int,
        @JvmField public val placeholdersEnabled: Boolean
    )

    /**
     * Holder object for inputs to [loadBefore] and [loadAfter].
     *
     * @param Key Type of data used to query pages.
     * @param key Load items before/after this key.
     *
     * Returned data must begin directly adjacent to this position.
     * @param requestedLoadSize Requested number of items to load.
     *
     * Returned page can be of this size, but it may be altered if that is easier, e.g. a network
     * data source where the backend defines page size.
     */
    public open class LoadParams<Key : Any>(
        @JvmField public val key: Key,
        @JvmField public val requestedLoadSize: Int
    )

    /**
     * Callback for [loadInitial] to return data and, optionally, position/count information.
     *
     * A callback can be called only once, and will throw if called again.
     *
     * If you can compute the number of items in the data set before and after the loaded range,
     * call the five parameter [onResult] to pass that information. You can skip passing this
     * information by calling the three parameter [onResult], either if it's difficult to compute,
     * or if [LoadInitialParams.placeholdersEnabled] is `false`, so the positioning information will
     * be ignored.
     *
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param Key Type of data used to query pages.
     * @param Value Type of items being loaded.
     */
    public abstract class LoadInitialCallback<Key, Value> {
        /**
         * Called to pass initial load state from a DataSource.
         *
         * Call this method from your DataSource's `loadInitial` function to return data,
         * and inform how many placeholders should be shown before and after. If counting is cheap
         * to compute (for example, if a network load returns the information regardless), it's
         * recommended to pass data back through this method.
         *
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the [DataSource]. If this is empty, the
         * [DataSource] is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are `N`
         * items before the items in data that can be loaded from this DataSource, pass `N`.
         * @param totalCount Total number of items that may be returned from this DataSource.
         * Includes the number in the initial `data` parameter as well as any items that can be
         * loaded in front or behind of `data`.
         */
        public abstract fun onResult(
            data: List<Value>,
            position: Int,
            totalCount: Int,
            previousPageKey: Key?,
            nextPageKey: Key?
        )

        /**
         * Called to pass loaded data from a DataSource.
         *
         * Call this from [loadInitial] to initialize without counting available data, or supporting
         * placeholders.
         *
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the [PageKeyedDataSource].
         * @param previousPageKey Key for page before the initial load result, or `null` if no more
         * data can be loaded before.
         * @param nextPageKey Key for page after the initial load result, or `null` if no more data
         * can be loaded after.
         */
        public abstract fun onResult(data: List<Value>, previousPageKey: Key?, nextPageKey: Key?)
    }

    /**
     * Callback for [loadBefore] and [loadAfter] to return data.
     *
     * A callback can be called only once, and will throw if called again.
     *
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param Key Type of data used to query pages.
     * @param Value Type of items being loaded.
     */
    public abstract class LoadCallback<Key, Value> {
        /**
         * Called to pass loaded data from a [DataSource].
         *
         * Call this method from your PageKeyedDataSource's [loadBefore] and [loadAfter] methods to
         * return data.
         *
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * Pass the key for the subsequent page to load to adjacentPageKey. For example, if you've
         * loaded a page in [loadBefore], pass the key for the previous page, or `null` if the
         * loaded page is the first. If in [loadAfter], pass the key for the next page, or `null`
         * if the loaded page is the last.
         *
         * @param data List of items loaded from the PageKeyedDataSource.
         * @param adjacentPageKey Key for subsequent page load (previous page in [loadBefore] / next
         * page in [loadAfter]), or `null` if there are no more pages to load in the current load
         * direction.
         */
        public abstract fun onResult(data: List<Value>, adjacentPageKey: Key?)
    }

    /**
     * @throws [IllegalArgumentException] when passed an unsupported load type.
     */
    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override suspend fun load(params: Params<Key>): BaseResult<Value> = when {
        params.type == LoadType.REFRESH -> loadInitial(
            LoadInitialParams(
                params.initialLoadSize,
                params.placeholdersEnabled
            )
        )
        params.key == null -> BaseResult.empty()
        params.type == LoadType.PREPEND -> loadBefore(LoadParams(params.key, params.pageSize))
        params.type == LoadType.APPEND -> loadAfter(LoadParams(params.key, params.pageSize))
        else -> throw IllegalArgumentException("Unsupported type " + params.type.toString())
    }

    private suspend fun loadInitial(params: LoadInitialParams<Key>) =
        suspendCancellableCoroutine<BaseResult<Value>> { cont ->
            loadInitial(
                params,
                object : LoadInitialCallback<Key, Value>() {
                    override fun onResult(
                        data: List<Value>,
                        position: Int,
                        totalCount: Int,
                        previousPageKey: Key?,
                        nextPageKey: Key?
                    ) {
                        cont.resume(
                            BaseResult(
                                data = data,
                                prevKey = previousPageKey,
                                nextKey = nextPageKey,
                                itemsBefore = position,
                                itemsAfter = totalCount - data.size - position
                            )
                        )
                    }

                    override fun onResult(
                        data: List<Value>,
                        previousPageKey: Key?,
                        nextPageKey: Key?
                    ) {
                        cont.resume(BaseResult(data, previousPageKey, nextPageKey))
                    }
                }
            )
        }

    private suspend fun loadBefore(params: LoadParams<Key>) =
        suspendCancellableCoroutine<BaseResult<Value>> { cont ->
            loadBefore(params, continuationAsCallback(cont, false))
        }

    private suspend fun loadAfter(params: LoadParams<Key>) =
        suspendCancellableCoroutine<BaseResult<Value>> { cont ->
            loadAfter(params, continuationAsCallback(cont, true))
        }

    // Possible workaround for b/161464680; issue was reported when built with Kotlin 1.3.71
    @Suppress("RemoveRedundantQualifierName")
    private fun continuationAsCallback(
        continuation: CancellableContinuation<BaseResult<Value>>,
        isAppend: Boolean
    ): LoadCallback<Key, Value> {
        return object : LoadCallback<Key, Value>() {
            override fun onResult(data: List<Value>, adjacentPageKey: Key?) {
                continuation.resume(
                    BaseResult(
                        data = data,
                        prevKey = if (isAppend) null else adjacentPageKey,
                        nextKey = if (isAppend) adjacentPageKey else null
                    )
                )
            }
        }
    }

    /**
     * Load initial data.
     *
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the loaded data to
     * the callback via the three-parameter [LoadInitialCallback.onResult]. This enables PagedLists
     * presenting data from this source to display placeholders to represent unloaded items.
     *
     * [LoadInitialParams.requestedLoadSize] is a hint, not a requirement, so it may be may be
     * altered or ignored.
     *
     * @param params Parameters for initial load, including requested load size.
     * @param callback Callback that receives initial load data.
     */
    public abstract fun loadInitial(
        params: LoadInitialParams<Key>,
        callback: LoadInitialCallback<Key, Value>
    )

    /**
     * Prepend page with the key specified by [LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * Data may be passed synchronously during the load method, or deferred and called at a later
     * time. Further loads going down will be blocked until the callback is called.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     * size.
     * @param callback Callback that receives loaded data.
     */
    public abstract fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Key, Value>)

    /**
     * Append page with the key specified by [LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * Data may be passed synchronously during the load method, or deferred and called at a later
     * time. Further loads going down will be blocked until the callback is called.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     * size.
     * @param callback Callback that receives loaded data.
     */
    public abstract fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Key, Value>)

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override fun getKeyInternal(item: Value): Key =
        throw IllegalStateException("Cannot get key by item in pageKeyedDataSource")

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override val supportsPageDropping = false

    @Suppress("DEPRECATION")
    final override fun <ToValue : Any> mapByPage(
        function: Function<List<Value>, List<ToValue>>
    ): PageKeyedDataSource<Key, ToValue> = WrapperPageKeyedDataSource(this, function)

    @Suppress("DEPRECATION")
    final override fun <ToValue : Any> mapByPage(
        function: (List<Value>) -> List<ToValue>
    ): PageKeyedDataSource<Key, ToValue> = mapByPage(Function { function(it) })

    @Suppress("DEPRECATION")
    final override fun <ToValue : Any> map(
        function: Function<Value, ToValue>
    ): PageKeyedDataSource<Key, ToValue> =
        mapByPage(Function { list -> list.map { function.apply(it) } })

    @Suppress("DEPRECATION")
    final override fun <ToValue : Any> map(
        function: (Value) -> ToValue
    ): PageKeyedDataSource<Key, ToValue> = mapByPage(Function { list -> list.map(function) })
}
