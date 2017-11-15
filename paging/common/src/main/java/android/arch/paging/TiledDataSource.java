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
import android.support.annotation.WorkerThread;

import java.util.Collections;
import java.util.List;

/**
 * Position-based data loader for fixed size, arbitrary positioned loading.
 * <p>
 * Extend TiledDataSource if you want to load arbitrary pages based solely on position information,
 * and can generate pages of a provided fixed size.
 * <p>
 * Room can generate a TiledDataSource for you:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY mAge DESC")
 *     public abstract TiledDataSource&lt;User> loadUsersByAgeDesc();
 * }</pre>
 *
 * Under the hood, Room will generate code equivalent to the below, using a limit/offset SQL query:
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
 * public class OffsetUserQueryDataSource extends TiledDataSource&lt;User> {
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
 *     public int countItems() {
 *         return mUserDao.getUserCount();
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;User> loadRange(int startPosition, int loadCount) {
 *         return mUserDao.userNameLimitOffset(loadCount, startPosition);
 *     }
 * }</pre>
 *
 * @param <Type> Type of items being loaded by the TiledDataSource.
 */
public abstract class TiledDataSource<Type> extends DataSource<Integer, Type> {

    private int mItemCount;

    /**
     * Number of items that this DataSource can provide in total.
     *
     * @return Number of items this DataSource can provide. Must be <code>0</code> or greater.
     */
    @WorkerThread
    public abstract int countItems();

    @Override
    boolean isContiguous() {
        return false;
    }

    /**
     * Called to load items at from the specified position range.
     * <p>
     * This method must return a list of requested size, unless at the end of list. Fixed size pages
     * enable TiledDataSource to navigate tiles efficiently, and quickly accesss any position in the
     * data set.
     * <p>
     * If a list of a different size is returned, but it is not the last list in the data set based
     * on the return value from {@link #countItems()}, an exception will be thrown.
     *
     * @param startPosition Index of first item to load.
     * @param count         Number of items to load.
     * @return List of loaded items, of the requested length unless at end of list. Null if the
     *         DataSource is no longer valid, and should not be queried again.
     */
    @WorkerThread
    public abstract List<Type> loadRange(int startPosition, int count);

    /**
     * blocking, and splits pages
     */
    void loadRangeInitial(int startPosition, int count, int pageSize, int itemCount,
            PageResult.Receiver<Integer, Type> receiver) {
        mItemCount = itemCount;

        if (itemCount == 0) {
            // no data to load, just immediately return empty
            receiver.onPageResult(new PageResult<>(
                    PageResult.INIT, new Page<Integer, Type>(Collections.<Type>emptyList()),
                    0, 0, startPosition));
            return;
        }

        List<Type> list = loadRangeWrapper(startPosition, count);

        count = Math.min(count, itemCount - startPosition);

        if (list == null) {
            // invalid data, pass to receiver
            receiver.onPageResult(new PageResult<Integer, Type>(
                    PageResult.INIT, null, 0, 0, startPosition));
            return;
        }

        if (list.size() != count) {
            throw new IllegalStateException("Invalid list, requested size: " + count
                    + ", returned size: " + list.size());
        }

        // emit the results as multiple pages
        int pageCount = (count + (pageSize - 1)) / pageSize;
        for (int i = 0; i < pageCount; i++) {
            int beginInclusive = i * pageSize;
            int endExclusive = Math.min(count, (i + 1) * pageSize);

            Page<Integer, Type> page = new Page<>(list.subList(beginInclusive, endExclusive));

            int leadingNulls = startPosition + beginInclusive;
            int trailingNulls = itemCount - leadingNulls - page.items.size();
            receiver.onPageResult(new PageResult<>(
                    PageResult.INIT, page, leadingNulls, trailingNulls, 0));
        }
    }

    void loadRange(int startPosition, int count, PageResult.Receiver<Integer, Type> receiver) {
        List<Type> list = loadRangeWrapper(startPosition, count);

        Page<Integer, Type> page = null;
        int trailingNulls = mItemCount - startPosition;

        if (list != null) {
            page = new Page<Integer, Type>(list);
            trailingNulls -= list.size();
        }
        receiver.postOnPageResult(new PageResult<>(
                PageResult.TILE, page, startPosition, trailingNulls, 0));
    }

    private List<Type> loadRangeWrapper(int startPosition, int count) {
        if (isInvalid()) {
            return null;
        }
        List<Type> list = loadRange(startPosition, count);
        if (isInvalid()) {
            return null;
        }
        return list;
    }

    ContiguousDataSource<Integer, Type> getAsContiguous() {
        return new TiledAsBoundedDataSource<>(this);
    }

    static class TiledAsBoundedDataSource<Value> extends BoundedDataSource<Value> {
        final TiledDataSource<Value> mTiledDataSource;

        TiledAsBoundedDataSource(TiledDataSource<Value> tiledDataSource) {
            mTiledDataSource = tiledDataSource;
        }

        @WorkerThread
        @Nullable
        @Override
        public List<Value> loadRange(int startPosition, int loadCount) {
            return mTiledDataSource.loadRange(startPosition, loadCount);
        }
    }
}
