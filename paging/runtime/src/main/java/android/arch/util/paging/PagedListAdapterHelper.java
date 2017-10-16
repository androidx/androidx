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
import android.support.v7.util.ListUpdateCallback;

import java.util.concurrent.Executor;

/**
 * Helper object for mapping a {@link PagedList} into a
 * {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter} - both the paging in
 * of new content as more data is loaded, and updates in the form of new PagedLists.
 *
 * @param <Value> Type of the PagedLists this helper will receive.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PagedListAdapterHelper<Value> extends PagerBaseAdapterHelper<Value> {
    private PagedList.ChangeCallback mChangeCallback = new PagedList.ChangeCallback() {
        @Override
        public void onInserted(int start, int count) {
            mListUpdateCallback.onInserted(start, count);
        }
    };

    public PagedListAdapterHelper(
            @NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            @NonNull ListUpdateCallback listUpdateCallback,
            @NonNull DiffCallback<Value> diffCallback) {
        super(mainThreadExecutor, backgroundThreadExecutor, listUpdateCallback, diffCallback);
    }

    @Override
    @NonNull
    public Value get(int index) {
        return super.get(index);
    }

    /**
     * Sets the paged list for this adapter helper. If you are manually observing the
     * {@link PagedList} for changes, you should call this method with the new
     * {@link PagedList} when the previous one is invalidated.
     * <p>
     * Adapter helper will calculate the diff between this list and the previous one on a background
     * thread then replace the data with this one, while unsubscribing from the previous list and
     * subscribing to the new one.
     *
     * @param newList The new PagedList to observe.
     */
    @MainThread
    public void setPagedList(@Nullable PagedList<Value> newList) {
        setPagerBase(newList);
    }

    @Override
    void addCallback(PagerBase<Value> list) {
        ((PagedList<Value>) list).addCallback(mChangeCallback);
    }

    @Override
    void removeCallback(PagerBase<Value> list) {
        ((PagedList<Value>) list).removeCallback(mChangeCallback);
    }
}
