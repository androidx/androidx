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

import java.util.ArrayList;
import java.util.List;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PageArrayList<T> extends PagedList<T> {
    // partial list of pages, doesn't include pages below the lowest accessed, or above the highest
    final ArrayList<List<T>> mPages;

    // to access page at index N, do mPages.get(N - mPageIndexOffset), but do bounds checking first!
    int mPageIndexOffset;

    final int mPageSize;
    final int mCount;
    final int mMaxPageCount;

    PageArrayList(int pageSize, int count) {
        mPages = new ArrayList<>();
        mPageSize = pageSize;
        mCount = count;
        mMaxPageCount = (mCount + mPageSize - 1) / mPageSize;
    }

    private PageArrayList(PageArrayList<T> other) {
        mPages = other.isImmutable() ? other.mPages : new ArrayList<>(other.mPages);
        mPageIndexOffset = other.mPageIndexOffset;
        mPageSize = other.mPageSize;
        mCount = other.size();
        mMaxPageCount = other.mMaxPageCount;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= mCount) {
            throw new IllegalArgumentException();
        }

        int localPageIndex = getLocalPageIndex(index);

        List<T> page = getPage(localPageIndex);

        if (page == null) {
            // page empty
            return null;
        }

        return page.get(index % mPageSize);
    }

    @Nullable
    private List<T> getPage(int localPageIndex) {
        if (localPageIndex < 0 || localPageIndex >= mPages.size()) {
            // page not present
            return null;
        }

        return mPages.get(localPageIndex);
    }

    private int getLocalPageIndex(int index) {
        return index / mPageSize - mPageIndexOffset;
    }

    @Override
    public void loadAround(int index) {
        // do nothing - immutable, so no fetching will be done
    }

    @Override
    public int size() {
        return mCount;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    boolean hasPage(int pageIndex) {
        final int localPageIndex = pageIndex - mPageIndexOffset;
        List<T> page = getPage(localPageIndex);
        return page != null && page.size() != 0;
    }

    @Override
    public PagedList<T> snapshot() {
        if (isImmutable()) {
            return this;
        }
        return new PageArrayList<>(this);
    }

    @Override
    boolean isContiguous() {
        return false;
    }

    @Override
    public void addWeakCallback(@Nullable PagedList<T> previousSnapshot,
            @NonNull Callback callback) {
        // no op, immutable
    }

    @Override
    public void removeWeakCallback(Callback callback) {
        // no op, immutable
    }
}
