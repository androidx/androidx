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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplest data source form that provides all of its data through a single loadRange() method.
 * <p>
 * Requires that your data resides in positions <code>0</code> through <code>N</code>, where
 * <code>N</code> is the value returned from {@link #countItems()}. You must return the exact number
 * requested, so that the data as returned can be safely prepended/appended to what has already
 * been loaded.
 * <p>
 * For more flexibility in how many items to load, or to avoid counting your data source, override
 * {@link PositionalDataSource} directly.
 *
 * @param <Value> Value type returned by the data source.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BoundedDataSource<Value> extends PositionalDataSource<Value> {
    /**
     * Called to load items at from the specified position range.
     *
     * @param startPosition Index of first item to load.
     * @param loadCount     Exact number of items to load. Returning a different number will cause
     *                      an exception to be thrown.
     * @return List of loaded items. Null if the BoundedDataSource is no longer valid, and should
     *         not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Value> loadRange(int startPosition, int loadCount);

    @WorkerThread
    @Nullable
    @Override
    public List<Value> loadAfter(int startIndex, int pageSize) {
        return loadRange(startIndex, pageSize);
    }

    @WorkerThread
    @Nullable
    @Override
    public List<Value> loadBefore(int startIndex, int pageSize) {
        if (startIndex < 0) {
            return new ArrayList<>();
        }
        int loadSize = Math.min(pageSize, startIndex + 1);
        startIndex = startIndex - loadSize + 1;
        List<Value> result = loadRange(startIndex, loadSize);
        if (result != null) {
            if (result.size() != loadSize) {
                throw new IllegalStateException("invalid number of items returned.");
            }
            Collections.reverse(result);
        }
        return result;
    }
}
