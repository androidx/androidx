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

public class LivePagedListBuilder<Key, Value> {
    private Key mInitialLoadKey;
    private PagedList.Config mConfig;
    private DataSource.Factory<Key, Value> mDataSourceFactory;
    private PagedList.BoundaryCallback mBoundaryCallback;
    private Executor mMainThreadExecutor;
    private Executor mBackgroundThreadExecutor;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public LivePagedListBuilder<Key, Value> setInitialLoadKey(@Nullable Key key) {
        mInitialLoadKey = key;
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public LivePagedListBuilder<Key, Value> setPagingConfig(@NonNull PagedList.Config config) {
        mConfig = config;
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public LivePagedListBuilder<Key, Value> setPagingConfig(int pageSize) {
        mConfig = new PagedList.Config.Builder().setPageSize(pageSize).build();
        return this;
    }

    @NonNull
    public LivePagedListBuilder<Key, Value> setDataSourceFactory(
            @NonNull DataSource.Factory<Key, Value> dataSourceFactory) {
        mDataSourceFactory = dataSourceFactory;
        return this;
    }

    @SuppressWarnings("unused")
    @NonNull
    public LivePagedListBuilder<Key, Value> setBoundaryCallback(
            @Nullable PagedList.BoundaryCallback<Value> boundaryCallback) {
        mBoundaryCallback = boundaryCallback;
        return this;
    }

    @SuppressWarnings("unused")
    @NonNull
    public LivePagedListBuilder<Key, Value> setMainThreadExecutor(
            @NonNull Executor mainThreadExecutor) {
        mMainThreadExecutor = mainThreadExecutor;
        return this;
    }

    @SuppressWarnings("unused")
    @NonNull
    public LivePagedListBuilder<Key, Value> setBackgroundThreadExecutor(
            @NonNull Executor backgroundThreadExecutor) {
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        return this;
    }

    @NonNull
    public LiveData<PagedList<Value>> build() {
        if (mConfig == null) {
            throw new IllegalArgumentException("PagedList.Config must be provided");
        }
        if (mDataSourceFactory == null) {
            throw new IllegalArgumentException("DataSource.Factory must be provided");
        }
        if (mMainThreadExecutor == null) {
            mMainThreadExecutor = ArchTaskExecutor.getMainThreadExecutor();
        }
        if (mBackgroundThreadExecutor == null) {
            mBackgroundThreadExecutor = ArchTaskExecutor.getIOThreadExecutor();
        }

        return create(mInitialLoadKey, mConfig, mBoundaryCallback, mDataSourceFactory,
                mMainThreadExecutor, mBackgroundThreadExecutor);
    }

    @AnyThread
    @NonNull
    public static <Key, Value> LiveData<PagedList<Value>> create(
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

                    mList = new PagedList.Builder<Key, Value>()
                            .setDataSource(mDataSource)
                            .setMainThreadExecutor(mainThreadExecutor)
                            .setBackgroundThreadExecutor(backgroundThreadExecutor)
                            .setBoundaryCallback(boundaryCallback)
                            .setConfig(config)
                            .setInitialKey(initializeKey)
                            .build();
                } while (mList.isDetached());
                return mList;
            }
        }.getLiveData();
    }
}
