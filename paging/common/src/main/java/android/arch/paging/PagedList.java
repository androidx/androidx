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

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @NonNull
    final Executor mMainThreadExecutor;
    @NonNull
    final Executor mBackgroundThreadExecutor;
    @Nullable
    final BoundaryCallback<T> mBoundaryCallback;
    @NonNull
    final Config mConfig;
    @NonNull
    final PagedStorage<?, T> mStorage;

    int mLastLoad = 0;
    T mLastItem = null;

    // if set to true, mBoundaryCallback is non-null, and should
    // be dispatched when nearby load has occurred
    private boolean mBoundaryCallbackBeginDeferred = false;
    private boolean mBoundaryCallbackEndDeferred = false;

    // lowest and highest index accessed by loadAround. Used to
    // decide when mBoundaryCallback should be dispatched
    private int mLowestIndexAccessed = Integer.MAX_VALUE;
    private int mHighestIndexAccessed = Integer.MIN_VALUE;

    private final AtomicBoolean mDetached = new AtomicBoolean(false);

    protected final ArrayList<WeakReference<Callback>> mCallbacks = new ArrayList<>();

    PagedList(@NonNull PagedStorage<?, T> storage,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable BoundaryCallback<T> boundaryCallback,
            @NonNull Config config) {
        mStorage = storage;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mBoundaryCallback = boundaryCallback;
        mConfig = config;
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
     * @param boundaryCallback Optional boundary callback to attach to the list.
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
            @Nullable BoundaryCallback<T> boundaryCallback,
            @NonNull Config config,
            @Nullable K key) {
        if (dataSource.isContiguous() || !config.enablePlaceholders) {
            if (!dataSource.isContiguous()) {
                //noinspection unchecked
                dataSource = (DataSource<K, T>) ((TiledDataSource<T>) dataSource).getAsContiguous();
            }
            ContiguousDataSource<K, T> contigDataSource = (ContiguousDataSource<K, T>) dataSource;
            return new ContiguousPagedList<>(contigDataSource,
                    mainThreadExecutor,
                    backgroundThreadExecutor,
                    boundaryCallback,
                    config,
                    key);
        } else {
            return new TiledPagedList<>((TiledDataSource<T>) dataSource,
                    mainThreadExecutor,
                    backgroundThreadExecutor,
                    boundaryCallback,
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
        private BoundaryCallback mBoundaryCallback;
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

        @NonNull
        public Builder<Key, Value> setBoundaryCallback(
                @Nullable BoundaryCallback boundaryCallback) {
            mBoundaryCallback = boundaryCallback;
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

            //noinspection unchecked
            return PagedList.create(
                    mDataSource,
                    mMainThreadExecutor,
                    mBackgroundThreadExecutor,
                    mBoundaryCallback,
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
    public T get(int index) {
        T item = mStorage.get(index);
        if (item != null) {
            mLastItem = item;
        }
        return item;
    }

    /**
     * Load adjacent items to passed index.
     *
     * @param index Index at which to load.
     */
    public void loadAround(int index) {
        mLastLoad = index + getPositionOffset();
        loadAroundInternal(index);

        mLowestIndexAccessed = Math.min(mLowestIndexAccessed, index);
        mHighestIndexAccessed = Math.max(mHighestIndexAccessed, index);

        /*
         * mLowestIndexAccessed / mHighestIndexAccessed have been updated, so check if we need to
         * dispatch boundary callbacks. Boundary callbacks are deferred until last items are loaded,
         * and accesses happen near the boundaries.
         *
         * Note: we post here, since RecyclerView may want to add items in response, and this
         * call occurs in PagedListAdapter bind.
         */
        tryDispatchBoundaryCallbacks(true);
    }

    // Creation thread for initial synchronous load, otherwise main thread
    // Safe to access main thread only state - no other thread has reference during construction
    @AnyThread
    void deferBoundaryCallbacks(final boolean deferEmpty,
            final boolean deferBegin, final boolean deferEnd) {
        if (mBoundaryCallback == null) {
            throw new IllegalStateException("Computing boundary");
        }

        /*
         * If lowest/highest haven't been initialized, set them to storage size,
         * since placeholders must already be computed by this point.
         *
         * This is just a minor optimization so that BoundaryCallback callbacks are sent immediately
         * if the initial load size is smaller than the prefetch window (see
         * TiledPagedListTest#boundaryCallback_immediate())
         */
        if (mLowestIndexAccessed == Integer.MAX_VALUE) {
            mLowestIndexAccessed = mStorage.size();
        }
        if (mHighestIndexAccessed == Integer.MIN_VALUE) {
            mHighestIndexAccessed = 0;
        }

        if (deferEmpty || deferBegin || deferEnd) {
            // Post to the main thread, since we may be on creation thread currently
            mMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // on is dispatched immediately, since items won't be accessed
                    //noinspection ConstantConditions
                    if (deferEmpty) {
                        mBoundaryCallback.onZeroItemsLoaded();
                    }

                    // for other callbacks, mark deferred, and only dispatch if loadAround
                    // has been called near to the position
                    if (deferBegin) {
                        mBoundaryCallbackBeginDeferred = true;
                    }
                    if (deferEnd) {
                        mBoundaryCallbackEndDeferred = true;
                    }
                    tryDispatchBoundaryCallbacks(false);
                }
            });
        }
    }

    /**
     * Call this when mLowest/HighestIndexAccessed are changed, or
     * mBoundaryCallbackBegin/EndDeferred is set.
     */
    private void tryDispatchBoundaryCallbacks(boolean post) {
        final boolean dispatchBegin = mBoundaryCallbackBeginDeferred
                && mLowestIndexAccessed <= mConfig.prefetchDistance;
        final boolean dispatchEnd = mBoundaryCallbackEndDeferred
                && mHighestIndexAccessed >= size() - mConfig.prefetchDistance;

        if (!dispatchBegin && !dispatchEnd) {
            return;
        }

        if (dispatchBegin) {
            mBoundaryCallbackBeginDeferred = false;
        }
        if (dispatchEnd) {
            mBoundaryCallbackEndDeferred = false;
        }
        if (post) {
            mMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    dispatchBoundaryCallbacks(dispatchBegin, dispatchEnd);
                }
            });
        } else {
            dispatchBoundaryCallbacks(dispatchBegin, dispatchEnd);
        }
    }

    private void dispatchBoundaryCallbacks(boolean begin, boolean end) {
        // safe to deref mBoundaryCallback here, since we only defer if mBoundaryCallback present
        if (begin) {
            //noinspection ConstantConditions
            mBoundaryCallback.onItemAtFrontLoaded(
                    snapshot(), mStorage.getFirstLoadedItem(), mStorage.size());
        }
        if (end) {
            //noinspection ConstantConditions
            mBoundaryCallback.onItemAtEndLoaded(
                    snapshot(), mStorage.getLastLoadedItem(), mStorage.size());
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void offsetBoundaryAccessIndices(int offset) {
        mLowestIndexAccessed += offset;
        mHighestIndexAccessed += offset;
    }

    /**
     * Returns size of the list, including any not-yet-loaded null padding.
     *
     * @return Current total size of the list.
     */
    @Override
    public int size() {
        return mStorage.size();
    }

    /**
     * Returns whether the list is immutable. Immutable lists may not become mutable again, and may
     * safely be accessed from any thread.
     *
     * @return True if the PagedList is immutable.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isImmutable() {
        return isDetached();
    }

    /**
     * Returns an immutable snapshot of the PagedList. If this PagedList is already
     * immutable, it will be returned.
     *
     * @return Immutable snapshot of PagedList data.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public List<T> snapshot() {
        if (isImmutable()) {
            return this;
        }

        return new SnapshotPagedList<>(this);
    }

    abstract boolean isContiguous();

    /**
     * Return the Config used to construct this PagedList.
     *
     * @return the Config of this PagedList
     */
    @NonNull
    public Config getConfig() {
        return mConfig;
    }

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
    public abstract Object getLastKey();

    /**
     * True if the PagedList has detached the DataSource it was loading from, and will no longer
     * load new data.
     *
     * @return True if the data source is detached.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isDetached() {
        return mDetached.get();
    }

    /**
     * Detach the PagedList from its DataSource, and attempt to load no more data.
     * <p>
     * This is called automatically when a DataSource load returns <code>null</code>, which is a
     * signal to stop loading. The PagedList will continue to present existing data, but will not
     * initiate new loads.
     */
    @SuppressWarnings("WeakerAccess")
    public void detach() {
        mDetached.set(true);
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
        return mStorage.getPositionOffset();
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
    @SuppressWarnings("WeakerAccess")
    public void addWeakCallback(@Nullable List<T> previousSnapshot, @NonNull Callback callback) {
        if (previousSnapshot != null && previousSnapshot != this) {
            PagedList<T> storageSnapshot = (PagedList<T>) previousSnapshot;
            //noinspection unchecked
            dispatchUpdatesSinceSnapshot(storageSnapshot, callback);
        }

        // first, clean up any empty weak refs
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            Callback currentCallback = mCallbacks.get(i).get();
            if (currentCallback == null) {
                mCallbacks.remove(i);
            }
        }

        // then add the new one
        mCallbacks.add(new WeakReference<>(callback));
    }
    /**
     * Removes a previously added callback.
     *
     * @param callback Callback, previously added.
     * @see #addWeakCallback(List, Callback)
     */
    @SuppressWarnings("WeakerAccess")
    public void removeWeakCallback(@NonNull Callback callback) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            Callback currentCallback = mCallbacks.get(i).get();
            if (currentCallback == null || currentCallback == callback) {
                // found callback, or empty weak ref
                mCallbacks.remove(i);
            }
        }
    }

    void notifyInserted(int position, int count) {
        if (count != 0) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                Callback callback = mCallbacks.get(i).get();
                if (callback != null) {
                    callback.onInserted(position, count);
                }
            }
        }
    }

    void notifyChanged(int position, int count) {
        if (count != 0) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                Callback callback = mCallbacks.get(i).get();

                if (callback != null) {
                    callback.onChanged(position, count);
                }
            }
        }
    }

    abstract void dispatchUpdatesSinceSnapshot(@NonNull PagedList<T> snapshot,
            @NonNull Callback callback);

    abstract void loadAroundInternal(int index);

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
        /**
         * Size of each page loaded by the PagedList.
         */
        public final int pageSize;

        /**
         * Prefetch distance which defines how far ahead to load.
         * <p>
         * If this value is set to 50, the paged list will attempt to load 50 items in advance of
         * data that's already been accessed.
         *
         * @see PagedList#loadAround(int)
         */
        @SuppressWarnings("WeakerAccess")
        public final int prefetchDistance;

        /**
         * Defines whether the PagedList may display null placeholders, if the DataSource provides
         * them.
         */
        @SuppressWarnings("WeakerAccess")
        public final boolean enablePlaceholders;

        /**
         * Size hint for initial load of PagedList, often larger than a regular page.
         */
        @SuppressWarnings("WeakerAccess")
        public final int initialLoadSizeHint;

        private Config(int pageSize, int prefetchDistance,
                boolean enablePlaceholders, int initialLoadSizeHint) {
            this.pageSize = pageSize;
            this.prefetchDistance = prefetchDistance;
            this.enablePlaceholders = enablePlaceholders;
            this.initialLoadSizeHint = initialLoadSizeHint;
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
             * This value is typically larger than page size, so on first load data there's a large
             * enough range of content loaded to cover small scrolls.
             * <p>
             * If used with a {@link TiledDataSource}, this value is rounded to the nearest number
             * of pages, with a minimum of two pages, and loaded with a single call to
             * {@link TiledDataSource#loadRange(int, int)}.
             * <p>
             * If used with a {@link KeyedDataSource}, this value will be passed to
             * {@link KeyedDataSource#loadInitial(int)}.
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

    /**
     * WIP API for load-more-into-local-storage callbacks
     */
    public abstract static class BoundaryCallback<T> {
        public abstract void onZeroItemsLoaded();
        public abstract void onItemAtFrontLoaded(@NonNull List<T> pagedListSnapshot,
                @NonNull T itemAtFront, int pagedListSize);
        public abstract void onItemAtEndLoaded(@NonNull List<T> pagedListSnapshot,
                @NonNull T itemAtEnd, int pagedListSize);
    }
}
