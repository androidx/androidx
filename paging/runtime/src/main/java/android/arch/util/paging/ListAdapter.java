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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter RecyclerView.Adapter} base class for presenting list data from a
 * LiveData of Lists in a {@link RecyclerView}.
 * <p>
 * This class is a convenience wrapper around ListAdapterHelper that implements common default
 * behavior for item access and counting.
 * <p>
 * While using a LiveData&lt;List> is an easy way to provide data to the adapter, it isn't required
 * - you can use {@link #setList(List)} when new lists are available.
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
 * class MyActivity extends Activity implements LifecycleRegistryOwner {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter();
 *         LiveListAdapterUtil.observe(viewModel.usersList, this, adapter);
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends ListAdapter&lt;User, UserViewHolder> {
 *     public UserAdapter() {
 *         super(User.DIFF_CALLBACK);
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         holder.bindTo(getItem(position));
 *     }
 * }
 *
 * </pre>
 *
 * Advanced users that wish for more control over adapter behavior, or to provide a specific base
 * class should refer to {@link ListAdapterHelper}, which provides custom mapping from diff events
 * to adapter positions.
 *
 * @param <T> Type of the Lists this Adapter will receive.
 * @param <VH> A class that extends ViewHolder that will be used by the adapter.
 */
public abstract class ListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private final ListAdapterHelper<T> mHelper;

    @SuppressWarnings("unused")
    protected ListAdapter(@NonNull DiffCallback<T> diffCallback) {
        mHelper = new ListAdapterHelper<>(new ListAdapterHelper.AdapterCallback(this),
                new ListAdapterConfig.Builder<T>().setDiffCallback(diffCallback).build());
    }

    @SuppressWarnings("unused")
    protected ListAdapter(@NonNull ListAdapterConfig<T> config) {
        mHelper = new ListAdapterHelper<>(new ListAdapterHelper.AdapterCallback(this), config);
    }

    /**
     * Set the new list to be displayed.
     * <p>
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    public void setList(List<T> list) {
        mHelper.setList(list);
    }

    protected T getItem(int position) {
        return mHelper.getItem(position);
    }

    @Override
    public int getItemCount() {
        return mHelper.getItemCount();
    }
}
