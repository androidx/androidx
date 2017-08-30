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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 * <p>
 * Implement a DataSource using KeyedDataSource if you need to use data from item <code>N-1</code>
 * to load item <code>N</code>. This is common, for example, in sorted database queries where
 * attributes of the item such just before the next query define how to execute it.
 * <p>
 * A compute usage pattern with Room SQL queries would look like this (though note, Room plans to
 * provide generation of some of this code in the future):
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * from user WHERE mName < :key ORDER BY mName DESC LIMIT :limit")
 *     public abstract List<User> userNameLoadAfter(String key, int limit);
 *
 *     {@literal @}Query("SELECT COUNT(*) from user WHERE mName < :key ORDER BY mName DESC")
 *     public abstract List<User> userNameCountAfter(String key);
 *
 *     {@literal @}Query("SELECT * from user WHERE mName > :key ORDER BY mName ASC LIMIT :limit")
 *     public abstract List<User> userNameLoadBefore(String key, int limit);
 *
 *     {@literal @}Query("SELECT COUNT(*) from user WHERE mName > :key ORDER BY mName ASC")
 *     public abstract List<User> userNameCountBefore(String key);
 * }
 *
 * public class KeyedUserQueryDataSource extends KeyedDataSource&lt;User> { // TODO: update
 *     {@literal @}Override
 *     public int loadCount() {
 *         return mUserDao.getUserCount();
 *     }
 *
 *     {@literal @}Nullable
 *     {@literal @}Override
 *     public List&lt;User> loadAfterInitial(int position, int pageSize) {
 *         return mUserDao.userNameLimitOffset(pageSize, position);
 *     }
 *
 *     {@literal @}Nullable
 *     {@literal @}Override
 *     public List&lt;User> loadAfter({@literal @}NonNull User currentEndItem, int pageSize) {
 *         return mUserDao.userNameLoadAfter(currentEndItem.getName(), pageSize);
 *     }
 *
 *     {@literal @}Nullable
 *     {@literal @}Override
 *     public List&lt;User> loadBefore({@literal @}NonNull User currentBeginItem, int pageSize) {
 *         return mUserDao.userNameLoadBefore(currentBeginItem.getName(), pageSize);
 *     }
 * }</pre>
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class KeyedDataSource<Key, Value> extends ContiguousDataSource<Key, Value> {
    public final int loadCount() {
        return 0; // method not called, can't be overridden
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public List<Value> loadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize) {
        return loadAfter(getKey(currentEndItem), pageSize);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public List<Value> loadBefore(int currentBeginIndex, @NonNull Value currentBeginItem,
            int pageSize) {
        return loadBefore(getKey(currentBeginItem), pageSize);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @WorkerThread
    public NullPaddedList<Value> loadInitial(@Nullable Key key, int initialLoadSize) {
        List<Value> after;
        if (key == null) {
            // no key, so load initial.
            after = loadInitial(initialLoadSize);
            if (after == null) {
                return null;
            }
        } else {
            after = loadAfter(key, initialLoadSize);
            if (after == null) {
                return null;
            }

            if (after.isEmpty()) {
                // if no content exists after current key, loadBefore instead
                after = loadBefore(key, initialLoadSize);
                if (after == null) {
                    return null;
                }
                Collections.reverse(after);
            }
        }

        if (after.isEmpty()) {
            // wasn't able to load any items, so publish an empty list that can't grow.
            return new NullPaddedList<>(0, Collections.<Value>emptyList());
        }

        // TODO: consider also loading another page before here

        int itemsBefore = countItemsBefore(getKey(after.get(0)));
        int itemsAfter = countItemsAfter(getKey(after.get(after.size() - 1)));
        if (itemsBefore == COUNT_UNDEFINED || itemsAfter == COUNT_UNDEFINED) {
            return new NullPaddedList<>(0, after, 0);
        } else {
            return new NullPaddedList<>(itemsBefore, after, itemsAfter);
        }
    }

    @NonNull
    public abstract Key getKey(@NonNull Value item);

    public int countItemsBefore(@NonNull Key key) {
        return COUNT_UNDEFINED;
    }

    public int countItemsAfter(@NonNull Key key) {
        return COUNT_UNDEFINED;
    }

    @WorkerThread
    @Nullable
    public abstract List<Value> loadInitial(int pageSize);

    /**
     * Load list data after the specified item.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param currentEndKey Load items after this key. May be null on initial load, to indicate load
     *                      from beginning.
     * @param pageSize      Suggested number of items to load.
     * @return List of items, starting after the specified item. Null if the data source is
     * no longer valid, and should not be queried again.
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    @Nullable
    public abstract List<Value> loadAfter(@NonNull Key currentEndKey, int pageSize);

    /**
     * Load data before the currently loaded content, starting at the provided index.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param currentBeginKey Load items before this key.
     * @param pageSize         Suggested number of items to load.
     * @return List of items, in descending order, starting after the specified item. Null if the
     * data source is no longer valid, and should not be queried again.
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    @Nullable
    public abstract List<Value> loadBefore(@NonNull Key currentBeginKey, int pageSize);
}
