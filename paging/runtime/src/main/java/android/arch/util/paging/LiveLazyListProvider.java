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
import android.support.annotation.WorkerThread;

/**
 * This class wraps to {@link android.arch.util.paging.CountedDataSource CountedDataSource} and can
 * provide a {@link android.arch.lifecycle.LiveData LiveData} of
 * {@link android.arch.util.paging.LazyList LazyList}.
 *
 * @param <T> Data type produced by the CountedDataSource, and held by the LazyLists.
 *
 * @see LazyListAdapterHelper
 */
public abstract class LiveLazyListProvider<T> {

    /**
     * Construct a new data source to be wrapped in a new LazyList, which will be returned through
     * the LiveData.
     *
     * @return The data source.
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    protected abstract CountedDataSource<T> createDataSource();

    /**
     * Creates a LiveData of LazyLists, given the ListConfig.
     * <p>
     * This LiveData can be passed to a {@link LazyListAdapterHelper} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param configuration ListConfig to use with created LazyLists. This specifies how the lists
     *                      will load data.
     *
     * @return The LiveData of LazyLists.
     */
    public LiveData<LazyList<T>> create(final ListConfig configuration) {
        return new ComputableLiveData<LazyList<T>>() {
            @Nullable
            private LazyList<T> mList;
            @Nullable
            private CountedDataSource<T> mDataSource;

            private final DataSourceBase.InvalidatedCallback mCallback =
                    new DataSourceBase.InvalidatedCallback() {
                @Override
                public void onInvalidated() {
                    invalidate();
                }
            };

            @Override
            protected LazyList<T> compute() {
                int loadAfterPos = mList == null ? -2 : mList.getInitialLoadPosition();

                boolean done = true;
                do {
                    if (mDataSource != null) {
                        mDataSource.removeInvalidatedCallback(mCallback);
                    }

                    mDataSource = createDataSource();
                    mDataSource.addInvalidatedCallback(mCallback);
                    mList = new LazyList<>(mDataSource,
                            AppToolkitTaskExecutor.getMainThreadExecutor(),
                            AppToolkitTaskExecutor.getIOThreadExecutor(),
                            configuration);
                    if (loadAfterPos >= -1) {
                        done = mList.internalInit(loadAfterPos);
                    }
                } while (!done);
                return mList;
            }
        }.getLiveData();
    }
}
