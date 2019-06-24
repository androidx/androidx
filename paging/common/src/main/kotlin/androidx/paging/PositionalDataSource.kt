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
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.util.Function
import androidx.concurrent.futures.ResolvableFuture
import androidx.paging.DataSource.KeyType.POSITIONAL
import androidx.paging.PositionalDataSource.LoadInitialCallback
import com.google.common.util.concurrent.ListenableFuture

/**
 * Position-based data loader for a fixed-size, countable data set, supporting fixed-size loads at
 * arbitrary page positions.
 *
 * Extend PositionalDataSource if you can load pages of a requested size at arbitrary positions,
 * and provide a fixed item count. If your data source can't support loading arbitrary requested
 * page sizes (e.g. when network page size constraints are only known at runtime), either use
 * [PageKeyedDataSource] or [ItemKeyedDataSource], or pass the initial result with the two parameter
 * [LoadInitialCallback.onResult].
 *
 * Room can generate a Factory of PositionalDataSources for you:
 * ```
 * @Dao
 * interface UserDao {
 *     @Query("SELECT * FROM user ORDER BY age DESC")
 *     public abstract DataSource.Factory<Integer, User> loadUsersByAgeDesc();
 * }
 * ```
 *
 * @param T Type of items being loaded by the [PositionalDataSource].
 */
abstract class PositionalDataSource<T : Any> : DataSource<Int, T>(POSITIONAL) {
    /**
     * Holder object for inputs to [loadInitial].
     */
    open class LoadInitialParams(
        /**
         * Initial load position requested.
         *
         * Note that this may not be within the bounds of your data set, it may need to be adjusted
         * before you execute your load.
         */
        @JvmField
        val requestedStartPosition: Int,
        /**
         * Requested number of items to load.
         *
         * Note that this may be larger than available data.
         */
        @JvmField
        val requestedLoadSize: Int,
        /**
         * Defines page size acceptable for return values.
         *
         * List of items passed to the callback must be an integer multiple of page size.
         */
        @JvmField
        val pageSize: Int,
        /**
         * Defines whether placeholders are enabled, and whether the loaded total count will be
         * ignored.
         */
        @JvmField
        val placeholdersEnabled: Boolean
    )

    /**
     * Holder object for inputs to [loadRange].
     */
    open class LoadRangeParams(
        /**
         * START position of data to load.
         *
         * Returned data must start at this position.
         */
        @JvmField
        val startPosition: Int,
        /**
         * Number of items to load.
         *
         * Returned data must be of this size, unless at end of the list.
         */
        @JvmField
        val loadSize: Int
    )

    /**
     * Type produced by [loadInitial] to represent initially loaded data.
     *
     * @param V The type of the data loaded.
     */
    internal class InitialResult<V : Any> : BaseResult<V> {
        constructor(data: List<V>, position: Int, totalCount: Int) :
                super(data, null, null, position, totalCount - data.size - position, 0, true) {
            if (data.isEmpty() && position != 0) {
                throw IllegalArgumentException(
                    "Initial result cannot be empty if items are present in data set."
                )
            }
        }

        constructor(data: List<V>, position: Int) : super(data, null, null, 0, 0, position, false) {
            if (data.isEmpty() && position != 0) {
                throw IllegalArgumentException(
                    "Initial result cannot be empty if items are present in data set."
                )
            }
        }
    }

    /**
     * Type produced by [loadRange] to represent a page of loaded data.
     *
     * @param V The type of the data loaded.
     */
    internal class RangeResult<V : Any>(data: List<V>) :
        BaseResult<V>(data, null, null, 0, 0, 0, false)

    /**
     * Callback for [loadInitial] to return data, position, and count.
     *
     * A callback should be called only once, and may throw if called again.
     *
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param T Type of items being loaded.
     */
    abstract class LoadInitialCallback<T> {
        /**
         * Called to pass initial load state from a DataSource.
         *
         * Call this method from [loadInitial] function to return data, and inform how many
         * placeholders should be shown before and after. If counting is cheap compute (for example,
         * if a network load returns the information regardless), it's recommended to pass the total
         * size to the totalCount parameter. If placeholders are not requested (when
         * [LoadInitialParams.placeholdersEnabled] is false), you can instead call [onResult].
         *
         * @param data List of items loaded from the [DataSource]. If this is empty, the
         * [DataSource] is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are N items
         * before the items in data that can be loaded from this DataSource, pass N.
         * @param totalCount Total number of items that may be returned from this DataSource.
         * Includes the number in the initial [data] parameter as well as any items that can be
         * loaded in front or behind of [data].
         */
        abstract fun onResult(data: List<T>, position: Int, totalCount: Int)

        /**
         * Called to pass initial load state from a DataSource without total count, when
         * placeholders aren't requested.
         *
         * **Note:** This method can only be called when placeholders are disabled (i.e.,
         * [LoadInitialParams.placeholdersEnabled] is `false`).
         *
         * Call this method from [loadInitial] function to return data, if position is known but
         * total size is not. If placeholders are requested, call the three parameter variant:
         * [onResult].
         *
         * @param data List of items loaded from the [DataSource]. If this is empty, the
         * [DataSource] is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are N items
         * before the items in data that can be provided by this [DataSource], pass N.
         */
        abstract fun onResult(data: List<T>, position: Int)

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
     * Callback for PositionalDataSource [loadRange] to return data.
     *
     * A callback should be called only once, and may throw if called again.
     *
     * It is always valid for a [DataSource] loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param T Type of items being loaded.
     */
    abstract class LoadRangeCallback<T> {
        /**
         * Called to pass loaded data from [loadRange].
         *
         * @param data List of items loaded from the [DataSource]. Must be same size as requested,
         *             unless at end of list.
         */
        abstract fun onResult(data: List<T>)

        /**
         * Called to report an error from a [DataSource].
         *
         * Call this method to report an error from [loadRange].
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    companion object {
        /**
         * Helper for computing an initial position in [loadInitial] when total data set size can be
         * computed ahead of loading.
         *
         * The value computed by this function will do bounds checking, page alignment, and
         * positioning based on initial load size requested.
         *
         * Example usage in a [PositionalDataSource] subclass:
         * ```
         * class ItemDataSource extends PositionalDataSource<Item> {
         *     private int computeCount() {
         *         // actual count code here
         *     }
         *
         *     private List<Item> loadRangeInternal(int startPosition, int loadCount) {
         *         // actual load code here
         *     }
         *
         *     @Override
         *     public void loadInitial(@NonNull LoadInitialParams params,
         *         @NonNull LoadInitialCallback<Item> callback) {
         *         int totalCount = computeCount();
         *         int position = computeInitialLoadPosition(params, totalCount);
         *         int loadSize = computeInitialLoadSize(params, position, totalCount);
         *         callback.onResult(loadRangeInternal(position, loadSize), position, totalCount);
         *     }
         *
         *     @Override
         *     public void loadRange(@NonNull LoadRangeParams params,
         *         @NonNull LoadRangeCallback<Item> callback) {
         *         callback.onResult(loadRangeInternal(params.startPosition, params.loadSize));
         *     }
         * }
         * ```
         *
         * @param params Params passed to [loadInitial], including page size, and requested start /
         *               loadSize.
         * @param totalCount Total size of the data set.
         * @return Position to start loading at.
         *
         * @see [computeInitialLoadSize]
         */
        @JvmStatic
        fun computeInitialLoadPosition(params: LoadInitialParams, totalCount: Int): Int {
            val position = params.requestedStartPosition
            val initialLoadSize = params.requestedLoadSize
            val pageSize = params.pageSize

            var pageStart = position / pageSize * pageSize

            // maximum start pos is that which will encompass end of list
            val maximumLoadPage =
                (totalCount - initialLoadSize + pageSize - 1) / pageSize * pageSize
            pageStart = minOf(maximumLoadPage, pageStart)

            // minimum start position is 0
            pageStart = maxOf(0, pageStart)

            return pageStart
        }

        /**
         * Helper for computing an initial load size in [loadInitial] when total data set size can
         * be computed ahead of loading.
         *
         * This function takes the requested load size, and bounds checks it against the value
         * returned by [computeInitialLoadPosition].
         *
         * Example usage in a [PositionalDataSource] subclass:
         * ```
         * class ItemDataSource extends PositionalDataSource<Item> {
         *     private int computeCount() {
         *         // actual count code here
         *     }
         *
         *     private List<Item> loadRangeInternal(int startPosition, int loadCount) {
         *         // actual load code here
         *     }
         *
         *     @Override
         *     public void loadInitial(@NonNull LoadInitialParams params,
         *         @NonNull LoadInitialCallback<Item> callback) {
         *         int totalCount = computeCount();
         *         int position = computeInitialLoadPosition(params, totalCount);
         *         int loadSize = computeInitialLoadSize(params, position, totalCount);
         *         callback.onResult(loadRangeInternal(position, loadSize), position, totalCount);
         *     }
         *
         *     @Override
         *     public void loadRange(@NonNull LoadRangeParams params,
         *         @NonNull LoadRangeCallback<Item> callback) {
         *         callback.onResult(loadRangeInternal(params.startPosition, params.loadSize));
         *     }
         * }
         * ```
         *
         * @param params Params passed to [loadInitial], including page size, and requested start /
         * loadSize.
         * @param initialLoadPosition Value returned by [computeInitialLoadPosition]
         * @param totalCount Total size of the data set.
         * @return Number of items to load.
         *
         * @see [computeInitialLoadPosition]
         */
        @JvmStatic
        fun computeInitialLoadSize(
            params: LoadInitialParams,
            initialLoadPosition: Int,
            totalCount: Int
        ) = minOf(totalCount - initialLoadPosition, params.requestedLoadSize)
    }

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override fun load(params: Params<Int>): ListenableFuture<out BaseResult<T>> {
        if (params.type == LoadType.INITIAL) {
            var initialPosition = 0
            var initialLoadSize = params.initialLoadSize
            if (params.key != null) {
                initialPosition = params.key

                if (params.placeholdersEnabled) {
                    // snap load size to page multiple (minimum two)
                    initialLoadSize =
                        maxOf(initialLoadSize / params.pageSize, 2) * params.pageSize

                    // move start so the load is centered around the key, not starting at it
                    val idealStart = initialPosition - initialLoadSize / 2
                    initialPosition = maxOf(0, idealStart / params.pageSize * params.pageSize)
                } else {
                    // not tiled, so don't try to snap or force multiple of a page size
                    initialPosition -= initialLoadSize / 2
                }
            }
            val initParams = LoadInitialParams(
                initialPosition,
                initialLoadSize,
                params.pageSize,
                params.placeholdersEnabled
            )
            return loadInitial(initParams)
        } else {
            var startIndex = params.key!!
            var loadSize = params.pageSize
            if (params.type == LoadType.START) {
                loadSize = minOf(loadSize, startIndex + 1)
                startIndex = startIndex - loadSize + 1
            }
            return loadRange(LoadRangeParams(startIndex, loadSize))
        }
    }

    /**
     * Load initial list data.
     *
     * This method is called to load the initial page(s) from the DataSource.
     *
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     * page size.
     * @return [ListenableFuture] of the loaded data.
     */
    @VisibleForTesting
    internal fun loadInitial(params: LoadInitialParams): ListenableFuture<InitialResult<T>> {
        val future = ResolvableFuture.create<InitialResult<T>>()
        executor.execute {
            val newParams = LoadInitialParams(
                params.requestedStartPosition,
                params.requestedLoadSize,
                params.pageSize,
                params.placeholdersEnabled
            )
            val callback = object : LoadInitialCallback<T>() {
                override fun onResult(data: List<T>, position: Int, totalCount: Int) {
                    if (isInvalid) {
                        // NOTE: this isInvalid check works around
                        // https://issuetracker.google.com/issues/124511903
                        future.set(InitialResult(emptyList(), 0, 0))
                    } else {
                        setFuture(newParams, InitialResult(data, position, totalCount))
                    }
                }

                override fun onResult(data: List<T>, position: Int) {
                    if (isInvalid) {
                        // NOTE: this isInvalid check works around
                        // https://issuetracker.google.com/issues/124511903
                        future.set(InitialResult(emptyList(), 0))
                    } else {
                        setFuture(newParams, InitialResult(data, position))
                    }
                }

                private fun setFuture(params: LoadInitialParams, result: InitialResult<T>) {
                    if (params.placeholdersEnabled) {
                        result.validateForInitialTiling(params.pageSize)
                    }
                    future.set(result)
                }

                override fun onError(error: Throwable) {
                    future.setException(error)
                }
            }
            loadInitial(newParams, callback)
        }
        return future
    }

    /**
     * Called to load a range of data from the DataSource.
     *
     * This method is called to load additional pages from the DataSource after the
     * [ItemKeyedDataSource.LoadInitialCallback] passed to dispatchLoadInitial has initialized a
     * [PagedList].
     *
     * Unlike [ItemKeyedDataSource.loadInitial], this method must return the number of items
     * requested, at the position requested.
     *
     * @param params Parameters for load, including start position and load size.
     * @return [ListenableFuture] of the loaded data.
     */
    private fun loadRange(params: LoadRangeParams): ListenableFuture<RangeResult<T>> {
        val future = ResolvableFuture.create<RangeResult<T>>()
        executor.execute {
            val callback = object : LoadRangeCallback<T>() {
                override fun onResult(data: List<T>) {
                    when {
                        isInvalid -> future.set(RangeResult(emptyList()))
                        else -> future.set(RangeResult(data))
                    }
                }

                override fun onError(error: Throwable) {
                    future.setException(error)
                }
            }
            loadRange(LoadRangeParams(params.startPosition, params.loadSize), callback)
        }
        return future
    }

    /**
     * Load initial list data.
     *
     * This method is called to load the initial page(s) from the [DataSource].
     *
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     * page size.
     * @param callback Callback that receives initial load data, including position and total data
     * set size.
     */
    @WorkerThread
    abstract fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>)

    /**
     * Called to load a range of data from the DataSource.
     *
     * This method is called to load additional pages from the DataSource after the
     * [LoadInitialCallback] passed to dispatchLoadInitial has initialized a [PagedList].
     *
     * Unlike [loadInitial], this method must return the number of items requested, at the position
     * requested.
     *
     * @param params Parameters for load, including start position and load size.
     * @param callback Callback that receives loaded data.
     */
    @WorkerThread
    abstract fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>)

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal override val isContiguous = false

    @Suppress("RedundantVisibilityModifier") // Metalava doesn't inherit visibility properly.
    internal final override fun getKeyInternal(item: T): Int =
        throw IllegalStateException("Cannot get key by item in positionalDataSource")

    final override fun <V : Any> mapByPage(
        function: Function<List<T>, List<V>>
    ): PositionalDataSource<V> = WrapperPositionalDataSource(this, function)

    final override fun <V : Any> mapByPage(
        function: (List<T>) -> List<V>
    ): PositionalDataSource<V> = mapByPage(Function { function(it) })

    final override fun <V : Any> map(function: Function<T, V>): PositionalDataSource<V> =
        mapByPage(Function { list -> list.map { function.apply(it) } })

    final override fun <V : Any> map(function: (T) -> V): PositionalDataSource<V> =
        mapByPage(Function { list -> list.map(function) })
}
