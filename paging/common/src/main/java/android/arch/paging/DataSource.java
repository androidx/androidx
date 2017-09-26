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
import android.support.annotation.WorkerThread;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for incremental data loading, used in list paging. To implement, extend either the
 * {@link KeyedDataSource}, or {@link TiledDataSource} subclass.
 * <p>
 * Choose based on whether each load operation is based on the position of the data in the list.
 * <p>
 * Use {@link KeyedDataSource} if you need to use data from item <code>N-1</code> to load item
 * <code>N</code>. For example, if requesting the backend for the next comments in the list
 * requires the ID or timestamp of the most recent loaded comment, or if querying the next users
 * from a name-sorted database query requires the name and unique ID of the previous.
 * <p>
 * Use {@link TiledDataSource} if you can load arbitrary pages based solely on position information,
 * and can provide a fixed item count. TiledDataSource supports querying pages at arbitrary
 * positions, so can provide data to PagedLists in arbitrary order.
 * <p>
 * Because a <code>null</code> item indicates a placeholder in {@link PagedList}, DataSource may not
 * return <code>null</code> items in lists that it loads. This is so that users of the PagedList
 * can differentiate unloaded placeholder items from content that has been paged in.
 *
 * @param <Key> Input used to trigger initial load from the DataSource. Often an Integer position.
 * @param <Value> Value type loaded by the DataSource.
 */
@SuppressWarnings("unused") // suppress warning to remove Key/Value, needed for subclass type safety
public abstract class DataSource<Key, Value> {

    // Since we currently rely on implementation details of two implementations,
    // prevent external subclassing, except through exposed subclasses
    DataSource() {
    }

    /**
     * If returned by countItems(), indicates an undefined number of items are provided by the data
     * source. Continued querying in either direction may continue to produce more data.
     */
    @SuppressWarnings("WeakerAccess")
    public static int COUNT_UNDEFINED = -1;

    /**
     * Number of items that this DataSource can provide in total, or {@link #COUNT_UNDEFINED}.
     *
     * @return number of items that this DataSource can provide in total, or
     * {@link #COUNT_UNDEFINED} if expensive or undesired to compute.
     */
    @WorkerThread
    public abstract int countItems();

    /**
     * Returns true if the data source guaranteed to produce a contiguous set of items,
     * never producing gaps.
     */
    abstract boolean isContiguous();

    /**
     * Invalidation callback for DataSource.
     * <p>
     * Used to signal when a DataSource a data source has become invalid, and that a new data source
     * is needed to continue loading data.
     */
    public interface InvalidatedCallback {
        /**
         * Called when the data backing the list has become invalid. This callback is typically used
         * to signal that a new data source is needed.
         * <p>
         * This callback will be invoked on the thread that calls {@link #invalidate()}. It is valid
         * for the data source to invalidate itself during its load methods, or for an outside
         * source to invalidate it.
         */
        @AnyThread
        void onInvalidated();
    }

    private AtomicBoolean mInvalid = new AtomicBoolean(false);

    private CopyOnWriteArrayList<InvalidatedCallback> mOnInvalidatedCallbacks =
            new CopyOnWriteArrayList<>();

    /**
     * Add a callback to invoke when the DataSource is first invalidated.
     * <p>
     * Once invalidated, a data source will not become valid again.
     * <p>
     * A data source will only invoke its callbacks once - the first time {@link #invalidate()}
     * is called, on that thread.
     *
     * @param onInvalidatedCallback The callback, will be invoked on thread that
     *                              {@link #invalidate()} is called on.
     */
    @AnyThread
    @SuppressWarnings("WeakerAccess")
    public void addInvalidatedCallback(InvalidatedCallback onInvalidatedCallback) {
        mOnInvalidatedCallbacks.add(onInvalidatedCallback);
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    @AnyThread
    @SuppressWarnings("WeakerAccess")
    public void removeInvalidatedCallback(InvalidatedCallback onInvalidatedCallback) {
        mOnInvalidatedCallbacks.remove(onInvalidatedCallback);
    }

    /**
     * Signal the data source to stop loading, and notify its callback.
     * <p>
     * If invalidate has already been called, this method does nothing.
     */
    @AnyThread
    public void invalidate() {
        if (mInvalid.compareAndSet(false, true)) {
            for (InvalidatedCallback callback : mOnInvalidatedCallbacks) {
                callback.onInvalidated();
            }
        }
    }

    /**
     * Returns true if the data source is invalid, and can no longer be queried for data.
     *
     * @return True if the data source is invalid, and can no longer return data.
     */
    @WorkerThread
    public boolean isInvalid() {
        return mInvalid.get();
    }
}
