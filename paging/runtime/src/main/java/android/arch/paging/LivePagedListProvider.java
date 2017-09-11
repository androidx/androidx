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

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.ComputableLiveData;
import android.arch.lifecycle.LiveData;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

/**
 * Provides a {@code LiveData<PagedList>}, given a means to construct a DataSource.
 * <p>
 * Return type for data-loading system of an application or library to produce a
 * {@code LiveData<PagedList>}, while leaving the details of the paging mechanism up to the
 * consumer.
 *
 * @param <Key> Tyep of input valued used to load data from the DataSource. Must be integer if
 *             you're using TiledDataSource.
 * @param <Value> Data type produced by the DataSource, and held by the PagedLists.
 *
 * @see DataSource
 * @see PagedList
 */
public abstract class LivePagedListProvider<Key, Value> {

    /**
     * Construct a new data source to be wrapped in a new PagedList, which will be returned
     * through the LiveData.
     *
     * @return The data source.
     */
    @WorkerThread
    protected abstract DataSource<Key, Value> createDataSource();

    /**
     * Creates a LiveData of PagedLists, given the page size.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapter} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param initialLoadKey Initial key used to load initial data from the data source.
     * @param pageSize Page size defining how many items are loaded from a data source at a time.
     *                 Recommended to be multiple times the size of item displayed at once.
     *
     * @return The LiveData of PagedLists.
     */
    public LiveData<PagedList<Value>> create(@Nullable Key initialLoadKey, int pageSize) {
        return create(initialLoadKey,
                new PagedList.Config.Builder()
                        .setPageSize(pageSize)
                        .build());
    }

    /**
     * Creates a LiveData of PagedLists, given the PagedList.Config.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapter} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param initialLoadKey Initial key to pass to the data source to initialize data with.
     * @param config PagedList.Config to use with created PagedLists. This specifies how the
     *               lists will load data.
     *
     * @return The LiveData of PagedLists.
     */
    public LiveData<PagedList<Value>> create(@Nullable final Key initialLoadKey,
            final PagedList.Config config) {
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

                    mDataSource = createDataSource();
                    mDataSource.addInvalidatedCallback(mCallback);

                    mList = new PagedList.Builder<Key, Value>()
                            .setDataSource(mDataSource)
                            .setMainThreadExecutor(AppToolkitTaskExecutor.getMainThreadExecutor())
                            .setBackgroundThreadExecutor(
                                    AppToolkitTaskExecutor.getIOThreadExecutor())
                            .setConfig(config)
                            .setInitialKey(initializeKey)
                            .build();
                } while (mList.isDetached());
                return mList;
            }
        }.getLiveData();
    }
}
