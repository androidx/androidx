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

package androidx.paging

import androidx.paging.PagedList.LoadState
import androidx.paging.PagedList.LoadType
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView.Adapter] base class for presenting paged data from [androidx.paging.PagedList]s in
 * a [RecyclerView].
 *
 * This class is a convenience wrapper around [AsyncPagedListDiffer] that implements common default
 * behavior for item counting, and listening to PagedList update callbacks.
 *
 * While using a LiveData<PagedList> is an easy way to provide data to the adapter, it isn't
 * required - you can use [submitList] when new lists are available.
 *
 * PagedListAdapter listens to PagedList loading callbacks as pages are loaded, and uses DiffUtil on
 * a background thread to compute fine grained updates as new PagedLists are received.
 *
 * Handles both the internal paging of the list as more data is loaded, and updates in the form of
 * new PagedLists.
 *
 * A complete usage pattern with Room would look like this:
 * ```
 * @Dao
 *     interface UserDao {
 *     @Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract DataSource.Factory<Integer, User> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData<PagedList<User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = new LivePagedListBuilder&lt;>(
 *         userDao.usersByLastName(), /* page size */ 20).build();
 *     }
 * }
 *
 * class MyActivity extends AppCompatActivity {
 *     @Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = new ViewModelProvider(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter();
 *         viewModel.usersList.observe(this, pagedList -> adapter.submitList(pagedList));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends PagedListAdapter<User, UserViewHolder> {
 *     public UserAdapter() {
 *         super(DIFF_CALLBACK);
 *     }
 *     @Override
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
 *     public static final DiffUtil.ItemCallback&lt;User> DIFF_CALLBACK =
 *             new DiffUtil.ItemCallback<User>() {
 *         @Override
 *         public boolean areItemsTheSame(@NonNull User oldUser, @NonNull User newUser) {
 *             // User properties may have changed if reloaded from the DB, but ID is fixed
 *             return oldUser.getId() == newUser.getId();
 *         }
 *         @Override
 *         public boolean areContentsTheSame(@NonNull User oldUser, @NonNull User newUser) {
 *             // NOTE: if you use equals, your object must properly override Object#equals()
 *             // Incorrectly returning false here will result in too many animations.
 *             return oldUser.equals(newUser);
 *         }
 *     }
 * }
 * ```
 *
 * Advanced users that wish for more control over adapter behavior, or to provide a specific base
 * class should refer to [AsyncPagedListDiffer], which provides the mapping from paging
 * events to adapter-friendly callbacks.
 *
 * @param T Type of the PagedLists this Adapter will receive.
 * @param VH A class that extends ViewHolder that will be used by the adapter.
 */
abstract class PagedListAdapter<T : Any, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH> {
    internal val differ: AsyncPagedListDiffer<T>
    private val listener = { previousList: PagedList<T>?, currentList: PagedList<T>? ->
        @Suppress("DEPRECATION")
        this@PagedListAdapter.onCurrentListChanged(currentList)
        this@PagedListAdapter.onCurrentListChanged(previousList, currentList)
    }
    private val loadStateListener
        get() = this::onLoadStateChanged

    /**
     * Returns the PagedList currently being displayed by the Adapter.
     *
     * This is not necessarily the most recent list passed to [submitList], because a diff is
     * computed asynchronously between the new list and the current list before updating the
     * currentList value. May be null if no PagedList is being presented.
     *
     * @return The list currently being displayed.
     *
     * @see onCurrentListChanged
     */
    open val currentList: PagedList<T>?
        get() = differ.currentList

    /**
     * Creates a PagedListAdapter with default threading and
     * [androidx.recyclerview.widget.ListUpdateCallback].
     *
     * Convenience for [.PagedListAdapter], which uses default threading
     * behavior.
     *
     * @param diffCallback The [DiffUtil.ItemCallback] instance to
     * compare items in the list.
     */
    protected constructor(diffCallback: DiffUtil.ItemCallback<T>) {
        differ = AsyncPagedListDiffer(this, diffCallback)
        differ.addPagedListListener(listener)
        differ.addLoadStateListener(loadStateListener)
    }

    protected constructor(config: AsyncDifferConfig<T>) {
        differ = AsyncPagedListDiffer(AdapterListUpdateCallback(this), config)
        differ.addPagedListListener(listener)
        differ.addLoadStateListener(loadStateListener)
    }

    /**
     * Set the new list to be displayed.
     *
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param pagedList The new list to be displayed.
     */
    open fun submitList(pagedList: PagedList<T>?) = differ.submitList(pagedList)

    /**
     * Set the new list to be displayed.
     *
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * The commit callback can be used to know when the PagedList is committed, but note that it
     * may not be executed. If PagedList B is submitted immediately after PagedList A, and is
     * committed directly, the callback associated with PagedList A will not be run.
     *
     * @param pagedList The new list to be displayed.
     * @param commitCallback Optional runnable that is executed when the PagedList is committed, if
     *                       it is committed.
     */
    open fun submitList(pagedList: PagedList<T>?, commitCallback: Runnable?) =
        differ.submitList(pagedList, commitCallback)

    protected open fun getItem(position: Int) = differ.getItem(position)

    override fun getItemCount() = differ.itemCount

    /**
     * Called when the current PagedList is updated.
     *
     * This may be dispatched as part of [.submitList] if a background diff isn't
     * needed (such as when the first list is passed, or the list is cleared). In either case,
     * PagedListAdapter will simply call
     * [notifyItemRangeInserted/Removed(0, mPreviousSize)][.notifyItemRangeInserted].
     *
     * This method will *not*be called when the Adapter switches from presenting a PagedList
     * to a snapshot version of the PagedList during a diff. This means you cannot observe each
     * PagedList via this method.
     *
     * @param currentList new PagedList being displayed, may be null.
     *
     * @see currentList
     */
    @Deprecated(
        "Use the two argument variant instead.",
        ReplaceWith("onCurrentListChanged(previousList, currentList)")
    )
    open fun onCurrentListChanged(currentList: PagedList<T>?) {
    }

    /**
     * Called when the current PagedList is updated.
     *
     * This may be dispatched as part of [.submitList] if a background diff isn't
     * needed (such as when the first list is passed, or the list is cleared). In either case,
     * PagedListAdapter will simply call
     * [notifyItemRangeInserted/Removed(0, mPreviousSize)][notifyItemRangeInserted].
     *
     * This method will *not*be called when the Adapter switches from presenting a PagedList
     * to a snapshot version of the PagedList during a diff. This means you cannot observe each
     * PagedList via this method.
     *
     * @param previousList PagedList that was previously displayed, may be null.
     * @param currentList new PagedList being displayed, may be null.
     *
     * @see currentList
     */
    open fun onCurrentListChanged(previousList: PagedList<T>?, currentList: PagedList<T>?) {
    }

    /**
     * Called when the LoadState for a particular type of load (START, END, REFRESH) has
     * changed.
     *
     * REFRESH events can be used to drive a `SwipeRefreshLayout`, or START/END events
     * can be used to drive loading spinner items in the Adapter.
     *
     * @param type Type of load - START, END, or REFRESH.
     * @param state State of load - IDLE, LOADING, DONE, ERROR, or RETRYABLE_ERROR
     * @param error Error, if in an error state, null otherwise.
     */
    open fun onLoadStateChanged(type: LoadType, state: LoadState, error: Throwable?) {
    }

    /**
     * Add a [LoadStateListener] to observe the loading state of the current [PagedList].
     *
     * As new PagedLists are submitted and displayed, the callback will be notified to reflect
     * current REFRESH, START, and END states.
     *
     * @param callback [LoadStateListener] to receive updates.
     *
     * @see removeLoadStateListener
     */
    open fun addLoadStateListener(callback: LoadStateListener) {
        differ.addLoadStateListener(callback)
    }

    /**
     * Remove a previously registered [LoadStateListener].
     *
     * @param callback Previously registered callback.
     * @see addLoadStateListener
     */
    open fun removeLoadStateListener(callback: LoadStateListener) {
        differ.removeLoadStateListener(callback)
    }
}
