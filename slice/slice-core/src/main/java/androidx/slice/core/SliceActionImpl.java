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

package androidx.slice.core;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_MILLIS;
import static android.app.slice.Slice.SUBTYPE_PRIORITY;
import static android.app.slice.Slice.SUBTYPE_TOGGLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.slice.core.SliceHints.ACTION_WITH_LABEL;
import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.RAW_IMAGE_LARGE;
import static androidx.slice.core.SliceHints.RAW_IMAGE_SMALL;
import static androidx.slice.core.SliceHints.SMALL_IMAGE;
import static androidx.slice.core.SliceHints.SUBTYPE_ACTION_KEY;
import static androidx.slice.core.SliceHints.SUBTYPE_DATE_PICKER;
import static androidx.slice.core.SliceHints.SUBTYPE_TIME_PICKER;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.ObjectsCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

/**
 * Class representing an action, supports tappable icons, custom toggle icons, and default toggles.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@Deprecated
public class SliceActionImpl implements SliceAction {

    // Either mAction or mActionItem must be non-null.
    @Nullable
    private PendingIntent mAction;

    // Either mAction or mActionItem must be non-null.
    @Nullable
    private SliceItem mActionItem;

    private IconCompat mIcon;
    private int mImageMode = UNKNOWN_IMAGE;
    private CharSequence mTitle;
    private CharSequence mContentDescription;

    @NonNull
    private ActionType mActionType = ActionType.DEFAULT;

    private boolean mIsChecked;
    private int mPriority = -1;
    private long mDateTimeMillis = -1;

    @Nullable
    private SliceItem mSliceItem;

    private String mActionKey;
    private boolean mIsActivity;

    enum ActionType {
        DEFAULT,
        TOGGLE,
        DATE_PICKER,
        TIME_PICKER,
    }

    /**
     * Construct a SliceAction representing a tappable icon.
     *
     * @param action      the pending intent to invoke for this action.
     * @param actionIcon  the icon to display for this action.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @NonNull CharSequence actionTitle) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
    }

    /**
     * Construct a SliceAction representing a timestamp connected to a picker.
     *
     * @param action         the pending intent to invoke for this picker.
     * @param actionTitle    the timestamp title for this picker.
     * @param dateTimeMillis the default state of the date or time picker.
     * @param isDatePicker   if it is a date picker, as opposed to a time picker.
     */
    @RestrictTo(LIBRARY)
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull CharSequence actionTitle,
            long dateTimeMillis, boolean isDatePicker) {
        mAction = action;
        mTitle = actionTitle;
        mActionType = isDatePicker ? ActionType.DATE_PICKER : ActionType.TIME_PICKER;
        mDateTimeMillis = dateTimeMillis;
    }

    /**
     * Construct a SliceAction representing a tappable icon. Use this method to specify the
     * format of the image, {@link SliceHints#ICON_IMAGE} will be presented as a tintable icon.
     * Note that there is no difference between {@link SliceHints#SMALL_IMAGE} and
     * {@link SliceHints#LARGE_IMAGE} for actions; these will just be represented as an
     * non-tintable image.
     *
     * @param action  the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param imageMode the mode this icon should be displayed in.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     *
     * @see SliceHints#ICON_IMAGE
     * @see SliceHints#SMALL_IMAGE
     * @see SliceHints#LARGE_IMAGE
     * @see SliceHints#ACTION_WITH_LABEL
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @SliceHints.ImageMode int imageMode, @NonNull CharSequence actionTitle) {
        mAction = action;
        mIcon = actionIcon;
        mTitle = actionTitle;
        mImageMode = imageMode;
    }

    /**
     * Construct a SliceAction representing a custom toggle icon.
     *
     * @param action the pending intent to invoke for this toggle.
     * @param actionIcon the icon to display for this toggle, should have a checked and unchecked
     *                    state.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
        mIsChecked = isChecked;
        mActionType = ActionType.TOGGLE;
    }

    /**
     * Construct a SliceAction representing a default toggle.
     *
     * @param action the pending intent to invoke for this toggle.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull CharSequence actionTitle,
            boolean isChecked) {
        mAction = action;
        mTitle = actionTitle;
        mActionType = ActionType.TOGGLE;
        mIsChecked = isChecked;
    }

    /**
     * Constructs a SliceAction based off of a {@link SliceItem}. Expects a specific format
     * for the item.
     *
     * @param slice the slice item to construct the action out of.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    @SuppressLint("InlinedApi")
    public SliceActionImpl(@NonNull SliceItem slice) {
        mSliceItem = slice;
        SliceItem actionItem = SliceQuery.find(slice, FORMAT_ACTION);
        if (actionItem == null) {
            // Can't have action slice without action
            return;
        }
        mActionItem = actionItem;
        mAction = actionItem.getAction();
        SliceItem iconItem = SliceQuery.find(actionItem.getSlice(), FORMAT_IMAGE);
        if (iconItem != null) {
            mIcon = iconItem.getIcon();
            mImageMode = parseImageMode(iconItem);
        }
        SliceItem titleItem = SliceQuery.find(actionItem.getSlice(), FORMAT_TEXT, HINT_TITLE,
                null /* nonHints */);
        if (titleItem != null) {
            mTitle = titleItem.getSanitizedText();
        }
        SliceItem cdItem = SliceQuery.findSubtype(actionItem.getSlice(), FORMAT_TEXT,
                SUBTYPE_CONTENT_DESCRIPTION);
        if (cdItem != null) {
            mContentDescription = cdItem.getText();
        }
        if (actionItem.getSubType() == null) {
            mActionType = ActionType.DEFAULT;
        } else {
            switch (actionItem.getSubType()) {
                case SUBTYPE_TOGGLE:
                    mActionType = ActionType.TOGGLE;
                    mIsChecked = actionItem.hasHint(HINT_SELECTED);
                    break;
                case SUBTYPE_DATE_PICKER:
                    mActionType = ActionType.DATE_PICKER;
                    SliceItem dateItem = SliceQuery.findSubtype(actionItem, FORMAT_LONG,
                            SUBTYPE_MILLIS);
                    if (dateItem != null) {
                        mDateTimeMillis = dateItem.getLong();
                    }
                    break;
                case SUBTYPE_TIME_PICKER:
                    mActionType = ActionType.TIME_PICKER;
                    SliceItem timeItem = SliceQuery.findSubtype(actionItem, FORMAT_LONG,
                            SUBTYPE_MILLIS);
                    if (timeItem != null) {
                        mDateTimeMillis = timeItem.getLong();
                    }
                    break;
                default:
                    mActionType = ActionType.DEFAULT;
            }
        }
        mIsActivity = mSliceItem.hasHint(SliceHints.HINT_ACTIVITY);
        SliceItem priority = SliceQuery.findSubtype(actionItem.getSlice(), FORMAT_INT,
                SUBTYPE_PRIORITY);
        mPriority = priority != null ? priority.getInt() : -1;
        SliceItem actionKeyItem = SliceQuery.findSubtype(actionItem.getSlice(), FORMAT_TEXT,
                SUBTYPE_ACTION_KEY);
        if (actionKeyItem != null) {
            mActionKey = actionKeyItem.getText().toString();
        }
    }

    /**
     * @param description the content description for this action.
     * @return
     */
    @Nullable
    @Override
    public @NonNull SliceAction setContentDescription(@NonNull CharSequence description) {
        mContentDescription = description;
        return this;
    }

    /**
     * @param isChecked whether the state of this action is checked or not; only used for toggle
     *                  actions.
     */
    @NonNull
    @Override
    public SliceActionImpl setChecked(boolean isChecked) {
        mIsChecked = isChecked;
        return this;
    }

    /**
     * Sets the priority of this action, with the lowest priority having the highest ranking.
     */
    @NonNull
    @Override
    public SliceActionImpl setPriority(@IntRange(from = 0) int priority) {
        mPriority = priority;
        return this;
    }

    /**
     * Sets the key for this action.
     */
    @Override
    @NonNull
    public SliceActionImpl setKey(@NonNull String key) {
        mActionKey = key;
        return this;
    }

    /**
     * @return the {@link PendingIntent} associated with this action.
     */
    @SuppressWarnings("ConstantConditions") // Either mAction or mActionItem must be non-null
    @NonNull
    @Override
    public PendingIntent getAction() {
        return mAction != null ? mAction : mActionItem.getAction();
    }

    /**
     */
    @Nullable
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public SliceItem getActionItem() {
        return mActionItem;
    }

    /**
     * @return the {@link Icon} to display for this action. This can be null when the action
     * represented is a default toggle.
     */
    @Nullable
    @Override
    public IconCompat getIcon() {
        return mIcon;
    }

    /**
     * @return the title for this action.
     */
    @NonNull
    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * @return the content description to use for this action.
     */
    @Nullable
    @Override
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    /**
     * @return the priority associated with this action, -1 if unset.
     */
    @Override
    public int getPriority() {
        return mPriority;
    }

    /**
     * @return the key associated with this action.
     */
    @Nullable
    @Override
    public String getKey() {
        return mActionKey;
    }

    /**
     * @return whether this action represents a toggle (i.e. has a checked and unchecked state).
     */
    @Override
    public boolean isToggle() {
        return mActionType == ActionType.TOGGLE;
    }

    /**
     * @return whether the state of this action is checked or not; only used for toggle actions.
     */
    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * @return the image mode to use for this action.
     */
    @Override
    public @SliceHints.ImageMode int getImageMode() {
        return mImageMode;
    }

    /**
     * @return whether this action is a toggle using the standard switch control.
     */
    @Override
    public boolean isDefaultToggle() {
        return mActionType == ActionType.TOGGLE && mIcon == null;
    }

    /**
     * @return the SliceItem used to construct this action, this is only populated if the action was
     * constructed with {@link #SliceActionImpl(SliceItem)}.
     */
    @Nullable
    public SliceItem getSliceItem() {
        return mSliceItem;
    }

    @Override
    public boolean isActivity() {
        return mIsActivity;
    }

    /**
     * @param builder this should be a new builder that has any additional hints the action might
     *                need.
     * @return the slice representation of this action.
     */
    @NonNull
    public Slice buildSlice(@NonNull Slice.Builder builder) {
        ObjectsCompat.requireNonNull(mAction, "Action must be non-null");

        return builder.addHints(HINT_SHORTCUT)
                .addAction(mAction, buildSliceContent(builder).build(), getSubtype())
                .build();
    }

    /**
     * @return the primary action slice content associated with this primary action.
     */
    @NonNull
    public Slice buildPrimaryActionSlice(@NonNull Slice.Builder builder) {
        // Primary action row is annotated with shortcut and title hints.
        return buildSliceContent(builder).addHints(HINT_SHORTCUT, HINT_TITLE).build();
    }

    /**
     */
    @NonNull
    private Slice.Builder buildSliceContent(@NonNull Slice.Builder builder) {
        Slice.Builder sb = new Slice.Builder(builder);
        if (mIcon != null) {
            @Slice.SliceHint String[] hints;
            if (mImageMode == ACTION_WITH_LABEL) {
                hints = new String[]{SliceHints.HINT_SHOW_LABEL};
            } else {
                hints = mImageMode == ICON_IMAGE
                        ? new String[]{}
                        : new String[]{HINT_NO_TINT};
            }
            sb.addIcon(mIcon, null, hints);
        }
        if (mTitle != null) {
            sb.addText(mTitle, null, HINT_TITLE);
        }
        if (mContentDescription != null) {
            sb.addText(mContentDescription, SUBTYPE_CONTENT_DESCRIPTION);
        }
        if (mDateTimeMillis != -1) {
            sb.addLong(mDateTimeMillis, SUBTYPE_MILLIS);
        }
        if (mActionType == ActionType.TOGGLE && mIsChecked) {
            sb.addHints(HINT_SELECTED);
        }
        if (mPriority != -1) {
            sb.addInt(mPriority, SUBTYPE_PRIORITY);
        }
        if (mActionKey != null) {
            sb.addText(mActionKey, SUBTYPE_ACTION_KEY);
        }
        if (mIsActivity) {
            builder.addHints(SliceHints.HINT_ACTIVITY);
        }
        return sb;
    }

    /**
     * @return the subtype of this slice action.
     */
    @Nullable
    public String getSubtype() {
        switch(mActionType){
            case TOGGLE:
                return SUBTYPE_TOGGLE;
            case DATE_PICKER:
                return SUBTYPE_DATE_PICKER;
            case TIME_PICKER:
                return SUBTYPE_TIME_PICKER;
            default:
                return null;
        }
    }

    public void setActivity(boolean isActivity) {
        mIsActivity = isActivity;
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static int parseImageMode(@NonNull SliceItem iconItem) {
        if (iconItem.hasHint(SliceHints.HINT_SHOW_LABEL)) {
            return ACTION_WITH_LABEL;
        }
        if (!iconItem.hasHint(HINT_NO_TINT)) {
            return ICON_IMAGE;
        }
        if (iconItem.hasHint(SliceHints.HINT_RAW)) {
            return iconItem.hasHint(HINT_LARGE) ? RAW_IMAGE_LARGE : RAW_IMAGE_SMALL;
        }
        if (iconItem.hasHint(HINT_LARGE)) {
            return LARGE_IMAGE;
        }
        return SMALL_IMAGE;
    }
}
