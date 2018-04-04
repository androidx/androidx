/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;

import java.util.concurrent.Executor;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;

/**
 * Builder for {@code Observable<PagedList>} or {@code Flowable<PagedList>}, given a
 * {@link DataSource.Factory} and a {@link PagedList.Config}.
 * <p>
 * The required parameters are in the constructor, so you can simply construct and build, or
 * optionally enable extra features (such as initial load key, or BoundaryCallback).
 * <p>
 * The returned observable/flowable will already be subscribed on the
 * {@link #setFetchScheduler(Scheduler)}, and will perform all loading on that scheduler. It will
 * already be observed on {@link #setNotifyScheduler(Scheduler)}, and will dispatch new PagedLists,
 * as well as their updates to that scheduler.
 *
 * @param <Key> Type of input valued used to load data from the DataSource. Must be integer if
 *             you're using PositionalDataSource.
 * @param <Value> Item type being presented.
 */
public final class RxPagedListBuilder<Key, Value> {
    private Key mInitialLoadKey;
    private PagedList.Config mConfig;
    private DataSource.Factory<Key, Value> mDataSourceFactory;
    private PagedList.BoundaryCallback mBoundaryCallback;
    private Executor mNotifyExecutor;
    private Executor mFetchExecutor;
    private Scheduler mFetchScheduler;
    private Scheduler mNotifyScheduler;

    /**
     * Creates a RxPagedListBuilder with required parameters.
     *
     * @param dataSourceFactory DataSource factory providing DataSource generations.
     * @param config Paging configuration.
     */
    public RxPagedListBuilder(@NonNull DataSource.Factory<Key, Value> dataSourceFactory,
            @NonNull PagedList.Config config) {
        //noinspection ConstantConditions
        if (config == null) {
            throw new IllegalArgumentException("PagedList.Config must be provided");
        }
        //noinspection ConstantConditions
        if (dataSourceFactory == null) {
            throw new IllegalArgumentException("DataSource.Factory must be provided");
        }
        mDataSourceFactory = dataSourceFactory;
        mConfig = config;
    }

    /**
     * Creates a RxPagedListBuilder with required parameters.
     * <p>
     * This method is a convenience for:
     * <pre>
     * RxPagedListBuilder(dataSourceFactory,
     *         new PagedList.Config.Builder().setPageSize(pageSize).build())
     * </pre>
     *
     * @param dataSourceFactory DataSource.Factory providing DataSource generations.
     * @param pageSize Size of pages to load.
     */
    @SuppressWarnings("unused")
    public RxPagedListBuilder(@NonNull DataSource.Factory<Key, Value> dataSourceFactory,
            int pageSize) {
        this(dataSourceFactory, new PagedList.Config.Builder().setPageSize(pageSize).build());
    }

    /**
     * First loading key passed to the first PagedList/DataSource.
     * <p>
     * When a new PagedList/DataSource pair is created after the first, it acquires a load key from
     * the previous generation so that data is loaded around the position already being observed.
     *
     * @param key Initial load key passed to the first PagedList/DataSource.
     * @return this
     */
    @SuppressWarnings("unused")
    @NonNull
    public RxPagedListBuilder<Key, Value> setInitialLoadKey(@Nullable Key key) {
        mInitialLoadKey = key;
        return this;
    }

    /**
     * Sets a {@link PagedList.BoundaryCallback} on each PagedList created, typically used to load
     * additional data from network when paging from local storage.
     * <p>
     * Pass a BoundaryCallback to listen to when the PagedList runs out of data to load. If this
     * method is not called, or {@code null} is passed, you will not be notified when each
     * DataSource runs out of data to provide to its PagedList.
     * <p>
     * If you are paging from a DataSource.Factory backed by local storage, you can set a
     * BoundaryCallback to know when there is no more information to page from local storage.
     * This is useful to page from the network when local storage is a cache of network data.
     * <p>
     * Note that when using a BoundaryCallback with a {@code Observable<PagedList>}, method calls
     * on the callback may be dispatched multiple times - one for each PagedList/DataSource
     * pair. If loading network data from a BoundaryCallback, you should prevent multiple
     * dispatches of the same method from triggering multiple simultaneous network loads.
     *
     * @param boundaryCallback The boundary callback for listening to PagedList load state.
     * @return this
     */
    @SuppressWarnings("unused")
    @NonNull
    public RxPagedListBuilder<Key, Value> setBoundaryCallback(
            @Nullable PagedList.BoundaryCallback<Value> boundaryCallback) {
        mBoundaryCallback = boundaryCallback;
        return this;
    }

    /**
     * Sets scheduler which will be used for observing new PagedLists, as well as loading updates
     * within the PagedLists.
     * <p>
     * The built observable will be {@link Observable#observeOn(Scheduler) observed on} this
     * scheduler, so that the thread receiving PagedLists will also receive the internal updates to
     * the PagedList.
     *
     * @param scheduler Scheduler for background DataSource loading.
     * @return this
     */
    public RxPagedListBuilder<Key, Value> setNotifyScheduler(
            final @NonNull Scheduler scheduler) {
        mNotifyScheduler = scheduler;
        final Scheduler.Worker worker = scheduler.createWorker();
        mNotifyExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                // We use a worker here since the page load notifications
                // should not be dispatched in parallel
                worker.schedule(command);
            }
        };
        return this;
    }

    /**
     * Sets scheduler which will be used for background fetching of PagedLists, as well as on-demand
     * fetching of pages inside.
     *
     * @param scheduler Scheduler for background DataSource loading.
     * @return this
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    @NonNull
    public RxPagedListBuilder<Key, Value> setFetchScheduler(
            final @NonNull Scheduler scheduler) {
        mFetchExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                // We use scheduleDirect since the page loads that use
                // executor are intentionally parallel.
                scheduler.scheduleDirect(command);
            }
        };
        mFetchScheduler = scheduler;
        return this;
    }

    /**
     * Constructs a {@code Observable<PagedList>}.
     * <p>
     * The returned Observable will already be observed on the
     * {@link #setNotifyScheduler(Scheduler) notify scheduler}, and subscribed on the
     * {@link #setFetchScheduler(Scheduler) fetch scheduler}.
     *
     * @return The Observable of PagedLists
     */
    @NonNull
    public Observable<PagedList<Value>> buildObservable() {
        if (mNotifyExecutor == null) {
            mNotifyExecutor = ArchTaskExecutor.getMainThreadExecutor();
            mNotifyScheduler = Schedulers.from(mNotifyExecutor);
        }
        if (mFetchExecutor == null) {
            mFetchExecutor = ArchTaskExecutor.getIOThreadExecutor();
            mFetchScheduler = Schedulers.from(mFetchExecutor);
        }
        return Observable.create(new PagingObservableOnSubscribe<>(
                mInitialLoadKey,
                mConfig,
                mBoundaryCallback,
                mDataSourceFactory,
                mNotifyExecutor,
                mFetchExecutor))
                        .observeOn(mNotifyScheduler)
                        .subscribeOn(mFetchScheduler);
    }

    /**
     * Constructs a {@code Flowable<PagedList>}.
     *
     * The returned Observable will already be observed on the
     * {@link #setNotifyScheduler(Scheduler) notify scheduler}, and subscribed on the
     * {@link #setFetchScheduler(Scheduler) fetch scheduler}.
     *
     * @param backpressureStrategy BackpressureStrategy for the Flowable to use.
     * @return The Flowable of PagedLists
     */
    @NonNull
    public Flowable<PagedList<Value>> buildFlowable(BackpressureStrategy backpressureStrategy) {
        return buildObservable()
                .toFlowable(backpressureStrategy);
    }

    static class PagingObservableOnSubscribe<Key, Value>
            implements ObservableOnSubscribe<PagedList<Value>>, DataSource.InvalidatedCallback,
            Cancellable,
            Runnable {

        @Nullable
        private final Key mInitialLoadKey;
        @NonNull
        private final PagedList.Config mConfig;
        @Nullable
        private final PagedList.BoundaryCallback mBoundaryCallback;
        @NonNull
        private final DataSource.Factory<Key, Value> mDataSourceFactory;
        @NonNull
        private final Executor mNotifyExecutor;
        @NonNull
        private final Executor mFetchExecutor;

        @Nullable
        private PagedList<Value> mList;
        @Nullable
        private DataSource<Key, Value> mDataSource;

        private ObservableEmitter<PagedList<Value>> mEmitter;

        private PagingObservableOnSubscribe(@Nullable Key initialLoadKey,
                @NonNull PagedList.Config config,
                @Nullable PagedList.BoundaryCallback boundaryCallback,
                @NonNull DataSource.Factory<Key, Value> dataSourceFactory,
                @NonNull Executor notifyExecutor,
                @NonNull Executor fetchExecutor) {
            mInitialLoadKey = initialLoadKey;
            mConfig = config;
            mBoundaryCallback = boundaryCallback;
            mDataSourceFactory = dataSourceFactory;
            mNotifyExecutor = notifyExecutor;
            mFetchExecutor = fetchExecutor;
        }

        @Override
        public void subscribe(ObservableEmitter<PagedList<Value>> emitter)
                throws Exception {
            mEmitter = emitter;
            mEmitter.setCancellable(this);

            // known that subscribe is already on fetchScheduler
            mEmitter.onNext(createPagedList());
        }

        @Override
        public void cancel() throws Exception {
            if (mDataSource != null) {
                mDataSource.removeInvalidatedCallback(this);
            }
        }

        @Override
        public void run() {
            // fetch data, run on fetchExecutor
            mEmitter.onNext(createPagedList());
        }

        @Override
        public void onInvalidated() {
            if (!mEmitter.isDisposed()) {
                mFetchExecutor.execute(this);
            }
        }

        private PagedList<Value> createPagedList() {
            @Nullable Key initializeKey = mInitialLoadKey;
            if (mList != null) {
                //noinspection unchecked
                initializeKey = (Key) mList.getLastKey();
            }

            do {
                if (mDataSource != null) {
                    mDataSource.removeInvalidatedCallback(this);
                }
                mDataSource = mDataSourceFactory.create();
                mDataSource.addInvalidatedCallback(this);

                mList = new PagedList.Builder<>(mDataSource, mConfig)
                        .setNotifyExecutor(mNotifyExecutor)
                        .setFetchExecutor(mFetchExecutor)
                        .setBoundaryCallback(mBoundaryCallback)
                        .setInitialKey(initializeKey)
                        .build();
            } while (mList.isDetached());
            return mList;
        }
    }
}
