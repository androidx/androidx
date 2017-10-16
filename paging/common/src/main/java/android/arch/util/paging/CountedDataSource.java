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

package android.arch.util.paging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;

/**
 * Incremental data loader for paging content when the total number of items is known.
 * <p>
 * All loading methods in CountedDataSource should only be called on a background thread.
 * loadAfter and loadBefore specifically may be called in parallel with each other, if loading in
 * both directions is required (often for prefetching).
 * <p>
 * Items returned from a CountedDataSource must be treated as immutable in all properties used by
 * {@link #loadAfter(int, Object, int)} and {@link #loadBefore(int, Object, int)}. For example, if
 * loadAfter(...User user...) is called, with <code>user.id</code> being used to load users after
 * that user's ID, the <code>id</code> field in User must remain immutable.
 * <p>
 * If the loadAfter and loadBefore methods only require indices to function, CountedDataSource does
 * not require items returned to be immutable in any way.
 *
 * @param <Type> Type of the items this CountedDataSource will produce.
 */
public abstract class CountedDataSource<Type> extends DataSourceBase {
    /**
     * @return The number of items in the DataSource.
     */
    @WorkerThread
    public abstract int loadCount();

    /**
     * Load initial data, starting after the passed position.
     *
     * @param position Position just before data to be loaded. -1 is passed to request data
     *                 from beginning of data source.
     * @param pageSize Suggested number of items to load.
     * @return List of initial items, representing data starting at position + 1. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadAfterInitial(int position, int pageSize);

    /**
     * Load data after the given position / item.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase number loaded than reduce.
     *
     * @param currentEndIndex Load items after this index, starting with currentEndIndex + 1.
     * @param currentEndItem Load items after this item, can be used for precise querying based on
     *                       item contents.
     * @param pageSize Suggested number of items to load.
     * @return List of items, starting at position currentEndIndex + 1. Null if the data source is
     *         no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadAfter(int currentEndIndex,
            @NonNull Type currentEndItem, int pageSize);

    /**
     * Load data before the given position / item.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase number loaded than reduce.
     *
     * @param currentBeginIndex Load items before this index, starting with currentBeginIndex - 1.
     * @param currentBeginItem Load items after this item, can be used for precise querying based on
     *                         item contents.
     * @param pageSize Suggested number of items to load.
     * @return List of items, in descending order, starting at position currentBeginIndex - 1.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadBefore(int currentBeginIndex,
            @NonNull Type currentBeginItem, int pageSize);
}
