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
 * @param <K> Key type of the DataSource, used to initialize PagedLists.
 * @param <T> Data type produced by the DataSource, and held by the PagedLists.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LivePagedListProvider<K, T> {

    /**
     * Construct a new data source to be wrapped in a new PagedList, which will be returned through
     * the LiveData.
     *
     * @return The data source.
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    protected abstract DataSource<K, T> createDataSource();

    /**
     * Creates a LiveData of PagedLists, given the ListConfig.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapterHelper} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param configuration ListConfig to use with created PagedLists. This specifies how the lists
     *                      will load data.
     *
     * @return The LiveData of LazyLists.
     */
    public LiveData<PagedList<T>> create(final ListConfig configuration) {
        return new ComputableLiveData<PagedList<T>>() {
            @Nullable
            private PagedList<T> mList;
            @Nullable
            private DataSource<K, T> mDataSource;

            private final DataSourceBase.InvalidatedCallback mCallback =
                    new DataSourceBase.InvalidatedCallback() {
                @Override
                public void onInvalidated() {
                    invalidate();
                }
            };

            @Override
            protected PagedList<T> compute() {
                PagedList<T> old = mList;

                boolean done = true;
                do {
                    if (mDataSource != null) {
                        mDataSource.removeInvalidatedCallback(mCallback);
                    }

                    mDataSource = createDataSource();
                    mDataSource.addInvalidatedCallback(mCallback);
                    mList = new PagedList<>(mDataSource,
                            AppToolkitTaskExecutor.getMainThreadExecutor(),
                            AppToolkitTaskExecutor.getIOThreadExecutor(),
                            configuration);
                    if (old != null) {
                        done = mList.initializeFrom(old);
                    }
                } while (!done);
                return mList;
            }
        }.getLiveData();
    }
}
