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

import android.support.annotation.AnyThread;
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
 * provide generation of much of this code in the future):
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * from user ORDER BY name DESC LIMIT :limit")
 *     public abstract List&lt;User> userNameInitial(int limit);
 *
 *     {@literal @}Query("SELECT * from user WHERE name &lt; :key ORDER BY name DESC LIMIT :limit")
 *     public abstract List&lt;User> userNameLoadAfter(String key, int limit);
 *
 *     {@literal @}Query("SELECT * from user WHERE name > :key ORDER BY name ASC LIMIT :limit")
 *     public abstract List&lt;User> userNameLoadBefore(String key, int limit);
 * }
 *
 * public class KeyedUserQueryDataSource extends KeyedDataSource&lt;String, User> {
 *     private MyDatabase mDb;
 *     private final UserDao mUserDao;
 *     {@literal @}SuppressWarnings("FieldCanBeLocal")
 *     private final InvalidationTracker.Observer mObserver;
 *
 *     public OffsetUserQueryDataSource(MyDatabase db) {
 *         mDb = db;
 *         mUserDao = db.getUserDao();
 *         mObserver = new InvalidationTracker.Observer("user") {
 *             {@literal @}Override
 *             public void onInvalidated({@literal @}NonNull Set&lt;String> tables) {
 *                 // the user table has been invalidated, invalidate the DataSource
 *                 invalidate();
 *             }
 *         };
 *         db.getInvalidationTracker().addWeakObserver(mObserver);
 *     }
 *
 *     {@literal @}Override
 *     public boolean isInvalid() {
 *         mDb.getInvalidationTracker().refreshVersionsSync();
 *         return super.isInvalid();
 *     }
 *
 *     {@literal @}Override
 *     public String getKey({@literal @}NonNull User item) {
 *         return item.getName();
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;User> loadInitial(int pageSize) {
 *         return mUserDao.userNameInitial(pageSize);
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;User> loadBefore({@literal @}NonNull String userName, int pageSize) {
 *         return mUserDao.userNameLoadBefore(userName, pageSize);
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;User> loadAfter({@literal @}Nullable String userName, int pageSize) {
 *         return mUserDao.userNameLoadAfter(userName, pageSize);
 *     }
 * }</pre>
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class KeyedDataSource<Key, Value> extends ContiguousDataSource<Key, Value> {
    @Override
    public final int countItems() {
        return 0; // method not called, can't be overridden
    }

    @Nullable
    @Override
    List<Value> loadAfterImpl(int currentEndIndex, @NonNull Value currentEndItem, int pageSize) {
        return loadAfter(getKey(currentEndItem), pageSize);
    }

    @Nullable
    @Override
    List<Value> loadBeforeImpl(
            int currentBeginIndex, @NonNull Value currentBeginItem, int pageSize) {
        return loadBefore(getKey(currentBeginItem), pageSize);
    }

    @Nullable
    private NullPaddedList<Value> loadInitialInternal(
            @Nullable Key key, int initialLoadSize, boolean enablePlaceholders) {
        List<Value> list;
        if (key == null) {
            // no key, so load initial.
            list = loadInitial(initialLoadSize);
            if (list == null) {
                return null;
            }
        } else {
            List<Value> after = loadAfter(key, initialLoadSize / 2);
            if (after == null) {
                return null;
            }

            Key loadBeforeKey = after.isEmpty() ? key : getKey(after.get(0));
            List<Value> before = loadBefore(loadBeforeKey, initialLoadSize / 2);
            if (before == null) {
                return null;
            }
            if (!after.isEmpty() || !before.isEmpty()) {
                // one of the lists has data
                if (after.isEmpty()) {
                    // retry loading after, since it may be that the key passed points to the end of
                    // the list, so we need to load after the last item in the before list
                    after = loadAfter(getKey(before.get(0)), initialLoadSize / 2);
                    if (after == null) {
                        return null;
                    }
                }
                // assemble full list
                list = new ArrayList<>();
                list.addAll(before);
                // Note - we reverse the list instead of before, in case before is immutable
                Collections.reverse(list);
                list.addAll(after);
            } else {
                // load before(key) and load after(key) failed - try load initial to be *sure* we
                // catch the case where there's only one item, which is loaded by the key case
                list = loadInitial(initialLoadSize);
                if (list == null) {
                    return null;
                }
            }
        }

        if (list.isEmpty()) {
            // wasn't able to load any items, so publish an unpadded empty list.
            return new NullPaddedList<>(0, Collections.<Value>emptyList());
        }

        int itemsBefore = COUNT_UNDEFINED;
        int itemsAfter = COUNT_UNDEFINED;
        if (enablePlaceholders) {
            itemsBefore = countItemsBefore(getKey(list.get(0)));
            itemsAfter = countItemsAfter(getKey(list.get(list.size() - 1)));
            if (isInvalid()) {
                return null;
            }
        }
        if (itemsBefore == COUNT_UNDEFINED || itemsAfter == COUNT_UNDEFINED) {
            return new NullPaddedList<>(0, list, 0);
        } else {
            return new NullPaddedList<>(itemsBefore, list, itemsAfter);
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public NullPaddedList<Value> loadInitial(
            @Nullable Key key, int initialLoadSize, boolean enablePlaceholders) {
        if (isInvalid()) {
            return null;
        }
        NullPaddedList<Value> list = loadInitialInternal(key, initialLoadSize, enablePlaceholders);
        if (list == null || isInvalid()) {
            return null;
        }
        return list;
    }

    /**
     * Return a key associated with the given item.
     * <p>
     * If your KeyedDataSource is loading from a source that is sorted and loaded by a unique
     * integer ID, you would return {@code item.getID()} here. This key can then be passed to
     * {@link #loadBefore(Key, int)} or {@link #loadAfter(Key, int)} to load additional items
     * adjacent to the item passed to this function.
     * <p>
     * If your key is more complex, such as when you're sorting by name, then resolving collisions
     * with integer ID, you'll need to return both. In such a case you would use a wrapper class,
     * such as {@code Pair<String, Integer>} or, in Kotlin,
     * {@code data class Key(val name: String, val id: Int)}
     *
     * @param item Item to get the key from.
     * @return Key associated with given item.
     */
    @NonNull
    @AnyThread
    public abstract Key getKey(@NonNull Value item);

    /**
     * Return the number of items that occur before the item uniquely identified by {@code key} in
     * the data set.
     * <p>
     * For example, if you're loading items sorted by ID, then this would return the total number of
     * items with ID less than {@code key}.
     * <p>
     * If you return {@link #COUNT_UNDEFINED} here, or from {@link #countItemsAfter(Key)}, your
     * data source will not present placeholder null items in place of unloaded data.
     *
     * @param key A unique identifier of an item in the data set.
     * @return Number of items in the data set before the item identified by {@code key}, or
     *         {@link #COUNT_UNDEFINED}.
     *
     * @see #countItemsAfter(Key)
     */
    @WorkerThread
    public int countItemsBefore(@NonNull Key key) {
        return COUNT_UNDEFINED;
    }

    /**
     * Return the number of items that occur after the item uniquely identified by {@code key} in
     * the data set.
     * <p>
     * For example, if you're loading items sorted by ID, then this would return the total number of
     * items with ID greater than {@code key}.
     * <p>
     * If you return {@link #COUNT_UNDEFINED} here, or from {@link #countItemsBefore(Key)}, your
     * data source will not present placeholder null items in place of unloaded data.
     *
     * @param key A unique identifier of an item in the data set.
     * @return Number of items in the data set after the item identified by {@code key}, or
     *         {@link #COUNT_UNDEFINED}.
     *
     * @see #countItemsBefore(Key)
     */
    @WorkerThread
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

    @Nullable
    @Override
    Key getKey(int position, Value item) {
        if (item == null) {
            return null;
        }
        return getKey(item);
    }
}
