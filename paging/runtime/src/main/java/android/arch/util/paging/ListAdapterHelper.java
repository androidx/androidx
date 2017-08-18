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

    private List<T> mList;

    // True if background thread executor has at least one task scheduled
    private boolean mUpdateScheduled;

    // Max generation of currently scheduled runnable
    private int mMaxScheduledGeneration;


    /**
     * Get the item from the current List at the specified index.
     *
     * @param index Index of item to get, must be >= 0, and &lt; {@link #getItemCount()}.
     * @return The item at the specified List position.
     */
    public T getItem(int index) {
        if (mList == null) {
            throw new IllegalArgumentException("No current list");
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
     * Pass a new PagedList to the AdapterHelper. Adapter updates will be computed on a background
     * thread.
     * <p>
     * If a PagedList is already present, a diff will be computed asynchronously on a background
     * thread. When the diff is computed, it will be applied (dispatched to the
     * {@link ListUpdateCallback}), and the new PagedList will be swapped in.
     * <p>
     * If this AdapterHelper is already consuming data from a LiveData&lt;PagedList>, calling this
     * method manually will throw.
     *
     * @param newList The new PagedList.
     */
    @SuppressWarnings("WeakerAccess")
    public void setList(final List<T> newList) {
        if (newList == mList) {
            // nothing to do
            return;
        }

        if (newList == null) {
            if (mUpdateScheduled) {
                // incrementing the generation effectively ignores any current running diffs
                mMaxScheduledGeneration++;
            }

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

        final int runGeneration = ++mMaxScheduledGeneration;
        final List<T> oldList = mList;
        mUpdateScheduled = true;
        mConfig.mBackgroundThreadExecutor.execute(new Runnable() {
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
                        return mConfig.mDiffCallback.areItemsTheSame(
                                oldList.get(oldItemPosition), newList.get(newItemPosition));
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return mConfig.mDiffCallback.areContentsTheSame(
                                oldList.get(oldItemPosition), newList.get(newItemPosition));
                    }
                });

                mConfig.mMainThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxScheduledGeneration == runGeneration) {
                            mUpdateScheduled = false;
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
