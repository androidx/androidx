/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import static androidx.slice.core.SliceHints.HINT_KEY_WORDS;
import static androidx.slice.core.SliceHints.HINT_LAST_UPDATED;
import static androidx.slice.core.SliceHints.HINT_TTL;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.core.SliceQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse a Slice and provide access to some information around its contents.
 */
public class SliceMetadata {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            LOADED_NONE, LOADED_PARTIAL, LOADED_ALL
    })
    public @interface SliceLoadingState{}

    /**
     * Indicates this slice is empty and waiting for content to be loaded.
     */
    public static final int LOADED_NONE = 0;
    /**
     * Indicates this slice has some content but is waiting for other content to be loaded.
     */
    public static final int LOADED_PARTIAL = 1;
    /**
     * Indicates this slice has fully loaded and is not waiting for other content.
     */
    public static final int LOADED_ALL = 2;

    private Slice mSlice;
    private Context mContext;
    private long mExpiry;
    private long mLastUpdated;
    private List<SliceItem> mSliceActions;

    /**
     * Create a SliceMetadata object to provide access to some information around the slice and
     * its contents.
     *
     * @param context the context to use for the slice.
     * @param slice the slice to extract metadata from.
     *
     * @return the metadata associated with the provided slice.
     */
    public static SliceMetadata from(@NonNull Context context, @NonNull Slice slice) {
        return new SliceMetadata(context, slice);
    }

    /**
     * Create a SliceMetadata object to provide access to some information around the slice and
     * its contents.
     *
     * @param context the context to use for the slice.
     * @param slice the slice to extract metadata from.
     */
    private SliceMetadata(@NonNull Context context, @NonNull Slice slice) {
        mSlice = slice;
        mContext = context;
        SliceItem ttlItem = SliceQuery.find(slice, FORMAT_TIMESTAMP, HINT_TTL, null);
        if (ttlItem != null) {
            mExpiry = ttlItem.getTimestamp();
        }
        SliceItem updatedItem = SliceQuery.find(slice, FORMAT_TIMESTAMP, HINT_LAST_UPDATED, null);
        if (updatedItem != null) {
            mLastUpdated = updatedItem.getTimestamp();
        }
        mSliceActions = getSliceActions(mSlice);
    }

    /**
     * @return the group of actions associated with this slice, if they exist.
     */
    @Nullable
    public List<SliceItem> getSliceActions() {
        return mSliceActions;
    }

    /**
     * @return the list of keywords associated with the provided slice, null if no keywords were
     * specified or an empty list if the slice was specified to have no keywords.
     */
    @Nullable
    public List<String> getSliceKeywords() {
        SliceItem keywordGroup = SliceQuery.find(mSlice, FORMAT_SLICE, HINT_KEY_WORDS, null);
        if (keywordGroup != null) {
            List<SliceItem> itemList = SliceQuery.findAll(keywordGroup, FORMAT_TEXT);
            if (itemList != null) {
                ArrayList<String> stringList = new ArrayList<>();
                for (int i = 0; i < itemList.size(); i++) {
                    String keyword = (String) itemList.get(i).getText();
                    if (!TextUtils.isEmpty(keyword)) {
                        stringList.add(keyword);
                    }
                }
                return stringList;
            }
        }
        return null;
    }

    /**
     * @return the current loading state for this slice.
     *
     * @see #LOADED_NONE
     * @see #LOADED_PARTIAL
     * @see #LOADED_ALL
     */
    public int getLoadingState() {
        // Check loading state
        boolean hasHintPartial = SliceQuery.find(mSlice, null, HINT_PARTIAL, null) != null;
        if (mSlice.getItems().size() == 0) {
            // Empty slice
            return LOADED_NONE;
        } else if (hasHintPartial) {
            // Slice with specific content to load
            return LOADED_PARTIAL;
        } else {
            // Full slice
            return LOADED_ALL;
        }
    }

    /**
     * @return the expiry for the content in this slice, or {@link Long#MAX_VALUE} if there is no
     * expiry specified.
     */
    public long getExpiry() {
        return mExpiry;
    }

    /**
     * @return when the slice was created or last updated.
     */
    public long getLastUpdatedTime() {
        return mLastUpdated;
    }

    /**
     * @return the group of actions associated with the provided slice, if they exist.
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static List<SliceItem> getSliceActions(@NonNull Slice slice) {
        SliceItem actionGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_ACTIONS, null);
        String[] hints = new String[] {HINT_ACTIONS, HINT_SHORTCUT};
        return (actionGroup != null)
                ? SliceQuery.findAll(actionGroup, FORMAT_SLICE, hints, null)
                : null;
    }
}
