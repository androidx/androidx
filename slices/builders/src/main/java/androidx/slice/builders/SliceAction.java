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

package androidx.slice.builders;

import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_PRIORITY;
import static android.app.slice.Slice.SUBTYPE_TOGGLE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;

/**
 * Class representing an action, supports tappable icons, custom toggle icons, and default toggles.
 */
public class SliceAction {

    private PendingIntent mAction;
    private Icon mIcon;
    private int mImageMode;
    private CharSequence mTitle;
    private CharSequence mContentDescription;
    private boolean mIsToggle;
    private boolean mIsChecked;
    private int mPriority = -1;

    /**
     * Construct a SliceAction representing a tappable icon.
     *
     * @param action the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     */
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
            @NonNull CharSequence actionTitle) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
    }

    /**
     * Construct a SliceAction representing a tappable icon. Use this method to specify the
     * format of the image, {@link ListBuilder#ICON_IMAGE} will be presented as a tintable icon.
     * Note that there is no difference between {@link ListBuilder#SMALL_IMAGE} and
     * {@link ListBuilder#LARGE_IMAGE} for actions; these will just be represented as an
     * non-tintable image.
     *
     * @param action the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param imageMode the mode this icon should be displayed in.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     *
     * @see ListBuilder#ICON_IMAGE
     * @see ListBuilder#SMALL_IMAGE
     * @see ListBuilder#LARGE_IMAGE
     */
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
            @ListBuilder.ImageMode int imageMode, @NonNull CharSequence actionTitle) {
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
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
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
    public SliceAction(@NonNull PendingIntent action, @NonNull CharSequence actionTitle,
            boolean isChecked) {
        mAction = action;
        mTitle = actionTitle;
        mIsToggle = true;
        mIsChecked = isChecked;

    }

    /**
     * @param description the content description for this action.
     */
    @Nullable
    public SliceAction setContentDescription(@NonNull CharSequence description) {
        mContentDescription = description;
        return this;
    }

    /**
     * @param isChecked whether the state of this action is checked or not; only used for toggle
     *                  actions.
     */
    public SliceAction setChecked(boolean isChecked) {
        mIsChecked = isChecked;
        return this;
    }

    /**
     * Sets the priority of this action, with the lowest priority having the highest ranking.
     */
    public SliceAction setPriority(@IntRange(from = 0) int priority) {
        mPriority = priority;
        return this;
    }

    /**
     * @return the {@link PendingIntent} associated with this action.
     */
    @NonNull
    public PendingIntent getAction() {
        return mAction;
    }

    /**
     * @return the {@link Icon} to display for this action. This can be null when the action
     * represented is a default toggle.
     */
    @Nullable
    public Icon getIcon() {
        return mIcon;
    }

    /**
     * @return the title for this action.
     */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * @return the content description to use for this action.
     */
    @Nullable
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    /**
     * @return the priority associated with this action, -1 if unset.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * @return whether this action represents a toggle (i.e. has a checked and unchecked state).
     */
    public boolean isToggle() {
        return mIsToggle;
    }

    /**
     * @return whether the state of this action is checked or not; only used for toggle actions.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public boolean isDefaultToggle() {
        return mIsToggle && mIcon == null;
    }

    /**
     * @param builder this should be a new builder that has any additional hints the action might
     *                need.
     * @return the slice representation of this action.
     * @hide
     */
    @RestrictTo(LIBRARY)
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
