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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.AnyThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for loading pages of snapshot data into a {@link PagedList}.
 * <p>
 * DataSource is queried to load pages of content into a {@link PagedList}. A PagedList can grow as
 * it loads more data, but the data loaded cannot be updated.
 * <p>
 * A PagedList / DataSource pair serve as a snapshot of the data set being loaded. If the
 * underlying data set is modified, a new PagedList / DataSource pair must be created to represent
 * the new data.
 * <h4>Loading Pages</h4>
 * PagedList queries data from its DataSource in response to loading hints. {@link PagedListAdapter}
 * calls {@link PagedList#loadAround(int)} to load content as the user scrolls in a RecyclerView.
 * <p>
 * To control how and when a PagedList queries data from its DataSource, see
 * {@link PagedList.Config}. The Config object defines things like load sizes and prefetch distance.
 * <h4>Updating Paged Data</h4>
 * A PagedList / DataSource pair are a snapshot of the data set. A new pair of
 * PagedList / DataSource must be created if an update occurs, such as a reorder, insert, delete, or
 * content update occurs. A DataSource must detect that it cannot continue loading its
 * snapshot (for instance, when Database query notices a table being invalidated), and call
 * {@link #invalidate()}. Then a new PagedList / DataSource pair would be created to load data from
 * the new state of the Database query.
 * <p>
 * To page in data that doesn't update, you can create a single DataSource, and pass it to a single
 * PagedList. For example, loading from network when the network's paging API doesn't provide
 * updates.
 * <p>
 * To page in data from a source that does provide updates, you can create a
 * {@link DataSource.Factory}, where each DataSource created is invalidated when an update to the
 * data set occurs that makes the current snapshot invalid. For example, when paging a query from
 * the Database, and the table being queried inserts or removes items. You can also use a
 * DataSource.Factory to provide multiple versions of network-paged lists. If reloading all content
 * (e.g. in response to an action like swipe-to-refresh) is required to get a new version of data,
 * you can connect an explicit refresh signal to call {@link #invalidate()} on the current
 * DataSource.
 * <p>
 * If you have more granular update signals, such as a network API signaling an update to a single
 * item in the list, it's recommended to load data from network into memory. Then present that
 * data to the PagedList via a DataSource that wraps an in-memory snapshot. Each time the in-memory
 * copy changes, invalidate the previous DataSource, and a new one wrapping the new state of the
 * snapshot can be created.
 * <h4>Implementing a DataSource</h4>
 * To implement, extend either the {@link KeyedDataSource}, or {@link PositionalDataSource}
 * subclass. Choose based on whether each load operation is based on the position of the data in the
 * list.
 * <p>
 * Use {@link KeyedDataSource} if you need to use data from item {@code N-1} to load item
 * {@code N}. For example, if requesting the backend for the next comments in the list
 * requires the ID or timestamp of the most recent loaded comment, or if querying the next users
 * from a name-sorted database query requires the name and unique ID of the previous.
 * <p>
 * Use {@link PositionalDataSource} if you can load arbitrary pages based solely on position
 * information, and can provide a fixed item count. PositionalDataSource supports querying pages at
 * arbitrary positions, so can provide data to PagedLists in arbitrary order.
 * <p>
 * Because a {@code null} item indicates a placeholder in {@link PagedList}, DataSource may not
 * return {@code null} items in lists that it loads. This is so that users of the PagedList
 * can differentiate unloaded placeholder items from content that has been paged in.
 *
 * @param <Key> Input used to trigger initial load from the DataSource. Often an Integer position.
 * @param <Value> Value type loaded by the DataSource.
 */
@SuppressWarnings("unused") // suppress warning to remove Key/Value, needed for subclass type safety
public abstract class DataSource<Key, Value> {
    /**
     * Factory for DataSources.
     * <p>
     * Data-loading systems of an application or library can implement this interface to allow
     * {@code LiveData<PagedList>}s to be created. For example, Room can provide a
     * DataSource.Factory for a given SQL query:
     *
     * <pre>
     * {@literal @}Dao
     * interface UserDao {
     *    {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
     *    public abstract DataSource.Factory&lt;Integer, User> usersByLastName();
     * }
     * </pre>
     * In the above sample, {@code Integer} is used because it is the {@code Key} type of
     * PositionalDataSource. Currently, Room uses the {@code LIMIT}/{@code OFFSET} SQL keywords to
     * page a large query with a PositionalDataSource.
     *
     * @param <Key> Key identifying items in DataSource.
     * @param <Value> Type of items in the list loaded by the DataSources.
     */
    public interface Factory<Key, Value> {
        /**
         * Create a DataSource.
         * <p>
         * The DataSource should invalidate itself if the snapshot is no longer valid, and a new
         * DataSource should be queried from the Factory.
         *
         * @return the new DataSource.
         */
        DataSource<Key, Value> create();
    }

    // Since we currently rely on implementation details of two implementations,
    // prevent external subclassing, except through exposed subclasses
    DataSource() {
    }

    /**
     * Returns true if the data source guaranteed to produce a contiguous set of items,
     * never producing gaps.
     */
    abstract boolean isContiguous();

    /**
     * Callback for DataSource initial loading methods to return data and position/count
     * information.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public static class InitialLoadCallback<T> extends LoadCallback<T> {
        private final int mPageSize;

        InitialLoadCallback(@LoadCountType int countType, int pageSize,
                DataSource dataSource, PageResult.Receiver<T> receiver) {
            super(PageResult.INIT, countType, dataSource, receiver);
            mPageSize = pageSize;
            if (mPageSize < 1) {
                throw new IllegalArgumentException("Page size must be non-negative");
            }
        }

        /**
         * Called to pass initial load state from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * and inform how many placeholders should be shown before and after. If counting is cheap
         * to compute (for example, if a network load returns the information regardless), it's
         * recommended to pass data back through this method.
         *
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list, relative to the total
         *                 count. If there are {@code N} items before the items in data that can be
         *                 loaded from this DataSource, pass {@code N}.
         * @param totalCount Total number of items that may be returned from this DataSource.
         *                   Includes the number in the initial {@code data} parameter
         *                   as well as any items that can be loaded in front or behind of
         *                   {@code data}.
         */
        public void onResult(@NonNull List<T> data, int position, int totalCount) {
            if (position < 0) {
                throw new IllegalArgumentException("Position must be non-negative");
            }
            if (data.size() + position > totalCount) {
                throw new IllegalArgumentException(
                        "List size + position too large; last item in list beyond totalCount");
            }
            if (data.size() == 0 && totalCount > 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
            if (mCountType == LOAD_COUNT_REQUIRED_TILED
                    && position + data.size() != totalCount
                    && data.size() % mPageSize != 0) {
                throw new IllegalArgumentException("PositionalDataSource requires initial load size"
                        + " to be a multiple of page size to support internal tiling.");
            }

            int trailingUnloadedCount = totalCount - position - data.size();
            if (mCountType == LOAD_COUNT_REQUIRED_TILED || mCountType == LOAD_COUNT_ACCEPTED) {
                dispatchResultToReceiver(new PageResult<>(
                        data, position, trailingUnloadedCount, 0));
            } else {
                dispatchResultToReceiver(new PageResult<>(data, position));
            }
        }

        /**
         * Called to pass initial load state from a DataSource without supporting placeholders.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * if position is known but total size is not. If counting is not expensive, consider
         * calling the three parameter variant: {@link #onResult(List, int, int)}.
         *
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are {@code N}
         *                 items before the items in data that can be provided by this DataSource,
         *                 pass {@code N}.
         */
        void onResult(@NonNull List<T> data, int position) {
            // not counting, don't need to check mAcceptCount
            dispatchResultToReceiver(new PageResult<>(
                    data, 0, 0, position));
        }
    }

    @Retention(SOURCE)
    @IntDef({LOAD_COUNT_PREVENTED, LOAD_COUNT_ACCEPTED, LOAD_COUNT_REQUIRED_TILED})
    @interface LoadCountType {}
    static final int LOAD_COUNT_PREVENTED = 0;
    static final int LOAD_COUNT_ACCEPTED = 1;
    static final int LOAD_COUNT_REQUIRED_TILED = 2;

    /**
     * Callback for DataSource loading methods to return data.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public static class LoadCallback<T> {
        @PageResult.ResultType
        final int mResultType;
        @LoadCountType
        final int mCountType;
        private final DataSource mDataSource;
        private final PageResult.Receiver<T> mReceiver;

        private int mPositionOffset = 0;

        // mSignalLock protects mPostExecutor, and mHasSignalled
        private final Object mSignalLock = new Object();
        private Executor mPostExecutor = null;
        private boolean mHasSignalled = false;

        private LoadCallback(@PageResult.ResultType int resultType, @LoadCountType int countType,
                DataSource dataSource, PageResult.Receiver<T> receiver) {
            mResultType = resultType;
            mCountType = countType;
            mDataSource = dataSource;
            mReceiver = receiver;
        }

        LoadCallback(int type, Executor mainThreadExecutor,
                DataSource dataSource, PageResult.Receiver<T> receiver) {
            mResultType = type;
            mCountType = LOAD_COUNT_PREVENTED;
            mPostExecutor = mainThreadExecutor;
            mDataSource = dataSource;
            mReceiver = receiver;
        }

        void setPositionOffset(int positionOffset) {
            mPositionOffset = positionOffset;
        }

        void setPostExecutor(Executor postExecutor) {
            synchronized (mSignalLock) {
                mPostExecutor = postExecutor;
            }
        }

        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code load} methods to return data.
         *
         * @param data List of items loaded from the DataSource.
         */
        public void onResult(@NonNull List<T> data) {
            if (mCountType == LOAD_COUNT_REQUIRED_TILED && !data.isEmpty()) {
                throw new IllegalArgumentException(
                        "PositionalDataSource requires calling the three argument version of"
                                + " InitialLoadCallback.onResult() to pass position information");
            }
            dispatchResultToReceiver(new PageResult<>(
                    data, 0, 0, mPositionOffset));
        }

        void dispatchResultToReceiver(final @NonNull PageResult<T> result) {
            Executor executor;
            synchronized (mSignalLock) {
                if (mHasSignalled) {
                    throw new IllegalStateException(
                            "LoadCallback already dispatched, cannot dispatch again.");
                }
                mHasSignalled = true;
                executor = mPostExecutor;
            }

            final PageResult<T> resolvedResult =
                    mDataSource.isInvalid() ? PageResult.<T>getInvalidResult() : result;

            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mReceiver.onPageResult(mResultType, result);
                    }
                });
            } else {
                mReceiver.onPageResult(mResultType, result);
            }
        }
    }

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
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mOnInvalidatedCallbacks.add(onInvalidatedCallback);
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    @AnyThread
    @SuppressWarnings("WeakerAccess")
    public void removeInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
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
