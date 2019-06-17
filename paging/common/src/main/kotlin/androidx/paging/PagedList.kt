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

import androidx.annotation.AnyThread
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.util.Function
import androidx.paging.PagedList.Callback
import androidx.paging.PagedList.Config
import androidx.paging.PagedList.Config.Builder
import androidx.paging.PagedList.Config.Companion.MAX_SIZE_UNBOUNDED
import androidx.paging.PagedList.LoadState
import androidx.paging.PagedList.LoadType
import androidx.paging.futures.DirectExecutor
import androidx.paging.futures.transform
import com.google.common.util.concurrent.ListenableFuture
import java.lang.ref.WeakReference
import java.util.AbstractList
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * Callback for changes to loading state - whether the refresh, prepend, or append is idle, loading,
 * or has an error.
 *
 * Used to observe the [LoadState] of any [LoadType] (REFRESH/START/END). For UI purposes (swipe
 * refresh, loading spinner, retry button), this is typically done by registering a
 * [LoadStateListener] with the [PagedListAdapter] or [AsyncPagedListDiffer].
 *
 * These calls will be dispatched on the executor defined by [Builder.setNotifyExecutor], which is
 * generally the main/UI thread.
 *
 * Called when the LoadState has changed - whether the refresh, prepend, or append is idle, loading,
 * or has an error.
 *
 * REFRESH events can be used to drive a [androidx.swiperefreshlayout.widget.SwipeRefreshLayout], or
 * START/END events can be used to drive loading spinner items in your `RecyclerView`.
 *
 * @param type [LoadType] - START, END, or REFRESH.
 * @param state [LoadState] - IDLE, LOADING, DONE, ERROR, or RETRYABLE_ERROR
 * @param error [Throwable] if in an error state, null otherwise.
 *
 * @see [PagedList.retry]
 */
typealias LoadStateListener = (type: LoadType, state: LoadState, error: Throwable?) -> Unit

/**
 * Lazy loading list that pages in immutable content from a [DataSource].
 *
 * A PagedList is a [List] which loads its data in chunks (pages) from a [DataSource]. Items can be
 * accessed with [get], and further loading can be triggered with [loadAround]. To display a
 * PagedList, see [androidx.paging.PagedListAdapter], which enables the binding of a PagedList to a
 * [androidx.recyclerview.widget.RecyclerView].
 *
 * <h4>Loading Data</h4>
 *
 * All data in a PagedList is loaded from its [DataSource]. Creating a PagedList loads the
 * first chunk of data from the DataSource immediately, and should for this reason be done on a
 * background thread. The constructed PagedList may then be passed to and used on the UI thread.
 * This is done to prevent passing a list with no loaded content to the UI thread, which should
 * generally not be presented to the user.
 *
 * A [PagedList] initially presents this first partial load as its content, and expands over time as
 * content is loaded in. When [loadAround] is called, items will be loaded in near the passed
 * list index. If placeholder `null`s are present in the list, they will be replaced as
 * content is loaded. If not, newly loaded items will be inserted at the beginning or end of the
 * list.
 *
 * [PagedList] can present data for an unbounded, infinite scrolling list, or a very large but
 * countable list. Use [Config] to control how many items a [PagedList] loads, and when.
 *
 * If you use [androidx.paging.LivePagedListBuilder] to get a [androidx.lifecycle.LiveData], it will
 * initialize PagedLists on a background thread for you.
 *
 * <h4>Placeholders</h4>
 *
 * There are two ways that [PagedList] can represent its not-yet-loaded data - with or without
 * `null` placeholders.
 *
 * With placeholders, the [PagedList] is always the full size of the data set. `get(N)` returns
 * the `N`th item in the data set, or `null` if its not yet loaded.
 *
 * Without `null` placeholders, the [PagedList] is the sublist of data that has already been
 * loaded. The size of the PagedList is the number of currently loaded items, and `get(N)`
 * returns the `N`th *loaded* item. This is not necessarily the `N`th item in the
 * data set.
 *
 * Placeholders have several benefits:
 *
 *  * They express the full sized list to the presentation layer (often a
 * [androidx.paging.PagedListAdapter]), and so can support scrollbars (without jumping as pages are
 * loaded or dropped) and fast-scrolling to any position, loaded or not.
 *  * They avoid the need for a loading spinner at the end of the loaded list, since the list
 * is always full sized.
 *
 * They also have drawbacks:
 *
 *  * Your Adapter needs to account for `null` items. This often means providing default
 * values in data you bind to a [androidx.recyclerview.widget.RecyclerView.ViewHolder].
 *  * They don't work well if your item views are of different sizes, as this will prevent
 * loading items from cross-fading nicely.
 *  * They require you to count your data set, which can be expensive or impossible, depending
 * on your [DataSource].
 *
 * Placeholders are enabled by default, but can be disabled in two ways. They are disabled if the
 * [DataSource] does not count its data set in its initial load, or if  `false` is passed to
 * [Config.Builder.setEnablePlaceholders] when building a [Config].
 *
 * <h4>Mutability and Snapshots</h4>
 *
 * A [PagedList] is *mutable* while loading, or ready to load from its [DataSource].
 * As loads succeed, a mutable [PagedList] will be updated via Runnables on the main thread. You can
 * listen to these updates with a [Callback]. (Note that [androidx.paging.PagedListAdapter] will
 * listen to these to signal RecyclerView about the updates/changes).
 *
 * If a [PagedList] attempts to load from an invalid [DataSource], it will [detach] from the
 * [DataSource], meaning that it will no longer attempt to load data. It will return true from
 * [isImmutable], and a new [DataSource] / [PagedList] pair must be created to load further data.
 *
 * See [DataSource] and [androidx.paging.LivePagedListBuilder] for how new PagedLists are created to
 * represent changed data.
 *
 * A [PagedList] snapshot is simply an immutable shallow copy of the current state of the
 * [PagedList] as a `List`. It will reference the same inner items, and contain the same `null`
 * placeholders, if present.
 *
 * @param T The type of the entries in the list.
 */
abstract class PagedList<T : Any> : AbstractList<T> {
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Create a [PagedList] which loads data from the provided data source on a background
         * thread,posting updates to the main thread.
         *
         * @param dataSource DataSource providing data to the PagedList
         * @param notifyExecutor Thread tat will use and consume data from the PagedList. Generally,
         *                       this is the UI/main thread.
         * @param fetchExecutor Data loading will be done via this executor - should be a background
         *                      thread.
         * @param boundaryCallback Optional boundary callback to attach to the list.
         * @param config PagedList Config, which defines how the PagedList will load data.
         * @param K Key type that indicates to the DataSource what data to load.
         * @param T Type of items to be held and loaded by the PagedList.
         *
         * @return [ListenableFuture] for newly created [PagedList], which will page in data from
         * the [DataSource] as needed.
         *
         * @hide
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun <K : Any, T : Any> create(
            dataSource: DataSource<K, T>,
            notifyExecutor: Executor,
            fetchExecutor: Executor,
            initialLoadExecutor: Executor,
            boundaryCallback: BoundaryCallback<T>?,
            config: Config,
            key: K?
        ): ListenableFuture<PagedList<T>> {
            dataSource.initExecutor(initialLoadExecutor)

            val lastLoad = when {
                dataSource.type == DataSource.KeyType.POSITIONAL && key != null -> key as Int
                else -> ContiguousPagedList.LAST_LOAD_UNSPECIFIED
            }

            val params = DataSource.Params(
                DataSource.LoadType.INITIAL,
                key,
                config.initialLoadSizeHint,
                config.enablePlaceholders,
                config.pageSize
            )
            return dataSource.load(params).transform(
                Function { initialResult ->
                    dataSource.initExecutor(fetchExecutor)
                    ContiguousPagedList(
                        dataSource,
                        notifyExecutor,
                        fetchExecutor,
                        boundaryCallback,
                        config,
                        initialResult,
                        lastLoad
                    )
                },
                DirectExecutor
            )
        }
    }

    /**
     * Type of load a PagedList can perform.
     *
     * You can use a [LoadStateListener] to observe [LoadState] of any [LoadType]. For UI purposes
     * (swipe refresh, loading spinner, retry button), this is typically done by registering a
     * Listener with the [androidx.paging.PagedListAdapter] or
     * [androidx.paging.AsyncPagedListDiffer].
     *
     * @see LoadState
     */
    enum class LoadType {
        /**
         * PagedList content being reloaded, may contain content updates.
         */
        REFRESH,

        /**
         * Load at the start of the PagedList.
         */
        START,

        /**
         * Load at the end of the PagedList.
         */
        END
    }

    /**
     * State of a PagedList load - associated with a `LoadType`
     *
     * You can use a [LoadStateListener] to observe [LoadState] of any [LoadType]. For UI
     * purposes (swipe refresh, loading spinner, retry button), this is typically done by
     * registering a callback with the `PagedListAdapter` or `AsyncPagedListDiffer`.
     */
    enum class LoadState {
        /**
         * Indicates the PagedList is not currently loading, and no error currently observed.
         */
        IDLE,

        /**
         * Loading is in progress.
         */
        LOADING,

        /**
         * Loading is complete.
         */
        DONE,

        /**
         * Loading hit a non-retryable error.
         */
        ERROR,

        /**
         * Loading hit a retryable error.
         *
         * @see .retry
         */
        RETRYABLE_ERROR
    }

    /**
     * Builder class for [PagedList].
     *
     * [DataSource], [Config], main thread and background executor must all be provided.
     *
     * A [PagedList] queries initial data from its [DataSource] during construction, to avoid empty
     * PagedLists being presented to the UI when possible. It's preferred to present initial data,
     * so that the UI doesn't show an empty list, or placeholders for a few frames, just before
     * showing initial content.
     *
     * [androidx.paging.LivePagedListBuilder] does this creation on a background thread
     * automatically, if you want to receive a `LiveData<PagedList<...>>`.
     *
     * @param Key Type of key used to load data from the [DataSource].
     * @param Value Type of items held and loaded by the [PagedList].
     */
    class Builder<Key : Any, Value : Any> {
        private val dataSource: DataSource<Key, Value>
        private val config: Config
        private var notifyExecutor: Executor? = null
        private var fetchExecutor: Executor? = null
        private var boundaryCallback: BoundaryCallback<Value>? = null
        private var initialKey: Key? = null

        /**
         * Create a PagedList.Builder with the provided [DataSource] and [Config].
         *
         * @param dataSource [DataSource] the [PagedList] will load from.
         * @param config [Config] that defines how the [PagedList] loads data from its [DataSource].
         */
        constructor(dataSource: DataSource<Key, Value>, config: Config) {
            this.dataSource = dataSource
            this.config = config
        }

        /**
         * Create a [PagedList.Builder] with the provided [DataSource] and page size.
         *
         * This method is a convenience for:
         * ```
         * PagedList.Builder(dataSource,
         *     new PagedList.Config.Builder().setPageSize(pageSize).build());
         * ```
         *
         * @param dataSource [DataSource] the [PagedList] will load from.
         * @param pageSize [Config] that defines how the [PagedList] loads data from its
         *                 [DataSource].
         */
        constructor(dataSource: DataSource<Key, Value>, pageSize: Int) : this(
            dataSource,
            PagedList.Config.Builder().setPageSize(pageSize).build()
        )

        /**
         * The executor defining where page loading updates are dispatched.
         *
         * @param notifyExecutor Executor that receives [PagedList] updates, and where [Callback]
         *                       calls are dispatched. Generally, this is the ui/main thread.
         * @return this
         */
        fun setNotifyExecutor(notifyExecutor: Executor) = apply {
            this.notifyExecutor = notifyExecutor
        }

        /**
         * The executor used to fetch additional pages from the [DataSource].
         *
         * Does not affect initial load, which will be done immediately on whichever thread the
         * [PagedList] is created on.
         *
         * @param fetchExecutor [Executor] used to fetch from [DataSources], generally a background
         *                      thread pool for e.g. I/O or network loading.
         * @return this
         */
        fun setFetchExecutor(fetchExecutor: Executor) = apply {
            this.fetchExecutor = fetchExecutor
        }

        /**
         * The [BoundaryCallback] for out of data events.
         *
         * Pass a [BoundaryCallback] to listen to when the [PagedList] runs out of data to load.
         *
         * @param boundaryCallback [BoundaryCallback] for listening to out-of-data events.
         * @return this
         */
        fun setBoundaryCallback(boundaryCallback: BoundaryCallback<Value>?) = apply {
            this.boundaryCallback = boundaryCallback
        }

        /**
         * Sets the initial key the [DataSource] should load around as part of initialization.
         *
         * @param initialKey Key the [DataSource] should load around as part of initialization.
         * @return this
         */
        fun setInitialKey(initialKey: Key?) = apply {
            this.initialKey = initialKey
        }

        /**
         * Creates a [PagedList] with the given parameters.
         *
         * This call will dispatch the [androidx.paging.DataSource]'s loadInitial method immediately
         * on the current thread, and block the current on the result. This method should always be
         * called on a worker thread to prevent blocking the main thread.
         *
         * It's fine to create a PagedList with an async DataSource on the main thread, such as in
         * the constructor of a ViewModel. An async network load won't block the initialLoad
         * function. For a synchronous DataSource such as one created from a Room database, a
         * `LiveData<PagedList>` can be safely constructed with
         * [androidx.paging.LivePagedListBuilder] on the main thread, since actual construction work
         * is deferred, and done on a background thread.
         *
         * While build() will always return a [PagedList], it's important to note that the
         * [PagedList] initial load may fail to acquire data from the [DataSource]. This can happen
         * for example if the [DataSource] is invalidated during its initial load. If this happens,
         * the [PagedList] will be immediately [detached][PagedList.isDetached], and you can retry
         * construction (including setting a new [DataSource]).
         *
         * @return The newly constructed [PagedList]
         */
        @WorkerThread
        @Deprecated(
            "This method has no means of handling errors encountered during initial load, and" +
                    " blocks on the initial load result. Use {@link #buildAsync()} instead."
        )
        fun build(): PagedList<Value> {
            // TODO: define defaults, once they can be used in module without android dependency
            if (notifyExecutor == null) {
                throw IllegalArgumentException("MainThreadExecutor required")
            }
            if (fetchExecutor == null) {
                throw IllegalArgumentException("BackgroundThreadExecutor required")
            }

            try {
                return create(DirectExecutor).get()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Creates a [PagedList] asynchronously with the given parameters.
         *
         * This call will dispatch the [DataSource]'s loadInitial method immediately, and return a
         * `ListenableFuture<PagedList<T>>` that will resolve (triggering
         * [loadStateListeners]) once the initial load is completed (success or failure).
         *
         * @return The newly constructed PagedList
         */
        @Suppress("unused")
        fun buildAsync(): ListenableFuture<PagedList<Value>> {
            // TODO: define defaults, once they can be used in module without android dependency
            if (notifyExecutor == null) {
                throw IllegalArgumentException("MainThreadExecutor required")
            }
            if (fetchExecutor == null) {
                throw IllegalArgumentException("BackgroundThreadExecutor required")
            }

            return create(fetchExecutor!!)
        }

        private fun create(initialFetchExecutor: Executor): ListenableFuture<PagedList<Value>> =
            create(
                dataSource,
                notifyExecutor!!,
                fetchExecutor!!,
                initialFetchExecutor,
                boundaryCallback,
                config,
                initialKey
            )
    }

    /**
     * Callback signaling when content is loaded into the list.
     *
     * Can be used to listen to items being paged in and out. These calls will be dispatched on
     * the executor defined by [Builder.setNotifyExecutor], which is generally the main/UI thread.
     */
    abstract class Callback {
        /**
         * Called when null padding items have been loaded to signal newly available data, or when
         * data that hasn't been used in a while has been dropped, and swapped back to null.
         *
         * @param position Position of first newly loaded items, out of total number of items
         *                 (including padded nulls).
         * @param count Number of items loaded.
         */
        abstract fun onChanged(position: Int, count: Int)

        /**
         * Called when new items have been loaded at the end or beginning of the list.
         *
         * @param position Position of the first newly loaded item (in practice, either `0` or
         *                 `size - 1`.
         * @param count Number of items loaded.
         */
        abstract fun onInserted(position: Int, count: Int)

        /**
         * Called when items have been removed at the end or beginning of the list, and have not
         * been replaced by padded nulls.
         *
         * @param position Position of the first newly loaded item (in practice, either `0` or
         *                 `size - 1`.
         * @param count Number of items loaded.
         */
        abstract fun onRemoved(position: Int, count: Int)
    }

    /**
     * Configures how a [PagedList] loads content from its [DataSource].
     *
     * Use [Config.Builder] to construct and define custom loading behavior, such as
     * [Builder.setPageSize], which defines number of items loaded at a time}.
     */
    class Config internal constructor(
        /**
         * Size of each page loaded by the PagedList.
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
        val prefetchDistance: Int,
        /**
         * Defines whether the PagedList may display null placeholders, if the DataSource provides
         * them.
         */
        @JvmField
        val enablePlaceholders: Boolean,
        /**
         * Size hint for initial load of PagedList, often larger than a regular page.
         */
        @JvmField
        val initialLoadSizeHint: Int,
        /**
         * Defines the maximum number of items that may be loaded into this pagedList before pages
         * should be dropped.
         *
         * [PageKeyedDataSource] does not currently support dropping pages - when loading from a
         * [PageKeyedDataSource], this value is ignored.
         *
         * @see MAX_SIZE_UNBOUNDED
         * @see Builder.setMaxSize
         */
        @JvmField
        val maxSize: Int
    ) {
        /**
         * Builder class for [Config].
         *
         * You must at minimum specify page size with [setPageSize].
         */
        class Builder {
            private var pageSize = -1
            private var prefetchDistance = -1
            private var initialLoadSizeHint = -1
            private var enablePlaceholders = true
            private var maxSize = MAX_SIZE_UNBOUNDED

            /**
             * Defines the number of items loaded at once from the [DataSource].
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
             * @param pageSize Number of items loaded at once from the [DataSource].
             * @return this
             */
            fun setPageSize(@IntRange(from = 1) pageSize: Int) = apply {
                if (pageSize < 1) {
                    throw IllegalArgumentException("Page size must be a positive number")
                }
                this.pageSize = pageSize
            }

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
             * Pass false to disable null placeholders in [PagedLists] using this [Config].
             *
             * If not set, defaults to true.
             *
             * A [PagedList] will present null placeholders for not-yet-loaded content if two
             * conditions are met:
             *
             * 1) Its [DataSource] can count all unloaded items (so that the number of nulls to
             * present is known).
             *
             * 2) placeholders are not disabled on the [Config].
             *
             * Call `setEnablePlaceholders(false)` to ensure the receiver of the PagedList
             * (often a [androidx.paging.PagedListAdapter]) doesn't need to account for null items.
             *
             * If placeholders are disabled, not-yet-loaded content will not be present in the list.
             * Paging will still occur, but as items are loaded or removed, they will be signaled
             * as inserts to the [PagedList.Callback].
             *
             * [PagedList.Callback.onChanged] will not be issued as part of loading, though a
             * [androidx.paging.PagedListAdapter] may still receive change events as a result of
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
             * When using a [PositionalDataSource], the initial load size will be coerced to an
             * integer multiple of pageSize, to enable efficient tiling.
             *
             * If not set, defaults to three times page size.
             *
             * @param initialLoadSizeHint Number of items to load while initializing the
             *                            [PagedList].
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
             * This value must be at least two times the [prefetchDistance][setPrefetchDistance]
             * plus the [pageSize][setPageSize]). This constraint prevent loads from being
             * continuously fetched and discarded due to prefetching.
             *
             * The max size specified here best effort, not a guarantee. In practice, if [maxSize]
             * is many times the page size, the number of items held by the [PagedList] will not
             * grow above this number. Exceptions are made as necessary to guarantee:
             *  * Pages are never dropped until there are more than two pages loaded. Note that
             * a [DataSource] may not be held strictly to [requested pageSize][Config.pageSize], so
             * two pages may be larger than expected.
             *  * Pages are never dropped if they are within a prefetch window (defined to be
             * `pageSize + (2 * prefetchDistance)`) of the most recent load.
             *
             * [PageKeyedDataSource] does not currently support dropping pages - when
             * loading from a [PageKeyedDataSource], this value is ignored.
             *
             * If not set, defaults to [MAX_SIZE_UNBOUNDED], which disables page dropping.
             *
             * @param maxSize Maximum number of items to keep in memory, or [MAX_SIZE_UNBOUNDED] to
             *                disable page dropping.
             * @return this
             *
             * @see Config.MAX_SIZE_UNBOUNDED
             * @see Config.maxSize
             */
            fun setMaxSize(@IntRange(from = 2) maxSize: Int) = apply {
                this.maxSize = maxSize
            }

            /**
             * Creates a [Config] with the given parameters.
             *
             * @return A new [Config].
             */
            fun build(): Config {
                if (prefetchDistance < 0) {
                    prefetchDistance = pageSize
                }
                if (initialLoadSizeHint < 0) {
                    initialLoadSizeHint = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER
                }
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

                return Config(
                    pageSize,
                    prefetchDistance,
                    enablePlaceholders,
                    initialLoadSizeHint,
                    maxSize
                )
            }

            internal companion object {
                internal const val DEFAULT_INITIAL_PAGE_MULTIPLIER = 3
            }
        }

        internal companion object {
            /**
             * When [maxSize] is set to [MAX_SIZE_UNBOUNDED], the maximum number of items loaded is
             * unbounded, and pages will never be dropped.
             */
            const val MAX_SIZE_UNBOUNDED = Int.MAX_VALUE
        }
    }

    /**
     * Signals when a PagedList has reached the end of available data.
     *
     * When local storage is a cache of network data, it's common to set up a streaming pipeline:
     * Network data is paged into the database, database is paged into UI. Paging from the database
     * to UI can be done with a `LiveData<PagedList>`, but it's still necessary to know when to
     * trigger network loads.
     *
     * [BoundaryCallback] does this signaling - when a DataSource runs out of data at the end of
     * the list, [onItemAtEndLoaded] is called, and you can start an async network load that will
     * write the result directly to the database. Because the database is being observed, the UI
     * bound to the `LiveData<PagedList>` will update automatically to account for the new items.
     *
     * Note that a BoundaryCallback instance shared across multiple PagedLists (e.g. when passed to
     * [androidx.paging.LivePagedListBuilder.setBoundaryCallback], the callbacks may be issued
     * multiple times. If for example [onItemAtEndLoaded] triggers a network load, it should avoid
     * triggering it again while the load is ongoing.
     *
     * The database + network Repository in the
     * [PagingWithNetworkSample](https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md)
     * shows how to implement a network BoundaryCallback using
     * [Retrofit](https://square.github.io/retrofit/), while handling swipe-to-refresh,
     * network errors, and retry.
     *
     * <h4>Requesting Network Data</h4>
     * [BoundaryCallback] only passes the item at front or end of the list when out of data. This
     * makes it an easy fit for item-keyed network requests, where you can use the item passed to
     * the [BoundaryCallback] to request more data from the network. In these cases, the source of
     * truth for next page to load is coming from local storage, based on what's already loaded.
     *
     * If you aren't using an item-keyed network API, you may be using page-keyed, or page-indexed.
     * If this is the case, the paging library doesn't know about the page key or index used in the
     * [BoundaryCallback], so you need to track it yourself. You can do this in one of two ways:
     *
     * <h5>Local storage Page key</h5>
     * If you want to perfectly resume your query, even if the app is killed and resumed, you can
     * store the key on disk. Note that with a positional/page index network API, there's a simple
     * way to do this, by using the `listSize` as an input to the next load (or
     * `listSize / NETWORK_PAGE_SIZE`, for page indexing).
     *
     * The current list size isn't passed to the BoundaryCallback though. This is because the
     * PagedList doesn't necessarily know the number of items in local storage. Placeholders may be
     * disabled, or the DataSource may not count total number of items.
     *
     * Instead, for these positional cases, you can query the database for the number of items, and
     * pass that to the network.
     * <h5>In-Memory Page key</h5>
     * Often it doesn't make sense to query the next page from network if the last page you fetched
     * was loaded many hours or days before. If you keep the key in memory, you can refresh any time
     * you start paging from a network source.
     *
     * Store the next key in memory, inside your BoundaryCallback. When you create a new
     * BoundaryCallback when creating a new `LiveData`/`Observable` of
     * `PagedList`, refresh data. For example,
     * [in the Paging Codelab](https://codelabs.developers.google.com/codelabs/android-paging/index.html#8),
     * the GitHub network page index is stored in memory.
     *
     * @param T Type loaded by the PagedList.
     */
    @MainThread
    abstract class BoundaryCallback<T> {
        /**
         * Called when zero items are returned from an initial load of the PagedList's data source.
         */
        open fun onZeroItemsLoaded() {}

        /**
         * Called when the item at the front of the PagedList has been loaded, and access has
         * occurred within [Config.prefetchDistance] of it.
         *
         * No more data will be prepended to the PagedList before this item.
         *
         * @param itemAtFront The first item of PagedList
         */
        open fun onItemAtFrontLoaded(itemAtFront: T) {}

        /**
         * Called when the item at the end of the PagedList has been loaded, and access has
         * occurred within [Config.prefetchDistance] of it.
         *
         * No more data will be appended to the [PagedList] after this item.
         *
         * @param itemAtEnd The first item of [PagedList]
         */
        open fun onItemAtEndLoaded(itemAtEnd: T) {}
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract class LoadStateManager {
        var refresh = LoadState.IDLE
            private set
        private var refreshError: Throwable? = null
        var start = LoadState.IDLE
            private set
        private var startError: Throwable? = null
        var end = LoadState.IDLE
            private set
        private var endError: Throwable? = null

        fun setState(type: LoadType, state: LoadState, error: Throwable?) {
            val expectError = state == LoadState.RETRYABLE_ERROR || state == LoadState.ERROR
            val hasError = error != null
            if (expectError != hasError) {
                throw IllegalArgumentException(
                    "Error states must be accompanied by a throwable, other states must not"
                )
            }

            // deduplicate signals
            when (type) {
                LoadType.REFRESH -> {
                    if (refresh == state && refreshError == error) return
                    refresh = state
                    refreshError = error
                }
                LoadType.START -> {
                    if (start == state && startError == error) return
                    start = state
                    startError = error
                }
                LoadType.END -> {
                    if (end == state && endError == error) return
                    end = state
                    endError = error
                }
            }
            onStateChanged(type, state, error)
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // protected otherwise.
        abstract fun onStateChanged(type: LoadType, state: LoadState, error: Throwable?)

        fun dispatchCurrentLoadState(callback: LoadStateListener) {
            callback(LoadType.REFRESH, refresh, refreshError)
            callback(LoadType.START, start, startError)
            callback(LoadType.END, end, endError)
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(
        storage: PagedStorage<T>,
        mainThreadExecutor: Executor,
        backgroundThreadExecutor: Executor,
        boundaryCallback: BoundaryCallback<T>?,
        config: Config
    ) : super() {
        this.storage = storage
        this.mainThreadExecutor = mainThreadExecutor
        this.backgroundThreadExecutor = backgroundThreadExecutor
        this.boundaryCallback = boundaryCallback
        this.config = config
        this.callbacks = ArrayList()
        this.loadStateListeners = ArrayList()
        requiredRemainder = this.config.prefetchDistance * 2 + this.config.pageSize
    }

    internal val storage: PagedStorage<T>

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // protected otherwise
    fun getStorage() = storage

    internal val mainThreadExecutor: Executor
    internal val backgroundThreadExecutor: Executor
    internal val boundaryCallback: BoundaryCallback<T>?

    internal var refreshRetryCallback: Runnable? = null

    /**
     * Last access location, in total position space (including offset).
     *
     * Used by positional data sources to initialize loading near viewport
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // internal otherwise
    var lastLoad = 0
    internal var lastItem: T? = null

    internal val requiredRemainder: Int

    /**
     * Return the Config used to construct this PagedList.
     *
     * @return the Config of this PagedList
     */
    open val config: Config

    private val callbacks: MutableList<WeakReference<Callback>>

    private val loadStateListeners: MutableList<WeakReference<LoadStateListener>>

    // if set to true, boundaryCallback is non-null, and should
    // be dispatched when nearby load has occurred
    private var boundaryCallbackBeginDeferred = false

    private var boundaryCallbackEndDeferred = false

    // lowest and highest index accessed by loadAround. Used to
    // decide when boundaryCallback should be dispatched
    private var lowestIndexAccessed = Int.MAX_VALUE
    private var highestIndexAccessed = Int.MIN_VALUE

    /**
     * Size of the list, including any placeholders (not-yet-loaded null padding).
     *
     * To get the number of loaded items, not counting placeholders, use [loadedCount].
     *
     * @see loadedCount
     */
    override val size
        get() = storage.size

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    abstract val isContiguous: Boolean

    /**
     * The [DataSource] that provides data to this [PagedList].
     */
    abstract val dataSource: DataSource<*, T>

    /**
     * Return the key for the position passed most recently to [loadAround].
     *
     * When a PagedList is invalidated, you can pass the key returned by this function to initialize
     * the next PagedList. This ensures (depending on load times) that the next PagedList that
     * arrives will have data that overlaps. If you use androidx.paging.LivePagedListBuilder, it
     * will do this for you.
     *
     * @return Key of position most recently passed to [loadAround].
     */
    abstract val lastKey: Any?

    /**
     * True if the [PagedList] has detached the [DataSource] it was loading from, and will no longer
     * load new data.
     *
     * A detached list is [immutable][isImmutable].
     *
     * @return True if the data source is detached.
     */
    abstract val isDetached: Boolean

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    abstract fun dispatchCurrentLoadState(callback: LoadStateListener)

    /**
     * Dispatch updates since the non-empty snapshot was taken.
     *
     * @param snapshot Non-empty snapshot.
     * @param callback [Callback] for updates that have occurred since snapshot.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    abstract fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<T>, callback: Callback)

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    abstract fun loadAroundInternal(index: Int)

    /**
     * Detach the PagedList from its DataSource, and attempt to load no more data.
     *
     * This is called automatically when a DataSource is observed to be invalid, which is a
     * signal to stop loading. The PagedList will continue to present existing data, but will not
     * initiate new loads.
     */
    abstract fun detach()

    /**
     * Returns the number of items loaded in the [PagedList].
     *
     * Unlike [size] this counts only loaded items, not placeholders.
     *
     * If placeholders are [disabled][Config.enablePlaceholders], this method is equivalent to
     * [size].
     *
     * @return Number of items currently loaded, not counting placeholders.
     *
     * @see size
     */
    open val loadedCount
        get() = storage.storageCount

    /**
     * Returns whether the list is immutable.
     *
     * Immutable lists may not become mutable again, and may safely be accessed from any thread.
     *
     * In the future, this method may return true when a PagedList has completed loading from its
     * DataSource. Currently, it is equivalent to [isDetached].
     *
     * @return `true` if the [PagedList] is immutable.
     */
    open val isImmutable
        get() = isDetached

    /**
     * Position offset of the data in the list.
     *
     * If data is supplied by a [PositionalDataSource], the item returned from `get(i)` has a
     * position of `i + getPositionOffset()`.
     *
     * If the DataSource is a [ItemKeyedDataSource] or [PageKeyedDataSource], it doesn't use
     * positions, returns 0.
     */
    open val positionOffset: Int
        get() = storage.positionOffset

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun setInitialLoadState(loadState: LoadState, error: Throwable?) {
    }

    /**
     * Retry any retryable errors associated with this [PagedList].
     *
     * If for example a network DataSource append timed out, calling this method will retry the
     * failed append load. Note that your DataSource will need to pass `true` to `onError()` to
     * signify the error as retryable.
     *
     * You can observe loading state via [addWeakLoadStateListener], though generally this is done
     * through the [PagedListAdapter][androidx.paging.PagedListAdapter] or
     * [AsyncPagedListDiffer][androidx.paging.AsyncPagedListDiffer].
     *
     * @see addWeakLoadStateListener
     * @see removeWeakLoadStateListener
     */
    open fun retry() {}

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setRetryCallback(refreshRetryCallback: Runnable?) {
        this.refreshRetryCallback = refreshRetryCallback
    }

    internal fun dispatchStateChange(type: LoadType, state: LoadState, error: Throwable?) {
        loadStateListeners.removeAll { it.get() == null }
        loadStateListeners.forEach { it.get()?.invoke(type, state, error) }
    }

    /**
     * Get the item in the list of loaded items at the provided index.
     *
     * @param index Index in the loaded item list. Must be >= 0, and < [size]
     * @return The item at the passed index, or `null` if a `null` placeholder is at the specified
     * position.
     *
     * @see size
     */
    override fun get(index: Int) = storage[index]?.also { item -> lastItem = item }

    /**
     * Load adjacent items to passed index.
     *
     * @param index Index at which to load.
     */
    open fun loadAround(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        lastLoad = index + positionOffset
        loadAroundInternal(index)

        lowestIndexAccessed = minOf(lowestIndexAccessed, index)
        highestIndexAccessed = maxOf(highestIndexAccessed, index)

        /*
         * lowestIndexAccessed / highestIndexAccessed have been updated, so check if we need to
         * dispatch boundary callbacks. Boundary callbacks are deferred until last items are loaded,
         * and accesses happen near the boundaries.
         *
         * Note: we post here, since RecyclerView may want to add items in response, and this
         * call occurs in PagedListAdapter bind.
         */
        tryDispatchBoundaryCallbacks(true)
    }

    // Creation thread for initial synchronous load, otherwise main thread
    // Safe to access main thread only state - no other thread has reference during construction
    @AnyThread
    internal fun deferBoundaryCallbacks(
        deferEmpty: Boolean,
        deferBegin: Boolean,
        deferEnd: Boolean
    ) {
        if (boundaryCallback == null) {
            throw IllegalStateException("Can't defer BoundaryCallback, no instance")
        }

        /*
         * If lowest/highest haven't been initialized, set them to storage size,
         * since placeholders must already be computed by this point.
         *
         * This is just a minor optimization so that BoundaryCallback callbacks are sent immediately
         * if the initial load size is smaller than the prefetch window (see
         * TiledPagedListTest#boundaryCallback_immediate())
         */
        if (lowestIndexAccessed == Int.MAX_VALUE) {
            lowestIndexAccessed = storage.size
        }
        if (highestIndexAccessed == Int.MIN_VALUE) {
            highestIndexAccessed = 0
        }

        if (deferEmpty || deferBegin || deferEnd) {
            // Post to the main thread, since we may be on creation thread currently
            mainThreadExecutor.execute {
                // on is dispatched immediately, since items won't be accessed

                if (deferEmpty) {
                    boundaryCallback.onZeroItemsLoaded()
                }

                // for other callbacks, mark deferred, and only dispatch if loadAround
                // has been called near to the position
                if (deferBegin) {
                    boundaryCallbackBeginDeferred = true
                }
                if (deferEnd) {
                    boundaryCallbackEndDeferred = true
                }
                tryDispatchBoundaryCallbacks(false)
            }
        }
    }

    /**
     * Call this when lowest/HighestIndexAccessed are changed, or boundaryCallbackBegin/EndDeferred
     * is set.
     */
    private fun tryDispatchBoundaryCallbacks(post: Boolean) {
        val dispatchBegin = boundaryCallbackBeginDeferred &&
                lowestIndexAccessed <= config.prefetchDistance
        val dispatchEnd = boundaryCallbackEndDeferred &&
                highestIndexAccessed >= size - 1 - config.prefetchDistance

        if (!dispatchBegin && !dispatchEnd) return

        if (dispatchBegin) {
            boundaryCallbackBeginDeferred = false
        }
        if (dispatchEnd) {
            boundaryCallbackEndDeferred = false
        }
        if (post) {
            mainThreadExecutor.execute { dispatchBoundaryCallbacks(dispatchBegin, dispatchEnd) }
        } else {
            dispatchBoundaryCallbacks(dispatchBegin, dispatchEnd)
        }
    }

    private fun dispatchBoundaryCallbacks(begin: Boolean, end: Boolean) {
        // safe to deref boundaryCallback here, since we only defer if boundaryCallback present
        if (begin) {
            boundaryCallback!!.onItemAtFrontLoaded(storage.firstLoadedItem)
        }
        if (end) {
            boundaryCallback!!.onItemAtEndLoaded(storage.lastLoadedItem)
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    internal fun offsetAccessIndices(offset: Int) {
        // update last loadAround index
        lastLoad += offset

        // update access range
        lowestIndexAccessed += offset
        highestIndexAccessed += offset
    }

    /**
     * Returns an immutable snapshot of the [PagedList] in its current state.
     *
     * If this [PagedList] [is immutable][isImmutable] due to its DataSource being invalid, it will
     * be returned.
     *
     * @return Immutable snapshot of PagedList data.
     */
    open fun snapshot(): List<T> = when {
        isImmutable -> this
        else -> SnapshotPagedList(this)
    }

    /**
     * Add a [LoadStateListener] to observe the loading state of the [PagedList].
     *
     * @param listener Listener to receive updates.
     *
     * @see removeWeakLoadStateListener
     */
    open fun addWeakLoadStateListener(listener: LoadStateListener) {
        // Clean up any empty weak refs.
        loadStateListeners.removeAll { it.get() == null }

        // Add the new one.
        loadStateListeners.add(WeakReference(listener))
        dispatchCurrentLoadState(listener)
    }

    /**
     * Remove a previously registered [LoadStateListener].
     *
     * @param listener Previously registered listener.
     *
     * @see addWeakLoadStateListener
     */
    open fun removeWeakLoadStateListener(listener: LoadStateListener) {
        loadStateListeners.removeAll { it.get() == null || it.get() === listener }
    }

    /**
     * Adds a callback, and issues updates since the [previousSnapshot] was created.
     *
     * If [previousSnapshot] is passed, the [callback] will also immediately be dispatched any
     * differences between the previous snapshot, and the current state. For example, if the
     * previousSnapshot was of 5 nulls, 10 items, 5 nulls, and the current state was 5 nulls,
     * 12 items, 3 nulls, the callback would immediately receive a call of`onChanged(14, 2)`.
     *
     * This allows an observer that's currently presenting a snapshot to catch up to the most recent
     * version, including any changes that may have been made.
     *
     * The callback is internally held as weak reference, so [PagedList] doesn't hold a strong
     * reference to its observer, such as a [androidx.paging.PagedListAdapter]. If an adapter were
     * held with a strong reference, it would be necessary to clear its [PagedList] observer before
     * it could be GC'd.
     *
     * @param previousSnapshot Snapshot previously captured from this List, or `null`.
     * @param callback Callback to dispatch to.
     *
     * @see removeWeakCallback
     */
    open fun addWeakCallback(previousSnapshot: List<T>?, callback: Callback) {
        if (previousSnapshot != null && previousSnapshot !== this) {
            if (previousSnapshot.isNotEmpty()) {
                val storageSnapshot = previousSnapshot as PagedList<T>
                dispatchUpdatesSinceSnapshot(storageSnapshot, callback)
            } else if (!storage.isEmpty()) {
                // If snapshot is empty, diff is trivial - just notify number new items.
                // Note: occurs in async init, when snapshot taken before init page arrives
                callback.onInserted(0, storage.size)
            }
        }

        // first, clean up any empty weak refs
        callbacks.removeAll { it.get() == null }

        // then add the new one
        callbacks.add(WeakReference(callback))
    }

    /**
     * Removes a previously added callback.
     *
     * @param callback Callback, previously added.
     *
     * @see addWeakCallback
     */
    open fun removeWeakCallback(callback: Callback) {
        callbacks.removeAll { it.get() == null || it.get() === callback }
    }

    internal fun notifyInserted(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onInserted(position, count) }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun notifyChanged(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onChanged(position, count) }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun notifyRemoved(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onRemoved(position, count) }
    }
}

/**
 * Constructs a [PagedList], convenience for [PagedList.Builder].
 *
 * @param Key Type of key used to load data from the DataSource.
 * @param Value Type of items held and loaded by the PagedList.
 * @param dataSource DataSource the PagedList will load from.
 * @param config Config that defines how the PagedList loads data from its DataSource.
 * @param notifyExecutor Executor that receives PagedList updates, and where [PagedList.Callback]
 *                       calls are dispatched. Generally, this is the UI/main thread.
 * @param fetchExecutor Executor used to fetch from DataSources, generally a background thread pool
 *                      for e.g. I/O or network loading.
 * @param boundaryCallback BoundaryCallback for listening to out-of-data events.
 * @param initialKey Key the DataSource should load around as part of initialization.
 */
@Suppress("FunctionName")
fun <Key : Any, Value : Any> PagedList(
    dataSource: DataSource<Key, Value>,
    config: Config,
    notifyExecutor: Executor,
    fetchExecutor: Executor,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    initialKey: Key? = null
): PagedList<Value> {
    @Suppress("DEPRECATION")
    return PagedList.Builder(dataSource, config)
        .setNotifyExecutor(notifyExecutor)
        .setFetchExecutor(fetchExecutor)
        .setBoundaryCallback(boundaryCallback)
        .setInitialKey(initialKey)
        .build()
}
