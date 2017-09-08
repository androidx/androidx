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

/**
 * Incremental data loader for paging positional content, where content can be loaded based on its
 * integer position.
 * <p>
 * Use PositionalDataSource if you only need position as input for item loading - if for example,
 * you're asking the backend for items at positions 10 through 20, or using a limit/offset database
 * query to load items at query position 10 through 20.
 * <p>
 * Implement a DataSource using PositionalDataSource if position is the only information you need to
 * load items.
 * <p>
 * Note that {@link BoundedDataSource} provides a simpler API for positional loading, if your
 * backend or data store doesn't require
 * <p>
 * @param <Value> Value type of items being loaded by the DataSource.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PositionalDataSource<Value> extends ContiguousDataSource<Integer, Value> {
    @Nullable
    @Override
    List<Value> loadAfterImpl(int currentEndIndex, @NonNull Value currentEndItem, int pageSize) {
        return loadAfter(currentEndIndex + 1, pageSize);
    }

    @Nullable
    @Override
    List<Value> loadBeforeImpl(int currentBeginIndex, @NonNull Value currentBeginItem,
            int pageSize) {
        return loadBefore(currentBeginIndex - 1, pageSize);
    }


    /**
     * Load initial data, starting after the passed position.
     *
     * @param position Index just before the data to be loaded.
     * @param initialLoadSize Suggested number of items to load.
     * @return List of initial items, representing data starting at position. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    @Nullable
    @Override
    public NullPaddedList<Value> loadInitial(
            Integer position, int initialLoadSize, boolean enablePlaceholders) {
        final int convertPosition = position == null ? 0 : position;
        final int loadPosition = Math.max(0, (convertPosition - initialLoadSize / 2));

        int count = COUNT_UNDEFINED;
        if (enablePlaceholders) {
            count = countItems();
        }
        List<Value> data = loadAfter(loadPosition, initialLoadSize);
        if (count == COUNT_UNDEFINED) {
            return new NullPaddedList<>(loadPosition, data);
        } else {
            return new NullPaddedList<>(loadPosition, count, data);
        }
    }

    /**
     * Load data after currently loaded content, starting at the provided index.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param startIndex Load items starting at this index.
     * @param pageSize Suggested number of items to load.
     * @return List of items, starting at position currentEndIndex + 1. Null if the data source is
     *         no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Value> loadAfter(int startIndex, int pageSize);

    /**
     * Load data before the currently loaded content, starting at the provided index.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param startIndex Load items, starting at this index.
     * @param pageSize Suggested number of items to load.
     * @return List of items, in descending order, starting at position currentBeginIndex - 1. Null
     *         if the data source is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Value> loadBefore(int startIndex, int pageSize);

    @Override
    Integer getKey(int position, Value item) {
        return position;
    }
}
