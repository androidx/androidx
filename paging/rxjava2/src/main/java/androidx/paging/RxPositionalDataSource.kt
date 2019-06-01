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

import com.google.common.util.concurrent.ListenableFuture

import io.reactivex.Scheduler
import io.reactivex.Single

internal abstract class RxPositionalDataSource<T : Any> : ListenablePositionalDataSource<T>() {
    /**
     * Must be lazily init  as [DataSource.executor] throws an error if it is accessed before this
     * DataSource is added to a [PagedList] and assigned an [java.util.concurrent.Executor].
     */
    private val scheduler: Scheduler by lazy {
        when (executor) {
            is Scheduler -> executor as Scheduler
            else -> ScheduledExecutor(executor)
        }
    }

    override fun loadInitial(params: LoadInitialParams): ListenableFuture<InitialResult<T>> =
        onLoadInitial(params).asListenableFuture(executor, scheduler)

    override fun loadRange(params: LoadRangeParams) =
        onLoadRange(params).asListenableFuture(executor, scheduler)

    /**
     * Invoked to load initial data from this DataSource as a [Single], e.g., when initializing or
     * resuming state of a [PagedList].
     *
     * Rx-extension of the parent method:
     * [ListenablePositionalDataSource.loadInitial].
     *
     * This method is called to load the initial page(s) from the DataSource.
     *
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * The [Single] returned by this method will be subscribed on this DataSource's executor, which
     * is normally supplied via [RxPagedListBuilder.setFetchScheduler] or
     * [LivePagedListBuilder.setFetchExecutor].
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return [Single] that receives the loaded data, including position and total data
     *                  set size.
     */
    abstract fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<T>>

    /**
     * Invoked to load a range of data from this DataSource as a [Single].
     *
     * Rx-extension of the parent method: [ListenablePositionalDataSource.loadRange].
     *
     * This method is called to load additional pages from the DataSource after the
     * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
     *
     * Unlike [loadInitial], this method must return the number of items requested, at the position
     * requested.
     *
     * The [Single] returned by this method will be subscribed on this DataSource's executor, which
     * is normally supplied via [RxPagedListBuilder.setFetchScheduler] or
     * [LivePagedListBuilder.setFetchExecutor].
     *
     * @param params Parameters for load, including start position and load size.
     * @return [Single] that receives the loaded data.
     */
    abstract fun onLoadRange(params: LoadRangeParams): Single<RangeResult<T>>
}
