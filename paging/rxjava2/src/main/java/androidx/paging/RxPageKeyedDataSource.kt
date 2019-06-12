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
import java.util.concurrent.Executor

internal abstract class RxPageKeyedDataSource<Key : Any, Value : Any> :
    ListenablePageKeyedDataSource<Key, Value>() {

    /**
     * Must be lazily init  as [DataSource.executor] throws an error if it is accessed before this
     * DataSource is added to a [PagedList] and assigned an [Executor].
     */
    private val scheduler: Scheduler by lazy {
        when (executor) {
            is Scheduler -> executor as Scheduler
            else -> ScheduledExecutor(executor)
        }
    }

    override fun loadInitial(params: LoadInitialParams<Key>) =
        onLoadInitial(params).asListenableFuture(executor, scheduler)

    override fun loadAfter(params: LoadParams<Key>) =
        onLoadAfter(params).asListenableFuture(executor, scheduler)

    override fun loadBefore(params: LoadParams<Key>): ListenableFuture<Result<Key, Value>> =
        onLoadBefore(params).asListenableFuture(executor, scheduler)

    /**
     * Invoked to load initial data from this DataSource as a [Single], e.g., when initializing or
     * resuming state of a [PagedList].
     *
     * Rx-extension of the parent method: [ListenablePageKeyedDataSource.loadInitial].
     *
     * The [Single] returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * [RxPagedListBuilder.setFetchScheduler] or [LivePagedListBuilder.setFetchExecutor].
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return [Single] that receives the loaded data, its size, and any adjacent page keys.
     */
    abstract fun onLoadInitial(params: LoadInitialParams<Key>): Single<InitialResult<Key, Value>>

    /**
     * Invoked to load a page of data to be appended to this DataSource as a [Single] with
     * the key specified by [LoadParams.key][PageKeyedDataSource.LoadParams.key].
     *
     * Rx-extension of the parent method: [ListenablePageKeyedDataSource.loadAfter].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [.invalidate] to invalidate the data source,
     * and prevent further loading.
     *
     * The [Single] returned by this method will be subscribed on this DataSource's executor,
     * which is normally supplied via [RxPagedListBuilder.setFetchScheduler] or
     * [LivePagedListBuilder.setFetchExecutor].
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return [Single] that receives the loaded data.
     */
    abstract fun onLoadAfter(params: LoadParams<Key>): Single<Result<Key, Value>>

    /**
     * Invoked to load a page of data to be prepended to this DataSource as a [Single] with the key
     * specified by [LoadParams.key][PageKeyedDataSource.LoadParams.key].
     *
     * Rx-extension of the parent method: [ListenablePageKeyedDataSource.loadBefore].
     *
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     *
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call [.invalidate] to invalidate the data source, and
     * prevent further loading.
     *
     * The [Single] returned by this method will be subscribed on this DataSource's executor, which
     * is normally supplied via [RxPagedListBuilder.setFetchScheduler] or
     * [LivePagedListBuilder.setFetchExecutor].
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return [Single] that receives the loaded data.
     */
    abstract fun onLoadBefore(params: LoadParams<Key>): Single<Result<Key, Value>>
}
