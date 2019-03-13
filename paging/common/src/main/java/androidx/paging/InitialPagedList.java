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
import androidx.paging.futures.DirectExecutor;

/**
 * InitialPagedList is an empty placeholder that's sent at the front of a stream of PagedLists.
 *
 * It's used solely for listening to {@link PagedList.LoadType#REFRESH} loading events, and retrying
 * any errors that occur during initial load.
 */
class InitialPagedList<K, V> extends ContiguousPagedList<K, V> {
    @Nullable
    private K mInitialKey;

    InitialPagedList(
            @NonNull DataSource<K, V> dataSource,
            @NonNull Config config,
            @Nullable K initialKey) {
        super(dataSource,
                DirectExecutor.INSTANCE,
                DirectExecutor.INSTANCE,
                null,
                config,
                DataSource.BaseResult.<V>empty(),
                /* no previous load, so pass 0 */ 0);
        mInitialKey = initialKey;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mInitialKey;
    }
}
