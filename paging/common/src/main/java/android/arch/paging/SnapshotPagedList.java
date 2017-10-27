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

class SnapshotPagedList<T> extends PagedList<T> {
    private final boolean mContiguous;
    private final Object mLastKey;

    SnapshotPagedList(@NonNull PagedList<T> pagedList) {
        super(pagedList.mStorage.snapshot(),
                pagedList.mMainThreadExecutor,
                pagedList.mBackgroundThreadExecutor,
                null,
                pagedList.mConfig);
        mContiguous = pagedList.isContiguous();
        mLastKey = pagedList.getLastKey();
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public boolean isDetached() {
        return true;
    }

    @Override
    boolean isContiguous() {
        return mContiguous;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mLastKey;
    }

    @Override
    void dispatchUpdatesSinceSnapshot(@NonNull PagedList<T> storageSnapshot,
            @NonNull Callback callback) {
    }

    @Override
    void loadAroundInternal(int index) {
    }
}
