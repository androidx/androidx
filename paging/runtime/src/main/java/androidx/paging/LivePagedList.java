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
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

class LivePagedList<Key, Value> extends LiveData<PagedList<Value>>
        implements FutureCallback<PagedList<Value>> {
    @NonNull
    private final PagedList.Config mConfig;

    @Nullable
    private final PagedList.BoundaryCallback<Value> mBoundaryCallback;

    @NonNull
    private final DataSource.Factory<Key, Value> mDataSourceFactory;

    @NonNull
    private final Executor mNotifyExecutor;

    @NonNull
    private final Executor mFetchExecutor;

    @NonNull
    private PagedList<Value> mCurrentData;

    @Nullable
    private ListenableFuture<PagedList<Value>> mCurrentFuture = null;

    @Override
    protected void onActive() {
        super.onActive();
        invalidate(false);
    }

    private final DataSource.InvalidatedCallback mCallback =
            new DataSource.InvalidatedCallback() {
                @Override
                public void onInvalidated() {
                    invalidate(true);
                }
            };

    private final Runnable mRefreshRetryCallback = new Runnable() {
        @Override
        public void run() {
            invalidate(true);
        }
    };

    LivePagedList(
            @Nullable Key initialKey,
            @NonNull PagedList.Config config,
            @Nullable PagedList.BoundaryCallback<Value> boundaryCallback,
            @NonNull DataSource.Factory<Key, Value> dataSourceFactory,
            @NonNull Executor notifyExecutor,
            @NonNull Executor fetchExecutor) {
        mConfig = config;
        mBoundaryCallback = boundaryCallback;
        mDataSourceFactory = dataSourceFactory;
        mNotifyExecutor = notifyExecutor;
        mFetchExecutor = fetchExecutor;
        mCurrentData = new InitialPagedList<>(dataSourceFactory.create(), config, initialKey);
        onSuccess(mCurrentData);
    }

    private void onItemUpdate(@NonNull PagedList<Value> previous, @NonNull PagedList<Value> next) {
        previous.setRetryCallback(null);
        next.setRetryCallback(mRefreshRetryCallback);
    }

    @SuppressWarnings("unchecked") // getLastKey guaranteed to be of 'Key' type
    @NonNull
    private ListenableFuture<PagedList<Value>> getListenableFuture() {
        DataSource<Key, Value> dataSource = mDataSourceFactory.create();
        mCurrentData.getDataSource().removeInvalidatedCallback(mCallback);
        dataSource.addInvalidatedCallback(mCallback);

        mCurrentData.setInitialLoadState(PagedList.LoadState.LOADING, null);

        return PagedList.create(
                dataSource,
                mNotifyExecutor,
                mFetchExecutor,
                mFetchExecutor,
                mBoundaryCallback,
                mConfig,
                (Key) mCurrentData.getLastKey());
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        PagedList.LoadState loadState = mCurrentData.getDataSource()
                .isRetryableError(throwable)
                ? PagedList.LoadState.RETRYABLE_ERROR
                : PagedList.LoadState.ERROR;
        mCurrentData.setInitialLoadState(loadState, throwable);
    }

    @Override
    public void onSuccess(@NonNull PagedList<Value> value) {
        onItemUpdate(mCurrentData, value);
        mCurrentData = value;
        setValue(value);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void invalidate(boolean force) {
        if (mCurrentFuture != null) {
            if (force) {
                mCurrentFuture.cancel(false);
            } else {
                // work is already ongoing, not forcing, so skip invalidate
                return;
            }
        }
        mCurrentFuture = getListenableFuture();
        Futures.addCallback(mCurrentFuture, this, mNotifyExecutor);
    }
}
