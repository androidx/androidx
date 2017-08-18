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
 *     {@literal @}Query("SELECT COUNT(*) from user")
 *     public abstract Integer getUserCount();
 *
 *     {@literal @}Query("SELECT * from user ORDER BY name DESC LIMIT :limit OFFSET :offset")
 *     public abstract List&lt;User> userNameLimitOffset(int limit, int offset);
 *
 *     {@literal @}Query("SELECT * from user WHERE name &lt; :key ORDER BY name DESC LIMIT :limit")
 *     public abstract List&lt;User> userNameLoadAfter(String key, int limit);
 *
 *     {@literal @}Query("SELECT * from user WHERE name > :key ORDER BY name ASC LIMIT :limit")
 *     public abstract List&lt;User> userNameLoadBefore(String key, int limit);
 * }
 *
 * public class KeyedUserQueryDataSource extends KeyedDataSource&lt;User> {
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
 * }
 * </pre>
 *
 * @param <Type> Type of items being loaded by the DataSource.
 */
public abstract class KeyedDataSource<Type> extends ContiguousDataSource<Type> {

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public List<Type> loadAfter(int currentEndIndex, @NonNull Type currentEndItem, int pageSize) {
        return loadAfter(currentEndItem, pageSize);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public List<Type> loadBefore(int currentBeginIndex, @NonNull Type currentBeginItem,
            int pageSize) {
        return loadBefore(currentBeginItem, pageSize);
    }

    /**
     * Load list data after the specified item.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param currentEndItem Load items after this item.
     * @param pageSize       Suggested number of items to load.
     * @return List of items, starting after the specified item. Null if the data source is
     * no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadAfter(@NonNull Type currentEndItem, int pageSize);

    /**
     * Load data before the currently loaded content, starting at the provided index.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     *
     * @param currentBeginItem Load items before this item.
     * @param pageSize         Suggested number of items to load.
     * @return List of items, in descending order, starting after the specified item. Null if the
     * data source is no longer valid, and should not be queried again.
     */
    @WorkerThread
    @Nullable
    public abstract List<Type> loadBefore(@NonNull Type currentBeginItem, int pageSize);
}
