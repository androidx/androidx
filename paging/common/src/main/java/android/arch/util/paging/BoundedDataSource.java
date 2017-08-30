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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplest data source form that provides all of its data through a single loadRange() method.
 * <p>
 * Requires that your data resides in positions <code>0</code> through <code>N</code>, where
 * <code>N</code> is the value returned from {@link #loadCount()}. You must return the exact number
 * requested, so that the data as returned can be safely prepended/appended to what has already
 * been loaded.
 * <p>
 * For more flexibility in how many items to load, or to avoid counting your data source, override
 * {@link PositionalDataSource} directly.
 * <p>
 * A compute usage pattern with Room limit/offset SQL queries would look like this (though note,
 * this is just for illustration - Room can generate this for you):
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT COUNT(*) from user")
 *     public abstract Integer getUserCount();
 *
 *     {@literal @}Query("SELECT * from user ORDER BY mName DESC LIMIT :limit OFFSET :offset")
 *     public abstract List&lt;User> userNameLimitOffset(int limit, int offset);
 * }
 *
 * public class OffsetUserQueryDataSource extends BoundedDataSource&lt;User> {
 *     {@literal @}Override
 *     public int loadCount() {
 *         return mUserDao.getUserCount();
 *     }
 *
 *     {@literal @}Nullable
 *     {@literal @}Override
 *     public List&lt;User> loadRange(int startPosition, int loadCount) {
 *         return mUserDao.userNameLimitOffset(loadCount, startPosition);
 *     }
 * }</pre>
 *
 * Room can generate this DataSource for you:
 *
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY mAge DESC")
 *     public abstract DataSource&lt;User> loadUsersByAgeDesc();
 * }</pre>
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
    @Nullable
    public abstract List<Value> loadRange(int startPosition, int loadCount);

    @Nullable
    @Override
    public List<Value> loadAfter(int startIndex, int pageSize) {
        if (mCount < 0) {
            // TODO: cleanup this testing-only hack by fixing tests
            mCount = loadCount();
        }
        if (mCount == COUNT_UNDEFINED) {
            throw new IllegalStateException("SimpleBounded requires a counted data source"
                    + " to return a bounding value from loadCount()");
        }

        if (startIndex >= mCount) {
            return new ArrayList<>();
        }
        int loadSize = Math.min(pageSize, mCount - startIndex);
        List<Value> result = loadRange(startIndex, loadSize);
        if (result != null && result.size() != loadSize) {
            throw new IllegalStateException("invalid number of items returned.");
        }
        return result;
    }

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
