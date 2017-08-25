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

package android.arch.util.paging;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.ComputableLiveData;
import android.arch.lifecycle.LiveData;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

/**
 * Provides a {@code LiveData<PagedList>}, given a means to construct a DataSource.
 * <p>
 * Return type for data-loading system of an application or library to produce a
 * {@code LiveData<PagedList>}, while leaving the details of the paging mechanism up to the
 * consumer.
 *
 * @param <T> Data type produced by the DataSource, and held by the PagedLists.
 *
 * @see DataSource
 * @see PagedList
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LivePagedListProvider<T> {

    /**
     * Construct a new data source to be wrapped in a new NullPaddedList, which will be returned
     * through the LiveData.
     *
     * @return The data source.
     */
    @WorkerThread
    protected abstract DataSource<T> createDataSource();

    /**
     * Creates a LiveData of PagedLists, given the NullPaddedList.Config.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapterHelper} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param config NullPaddedList.Config to use with created PagedLists. This specifies how the
     *               lists will load data.
     *
     * @return The LiveData of PagedLists.
     */
    public LiveData<PagedList<T>> create(final PagedList.Config config) {
        return new ComputableLiveData<PagedList<T>>() {
            @Nullable
            private PagedList<T> mList;
            @Nullable
            private DataSource mDataSource;

            private final DataSource.InvalidatedCallback mCallback =
                    new DataSource.InvalidatedCallback() {
                @Override
                public void onInvalidated() {
                    invalidate();
                }
            };

            @Override
            protected PagedList<T> compute() {
                final int position;
                if (mList != null && mDataSource != null) {
                    if (mDataSource instanceof ContiguousDataSource) {
                        position = Math.max(0, mList.getLastLoad() - config.mInitialLoadSize / 2);
                    } else {
                        position = mList.getLastLoad();
                    }
                } else {
                    position = 0;
                }

                do {
                    if (mDataSource != null) {
                        mDataSource.removeInvalidatedCallback(mCallback);
                    }

                    mDataSource = createDataSource();
                    mDataSource.addInvalidatedCallback(mCallback);

                    mList = PagedList.create(mDataSource,
                            AppToolkitTaskExecutor.getMainThreadExecutor(),
                            AppToolkitTaskExecutor.getIOThreadExecutor(),
                            config,
                            position);
                } while (mList.isDetached());
                return mList;
            }
        }.getLiveData();
    }
}
