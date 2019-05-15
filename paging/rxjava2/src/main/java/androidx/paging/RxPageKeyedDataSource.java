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

package androidx.paging;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

import io.reactivex.Scheduler;
import io.reactivex.Single;

abstract class RxPageKeyedDataSource<Key, Value> extends ListenablePageKeyedDataSource<Key, Value> {
    private Scheduler mScheduler;

    @Override
    @NonNull
    public final ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadInitial(params),
                getExecutor(), getScheduler());
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Key, Value>> loadAfter(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadAfter(params),
                getExecutor(), getScheduler());
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Key, Value>> loadBefore(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadBefore(params),
                getExecutor(), getScheduler());
    }

    /**
     * An explicit getter is necessary to lazily init mScheduler as {@link DataSource#getExecutor()}
     * throws an error if it is accessed before this DataSource is added to a {@link PagedList} and
     * assigned an {@link Executor}.
     */
    private Scheduler getScheduler() {
        if (mScheduler == null) {
            mScheduler = new ScheduledExecutor(getExecutor());
        }

        return mScheduler;
    }

    /**
     * Invoked to load initial data from this DataSource as a {@link Single}, e.g., when
     * initializing or resuming state of a {@link PagedList}.
     *
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadInitial(
     *ListenablePageKeyedDataSource.LoadInitialParams)}.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return {@link Single} that receives the loaded data, its size, and any adjacent page keys.
     */
    @NonNull
    public abstract Single<InitialResult<Key, Value>> onLoadInitial(LoadInitialParams<Key> params);

    /**
     * Invoked to load a page of data to be appended to this DataSource as a {@link Single} with
     * the key specified by {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     *
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadAfter(ListenablePageKeyedDataSource.LoadParams)}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Key, Value>> onLoadAfter(LoadParams<Key> params);

    /**
     * Invoked to load a page of data to be prepended to this DataSource as a {@link Single} with
     * the key specified by {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     *
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadBefore(ListenablePageKeyedDataSource.LoadParams)}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Key, Value>> onLoadBefore(LoadParams<Key> params);
}
