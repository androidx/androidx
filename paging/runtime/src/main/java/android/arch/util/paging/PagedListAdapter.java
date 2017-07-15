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

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * {@link RecyclerView.Adapter RecyclerView.Adapter} base class for presenting paged data from
 * {@link PagedList}s in a {@link RecyclerView}.
 * <p>
 * This class is a convenience wrapper around PagedListAdapterHelper that implements common default
 * behavior for item counting, and listening to PagedList update callbacks.
 * <p>
 * Handles both the internal paging of the list as more data is loaded, and updates in the form of
 * new PagedLists.
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
 * class UserAdapter extends PagedListAdapterHelper&lt;User, UserViewHolder> {
 *     public UserAdapter(PagedListAdapterHelper.Builder&lt;User> builder) {
 *         super(builder);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder({@literal @}Nullable User user,
 *             UserViewHolder holder, int position) {
 *         if (user == null) {
 *             // AdapterHelper will automatically invalidate this row when the actual
 *             // object is loaded from the database
 *             holder.clear();
 *         } else {
 *             holder.bindTo(user);
 *         }
 *     }
 * }
 *
 * </pre>
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

    protected PagedListAdapter(PagedListAdapterHelper.Builder<T> builder) {
        mHelper = builder.setUpdateAdapter(this).build();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(mHelper.get(position), holder, position);
    }

    /**
     * Bind the specified view holder with the item, or null if a placeholder should be bound.
     */
    public abstract void onBindViewHolder(@Nullable T item, VH holder, int position);

    @Override
    public int getItemCount() {
        return mHelper.getItemCount();
    }
}
