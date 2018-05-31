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

import java.util.concurrent.Executor;

abstract class ContiguousDataSource<Key, Value> extends DataSource<Key, Value> {
    @Override
    boolean isContiguous() {
        return true;
    }

    abstract void dispatchLoadInitial(
            @Nullable Key key,
            int initialLoadSize,
            int pageSize,
            boolean enablePlaceholders,
            @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver);

    abstract void dispatchLoadAfter(
            int currentEndIndex,
            @NonNull Value currentEndItem,
            int pageSize,
            @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver);

    abstract void dispatchLoadBefore(
            int currentBeginIndex,
            @NonNull Value currentBeginItem,
            int pageSize,
            @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver);

    /**
     * Get the key from either the position, or item, or null if position/item invalid.
     * <p>
     * Position may not match passed item's position - if trying to query the key from a position
     * that isn't yet loaded, a fallback item (last loaded item accessed) will be passed.
     */
    abstract Key getKey(int position, Value item);
}
