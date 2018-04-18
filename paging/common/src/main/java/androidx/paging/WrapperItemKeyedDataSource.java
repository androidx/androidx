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
import androidx.arch.core.util.Function;

import java.util.IdentityHashMap;
import java.util.List;

class WrapperItemKeyedDataSource<K, A, B> extends ItemKeyedDataSource<K, B> {
    private final ItemKeyedDataSource<K, A> mSource;
    private final Function<List<A>, List<B>> mListFunction;

    private final IdentityHashMap<B, K> mKeyMap = new IdentityHashMap<>();

    WrapperItemKeyedDataSource(ItemKeyedDataSource<K, A> source,
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

    private List<B> convertWithStashedKeys(List<A> source) {
        List<B> dest = convert(mListFunction, source);
        synchronized (mKeyMap) {
            // synchronize on mKeyMap, since multiple loads may occur simultaneously.
            // Note: manually sync avoids locking per-item (e.g. Collections.synchronizedMap)
            for (int i = 0; i < dest.size(); i++) {
                mKeyMap.put(dest.get(i), mSource.getKey(source.get(i)));
            }
        }
        return dest;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<K> params,
            final @NonNull LoadInitialCallback<B> callback) {
        mSource.loadInitial(params, new LoadInitialCallback<A>() {
            @Override
            public void onResult(@NonNull List<A> data, int position, int totalCount) {
                callback.onResult(convertWithStashedKeys(data), position, totalCount);
            }

            @Override
            public void onResult(@NonNull List<A> data) {
                callback.onResult(convertWithStashedKeys(data));
            }
        });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<K> params,
            final @NonNull LoadCallback<B> callback) {
        mSource.loadAfter(params, new LoadCallback<A>() {
            @Override
            public void onResult(@NonNull List<A> data) {
                callback.onResult(convertWithStashedKeys(data));
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<K> params,
            final @NonNull LoadCallback<B> callback) {
        mSource.loadBefore(params, new LoadCallback<A>() {
            @Override
            public void onResult(@NonNull List<A> data) {
                callback.onResult(convertWithStashedKeys(data));
            }
        });
    }

    @NonNull
    @Override
    public K getKey(@NonNull B item) {
        synchronized (mKeyMap) {
            return mKeyMap.get(item);
        }
    }
}
