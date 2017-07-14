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

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.concurrent.Executor;

/**
 * @param <Value> Type of the PagerBases this helper will receive.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PagerBaseAdapterHelper<Value> {
    final ListUpdateCallback mListUpdateCallback;
    private final DiffCallback<Value> mDiffCallback;
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;

    /**
     * Default ListUpdateCallback that dispatches directly to an adapter. Can be replaced by a
     * custom ListUpdateCallback if e.g. your adapter has a header in it, and so has an offset
     * between list positions and adapter positions.
     */
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

    PagerBaseAdapterHelper(
            @NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            @NonNull ListUpdateCallback listUpdateCallback,
            @NonNull DiffCallback<Value> diffCallback) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mListUpdateCallback = listUpdateCallback;
        mDiffCallback = diffCallback;
    }

    private PagerBase<Value> mList;
    private PagerBase<Value> mProcessingList;
    private PagerBase<Value> mQueuedList;
    private DiffUtil.DiffResult mDiffResult;

    /**
     * Get the current list item at the given index. Index must be less than value of
     * {@link #getItemCount()}.
     */
    public Value get(int index) {
        return mList.get(index);
    }

    /**
     * Returns the number of items in the list. This count includes all items (even the ones that
     * are not yet loaded into memory).
     *
     * @return The number of items in the list.
     */
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    @NonNull
    private DiffUtil.DiffResult computeDiff(
            final PagerBase<Value> oldList, final PagerBase<Value> newList) {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return null;
            }

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
                Value oldItem = oldList.access(oldItemPosition);
                Value newItem = newList.access(newItemPosition);
                if (oldItem == newItem) {
                    return true;
                }
                if (oldItem == null || newItem == null) {
                    return false;
                }
                return mDiffCallback.areItemsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Value oldItem = oldList.access(oldItemPosition);
                Value newItem = newList.access(newItemPosition);
                if (oldItem == newItem) {
                    return true;
                }
                if (oldItem == null || newItem == null) {
                    return false;
                }

                return mDiffCallback.areContentsTheSame(oldItem, newItem);
            }
        }, false);
    }

    private void applyDiff(DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(mListUpdateCallback);
    }

    private Runnable mBackgroundDiffRunnable = new Runnable() {
        @WorkerThread
        @Override
        public void run() {
            mDiffResult = computeDiff(mList, mProcessingList);
            mMainThreadExecutor.execute(mApplyDiffRunnable);
        }
    };

    private Runnable mApplyDiffRunnable = new Runnable() {
        @MainThread
        @Override
        public void run() {
            if (mQueuedList == null) {
                // null list queued, trigger immediately
                removeCallback(mList);
                mListUpdateCallback.onRemoved(0, mList.size());
                mList = null;
                mProcessingList = null;
                mDiffResult = null;
                return;
            }

            // apply processing list, dispatching updates
            removeCallback(mList);
            mList = mProcessingList;
            addCallback(mList);

            applyDiff(mDiffResult);
            mDiffResult = null;

            if (mQueuedList == mProcessingList) {
                // no further work
                mProcessingList = null;
                mQueuedList = null;
            } else {
                // enqueue diff for most recent PagerBase
                mProcessingList = mQueuedList;
                mBackgroundThreadExecutor.execute(mBackgroundDiffRunnable);
            }
        }
    };

    @MainThread
    void setPagerBase(@Nullable PagerBase<Value> newList) {
        if (mList == null) {
            // first list
            mList = newList;
            if (mList != null) {
                addCallback(mList);
                mListUpdateCallback.onInserted(0, mList.size());
            }
        } else if (mProcessingList == null) {
            if (newList == null) {
                // swap in immediately
                removeCallback(mList);
                mListUpdateCallback.onRemoved(0, mList.size());
                mList = null;
            } else {
                // not working on a list, enqueue runnable
                mProcessingList = newList;
                mQueuedList = newList;
                mBackgroundThreadExecutor.execute(mBackgroundDiffRunnable);
            }
        } else {
            // already working on new list, enqueue work. Note that this includes a null list,
            // which is greedily applied in mApplyDiffRunnable. For simplicity, we don't attempt to
            mQueuedList = newList;
        }

    }

    abstract void addCallback(PagerBase<Value> list);
    abstract void removeCallback(PagerBase<Value> list);

    @Override
    public String toString() {
        return mList + ", " + mProcessingList + ", " + mQueuedList;
    }
}
