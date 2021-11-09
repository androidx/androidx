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

package androidx.paging.rxjava2

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.RemoteMediator.MediatorResult
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

/**
 * RxJava2 compatibility wrapper around [RemoteMediator]'s suspending APIs.
 */
@ExperimentalPagingApi
abstract class RxRemoteMediator<Key : Any, Value : Any> : RemoteMediator<Key, Value>() {
    /**
     * Implement this method to load additional remote data, which will then be stored for the
     * [PagingSource] to access. These loads take one of two forms:
     *  * type == [LoadType.PREPEND] / [LoadType.APPEND]
     *  The [PagingSource] has loaded a 'boundary' page, with a `null` adjacent key. This means
     *  this method should load additional remote data to append / prepend as appropriate, and store
     *  it locally.
     *  * type == [LoadType.REFRESH]
     *  The app (or [initialize]) has requested a remote refresh of data. This means the method
     *  should generally load remote data, and **replace** all local data.
     *
     * The runtime of this method defines loading state behavior in boundary conditions, which
     * affects e.g., [LoadState] callbacks registered to [androidx.paging.PagingDataAdapter].
     *
     * NOTE: A [PagingSource.load] request which is fulfilled by a page that hits a boundary
     * condition in either direction will trigger this callback with [LoadType.PREPEND] or
     * [LoadType.APPEND] or both. [LoadType.REFRESH] occurs as a result of [initialize].
     *
     * @param loadType [LoadType] of the boundary condition which triggered this callback.
     *  * [LoadType.PREPEND] indicates a boundary condition at the front of the list.
     *  * [LoadType.APPEND] indicates a boundary condition at the end of the list.
     *  * [LoadType.REFRESH] indicates this callback was triggered as the result of a requested
     *  refresh - either driven by the UI, or by [initialize].
     * @param state A copy of the state including the list of pages currently held in
     * memory of the currently presented [PagingData] at the time of starting the load. E.g. for
     * load(loadType = END), you can use the page or item at the end as input for what to load from
     * the network.
     *
     * @return [MediatorResult] signifying what [LoadState] to be passed to the UI, and whether
     * there's more data available.
     */
    abstract fun loadSingle(
        loadType: LoadType,
        state: PagingState<Key, Value>
    ): Single<MediatorResult>

    /**
     * Callback fired during initialization of a [PagingData] stream, before initial load.
     *
     * This function runs to completion before any loading is performed.
     *
     * @return [InitializeAction] indicating the action to take after initialization:
     *  * [LAUNCH_INITIAL_REFRESH] to immediately dispatch a [load] asynchronously with load type
     *  [LoadType.REFRESH], to update paginated content when the stream is initialized.
     *  Note: This also prevents [RemoteMediator] from triggering [PREPEND] or [APPEND] until
     *  [REFRESH] succeeds.
     *  * [SKIP_INITIAL_REFRESH][InitializeAction.SKIP_INITIAL_REFRESH] to wait for a
     *  refresh request from the UI before dispatching a [load] with load type [LoadType.REFRESH].
     */
    open fun initializeSingle(): Single<InitializeAction> = Single.just(LAUNCH_INITIAL_REFRESH)

    final override suspend fun load(loadType: LoadType, state: PagingState<Key, Value>):
        MediatorResult {
            return loadSingle(loadType, state).await()
        }

    final override suspend fun initialize(): InitializeAction {
        return initializeSingle().await()
    }
}