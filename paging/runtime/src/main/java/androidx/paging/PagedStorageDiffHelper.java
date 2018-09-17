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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

/**
 * Methods for computing and applying DiffResults between PagedLists.
 *
 * To minimize the amount of diffing caused by placeholders, we only execute DiffUtil in a reduced
 * 'diff space' - in the range (computeLeadingNulls..size-computeTrailingNulls).
 *
 * This allows the diff of a PagedList, e.g.:
 *     100 nulls, placeholder page, (empty page) x 5, page, 100 nulls
 *
 * To only inform DiffUtil about single loaded page in this case, by pruning all other nulls from
 * consideration.
 *
 * @see PagedStorage#computeLeadingNulls()
 * @see PagedStorage#computeTrailingNulls()
 */
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

        OffsettingListUpdateCallback(int offset, ListUpdateCallback callback) {
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
            mCallback.onMoved(fromPosition + mOffset, toPosition + mOffset);
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

    /**
     * Given an oldPosition representing an anchor in the old data set, computes its new position
     * after the diff, or a guess if it no longer exists.
     */
    static int transformAnchorIndex(@NonNull DiffUtil.DiffResult diffResult,
            @NonNull PagedStorage oldList, @NonNull PagedStorage newList, final int oldPosition) {
        final int oldOffset = oldList.computeLeadingNulls();

        // diffResult's indices starting after nulls, need to transform to diffutil indices
        // (see also dispatchDiff(), which adds this offset when dispatching)
        int diffIndex = oldPosition - oldOffset;

        final int oldSize = oldList.size() - oldOffset - oldList.computeTrailingNulls();

        // if our anchor is non-null, use it or close item's position in new list
        if (diffIndex >= 0 && diffIndex < oldSize) {
            // search outward from old position for position that maps
            for (int i = 0; i < 30; i++) {
                int positionToTry = diffIndex + (i / 2 * (i % 2 == 1 ? -1 : 1));

                // reject if (null) item was not passed to DiffUtil, and wouldn't be in the result
                if (positionToTry < 0 || positionToTry >= oldList.getStorageCount()) {
                    continue;
                }

                int result = diffResult.convertOldPositionToNew(positionToTry);
                if (result != -1) {
                    // also need to transform from diffutil output indices to newList
                    return result + newList.getLeadingNullCount();
                }
            }
        }

        // not anchored to an item in new list, so just reuse position (clamped to newList size)
        return Math.max(0, Math.min(oldPosition, newList.size() - 1));
    }
}
