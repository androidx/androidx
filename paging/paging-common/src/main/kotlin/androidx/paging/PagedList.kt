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
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.AbstractList
import java.util.concurrent.Executor

/**
 * Lazy loading list that pages in immutable content from a [PagingSource].
 *
 * A [PagedList] is a [List] which loads its data in chunks (pages) from a [PagingSource]. Items can
 * be accessed with [get], and further loading can be triggered with [loadAround]. To display a
 * [PagedList], see [androidx.paging.PagedListAdapter], which enables the binding of a [PagedList]
 * to a [androidx.recyclerview.widget.RecyclerView].
 *
 * ### Loading Data
 *
 * All data in a [PagedList] is loaded from its [PagingSource]. Creating a [PagedList] loads the
 * first chunk of data from the [PagingSource] immediately, and should for this reason be done on a
 * background thread. The constructed [PagedList] may then be passed to and used on the UI thread.
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
 * countable list. Use [PagedList.Config] to control how many items a [PagedList] loads, and when.
 *
 * If you use [androidx.paging.LivePagedListBuilder] to get a [androidx.lifecycle.LiveData], it will
 * initialize [PagedList]s on a background thread for you.
 *
 * ### Placeholders
 *
 * There are two ways that [PagedList] can represent its not-yet-loaded data - with or without
 * `null` placeholders.
 *
 * With placeholders, the [PagedList] is always the full size of the data set. `get(N)` returns
 * the `N`th item in the data set, or `null` if its not yet loaded.
 *
 * Without `null` placeholders, the [PagedList] is the sublist of data that has already been
 * loaded. The size of the [PagedList] is the number of currently loaded items, and `get(N)`
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
 * on your [PagingSource].
 *
 * Placeholders are enabled by default, but can be disabled in two ways. They are disabled if the
 * [PagingSource] does not count its data set in its initial load, or if  `false` is passed to
 * [PagedList.Config.Builder.setEnablePlaceholders] when building a [PagedList.Config].
 *
 * ### Mutability and Snapshots
 *
 * A [PagedList] is *mutable* while loading, or ready to load from its [PagingSource].
 * As loads succeed, a mutable [PagedList] will be updated via Runnables on the main thread. You can
 * listen to these updates with a [PagedList.Callback]. (Note that [androidx.paging
 * .PagedListAdapter] will
 * listen to these to signal RecyclerView about the updates/changes).
 *
 * If a [PagedList] attempts to load from an invalid [PagingSource], it will [detach] from the
 * [PagingSource], meaning that it will no longer attempt to load data. It will return true from
 * [isImmutable], and a new [PagingSource] / [PagedList] pair must be created to load further data.
 *
 * See [PagingSource] and [androidx.paging.LivePagedListBuilder] for how new [PagedList]s are
 * created to represent changed data.
 *
 * A [PagedList] snapshot is simply an immutable shallow copy of the current state of the
 * [PagedList] as a `List`. It will reference the same inner items, and contain the same `null`
 * placeholders, if present.
 *
 * @param T The type of the entries in the list.
 */
@Suppress("DEPRECATION")
@Deprecated("PagedList is deprecated and has been replaced by PagingData")
public abstract class PagedList<T : Any> internal constructor(
    /**
     * The [PagingSource] that provides data to this [PagedList].
     *
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val pagingSource: PagingSource<*, T>,
    internal val coroutineScope: CoroutineScope,
    internal val notifyDispatcher: CoroutineDispatcher,
    internal val storage: PagedStorage<T>,

    /**
     * Return the Config used to construct this PagedList.
     *
     * @return the Config of this PagedList
     */
    public val config: Config
) : AbstractList<T>() {
    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Create a [PagedList] which loads data from the provided data source on a background
         * thread, posting updates to the main thread.
         *
         * @param pagingSource [PagingSource] providing data to the [PagedList]
         * @param notifyDispatcher [CoroutineDispatcher] that will use and consume data from the
         * [PagedList]. Generally, this is the UI/main thread.
         * @param fetchDispatcher Data loading jobs will be dispatched to this
         * [CoroutineDispatcher] - should be a background thread.
         * @param boundaryCallback Optional boundary callback to attach to the list.
         * @param config [PagedList.Config], which defines how the [PagedList] will load data.
         * @param K Key type that indicates to the [PagingSource] what data to load.
         * @param T Type of items to be held and loaded by the [PagedList].
         *
         * @return The newly created [PagedList], which will page in data from the [PagingSource] as
         * needed.
         *
         * @suppress
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun <K : Any, T : Any> create(
            pagingSource: PagingSource<K, T>,
            initialPage: PagingSource.LoadResult.Page<K, T>?,
            coroutineScope: CoroutineScope,
            notifyDispatcher: CoroutineDispatcher,
            fetchDispatcher: CoroutineDispatcher,
            boundaryCallback: BoundaryCallback<T>?,
            config: Config,
            key: K?
        ): PagedList<T> {
            val resolvedInitialPage = when (initialPage) {
                null -> {
                    // Compatibility codepath - perform the initial load immediately, since caller
                    // hasn't done it. We block in this case, but it's only used in the legacy path.
                    val params = PagingSource.LoadParams.Refresh(
                        key,
                        config.initialLoadSizeHint,
                        config.enablePlaceholders,
                    )
                    runBlocking {
                        val initialResult = pagingSource.load(params)
                        when (initialResult) {
                            is PagingSource.LoadResult.Page -> initialResult
                            is PagingSource.LoadResult.Error -> throw initialResult.throwable
                            is PagingSource.LoadResult.Invalid ->
                                throw IllegalStateException(
                                    "Failed to create PagedList. The provided PagingSource " +
                                        "returned LoadResult.Invalid, but a LoadResult.Page was " +
                                        "expected. To use a PagingSource which supports " +
                                        "invalidation, use a PagedList builder that accepts a " +
                                        "factory method for PagingSource or DataSource.Factory, " +
                                        "such as LivePagedList."
                                )
                        }
                    }
                }
                else -> initialPage
            }
            return ContiguousPagedList(
                pagingSource,
                coroutineScope,
                notifyDispatcher,
                fetchDispatcher,
                boundaryCallback,
                config,
                resolvedInitialPage,
                key
            )
        }

        /**
         * Extremely naive diff dispatch: mark entire list as modified (essentially,
         * notifyDataSetChanged). We do this because previous logic was incorrect, and could
         * dispatch invalid diffs when pages are dropped. Instead of passing a snapshot, we now
         * recommend to strictly use the addWeakCallback variant that only accepts a callback.
         */
        internal fun dispatchNaiveUpdatesSinceSnapshot(
            currentSize: Int,
            snapshotSize: Int,
            callback: Callback
        ) {
            if (snapshotSize < currentSize) {
                if (snapshotSize > 0) {
                    callback.onChanged(0, snapshotSize)
                }
                val diffCount = currentSize - snapshotSize
                if (diffCount > 0) {
                    callback.onInserted(snapshotSize, diffCount)
                }
            } else {
                if (currentSize > 0) {
                    callback.onChanged(0, currentSize)
                }
                val diffCount = snapshotSize - currentSize
                if (diffCount != 0) {
                    callback.onRemoved(currentSize, diffCount)
                }
            }
        }
    }

    /**
     * Builder class for [PagedList].
     *
     * [pagingSource], [config], [notifyDispatcher] and [fetchDispatcher] must all be provided.
     *
     * A [PagedList] queries initial data from its [PagingSource] during construction, to avoid
     * empty [PagedList]s being presented to the UI when possible. It's preferred to present
     * initial data, so that the UI doesn't show an empty list, or placeholders for a few frames,
     * just before showing initial content.
     *
     * [LivePagedListBuilder][androidx.paging.LivePagedListBuilder] does this creation on a
     * background thread automatically, if you want to receive a `LiveData<PagedList<...>>`.
     *
     * @param Key Type of key used to load data from the [PagingSource].
     * @param Value Type of items held and loaded by the [PagedList].
     */
    @Deprecated(
        message = "PagedList is deprecated and has been replaced by PagingData, which no " +
            "longer supports constructing snapshots of loaded data manually.",
        replaceWith = ReplaceWith("Pager.flow", "androidx.paging.Pager")
    )
    public class Builder<Key : Any, Value : Any> {
        private val pagingSource: PagingSource<Key, Value>?
        private var dataSource: DataSource<Key, Value>?
        private val initialPage: PagingSource.LoadResult.Page<Key, Value>?
        private val config: Config
        @OptIn(DelicateCoroutinesApi::class)
        private var coroutineScope: CoroutineScope = GlobalScope
        private var notifyDispatcher: CoroutineDispatcher? = null
        private var fetchDispatcher: CoroutineDispatcher? = null
        private var boundaryCallback: BoundaryCallback<Value>? = null
        private var initialKey: Key? = null

        /**
         * Create a [Builder][PagedList.Builder] with the provided [DataSource] and
         * [Config][PagedList.Config].
         *
         * @param dataSource [DataSource] the [PagedList] will load from.
         * @param config [PagedList.Config] that defines how the [PagedList] loads data from its
         * [DataSource].
         */
        public constructor(dataSource: DataSource<Key, Value>, config: Config) {
            this.pagingSource = null
            this.dataSource = dataSource
            this.initialPage = null
            this.config = config
        }

        /**
         * Create a [PagedList.Builder] with the provided [DataSource] and [pageSize].
         *
         * This method is a convenience for:
         * ```
         * PagedList.Builder(dataSource,
         *     new PagedList.Config.Builder().setPageSize(pageSize).build());
         * ```
         *
         * @param dataSource [DataSource] the [PagedList] will load from.
         * @param pageSize Size of loaded pages when the [PagedList] loads data from its
         * [DataSource].
         */
        public constructor(dataSource: DataSource<Key, Value>, pageSize: Int) : this(
            dataSource = dataSource,
            config = Config(pageSize)
        )

        /**
         * Create a [PagedList.Builder] with the provided [PagingSource], initial
         * [PagingSource.LoadResult.Page], and [PagedList.Config].
         *
         * @param pagingSource [PagingSource] the [PagedList] will load from.
         * @param initialPage Initial page loaded from the [PagingSource].
         * @param config [PagedList.Config] that defines how the [PagedList] loads data from its
         * [PagingSource].
         */
        public constructor(
            pagingSource: PagingSource<Key, Value>,
            initialPage: PagingSource.LoadResult.Page<Key, Value>,
            config: Config
        ) {
            this.pagingSource = pagingSource
            this.dataSource = null
            this.initialPage = initialPage
            this.config = config
        }

        /**
         * Create a [PagedList.Builder] with the provided [PagingSource], initial
         * [PagingSource.LoadResult.Page], and [pageSize].
         *
         * This method is a convenience for:
         * ```
         * PagedList.Builder(
         *     pagingSource,
         *     page,
         *     PagedList.Config.Builder().setPageSize(pageSize).build()
         * )
         * ```
         *
         * @param pagingSource [PagingSource] the [PagedList] will load from.
         * @param initialPage Initial page loaded from the [PagingSource].
         * @param pageSize Size of loaded pages when the [PagedList] loads data from its
         * [PagingSource].
         */
        public constructor(
            pagingSource: PagingSource<Key, Value>,
            initialPage: PagingSource.LoadResult.Page<Key, Value>,
            pageSize: Int
        ) : this(
            pagingSource = pagingSource,
            initialPage = initialPage,
            config = Config(pageSize)
        )

        /**
         * Set the [CoroutineScope] that page loads should be launched within.
         *
         * The set [coroutineScope] allows a [PagingSource] to cancel running load operations when
         * the results are no longer needed - for example, when the containing Activity is
         * destroyed.
         *
         * Defaults to [GlobalScope].
         *
         * @param coroutineScope
         * @return this
         */
        public fun setCoroutineScope(
            coroutineScope: CoroutineScope
        ): Builder<Key, Value> = apply {
            this.coroutineScope = coroutineScope
        }

        /**
         * The [Executor] defining where page loading updates are dispatched.
         *
         * @param notifyExecutor [Executor] that receives [PagedList] updates, and where
         * [PagedList.Callback] calls are dispatched. Generally, this is the ui/main thread.
         * @return this
         */
        @Deprecated(
            message = "Passing an executor will cause it get wrapped as a CoroutineDispatcher, " +
                "consider passing a CoroutineDispatcher directly",
            replaceWith = ReplaceWith(
                "setNotifyDispatcher(fetchExecutor.asCoroutineDispatcher())",
                "kotlinx.coroutines.asCoroutineDispatcher"
            )
        )
        public fun setNotifyExecutor(
            notifyExecutor: Executor
        ): Builder<Key, Value> = apply {
            this.notifyDispatcher = notifyExecutor.asCoroutineDispatcher()
        }

        /**
         * The [CoroutineDispatcher] defining where page loading updates are dispatched.
         *
         * @param notifyDispatcher [CoroutineDispatcher] that receives [PagedList] updates, and
         * where [PagedList.Callback] calls are dispatched. Generally, this is the ui/main thread.
         * @return this
         */
        public fun setNotifyDispatcher(
            notifyDispatcher: CoroutineDispatcher
        ): Builder<Key, Value> = apply {
            this.notifyDispatcher = notifyDispatcher
        }

        /**
         * The [Executor] used to fetch additional pages from the [PagingSource].
         *
         * Does not affect initial load, which will be done immediately on whichever thread the
         * [PagedList] is created on.
         *
         * @param fetchExecutor [Executor] used to fetch from [PagingSource]s, generally a
         * background thread pool for e.g. I/O or network loading.
         * @return this
         */
        @Deprecated(
            message = "Passing an executor will cause it get wrapped as a CoroutineDispatcher, " +
                "consider passing a CoroutineDispatcher directly",
            replaceWith = ReplaceWith(
                "setFetchDispatcher(fetchExecutor.asCoroutineDispatcher())",
                "kotlinx.coroutines.asCoroutineDispatcher"
            )
        )
        public fun setFetchExecutor(
            fetchExecutor: Executor
        ): Builder<Key, Value> = apply {
            this.fetchDispatcher = fetchExecutor.asCoroutineDispatcher()
        }

        /**
         * The [CoroutineDispatcher] used to fetch additional pages from the [PagingSource].
         *
         * Does not affect initial load, which will be done immediately on whichever thread the
         * [PagedList] is created on.
         *
         * @param fetchDispatcher [CoroutineDispatcher] used to fetch from [PagingSource]s,
         * generally a background thread pool for e.g. I/O or network loading.
         * @return this
         */
        public fun setFetchDispatcher(
            fetchDispatcher: CoroutineDispatcher
        ): Builder<Key, Value> = apply {
            this.fetchDispatcher = fetchDispatcher
        }

        /**
         * The [BoundaryCallback] for out of data events.
         *
         * Pass a [BoundaryCallback] to listen to when the [PagedList] runs out of data to load.
         *
         * @param boundaryCallback [BoundaryCallback] for listening to out-of-data events.
         * @return this
         */
        public fun setBoundaryCallback(
            boundaryCallback: BoundaryCallback<Value>?
        ): Builder<Key, Value> = apply {
            this.boundaryCallback = boundaryCallback
        }

        /**
         * Sets the initial key the [PagingSource] should load around as part of initialization.
         *
         * @param initialKey Key the [PagingSource] should load around as part of initialization.
         * @return this
         */
        public fun setInitialKey(
            initialKey: Key?
        ): Builder<Key, Value> = apply {
            this.initialKey = initialKey
        }

        /**
         * Creates a [PagedList] with the given parameters.
         *
         * This call will dispatch the [androidx.paging.PagingSource]'s loadInitial method
         * immediately on the current thread, and block the current on the result. This method
         * should always be called on a worker thread to prevent blocking the main thread.
         *
         * It's fine to create a [PagedList] with an async [PagingSource] on the main thread, such
         * as in the constructor of a ViewModel. An async network load won't block the initial call
         * to the Load function. For a synchronous [PagingSource] such as one created from a Room
         * database, a `LiveData<PagedList>` can be safely constructed with
         * [androidx.paging.LivePagedListBuilder] on the main thread, since actual construction work
         * is deferred, and done on a background thread.
         *
         * While [build] will always return a [PagedList], it's important to note that the
         * [PagedList] initial load may fail to acquire data from the [PagingSource]. This can
         * happen for example if the [PagingSource] is invalidated during its initial load. If this
         * happens, the [PagedList] will be immediately [detached][PagedList.isDetached], and you
         * can retry construction (including setting a new [PagingSource]).
         *
         * @throws IllegalArgumentException if [notifyDispatcher] or [fetchDispatcher] are not set.
         *
         * @return The newly constructed [PagedList]
         */
        public fun build(): PagedList<Value> {
            val fetchDispatcher = fetchDispatcher ?: Dispatchers.IO
            val pagingSource = pagingSource ?: dataSource?.let { dataSource ->
                LegacyPagingSource(
                    fetchContext = fetchDispatcher,
                    dataSource = dataSource
                )
            }

            if (pagingSource is LegacyPagingSource) {
                pagingSource.setPageSize(config.pageSize)
            }

            check(pagingSource != null) {
                "PagedList cannot be built without a PagingSource or DataSource"
            }

            return create(
                pagingSource,
                initialPage,
                coroutineScope,
                notifyDispatcher ?: Dispatchers.Main.immediate,
                fetchDispatcher,
                boundaryCallback,
                config,
                initialKey
            )
        }
    }

    /**
     * Callback signaling when content is loaded into the list.
     *
     * Can be used to listen to items being paged in and out. These calls will be dispatched on
     * the dispatcher defined by [PagedList.Builder.setNotifyDispatcher], which is generally the
     * main/UI thread.
     */
    public abstract class Callback {
        /**
         * Called when null padding items have been loaded to signal newly available data, or when
         * data that hasn't been used in a while has been dropped, and swapped back to null.
         *
         * @param position Position of first newly loaded items, out of total number of items
         * (including padded nulls).
         * @param count Number of items loaded.
         */
        public abstract fun onChanged(position: Int, count: Int)

        /**
         * Called when new items have been loaded at the end or beginning of the list.
         *
         * @param position Position of the first newly loaded item (in practice, either `0` or
         * `size - 1`.
         * @param count Number of items loaded.
         */
        public abstract fun onInserted(position: Int, count: Int)

        /**
         * Called when items have been removed at the end or beginning of the list, and have not
         * been replaced by padded nulls.
         *
         * @param position Position of the first newly loaded item (in practice, either `0` or
         * `size - 1`.
         * @param count Number of items loaded.
         */
        public abstract fun onRemoved(position: Int, count: Int)
    }

    /**
     * Configures how a [PagedList] loads content from its [PagingSource].
     *
     * Use [PagedList.Config.Builder] to construct and define custom loading behavior, such as
     * [setPageSize][PagedList.Config.Builder.setPageSize], which defines number of items loaded at
     * a time.
     */
    public class Config internal constructor(
        /**
         * Size of each page loaded by the PagedList.
         */
        @JvmField
        public val pageSize: Int,
        /**
         * Prefetch distance which defines how far ahead to load.
         *
         * If this value is set to 50, the paged list will attempt to load 50 items in advance of
         * data that's already been accessed.
         *
         * @see PagedList.loadAround
         */
        @JvmField
        public val prefetchDistance: Int,
        /**
         * Defines whether the [PagedList] may display null placeholders, if the [PagingSource]
         * provides them.
         */
        @JvmField
        public val enablePlaceholders: Boolean,
        /**
         * Size hint for initial load of PagedList, often larger than a regular page.
         */
        @JvmField
        public val initialLoadSizeHint: Int,
        /**
         * Defines the maximum number of items that may be loaded into this pagedList before pages
         * should be dropped.
         *
         * If set to [PagedList.Config.Companion.MAX_SIZE_UNBOUNDED], pages will never be dropped.
         *
         * @see PagedList.Config.Companion.MAX_SIZE_UNBOUNDED
         * @see PagedList.Config.Builder.setMaxSize
         */
        @JvmField
        public val maxSize: Int
    ) {
        /**
         * Builder class for [PagedList.Config].
         *
         * You must at minimum specify page size with [setPageSize].
         */
        public class Builder {
            private var pageSize = -1
            private var prefetchDistance = -1
            private var initialLoadSizeHint = -1
            private var enablePlaceholders = true
            private var maxSize = MAX_SIZE_UNBOUNDED

            /**
             * Defines the number of items loaded at once from the [PagingSource].
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
             * @param pageSize Number of items loaded at once from the [PagingSource].
             * @return this
             *
             * @throws IllegalArgumentException if pageSize is < `1`.
             */
            public fun setPageSize(
                @IntRange(from = 1) pageSize: Int
            ): Builder = apply {
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
            public fun setPrefetchDistance(
                @IntRange(from = 0) prefetchDistance: Int
            ): Builder = apply {
                this.prefetchDistance = prefetchDistance
            }

            /**
             * Pass false to disable null placeholders in [PagedList]s using this [PagedList.Config].
             *
             * If not set, defaults to true.
             *
             * A [PagedList] will present null placeholders for not-yet-loaded content if two
             * conditions are met:
             *
             * 1) Its [PagingSource] can count all unloaded items (so that the number of nulls to
             * present is known).
             *
             * 2) placeholders are not disabled on the [PagedList.Config].
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
            public fun setEnablePlaceholders(
                enablePlaceholders: Boolean
            ): Builder = apply {
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
            public fun setInitialLoadSizeHint(
                @IntRange(from = 1) initialLoadSizeHint: Int
            ): Builder = apply {
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
             * a [PagingSource] may not be held strictly to
             * [requested pageSize][PagedList.Config.pageSize], so two pages may be larger than
             * expected.
             *  * Pages are never dropped if they are within a prefetch window (defined to be
             * `pageSize + (2 * prefetchDistance)`) of the most recent load.
             *
             * If not set, defaults to [PagedList.Config.Companion.MAX_SIZE_UNBOUNDED], which
             * disables page dropping.
             *
             * @param maxSize Maximum number of items to keep in memory, or
             * [PagedList.Config.Companion.MAX_SIZE_UNBOUNDED] to disable page dropping.
             * @return this
             *
             * @see Config.MAX_SIZE_UNBOUNDED
             * @see Config.maxSize
             */
            public fun setMaxSize(@IntRange(from = 2) maxSize: Int): Builder = apply {
                this.maxSize = maxSize
            }

            /**
             * Creates a [PagedList.Config] with the given parameters.
             *
             * @return A new [PagedList.Config].
             *
             * @throws IllegalArgumentException if placeholders are disabled and prefetchDistance
             * is set to 0
             * @throws IllegalArgumentException if maximum size is less than pageSize +
             * 2*prefetchDistance
             */
            public fun build(): Config {
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
            @Suppress("MinMaxConstant")
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
     * [BoundaryCallback] does this signaling - when a [PagingSource] runs out of data at the end of
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
     * ### Requesting Network Data
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
     * disabled, or the [PagingSource] may not count total number of items.
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
     * @param T Type loaded by the [PagedList].
     */
    @MainThread
    public abstract class BoundaryCallback<T : Any> {
        /**
         * Called when zero items are returned from an initial load of the PagedList's data source.
         */
        public open fun onZeroItemsLoaded() {}

        /**
         * Called when the item at the front of the PagedList has been loaded, and access has
         * occurred within [PagedList.Config.prefetchDistance] of it.
         *
         * No more data will be prepended to the PagedList before this item.
         *
         * @param itemAtFront The first item of PagedList
         */
        public open fun onItemAtFrontLoaded(itemAtFront: T) {}

        /**
         * Called when the item at the end of the PagedList has been loaded, and access has
         * occurred within [PagedList.Config.prefetchDistance] of it.
         *
         * No more data will be appended to the [PagedList] after this item.
         *
         * @param itemAtEnd The first item of [PagedList]
         */
        public open fun onItemAtEndLoaded(itemAtEnd: T) {}
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract class LoadStateManager {
        public var refreshState: LoadState = LoadState.NotLoading.Incomplete
        public var startState: LoadState = LoadState.NotLoading.Incomplete
        public var endState: LoadState = LoadState.NotLoading.Incomplete

        public fun setState(type: LoadType, state: LoadState) {
            // deduplicate signals
            when (type) {
                LoadType.REFRESH -> {
                    if (refreshState == state) return
                    refreshState = state
                }
                LoadType.PREPEND -> {
                    if (startState == state) return
                    startState = state
                }
                LoadType.APPEND -> {
                    if (endState == state) return
                    endState = state
                }
            }

            onStateChanged(type, state)
        }

        /**
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // protected otherwise.
        public abstract fun onStateChanged(type: LoadType, state: LoadState)

        public fun dispatchCurrentLoadState(callback: (LoadType, LoadState) -> Unit) {
            callback(LoadType.REFRESH, refreshState)
            callback(LoadType.PREPEND, startState)
            callback(LoadType.APPEND, endState)
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // protected otherwise
    public fun getNullPaddedList(): NullPaddedList<T> = storage

    internal var refreshRetryCallback: Runnable? = null

    /**
     * Last access location in list.
     *
     * Used by list diffing to re-initialize loading near viewport.
     *
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun lastLoad(): Int = storage.lastLoadAroundIndex

    internal val requiredRemainder = config.prefetchDistance * 2 + config.pageSize

    private val callbacks = mutableListOf<WeakReference<Callback>>()

    private val loadStateListeners = mutableListOf<WeakReference<(LoadType, LoadState) -> Unit>>()

    /**
     * Size of the list, including any placeholders (not-yet-loaded null padding).
     *
     * To get the number of loaded items, not counting placeholders, use [loadedCount].
     *
     * @see loadedCount
     */
    override val size: Int
        get() = storage.size

    /**
     * @throws IllegalStateException if this [PagedList] was instantiated without a
     * wrapping a backing [DataSource]
     */
    @Deprecated(
        message = "DataSource is deprecated and has been replaced by PagingSource. PagedList " +
            "offers indirect ways of controlling fetch ('loadAround()', 'retry()') so that " +
            "you should not need to access the DataSource/PagingSource."
    )
    public val dataSource: DataSource<*, T>
        @Suppress("DocumentExceptions")
        get() {
            val pagingSource = pagingSource
            @Suppress("UNCHECKED_CAST")
            if (pagingSource is LegacyPagingSource<*, *>) {
                return pagingSource.dataSource as DataSource<*, T>
            }
            throw IllegalStateException(
                "Attempt to access dataSource on a PagedList that was instantiated with a " +
                    "${pagingSource::class.java.simpleName} instead of a DataSource"
            )
        }

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
    public abstract val lastKey: Any?

    /**
     * True if the [PagedList] has detached the [PagingSource] it was loading from, and will no
     * longer load new data.
     *
     * A detached list is [immutable][isImmutable].
     *
     * @return `true` if the data source is detached.
     */
    public abstract val isDetached: Boolean

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract fun dispatchCurrentLoadState(callback: (LoadType, LoadState) -> Unit)

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract fun loadAroundInternal(index: Int)

    /**
     * Detach the [PagedList] from its [PagingSource], and attempt to load no more data.
     *
     * This is called automatically when a [PagingSource] is observed to be invalid, which is a
     * signal to stop loading. The [PagedList] will continue to present existing data, but will not
     * initiate new loads.
     */
    public abstract fun detach()

    /**
     * Returns the number of items loaded in the [PagedList].
     *
     * Unlike [size] this counts only loaded items, not placeholders.
     *
     * If placeholders are [disabled][PagedList.Config.enablePlaceholders], this method is
     * equivalent to [size].
     *
     * @return Number of items currently loaded, not counting placeholders.
     *
     * @see size
     */
    public val loadedCount: Int
        get() = storage.storageCount

    /**
     * Returns whether the list is immutable.
     *
     * Immutable lists may not become mutable again, and may safely be accessed from any thread.
     *
     * In the future, this method may return true when a PagedList has completed loading from its
     * [PagingSource]. Currently, it is equivalent to [isDetached].
     *
     * @return `true` if the [PagedList] is immutable.
     */
    public open val isImmutable: Boolean
        get() = isDetached

    /**
     * Position offset of the data in the list.
     *
     * If the PagingSource backing this PagedList is counted, the item returned from `get(i)` has
     * a position in the original data set of `i + getPositionOffset()`.
     *
     * If placeholders are enabled, this value is always `0`, since `get(i)` will return either
     * the data in its original index, or null if it is not loaded.
     */
    public val positionOffset: Int
        get() = storage.positionOffset

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setInitialLoadState(loadType: LoadType, loadState: LoadState) {
    }

    /**
     * Retry any errors associated with this [PagedList].
     *
     * If for example a network [PagingSource] append timed out, calling this method will retry the
     * failed append load.
     *
     * You can observe loading state via [addWeakLoadStateListener], though generally this is done
     * through the [PagedListAdapter][androidx.paging.PagedListAdapter] or
     * [AsyncPagedListDiffer][androidx.paging.AsyncPagedListDiffer].
     *
     * @see addWeakLoadStateListener
     * @see removeWeakLoadStateListener
     */
    public open fun retry() {}

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun setRetryCallback(refreshRetryCallback: Runnable?) {
        this.refreshRetryCallback = refreshRetryCallback
    }

    internal fun dispatchStateChangeAsync(type: LoadType, state: LoadState) {
        coroutineScope.launch(notifyDispatcher) {
            loadStateListeners.removeAll { it.get() == null }
            loadStateListeners.forEach { it.get()?.invoke(type, state) }
        }
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
    public override fun get(index: Int): T? = storage[index]

    /**
     * Load adjacent items to passed index.
     *
     * @param index Index at which to load.
     *
     * @throws IndexOutOfBoundsException if index is not within bounds.
     */
    public fun loadAround(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
        storage.lastLoadAroundIndex = index
        loadAroundInternal(index)
    }

    /**
     * Returns an immutable snapshot of the [PagedList] in its current state.
     *
     * If this [PagedList] [is immutable][isImmutable] due to its [PagingSource] being invalid, it
     * will be returned.
     *
     * @return Immutable snapshot of [PagedList] data.
     */
    public fun snapshot(): List<T> = when {
        isImmutable -> this
        else -> SnapshotPagedList(this)
    }

    /**
     * Add a listener to observe the loading state of the [PagedList].
     *
     * @param listener Listener to receive updates.
     *
     * @see removeWeakLoadStateListener
     */
    public fun addWeakLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        // Clean up any empty weak refs.
        loadStateListeners.removeAll { it.get() == null }

        // Add the new one.
        loadStateListeners.add(WeakReference(listener))
        dispatchCurrentLoadState(listener)
    }

    /**
     * Remove a previously registered load state listener.
     *
     * @param listener Previously registered listener.
     *
     * @see addWeakLoadStateListener
     */
    public fun removeWeakLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
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
     * reference to its observer, such as a [PagedListAdapter][androidx.paging.PagedListAdapter].
     * If an adapter were held with a strong reference, it would be necessary to clear its
     * [PagedList] observer before it could be GC'd.
     *
     * @param previousSnapshot Snapshot previously captured from this List, or `null`.
     * @param callback [PagedList.Callback] to dispatch to.
     *
     * @see removeWeakCallback
     */
    @Deprecated(
        "Dispatching a diff since snapshot created is behavior that can be instead " +
            "tracked by attaching a Callback to the PagedList that is mutating, and tracking " +
            "changes since calling PagedList.snapshot()."
    )
    public fun addWeakCallback(previousSnapshot: List<T>?, callback: Callback) {
        if (previousSnapshot != null && previousSnapshot !== this) {
            dispatchNaiveUpdatesSinceSnapshot(size, previousSnapshot.size, callback)
        }
        addWeakCallback(callback)
    }

    /**
     * Adds a callback.
     *
     * The callback is internally held as weak reference, so [PagedList] doesn't hold a strong
     * reference to its observer, such as a [androidx.paging.PagedListAdapter]. If an adapter were
     * held with a strong reference, it would be necessary to clear its [PagedList] observer before
     * it could be GC'd.
     *
     * @param callback Callback to dispatch to.
     *
     * @see removeWeakCallback
     */
    @Suppress("RegistrationName")
    public fun addWeakCallback(callback: Callback) {
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
    @Suppress("RegistrationName")
    public fun removeWeakCallback(callback: Callback) {
        callbacks.removeAll { it.get() == null || it.get() === callback }
    }

    internal fun notifyInserted(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onInserted(position, count) }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun notifyChanged(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onChanged(position, count) }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun notifyRemoved(position: Int, count: Int) {
        if (count == 0) return
        callbacks.reversed().forEach { it.get()?.onRemoved(position, count) }
    }
}

/**
 * Constructs a [PagedList], convenience for [PagedList.Builder].
 *
 * @param Key Type of key used to load data from the [DataSource].
 * @param Value Type of items held and loaded by the [PagedList].
 * @param dataSource [DataSource] the [PagedList] will load from.
 * @param config Config that defines how the [PagedList] loads data from its [DataSource].
 * @param notifyExecutor [Executor] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI/main thread.
 * @param fetchExecutor [Executor] used to fetch from [DataSource]s, generally a background thread
 * pool for e.g. I/O or network loading.
 * @param boundaryCallback [PagedList.BoundaryCallback] for listening to out-of-data events.
 * @param initialKey [Key] the [DataSource] should load around as part of initialization.
 */
@Suppress(
    "FunctionName",
    "DEPRECATION"
)
@JvmSynthetic
@Deprecated("DataSource is deprecated and has been replaced by PagingSource")
public fun <Key : Any, Value : Any> PagedList(
    dataSource: DataSource<Key, Value>,
    config: PagedList.Config,
    notifyExecutor: Executor,
    fetchExecutor: Executor,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    initialKey: Key? = null
): PagedList<Value> {
    return PagedList.Builder(dataSource, config)
        .setNotifyExecutor(notifyExecutor)
        .setFetchExecutor(fetchExecutor)
        .setBoundaryCallback(boundaryCallback)
        .setInitialKey(initialKey)
        .build()
}
