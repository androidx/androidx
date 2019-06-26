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

import androidx.annotation.VisibleForTesting
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LiveData
import androidx.paging.PagedList.LoadState
import androidx.paging.PagedList.LoadStateManager
import androidx.paging.PagedList.LoadType
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.CopyOnWriteArrayList

typealias OnCurrentListChanged<T> =
            (previousList: PagedList<T>?, currentList: PagedList<T>?) -> Unit

/**
 * Helper object for mapping a [androidx.paging.PagedList] into a
 * [RecyclerView.Adapter][androidx.recyclerview.widget.RecyclerView.Adapter].
 *
 * For simplicity, the [PagedListAdapter] wrapper class can often be used instead of the differ
 * directly. This diff class is exposed for complex cases, and where overriding an adapter base
 * class to support paging isn't convenient.
 *
 * When consuming a [LiveData] of PagedList, you can observe updates and dispatch them directly to
 * [submitList]. The AsyncPagedListDiffer then can present this updating data set simply for an
 * adapter. It listens to PagedList loading callbacks, and uses DiffUtil on a background thread to
 * compute updates as new PagedLists are received.
 *
 * It provides a simple list-like API with [getItem] and [itemCount] for an adapter to acquire
 * and present data objects.
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
 *         usersList = new LivePagedListBuilder<>(
 *         userDao.usersByLastName(), /* page size */ 20).build();
 *     }
 * }
 *
 * class MyActivity extends AppCompatActivity {
 *     @Override
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
 *     private final AsyncPagedListDiffer&lt;User> differ =
 *             new AsyncPagedListDiffer(this, DIFF_CALLBACK);
 *     @Override
 *     public int getItemCount() {
 *         return differ.getItemCount();
 *     }
 *     public void submitList(PagedList&lt;User> pagedList) {
 *         differ.submitList(pagedList);
 *     }
 *     @Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = differ.getItem(position);
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
 *         @Override
 *         public boolean areItemsTheSame(
 *             @NonNull User oldUser, @NonNull User newUser) {
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
 * @param T Type of the PagedLists this differ will receive.
 */
open class AsyncPagedListDiffer<T : Any> {
    /**
     * updateCallback notifications must only be notified *after* new data and item count are stored
     * this ensures Adapter#notifyItemRangeInserted etc are accessing the new data
     */
    internal lateinit var updateCallback: ListUpdateCallback

    @Suppress("MemberVisibilityCanBePrivate") // synthetic access
    internal val config: AsyncDifferConfig<T>

    internal var mainThreadExecutor = ArchTaskExecutor.getMainThreadExecutor()

    @VisibleForTesting
    internal val listeners = CopyOnWriteArrayList<PagedListListener<T>>()
    private var isContiguous: Boolean = false
    private var pagedList: PagedList<T>? = null
    private var snapshot: PagedList<T>? = null

    /**
     *  Max generation of currently scheduled runnable
     */
    @Suppress("MemberVisibilityCanBePrivate") // synthetic access
    internal var maxScheduledGeneration: Int = 0

    private val loadStateManager: LoadStateManager = object : LoadStateManager() {
        override fun onStateChanged(type: LoadType, state: LoadState, error: Throwable?) {
            // Don't need to post - PagedList will already have done that
            loadStateListeners.forEach { it(type, state, error) }
        }
    }

    private val loadStateListener = loadStateManager::onStateChanged

    internal val loadStateListeners: MutableList<LoadStateListener> =
        CopyOnWriteArrayList()

    private val pagedListCallback = object : PagedList.Callback() {
        override fun onInserted(position: Int, count: Int) =
            updateCallback.onInserted(position, count)

        override fun onRemoved(position: Int, count: Int) =
            updateCallback.onRemoved(position, count)

        override fun onChanged(position: Int, count: Int) {
            // NOTE: pass a null payload to convey null -> item
            updateCallback.onChanged(position, count, null)
        }
    }

    /**
     * Get the number of items currently presented by this Differ. This value can be directly
     * returned to [RecyclerView.Adapter.getItemCount].
     *
     * @return Number of items being presented.
     */
    open val itemCount: Int
        get() = currentList?.size ?: 0

    /**
     * Returns the PagedList currently being displayed by the differ.
     *
     * This is not necessarily the most recent list passed to [submitList], because a diff is
     * computed asynchronously between the new list and the current list before updating the
     * currentList value. May be `null` if no [PagedList] is being presented.
     *
     * @return The list currently being displayed, may be `null`.
     */
    open val currentList: PagedList<T>?
        get() = snapshot ?: pagedList

    /**
     * Listener for when the current [PagedList] is updated.
     *
     * @param T Type of items in [PagedList]
     */
    interface PagedListListener<T : Any> {
        /**
         * Called after the current PagedList has been updated.
         *
         * @param previousList The previous list, may be null.
         * @param currentList The new current list, may be null.
         */
        fun onCurrentListChanged(previousList: PagedList<T>?, currentList: PagedList<T>?)
    }

    /**
     * Wrapper for [OnCurrentListChanged] for when the current [PagedList] is updated.
     *
     * @param T Type of items in [PagedList]
     */
    private class OnCurrentListChangedWrappper<T : Any>(
        val callback: OnCurrentListChanged<T>
    ) : PagedListListener<T> {
        override fun onCurrentListChanged(previousList: PagedList<T>?, currentList: PagedList<T>?) {
            callback(previousList, currentList)
        }
    }

    /**
     * Convenience for `AsyncPagedListDiffer(new AdapterListUpdateCallback(adapter),
     * new AsyncDifferConfig.Builder<T>(diffCallback).build();`
     *
     * @param adapter Adapter that will receive update signals.
     * @param diffCallback The [DiffUtil.ItemCallback] instance to compare items in the list.
     */
    constructor(adapter: RecyclerView.Adapter<*>, diffCallback: DiffUtil.ItemCallback<T>) {
        updateCallback = AdapterListUpdateCallback(adapter)
        config = AsyncDifferConfig.Builder(diffCallback).build()
    }

    constructor(listUpdateCallback: ListUpdateCallback, config: AsyncDifferConfig<T>) {
        updateCallback = listUpdateCallback
        this.config = config
    }

    /**
     * Get the item from the current PagedList at the specified index.
     *
     * Note that this operates on both loaded items and null padding within the PagedList.
     *
     * @param index Index of item to get, must be >= 0, and < `getItemCount`.
     * @return The item, or null, if a null placeholder is at the specified position.
     */
    open fun getItem(index: Int): T? {
        val snapshot = this.snapshot
        val pagedList = this.pagedList

        return when {
            snapshot != null -> snapshot[index]
            pagedList != null -> {
                pagedList.loadAround(index)
                pagedList[index]
            }
            else -> throw IndexOutOfBoundsException("Item count is zero, getItem() call is invalid")
        }
    }

    /**
     * Pass a new [PagedList] to the differ.
     *
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * [ListUpdateCallback]), and the new PagedList will be swapped in as the [currentList].
     *
     * @param pagedList The new PagedList.
     */
    open fun submitList(pagedList: PagedList<T>?) = submitList(pagedList, null)

    /**
     * Pass a new PagedList to the differ.
     *
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * [ListUpdateCallback]), and the new PagedList will be swapped in as the
     * [current list][currentList].
     *
     * The commit callback can be used to know when the PagedList is committed, but note that it
     * may not be executed. If PagedList B is submitted immediately after PagedList A, and is
     * committed directly, the callback associated with PagedList A will not be run.
     *
     * @param pagedList The new [PagedList].
     * @param commitCallback Optional runnable that is executed when the PagedList is committed, if
     *                       it is committed.
     */
    open fun submitList(pagedList: PagedList<T>?, commitCallback: Runnable?) {
        if (pagedList != null) {
            if (currentList == null) {
                isContiguous = pagedList.isContiguous
            } else if (pagedList.isContiguous != isContiguous) {
                throw IllegalArgumentException(
                    "AsyncPagedListDiffer cannot handle both contiguous and non-contiguous lists."
                )
            }
        }

        // incrementing generation means any currently-running diffs are discarded when they finish
        val runGeneration = ++maxScheduledGeneration

        if (pagedList === this.pagedList) {
            // nothing to do (Note - still had to inc generation, since may have ongoing work)
            commitCallback?.run()
            return
        }

        val previous = currentList

        if (pagedList == null) {
            val removedCount = itemCount
            val currentPagedList = this.pagedList
            if (currentPagedList != null) {
                currentPagedList.removeWeakCallback(pagedListCallback)
                currentPagedList.removeWeakLoadStateListener(loadStateListener)
                this.pagedList = null
            } else if (snapshot != null) {
                snapshot = null
            }
            // dispatch update callback after updating pagedList/snapshot
            updateCallback.onRemoved(0, removedCount)
            onCurrentListChanged(previous, null, commitCallback)
            return
        }

        if (currentList == null) {
            // fast simple first insert
            this.pagedList = pagedList
            pagedList.addWeakLoadStateListener(loadStateListener)
            pagedList.addWeakCallback(null, pagedListCallback)

            // dispatch update callback after updating pagedList/snapshot
            updateCallback.onInserted(0, pagedList.size)

            onCurrentListChanged(null, pagedList, commitCallback)
            return
        }

        this.pagedList?.let {
            // first update scheduled on this list, so capture mPages as a snapshot, removing
            // callbacks so we don't have resolve updates against a moving target
            it.removeWeakCallback(pagedListCallback)
            it.removeWeakLoadStateListener(loadStateListener)

            snapshot = it.snapshot() as PagedList<T>
            this.pagedList = null
        }

        val oldSnapshot = snapshot
        if (oldSnapshot == null || this.pagedList != null) {
            throw IllegalStateException("must be in snapshot state to diff")
        }

        val newSnapshot = pagedList.snapshot() as PagedList<T>
        config.backgroundThreadExecutor.execute {
            val result = oldSnapshot.getStorage().computeDiff(
                newSnapshot.getStorage(),
                config.diffCallback
            )

            mainThreadExecutor.execute {
                if (maxScheduledGeneration == runGeneration) {
                    latchPagedList(
                        pagedList,
                        newSnapshot,
                        result,
                        oldSnapshot.lastLoad,
                        commitCallback
                    )
                }
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate") // synthetic access
    internal fun latchPagedList(
        newList: PagedList<T>,
        diffSnapshot: PagedList<T>,
        diffResult: DiffUtil.DiffResult,
        lastAccessIndex: Int,
        commitCallback: Runnable?
    ) {
        val previousSnapshot = snapshot
        if (previousSnapshot == null || pagedList != null) {
            throw IllegalStateException("must be in snapshot state to apply diff")
        }

        pagedList = newList
        newList.addWeakLoadStateListener(loadStateListener)
        snapshot = null

        // dispatch update callback after updating pagedList/snapshot

        previousSnapshot.getStorage().dispatchDiff(
            updateCallback,
            newList.getStorage(),
            diffResult
        )

        newList.addWeakCallback(diffSnapshot, pagedListCallback)

        if (!newList.isEmpty()) {
            // Transform the last loadAround() index from the old list to the new list by passing it
            // through the DiffResult. This ensures the lastKey of a positional PagedList is carried
            // to new list even if no in-viewport item changes (AsyncPagedListDiffer#get not called)
            // Note: we don't take into account loads between new list snapshot and new list, but
            // this is only a problem in rare cases when placeholders are disabled, and a load
            // starts (for some reason) and finishes before diff completes.
            val newPosition = previousSnapshot.getStorage().transformAnchorIndex(
                diffResult,
                diffSnapshot.getStorage(),
                lastAccessIndex
            )

            // Trigger load in new list at this position, clamped to list bounds.
            // This is a load, not just an update of last load position, since the new list may be
            // incomplete. If new list is subset of old list, but doesn't fill the viewport, this
            // will likely trigger a load of new data.
            newList.loadAround(Math.max(0, Math.min(newList.size - 1, newPosition)))
        }

        onCurrentListChanged(previousSnapshot, pagedList, commitCallback)
    }

    private fun onCurrentListChanged(
        previousList: PagedList<T>?,
        currentList: PagedList<T>?,
        commitCallback: Runnable?
    ) {
        listeners.forEach { it.onCurrentListChanged(previousList, currentList) }
        commitCallback?.run()
    }

    /**
     * Add a [PagedListListener] to receive updates when the current [PagedList] changes.
     *
     * @param listener Listener to receive updates.
     *
     * @see currentList
     * @see removePagedListListener
     */
    open fun addPagedListListener(listener: PagedListListener<T>) {
        listeners.add(listener)
    }

    /**
     * Add a [OnCurrentListChanged] callback to receive updates when the current [PagedList]
     * changes.
     *
     * @param callback [OnCurrentListChanged] callback to receive updates.
     *
     * @see currentList
     * @see removePagedListListener
     */
    fun addPagedListListener(callback: OnCurrentListChanged<T>) {
        listeners.add(OnCurrentListChangedWrappper(callback))
    }

    /**
     * Remove a previously registered [PagedListListener].
     *
     * @param listener Previously registered listener.
     *
     * @see currentList
     * @see addPagedListListener
     */
    open fun removePagedListListener(listener: PagedListListener<T>) {
        listeners.remove(listener)
    }

    /**
     * Remove a previously registered [OnCurrentListChanged] callback.
     *
     * @param callback Previously registered callback.
     *
     * @see currentList
     * @see addPagedListListener
     */
    fun removePagedListListener(callback: OnCurrentListChanged<T>) {
        listeners.removeAll { it is OnCurrentListChangedWrappper<T> && it.callback === callback }
    }

    /**
     * Add a [LoadStateListener] to observe the loading state of the current [PagedList].
     *
     * As new PagedLists are submitted and displayed, the listener will be notified to reflect
     * current REFRESH, START, and END states.
     *
     * @param listener [LoadStateListener] to receive updates.
     *
     * @see removeLoadStateListener
     */
    open fun addLoadStateListener(listener: LoadStateListener) {
        val pagedList = this.pagedList
        if (pagedList != null) {
            pagedList.addWeakLoadStateListener(listener)
        } else {
            loadStateManager.dispatchCurrentLoadState(listener)
        }
        loadStateListeners.add(listener)
    }

    /**
     * Remove a previously registered [LoadStateListener].
     *
     * @param listener Previously registered listener.
     *
     * @see currentList
     * @see addPagedListListener
     */
    open fun removeLoadStateListener(listener: LoadStateListener) {
        loadStateListeners.remove(listener)
        pagedList?.removeWeakLoadStateListener(listener)
    }
}
