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
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.Collections;
import java.util.List;

/**
 * @param <T> Type loaded by the TiledDataSource.
 *
 * @deprecated Use {@link PositionalDataSource}
 * @hide
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class TiledDataSource<T> extends PositionalDataSource<T> {

    @WorkerThread
    public abstract int countItems();

    @Override
    boolean isContiguous() {
        return false;
    }

    @WorkerThread
    public abstract List<T> loadRange(int startPosition, int count);

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
            @NonNull LoadInitialCallback<T> callback) {
        int totalCount = countItems();
        if (totalCount == 0) {
            callback.onResult(Collections.<T>emptyList(), 0, 0);
            return;
        }

        // bound the size requested, based on known count
        final int firstLoadPosition = computeInitialLoadPosition(params, totalCount);
        final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount);

        // convert from legacy behavior
        List<T> list = loadRange(firstLoadPosition, firstLoadSize);
        if (list != null) {
            callback.onResult(list, firstLoadPosition, totalCount);
        } else {
            invalidate();
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
            @NonNull LoadRangeCallback<T> callback) {
        List<T> list = loadRange(params.startPosition, params.loadSize);
        if (list != null) {
            callback.onResult(list);
        } else {
            invalidate();
        }
    }
}
