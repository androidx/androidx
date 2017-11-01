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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.List;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ContiguousDataSource<Key, Value> extends DataSource<Key, Value> {
    @Override
    boolean isContiguous() {
        return true;
    }

    void loadInitial(Key key, int pageSize, boolean enablePlaceholders,
            PageResult.Receiver<Key, Value> receiver) {
        NullPaddedList<Value> initial = loadInitial(key, pageSize, enablePlaceholders);
        if (initial != null) {
            receiver.onPageResult(new PageResult<>(
                    PageResult.INIT,
                    new Page<Key, Value>(initial.mList),
                    initial.getLeadingNullCount(),
                    initial.getTrailingNullCount(),
                    initial.getPositionOffset()));
        } else {
            receiver.onPageResult(new PageResult<Key, Value>(
                    PageResult.INIT, null, 0, 0, 0));
        }
    }

    void loadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize,
            PageResult.Receiver<Key, Value> receiver) {
        List<Value> list = loadAfter(currentEndIndex, currentEndItem, pageSize);

        Page<Key, Value> page = list != null
                ? new Page<Key, Value>(list) : null;

        receiver.postOnPageResult(new PageResult<>(
                PageResult.APPEND, page, 0, 0, 0));
    }

    void loadBefore(int currentBeginIndex, @NonNull Value currentBeginItem, int pageSize,
            PageResult.Receiver<Key, Value> receiver) {
        List<Value> list = loadBefore(currentBeginIndex, currentBeginItem, pageSize);

        Page<Key, Value> page = list != null
                ? new Page<Key, Value>(list) : null;

        receiver.postOnPageResult(new PageResult<>(
                PageResult.PREPEND, page, 0, 0, 0));
    }

    /**
     * Get the key from either the position, or item, or null if position/item invalid.
     * <p>
     * Position may not match passed item's position - if trying to query the key from a position
     * that isn't yet loaded, a fallback item (last loaded item accessed) will be passed.
     */
    abstract Key getKey(int position, Value item);

    @Nullable
    abstract List<Value> loadAfterImpl(int currentEndIndex,
            @NonNull Value currentEndItem, int pageSize);

    @Nullable
    abstract List<Value> loadBeforeImpl(int currentBeginIndex,
            @NonNull Value currentBeginItem, int pageSize);

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    @Nullable
    public abstract NullPaddedList<Value> loadInitial(
            Key key, int initialLoadSize, boolean enablePlaceholders);

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    @Nullable
    public final List<Value> loadAfter(int currentEndIndex,
            @NonNull Value currentEndItem, int pageSize) {
        if (isInvalid()) {
            return null;
        }
        List<Value> list = loadAfterImpl(currentEndIndex, currentEndItem, pageSize);
        if (isInvalid()) {
            return null;
        }
        return list;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    @Nullable
    public final List<Value> loadBefore(int currentBeginIndex,
            @NonNull Value currentBeginItem, int pageSize) {
        if (isInvalid()) {
            return null;
        }
        List<Value> list = loadBeforeImpl(currentBeginIndex, currentBeginItem, pageSize);
        if (isInvalid()) {
            return null;
        }
        return list;

    }
}
