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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PageArrayList<T> extends PagedList<T> {
    int mListOffset;
    final ArrayList<List<T>> mList;
    final int mPageSize;
    final int mCount;
    final int mMaxPageCount;

    PageArrayList(int pageSize, int count) {
        mList = new ArrayList<>();
        mPageSize = pageSize;
        mCount = count;
        mMaxPageCount = (mCount + mPageSize - 1) / mPageSize;
    }

    PageArrayList(PageArrayList<T> other) {
        mListOffset = other.mListOffset;
        mList = other.isImmutable() ? other.mList : new ArrayList<>(other.mList);
        mPageSize = other.mPageSize;
        mCount = other.size();
        mMaxPageCount = other.mMaxPageCount;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= mCount) {
            throw new IllegalArgumentException();
        }

        int pageIndex = index / mPageSize - mListOffset;

        if (pageIndex < 0 || pageIndex > mList.size()) {
            // page not present
            return null;
        }

        List<T> page = mList.get(pageIndex);

        if (page == null) {
            // page empty
            return null;
        }

        return page.get(index % mPageSize);
    }

    int getPageIndex(int index) {
        return index / mPageSize - mListOffset;
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
    public void addCallback(@Nullable PagedList<T> previousSnapshot, Callback callback) {
        // no op, immutable
    }

    @Override
    public void removeCallback(Callback callback) {
        // no op, immutable
    }
}
