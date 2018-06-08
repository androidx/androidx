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

package androidx.paging;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

class PagedStorageDiffHelper {
    private PagedStorageDiffHelper() {
    }

    static <T> DiffUtil.DiffResult computeDiff(
            final PagedStorage<T> oldList,
            final PagedStorage<T> newList,
            final DiffUtil.ItemCallback<T> diffCallback) {
        final int oldOffset = oldList.computeLeadingNulls();
        final int newOffset = newList.computeLeadingNulls();

        final int oldSize = oldList.size() - oldOffset - oldList.computeTrailingNulls();
        final int newSize = newList.size() - newOffset - newList.computeTrailingNulls();

        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition + oldOffset);
                T newItem = newList.get(newItemPosition + newList.getLeadingNullCount());
                if (oldItem == null || newItem == null) {
                    return null;
                }
                return diffCallback.getChangePayload(oldItem, newItem);
            }

            @Override
            public int getOldListSize() {
                return oldSize;
            }

            @Override
            public int getNewListSize() {
                return newSize;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition + oldOffset);
                T newItem = newList.get(newItemPosition + newList.getLeadingNullCount());
                if (oldItem == newItem) {
                    return true;
                }
                //noinspection SimplifiableIfStatement
                if (oldItem == null || newItem == null) {
                    return false;
                }
                return diffCallback.areItemsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition + oldOffset);
                T newItem = newList.get(newItemPosition + newList.getLeadingNullCount());
                if (oldItem == newItem) {
                    return true;
                }
                //noinspection SimplifiableIfStatement
                if (oldItem == null || newItem == null) {
                    return false;
                }

                return diffCallback.areContentsTheSame(oldItem, newItem);
            }
        }, true);
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
            final PagedStorage<T> oldList,
            final PagedStorage<T> newList,
            final DiffUtil.DiffResult diffResult) {

        final int trailingOld = oldList.computeTrailingNulls();
        final int trailingNew = newList.computeTrailingNulls();
        final int leadingOld = oldList.computeLeadingNulls();
        final int leadingNew = newList.computeLeadingNulls();

        if (trailingOld == 0
                && trailingNew == 0
                && leadingOld == 0
                && leadingNew == 0) {
            // Simple case, dispatch & return
            diffResult.dispatchUpdatesTo(callback);
            return;
        }

        // First, remove or insert trailing nulls
        if (trailingOld > trailingNew) {
            int count = trailingOld - trailingNew;
            callback.onRemoved(oldList.size() - count, count);
        } else if (trailingOld < trailingNew) {
            callback.onInserted(oldList.size(), trailingNew - trailingOld);
        }

        // Second, remove or insert leading nulls
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
