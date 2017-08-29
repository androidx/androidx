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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.AbstractList;
import java.util.List;
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
public abstract class PagedList<T> extends AbstractList<T> {
    // Since we currently rely on implementation details of two implementations,
    // prevent external subclassing
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
    private static <K, T> PagedList<T> create(@NonNull DataSource<K, T> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @NonNull Config config,
            @Nullable K key) {

        if (dataSource.isContiguous() || !config.mEnablePlaceholders) {
            if (!dataSource.isContiguous()) {
                dataSource = (DataSource<K, T>) ((TiledDataSource<T>) dataSource).getAsContiguous();
            }
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

    @SuppressWarnings("WeakerAccess")
    public static class Builder<Key, Value> {
        private DataSource<Key, Value> mDataSource;
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;
        private Config mConfig;
        private Key mInitialKey;

        @NonNull
        public Builder<Key, Value> setDataSource(@NonNull DataSource<Key, Value> dataSource) {
            mDataSource = dataSource;
            return this;
        }

        @NonNull
        public Builder<Key, Value> setMainThreadExecutor(@NonNull Executor mainThreadExecutor) {
            mMainThreadExecutor = mainThreadExecutor;
            return this;
        }

        @NonNull
        public Builder<Key, Value> setBackgroundThreadExecutor(
                @NonNull Executor backgroundThreadExecutor) {
            mBackgroundThreadExecutor = backgroundThreadExecutor;
            return this;
        }

        @NonNull
        public Builder<Key, Value> setConfig(@NonNull Config config) {
            mConfig = config;
            return this;
        }

        @NonNull
        public Builder<Key, Value> setInitialKey(@Nullable Key initialKey) {
            mInitialKey = initialKey;
            return this;
        }

        @NonNull
        public PagedList<Value> build() {
            if (mDataSource == null) {
                throw new IllegalArgumentException("DataSource required");
            }
            if (mMainThreadExecutor == null) {
                throw new IllegalArgumentException("MainThreadExecutor required");
            }
            if (mBackgroundThreadExecutor == null) {
                throw new IllegalArgumentException("BackgroundThreadExecutor required");
            }
            if (mConfig == null) {
                throw new IllegalArgumentException("Config required");
            }

            return PagedList.create(
                    mDataSource,
                    mMainThreadExecutor,
                    mBackgroundThreadExecutor,
                    mConfig,
                    mInitialKey);
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
    @Override
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
    @Override
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
    public abstract List<T> snapshot();

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
         * Called when items have been removed at the end or beginning of the list, and have not
         * been replaced by padded nulls.
         *
         * @param position Position of the first newly loaded item (in practice, either
         *                 <code>0</code> or <code>size - 1</code>.
         * @param count    Number of items loaded.
         */
        @SuppressWarnings("unused")
        public abstract void onRemoved(int position, int count);
    }

    /**
     * Configures how a PagedList loads content from its DataSource.
     * <p>
     * Use a Config {@link Builder} to construct and define custom loading behavior, such as
     * {@link Builder#setPageSize(int)}, which defines number of items loaded at a time}.
     */
    public static class Config {
        final int mPageSize;
        final int mPrefetchDistance;
        final boolean mEnablePlaceholders;
        final int mInitialLoadSizeHint;

        private Config(int pageSize, int prefetchDistance,
                boolean enablePlaceholders, int initialLoadSizeHint) {
            mPageSize = pageSize;
            mPrefetchDistance = prefetchDistance;
            mEnablePlaceholders = enablePlaceholders;
            mInitialLoadSizeHint = initialLoadSizeHint;
        }

        /**
         * Builder class for {@link Config}.
         * <p>
         * You must at minimum specify page size with {@link Builder#setPageSize(int)}.
         */
        public static class Builder {
            private int mPageSize = -1;
            private int mPrefetchDistance = -1;
            private int mInitialLoadSizeHint = -1;
            private boolean mEnablePlaceholders = true;

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
             * Pass false to disable null placeholders in PagedLists using this Config.
             * <p>
             * A PagedList will present null placeholders for not yet loaded content if two
             * contitions are met:
             * <p>
             * 1) Its DataSource can count all unloaded items (so that the number of nulls to
             * present is known).
             * <p>
             * 2) placeholders are not disabled on the Config.
             * <p>
             * Call {@code setEnablePlaceholders(false)} to ensure the receiver of the PagedList
             * (often a {@link PagedListAdapter)} doesn't need to account for null items.
             */
            @SuppressWarnings("SameParameterValue")
            public Builder setEnablePlaceholders(boolean enablePlaceholders) {
                this.mEnablePlaceholders = enablePlaceholders;
                return this;
            }

            /**
             * Defines how many items to load when first load occurs, if you are using a
             * {@link KeyedDataSource}.
             * <p>
             * If you are using an {@link TiledDataSource}, this value is currently ignored.
             * Otherwise, this value will be passed to
             * {@link KeyedDataSource#loadInitial(Object, int)} to load a (typically) larger amount
             * of data on first load.
             * <p>
             * If not set, defaults to three times page size.
             */
            @SuppressWarnings("WeakerAccess")
            public Builder setInitialLoadSizeHint(int initialLoadSizeHint) {
                this.mInitialLoadSizeHint = initialLoadSizeHint;
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
                if (mInitialLoadSizeHint < 0) {
                    mInitialLoadSizeHint = mPageSize * 3;
                }
                if (!mEnablePlaceholders && mPrefetchDistance == 0) {
                    throw new IllegalArgumentException("Placeholders and prefetch are the only ways"
                            + " to trigger loading of more data in the PagedList, so either"
                            + " placeholders must be enabled, or prefetch distance must be > 0.");
                }


                return new Config(mPageSize, mPrefetchDistance,
                        mEnablePlaceholders, mInitialLoadSizeHint);
            }
        }
    }
}
