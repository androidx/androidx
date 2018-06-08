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
import static android.app.slice.Slice.SUBTYPE_PRIORITY;
import static android.app.slice.Slice.SUBTYPE_TOGGLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.SMALL_IMAGE;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

/**
 * Class representing an action, supports tappable icons, custom toggle icons, and default toggles.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SliceActionImpl implements SliceAction {

    private PendingIntent mAction;
    private IconCompat mIcon;
    private int mImageMode = UNKNOWN_IMAGE;
    private CharSequence mTitle;
    private CharSequence mContentDescription;
    private boolean mIsToggle;
    private boolean mIsChecked;
    private int mPriority = -1;
    private SliceItem mSliceItem;
    private SliceItem mActionItem;

    /**
     * Construct a SliceAction representing a tappable icon.
     *
     * @param action the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @NonNull CharSequence actionTitle) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
    }

    /**
     * Construct a SliceAction representing a tappable icon. Use this method to specify the
     * format of the image, {@link SliceHints#ICON_IMAGE} will be presented as a tintable icon.
     * Note that there is no difference between {@link SliceHints#SMALL_IMAGE} and
     * {@link SliceHints#LARGE_IMAGE} for actions; these will just be represented as an
     * non-tintable image.
     *
     * @param action the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param imageMode the mode this icon should be displayed in.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     *
     * @see SliceHints#ICON_IMAGE
     * @see SliceHints#SMALL_IMAGE
     * @see SliceHints#LARGE_IMAGE
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
     *                   state.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public SliceActionImpl(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
        mIsChecked = isChecked;
        mIsToggle = true;
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
        mIsToggle = true;
        mIsChecked = isChecked;
    }

    /**
     * Constructs a SliceAction based off of a {@link SliceItem}. Expects a specific format
     * for the item.
     *
     * @param slice the slice item to construct the action out of.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public SliceActionImpl(SliceItem slice) {
        mSliceItem = slice;
        SliceItem actionItem = SliceQuery.find(slice, FORMAT_ACTION);
        if (actionItem == null) {
            // Can't have action slice without action
            return;
        }
        mActionItem = actionItem;
        SliceItem iconItem = SliceQuery.find(actionItem.getSlice(), FORMAT_IMAGE);
        if (iconItem != null) {
            mIcon = iconItem.getIcon();
            mImageMode = iconItem.hasHint(HINT_NO_TINT)
                    ? iconItem.hasHint(HINT_LARGE) ? LARGE_IMAGE : SMALL_IMAGE
                    : ICON_IMAGE;
        }
        SliceItem titleItem = SliceQuery.find(actionItem.getSlice(), FORMAT_TEXT, HINT_TITLE,
                null /* nonHints */);
        if (titleItem != null) {
            mTitle = titleItem.getText();
        }
        SliceItem cdItem = SliceQuery.findSubtype(actionItem.getSlice(), FORMAT_TEXT,
                SUBTYPE_CONTENT_DESCRIPTION);
        if (cdItem != null) {
            mContentDescription = cdItem.getText();
        }
        mIsToggle = SUBTYPE_TOGGLE.equals(actionItem.getSubType());
        if (mIsToggle) {
            mIsChecked = actionItem.hasHint(HINT_SELECTED);
        }
        SliceItem priority = SliceQuery.findSubtype(actionItem.getSlice(), FORMAT_INT,
                SUBTYPE_PRIORITY);
        mPriority = priority != null ? priority.getInt() : -1;
    }

    /**
     * @param description the content description for this action.
     */
    @Nullable
    @Override
    public SliceActionImpl setContentDescription(@NonNull CharSequence description) {
        mContentDescription = description;
        return this;
    }

    /**
     * @param isChecked whether the state of this action is checked or not; only used for toggle
     *                  actions.
     */
    @Override
    public SliceActionImpl setChecked(boolean isChecked) {
        mIsChecked = isChecked;
        return this;
    }

    /**
     * Sets the priority of this action, with the lowest priority having the highest ranking.
     */
    @Override
    public SliceActionImpl setPriority(@IntRange(from = 0) int priority) {
        mPriority = priority;
        return this;
    }

    /**
     * @return the {@link PendingIntent} associated with this action.
     */
    @NonNull
    @Override
    public PendingIntent getAction() {
        return mAction != null ? mAction : mActionItem.getAction();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
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
     * @return whether this action represents a toggle (i.e. has a checked and unchecked state).
     */
    @Override
    public boolean isToggle() {
        return mIsToggle;
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
        return mIsToggle && mIcon == null;
    }

    /**
     * @return the SliceItem used to construct this action, this is only populated if the action was
     * constructed with {@link #SliceActionImpl(SliceItem)}.
     */
    @Nullable
    public SliceItem getSliceItem() {
        return mSliceItem;
    }

    /**
     * @param builder this should be a new builder that has any additional hints the action might
     *                need.
     * @return the slice representation of this action.
     */
    @NonNull
    public Slice buildSlice(@NonNull Slice.Builder builder) {
        Slice.Builder sb = new Slice.Builder(builder);
        if (mIcon != null) {
            @Slice.SliceHint String[] hints = mImageMode == ICON_IMAGE
                    ? new String[] {}
                    : new String[] {HINT_NO_TINT};
            sb.addIcon(mIcon, null, hints);
        }
        if (mTitle != null) {
            sb.addText(mTitle, null, HINT_TITLE);
        }
        if (mContentDescription != null) {
            sb.addText(mContentDescription, SUBTYPE_CONTENT_DESCRIPTION);
        }
        if (mIsToggle && mIsChecked) {
            sb.addHints(HINT_SELECTED);
        }
        if (mPriority != -1) {
            sb.addInt(mPriority, SUBTYPE_PRIORITY);
        }
        String subtype = mIsToggle ? SUBTYPE_TOGGLE : null;
        builder.addHints(HINT_SHORTCUT);
        builder.addAction(mAction, sb.build(), subtype);
        return builder.build();
    }
}
