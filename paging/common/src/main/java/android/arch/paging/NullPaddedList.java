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

import android.support.annotation.RestrictTo;

import java.util.AbstractList;
import java.util.List;

/**
 * NullPaddedList is a simple list, with optional null padding on the beginning and end.
 *
 * @param <Type> The type of the entries in the list.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NullPaddedList<Type> extends AbstractList<Type> {
    List<Type> mList;
    private int mTrailingNullCount;
    private int mLeadingNullCount;
    private int mPositionOffset;

    @Override
    public String toString() {
        return "NullPaddedList " + mLeadingNullCount
                + ", " + mList.size()
                + ", " + mTrailingNullCount;
    }

    /**
     * Create a static, immutable NullPaddedList with the specified list,
     *
     * @param leadingNullCount Number of empty items in advance of the passed list.
     * @param list List of items.
     * @param trailingNullCount Number of empty items following the passed list.
     */
    NullPaddedList(int leadingNullCount, List<Type> list, int trailingNullCount) {
        if (leadingNullCount < 0 || trailingNullCount < 0) {
            throw new IllegalArgumentException("leading/trailing null count must be non-negative");
        }
        if (list == null) {
            throw new IllegalArgumentException("list must be non-null");
        }
        mList = list;
        mLeadingNullCount = leadingNullCount;
        mTrailingNullCount = trailingNullCount;
    }

    NullPaddedList(int leadingNullCount, int totalCount, List<Type> list) {
        if (list == null) {
            throw new IllegalArgumentException("list must be non-null");
        }

        int trailingNullCount = totalCount - (leadingNullCount) - list.size();

        mList = list;
        mLeadingNullCount = leadingNullCount;
        mTrailingNullCount = trailingNullCount;
    }

    NullPaddedList(int positionOffset, List<Type> list) {
        if (list == null) {
            throw new IllegalArgumentException("list must be non-null");
        }

        mList = list;
        mPositionOffset = positionOffset;
    }

    // --------------- PagedList API ---------------

    @Override
    public Type get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException();
        }

        index -= mLeadingNullCount;
        if (index < 0) {
            return null;
        }
        if (index >= mList.size()) {
            return null;
        }
        return mList.get(index);
    }

    @Override
    public final int size() {
        return getLoadedCount() + getLeadingNullCount() + getTrailingNullCount();
    }

    // --------------- Contiguous API ---------------

    public int getPositionOffset() {
        return mPositionOffset;
    }

    /**
     * Number of loaded items. This does not account for leading or trailing null padding.
     *
     * @return Number of loaded items.
     */
    public int getLoadedCount() {
        return mList.size();
    }

    /**
     * Number of empty, unloaded items ahead of the loaded item region.
     *
     * @return Number of nulls before the loaded list.
     */
    public int getLeadingNullCount() {
        return mLeadingNullCount;
    }

    /**
     * Number of empty, unloaded items behind the loaded item region.
     *
     * @return Number of nulls after the loaded list.
     */
    public int getTrailingNullCount() {
        return mTrailingNullCount;
    }
}
