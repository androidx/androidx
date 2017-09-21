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
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.recyclerview.extensions.ListAdapterHelper;
import android.support.v7.widget.RecyclerView;

/**
 * {@link RecyclerView.Adapter RecyclerView.Adapter} base class for presenting paged data from
 * {@link PagedList}s in a {@link RecyclerView}.
 * <p>
 * This class is a convenience wrapper around PagedListAdapterHelper that implements common default
 * behavior for item counting, and listening to PagedList update callbacks.
 * <p>
 * While using a LiveData&lt;PagedList> is an easy way to provide data to the adapter, it isn't
 * required - you can use {@link #setList(PagedList)} when new lists are available.
 * <p>
 * PagedListAdapter listens to PagedList loading callbacks as pages are loaded, and uses DiffUtil on
 * a background thread to compute fine grained updates as new PagedLists are received.
 * <p>
 * Handles both the internal paging of the list as more data is loaded, and updates in the form of
 * new PagedLists.
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
 *         UserAdapter&lt;User> adapter = new UserAdapter();
 *         viewModel.usersList.observe(this, pagedList -> adapter.setList(pagedList));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends PagedListAdapter&lt;User, UserViewHolder> {
 *     public UserAdapter() {
 *         super(DIFF_CALLBACK);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = getItem(position);
 *         if (user != null) {
 *             holder.bindTo(user);
 *         } else {
 *             // Null defines a placeholder item - PagedListAdapter will automatically invalidate
 *             // this row when the actual object is loaded from the database
 *             holder.clear();
 *         }
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
 * Advanced users that wish for more control over adapter behavior, or to provide a specific base
 * class should refer to {@link PagedListAdapterHelper}, which provides the mapping from paging
 * events to adapter-friendly callbacks.
 *
 * @param <T> Type of the PagedLists this helper will receive.
 * @param <VH> A class that extends ViewHolder that will be used by the adapter.
 */
public abstract class PagedListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private final PagedListAdapterHelper<T> mHelper;

    /**
     * Creates a PagedListAdapter with default threading and
     * {@link android.support.v7.util.ListUpdateCallback}.
     *
     * Convenience for {@link #PagedListAdapter(ListAdapterConfig)}, which uses default threading
     * behavior.
     *
     * @param diffCallback The {@link DiffCallback} instance to compare items in the list.
     */
    protected PagedListAdapter(@NonNull DiffCallback<T> diffCallback) {
        mHelper = new PagedListAdapterHelper<>(this, diffCallback);
    }

    @SuppressWarnings("unused, WeakerAccess")
    protected PagedListAdapter(@NonNull ListAdapterConfig<T> config) {
        mHelper = new PagedListAdapterHelper<>(new ListAdapterHelper.AdapterCallback(this), config);
    }

    /**
     * Set the new list to be displayed.
     * <p>
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param pagedList The new list to be displayed.
     */
    public void setList(PagedList<T> pagedList) {
        mHelper.setList(pagedList);
    }

    @Nullable
    protected T getItem(int position) {
        return mHelper.getItem(position);
    }

    @Override
    public int getItemCount() {
        return mHelper.getItemCount();
    }

    /**
     * Returns the list currently being displayed by the Adapter.
     * <p>
     * This is not necessarily the most recent list passed to {@link #setList(PagedList)}, because a
     * diff is computed asynchronously between the new list and the current list before updating the
     * currentList value.
     *
     * @return The list currently being displayed.
     */
    @Nullable
    public PagedList<T> getCurrentList() {
        return mHelper.getCurrentList();
    }
}
