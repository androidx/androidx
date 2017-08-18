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

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.concurrent.Executor;

/**
 * Helper object for mapping a {@link PagedList} into a
 * {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}.
 * <p>
 * For simplicity, the {@link PagedListAdapter} wrapper class can often be used instead of the
 * helper directly. This helper class is exposed for complex cases, and where overriding an adapter
 * base class to support paging isn't convenient.
 * <p>
 * Both the internal paging of the list as more data is loaded, and updates in the form of new
 * PagedLists.
 * <p>
 * The PagedListAdapterHelper can take a {@link LiveData} of PagedList and present the data simply
 * for an adapter. It listens to PagedList loading callbacks, and uses DiffUtil on a background
 * thread to compute updates as new PagedLists are received.
 * <p>
 * It provides a simple list-like API with {@link #get(int)} and {@link #getItemCount()} for an
 * adapter to acquire and present data objects.
 * <p>
 * A complete usage pattern with Room would look like this:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract LivePagedListProvider&lt;User> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData&lt;PagedList&lt;User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = userDao.usersByLastName().build(
 *                 new PagedList.Config.Builder()
 *                         .setPageSize(50)
 *                         .setPrefetchDistance(50)
 *                         .build());
 *     }
 * }
 *
 * class MyActivity extends Activity implements LifecycleRegistryOwner {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter(
 *                 new PagedListAdapterHelper.&lt;User>Builder()
 *                         .setSource(viewModel.usersList)
 *                         .setLifecycleOwner(this)
 *                         .setDiffCallback(User.DIFF_CALLBACK));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends RecyclerView.Adapter&lt;UserViewHolder> {
 *     private final PagedListAdapterHelper&lt;User> helper;
 *     public UserAdapter(PagedListAdapterHelper.Builder&lt;User> builder) {
 *         helper = builder.setUpdateAdapter(this).build();
 *     }
 *     {@literal @}Override
 *     public int getItemCount() {
 *         return mHelper.getItemCount();
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = mHelper.get(position);
 *         if (user == null) {
 *             // AdapterHelper will automatically invalidate this row when the actual
 *             // object is loaded from the database
 *             holder.clear();
 *         } else {
 *             holder.bindTo(user);
 *         }
 *     }
 * }
 * </pre>
 *
 * @param <T> Type of the PagedLists this helper will receive.
 */
public class PagedListAdapterHelper<T> {
    private final ListUpdateCallback mListUpdateCallback;
    private final DiffCallback<T> mDiffCallback;
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final boolean mSetterAllowed;

    private Boolean mIsContiguous;

    private PagedListAdapterHelper(Executor mainThreadExecutor, Executor backgroundThreadExecutor,
            ListUpdateCallback listUpdateCallback, DiffCallback<T> diffCallback,
            boolean setterAllowed) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mListUpdateCallback = listUpdateCallback;
        mDiffCallback = diffCallback;
        mSetterAllowed = setterAllowed;
    }

    /**
     * Default ListUpdateCallback that dispatches directly to an adapter. Can be replaced by a
     * custom ListUpdateCallback if e.g. your adapter has a header in it, and so has an offset
     * between list positions and adapter positions.
     */
    static class AdapterCallback implements ListUpdateCallback {
        private final RecyclerView.Adapter mAdapter;

        AdapterCallback(RecyclerView.Adapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onInserted(int position, int count) {
            mAdapter.notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            mAdapter.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mAdapter.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            mAdapter.notifyItemRangeChanged(position, count, payload);
        }
    }

    private PagedList.Callback mPagedListCallback = new PagedList.Callback() {
        @Override
        public void onInserted(int position, int count) {
            mListUpdateCallback.onInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            mListUpdateCallback.onRemoved(position, count);
        }

        @Override
        public void onChanged(int position, int count) {
            // NOTE: pass a null payload to convey null -> item
            mListUpdateCallback.onChanged(position, count, null);
        }
    };

    private PagedList<T> mList;

    // True if background thread executor has at least one task scheduled
    private boolean mUpdateScheduled;

    // Max generation of currently scheduled runnable
    private int mMaxScheduledGeneration;


    /**
     * Get the item from the current PagedList at the specified index.
     * <p>
     * Note that this operates on both loaded items and null padding within the PagedList.
     *
     * @param index Index of item to get, must be >= 0, and &lt; {@link #getItemCount()}.
     * @return The item, or null, if a null placeholder is at the specified position.
     */
    public T get(int index) {
        if (mList == null) {
            return null;
        }

        mList.loadAround(index);
        return mList.get(index);
    }

    /**
     * Get the number of items currently presented by this AdapterHelper. This value can be directly
     * returned to {@link RecyclerView.Adapter#getItemCount()}.
     *
     * @return Number of items being presented.
     */
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    /**
     * Pass a new PagedList to the AdapterHelper.
     * <p>
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * {@link ListUpdateCallback}), and the new PagedList will be swapped in.
     * <p>
     * If this AdapterHelper is already consuming data from a LiveData&lt;PagedList>, calling this
     * method manually will throw.
     *
     * @param list The new PagedList.
     */
    @SuppressWarnings("WeakerAccess")
    public void setPagedList(PagedList<T> list) {
        if (!mSetterAllowed) {
            throw new IllegalStateException("When an AdapterHelper is observing a LiveData, you"
                    + " cannot set the list on it because it will be overridden by the LiveData"
                    + " setSource");
        }
        internalSetPagedList(list);
    }


    private void internalSetPagedList(final PagedList<T> list) {
        if (list != null) {
            if (mList == null) {
                mIsContiguous = list.isContiguous();
            } else {
                if (list.isContiguous() != mIsContiguous) {
                    throw new IllegalArgumentException("AdapterHelper cannot handle both contiguous"
                            + " and non-contiguous lists.");
                }
            }
        }

        if (list == mList) {
            // nothing to do
            return;
        }

        if (list == null) {
            if (mUpdateScheduled) {
                throw new IllegalStateException("TODO: this");
            }

            mListUpdateCallback.onRemoved(0, mList.size());
            mList.removeCallback(mPagedListCallback);
            mList = null;
            return;
        }

        if (mList == null) {
            // fast simple first insert
            mListUpdateCallback.onInserted(0, list.size());
            mList = list;
            list.addCallback(null, mPagedListCallback);
            return;
        }

        if (!mUpdateScheduled) {
            // first update scheduled on this list, so capture mList as a snapshot, removing
            // callbacks so we don't have resolve updates against a moving target
            mList.removeCallback(mPagedListCallback);
            mList = mList.snapshot();
        }

        final int runGeneration = ++mMaxScheduledGeneration;
        final PagedList<T> oldSnapshot = mList;
        final PagedList<T> newSnapshot = list.snapshot();
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult result;
                if (mIsContiguous) {
                    result = ContiguousDiffHelper.computeDiff(
                            (NullPaddedList<T>) oldSnapshot, (NullPaddedList<T>) newSnapshot,
                            mDiffCallback, /*TODO*/false);
                } else {
                    result = SparseDiffHelper.computeDiff(
                            (PageArrayList<T>) oldSnapshot, (PageArrayList<T>) newSnapshot,
                            mDiffCallback, /*TODO*/false);
                }

                mMainThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxScheduledGeneration == runGeneration) {
                            mUpdateScheduled = false;
                            latchPagedList(list, newSnapshot, result);
                        }
                    }
                });
            }
        });
    }

    private void latchPagedList(
            PagedList<T> newList, PagedList<T> diffSnapshot,
            DiffUtil.DiffResult diffResult) {
        if (mIsContiguous) {
            ContiguousDiffHelper.dispatchDiff(mListUpdateCallback,
                    (NullPaddedList<T>) mList, (ContiguousPagedList<T>) newList, diffResult);
        } else {
            SparseDiffHelper.dispatchDiff(mListUpdateCallback, diffResult);
        }
        mList = newList;
        newList.addCallback(diffSnapshot, mPagedListCallback);
    }

    /**
     * Builder class for {@link PagedListAdapterHelper}.
     * <p>
     * You must at minimum specify a {@link DiffCallback}, and either a RecyclerView
     * {@link android.support.v7.widget.RecyclerView.Adapter}, or a {@link ListUpdateCallback}.
     *
     * @param <Value> Type of the PagedList or LiveData&lt;PagedList> that will be presented by the
     *               AdapterHelper
     */
    public static class Builder<Value> {
        private DiffCallback<Value> mDiffCallback;
        private ListUpdateCallback mUpdateCallback;
        private LifecycleOwner mLifecycle;
        private LiveData<PagedList<Value>> mLiveData;
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;

        /**
         * Sets the {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}
         * instance that will receive the update events.
         * <p>
         * If you have a more complex case where your adapter has additional items from different
         * data sources, you can use the {@link #setUpdateCallback(ListUpdateCallback)} to manually
         * dispatch changes to your adapter.
         *
         * @param adapter The adapter to receive change/move/insert/remove updates when the data
         *                provided by the helper changes.
         */
        public Builder<Value> setUpdateAdapter(RecyclerView.Adapter adapter) {
            return setUpdateCallback(new AdapterCallback(adapter));
        }

        /**
         * Sets the ListUpdateCallback that will receive updates as the data maintained by the
         * helper is updated.
         * <p>
         * In simple cases, you can instead pass your Adapter to
         * {@link #setUpdateAdapter(RecyclerView.Adapter)} to receive updates in the form of e.g.
         * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRangeChanged(int, int)}
         * or
         * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRangeInserted(int, int)}
         * on your adapter.
         *
         * @param callback The callback to receive change/move/insert/remove updates when the data
         *                 provided by the helper changes.
         */
        @SuppressWarnings("WeakerAccess")
        public Builder<Value> setUpdateCallback(ListUpdateCallback callback) {
            mUpdateCallback = callback;
            return this;
        }

        /**
         * The {@link DiffCallback} to be used while diffing an old list with the updated one.
         * Must be provided.
         *
         * @param diffCallback The {@link DiffCallback} instance to compare items in the list.
         * @return this
         */
        public Builder<Value> setDiffCallback(DiffCallback<Value> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        /**
         * Assigns a lifecycle to the {@link PagedListAdapterHelper} so that it can cancel the
         * observers automatically when the lifecycle is destroyed. This is especially useful if
         * the actual {@link PagedList} is owned by a
         * {@link android.arch.lifecycle.ViewModel ViewModel} or any other class that outlives
         * the {@link android.app.Activity Activity} or the
         * {@link android.support.v4.app.Fragment Fragment}.
         * <p>
         * Optional. If you don't provide this and the {@link PagedList} outlives the
         * {@link android.support.v7.widget.RecyclerView.Adapter Adapter}, you should call
         * {@link PagedListAdapterHelper#setPagedList(PagedList)} with {@code null} when the UI
         * element is destroyed.
         *
         * @param lifecycleOwner The {@link LifecycleOwner} where the adapter lives.
         * @return this
         */
        public Builder<Value> setLifecycleOwner(LifecycleOwner lifecycleOwner) {
            mLifecycle = lifecycleOwner;
            return this;
        }

        /**
         * If provided, the created {@link PagedListAdapterHelper} will observe the given
         * {@code source} automatically. If you provide a {@code source}, you must also provide a
         * {@link LifecycleOwner} via the {@link #setLifecycleOwner(LifecycleOwner)} method.
         *
         * @param source The LiveData source that should be observed.
         * @return this
         */
        public Builder<Value> setSource(LiveData<PagedList<Value>> source) {
            // TODO: pass LifecycleOwner, remove setLifecycleOwner?
            mLiveData = source;
            return this;
        }

        /**
         * If provided, {@link PagedListAdapterHelper} will use the given executor to execute
         * adapter update notifications on the main thread.
         * <p>
         * If not provided, it will default to the UI thread.
         *
         * @param executor The executor which can run tasks in the UI thread.
         * @return this
         */
        @SuppressWarnings("unused")
        public Builder<Value> setMainThreadExecutor(Executor executor) {
            mMainThreadExecutor = executor;
            return this;
        }

        /**
         * If provided, {@link PagedListAdapterHelper} will use the given executor to calculate the
         * diff between an old and a new list.
         * <p>
         * If not provided, defaults to the IO thread pool from Architecture Components.
         *
         * @param executor The background executor to run list diffing.
         * @return this
         */
        @SuppressWarnings("unused")
        public Builder<Value> setBackgroundThreadExecutor(Executor executor) {
            mBackgroundThreadExecutor = executor;
            return this;
        }

        /**
         * Creates a {@link PagedListAdapterHelper} with the given parameters.
         *
         * @return A new PagedListAdapterHelper.
         */
        public PagedListAdapterHelper<Value> build() {
            if (mDiffCallback == null) {
                throw new IllegalArgumentException("Must provide a diffCallback");
            }
            if (mUpdateCallback == null) {
                throw new IllegalArgumentException(
                        "must provide either an adapter or update callback");
            }
            if (mBackgroundThreadExecutor == null) {
                mBackgroundThreadExecutor = AppToolkitTaskExecutor.getIOThreadExecutor();
            }
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = AppToolkitTaskExecutor.getMainThreadExecutor();
            }
            final PagedListAdapterHelper<Value> result =
                    new PagedListAdapterHelper<>(
                            mMainThreadExecutor,
                            mBackgroundThreadExecutor,
                            mUpdateCallback, mDiffCallback,
                            mLiveData == null);

            if (mLifecycle != null) {
                mLifecycle.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void clear() {
                        result.internalSetPagedList(null);
                    }
                });
            }
            if (mLiveData != null) {
                if (mLifecycle == null) {
                    throw new IllegalArgumentException(
                            "If you provide a LiveData to be observed, you must also provide a"
                                    + " Livecycle via the setLifecycle() method.");
                }
                mLiveData.observe(mLifecycle, new Observer<PagedList<Value>>() {
                    @Override
                    public void onChanged(@Nullable PagedList<Value> valuePagedList) {
                        result.internalSetPagedList(valuePagedList);
                    }
                });
            }
            return result;
        }
    }
}
