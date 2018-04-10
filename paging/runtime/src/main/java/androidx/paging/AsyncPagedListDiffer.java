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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.Executor;

/**
 * Helper object for mapping a {@link PagedList} into a
 * {@link androidx.recyclerview.widget.RecyclerView.Adapter RecyclerView.Adapter}.
 * <p>
 * For simplicity, the {@link PagedListAdapter} wrapper class can often be used instead of the
 * differ directly. This diff class is exposed for complex cases, and where overriding an adapter
 * base class to support paging isn't convenient.
 * <p>
 * When consuming a {@link LiveData} of PagedList, you can observe updates and dispatch them
 * directly to {@link #submitList(PagedList)}. The AsyncPagedListDiffer then can present this
 * updating data set simply for an adapter. It listens to PagedList loading callbacks, and uses
 * DiffUtil on a background thread to compute updates as new PagedLists are received.
 * <p>
 * It provides a simple list-like API with {@link #getItem(int)} and {@link #getItemCount()} for an
 * adapter to acquire and present data objects.
 * <p>
 * A complete usage pattern with Room would look like this:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract DataSource.Factory&lt;Integer, User> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData&lt;PagedList&lt;User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = new LivePagedListBuilder&lt;>(
 *                 userDao.usersByLastName(), /* page size {@literal *}/ 20).build();
 *     }
 * }
 *
 * class MyActivity extends AppCompatActivity {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         final UserAdapter adapter = new UserAdapter();
 *         viewModel.usersList.observe(this, pagedList -> adapter.submitList(pagedList));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends RecyclerView.Adapter&lt;UserViewHolder> {
 *     private final AsyncPagedListDiffer&lt;User> mDiffer
 *             = new AsyncPagedListDiffer(this, DIFF_CALLBACK);
 *     {@literal @}Override
 *     public int getItemCount() {
 *         return mDiffer.getItemCount();
 *     }
 *     public void submitList(PagedList&lt;User> pagedList) {
 *         mDiffer.submitList(pagedList);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = mDiffer.getItem(position);
 *         if (user != null) {
 *             holder.bindTo(user);
 *         } else {
 *             // Null defines a placeholder item - AsyncPagedListDiffer will automatically
 *             // invalidate this row when the actual object is loaded from the database
 *             holder.clear();
 *         }
 *     }
 *     public static final DiffUtil.ItemCallback&lt;User> DIFF_CALLBACK =
 *             new DiffUtil.ItemCallback&lt;User>() {
 *          {@literal @}Override
 *          public boolean areItemsTheSame(
 *                  {@literal @}NonNull User oldUser, {@literal @}NonNull User newUser) {
 *              // User properties may have changed if reloaded from the DB, but ID is fixed
 *              return oldUser.getId() == newUser.getId();
 *          }
 *          {@literal @}Override
 *          public boolean areContentsTheSame(
 *                  {@literal @}NonNull User oldUser, {@literal @}NonNull User newUser) {
 *              // NOTE: if you use equals, your object must properly override Object#equals()
 *              // Incorrectly returning false here will result in too many animations.
 *              return oldUser.equals(newUser);
 *          }
 *      }
 * }</pre>
 *
 * @param <T> Type of the PagedLists this differ will receive.
 */
public class AsyncPagedListDiffer<T> {
    // updateCallback notifications must only be notified *after* new data and item count are stored
    // this ensures Adapter#notifyItemRangeInserted etc are accessing the new data
    private final ListUpdateCallback mUpdateCallback;
    private final AsyncDifferConfig<T> mConfig;

    @SuppressWarnings("RestrictedApi")
    Executor mMainThreadExecutor = ArchTaskExecutor.getMainThreadExecutor();

    // TODO: REAL API
    interface PagedListListener<T> {
        void onCurrentListChanged(@Nullable PagedList<T> currentList);
    }

    @Nullable
    PagedListListener<T> mListener;

    private boolean mIsContiguous;

    private PagedList<T> mPagedList;
    private PagedList<T> mSnapshot;

    // Max generation of currently scheduled runnable
    private int mMaxScheduledGeneration;

    /**
     * Convenience for {@code AsyncPagedListDiffer(new AdapterListUpdateCallback(adapter),
     * new AsyncDifferConfig.Builder<T>(diffCallback).build();}
     *
     * @param adapter Adapter that will receive update signals.
     * @param diffCallback The {@link DiffUtil.ItemCallback DiffUtil.ItemCallback} instance to
     * compare items in the list.
     */
    @SuppressWarnings("WeakerAccess")
    public AsyncPagedListDiffer(@NonNull RecyclerView.Adapter adapter,
            @NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mUpdateCallback = new AdapterListUpdateCallback(adapter);
        mConfig = new AsyncDifferConfig.Builder<>(diffCallback).build();
    }

    @SuppressWarnings("WeakerAccess")
    public AsyncPagedListDiffer(@NonNull ListUpdateCallback listUpdateCallback,
            @NonNull AsyncDifferConfig<T> config) {
        mUpdateCallback = listUpdateCallback;
        mConfig = config;
    }

    private PagedList.Callback mPagedListCallback = new PagedList.Callback() {
        @Override
        public void onInserted(int position, int count) {
            mUpdateCallback.onInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            mUpdateCallback.onRemoved(position, count);
        }

        @Override
        public void onChanged(int position, int count) {
            // NOTE: pass a null payload to convey null -> item
            mUpdateCallback.onChanged(position, count, null);
        }
    };

    /**
     * Get the item from the current PagedList at the specified index.
     * <p>
     * Note that this operates on both loaded items and null padding within the PagedList.
     *
     * @param index Index of item to get, must be >= 0, and &lt; {@link #getItemCount()}.
     * @return The item, or null, if a null placeholder is at the specified position.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public T getItem(int index) {
        if (mPagedList == null) {
            if (mSnapshot == null) {
                throw new IndexOutOfBoundsException(
                        "Item count is zero, getItem() call is invalid");
            } else {
                return mSnapshot.get(index);
            }
        }

        mPagedList.loadAround(index);
        return mPagedList.get(index);
    }

    /**
     * Get the number of items currently presented by this Differ. This value can be directly
     * returned to {@link RecyclerView.Adapter#getItemCount()}.
     *
     * @return Number of items being presented.
     */
    @SuppressWarnings("WeakerAccess")
    public int getItemCount() {
        if (mPagedList != null) {
            return mPagedList.size();
        }

        return mSnapshot == null ? 0 : mSnapshot.size();
    }

    /**
     * Pass a new PagedList to the differ.
     * <p>
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * {@link ListUpdateCallback}), and the new PagedList will be swapped in as the
     * {@link #getCurrentList() current list}.
     *
     * @param pagedList The new PagedList.
     */
    @SuppressWarnings("ReferenceEquality")
    public void submitList(final PagedList<T> pagedList) {
        if (pagedList != null) {
            if (mPagedList == null && mSnapshot == null) {
                mIsContiguous = pagedList.isContiguous();
            } else {
                if (pagedList.isContiguous() != mIsContiguous) {
                    throw new IllegalArgumentException("AsyncPagedListDiffer cannot handle both"
                            + " contiguous and non-contiguous lists.");
                }
            }
        }

        if (pagedList == mPagedList) {
            // nothing to do
            return;
        }

        // incrementing generation means any currently-running diffs are discarded when they finish
        final int runGeneration = ++mMaxScheduledGeneration;

        if (pagedList == null) {
            int removedCount = getItemCount();
            if (mPagedList != null) {
                mPagedList.removeWeakCallback(mPagedListCallback);
                mPagedList = null;
            } else if (mSnapshot != null) {
                mSnapshot = null;
            }
            // dispatch update callback after updating mPagedList/mSnapshot
            mUpdateCallback.onRemoved(0, removedCount);
            if (mListener != null) {
                mListener.onCurrentListChanged(null);
            }
            return;
        }

        if (mPagedList == null && mSnapshot == null) {
            // fast simple first insert
            mPagedList = pagedList;
            pagedList.addWeakCallback(null, mPagedListCallback);

            // dispatch update callback after updating mPagedList/mSnapshot
            mUpdateCallback.onInserted(0, pagedList.size());

            if (mListener != null) {
                mListener.onCurrentListChanged(pagedList);
            }
            return;
        }

        if (mPagedList != null) {
            // first update scheduled on this list, so capture mPages as a snapshot, removing
            // callbacks so we don't have resolve updates against a moving target
            mPagedList.removeWeakCallback(mPagedListCallback);
            mSnapshot = (PagedList<T>) mPagedList.snapshot();
            mPagedList = null;
        }

        if (mSnapshot == null || mPagedList != null) {
            throw new IllegalStateException("must be in snapshot state to diff");
        }

        final PagedList<T> oldSnapshot = mSnapshot;
        final PagedList<T> newSnapshot = (PagedList<T>) pagedList.snapshot();
        mConfig.getBackgroundThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult result;
                result = PagedStorageDiffHelper.computeDiff(
                        oldSnapshot.mStorage,
                        newSnapshot.mStorage,
                        mConfig.getDiffCallback());

                mMainThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxScheduledGeneration == runGeneration) {
                            latchPagedList(pagedList, newSnapshot, result);
                        }
                    }
                });
            }
        });
    }

    private void latchPagedList(
            PagedList<T> newList, PagedList<T> diffSnapshot,
            DiffUtil.DiffResult diffResult) {
        if (mSnapshot == null || mPagedList != null) {
            throw new IllegalStateException("must be in snapshot state to apply diff");
        }

        PagedList<T> previousSnapshot = mSnapshot;
        mPagedList = newList;
        mSnapshot = null;

        // dispatch update callback after updating mPagedList/mSnapshot
        PagedStorageDiffHelper.dispatchDiff(mUpdateCallback,
                previousSnapshot.mStorage, newList.mStorage, diffResult);

        newList.addWeakCallback(diffSnapshot, mPagedListCallback);
        if (mListener != null) {
            mListener.onCurrentListChanged(mPagedList);
        }
    }

    /**
     * Returns the PagedList currently being displayed by the differ.
     * <p>
     * This is not necessarily the most recent list passed to {@link #submitList(PagedList)},
     * because a diff is computed asynchronously between the new list and the current list before
     * updating the currentList value. May be null if no PagedList is being presented.
     *
     * @return The list currently being displayed, may be null.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public PagedList<T> getCurrentList() {
        if (mSnapshot != null) {
            return mSnapshot;
        }
        return mPagedList;
    }
}
