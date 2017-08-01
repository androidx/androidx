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
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.List;


/**
 * Incremental data loader for list paging.
 *
 * @param <Key> Type of the key used to load initial data.
 * @param <Type> Type of the items this CountedDataSource will produce.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class DataSource<Key, Type> extends DataSourceBase {

    /**
     * Get key from item.
     *
     * @param item The item.
     * @return The Key.
     */
    public abstract Key getKey(@NonNull Type item);

    /**
     * Load initial data, starting after the passed key.
     *
     * @param key Key just before the data to be loaded.
     * @param pageSize Suggested number of items to load.
     * @return List of initial items, representing data starting at key + 1. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadAfterInitial(@Nullable Key key, int pageSize);

    /**
     * Load data, starting after the passed item.
     *
     * @param currentEndItem Load items after this item, can be used for precise querying based on
     *                       item contents.
     * @param pageSize Suggested number of items to load.
     * @return List of initial items, representing data starting at key + 1. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadAfter(@NonNull Type currentEndItem, int pageSize);

    /**
     * Load data, before the passed item.
     *
     * @param currentBeginItem Load items after this item, can be used for precise querying based on
     *                         item contents.
     * @param pageSize Suggested number of items to load.
     * @return List of initial items, representing data starting at key + 1. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadBefore(@NonNull Type currentBeginItem, int pageSize);
}
