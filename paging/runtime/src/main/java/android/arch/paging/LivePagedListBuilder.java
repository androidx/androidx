/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.paging;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.lifecycle.ComputableLiveData;
import android.arch.lifecycle.LiveData;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;

/**
 * Builder for {@code LiveData<PagedList>}, given a {@link DataSource.Factory} and a
 * {@link PagedList.Config}.
 * <p>
 * The required parameters are in the constructor, so you can simply construct and build, or
 * optionally enable extra features (such as initial load key, or BoundaryCallback.
 *
 * @param <Key> Type of input valued used to load data from the DataSource. Must be integer if
 *             you're using PositionalDataSource.
 * @param <Value> Item type being presented.
 */
public class LivePagedListBuilder<Key, Value> {
    private Key mInitialLoadKey;
    private PagedList.Config mConfig;
    private DataSource.Factory<Key, Value> mDataSourceFactory;
    private PagedList.BoundaryCallback mBoundaryCallback;
    private Executor mBackgroundThreadExecutor;

    /**
     * Creates a LivePagedListBuilder with required parameters.
     *
     * @param dataSourceFactory DataSource factory providing DataSource generations.
     * @param config Paging configuration.
     */
    public LivePagedListBuilder(@NonNull DataSource.Factory<Key, Value> dataSourceFactory,
            @NonNull PagedList.Config config) {
        mDataSourceFactory = dataSourceFactory;
        mConfig = config;
    }

    /**
     * Creates a LivePagedListBuilder with required parameters.
     * <p>
     * This method is a convenience for:
     * <pre>
     * LivePagedListBuilder(dataSourceFactory,
     *         new PagedList.Config.Builder().setPageSize(pageSize).build())
     * </pre>
     *
     * @param dataSourceFactory DataSource.Factory providing DataSource generations.
     * @param pageSize Size of pages to load.
     */
    public LivePagedListBuilder(@NonNull DataSource.Factory<Key, Value> dataSourceFactory,
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
    @NonNull
    public LivePagedListBuilder<Key, Value> setInitialLoadKey(@Nullable Key key) {
        mInitialLoadKey = key;
        return this;
    }

    /**
     * Sets a {@link PagedList.BoundaryCallback} on each PagedList created.
     * <p>
     * This can be used to
     *
     * @param boundaryCallback The boundary callback for listening to PagedList load state.
     * @return this
     */
    @SuppressWarnings("unused")
    @NonNull
    public LivePagedListBuilder<Key, Value> setBoundaryCallback(
            @Nullable PagedList.BoundaryCallback<Value> boundaryCallback) {
        mBoundaryCallback = boundaryCallback;
        return this;
    }

    /**
     * Sets executor which will be used for background loading of pages.
     * <p>
     * Does not affect initial load, which will be always be done on done on the Arch components
     * I/O thread.
     *
     * @param backgroundThreadExecutor Executor for background DataSource loading.
     * @return this
     */
    @SuppressWarnings("unused")
    @NonNull
    public LivePagedListBuilder<Key, Value> setBackgroundThreadExecutor(
            @NonNull Executor backgroundThreadExecutor) {
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        return this;
    }

    /**
     * Constructs the {@code LiveData<PagedList>}.
     * <p>
     * No work (such as loading) is done immediately, the creation of the first PagedList is is
     * deferred until the LiveData is observed.
     *
     * @return The LiveData of PagedLists
     */
    @NonNull
    public LiveData<PagedList<Value>> build() {
        if (mConfig == null) {
            throw new IllegalArgumentException("PagedList.Config must be provided");
        }
        if (mDataSourceFactory == null) {
            throw new IllegalArgumentException("DataSource.Factory must be provided");
        }
        if (mBackgroundThreadExecutor == null) {
            mBackgroundThreadExecutor = ArchTaskExecutor.getIOThreadExecutor();
        }

        return create(mInitialLoadKey, mConfig, mBoundaryCallback, mDataSourceFactory,
                ArchTaskExecutor.getMainThreadExecutor(), mBackgroundThreadExecutor);
    }

    @AnyThread
    @NonNull
    private static <Key, Value> LiveData<PagedList<Value>> create(
            @Nullable final Key initialLoadKey,
            @NonNull final PagedList.Config config,
            @Nullable final PagedList.BoundaryCallback boundaryCallback,
            @NonNull final DataSource.Factory<Key, Value> dataSourceFactory,
            @NonNull final Executor mainThreadExecutor,
            @NonNull final Executor backgroundThreadExecutor) {
        return new ComputableLiveData<PagedList<Value>>() {
            @Nullable
            private PagedList<Value> mList;
            @Nullable
            private DataSource<Key, Value> mDataSource;

            private final DataSource.InvalidatedCallback mCallback =
                    new DataSource.InvalidatedCallback() {
                        @Override
                        public void onInvalidated() {
                            invalidate();
                        }
                    };

            @Override
            protected PagedList<Value> compute() {
                @Nullable Key initializeKey = initialLoadKey;
                if (mList != null) {
                    //noinspection unchecked
                    initializeKey = (Key) mList.getLastKey();
                }

                do {
                    if (mDataSource != null) {
                        mDataSource.removeInvalidatedCallback(mCallback);
                    }

                    mDataSource = dataSourceFactory.create();
                    mDataSource.addInvalidatedCallback(mCallback);

                    mList = new PagedList.Builder<>(mDataSource, config)
                            .setMainThreadExecutor(mainThreadExecutor)
                            .setBackgroundThreadExecutor(backgroundThreadExecutor)
                            .setBoundaryCallback(boundaryCallback)
                            .setInitialKey(initializeKey)
                            .build();
                } while (mList.isDetached());
                return mList;
            }
        }.getLiveData();
    }
}
