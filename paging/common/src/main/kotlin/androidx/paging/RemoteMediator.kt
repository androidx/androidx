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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingSource.LoadResult
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH

/**
 * Defines a set of callbacks used to incrementally load data from a remote source into a local
 * source wrapped by a [PagingSource], e.g., loading data from network into a local db cache.
 *
 * A [RemoteMediator] is registered by passing it to [Pager]'s constructor.
 *
 * [RemoteMediator] allows hooking into the following events:
 *  * Stream initialization
 *  * [REFRESH] signal driven from UI
 *  * [PagingSource] returns a [LoadResult] which signals a boundary condition, i.e., the most
 *  recent [LoadResult.Page] in the [PREPEND] or [APPEND] direction has [LoadResult.Page.prevKey]
 *  or [LoadResult.Page.nextKey] set to `null` respectively.
 *
 * @sample androidx.paging.samples.remoteMediatorItemKeyedSample
 * @sample androidx.paging.samples.remoteMediatorPageKeyedSample
 */
@ExperimentalPagingApi
public abstract class RemoteMediator<Key : Any, Value : Any> {
    /**
     * Callback triggered when Paging needs to request more data from a remote source due to any of
     * the following events:
     *  * Stream initialization if [initialize] returns [LAUNCH_INITIAL_REFRESH]
     *  * [REFRESH] signal driven from UI
     *  * [PagingSource] returns a [LoadResult] which signals a boundary condition, i.e., the most
     *  recent [LoadResult.Page] in the [PREPEND] or [APPEND] direction has
     *  [LoadResult.Page.prevKey] or [LoadResult.Page.nextKey] set to `null` respectively.
     *
     * It is the responsibility of this method to update the backing dataset and trigger
     * [PagingSource.invalidate] to allow [androidx.paging.PagingDataAdapter] to pick up new
     * items found by [load].
     *
     * The runtime and result of this method defines the remote [LoadState] behavior sent to the
     * UI via [CombinedLoadStates].
     *
     * This method is never called concurrently *unless* [Pager.flow] has multiple collectors.
     * Note that Paging might cancel calls to this function if it is currently executing a
     * [PREPEND] or [APPEND] and a [REFRESH] is requested. In that case, [REFRESH] has higher
     * priority and will be executed after the previous call is cancelled. If the [load] call with
     * [REFRESH] returns an error, Paging will call [load] with the previously cancelled [APPEND]
     * or [PREPEND] request. If [REFRESH] succeeds, it won't make the [APPEND] or [PREPEND] requests
     * unless they are necessary again after the [REFRESH] is applied to the UI.
     *
     * @param loadType [LoadType] of the condition which triggered this callback.
     *  * [PREPEND] indicates the end of pagination in the [PREPEND] direction was reached. This
     *  occurs when [PagingSource.load] returns a [LoadResult.Page] with
     *  [LoadResult.Page.prevKey] == `null`.
     *  * [APPEND] indicates the end of pagination in the [APPEND] direction was reached. This
     *  occurs when [PagingSource.load] returns a [LoadResult.Page] with
     *  [LoadResult.Page.nextKey] == `null`.
     *  * [REFRESH] indicates this method was triggered due to a requested refresh. Generally, this
     *  means that a request to load remote data and **replace** all local data was made. This can
     *  happen when:
     *    * Stream initialization if [initialize] returns [LAUNCH_INITIAL_REFRESH]
     *    * An explicit call to refresh driven by the UI
     * @param state A copy of the state including the list of pages currently held in memory of the
     * currently presented [PagingData] at the time of starting the load. E.g. for
     * load(loadType = APPEND), you can use the page or item at the end as input for what to load
     * from the network.
     *
     * @return [MediatorResult] signifying what [LoadState] to be passed to the UI, and whether
     * there's more data available.
     */
    public abstract suspend fun load(
        loadType: LoadType,
        state: PagingState<Key, Value>
    ): MediatorResult

    /**
     * Callback fired during initialization of a [PagingData] stream, before initial load.
     *
     * This function runs to completion before any loading is performed.
     *
     * @return [InitializeAction] used to control whether [load] with load type [REFRESH] will be
     * immediately dispatched when the first [PagingData] is submitted:
     *  * [LAUNCH_INITIAL_REFRESH] to immediately dispatch [load] asynchronously with load type
     *  [REFRESH], to update paginated content when the stream is initialized.
     *  Note: This also prevents [RemoteMediator] from triggering [PREPEND] or [APPEND] until
     *  [REFRESH] succeeds.
     *  * [SKIP_INITIAL_REFRESH] to wait for a refresh request from the UI before dispatching [load]
     *  asynchronously with load type [REFRESH].
     */
    public open suspend fun initialize(): InitializeAction = LAUNCH_INITIAL_REFRESH

    /**
     * Return type of [load], which determines [LoadState].
     */
    public sealed class MediatorResult {
        /**
         * Recoverable error that can be retried, sets the [LoadState] to [LoadState.Error].
         */
        public class Error(public val throwable: Throwable) : MediatorResult()

        /**
         * Success signaling that [LoadState] should be set to [LoadState.NotLoading] if
         * [endOfPaginationReached] is `true`, otherwise [LoadState] is kept at [LoadState.Loading]
         * to await invalidation.
         *
         * NOTE: It is the responsibility of [load] to update the backing dataset and trigger
         * [PagingSource.invalidate] to allow [androidx.paging.PagingDataAdapter] to pick up new
         * items found by [load].
         */
        public class Success(
            @get:JvmName("endOfPaginationReached") public val endOfPaginationReached: Boolean
        ) : MediatorResult()
    }

    /**
     * Return type of [initialize], which signals the action to take after [initialize] completes.
     */
    public enum class InitializeAction {
        /**
         * Immediately dispatch a [load] asynchronously with load type [REFRESH], to update
         * paginated content when the stream is initialized.
         *
         * Note: This also prevents [RemoteMediator] from triggering [PREPEND] or [APPEND] until
         * [REFRESH] succeeds.
         */
        LAUNCH_INITIAL_REFRESH,

        /**
         * Wait for a refresh request from the UI before dispatching [load] with load type [REFRESH]
         */
        SKIP_INITIAL_REFRESH
    }
}
