/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.paging;

import androidx.annotation.AnyThread;
import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lazy loading list that pages in immutable content from a {@link DataSource}.
 * <p>
 * A PagedList is a {@link List} which loads its data in chunks (pages) from a {@link DataSource}.
 * Items can be accessed with {@link #get(int)}, and further loading can be triggered with
 * {@link #loadAround(int)}. To display a PagedList, see {@link PagedListAdapter}, which enables the
 * binding of a PagedList to a {@link androidx.recyclerview.widget.RecyclerView}.
 * <h4>Loading Data</h4>
 * <p>
 * All data in a PagedList is loaded from its {@link DataSource}. Creating a PagedList loads the
 * first chunk of data from the DataSource immediately, and should for this reason be done on a
 * background thread. The constructed PagedList may then be passed to and used on the UI thread.
 * This is done to prevent passing a list with no loaded content to the UI thread, which should
 * generally not be presented to the user.
 * <p>
 * A PagedList initially presents this first partial load as its content, and expands over time as
 * content is loaded in. When {@link #loadAround} is called, items will be loaded in near the passed
 * list index. If placeholder {@code null}s are present in the list, they will be replaced as
 * content is loaded. If not, newly loaded items will be inserted at the beginning or end of the
 * list.
 * <p>
 * PagedList can present data for an unbounded, infinite scrolling list, or a very large but
 * countable list. Use {@link Config} to control how many items a PagedList loads, and when.
 * <p>
 * If you use {@link LivePagedListBuilder} to get a
 * {@link androidx.lifecycle.LiveData}&lt;PagedList>, it will initialize PagedLists on a
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
 *     loaded or dropped) and fast-scrolling to any position, loaded or not.
 *     <li>They avoid the need for a loading spinner at the end of the loaded list, since the list
 *     is always full sized.
 * </ul>
 * <p>
 * They also have drawbacks:
 * <ul>
 *     <li>Your Adapter needs to account for {@code null} items. This often means providing default
 *     values in data you bind to a {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}.
 *     <li>They don't work well if your item views are of different sizes, as this will prevent
 *     loading items from cross-fading nicely.
 *     <li>They require you to count your data set, which can be expensive or impossible, depending
 *     on your DataSource.
 * </ul>
 * <p>
 * Placeholders are enabled by default, but can be disabled in two ways. They are disabled if the
 * DataSource does not count its data set in its initial load, or if  {@code false} is passed to
 * {@link Config.Builder#setEnablePlaceholders(boolean)} when building a {@link Config}.
 * <h4>Mutability and Snapshots</h4>
 * A PagedList is <em>mutable</em> while loading, or ready to load from its DataSource.
 * As loads succeed, a mutable PagedList will be updated via Runnables on the main thread. You can
 * listen to these updates with a {@link Callback}. (Note that {@link PagedListAdapter} will listen
 * to these to signal RecyclerView about the updates/changes).
 * <p>
 * If a PagedList attempts to load from an invalid DataSource, it will {@link #detach()}
 * from the DataSource, meaning that it will no longer attempt to load data. It will return true
 * from {@link #isImmutable()}, and a new DataSource / PagedList pair must be created to load
 * further data. See {@link DataSource} and {@link LivePagedListBuilder} for how new PagedLists are
 * created to represent changed data.
 * <p>
 * A PagedList snapshot is simply an immutable shallow copy of the current state of the PagedList as
 * a {@code List}. It will reference the same inner items, and contain the same {@code null}
 * placeholders, if present.
 *
 * @param <T> The type of the entries in the list.
 */
public abstract class PagedList<T> extends AbstractList<T> {

    // Notes on threading:
    //
    // PagedList and its subclasses are passed and accessed on multiple threads, but are always
    // owned by a single thread. During initialization, this is the creation thread, generally the
    // fetchExecutor/fetchScheduler when using LiveData/RxJava. After initialization, the PagedList
    // is owned by the main thread (or notifyScheduler, if overridden in RxJava).
    //
    // The only exception is detach()/isDetached(), which can be called from the fetch thread.
    // However these methods simply wrap a atomic boolean, so are safe.
    //
    // The PageResult.Receiver that receives new data from the DataSource is always run on the
    // owning thread.

    @NonNull
    final Executor mMainThreadExecutor;
    @NonNull
    final Executor mBackgroundThreadExecutor;
    @Nullable
    final BoundaryCallback<T> mBoundaryCallback;
    @NonNull
    final Config mConfig;
    @NonNull
    final PagedStorage<T> mStorage;

    /**
     * Last access location, in total position space (including offset).
     * <p>
     * Used by positional data
     * sources to initialize loading near viewport
     */
    int mLastLoad = 0;
    T mLastItem = null;

    final int mRequiredRemainder;

    // if set to true, mBoundaryCallback is non-null, and should
    // be dispatched when nearby load has occurred
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mBoundaryCallbackBeginDeferred = false;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mBoundaryCallbackEndDeferred = false;

    // lowest and highest index accessed by loadAround. Used to
    // decide when mBoundaryCallback should be dispatched
    private int mLowestIndexAccessed = Integer.MAX_VALUE;
    private int mHighestIndexAccessed = Integer.MIN_VALUE;

    private final AtomicBoolean mDetached = new AtomicBoolean(false);

    private final ArrayList<WeakReference<Callback>> mCallbacks = new ArrayList<>();

    PagedList(@NonNull PagedStorage<T> storage,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable BoundaryCallback<T> boundaryCallback,
            @NonNull Config config) {
        mStorage = storage;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mBoundaryCallback = boundaryCallback;
        mConfig = config;
        mRequiredRemainder = mConfig.prefetchDistance * 2 + mConfig.pageSize;
    }

    /**
     * Create a PagedList which loads data from the provided data source on a background thread,
     * posting updates to the main thread.
     *
     *
     * @param dataSource DataSource providing data to the PagedList
     * @param notifyExecutor Thread that will use and consume data from the PagedList.
     *                       Generally, this is the UI/main thread.
     * @param fetchExecutor Data loading will be done via this executor -
     *                      should be a background thread.
     * @param boundaryCallback Optional boundary callback to attach to the list.
     * @param config PagedList Config, which defines how the PagedList will load data.
     * @param <K> Key type that indicates to the DataSource what data to load.
     * @param <T> Type of items to be held and loaded by the PagedList.
     *
     * @return Newly created PagedList, which will page in data from the DataSource as needed.
     */
    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static <K, T> PagedList<T> create(@NonNull DataSource<K, T> dataSource,
            @NonNull Executor notifyExecutor,
            @NonNull Executor fetchExecutor,
            @Nullable BoundaryCallback<T> boundaryCallback,
            @NonNull Config config,
            @Nullable K key) {
        if (dataSource.isContiguous() || !config.enablePlaceholders) {
            int lastLoad = ContiguousPagedList.LAST_LOAD_UNSPECIFIED;
            if (!dataSource.isContiguous()) {
                //noinspection unchecked
                dataSource = (DataSource<K, T>) ((PositionalDataSource<T>) dataSource)
                        .wrapAsContiguousWithoutPlaceholders();
                if (key != null) {
                    lastLoad = (Integer) key;
                }
            }
            ContiguousDataSource<K, T> contigDataSource = (ContiguousDataSource<K, T>) dataSource;
            return new ContiguousPagedList<>(contigDataSource,
                    notifyExecutor,
                    fetchExecutor,
                    boundaryCallback,
                    config,
                    key,
                    lastLoad);
        } else {
            return new TiledPagedList<>((PositionalDataSource<T>) dataSource,
                    notifyExecutor,
                    fetchExecutor,
                    boundaryCallback,
                    config,
                    (key != null) ? (Integer) key : 0);
        }
    }

    /**
     * Builder class for PagedList.
     * <p>
     * DataSource, Config, main thread and background executor must all be provided.
     * <p>
     * A PagedList queries initial data from its DataSource during construction, to avoid empty
     * PagedLists being presented to the UI when possible. It's preferred to present initial data,
     * so that the UI doesn't show an empty list, or placeholders for a few frames, just before
     * showing initial content.
     * <p>
     * {@link LivePagedListBuilder} does this creation on a background thread automatically, if you
     * want to receive a {@code LiveData<PagedList<...>>}.
     *
     * @param <Key> Type of key used to load data from the DataSource.
     * @param <Value> Type of items held and loaded by the PagedList.
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder<Key, Value> {
        private final DataSource<Key, Value> mDataSource;
        private final Config mConfig;
        private Executor mNotifyExecutor;
        private Executor mFetchExecutor;
        private BoundaryCallback mBoundaryCallback;
        private Key mInitialKey;

        /**
         * Create a PagedList.Builder with the provided {@link DataSource} and {@link Config}.
         *
         * @param dataSource DataSource the PagedList will load from.
         * @param config Config that defines how the PagedList loads data from its DataSource.
         */
        public Builder(@NonNull DataSource<Key, Value> dataSource, @NonNull Config config) {
            //noinspection ConstantConditions
            if (dataSource == null) {
                throw new IllegalArgumentException("DataSource may not be null");
            }
            //noinspection ConstantConditions
            if (config == null) {
                throw new IllegalArgumentException("Config may not be null");
            }
            mDataSource = dataSource;
            mConfig = config;
        }

        /**
         * Create a PagedList.Builder with the provided {@link DataSource} and page size.
         * <p>
         * This method is a convenience for:
         * <pre>
         * PagedList.Builder(dataSource,
         *         new PagedList.Config.Builder().setPageSize(pageSize).build());
         * </pre>
         *
         * @param dataSource DataSource the PagedList will load from.
         * @param pageSize Config that defines how the PagedList loads data from its DataSource.
         */
        public Builder(@NonNull DataSource<Key, Value> dataSource, int pageSize) {
            this(dataSource, new PagedList.Config.Builder().setPageSize(pageSize).build());
        }
        /**
         * The executor defining where page loading updates are dispatched.
         *
         * @param notifyExecutor Executor that receives PagedList updates, and where
         * {@link Callback} calls are dispatched. Generally, this is the ui/main thread.
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setNotifyExecutor(@NonNull Executor notifyExecutor) {
            mNotifyExecutor = notifyExecutor;
            return this;
        }

        /**
         * The executor used to fetch additional pages from the DataSource.
         *
         * Does not affect initial load, which will be done immediately on whichever thread the
         * PagedList is created on.
         *
         * @param fetchExecutor Executor used to fetch from DataSources, generally a background
         *                      thread pool for e.g. I/O or network loading.
         * @return this
         */
        @NonNull
        public Builder<Key, Value> setFetchExecutor(@NonNull Executor fetchExecutor) {
            mFetchExecutor = fetchExecutor;
            return this;
        }

        /**
         * The BoundaryCallback for out of data events.
         * <p>
         * Pass a BoundaryCallback to listen to when the PagedList runs out of data to load.
         *
         * @param boundaryCallback BoundaryCallback for listening to out-of-data events.
         * @return this
         */
        @SuppressWarnings("unused")
        @NonNull
        public Builder<Key, Value> setBoundaryCallback(
                @Nullable BoundaryCallback boundaryCallback) {
            mBoundaryCallback = boundaryCallback;
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
         * This call will dispatch the {@link DataSource}'s loadInitial method immediately. If a
         * DataSource posts all of its work (e.g. to a network thread), the PagedList will
         * be immediately created as empty, and grow to its initial size when the initial load
         * completes.
         * <p>
         * If the DataSource implements its load synchronously, doing the load work immediately in
         * the loadInitial method, the PagedList will block on that load before completing
         * construction. In this case, use a background thread to create a PagedList.
         * <p>
         * It's fine to create a PagedList with an async DataSource on the main thread, such as in
         * the constructor of a ViewModel. An async network load won't block the initialLoad
         * function. For a synchronous DataSource such as one created from a Room database, a
         * {@code LiveData<PagedList>} can be safely constructed with {@link LivePagedListBuilder}
         * on the main thread, since actual construction work is deferred, and done on a background
         * thread.
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
            // TODO: define defaults, once they can be used in module without android dependency
            if (mNotifyExecutor == null) {
                throw new IllegalArgumentException("MainThreadExecutor required");
            }
            if (mFetchExecutor == null) {
                throw new IllegalArgumentException("BackgroundThreadExecutor required");
            }

            //noinspection unchecked
            return PagedList.create(
                    mDataSource,
                    mNotifyExecutor,
                    mFetchExecutor,
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
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

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
            throw new IllegalStateException("Can't defer BoundaryCallback, no instance");
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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void tryDispatchBoundaryCallbacks(boolean post) {
        final boolean dispatchBegin = mBoundaryCallbackBeginDeferred
                && mLowestIndexAccessed <= mConfig.prefetchDistance;
        final boolean dispatchEnd = mBoundaryCallbackEndDeferred
                && mHighestIndexAccessed >= size() - 1 - mConfig.prefetchDistance;

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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dispatchBoundaryCallbacks(boolean begin, boolean end) {
        // safe to deref mBoundaryCallback here, since we only defer if mBoundaryCallback present
        if (begin) {
            //noinspection ConstantConditions
            mBoundaryCallback.onItemAtFrontLoaded(mStorage.getFirstLoadedItem());
        }
        if (end) {
            //noinspection ConstantConditions
            mBoundaryCallback.onItemAtEndLoaded(mStorage.getLastLoadedItem());
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void offsetAccessIndices(int offset) {
        // update last loadAround index
        mLastLoad += offset;

        // update access range
        mLowestIndexAccessed += offset;
        mHighestIndexAccessed += offset;
    }

    /**
     * Returns size of the list, including any not-yet-loaded null padding.
     *
     * To get the number of loaded items, not counting placeholders, use {@link #getLoadedCount()}.
     *
     * @return Current total size of the list, including placeholders.
     *
     * @see #getLoadedCount()
     */
    @Override
    public int size() {
        return mStorage.size();
    }

    /**
     * Returns the number of items loaded in the PagedList.
     *
     * Unlike {@link #size()} this counts only loaded items, not placeholders.
     * <p>
     * If placeholders are {@link Config#enablePlaceholders disabled}, this method is equivalent to
     * {@link #size()}.
     *
     * @return Number of items currently loaded, not counting placeholders.
     *
     * @see #size()
     */
    public int getLoadedCount() {
        return mStorage.getLoadedCount();
    }

    /**
     * Returns whether the list is immutable.
     *
     * Immutable lists may not become mutable again, and may safely be accessed from any thread.
     * <p>
     * In the future, this method may return true when a PagedList has completed loading from its
     * DataSource. Currently, it is equivalent to {@link #isDetached()}.
     *
     * @return True if the PagedList is immutable.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isImmutable() {
        return isDetached();
    }

    /**
     * Returns an immutable snapshot of the PagedList in its current state.
     *
     * If this PagedList {@link #isImmutable() is immutable} due to its DataSource being invalid, it
     * will be returned.
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
     * Return the DataSource that provides data to this PagedList.
     *
     * @return the DataSource of this PagedList.
     */
    @NonNull
    public abstract DataSource<?, T> getDataSource();

    /**
     * Return the key for the position passed most recently to {@link #loadAround(int)}.
     * <p>
     * When a PagedList is invalidated, you can pass the key returned by this function to initialize
     * the next PagedList. This ensures (depending on load times) that the next PagedList that
     * arrives will have data that overlaps. If you use {@link LivePagedListBuilder}, it will do
     * this for you.
     *
     * @return Key of position most recently passed to {@link #loadAround(int)}.
     */
    @Nullable
    public abstract Object getLastKey();

    /**
     * True if the PagedList has detached the DataSource it was loading from, and will no longer
     * load new data.
     * <p>
     * A detached list is {@link #isImmutable() immutable}.
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
     * If data is supplied by a {@link PositionalDataSource}, the item returned from
     * <code>get(i)</code> has a position of <code>i + getPositionOffset()</code>.
     * <p>
     * If the DataSource is a {@link ItemKeyedDataSource} or {@link PageKeyedDataSource}, it
     * doesn't use positions, returns 0.
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
     * @param callback Callback to dispatch to.
     *
     * @see #removeWeakCallback(Callback)
     */
    @SuppressWarnings("WeakerAccess")
    public void addWeakCallback(@Nullable List<T> previousSnapshot, @NonNull Callback callback) {
        if (previousSnapshot != null && previousSnapshot != this) {

            if (previousSnapshot.isEmpty()) {
                if (!mStorage.isEmpty()) {
                    // If snapshot is empty, diff is trivial - just notify number new items.
                    // Note: occurs in async init, when snapshot taken before init page arrives
                    callback.onInserted(0, mStorage.size());
                }
            } else {
                PagedList<T> storageSnapshot = (PagedList<T>) previousSnapshot;

                //noinspection unchecked
                dispatchUpdatesSinceSnapshot(storageSnapshot, callback);
            }
        }

        // first, clean up any empty weak refs
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            final Callback currentCallback = mCallbacks.get(i).get();
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
            final Callback currentCallback = mCallbacks.get(i).get();
            if (currentCallback == null || currentCallback == callback) {
                // found callback, or empty weak ref
                mCallbacks.remove(i);
            }
        }
    }

    void notifyInserted(int position, int count) {
        if (count != 0) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                final Callback callback = mCallbacks.get(i).get();
                if (callback != null) {
                    callback.onInserted(position, count);
                }
            }
        }
    }

    void notifyChanged(int position, int count) {
        if (count != 0) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                final Callback callback = mCallbacks.get(i).get();

                if (callback != null) {
                    callback.onChanged(position, count);
                }
            }
        }
    }

    void notifyRemoved(int position, int count) {
        if (count != 0) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                final Callback callback = mCallbacks.get(i).get();

                if (callback != null) {
                    callback.onRemoved(position, count);
                }
            }
        }
    }

    /**
     * Dispatch updates since the non-empty snapshot was taken.
     *
     * @param snapshot Non-empty snapshot.
     * @param callback Callback for updates that have occurred since snapshot.
     */
    abstract void dispatchUpdatesSinceSnapshot(@NonNull PagedList<T> snapshot,
            @NonNull Callback callback);

    abstract void loadAroundInternal(int index);

    /**
     * Callback signaling when content is loaded into the list.
     * <p>
     * Can be used to listen to items being paged in and out. These calls will be dispatched on
     * the executor defined by {@link Builder#setNotifyExecutor(Executor)}, which is generally
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
         * When {@link #maxSize} is set to {@code MAX_SIZE_UNBOUNDED}, the maximum number of items
         * loaded is unbounded, and pages will never be dropped.
         */
        @SuppressWarnings("WeakerAccess")
        public static final int MAX_SIZE_UNBOUNDED = Integer.MAX_VALUE;

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
         * Defines the maximum number of items that may be loaded into this pagedList before pages
         * should be dropped.
         * <p>
         * {@link PageKeyedDataSource} does not currently support dropping pages - when
         * loading from a {@code PageKeyedDataSource}, this value is ignored.
         *
         * @see #MAX_SIZE_UNBOUNDED
         * @see Builder#setMaxSize(int)
         */
        public final int maxSize;

        /**
         * Size hint for initial load of PagedList, often larger than a regular page.
         */
        @SuppressWarnings("WeakerAccess")
        public final int initialLoadSizeHint;

        Config(int pageSize, int prefetchDistance,
                boolean enablePlaceholders, int initialLoadSizeHint, int maxSize) {
            this.pageSize = pageSize;
            this.prefetchDistance = prefetchDistance;
            this.enablePlaceholders = enablePlaceholders;
            this.initialLoadSizeHint = initialLoadSizeHint;
            this.maxSize = maxSize;
        }

        /**
         * Builder class for {@link Config}.
         * <p>
         * You must at minimum specify page size with {@link #setPageSize(int)}.
         */
        public static final class Builder {
            static final int DEFAULT_INITIAL_PAGE_MULTIPLIER = 3;

            private int mPageSize = -1;
            private int mPrefetchDistance = -1;
            private int mInitialLoadSizeHint = -1;
            private boolean mEnablePlaceholders = true;
            private int mMaxSize = MAX_SIZE_UNBOUNDED;

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
            @NonNull
            public Builder setPageSize(@IntRange(from = 1) int pageSize) {
                if (pageSize < 1) {
                    throw new IllegalArgumentException("Page size must be a positive number");
                }
                mPageSize = pageSize;
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
            @NonNull
            public Builder setPrefetchDistance(@IntRange(from = 0) int prefetchDistance) {
                mPrefetchDistance = prefetchDistance;
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
            @NonNull
            public Builder setEnablePlaceholders(boolean enablePlaceholders) {
                mEnablePlaceholders = enablePlaceholders;
                return this;
            }

            /**
             * Defines how many items to load when first load occurs.
             * <p>
             * This value is typically larger than page size, so on first load data there's a large
             * enough range of content loaded to cover small scrolls.
             * <p>
             * When using a {@link PositionalDataSource}, the initial load size will be coerced to
             * an integer multiple of pageSize, to enable efficient tiling.
             * <p>
             * If not set, defaults to three times page size.
             *
             * @param initialLoadSizeHint Number of items to load while initializing the PagedList.
             * @return this
             */
            @SuppressWarnings("WeakerAccess")
            @NonNull
            public Builder setInitialLoadSizeHint(@IntRange(from = 1) int initialLoadSizeHint) {
                mInitialLoadSizeHint = initialLoadSizeHint;
                return this;
            }

            /**
             * Defines how many items to keep loaded at once.
             * <p>
             * This can be used to cap the number of items kept in memory by dropping pages. This
             * value is typically many pages so old pages are cached in case the user scrolls back.
             * <p>
             * This value must be at least two times the
             * {@link #setPrefetchDistance(int)} prefetch distance} plus the
             * {@link #setPageSize(int) page size}). This constraint prevent loads from being
             * continuously fetched and discarded due to prefetching.
             * <p>
             * The max size specified here best effort, not a guarantee. In practice, if maxSize
             * is many times the page size, the number of items held by the PagedList will not grow
             * above this number. Exceptions are made as necessary to guarantee:
             * <ul>
             *     <li>Pages are never dropped until there are more than two pages loaded. Note that
             *     a DataSource may not be held strictly to
             *     {@link Config#pageSize requested pageSize}, so two pages may be larger than
             *     expected.
             *     <li>Pages are never dropped if they are within a prefetch window (defined to be
             *     {@code pageSize + (2 * prefetchDistance)}) of the most recent load.
             * </ul>
             * <p>
             * {@link PageKeyedDataSource} does not currently support dropping pages - when
             * loading from a {@code PageKeyedDataSource}, this value is ignored.
             * <p>
             * If not set, defaults to {@code MAX_SIZE_UNBOUNDED}, which disables page dropping.
             *
             * @param maxSize Maximum number of items to keep in memory, or
             *                {@code MAX_SIZE_UNBOUNDED} to disable page dropping.
             * @return this
             *
             * @see Config#MAX_SIZE_UNBOUNDED
             * @see Config#maxSize
             */
            @NonNull
            public Builder setMaxSize(@IntRange(from = 2) int maxSize) {
                mMaxSize = maxSize;
                return this;
            }

            /**
             * Creates a {@link Config} with the given parameters.
             *
             * @return A new Config.
             */
            @NonNull
            public Config build() {
                if (mPrefetchDistance < 0) {
                    mPrefetchDistance = mPageSize;
                }
                if (mInitialLoadSizeHint < 0) {
                    mInitialLoadSizeHint = mPageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER;
                }
                if (!mEnablePlaceholders && mPrefetchDistance == 0) {
                    throw new IllegalArgumentException("Placeholders and prefetch are the only ways"
                            + " to trigger loading of more data in the PagedList, so either"
                            + " placeholders must be enabled, or prefetch distance must be > 0.");
                }
                if (mMaxSize != MAX_SIZE_UNBOUNDED) {
                    if (mMaxSize < mPageSize + mPrefetchDistance * 2) {
                        throw new IllegalArgumentException("Maximum size must be at least"
                                + " pageSize + 2*prefetchDist, pageSize=" + mPageSize
                                + ", prefetchDist=" + mPrefetchDistance + ", maxSize=" + mMaxSize);
                    }
                }

                return new Config(mPageSize, mPrefetchDistance,
                        mEnablePlaceholders, mInitialLoadSizeHint, mMaxSize);
            }
        }
    }

    /**
     * Signals when a PagedList has reached the end of available data.
     * <p>
     * When local storage is a cache of network data, it's common to set up a streaming pipeline:
     * Network data is paged into the database, database is paged into UI. Paging from the database
     * to UI can be done with a {@code LiveData<PagedList>}, but it's still necessary to know when
     * to trigger network loads.
     * <p>
     * BoundaryCallback does this signaling - when a DataSource runs out of data at the end of
     * the list, {@link #onItemAtEndLoaded(Object)} is called, and you can start an async network
     * load that will write the result directly to the database. Because the database is being
     * observed, the UI bound to the {@code LiveData<PagedList>} will update automatically to
     * account for the new items.
     * <p>
     * Note that a BoundaryCallback instance shared across multiple PagedLists (e.g. when passed to
     * {@link LivePagedListBuilder#setBoundaryCallback}), the callbacks may be issued multiple
     * times. If for example {@link #onItemAtEndLoaded(Object)} triggers a network load, it should
     * avoid triggering it again while the load is ongoing.
     * <p>
     * The database + network Repository in the
     * <a href="https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md">PagingWithNetworkSample</a>
     * shows how to implement a network BoundaryCallback using
     * <a href="https://square.github.io/retrofit/">Retrofit</a>, while
     * handling swipe-to-refresh, network errors, and retry.
     * <h4>Requesting Network Data</h4>
     * BoundaryCallback only passes the item at front or end of the list when out of data. This
     * makes it an easy fit for item-keyed network requests, where you can use the item passed to
     * the BoundaryCallback to request more data from the network. In these cases, the source of
     * truth for next page to load is coming from local storage, based on what's already loaded.
     * <p>
     * If you aren't using an item-keyed network API, you may be using page-keyed, or page-indexed.
     * If this is the case, the paging library doesn't know about the page key or index used in the
     * BoundaryCallback, so you need to track it yourself. You can do this in one of two ways:
     * <h5>Local storage Page key</h5>
     * If you want to perfectly resume your query, even if the app is killed and resumed, you can
     * store the key on disk. Note that with a positional/page index network API, there's a simple
     * way to do this, by using the {@code listSize} as an input to the next load (or
     * {@code listSize / NETWORK_PAGE_SIZE}, for page indexing).
     * <p>
     * The current list size isn't passed to the BoundaryCallback though. This is because the
     * PagedList doesn't necessarily know the number of items in local storage. Placeholders may be
     * disabled, or the DataSource may not count total number of items.
     * <p>
     * Instead, for these positional cases, you can query the database for the number of items, and
     * pass that to the network.
     * <h5>In-Memory Page key</h5>
     * Often it doesn't make sense to query the next page from network if the last page you fetched
     * was loaded many hours or days before. If you keep the key in memory, you can refresh any time
     * you start paging from a network source.
     * <p>
     * Store the next key in memory, inside your BoundaryCallback. When you create a new
     * BoundaryCallback when creating a new {@code LiveData}/{@code Observable} of
     * {@code PagedList}, refresh data. For example,
     * <a href="https://codelabs.developers.google.com/codelabs/android-paging/index.html#8">in the
     * Paging Codelab</a>, the GitHub network page index is stored in memory.
     *
     * @param <T> Type loaded by the PagedList.
     */
    @MainThread
    public abstract static class BoundaryCallback<T> {
        /**
         * Called when zero items are returned from an initial load of the PagedList's data source.
         */
        public void onZeroItemsLoaded() {}

        /**
         * Called when the item at the front of the PagedList has been loaded, and access has
         * occurred within {@link Config#prefetchDistance} of it.
         * <p>
         * No more data will be prepended to the PagedList before this item.
         *
         * @param itemAtFront The first item of PagedList
         */
        public void onItemAtFrontLoaded(@NonNull T itemAtFront) {}

        /**
         * Called when the item at the end of the PagedList has been loaded, and access has
         * occurred within {@link Config#prefetchDistance} of it.
         * <p>
         * No more data will be appended to the PagedList after this item.
         *
         * @param itemAtEnd The first item of PagedList
         */
        public void onItemAtEndLoaded(@NonNull T itemAtEnd) {}
    }
}
