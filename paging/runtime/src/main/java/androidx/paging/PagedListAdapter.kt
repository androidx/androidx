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

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ConcatAdapter
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
 * interface UserDao {
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
@Deprecated(
    message = "PagedListAdapter is deprecated and has been replaced by PagingDataAdapter",
    replaceWith = ReplaceWith(
        "PagingDataAdapter<T, VH>",
        "androidx.paging.PagingDataAdapter"
    )
)
abstract class PagedListAdapter<T : Any, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH> {
    @Suppress("DEPRECATION")
    internal val differ: AsyncPagedListDiffer<T>
    @Suppress("DEPRECATION")
    private val listener = { previousList: PagedList<T>?, currentList: PagedList<T>? ->
        this@PagedListAdapter.onCurrentListChanged(currentList)
        this@PagedListAdapter.onCurrentListChanged(previousList, currentList)
    }

    /**
     * Returns the [PagedList] currently being displayed by the [PagedListAdapter].
     *
     * This is not necessarily the most recent list passed to [submitList], because a diff is
     * computed asynchronously between the new list and the current list before updating the
     * currentList value. May be null if no PagedList is being presented.
     *
     * @return The list currently being displayed.
     *
     * @see onCurrentListChanged
     */
    @Suppress("DEPRECATION")
    open val currentList: PagedList<T>?
        get() = differ.currentList

    /**
     * Creates a [PagedListAdapter] with default threading and
     * [androidx.recyclerview.widget.ListUpdateCallback].
     *
     * Convenience for [PagedListAdapter], which uses default threading behavior.
     *
     * @param diffCallback The [DiffUtil.ItemCallback] instance to
     * compare items in the list.
     */
    protected constructor(diffCallback: DiffUtil.ItemCallback<T>) {
        @Suppress("DEPRECATION")
        differ = AsyncPagedListDiffer(this, diffCallback)
        differ.addPagedListListener(listener)
    }

    protected constructor(config: AsyncDifferConfig<T>) {
        @Suppress("DEPRECATION")
        differ = AsyncPagedListDiffer(AdapterListUpdateCallback(this), config)
        differ.addPagedListListener(listener)
    }

    /**
     * Set the new list to be displayed.
     *
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param pagedList The new list to be displayed.
     */
    open fun submitList(@Suppress("DEPRECATION") pagedList: PagedList<T>?) =
        differ.submitList(pagedList)

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
     * it is committed.
     */
    open fun submitList(
        @Suppress("DEPRECATION") pagedList: PagedList<T>?,
        commitCallback: Runnable?
    ) = differ.submitList(pagedList, commitCallback)

    protected open fun getItem(position: Int) = differ.getItem(position)

    override fun getItemCount() = differ.itemCount

    /**
     * Called when the current PagedList is updated.
     *
     * This may be dispatched as part of [submitList] if a background diff isn't
     * needed (such as when the first list is passed, or the list is cleared). In either case,
     * PagedListAdapter will simply call
     * [notifyItemRangeInserted/Removed(0, mPreviousSize)][notifyItemRangeInserted].
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
    open fun onCurrentListChanged(@Suppress("DEPRECATION") currentList: PagedList<T>?) {
    }

    /**
     * Called when the current PagedList is updated.
     *
     * This may be dispatched as part of [submitList] if a background diff isn't
     * needed (such as when the first list is passed, or the list is cleared). In either case,
     * PagedListAdapter will simply call
     * [notifyItemRangeInserted/Removed(0, mPreviousSize)][notifyItemRangeInserted].
     *
     * This method will *not*be called when the Adapter switches from presenting a PagedList
     * to a snapshot version of the PagedList during a diff. This means you cannot observe each
     * PagedList via this method.
     *
     * @param previousList [PagedList] that was previously displayed, may be null.
     * @param currentList new [PagedList] being displayed, may be null.
     *
     * @see currentList
     */
    open fun onCurrentListChanged(
        @Suppress("DEPRECATION") previousList: PagedList<T>?,
        @Suppress("DEPRECATION") currentList: PagedList<T>?
    ) {
    }

    /**
     * Add a [LoadState] listener to observe the loading state of the current [PagedList].
     *
     * As new PagedLists are submitted and displayed, the listener will be notified to reflect
     * current [LoadType.REFRESH], [LoadType.PREPEND], and [LoadType.APPEND] states.
     *
     * @param listener Listener to receive [LoadState] updates.
     *
     * @see removeLoadStateListener
     */
    open fun addLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        differ.addLoadStateListener(listener)
    }

    /**
     * Remove a previously registered [LoadState] listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    open fun removeLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        differ.removeLoadStateListener(listener)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.PREPEND] [LoadState] as a list item at the end of the presented list.
     */
    fun withLoadStateHeader(
        header: LoadStateAdapter<*>
    ): ConcatAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.PREPEND) {
                header.loadState = loadState
            }
        }
        return ConcatAdapter(header, this)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.APPEND] [LoadState] as a list item at the start of the presented list.
     */
    fun withLoadStateFooter(
        footer: LoadStateAdapter<*>
    ): ConcatAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.APPEND) {
                footer.loadState = loadState
            }
        }
        return ConcatAdapter(this, footer)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.PREPEND] and [LoadType.APPEND] [LoadState]s as list items at the start and end
     * respectively.
     */
    fun withLoadStateHeaderAndFooter(
        header: LoadStateAdapter<*>,
        footer: LoadStateAdapter<*>
    ): ConcatAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.PREPEND) {
                header.loadState = loadState
            } else if (loadType == LoadType.APPEND) {
                footer.loadState = loadState
            }
        }
        return ConcatAdapter(header, this, footer)
    }
}
