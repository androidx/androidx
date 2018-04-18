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
import androidx.arch.core.util.Function;

import java.util.List;

class WrapperPageKeyedDataSource<K, A, B> extends PageKeyedDataSource<K, B> {
    private final PageKeyedDataSource<K, A> mSource;
    private final Function<List<A>, List<B>> mListFunction;

    WrapperPageKeyedDataSource(PageKeyedDataSource<K, A> source,
            Function<List<A>, List<B>> listFunction) {
        mSource = source;
        mListFunction = listFunction;
    }

    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mSource.addInvalidatedCallback(onInvalidatedCallback);
    }

    @Override
    public void removeInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mSource.removeInvalidatedCallback(onInvalidatedCallback);
    }

    @Override
    public void invalidate() {
        mSource.invalidate();
    }

    @Override
    public boolean isInvalid() {
        return mSource.isInvalid();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<K> params,
            final @NonNull LoadInitialCallback<K, B> callback) {
        mSource.loadInitial(params, new LoadInitialCallback<K, A>() {
            @Override
            public void onResult(@NonNull List<A> data, int position, int totalCount,
                    @Nullable K previousPageKey, @Nullable K nextPageKey) {
                callback.onResult(convert(mListFunction, data), position, totalCount,
                        previousPageKey, nextPageKey);
            }

            @Override
            public void onResult(@NonNull List<A> data, @Nullable K previousPageKey,
                    @Nullable K nextPageKey) {
                callback.onResult(convert(mListFunction, data), previousPageKey, nextPageKey);
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<K> params,
            final @NonNull LoadCallback<K, B> callback) {
        mSource.loadBefore(params, new LoadCallback<K, A>() {
            @Override
            public void onResult(@NonNull List<A> data, @Nullable K adjacentPageKey) {
                callback.onResult(convert(mListFunction, data), adjacentPageKey);
            }
        });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<K> params,
            final @NonNull LoadCallback<K, B> callback) {
        mSource.loadAfter(params, new LoadCallback<K, A>() {
            @Override
            public void onResult(@NonNull List<A> data, @Nullable K adjacentPageKey) {
                callback.onResult(convert(mListFunction, data), adjacentPageKey);
            }
        });
    }
}
