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
class SparseDiffHelper {
    private SparseDiffHelper() {
    }

    @NonNull
    static <T> DiffUtil.DiffResult computeDiff(
            final PageArrayList<T> oldList, final PageArrayList<T> newList,
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
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
                if (oldItem == null || newItem == null) {
                    return null;
                }
                return diffCallback.getChangePayload(oldItem, newItem);
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
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
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
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
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

    static <T> void dispatchDiff(ListUpdateCallback callback,
            final DiffUtil.DiffResult diffResult) {
        // Simple case, dispatch & return
        diffResult.dispatchUpdatesTo(callback);
    }
}
