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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.remotecallback.RemoteCallback;
import androidx.slice.Slice;
import androidx.slice.core.SliceActionImpl;

/**
 * Class representing an action, supports tappable icons, custom toggle icons, and default
 * toggles, as well as date and time pickers.
 */
@RequiresApi(19)
public class SliceAction implements androidx.slice.core.SliceAction {

    private final SliceActionImpl mSliceAction;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @RequiresApi(23)
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
            @NonNull CharSequence actionTitle) {
        this(action, actionIcon, ICON_IMAGE, actionTitle);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @RequiresApi(23)
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
            @ListBuilder.ImageMode int imageMode, @NonNull CharSequence actionTitle) {
        this(action, IconCompat.createFromIcon(actionIcon), imageMode, actionTitle);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @RequiresApi(23)
    public SliceAction(@NonNull PendingIntent action, @NonNull Icon actionIcon,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        this(action, IconCompat.createFromIcon(actionIcon), actionTitle, isChecked);
    }

    /**
     * Construct a SliceAction representing a tappable icon.
     *
     * @param action the pending intent to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public SliceAction(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public SliceAction(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @ListBuilder.ImageMode int imageMode, @NonNull CharSequence actionTitle) {
        mSliceAction = new SliceActionImpl(action, actionIcon, imageMode, actionTitle);
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public SliceAction(@NonNull PendingIntent action, @NonNull IconCompat actionIcon,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        mSliceAction = new SliceActionImpl(action, actionIcon, actionTitle, isChecked);
    }

    /**
     * Construct a SliceAction representing a default toggle.
     *
     * @param action the pending intent to invoke for this toggle.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public SliceAction(@NonNull PendingIntent action, @NonNull CharSequence actionTitle,
            boolean isChecked) {
        mSliceAction = new SliceActionImpl(action, actionTitle, isChecked);
    }

    /**
     * Construct a SliceAction representing a default date or time picker.
     *
     * @param action         the pending intent to invoke for this date picker.
     * @param actionTitle    the timestamp for this date or time picker.
     * @param dateTimeMillis the default state of the date or time picker.
     * @param isDatePicker   if it is a date picker, as opposed to a time picker.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public SliceAction(@NonNull PendingIntent action, @NonNull CharSequence actionTitle,
            long dateTimeMillis, boolean isDatePicker) {
        mSliceAction = new SliceActionImpl(action, actionTitle, dateTimeMillis, isDatePicker);
    }

    /**
     * Construct a SliceAction representing a tappable icon.
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
    public static SliceAction create(@NonNull PendingIntent action,
            @NonNull IconCompat actionIcon, @ListBuilder.ImageMode int imageMode,
            @NonNull CharSequence actionTitle) {
        return new SliceAction(action, actionIcon, imageMode, actionTitle);
    }

    /**
     * Construct a SliceAction representing a tappable icon.
     *
     * @param action the remote callback to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param imageMode the mode this icon should be displayed in.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     *
     * @see ListBuilder#ICON_IMAGE
     * @see ListBuilder#SMALL_IMAGE
     * @see ListBuilder#LARGE_IMAGE
     */
    public static SliceAction create(@NonNull RemoteCallback action,
            @NonNull IconCompat actionIcon, @ListBuilder.ImageMode int imageMode,
            @NonNull CharSequence actionTitle) {
        return new SliceAction(action.toPendingIntent(), actionIcon, imageMode, actionTitle);
    }

    /**
     * Construct a SliceAction representing a timestamp connected to a date picker.
     * Currently only supported in GridRow.
     *
     * @param action         the pending intent to invoke for this picker.
     * @param actionTitle    the timestamp title for this picker.
     * @param dateTimeMillis the default state of the date picker.
     * @hide
     */
    @NonNull
    public static SliceAction createDatePicker(@NonNull PendingIntent action,
            @NonNull CharSequence actionTitle, long dateTimeMillis) {
        return new SliceAction(action, actionTitle, dateTimeMillis, true);
    }

    /**
     * Construct a SliceAction representing a timestamp connected to a time picker.
     * Currently only supported in GridRow.
     *
     * @param action         the pending intent to invoke for this picker.
     * @param actionTitle    the timestamp title for this picker.
     * @param dateTimeMillis the default state of the time picker.
     * @hide
     */
    @NonNull
    public static SliceAction createTimePicker(@NonNull PendingIntent action,
            @NonNull CharSequence actionTitle, long dateTimeMillis) {
        return new SliceAction(action, actionTitle, dateTimeMillis, false);
    }

    /**
     * Construct a SliceAction representing a tappable icon that launches an
     * activity when clicked.
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
    public static SliceAction createDeeplink(@NonNull PendingIntent action,
            @NonNull IconCompat actionIcon, @ListBuilder.ImageMode int imageMode,
            @NonNull CharSequence actionTitle) {
        SliceAction sliceAction = new SliceAction(action, actionIcon, imageMode, actionTitle);
        sliceAction.mSliceAction.setActivity(true);
        return sliceAction;
    }

    /**
     * Construct a SliceAction representing a tappable icon that launches an
     * activity when clicked.
     *
     * @param action the remote callback to invoke for this action.
     * @param actionIcon the icon to display for this action.
     * @param imageMode the mode this icon should be displayed in.
     * @param actionTitle the title for this action, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     *
     * @see ListBuilder#ICON_IMAGE
     * @see ListBuilder#SMALL_IMAGE
     * @see ListBuilder#LARGE_IMAGE
     */
    public static SliceAction createDeeplink(@NonNull RemoteCallback action,
            @NonNull IconCompat actionIcon, @ListBuilder.ImageMode int imageMode,
            @NonNull CharSequence actionTitle) {
        SliceAction sliceAction = new SliceAction(action.toPendingIntent(), actionIcon, imageMode,
                actionTitle);
        sliceAction.mSliceAction.setActivity(true);
        return sliceAction;
    }

    /**
     * Construct a SliceAction representing a default toggle.
     *
     * @param action the pending intent to invoke for this toggle.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public static SliceAction createToggle(@NonNull PendingIntent action,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        return new SliceAction(action, actionTitle, isChecked);
    }

    /**
     * Construct a SliceAction representing a default toggle.
     *
     * @param action the remote callback to invoke for this toggle.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public static SliceAction createToggle(@NonNull RemoteCallback action,
            @NonNull CharSequence actionTitle, boolean isChecked) {
        return new SliceAction(action.toPendingIntent(), actionTitle, isChecked);
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
    public static SliceAction createToggle(@NonNull PendingIntent action,
            @NonNull IconCompat actionIcon, @NonNull CharSequence actionTitle, boolean isChecked) {
        return new SliceAction(action, actionIcon, actionTitle, isChecked);
    }

    /**
     * Construct a SliceAction representing a custom toggle icon.
     *
     * @param action the remote callback to invoke for this toggle.
     * @param actionIcon the icon to display for this toggle, should have a checked and unchecked
     *                   state.
     * @param actionTitle the title for this toggle, also used for content description if one hasn't
     *                    been set via {@link #setContentDescription(CharSequence)}.
     * @param isChecked the state of the toggle.
     */
    public static SliceAction createToggle(@NonNull RemoteCallback action,
            @NonNull IconCompat actionIcon, @NonNull CharSequence actionTitle, boolean isChecked) {
        return new SliceAction(action.toPendingIntent(), actionIcon, actionTitle, isChecked);
    }

    /**
     * @param description the content description for this action.
     */
    @NonNull
    @Override
    public SliceAction setContentDescription(@NonNull CharSequence description) {
        mSliceAction.setContentDescription(description);
        return this;
    }

    /**
     * @param isChecked whether the state of this action is checked or not; only used for toggle
     *                  actions.
     */
    @NonNull
    @Override
    public SliceAction setChecked(boolean isChecked) {
        mSliceAction.setChecked(isChecked);
        return this;
    }

    /**
     * Sets the priority of this action, with the lowest priority having the highest ranking.
     */
    @NonNull
    @Override
    public SliceAction setPriority(@IntRange(from = 0) int priority) {
        mSliceAction.setPriority(priority);
        return this;
    }

    /**
     * Sets the key of this action to provide extra information to the host renderer.
     */
    @NonNull
    @Override
    public SliceAction setKey(@NonNull String key) {
        mSliceAction.setKey(key);
        return this;
    }

    /**
     * @return the {@link PendingIntent} associated with this action.
     */
    @NonNull
    @Override
    public PendingIntent getAction() {
        return mSliceAction.getAction();
    }

    /**
     * @return the {@link Icon} to display for this action. This can be null when the action
     * represented is a default toggle.
     */
    @Nullable
    @Override
    public IconCompat getIcon() {
        return mSliceAction.getIcon();
    }

    /**
     * @return the title for this action.
     */
    @NonNull
    @Override
    public CharSequence getTitle() {
        return mSliceAction.getTitle();
    }

    @Override
    public boolean isActivity() {
        return mSliceAction.isActivity();
    }

    /**
     * @return the content description to use for this action.
     */
    @Nullable
    @Override
    public CharSequence getContentDescription() {
        return mSliceAction.getContentDescription();
    }

    /**
     * @return the priority associated with this action, -1 if unset.
     */
    @Override
    public int getPriority() {
        return mSliceAction.getPriority();
    }

    /**
     * @return the key associated with this action.
     */
    @Nullable
    @Override
    public String getKey() {
        return mSliceAction.getKey();
    }

    /**
     * @return whether this action represents a toggle (i.e. has a checked and unchecked state).
     */
    @Override
    public boolean isToggle() {
        return mSliceAction.isToggle();
    }

    /**
     * @return whether the state of this action is checked or not; only used for toggle actions.
     */
    @Override
    public boolean isChecked() {
        return mSliceAction.isChecked();
    }

    /**
     * @return the image mode to use for this action.
     */
    @Override
    public @ListBuilder.ImageMode int getImageMode() {
        return mSliceAction.getImageMode();
    }

    /**
     * @return whether this action is a toggle using the standard switch control.
     */
    @Override
    public boolean isDefaultToggle() {
        return mSliceAction.isDefaultToggle();
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
        return mSliceAction.buildSlice(builder);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public SliceActionImpl getImpl() {
        return mSliceAction;
    }

    /**
     * @param builder the parent slice builder that contains the primary action.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void setPrimaryAction(@NonNull Slice.Builder builder) {
        builder.addAction(mSliceAction.getAction(),
                mSliceAction.buildPrimaryActionSlice(builder), mSliceAction.getSubtype());
    }
}
