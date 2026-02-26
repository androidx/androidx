/*
 * Copyright 2020 The Android Open Source Project
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

import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.flow.Flow

/**
 * Primary entry point into Paging; constructor for a reactive stream of [PagingData]. The same
 * Pager instance should be reused within an instance of ViewModel. For example in your ViewModel:
 * ```
 * // create a Pager instance and store to a variable
 * val pager = Pager(
 *      ...
 *      )
 *      .flow
 *      .cachedIn(viewModelScope)
 * ```
 *
 * Each [PagingData] represents a snapshot of the backing paginated data. Updates to the backing
 * dataset should be represented by a new instance of [PagingData].
 *
 * [PagingSource.invalidate] and calls to [AsyncPagingDataDiffer.refresh] or
 * [PagingDataAdapter.refresh] will notify [Pager] that the backing dataset has been updated and a
 * new [PagingData] / [PagingSource] pair will be generated to represent an updated snapshot.
 *
 * [PagingData] can be transformed to alter data as it loads, and presented in a `RecyclerView` via
 * `AsyncPagingDataDiffer` or `PagingDataAdapter`.
 *
 * LiveData support is available as an extension property provided by the
 * `androidx.paging:paging-runtime` artifact.
 *
 * RxJava support is available as extension properties provided by the
 * `androidx.paging:paging-rxjava2` artifact.
 */
public class Pager<Key : Any, Value : Any>
// Experimental usage is propagated to public API via constructor argument.
@ExperimentalPagingApi
constructor(
    config: PagingConfig,
    initialKey: Key? = null,
    remoteMediator: RemoteMediator<Key, Value>?,
    pagingSourceFactory: () -> PagingSource<Key, Value>,
) {
    // Experimental usage is internal, so opt-in is allowed here.
    @JvmOverloads
    @OptIn(ExperimentalPagingApi::class)
    public constructor(
        config: PagingConfig,
        initialKey: Key? = null,
        pagingSourceFactory: () -> PagingSource<Key, Value>,
    ) : this(config, initialKey, null, pagingSourceFactory)

    @OptIn(ExperimentalPagingApi::class)
    private val pageFetcher =
        PageFetcher(
            pagingSourceFactory =
                if ((pagingSourceFactory as Any) is SuspendingPagingSourceFactory<*, *>) {
                    @Suppress("CAST_NEVER_SUCCEEDS")
                    (pagingSourceFactory as SuspendingPagingSourceFactory<Key, Value>)::create
                } else {
                    // cannot pass it as is since it is not a suspend function. Hence, we wrap
                    // it in {}
                    // which means we are calling the original factory inside a suspend function
                    { pagingSourceFactory() }
                },
            initialKey = initialKey,
            config = config,
            remoteMediator = remoteMediator,
        )

    /**
     * A cold [Flow] of [PagingData], which emits new instances of [PagingData] once they become
     * invalidated by [PagingSource.invalidate] or calls to [AsyncPagingDataDiffer.refresh] or
     * [PagingDataAdapter.refresh].
     *
     * To consume this stream as a LiveData or in Rx, you may use the extensions available in the
     * paging-runtime or paging-rxjava* artifacts.
     *
     * NOTE: Instances of [PagingData] emitted by this [Flow] are not re-usable and cannot be
     * submitted multiple times. This is especially relevant for transforms such as
     * [Flow.combine][kotlinx.coroutines.flow.combine], which would replay the latest value
     * downstream. To ensure you get a new instance of [PagingData] for each downstream observer,
     * you should use the [cachedIn] operator which multicasts the [Flow] in a way that returns a
     * new instance of [PagingData] with cached data pre-loaded.
     */
    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    public val flow: Flow<PagingData<Value>> = pageFetcher.flow

    /**
     * Loads a page at the end of current loaded data. Provides a way to manually trigger appends
     * without scrolling.
     *
     * This function is no-op if there are no more data to be loaded from the datasource (i.e.
     * append is [LoadState.NotLoading.Complete])
     *
     * Appends that are triggered by scrolling (i.e. if user scrolls to end of the list and triggers
     * [PagingConfig.prefetchDistance]) takes precedence over this append. This ensures that items
     * that are currently accessed gets loaded in first.
     */
    public fun append() {
        pageFetcher.load(LoadType.APPEND)
    }

    /**
     * Loads a page at the start of current loaded data. Provides a way to manually trigger prepends
     * without scrolling.
     *
     * This function is no-op if there are no more data to be loaded from the datasource (i.e.
     * prepend is [LoadState.NotLoading.Complete])
     *
     * Prepends that are triggered by scrolling (i.e. if user scrolls to start of the list and
     * triggers [PagingConfig.prefetchDistance]) takes precedence over this prepend. This ensures
     * that items that are currently accessed gets loaded in first.
     */
    public fun prepend() {
        pageFetcher.load(LoadType.PREPEND)
    }

    /**
     * Refresh all currently loaded data.
     *
     * The refresh key is the key that was used to load the current first loaded page, and the
     * loadSize is the size of current loaded data.
     *
     * Note that this is a best-effort attempt to reload all current data. Actual loaded data may
     * differ based on how [PagingSource.load] handles the same key for different [LoadType]. For
     * example the current first page might have been a prepended page, and [PagingSource.load]
     * could handle the same key in different ways for [LoadType.PREPEND] versus [LoadType.REFRESH]
     */
    public fun refresh() {
        pageFetcher.refreshAll()
    }

    /**
     * Refresh based on a given loaded item.
     *
     * The refresh key is the key that was originally used to the load the page that contains
     * [item], and the loadSize is [PagingConfig.initialLoadSize].
     *
     * Note that this is a best-effort attempt to reload around the given loaded item. The requested
     * [item] is expected to be in the first page of reloaded items but will likely not be the very
     * first reloaded item. For example if two pages are currently loaded with loadKeys `MyKey1` and
     * `MyKey2` respectively:
     * - pg1: MyKey1 = items [0, 1, 2, 3]
     * - pg2: MyKey2 = items [4, 5, 6, 7] If this method were called with item 5 - refresh(5) - then
     *   the refreshKey will be `MyKey2`, which will theoretically refresh starting at (item 4)
     *   until (item4 + initialLoadSize).
     */
    public fun refresh(item: Value) {
        pageFetcher.refresh(item)
    }
}
