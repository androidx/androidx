/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import static android.support.v17.leanback.widget.ObjectAdapter.NO_ID;

/**
 * A row in the RowContainerFragment.  This is the basic class for all Rows.
 * Developer usually overrides {@link ListRow}, but may override this class
 * for non-list Row (e.g. a HtmlRow).
 */
public class Row {

    private static final int FLAG_ID_USE_MASK = 1;
    private static final int FLAG_ID_USE_HEADER = 1;
    private static final int FLAG_ID_USE_ID = 0;

    private int mFlags = FLAG_ID_USE_HEADER;
    private HeaderItem mHeaderItem;
    private long mId = NO_ID;

    public Row(long id, HeaderItem headerItem) {
        setId(id);
        setHeaderItem(headerItem);
    }

    public Row(HeaderItem headerItem) {
        setHeaderItem(headerItem);
    }

    public Row() {
    }

    /**
     * Get optional {@link HeaderItem} that represents metadata for the row.
     */
    public final HeaderItem getHeaderItem() {
        return mHeaderItem;
    }

    /**
     * Set the {@link HeaderItem} that represents metadata for the row.
     */
    public final void setHeaderItem(HeaderItem headerItem) {
        mHeaderItem = headerItem;
    }

    /**
     * Set id for this row.
     */
    public final void setId(long id) {
        mId = id;
        setFlags(FLAG_ID_USE_ID, FLAG_ID_USE_MASK);
    }

    /**
     * Returns a unique identifier for this row.  If {@link #setId(long)}
     * is ever called, it will return this id; else returns {@link HeaderItem#getId()}
     * if header item is null; otherwise returns NO_ID.
     */
    public final long getId() {
        if ( (mFlags & FLAG_ID_USE_MASK) == FLAG_ID_USE_HEADER) {
            HeaderItem header = getHeaderItem();
            if (header != null) {
                return header.getId();
            }
            return NO_ID;
        } else {
            return mId;
        }
    }

    final void setFlags(int flags, int mask) {
        mFlags = (mFlags & ~mask) | (flags & mask);
    }

    final int getFlags() {
        return mFlags;
    }
}
