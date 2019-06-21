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

package androidx.car.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.car.R;

/**
 * Class to build a {@code CarMenuItem} that appears in the {@link CarToolbar} menu.
 *
 * <p>The following properties can be specified:
 * <ul>
 *     <li>Title - Primary text that is shown on the item.
 *     <li>{@link CarMenuItem.OnClickListener} - Listener that handles the clicks on the item.
 *     <li>Icon - An icon shown before the title, if the item is not checkable (a switch).
 *     <li>Style - A Resource Id that specifies the style of the item if it's not an overflow item.
 *     <li>Enabled - A boolean that specifies whether the item is enabled or disabled.
 *     <li>Checkable - A boolean that specifies whether the item is checkable (a switch) or not.
 *     <li>Checked - A boolean that specifies whether the item is currently checked or not.
 *     <li>DisplayBehavior - A {@link DisplayBehavior} that specifies where the item is displayed.
 * </ul>
 *
 * <p>Properties such as the title, isEnabled, and isChecked can be modified
 * after creation, and as such, have setters in the class and the builder.
 *
 */
public final class CarMenuItem {
    /**
     * Interface definition for a callback to be invoked when a {@code CarMenuItem} is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a {@code CarMenuItem} has been clicked, the CarMenuItem is passed
         * in to support using one OnClickListener
         *
         * @param item The {@code CarMenuItem} that was clicked.
         */
        void onClick(CarMenuItem item);
    }

    /**
     * Display behaviors for {@code CarMenuItem}s. describes whether the items
     * will be displayed on the toolbar or in the overflow menu.
     */
    public enum DisplayBehavior {
        /**
         * The item is always displayed on the toolbar, never in the overflow menu.
         */
        ALWAYS,
        /**
         * The item is displayed on the toolbar if there's room, otherwise in the overflow menu.
         */
        IF_ROOM,
        /**
         * The item is always displayed in the overflow menu, never on the toolbar.
         */
        NEVER
    }
    @Nullable
    private CharSequence mTitle;
    private boolean mIsEnabled;
    private boolean mIsChecked;
    @StyleRes
    private final int mStyleResId;
    @Nullable
    private final OnClickListener mOnClickListener;
    @Nullable
    private final Drawable mIconDrawable;

    private final boolean mIsCheckable;
    private final DisplayBehavior mDisplayBehavior;

    CarMenuItem(Builder builder) {
        mTitle = builder.mTitle;
        mOnClickListener = builder.mOnClickListener;
        mStyleResId = builder.mStyleResId;
        mIconDrawable = builder.mIconDrawable;
        mIsEnabled = builder.mIsEnabled;
        mIsChecked = builder.mIsChecked;
        mIsCheckable = builder.mIsCheckable;
        mDisplayBehavior = builder.mDisplayBehavior;
    }

    /**
     * Sets the title of the {@code CarMenuItem}.
     *
     * @param title Title of the {@code CarMenuItem}.
     */
    public void setTitle(@Nullable CharSequence title) {
        mTitle = title;
    }

    /**
     * Sets whether the {@code CarMenuItem} is enabled or disabled.
     *
     * <p>Items are enabled by default.
     *
     * @param enabled {@code true} if the {@code CarMenuItem} is enabled.
     */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Sets whether the {@code CarMenuItem} is checked or not.
     *
     * This method will only have an effect if this {@code CarMenuItem} was built
     * with {@link Builder#setCheckable(boolean)} set to {@code true}.
     *
     * @param checked {@code true} if the {@code CarMenuItem} is checked.
     */
    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    /**
     * Returns the icon of the {@code CarMenuItem}.
     */
    @Nullable
    public Drawable getIcon() {
        return mIconDrawable;
    }

    /**
     * Returns the title of the {@code CarMenuItem}.
     */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the Res Id of the {@code CarMenuItem}'s style.
     */
    @StyleRes
    public int getStyleResId() {
        return mStyleResId;
    }

    /**
     * Returns {@code true} if the {@code CarMenuItem} is enabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Returns {@code true} if the {@code CarMenuItem} is checkable.
     */
    public boolean isCheckable() {
        return mIsCheckable;
    }

    /**
     * Returns {@code true} if the {@code CarMenuItem} is checked.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Returns The {@link DisplayBehavior} of the {@code CarMenuItem}.
     */
    @NonNull
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /**
     * Returns the {@link OnClickListener} of the {@code CarMenuItem}.
     */
    @Nullable
    OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    /**
     * Builder for creating a {@link CarMenuItem}
     */
    public static final class Builder {
        CharSequence mTitle;
        @Nullable
        OnClickListener mOnClickListener;
        @Nullable
        Drawable mIconDrawable;
        @StyleRes
        int mStyleResId = R.style.Widget_Car_ActionButton_Light;
        boolean mIsEnabled = true;
        boolean mIsChecked;
        boolean mIsCheckable;
        // If not specified, the item will be displayed only if there is room on the toolbar.
        DisplayBehavior mDisplayBehavior = DisplayBehavior.IF_ROOM;

        /**
         * Sets the title of the {@code CarMenuItem}.
         *
         * @param title Title of the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets {@link OnClickListener} of the {@code CarMenuItem}.
         *
         * @param listener OnClick listener of the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setOnClickListener(@NonNull OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Sets the style of the {@code CarMenuItem}.
         *
         * @param styleResId Res Id of the style to be used for the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setStyle(@StyleRes int styleResId) {
            mStyleResId = styleResId;
            return this;
        }

        /**
         * Sets the icon of the {@code CarMenuItem}.
         *
         * @param icon Icon of the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setIcon(@NonNull Drawable icon) {
            mIconDrawable = icon;
            return this;
        }

        /**
         * Sets the icon of the {@code CarMenuItem}.
         *
         * @param context Context to load the drawable resource with.
         * @param iconResId Resource id of icon of the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setIcon(@NonNull Context context, @DrawableRes int iconResId) {
            mIconDrawable = context.getDrawable(iconResId);
            return this;
        }

        /**
         * Sets whether the {@code CarMenuItem} is enabled or disabled.
         *
         * <p>Items are enabled by default.
         *
         * @param enabled {@code true} if the {@code CarMenuItem} is enabled.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /**
         * Sets whether the {@code CarMenuItem} is checkable or not.
         *
         * <p>Checkable items are rendered as switch widgets.
         *
         * @param checkable {@code true} if the {@code CarMenuItem} is checkable.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setCheckable(boolean checkable) {
            mIsCheckable = checkable;
            return this;
        }

        /**
         * Sets whether the {@code CarMenuItem} is checked or not.
         *
         * <p>Items are unchecked by default, this has no effect if {@link #setCheckable(boolean)}
         * is {@code false}.
         *
         * @param checked {@code true} if the {@code CarMenuItem} is checked.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setChecked(boolean checked) {
            mIsChecked = checked;
            return this;
        }

        /**
         * Sets the display behavior of the @code CarMenuItem}.
         *
         * The display behavior determines whether the item is displayed on
         * the Toolbar or in the overflow menu, see {@link DisplayBehavior}.
         *
         * @param displayBehavior Display behavior of the {@code CarMenuItem}.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setDisplayBehavior(@NonNull DisplayBehavior displayBehavior) {
            mDisplayBehavior = displayBehavior;
            return this;
        }

        /**
         * Returns a {@link CarMenuItem} built with the provided information.
         */
        @NonNull
        public CarMenuItem build() {
            return new CarMenuItem(this);
        }
    }
}
