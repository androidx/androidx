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

import android.arch.lifecycle.LiveData;
import android.support.annotation.Nullable;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.recyclerview.extensions.ListAdapterHelper;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.List;

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
 * The PagedListAdapterHelper can be bound to a {@link LiveData} of PagedList and present the data
 * simply for an adapter. It listens to PagedList loading callbacks, and uses DiffUtil on a
 * background thread to compute updates as new PagedLists are received.
 * <p>
 * It provides a simple list-like API with {@link #getItem(int)} and {@link #getItemCount()} for an
 * adapter to acquire and present data objects.
 * <p>
 * A complete usage pattern with Room would look like this:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract LivePagedListProvider&lt;Integer, User> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData&lt;PagedList&lt;User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = userDao.usersByLastName().create(
 *                 /* initial load position {@literal *}/ 0,
 *                 new PagedList.Config.Builder()
 *                         .setPageSize(50)
 *                         .setPrefetchDistance(50)
 *                         .build());
 *     }
 * }
 *
 * class MyActivity extends AppCompatActivity {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         final UserAdapter&lt;User> adapter = new UserAdapter();
 *         viewModel.usersList.observe(this, pagedList -> adapter.setList(pagedList));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends RecyclerView.Adapter&lt;UserViewHolder> {
 *     private final PagedListAdapterHelper&lt;User> mHelper;
 *     public UserAdapter(PagedListAdapterHelper.Builder&lt;User> builder) {
 *         mHelper = new PagedListAdapterHelper(this, DIFF_CALLBACK);
 *     }
 *     {@literal @}Override
 *     public int getItemCount() {
 *         return mHelper.getItemCount();
 *     }
 *     public void setList(PagedList&lt;User> pagedList) {
 *         mHelper.setList(pagedList);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = mHelper.getItem(position);
 *         if (user != null) {
 *             holder.bindTo(user);
 *         } else {
 *             // Null defines a placeholder item - PagedListAdapterHelper will automatically
 *             // invalidate this row when the actual object is loaded from the database
 *             holder.clear();
 *         }
 *     }
 *     public static final DiffCallback&lt;User> DIFF_CALLBACK = new DiffCallback&lt;User>() {
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
 * @param <T> Type of the PagedLists this helper will receive.
 */
public class PagedListAdapterHelper<T> {
    private final ListUpdateCallback mUpdateCallback;
    private final ListAdapterConfig<T> mConfig;

    // true if our listener is detached from mList, because it's been snapshotted
    private boolean mUpdateScheduled;

    private boolean mIsContiguous;

    private PagedList<T> mList;

    // Max generation of currently scheduled runnable
    private int mMaxScheduledGeneration;

    /**
     * Convenience for {@code PagedListAdapterHelper(new ListAdapterHelper.AdapterCallback(adapter),
     * new ListAdapterConfig.Builder<T>().setDiffCallback(diffCallback).build());
     *
     * @param adapter Adapter that will receive update signals.
     * @param diffCallback The {@link DiffCallback } instance to compare items in the list.
     */
    @SuppressWarnings("WeakerAccess")
    public PagedListAdapterHelper(RecyclerView.Adapter adapter, DiffCallback<T> diffCallback) {
        mUpdateCallback = new ListAdapterHelper.AdapterCallback(adapter);
        mConfig = new ListAdapterConfig.Builder<T>().setDiffCallback(diffCallback).build();
    }

    @SuppressWarnings("WeakerAccess")
    public PagedListAdapterHelper(ListUpdateCallback listUpdateCallback,
            ListAdapterConfig<T> config) {
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
        if (mList == null) {
            throw new IndexOutOfBoundsException("Item count is zero, getItem() call is invalid");
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
    @SuppressWarnings("WeakerAccess")
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    /**
     * Pass a new PagedList to the AdapterHelper.
     * <p>
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * {@link ListUpdateCallback}), and the new PagedList will be swapped in.
     *
     * @param pagedList The new PagedList.
     */
    public void setList(final PagedList<T> pagedList) {
        if (pagedList != null) {
            if (mList == null) {
                mIsContiguous = pagedList.isContiguous();
            } else {
                if (pagedList.isContiguous() != mIsContiguous) {
                    throw new IllegalArgumentException("AdapterHelper cannot handle both contiguous"
                            + " and non-contiguous lists.");
                }
            }
        }

        if (pagedList == mList) {
            // nothing to do
            return;
        }

        // incrementing generation means any currently-running diffs are discarded when they finish
        final int runGeneration = ++mMaxScheduledGeneration;

        if (pagedList == null) {
            mUpdateCallback.onRemoved(0, mList.size());
            mList.removeWeakCallback(mPagedListCallback);
            mList = null;
            return;
        }

        if (mList == null) {
            // fast simple first insert
            mUpdateCallback.onInserted(0, pagedList.size());
            mList = pagedList;
            pagedList.addWeakCallback(null, mPagedListCallback);
            return;
        }

        if (!mList.isImmutable()) {
            // first update scheduled on this list, so capture mPages as a snapshot, removing
            // callbacks so we don't have resolve updates against a moving target
            mList.removeWeakCallback(mPagedListCallback);
            mList = (PagedList<T>) mList.snapshot();
        }

        final PagedList<T> oldSnapshot = mList;
        final List<T> newSnapshot = pagedList.snapshot();
        mUpdateScheduled = true;
        mConfig.getBackgroundThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult result;
                if (mIsContiguous) {
                    result = ContiguousDiffHelper.computeDiff(
                            (NullPaddedList<T>) oldSnapshot, (NullPaddedList<T>) newSnapshot,
                            mConfig.getDiffCallback(), true);
                } else {
                    result = SparseDiffHelper.computeDiff(
                            (PageArrayList<T>) oldSnapshot, (PageArrayList<T>) newSnapshot,
                            mConfig.getDiffCallback(), true);
                }

                mConfig.getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxScheduledGeneration == runGeneration) {
                            mUpdateScheduled = false;
                            latchPagedList(pagedList, newSnapshot, result);
                        }
                    }
                });
            }
        });
    }

    private void latchPagedList(
            PagedList<T> newList, List<T> diffSnapshot,
            DiffUtil.DiffResult diffResult) {
        if (mIsContiguous) {
            ContiguousDiffHelper.dispatchDiff(mUpdateCallback,
                    (NullPaddedList<T>) mList, (ContiguousPagedList<T>) newList, diffResult);
        } else {
            SparseDiffHelper.dispatchDiff(mUpdateCallback, diffResult);
        }
        mList = newList;
        newList.addWeakCallback((PagedList<T>) diffSnapshot, mPagedListCallback);
    }

    /**
     * Returns the list currently being displayed by the AdapterHelper.
     * <p>
     * This is not necessarily the most recent list passed to {@link #setList(PagedList)}, because a
     * diff is computed asynchronously between the new list and the current list before updating the
     * currentList value.
     *
     * @return The list currently being displayed.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public PagedList<T> getCurrentList() {
        return mList;
    }
}
