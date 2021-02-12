/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging.samples.java;

import androidx.annotation.NonNull;
import androidx.paging.ListenableFuturePagingSource;
import androidx.paging.PagingState;
import androidx.paging.samples.shared.ExampleGuavaBackendService;
import androidx.paging.samples.shared.SearchUserResponse;
import androidx.paging.samples.shared.User;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.Executor;

import retrofit2.HttpException;

class PagingSourceListenableFutureSample extends ListenableFuturePagingSource<Integer, User> {
    @NonNull
    private ExampleGuavaBackendService mBackend;
    @NonNull
    private String mQuery;
    @NonNull
    private Executor mBgExecutor;

    PagingSourceListenableFutureSample(
            @NonNull ExampleGuavaBackendService backend,
            @NonNull String query, @NonNull Executor bgExecutor) {
        mBackend = backend;
        mQuery = query;
        mBgExecutor = bgExecutor;
    }

    @NotNull
    @Override
    public ListenableFuture<LoadResult<Integer, User>> loadFuture(
            @NotNull LoadParams<Integer> params) {
        // Start refresh at page 1 if undefined.
        Integer nextPageNumber = params.getKey();
        if (nextPageNumber == null) {
            nextPageNumber = 1;
        }

        ListenableFuture<LoadResult<Integer, User>> pageFuture = Futures.transform(
                mBackend.searchUsers(mQuery, nextPageNumber),
                this::toLoadResult, mBgExecutor);

        ListenableFuture<LoadResult<Integer, User>> partialLoadResultFuture = Futures.catching(
                pageFuture, HttpException.class,
                LoadResult.Error::new, mBgExecutor);

        return Futures.catching(partialLoadResultFuture,
                IOException.class, LoadResult.Error::new, mBgExecutor);
    }

    private LoadResult<Integer, User> toLoadResult(@NonNull SearchUserResponse response) {
        return new LoadResult.Page<>(
                response.getUsers(),
                null, // Only paging forward.
                response.getNextPageNumber(),
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NotNull PagingState<Integer, User> state) {
        Integer anchorPosition = state.getAnchorPosition();
        if (anchorPosition == null) {
            return null;
        }

        LoadResult.Page<Integer, User> anchorPage = state.closestPageToPosition(anchorPosition);
        if (anchorPage == null) {
            return null;
        }

        Integer prevKey = anchorPage.getPrevKey();
        if (prevKey != null) {
            return prevKey + 1;
        }

        Integer nextKey = anchorPage.getNextKey();
        if (nextKey != null) {
            return nextKey + 1;
        }

        return null;
    }
}
