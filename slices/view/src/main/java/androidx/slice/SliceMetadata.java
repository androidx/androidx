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

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_ERROR;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_PERMISSION_REQUEST;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_MAX;
import static android.app.slice.Slice.SUBTYPE_VALUE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.core.SliceHints.HINT_CACHED;
import static androidx.slice.core.SliceHints.SUBTYPE_MIN;
import static androidx.slice.widget.EventInfo.ROW_TYPE_PROGRESS;
import static androidx.slice.widget.EventInfo.ROW_TYPE_SLIDER;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.math.MathUtils;
import androidx.core.util.Pair;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceHints;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.EventInfo;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.RowContent;
import androidx.slice.widget.SliceView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse a {@link Slice} and provide access to information around its contents.
 */
@RequiresApi(19)
public class SliceMetadata {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            LOADED_NONE, LOADED_PARTIAL, LOADED_ALL
    })
    @Retention(RetentionPolicy.SOURCE)
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
    private ListContent mListContent;
    private RowContent mHeaderContent;
    private SliceAction mPrimaryAction;
    private List<SliceAction> mSliceActions;
    private @EventInfo.SliceRowType int mTemplateType;

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
        SliceItem ttlItem = SliceQuery.find(slice, FORMAT_LONG, HINT_TTL, null);
        if (ttlItem != null) {
            mExpiry = ttlItem.getLong();
        }
        SliceItem updatedItem = SliceQuery.find(slice, FORMAT_LONG, HINT_LAST_UPDATED, null);
        if (updatedItem != null) {
            mLastUpdated = updatedItem.getLong();
        }
        mListContent = new ListContent(slice);
        mHeaderContent = mListContent.getHeader();
        mTemplateType = mListContent.getHeaderTemplateType();
        mPrimaryAction = mListContent.getShortcut(mContext);
        mSliceActions = mListContent.getSliceActions();
        if (mSliceActions == null && mHeaderContent != null
                && SliceQuery.hasHints(mHeaderContent.getSliceItem(), HINT_LIST_ITEM)) {
            // It's not a real header, check it for end items.
            List<SliceItem> items = mHeaderContent.getEndItems();
            List<SliceAction> actions = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                if (SliceQuery.find(items.get(i), FORMAT_ACTION) != null) {
                    actions.add(new SliceActionImpl(items.get(i)));
                }
            }
            if (actions.size() > 0) {
                mSliceActions = actions;
            }
        }
    }

    /**
     * @return the title associated with this slice, if it exists.
     */
    @Nullable
    public CharSequence getTitle() {
        CharSequence title = null;
        if (mHeaderContent != null && mHeaderContent.getTitleItem() != null) {
            title = mHeaderContent.getTitleItem().getText();
        }
        if (TextUtils.isEmpty(title) && mPrimaryAction != null) {
            return mPrimaryAction.getTitle();

        }
        return title;
    }

    /**
     * @return the subtitle associated with this slice, if it exists.
     */
    @Nullable
    public CharSequence getSubtitle() {
        if (mHeaderContent != null && mHeaderContent.getSubtitleItem() != null) {
            return mHeaderContent.getSubtitleItem().getText();
        }
        return null;
    }

    /**
     * @return the summary associated with this slice, if it exists.
     */
    @Nullable
    public CharSequence getSummary() {
        if (mHeaderContent != null && mHeaderContent.getSummaryItem() != null) {
            return mHeaderContent.getSummaryItem().getText();
        }
        return null;
    }

    /**
     * @return the group of actions associated with this slice, if they exist.
     */
    @Nullable
    public List<SliceAction> getSliceActions() {
        return mSliceActions;
    }

    /**
     * @return the primary action for this slice, null if none specified.
     */
    @Nullable
    public SliceAction getPrimaryAction() {
        return mPrimaryAction;
    }

    /**
     * @return the type of row that is used for the header of this slice, -1 if unknown.
     */
    public @EventInfo.SliceRowType int getHeaderType() {
        return mTemplateType;
    }

    /**
     * @return whether this slice has content to show when presented
     * in {@link SliceView#MODE_LARGE}.
     */
    public boolean hasLargeMode() {
        return mListContent.getRowItems().size() > 1;
    }

    /**
     * @return the toggles associated with the header of this slice.
     */
    public List<SliceAction> getToggles() {
        List<SliceAction> toggles = new ArrayList<>();
        // Is it the primary action?
        if (mPrimaryAction != null && mPrimaryAction.isToggle()) {
            toggles.add(mPrimaryAction);
        } else if (mSliceActions != null && mSliceActions.size() > 0) {
            for (int i = 0; i < mSliceActions.size(); i++) {
                SliceAction action = mSliceActions.get(i);
                if (action.isToggle()) {
                    toggles.add(action);
                }
            }
        } else {
            toggles = mHeaderContent.getToggleItems();
        }
        return toggles;
    }

    /**
     * Sends the intent to adjust the state of the provided toggle action.
     *
     * @param toggleAction the toggle action.
     * @param toggleValue the new value to set the toggle to.
     * @return whether there was an action to send.
     */
    public boolean sendToggleAction(SliceAction toggleAction, boolean toggleValue)
            throws PendingIntent.CanceledException {
        if (toggleAction != null) {
            Intent intent = new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(EXTRA_TOGGLE_STATE, toggleValue);
            toggleAction.getAction().send(mContext, 0, intent, null, null);
            return true;
        }
        return false;
    }

    /**
     * Gets the input range action associated with the header of this slice, if it exists.
     *
     * @return the {@link android.app.PendingIntent} for the input range.
     */
    @Nullable
    public PendingIntent getInputRangeAction() {
        if (mTemplateType == ROW_TYPE_SLIDER) {
            SliceItem range = mHeaderContent.getRange();
            if (range != null) {
                return range.getAction();
            }
        }
        return null;
    }

    /**
     * Sends the intent to adjust the input range value for the header of this slice, if it exists.
     *
     * @param newValue the value to set the input range to.
     * @return whether there was an action to send.
     */
    public boolean sendInputRangeAction(int newValue) throws PendingIntent.CanceledException {
        if (mTemplateType == ROW_TYPE_SLIDER) {
            SliceItem range = mHeaderContent.getRange();
            if (range != null) {
                // Ensure new value is valid
                Pair<Integer, Integer> validRange = getRange();
                int adjustedValue = MathUtils.clamp(newValue, validRange.first, validRange.second);
                Intent intent = new Intent()
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(EXTRA_RANGE_VALUE, adjustedValue);
                range.fireAction(mContext, intent);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the range information associated with a progress bar or input range associated with this
     * slice, if it exists.
     *
     * @return a pair where the first item is the minimum value of the range and the second item is
     * the maximum value of the range.
     */
    @Nullable
    public Pair<Integer, Integer> getRange() {
        if (mTemplateType == ROW_TYPE_SLIDER
                || mTemplateType == ROW_TYPE_PROGRESS) {
            SliceItem range = mHeaderContent.getRange();
            SliceItem maxItem = SliceQuery.findSubtype(range, FORMAT_INT, SUBTYPE_MAX);
            SliceItem minItem = SliceQuery.findSubtype(range, FORMAT_INT, SUBTYPE_MIN);
            int max = maxItem != null ? maxItem.getInt() : 100; // default max of range
            int min = minItem != null ? minItem.getInt() : 0; // default min of range
            return new Pair<>(min, max);
        }
        return null;
    }

    /**
     * Gets the current value for a progress bar or input range associated with this slice, if it
     * exists, -1 if unknown.
     *
     * @return the current value of a progress bar or input range associated with this slice.
     */
    @NonNull
    public int getRangeValue() {
        if (mTemplateType == ROW_TYPE_SLIDER
                || mTemplateType == ROW_TYPE_PROGRESS) {
            SliceItem range = mHeaderContent.getRange();
            SliceItem currentItem = SliceQuery.findSubtype(range, FORMAT_INT, SUBTYPE_VALUE);
            return currentItem != null ? currentItem.getInt() : -1;
        }
        return -1;

    }

    /**
     * @return the list of keywords associated with the provided slice, null if no keywords were
     * specified or an empty list if the slice was specified to have no keywords.
     */
    @Nullable
    public List<String> getSliceKeywords() {
        SliceItem keywordGroup = SliceQuery.find(mSlice, FORMAT_SLICE, HINT_KEYWORDS, null);
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
        if (!mListContent.isValid()) {
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
     * A slice contains an expiry to indicate when the content in the slice might no longer be
     * valid.
     *
     * @return the time, measured in milliseconds, between the expiry time of this slice and
     * midnight, January 1, 1970 UTC, or {@link androidx.slice.builders.ListBuilder#INFINITY} if
     * the slice is not time-sensitive.
     */
    public long getExpiry() {
        return mExpiry;
    }

    /**
     * @return the time, measured in milliseconds, between when the slice was created or last
     * updated, and midnight, January 1, 1970 UTC.
     */
    public long getLastUpdatedTime() {
        return mLastUpdated;
    }

    /**
     * To present a slice from another app, the app must grant uri permissions for the slice. If
     * these permissions have not been granted and the app slice is requested then
     * a permission request slice will be returned instead, allowing the user to grant permission.
     * This method can be used to identify if a slice is a permission request.
     *
     * @return whether this slice represents a permission request.
     */
    public boolean isPermissionSlice() {
        return mSlice.hasHint(HINT_PERMISSION_REQUEST);
    }

    /**
     * Indicates whether this slice indicates an error, i.e. the normal contents of this slice are
     * unavailable and instead the slice contains a message indicating an error.
     *
     * @return whether this slice represents an error.
     */
    public boolean isErrorSlice() {
        return mSlice.hasHint(HINT_ERROR);
    }

    /**
     * Indicates whether this slice was created using {@link SliceUtils#parseSlice} or through
     * normal binding.
     */
    public boolean isCachedSlice() {
        return mSlice.hasHint(HINT_CACHED);
    }

    /**
     * @return the group of slice actions associated with the provided slice, if they exist.
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static List<SliceAction> getSliceActions(@NonNull Slice slice) {
        SliceItem actionGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_ACTIONS, null);
        String[] hints = new String[] {HINT_ACTIONS, HINT_SHORTCUT};
        List<SliceItem> items =  (actionGroup != null)
                ? SliceQuery.findAll(actionGroup, FORMAT_SLICE, hints, null)
                : null;
        if (items != null) {
            List<SliceAction> actions = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                SliceItem item = items.get(i);
                actions.add(new SliceActionImpl(item));
            }
            return actions;
        }
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return mExpiry != 0 && mExpiry != SliceHints.INFINITY && now > mExpiry;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean neverExpires() {
        return mExpiry == SliceHints.INFINITY;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public long getTimeToExpiry() {
        long now = System.currentTimeMillis();
        return (mExpiry == 0 || mExpiry == SliceHints.INFINITY || now > mExpiry)
                ? 0 : mExpiry - now;
    }
}
