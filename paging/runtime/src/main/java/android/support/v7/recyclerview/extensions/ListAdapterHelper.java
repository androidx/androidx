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

package android.support.v7.recyclerview.extensions;

import android.arch.lifecycle.LiveData;
import android.support.annotation.RestrictTo;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * Helper object for displaying a List in {@link RecyclerView.Adapter RecyclerView.Adapter}, which
 * signals the adapter of changes when the List is changed by computing changes with DiffUtil in the
 * background.
 * <p>
 * For simplicity, the {@link ListAdapter} wrapper class can often be used instead of the
 * helper directly. This helper class is exposed for complex cases, and where overriding an adapter
 * base class to support List diffing isn't convenient.
 * <p>
 * The ListAdapterHelper can take a {@link LiveData} of List and present the data simply for an
 * adapter. It computes differences in List contents via DiffUtil on a background thread as new
 * Lists are received.
 * <p>
 * It provides a simple list-like API with {@link #getItem(int)} and {@link #getItemCount()} for an
 * adapter to acquire and present data objects.
 * <p>
 * A complete usage pattern with Room would look like this:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract LiveData&lt;List&lt;User>> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData&lt;List&lt;User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = userDao.usersByLastName();
 *     }
 * }
 *
 * class MyActivity extends AppCompatActivity {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter();
 *         viewModel.usersList.observe(this, list -> adapter.setList(list));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends RecyclerView.Adapter&lt;UserViewHolder> {
 *     private final ListAdapterHelper&lt;User> mHelper;
 *     public UserAdapter(ListAdapterHelper.Builder&lt;User> builder) {
 *         mHelper = new ListAdapterHelper(this, User.DIFF_CALLBACK);
 *     }
 *     {@literal @}Override
 *     public int getItemCount() {
 *         return mHelper.getItemCount();
 *     }
 *     public void setList(List&lt;User> list) {
 *         mHelper.setList(list);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = mHelper.getItem(position);
 *         holder.bindTo(user);
 *     }
 *     public static final DiffCallback&lt;User> DIFF_CALLBACK = new DiffCallback&lt;User>() {
 *         {@literal @}Override
 *         public boolean areItemsTheSame(
 *                 {@literal @}NonNull User oldUser, {@literal @}NonNull User newUser) {
 *             // User properties may have changed if reloaded from the DB, but ID is fixed
 *             return oldUser.getId() == newUser.getId();
 *         }
 *         {@literal @}Override
 *         public boolean areContentsTheSame(
 *                 {@literal @}NonNull User oldUser, {@literal @}NonNull User newUser) {
 *             // NOTE: if you use equals, your object must properly override Object#equals()
 *             // Incorrectly returning false here will result in too many animations.
 *             return oldUser.equals(newUser);
 *         }
 *     }
 * }</pre>
 *
 * @param <T> Type of the lists this helper will receive.
 */
public class ListAdapterHelper<T> {
    private final ListUpdateCallback mUpdateCallback;
    private final ListAdapterConfig<T> mConfig;

    @SuppressWarnings("WeakerAccess")
    public ListAdapterHelper(ListUpdateCallback listUpdateCallback,
            ListAdapterConfig<T> config) {
        mUpdateCallback = listUpdateCallback;
        mConfig = config;
    }

    /**
     * Default ListUpdateCallback that dispatches directly to an adapter. Can be replaced by a
     * custom ListUpdateCallback if e.g. your adapter has a header in it, and so has an offset
     * between list positions and adapter positions.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class AdapterCallback implements ListUpdateCallback {
        private final RecyclerView.Adapter mAdapter;

        public AdapterCallback(RecyclerView.Adapter adapter) {
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

    private List<T> mList;

    // Max generation of currently scheduled runnable
    private int mMaxScheduledGeneration;


    /**
     * Get the item from the current List at the specified index.
     *
     * @param index Index of item to get, must be >= 0, and &lt; {@link #getItemCount()}.
     * @return The item at the specified List position.
     */
    @SuppressWarnings("WeakerAccess")
    public T getItem(int index) {
        if (mList == null) {
            throw new IndexOutOfBoundsException("Item count is zero, getItem() call is invalid");
        }

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
     * Pass a new List to the AdapterHelper. Adapter updates will be computed on a background
     * thread.
     * <p>
     * If a List is already present, a diff will be computed asynchronously on a background thread.
     * When the diff is computed, it will be applied (dispatched to the {@link ListUpdateCallback}),
     * and the new List will be swapped in.
     *
     * @param newList The new List.
     */
    @SuppressWarnings("WeakerAccess")
    public void setList(final List<T> newList) {
        if (newList == mList) {
            // nothing to do
            return;
        }

        // incrementing generation means any currently-running diffs are discarded when they finish
        final int runGeneration = ++mMaxScheduledGeneration;

        if (newList == null) {
            mUpdateCallback.onRemoved(0, mList.size());
            mList = null;
            return;
        }

        if (mList == null) {
            // fast simple first insert
            mUpdateCallback.onInserted(0, newList.size());
            mList = newList;
            return;
        }

        final List<T> oldList = mList;
        mConfig.getBackgroundThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldList.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return newList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return mConfig.getDiffCallback().areItemsTheSame(
                                oldList.get(oldItemPosition), newList.get(newItemPosition));
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return mConfig.getDiffCallback().areContentsTheSame(
                                oldList.get(oldItemPosition), newList.get(newItemPosition));
                    }
                });

                mConfig.getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxScheduledGeneration == runGeneration) {
                            latchList(newList, result);
                        }
                    }
                });
            }
        });
    }

    private void latchList(List<T> newList, DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(mUpdateCallback);
        mList = newList;
    }
}
