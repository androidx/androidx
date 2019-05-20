/*
 * Copyright 2018 The Android Open Source Project
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
import com.google.common.util.concurrent.ListenableFuture

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 *
 * Implement a DataSource using ListenableItemKeyedDataSource if you need to use data from item
 * `N - 1` to load item `N`. This is common, for example, in uniquely sorted database
 * queries where attributes of the item such just before the next query define how to execute it.
 *
 * @see ItemKeyedDataSource
 *
 * @param Key Type of data used to query Value types out of the DataSource.
 * @param Value Type of items being loaded by the DataSource.
 */
abstract class ListenableItemKeyedDataSource<Key : Any, Value : Any> :
    DataSource<Key, Value>(KeyType.ITEM_KEYED) {

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override fun load(params: Params<Key>): ListenableFuture<out BaseResult<Value>> {
        when (params.type) {
            LoadType.INITIAL -> {
                val initParams = ItemKeyedDataSource.LoadInitialParams(
                    params.key, params.initialLoadSize, params.placeholdersEnabled
                )
                return loadInitial(initParams)
            }
            LoadType.START -> {
                val loadParams = ItemKeyedDataSource.LoadParams(params.key!!, params.pageSize)
                return loadBefore(loadParams)
            }
            LoadType.END -> {
                val loadParams = ItemKeyedDataSource.LoadParams(params.key!!, params.pageSize)
                return loadAfter(loadParams)
            }
        }
    }

    /**
     * Holder object for inputs to [loadInitial].
     *
     * @param Key Type of data used to query Value types out of the DataSource.
     * @property requestedInitialKey Load items around this key, or at the beginning of the data set
     *                               if `null` is passed.
     *
     *                               Note that this key is generally a hint, and may be ignored if
     *                               you want to always load from the beginning.
     * @property requestedLoadSize Requested number of items to load.
     *
     *                             Note that this may be larger than available data.
     * @property placeholdersEnabled Defines whether placeholders are enabled, and whether the
     *                               loaded total count will be ignored.
     */
    open class LoadInitialParams<Key : Any>(
        @JvmField val requestedInitialKey: Key?,
        @JvmField val requestedLoadSize: Int,
        @JvmField val placeholdersEnabled: Boolean
    )

    /**
     * Holder object for inputs to [loadBefore] and [loadAfter].
     *
     * @param Key Type of data used to query Value types out of the DataSource.
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
     * the items that can be loaded by the DataSource, it's recommended to pass `totalCount`
     * to the [InitialResult] constructor. This enables PagedLists presenting data from this
     * source to display placeholders to represent unloaded items.
     *
     * [ItemKeyedDataSource.LoadInitialParams.requestedInitialKey] and
     * [ItemKeyedDataSource.LoadInitialParams.requestedLoadSize] are hints, not requirements,
     * so they may be altered or ignored. Note that ignoring the `requestedInitialKey` can
     * prevent subsequent PagedList/DataSource pairs from initializing at the same location. If your
     * DataSource never invalidates (for example, loading from the network without the network ever
     * signalling that old data must be reloaded), it's fine to ignore the `initialLoadKey`
     * and always start from the beginning of the data set.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @return ListenableFuture of the loaded data.
     */
    abstract fun loadInitial(params: LoadInitialParams<Key>): ListenableFuture<InitialResult<Value>>

    /**
     * Load list data after the key specified in
     * [LoadParams.key][ItemKeyedDataSource.LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @return [ListenableFuture] of the loaded data.
     */
    abstract fun loadAfter(params: LoadParams<Key>): ListenableFuture<Result<Value>>

    /**
     * Load list data after the key specified in
     * [LoadParams.key][ItemKeyedDataSource.LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * **Note:** Data returned will be prepended just before the key
     * passed, so if you don't return a page of the requested size, ensure that the last item is
     * adjacent to the passed key.
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @return ListenableFuture of the loaded data.
     */
    abstract fun loadBefore(params: LoadParams<Key>): ListenableFuture<Result<Value>>

    abstract fun getKey(item: Value): Key

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    final override fun getKeyInternal(item: Value): Key = getKey(item)

    /**
     * Type produced by [loadInitial] to represent initially loaded data.
     *
     * @param V The type of the data loaded.
     */
    open class InitialResult<V : Any> : BaseResult<V> {
        constructor(data: List<V>, position: Int, totalCount: Int) : super(
            data,
            null,
            null,
            position,
            totalCount - data.size - position,
            position,
            true
        )

        constructor(data: List<V>) : super(data, null, null, 0, 0, 0, false)
    }

    /**
     * Type produced by [loadBefore] and [loadAfter] to represent a page of loaded data.
     *
     * @param V The type of the data loaded.
     */
    open class Result<V : Any>(data: List<V>) :
        DataSource.BaseResult<V>(data, null, null, 0, 0, 0, false)
}
