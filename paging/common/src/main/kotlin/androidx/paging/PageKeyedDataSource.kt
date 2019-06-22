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
import androidx.concurrent.futures.ResolvableFuture
import androidx.paging.DataSource.KeyType.PAGE_KEYED
import com.google.common.util.concurrent.ListenableFuture

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
abstract class PageKeyedDataSource<Key : Any, Value : Any> : DataSource<Key, Value>(PAGE_KEYED) {

    /**
     * Holder object for inputs to [loadInitial].
     *
     * @param Key Type of data used to query pages.
     * @property requestedLoadSize Requested number of items to load.
     *
     * Note that this may be larger than available data.
     * @property placeholdersEnabled Defines whether placeholders are enabled, and whether the
     * loaded total count will be ignored.
     */
    open class LoadInitialParams<Key : Any>(
        @JvmField val requestedLoadSize: Int,
        @JvmField val placeholdersEnabled: Boolean
    )

    /**
     * Holder object for inputs to [loadBefore] and [loadAfter].
     *
     * @param Key Type of data used to query pages.
     * @property key Load items before/after this key.
     *
     * Returned data must begin directly adjacent to this position.
     * @property requestedLoadSize Requested number of items to load.
     *
     * Returned page can be of this size, but it may be altered if that is easier, e.g. a network
     * data source where the backend defines page size.
     */
    open class LoadParams<Key : Any>(@JvmField val key: Key, @JvmField val requestedLoadSize: Int)

    /**
     * Type produced by [loadInitial] to represent initially loaded data.
     *
     * @param Key Type of key used to identify pages.
     * @param Value Type of items being loaded by the [DataSource].
     */
    internal class InitialResult<Key : Any, Value : Any> : BaseResult<Value> {
        constructor(
            data: List<Value>,
            position: Int,
            totalCount: Int,
            previousPageKey: Key?,
            nextPageKey: Key?
        ) : super(
            data,
            previousPageKey,
            nextPageKey,
            position,
            totalCount - data.size - position,
            position,
            true
        )

        constructor(data: List<Value>, previousPageKey: Key?, nextPageKey: Key?) :
                super(data, previousPageKey, nextPageKey, 0, 0, 0, false)
    }

    /**
     * Type produced by [loadBefore] and [loadAfter] to represent a page of loaded data.
     *
     * @param Key Type of key used to identify pages.
     * @param Value Type of items being loaded by the [DataSource].
     */
    internal class Result<Key : Any, Value : Any>(data: List<Value>, adjacentPageKey: Key?) :
        DataSource.BaseResult<Value>(data, adjacentPageKey, adjacentPageKey, 0, 0, 0, false)

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
    abstract class LoadInitialCallback<Key, Value> {
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
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are `N`
         *                 items before the items in data that can be loaded from this DataSource,
         *                 pass `N`.
         * @param totalCount Total number of items that may be returned from this DataSource.
         *                   Includes the number in the initial `data` parameter as well as any
         *                   items that can be loaded in front or behind of `data`.
         */
        abstract fun onResult(
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
         *                        data can be loaded before.
         * @param nextPageKey Key for page after the initial load result, or `null` if no
         *                    more data can be loaded after.
         */
        abstract fun onResult(data: List<Value>, previousPageKey: Key?, nextPageKey: Key?)

        /**
         * Called to report an error from a DataSource.
         *
         * Call this method to report an error from [loadInitial].
         *
         * @param error The error that occurred during loading.
         */
        open fun onError(error: Throwable) {
            // TODO: remove default implementation in 3.0
            throw IllegalStateException(
                "You must implement onError if implementing your own load callback"
            )
        }
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
    abstract class LoadCallback<Key, Value> {
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
         *                        page in [loadAfter]), or `null` if there are no more pages to load
         *                        in the current load direction.
         */
        abstract fun onResult(data: List<Value>, adjacentPageKey: Key?)

        /**
         * Called to report an error from a DataSource.
         *
         * Call this method to report an error from your PageKeyedDataSource's [loadBefore] and
         * [loadAfter] methods.
         *
         * @param error The error that occurred during loading.
         */
        open fun onError(error: Throwable) {
            // TODO: remove default implementation in 3.0
            throw IllegalStateException(
                "You must implement onError if implementing your own load callback"
            )
        }
    }

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override fun load(params: Params<Key>): ListenableFuture<out BaseResult<Value>> {
        if (params.type == LoadType.INITIAL) {
            val initParams = LoadInitialParams<Key>(
                params.initialLoadSize,
                params.placeholdersEnabled
            )
            return loadInitial(initParams)
        } else {
            if (params.key == null) {
                // null key, immediately return empty data
                val future = ResolvableFuture.create<BaseResult<Value>>()
                future.set(BaseResult.empty())
                return future
            }

            val loadParams = LoadParams(params.key, params.pageSize)

            if (params.type == LoadType.START) {
                return loadBefore(loadParams)
            } else if (params.type == LoadType.END) {
                return loadAfter(loadParams)
            }
        }
        throw IllegalArgumentException("Unsupported type " + params.type.toString())
    }

    internal fun loadInitial(
        params: LoadInitialParams<Key>
    ): ListenableFuture<InitialResult<Key, Value>> {
        val future = ResolvableFuture.create<InitialResult<Key, Value>>()
        executor.execute {
            val callback = object : LoadInitialCallback<Key, Value>() {
                override fun onResult(
                    data: List<Value>,
                    position: Int,
                    totalCount: Int,
                    previousPageKey: Key?,
                    nextPageKey: Key?
                ) {
                    future.set(
                        InitialResult(data, position, totalCount, previousPageKey, nextPageKey)
                    )
                }

                override fun onResult(data: List<Value>, previousPageKey: Key?, nextPageKey: Key?) {
                    future.set(InitialResult(data, previousPageKey, nextPageKey))
                }

                override fun onError(error: Throwable) {
                    future.setException(error)
                }
            }
            loadInitial(
                LoadInitialParams(params.requestedLoadSize, params.placeholdersEnabled),
                callback
            )
        }
        return future
    }

    private fun getFutureAsCallback(future: ResolvableFuture<Result<Key, Value>>) =
        object : LoadCallback<Key, Value>() {
            override fun onResult(data: List<Value>, adjacentPageKey: Key?) {
                future.set(Result(data, adjacentPageKey))
            }

            override fun onError(error: Throwable) {
                future.setException(error)
            }
        }

    private fun loadBefore(params: LoadParams<Key>): ListenableFuture<Result<Key, Value>> {
        val future = ResolvableFuture.create<Result<Key, Value>>()
        executor.execute {
            loadBefore(
                LoadParams(params.key, params.requestedLoadSize),
                getFutureAsCallback(future)
            )
        }
        return future
    }

    private fun loadAfter(params: LoadParams<Key>): ListenableFuture<Result<Key, Value>> {
        val future = ResolvableFuture.create<Result<Key, Value>>()
        executor.execute {
            loadAfter(LoadParams(params.key, params.requestedLoadSize), getFutureAsCallback(future))
        }
        return future
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
    abstract fun loadInitial(
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
     *               size.
     * @param callback Callback that receives loaded data.
     */
    abstract fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Key, Value>)

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
     *               size.
     * @param callback Callback that receives loaded data.
     */
    abstract fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Key, Value>)

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override fun getKeyInternal(item: Value): Key =
        throw IllegalStateException("Cannot get key by item in pageKeyedDataSource")

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override val supportsPageDropping = false

    final override fun <ToValue : Any> mapByPage(
        function: Function<List<Value>, List<ToValue>>
    ): PageKeyedDataSource<Key, ToValue> = WrapperPageKeyedDataSource(this, function)

    final override fun <ToValue : Any> mapByPage(
        function: (List<Value>) -> List<ToValue>
    ): PageKeyedDataSource<Key, ToValue> = mapByPage(Function { function(it) })

    final override fun <ToValue : Any> map(
        function: Function<Value, ToValue>
    ): PageKeyedDataSource<Key, ToValue> =
        mapByPage(Function { list -> list.map { function.apply(it) } })

    final override fun <ToValue : Any> map(
        function: (Value) -> ToValue
    ): PageKeyedDataSource<Key, ToValue> = mapByPage(Function { list -> list.map(function) })
}
