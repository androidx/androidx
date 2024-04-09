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

import androidx.annotation.VisibleForTesting
import androidx.arch.core.util.Function
import androidx.paging.DataSource.KeyType.ITEM_KEYED
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 *
 * Implement a DataSource using ItemKeyedDataSource if you need to use data from item `N - 1`
 * to load item `N`. This is common, for example, in uniquely sorted database queries where
 * attributes of the item such just before the next query define how to execute it.
 *
 * The `InMemoryByItemRepository` in the
 * [PagingWithNetworkSample](https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md)
 * shows how to implement a network ItemKeyedDataSource using
 * [Retrofit](https://square.github.io/retrofit/), while handling swipe-to-refresh, network errors,
 * and retry.
 *
 * @param Key Type of data used to query Value types out of the [DataSource].
 * @param Value Type of items being loaded by the [DataSource].
 */
@Deprecated(
    message = "ItemKeyedDataSource is deprecated and has been replaced by PagingSource",
    replaceWith = ReplaceWith(
        "PagingSource<Key, Value>",
        "androidx.paging.PagingSource"
    )
)
public abstract class ItemKeyedDataSource<Key : Any, Value : Any> : DataSource<Key, Value>(
    ITEM_KEYED
) {

    /**
     * Holder object for inputs to [loadInitial].
     *
     * @param Key Type of data used to query [Value] types out of the [DataSource].
     * @param requestedInitialKey Load items around this key, or at the beginning of the data set
     * if `null` is passed.
     *
     * Note that this key is generally a hint, and may be ignored if you want to always load from
     * the beginning.
     * @param requestedLoadSize Requested number of items to load.
     *
     * Note that this may be larger than available data.
     * @param placeholdersEnabled Defines whether placeholders are enabled, and whether the
     * loaded total count will be ignored.
     */
    public open class LoadInitialParams<Key : Any>(
        @JvmField
        public val requestedInitialKey: Key?,
        @JvmField
        public val requestedLoadSize: Int,
        @JvmField
        public val placeholdersEnabled: Boolean
    )

    /**
     * Holder object for inputs to [loadBefore] and [loadAfter].
     *
     * @param Key Type of data used to query [Value] types out of the [DataSource].
     * @param key Load items before/after this key.
     *
     * Returned data must begin directly adjacent to this position.
     * @param requestedLoadSize Requested number of items to load.
     *
     * Returned page can be of this size, but it may be altered if that is easier, e.g. a network
     * data source where the backend defines page size.
     */
    public open class LoadParams<Key : Any>(
        @JvmField
        public val key: Key,
        @JvmField
        public val requestedLoadSize: Int
    )

    /**
     * Callback for [loadInitial]
     * to return data and, optionally, position/count information.
     *
     * A callback can be called only once, and will throw if called again.
     *
     * If you can compute the number of items in the data set before and after the loaded range,
     * call the three parameter [onResult] to pass that information. You can skip passing this
     * information by calling the single parameter [onResult], either if it's difficult to compute,
     * or if [LoadInitialParams.placeholdersEnabled] is `false`, so the positioning information will
     * be ignored.
     *
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param Value Type of items being loaded.
     */
    public abstract class LoadInitialCallback<Value> : LoadCallback<Value>() {
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
         * is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are `N`
         * items before the items in data that can be loaded from this DataSource, pass `N`.
         * @param totalCount Total number of items that may be returned from this [DataSource].
         * Includes the number in the initial `data` parameter as well as any items that can be
         * loaded in front or behind of `data`.
         */
        public abstract fun onResult(data: List<Value>, position: Int, totalCount: Int)
    }

    /**
     * Callback for ItemKeyedDataSource [loadBefore] and [loadAfter] to return data.
     *
     * A callback can be called only once, and will throw if called again.
     *
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param Value Type of items being loaded.
     */
    public abstract class LoadCallback<Value> {
        /**
         * Called to pass loaded data from a DataSource.
         *
         * Call this method from your ItemKeyedDataSource's [loadBefore] and [loadAfter] methods to
         * return data.
         *
         * Call this from [loadInitial] to initialize without counting available data, or supporting
         * placeholders.
         *
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the [ItemKeyedDataSource].
         */
        public abstract fun onResult(data: List<Value>)
    }

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override suspend fun load(params: Params<Key>): BaseResult<Value> {
        return when (params.type) {
            LoadType.REFRESH -> loadInitial(
                LoadInitialParams(
                    params.key,
                    params.initialLoadSize,
                    params.placeholdersEnabled
                )
            )
            LoadType.PREPEND -> loadBefore(LoadParams(params.key!!, params.pageSize))
            LoadType.APPEND -> loadAfter(LoadParams(params.key!!, params.pageSize))
        }
    }

    internal fun List<Value>.getPrevKey() = firstOrNull()?.let { getKey(it) }
    internal fun List<Value>.getNextKey() = lastOrNull()?.let { getKey(it) }

    @VisibleForTesting
    internal suspend fun loadInitial(params: LoadInitialParams<Key>) =
        suspendCancellableCoroutine<BaseResult<Value>> { cont ->
            loadInitial(
                params,
                object : LoadInitialCallback<Value>() {
                    override fun onResult(data: List<Value>, position: Int, totalCount: Int) {
                        cont.resume(
                            BaseResult(
                                data = data,
                                prevKey = data.getPrevKey(),
                                nextKey = data.getNextKey(),
                                itemsBefore = position,
                                itemsAfter = totalCount - data.size - position
                            )
                        )
                    }

                    override fun onResult(data: List<Value>) {
                        cont.resume(
                            BaseResult(
                                data = data,
                                prevKey = data.getPrevKey(),
                                nextKey = data.getNextKey()
                            )
                        )
                    }
                }
            )
        }

    @Suppress("DEPRECATION")
    private fun CancellableContinuation<BaseResult<Value>>.asCallback() =
        object : ItemKeyedDataSource.LoadCallback<Value>() {
            override fun onResult(data: List<Value>) {
                resume(
                    BaseResult(
                        data,
                        data.getPrevKey(),
                        data.getNextKey()
                    )
                )
            }
        }

    @VisibleForTesting
    internal suspend fun loadBefore(params: LoadParams<Key>) =
        suspendCancellableCoroutine<BaseResult<Value>> { cont ->
            loadBefore(params, cont.asCallback())
        }

    @VisibleForTesting
    internal suspend fun loadAfter(params: LoadParams<Key>): BaseResult<Value> {
        return suspendCancellableCoroutine { cont ->
            loadAfter(params, cont.asCallback())
        }
    }

    /**
     * Load initial data.
     *
     * This method is called first to initialize a [PagedList] with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the loaded data to
     * the callback via the three-parameter [LoadInitialCallback.onResult]. This enables PagedLists
     * presenting data from this source to display placeholders to represent unloaded items.
     *
     * [LoadInitialParams.requestedInitialKey] and [LoadInitialParams.requestedLoadSize]
     * are hints, not requirements, so they may be altered or ignored. Note that ignoring the
     * `requestedInitialKey` can prevent subsequent PagedList/DataSource pairs from
     * initializing at the same location. If your DataSource never invalidates (for example,
     * loading from the network without the network ever signalling that old data must be reloaded),
     * it's fine to ignore the `initialLoadKey` and always start from the beginning of the
     * data set.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @param callback Callback that receives initial load data.
     */
    public abstract fun loadInitial(
        params: LoadInitialParams<Key>,
        callback: LoadInitialCallback<Value>
    )

    /**
     * Load list data after the key specified in [LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * Data may be passed synchronously during the loadAfter method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @param callback Callback that receives loaded data.
     */
    public abstract fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Value>)

    /**
     * Load list data before the key specified in [LoadParams.key].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * **Note:** Data returned will be prepended just before the key passed, so if you vary size,
     * ensure that the last item is adjacent to the passed key.
     *
     * Data may be passed synchronously during the loadBefore method, or deferred and called at a
     * later time. Further loads going up will be blocked until the callback is called.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @param callback Callback that receives loaded data.
     */
    public abstract fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Value>)

    /**
     * Return a key associated with the given item.
     *
     * If your ItemKeyedDataSource is loading from a source that is sorted and loaded by a unique
     * integer ID, you would return `item.getID()` here. This key can then be passed to
     * [loadBefore] or [loadAfter] to load additional items adjacent to the item passed to this
     * function.
     *
     * If your key is more complex, such as when you're sorting by name, then resolving collisions
     * with integer ID, you'll need to return both. In such a case you would use a wrapper class,
     * such as `Pair<String, Integer>` or, in Kotlin,
     * `data class Key(val name: String, val id: Int)`
     *
     * @param item Item to get the key from.
     * @return Key associated with given item.
     */
    public abstract fun getKey(item: Value): Key

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override fun getKeyInternal(item: Value): Key = getKey(item)

    @Suppress("DEPRECATION")
    public final override fun <ToValue : Any> mapByPage(
        function: Function<List<Value>, List<ToValue>>
    ): ItemKeyedDataSource<Key, ToValue> = WrapperItemKeyedDataSource(this, function)

    @Suppress("DEPRECATION")
    public final override fun <ToValue : Any> mapByPage(
        function: (List<Value>) -> List<ToValue>
    ): ItemKeyedDataSource<Key, ToValue> = mapByPage(Function { function(it) })

    @Suppress("DEPRECATION")
    public final override fun <ToValue : Any> map(
        function: Function<Value, ToValue>
    ): ItemKeyedDataSource<Key, ToValue> =
        mapByPage(Function { list -> list.map { function.apply(it) } })

    @Suppress("DEPRECATION")
    public final override fun <ToValue : Any> map(
        function: (Value) -> ToValue
    ): ItemKeyedDataSource<Key, ToValue> = mapByPage(Function { list -> list.map(function) })
}
