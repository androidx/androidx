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
import androidx.arch.core.util.Function;
import androidx.paging.futures.DirectExecutor;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * @param <Key> DataSource key type, same for original and wrapped.
 * @param <ValueFrom> Value type of original DataSource.
 * @param <ValueTo> Value type of new DataSource.
 */
class WrapperDataSource<Key, ValueFrom, ValueTo> extends DataSource<Key, ValueTo> {
    private final DataSource<Key, ValueFrom> mSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Function<List<ValueFrom>, List<ValueTo>> mListFunction;


    private final IdentityHashMap<ValueTo, Key> mKeyMap;

    WrapperDataSource(@NonNull DataSource<Key, ValueFrom> source,
            @NonNull Function<List<ValueFrom>, List<ValueTo>> listFunction) {
        super(source.mType);
        mSource = source;
        mListFunction = listFunction;
        mKeyMap = source.mType == KeyType.ITEM_KEYED ? new IdentityHashMap<ValueTo, Key>() : null;
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

    @Nullable
    @Override
    Key getKey(@NonNull ValueTo item) {
        if (mKeyMap != null) {
            synchronized (mKeyMap) {
                return mKeyMap.get(item);
            }
        }
        // positional / page-keyed
        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void stashKeysIfNeeded(@NonNull List<ValueFrom> source, @NonNull List<ValueTo> dest) {
        if (mKeyMap != null) {
            synchronized (mKeyMap) {
                for (int i = 0; i < dest.size(); i++) {
                    mKeyMap.put(dest.get(i), mSource.getKey(source.get(i)));
                }
            }
        }
    }

    @Override
    final ListenableFuture<? extends BaseResult> load(@NonNull Params params) {
        //noinspection unchecked
        return Futures.transform(
                mSource.load(params),
                new Function<BaseResult<ValueFrom>, BaseResult<ValueTo>>() {
                    @Override
                    public BaseResult<ValueTo> apply(BaseResult<ValueFrom> input) {
                        BaseResult<ValueTo> result = new BaseResult<>(input, mListFunction);
                        stashKeysIfNeeded(input.data, result.data);
                        return result;
                    }
                },
                DirectExecutor.INSTANCE);
    }
}
