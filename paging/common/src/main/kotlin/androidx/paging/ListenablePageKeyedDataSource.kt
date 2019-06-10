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

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture

/**
 * Incremental data loader for page-keyed content, where requests return keys for next/previous
 * pages.
 *
 * Implement a DataSource using PageKeyedDataSource if you need to use data from page `N - 1`
 * to load page `N`. This is common, for example, in network APIs that include a next/previous
 * link or key with each page load.
 *
 * @param Key Type of data used to query Value types out of the DataSource.
 * @param Value Type of items being loaded by the DataSource.
 */
abstract class ListenablePageKeyedDataSource<Key : Any, Value : Any> :
    DataSource<Key, Value>(KeyType.PAGE_KEYED) {

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override fun load(params: Params<Key>): ListenableFuture<out BaseResult<Value>> {
        if (params.type == LoadType.INITIAL) {
            val initParams = PageKeyedDataSource.LoadInitialParams<Key>(
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

            val loadParams = PageKeyedDataSource.LoadParams(params.key, params.pageSize)

            if (params.type == LoadType.START) {
                return loadBefore(loadParams)
            } else if (params.type == LoadType.END) {
                return loadAfter(loadParams)
            }
        }
        throw IllegalArgumentException("Unsupported type " + params.type.toString())
    }

    /**
     * Holder object for inputs to [loadInitial].
     *
     * @param Key Type of data used to query pages.
     * @property requestedLoadSize Requested number of items to load.
     *
     *                             Note that this may be larger than available data.
     * @property placeholdersEnabled Defines whether placeholders are enabled, and whether the
     *                               loaded total count will be ignored.
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
     *               Returned data must begin directly adjacent to this position.
     * @property requestedLoadSize Requested number of items to load.
     *
     *                             Returned page can be of this size, but it may be altered if that
     *                             is easier, e.g. a network data source where the backend defines
     *                             page size.
     */
    open class LoadParams<Key : Any>(@JvmField val key: Key, @JvmField val requestedLoadSize: Int)

    /**
     * Load initial data.
     *
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the position and
     * count to the [InitialResult constructor][InitialResult]. This enables PagedLists presenting
     * data from this source to display placeholders to represent unloaded items.
     *
     * [LoadInitialParams.requestedLoadSize] is a hint, not a requirement, so it may be may be
     * altered or ignored.
     *
     * @param params Parameters for initial load, including requested load size.
     * @return ListenableFuture of the loaded data.
     */
    abstract fun loadInitial(
        params: LoadInitialParams<Key>
    ): ListenableFuture<InitialResult<Key, Value>>

    /**
     * Prepend page with the key specified by [LoadParams.key][PageKeyedDataSource.LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return ListenableFuture of the loaded data.
     */
    abstract fun loadBefore(params: LoadParams<Key>): ListenableFuture<Result<Key, Value>>

    /**
     * Append page with the key specified by [LoadParams.key][PageKeyedDataSource.LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return ListenableFuture of the loaded data.
     */
    abstract fun loadAfter(params: LoadParams<Key>): ListenableFuture<Result<Key, Value>>

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getKeyInternal(item: Value): Key =
        throw IllegalStateException("Cannot get key by item in pageKeyedDataSource")

    /**
     *  To support page dropping when PageKeyed, we'll need to:
     *    - Stash keys for every page we have loaded (can id by index relative to loadInitial)
     *    - Drop keys for any page not adjacent to loaded content
     *    - And either:
     *        - Allow impl to signal previous page key: onResult(data, nextPageKey, prevPageKey)
     *        - Re-trigger loadInitial, and break assumption it will only occur once.
     */
    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override val supportsPageDropping = false

    /**
     * Type produced by [loadInitial] to represent initially loaded data.
     *
     * @param Key Type of key used to identify pages.
     * @param Value Type of items being loaded by the DataSource.
     */
    open class InitialResult<Key : Any, Value : Any> : BaseResult<Value> {
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
    open class Result<Key : Any, Value : Any>(data: List<Value>, adjacentPageKey: Key?) :
        DataSource.BaseResult<Value>(data, adjacentPageKey, adjacentPageKey, 0, 0, 0, false)
}
