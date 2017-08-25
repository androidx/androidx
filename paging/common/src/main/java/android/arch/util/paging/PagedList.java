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

import java.util.concurrent.Executor;

/**
 * Lazy loading list that pages in content from a provided {@link DataSource}.
 * <p>
 * A PagedList is a lazy loaded list, which presents data from a {@link DataSource}. If the
 * DataSource is counted (returns a valid number from {@link DataSource#loadCount()}), the PagedList
 * will present null items at the beginning and end of loaded content. As {@link #loadAround} is
 * called, items will be added to the beginning or end of the list as appropriate, and if nulls are
 * present, a corresponding number will be removed.
 * <p>
 * In this way, PagedList can present data for an infinite scrolling list, or a very large but
 * countable list. See {@link PagedListAdapter}, which enables the binding of a PagedList to a
 * RecyclerView. Use {@link Config} to control how a PagedList loads content.
 *
 * @param <T> The type of the entries in the list.
 */
public abstract class PagedList<T> {
    // Since we currently rely on implementation details of two implementations,
    // prevent external subclassing, except through exposed subclasses
    PagedList() {
    }

    /**
     * Create a PagedList which loads data from the provided data source on a background thread,
     * posting updates to the main thread.
     *
     *
     * @param dataSource DataSource providing data to the PagedList
     * @param mainThreadExecutor Thread that will use and consume data from the PagedList.
     *                           Generally, this is the UI/main thread.
     * @param backgroundThreadExecutor Data loading will be done via this executor - should be a
     *                                 background thread.
     * @param config PagedList Config, which defines how the PagedList will load data.
     * @param <K> Key type that indicates to the DataSource what data to load.
     * @param <T> Type of items to be held and loaded by the PagedList.
     *
     * @return Newly created PagedList, which will page in data from the DataSource as needed.
     */
    @NonNull
    static <K, T> PagedList<T> create(@NonNull DataSource<K, T> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @NonNull Config config,
            @Nullable K key) {

        if (dataSource.isContiguous()) {
            ContiguousDataSource<K, T> contigDataSource = (ContiguousDataSource<K, T>) dataSource;
            return new ContiguousPagedList<>(contigDataSource,
                    mainThreadExecutor,
                    backgroundThreadExecutor,
                    config,
                    key);
        } else {
            return new TiledPagedList<>((TiledDataSource<T>) dataSource,
                    mainThreadExecutor,
                    backgroundThreadExecutor,
                    config,
                    (key != null) ? (Integer) key : 0);
        }
    }

    /**
     * Get the item in the list of loaded items at the provided index.
     *
     * @param index Index in the loaded item list. Must be >= 0, and &lt; {@link #size()}
     * @return The item at the passed index, or null if a null placeholder is at the specified
     *         position.
     *
     * @see #size()
     */
    @Nullable
    public abstract T get(int index);


    /**
     * Load adjacent items to passed index.
     *
     * @param index Index at which to load.
     */
    public abstract void loadAround(int index);


    /**
     * Returns size of the list, including any not-yet-loaded null padding.
     *
     * @return Current total size of the list.
     */
    public abstract int size();

    /**
     * Returns whether the list is immutable. Immutable lists may not become mutable again, and may
     * safely be accessed from any thread.
     *
     * @return True if the PagedList is immutable, and will not become mutable again.
     */
    public abstract boolean isImmutable();

    /**
     * Returns an immutable snapshot of the PagedList. If this PagedList is already
     * immutable, it will be returned.
     *
     * @return Immutable snapshot of PagedList, which may be the PagedList itself.
     */
    public abstract PagedList<T> snapshot();

    abstract boolean isContiguous();

    int getLastLoad() {
        return 0;
    }

    T getLastItem() {
        return null;
    }

    boolean isDetached() {
        return true;
    }

    /**
     * Adds a callback, and issues updates since the previousSnapshot was created.
     * <p>
     * If previousSnapshot is passed, the callback will also immediately be dispatched any
     * differences between the previous snapshot, and the current state. For example, if the
     * previousSnapshot was of 5 nulls, 10 items, 5 nulls, and the current state was 5 nulls,
     * 12 items, 3 nulls, the callback would immediately receive a call of
     * <code>onChanged(14, 2)</code>.
     * <p>
     * This allows an observer that's currently presenting a snapshot to catch up to the most recent
     * version, including any changes that may have been made.
     *
     * @param previousSnapshot Snapshot previously captured from this List, or null.
     * @param callback         Callback to dispatch to.
     * @see #removeCallback(Callback)
     */
    public abstract void addCallback(@Nullable PagedList<T> previousSnapshot,
            @NonNull Callback callback);

    /**
     * Removes a previously added callback.
     *
     * @param callback Callback, previously added.
     * @see #addCallback(PagedList, Callback)
     */
    public abstract void removeCallback(Callback callback);

    /**
     * Callback signalling when content is loaded into the list.
     */
    public abstract static class Callback {
        /**
         * Called when null padding items have been loaded to signal newly available data, or when
         * data that hasn't been used in a while has been dropped, and swapped back to null.
         *
         * @param position Position of first newly loaded items, out of total number of items
         *                 (including padded nulls).
         * @param count    Number of items loaded.
         */
        public abstract void onChanged(int position, int count);

        /**
         * Called when new items have been loaded at the end or beginning of the list.
         *
         * @param position Position of the first newly loaded item (in practice, either
         *                 <code>0</code> or <code>size - 1</code>.
         * @param count    Number of items loaded.
         */
        public abstract void onInserted(int position, int count);

        /**
         * Called when items have been loaded at the end or beginnig of the list, and no padding
         * nulls remain.
         *
         * @param position Position of the first newly loaded item (in practice, either
         *                 <code>0</code> or <code>size - 1</code>.
         * @param count    Number of items loaded.
         */
        public abstract void onRemoved(int position, int count);
    }

    /**
     * Configures how a PagedList loads content from its DataSource.
     * <p>
     * Use a Config {@link Builder} to construct and define custom loading behavior, such as
     * {@link Builder#setPageSize(int)}, which defines number of items loaded at a time}.
     */
    public static class Config {
        final int mInitialLoadSize;
        final int mPageSize;
        final int mPrefetchDistance;

        private Config(int initialLoadSize, int pageSize, int prefetchDistance) {
            mInitialLoadSize = initialLoadSize;
            mPageSize = pageSize;
            mPrefetchDistance = prefetchDistance;
        }

        /**
         * Builder class for {@link Config}.
         * <p>
         * You must at minimum specify page size with {@link Builder#setPageSize(int)}.
         */
        public static class Builder {
            private int mInitialLoadSize = -1;
            private int mPageSize = -1;
            private int mPrefetchDistance = -1;

            /**
             * Defines how many items to load when first load occurs, if you are using a
             * {@link KeyedDataSource}.
             * <p>
             * If you are using an {@link TiledDataSource}, this value is ignored. Otherwise, this
             * value will be passed to {@link KeyedDataSource#loadAfterInitial(int, int)} to load a
             * (generally) larger amount of data on first load.
             * <p>
             * If not set, defaults to three times page size.
             */
            @SuppressWarnings("WeakerAccess")
            public Builder setInitialLoadSize(int initialLoadSize) {
                this.mInitialLoadSize = initialLoadSize;
                return this;
            }

            /**
             * Defines the number of items loaded at once from the DataSource.
             * <p>
             * Should be several times the number of visible items onscreen.
             */
            public Builder setPageSize(int pageSize) {
                this.mPageSize = pageSize;
                return this;
            }

            /**
             * Defines how far from the edge of loaded content an access must be to trigger further
             * loading. Defaults to page size.
             * <p>
             * A value of 0 indicates that no list items will be loaded before they are first
             * requested.
             * <p>
             * Should be several times the number of visible items onscreen.
             */
            public Builder setPrefetchDistance(int prefetchDistance) {
                this.mPrefetchDistance = prefetchDistance;
                return this;
            }

            /**
             * Creates a {@link Config} with the given parameters.
             *
             * @return A new Config.
             */
            public Config build() {
                if (mPageSize < 1) {
                    throw new IllegalArgumentException("Page size must be a positive number");
                }
                if (mPrefetchDistance < 0) {
                    mPrefetchDistance = mPageSize;
                }
                if (mInitialLoadSize < 0) {
                    mInitialLoadSize = mPageSize * 3;
                }
                return new Config(mInitialLoadSize, mPageSize, mPrefetchDistance);
            }
        }
    }
}
