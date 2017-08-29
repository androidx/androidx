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
import android.support.annotation.RestrictTo;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ContiguousDiffHelper {
    private ContiguousDiffHelper() {
    }

    @NonNull
    static <T> DiffUtil.DiffResult computeDiff(
            final NullPaddedList<T> oldList, final NullPaddedList<T> newList,
            final DiffCallback<T> diffCallback, boolean detectMoves) {

        if (!oldList.isImmutable()) {
            throw new IllegalArgumentException("list must be immutable to safely perform diff");
        }
        if (!newList.isImmutable()) {
            throw new IllegalArgumentException("list must be immutable to safely perform diff");
        }
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.mList.get(oldItemPosition);
                T newItem = newList.mList.get(newItemPosition);
                if (oldItem == null || newItem == null) {
                    return null;
                }
                return diffCallback.getChangePayload(oldItem, newItem);
            }

            @Override
            public int getOldListSize() {
                return oldList.mList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.mList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.mList.get(oldItemPosition);
                T newItem = newList.mList.get(newItemPosition);
                if (oldItem == newItem) {
                    return true;
                }
                if (oldItem == null || newItem == null) {
                    return false;
                }
                return diffCallback.areItemsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.mList.get(oldItemPosition);
                T newItem = newList.mList.get(newItemPosition);
                if (oldItem == newItem) {
                    return true;
                }
                if (oldItem == null || newItem == null) {
                    return false;
                }

                return diffCallback.areContentsTheSame(oldItem, newItem);
            }
        }, detectMoves);
    }

    private static class OffsettingListUpdateCallback implements ListUpdateCallback {
        private final int mOffset;
        private final ListUpdateCallback mCallback;

        private OffsettingListUpdateCallback(int offset, ListUpdateCallback callback) {
            mOffset = offset;
            mCallback = callback;
        }

        @Override
        public void onInserted(int position, int count) {
            mCallback.onInserted(position + mOffset, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            mCallback.onRemoved(position + mOffset, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mCallback.onRemoved(fromPosition + mOffset, toPosition + mOffset);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            mCallback.onChanged(position + mOffset, count, payload);
        }
    }

    /**
     * TODO: improve diffing logic
     *
     * This function currently does a naive diff, assuming null does not become an item, and vice
     * versa (so it won't dispatch onChange events for these). It's similar to passing a list with
     * leading/trailing nulls in the beginning / end to DiffUtil, but dispatches the remove/insert
     * for changed nulls at the beginning / end of the list.
     *
     * Note: if lists mutate between diffing the snapshot and dispatching the diff here, then we
     * handle this by passing the snapshot to the callback, and dispatching those changes
     * immediately after dispatching this diff.
     */
    static <T> void dispatchDiff(ListUpdateCallback callback,
            final NullPaddedList<T> oldList, final NullPaddedList<T> newList,
            final DiffUtil.DiffResult diffResult) {

        if (oldList.getLeadingNullCount() == 0
                && oldList.getTrailingNullCount() == 0
                && newList.getLeadingNullCount() == 0
                && newList.getTrailingNullCount() == 0) {
            // Simple case, dispatch & return
            diffResult.dispatchUpdatesTo(callback);
            return;
        }

        // First, remove or insert trailing nulls
        final int trailingOld = oldList.getTrailingNullCount();
        final int trailingNew = newList.getTrailingNullCount();
        if (trailingOld > trailingNew) {
            int count = trailingOld - trailingNew;
            callback.onRemoved(oldList.size() - count, count);
        } else if (trailingOld < trailingNew) {
            callback.onInserted(oldList.size(), trailingNew - trailingOld);
        }

        // Second, remove or insert leading nulls
        final int leadingOld = oldList.getLeadingNullCount();
        final int leadingNew = newList.getLeadingNullCount();
        if (leadingOld > leadingNew) {
            callback.onRemoved(0, leadingOld - leadingNew);
        } else if (leadingOld < leadingNew) {
            callback.onInserted(0, leadingNew - leadingOld);
        }

        // apply the diff, with an offset if needed
        if (leadingNew != 0) {
            diffResult.dispatchUpdatesTo(new OffsettingListUpdateCallback(leadingNew, callback));
        } else {
            diffResult.dispatchUpdatesTo(callback);
        }
    }
}
