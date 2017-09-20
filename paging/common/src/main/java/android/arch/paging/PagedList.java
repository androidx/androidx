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
import android.support.annotation.WorkerThread;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Lazy loading list that pages in content from a {@link DataSource}.
 * <p>
 * A PagedList is a {@link List} which loads its data in chunks (pages) from a {@link DataSource}.
 * Items can be accessed with {@link #get(int)}, and further loading can be triggered with
 * {@link #loadAround(int)}. See {@link PagedListAdapter}, which enables the binding of a PagedList
 * to a {@link android.support.v7.widget.RecyclerView}.
 * <h4>Loading Data</h4>
 * <p>
 * All data in a PagedList is loaded from its {@link DataSource}. Creating a PagedList loads data
 * from the DataSource immediately, and should for this reason be done on a background thread. The
 * constructed PagedList may then be passed to and used on the UI thread. This is done to prevent
 * passing a list with no loaded content to the UI thread, which should generally not be presented
 * to the user.
 * <p>
 * When {@link #loadAround} is called, items will be loaded in near the passed list index. If
 * placeholder {@code null}s are present in the list, they will be replaced as content is
 * loaded. If not, newly loaded items will be inserted at the beginning or end of the list.
 * <p>
 * PagedList can present data for an unbounded, infinite scrolling list, or a very large but
 * countable list. Use {@link Config} to control how many items a PagedList loads, and when.
 * <p>
 * If you use {@link LivePagedListProvider} to get a
 * {@link android.arch.lifecycle.LiveData}&lt;PagedList>, it will initialize PagedLists on a
 * background thread for you.
 * <h4>Placeholders</h4>
 * <p>
 * There are two ways that PagedList can represent its not-yet-loaded data - with or without
 * {@code null} placeholders.
 * <p>
 * With placeholders, the PagedList is always the full size of the data set. {@code get(N)} returns
 * the {@code N}th item in the data set, or {@code null} if its not yet loaded.
 * <p>
 * Without {@code null} placeholders, the PagedList is the sublist of data that has already been
 * loaded. The size of the PagedList is the number of currently loaded items, and {@code get(N)}
 * returns the {@code N}th <em>loaded</em> item. This is not necessarily the {@code N}th item in the
 * data set.
 * <p>
 * Placeholders have several benefits:
 * <ul>
 *     <li>They express the full sized list to the presentation layer (often a
 *     {@link PagedListAdapter}), and so can support scrollbars (without jumping as pages are
 *     loaded) and fast-scrolling to any position, whether loaded or not.
 *     <li>They avoid the need for a loading spinner at the end of the loaded list, since the list
 *     is always full sized.
 * </ul>
 * <p>
 * They also have drawbacks:
 * <ul>
 *     <li>Your Adapter (or other presentation mechanism) needs to account for {@code null} items.
 *     This often means providing default values in data you bind to a
 *     {@link android.support.v7.widget.RecyclerView.ViewHolder}.
 *     <li>They don't work well if your item views are of different sizes, as this will prevent
 *     loading items from cross-fading nicely.
 *     <li>They require you to count your data set, which can be expensive or impossible, depending
 *     on where your data comes from.
 * </ul>
 * <p>
 * Placeholders are enabled by default, but can be disabled in two ways. They are disabled if the
 * DataSource returns {@link DataSource#COUNT_UNDEFINED} from any item counting method, or if
 * {@code false} is passed to {@link Config.Builder#setEnablePlaceholders(boolean)} when building a
 * {@link Config}.
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
                //noinspection unchecked
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

    /**
     * Builder class for PagedList.
     * <p>
     * DataSource, main thread and background executor, and Config must all be provided.
     * <p>
     * A valid PagedList may not be constructed without data, so building a PagedList queries
     * initial data from the data source. This is done because it's generally undesired to present a
     * PagedList with no data in it to the UI. It's better to present initial data, so that the UI
     * doesn't show an empty list, or placeholders for a few frames, just before showing initial
     * content.
     * <p>
     * Because PagedLists are initialized with data, PagedLists must be built on a background
     * thread.
     * <p>
     * {@link LivePagedListProvider} does this creation on a background thread automatically, if you
     * want to receive a {@code LiveData<PagedList<...>>}.
     *
     * @param <Key> Type of key used to load data from the DataSource.
     * @param <Value> Type of items held and loaded by the PagedList.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder<Key, Value> {
        private DataSource<Key, Value> mDataSource;
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;
        private Config mConfig;
        private Key mInitialKey;

        /**
         * The source of data that the PagedList should load from.
         * @param dataSource Source of data for the PagedList.
         *
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setDataSource(@NonNull DataSource<Key, Value> dataSource) {
            mDataSource = dataSource;
            return this;
        }

        /**
         * The executor defining where main/UI thread for page loading updates.
         *
         * @param mainThreadExecutor Executor for main/UI thread to receive {@link Callback} calls.
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setMainThreadExecutor(@NonNull Executor mainThreadExecutor) {
            mMainThreadExecutor = mainThreadExecutor;
            return this;
        }

        /**
         * The executor on which background loading will be run.
         * <p>
         * Does not affect initial load, which will be done on whichever thread the PagedList is
         * created on.
         *
         * @param backgroundThreadExecutor Executor for background DataSource loading.
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setBackgroundThreadExecutor(
                @NonNull Executor backgroundThreadExecutor) {
            mBackgroundThreadExecutor = backgroundThreadExecutor;
            return this;
        }

        /**
         * The Config defining how the PagedList should load from the DataSource.
         *
         * @param config The config that will define how the PagedList loads from the DataSource.
         *
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setConfig(@NonNull Config config) {
            mConfig = config;
            return this;
        }

        /**
         * Sets the initial key the DataSource should load around as part of initialization.
         *
         * @param initialKey Key the DataSource should load around as part of initialization.
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setInitialKey(@Nullable Key initialKey) {
            mInitialKey = initialKey;
            return this;
        }

        /**
         * Creates a {@link PagedList} with the given parameters.
         * <p>
         * This call will initial data and perform any counting needed to initialize the PagedList,
         * therefore it should only be called on a worker thread.
         * <p>
         * While build() will always return a PagedList, it's important to note that the PagedList
         * initial load may fail to acquire data from the DataSource. This can happen for example if
         * the DataSource is invalidated during its initial load. If this happens, the PagedList
         * will be immediately {@link PagedList#isDetached() detached}, and you can retry
         * construction (including setting a new DataSource).
         *
         * @return The newly constructed PagedList
         */
        @WorkerThread
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
     * @return True if the PagedList is immutable.
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

    /**
     * Return the key for the position passed most recently to {@link #loadAround(int)}.
     * <p>
     * When a PagedList is invalidated, you can pass the key returned by this function to initialize
     * the next PagedList. This ensures (depending on load times) that the next PagedList that
     * arrives will have data that overlaps. If you use {@link LivePagedListProvider}, it will do
     * this for you.
     *
     * @return Key of position most recently passed to {@link #loadAround(int)}.
     */
    @Nullable
    public Object getLastKey() {
        return null;
    }

    /**
     * True if the PagedList has detached the DataSource it was loading from, and will no longer
     * load new data.
     *
     * @return True if the data source is detached.
     */
    public boolean isDetached() {
        return true;
    }

    /**
     * Detach the PagedList from its DataSource, and attempt to load no more data.
     * <p>
     * This is called automatically when a DataSource load returns <code>null</code>, which is a
     * signal to stop loading. The PagedList will continue to present existing data, but will not
     * initiate new loads.
     */
    public void detach() {
    }

    /**
     * Position offset of the data in the list.
     * <p>
     * If data is supplied by a {@link TiledDataSource}, the item returned from <code>get(i)</code>
     * has a position of <code>i + getPositionOffset()</code>.
     * <p>
     * If the DataSource is a {@link KeyedDataSource}, and thus doesn't use positions, returns 0.
     */
    public int getPositionOffset() {
        return 0;
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
     * <p>
     * The callback is internally held as weak reference, so PagedList doesn't hold a strong
     * reference to its observer, such as a {@link PagedListAdapter}. If an adapter were held with a
     * strong reference, it would be necessary to clear its PagedList observer before it could be
     * GC'd.
     *
     * @param previousSnapshot Snapshot previously captured from this List, or null.
     * @param callback         Callback to dispatch to.
     * @see #removeWeakCallback(Callback)
     */
    public abstract void addWeakCallback(@Nullable PagedList<T> previousSnapshot,
            @NonNull Callback callback);

    /**
     * Removes a previously added callback.
     *
     * @param callback Callback, previously added.
     * @see #addWeakCallback(PagedList, Callback)
     */
    public abstract void removeWeakCallback(Callback callback);

    /**
     * Callback signaling when content is loaded into the list.
     * <p>
     * Can be used to listen to items being paged in and out. These calls will be dispatched on
     * the executor defined by {@link Builder#setMainThreadExecutor(Executor)}, which defaults to
     * the main/UI thread.
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
         * You must at minimum specify page size with {@link #setPageSize(int)}.
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
             * <p>
             * Configuring your page size depends on how your data is being loaded and used. Smaller
             * page sizes improve memory usage, latency, and avoid GC churn. Larger pages generally
             * improve loading throughput, to a point
             * (avoid loading more than 2MB from SQLite at once, since it incurs extra cost).
             * <p>
             * If you're loading data for very large, social-media style cards that take up most of
             * a screen, and your database isn't a bottleneck, 10-20 may make sense. If you're
             * displaying dozens of items in a tiled grid, which can present items during a scroll
             * much more quickly, consider closer to 100.
             *
             * @param pageSize Number of items loaded at once from the DataSource.
             * @return this
             */
            public Builder setPageSize(int pageSize) {
                this.mPageSize = pageSize;
                return this;
            }

            /**
             * Defines how far from the edge of loaded content an access must be to trigger further
             * loading.
             * <p>
             * Should be several times the number of visible items onscreen.
             * <p>
             * If not set, defaults to page size.
             * <p>
             * A value of 0 indicates that no list items will be loaded until they are specifically
             * requested. This is generally not recommended, so that users don't observe a
             * placeholder item (with placeholders) or end of list (without) while scrolling.
             *
             * @param prefetchDistance Distance the PagedList should prefetch.
             * @return this
             */
            public Builder setPrefetchDistance(int prefetchDistance) {
                this.mPrefetchDistance = prefetchDistance;
                return this;
            }

            /**
             * Pass false to disable null placeholders in PagedLists using this Config.
             * <p>
             * If not set, defaults to true.
             * <p>
             * A PagedList will present null placeholders for not-yet-loaded content if two
             * conditions are met:
             * <p>
             * 1) Its DataSource can count all unloaded items (so that the number of nulls to
             * present is known).
             * <p>
             * 2) placeholders are not disabled on the Config.
             * <p>
             * Call {@code setEnablePlaceholders(false)} to ensure the receiver of the PagedList
             * (often a {@link PagedListAdapter}) doesn't need to account for null items.
             * <p>
             * If placeholders are disabled, not-yet-loaded content will not be present in the list.
             * Paging will still occur, but as items are loaded or removed, they will be signaled
             * as inserts to the {@link PagedList.Callback}.
             * {@link PagedList.Callback#onChanged(int, int)} will not be issued as part of loading,
             * though a {@link PagedListAdapter} may still receive change events as a result of
             * PagedList diffing.
             *
             * @param enablePlaceholders False if null placeholders should be disabled.
             * @return this
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
             * {@link KeyedDataSource#loadInitial(int)} to load a (typically) larger amount
             * of data on first load.
             * <p>
             * If not set, defaults to three times page size.
             *
             * @param initialLoadSizeHint Number of items to load while initializing the PagedList.
             * @return this
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
