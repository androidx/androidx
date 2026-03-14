/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.car.app.model.constraints.CarTextConstraints.TEXT_WITH_COLORS;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A Filter Chip is a toggle-like button with text and/or an icon used to filter results
 * within a template.
 *
 * <p>Filter chips are used to filter content. They can contain text, a start icon, and an end icon.
 * They can also be selected or unselected.
 */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(9)
@KeepFields
public final class FilterChip implements Item {
    @Nullable
    private final CarText mTitle;
    @Nullable
    private final CarIcon mStartIcon;
    @Nullable
    private final CarIcon mEndIcon;
    private final boolean mIsSelected;
    @Nullable
    private final OnClickDelegate mOnClickDelegate;
    @Nullable
    private final FilterChipStyle mStyle;

    /**
     * Returns the title of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the start icon of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarIcon getStartIcon() {
        return mStartIcon;
    }

    /**
     * Returns the end icon of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarIcon getEndIcon() {
        return mEndIcon;
    }

    /**
     * Returns whether the chip is selected.
     */
    public boolean isSelected() {
        return mIsSelected;
    }

    /**
     * Returns the {@link OnClickDelegate} to be called when the chip is clicked.
     */
    @Nullable
    public OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns the style of the chip, or {@code null} if not set.
     */
    @Nullable
    public FilterChipStyle getStyle() {
        return mStyle;
    }

    @Override
    @NonNull
    public String toString() {
        return "FilterChip{"
                + "title="
                + mTitle
                + ", startIcon="
                + mStartIcon
                + ", endIcon="
                + mEndIcon
                + ", isSelected="
                + mIsSelected
                + ", onClickDelegate="
                + mOnClickDelegate
                + ", style="
                + mStyle
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mStartIcon, mEndIcon, mIsSelected,
                mOnClickDelegate == null, mStyle);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FilterChip)) {
            return false;
        }
        FilterChip otherChip = (FilterChip) other;
        return Objects.equals(mTitle, otherChip.mTitle)
                && Objects.equals(mStartIcon, otherChip.mStartIcon)
                && Objects.equals(mEndIcon, otherChip.mEndIcon)
                && mIsSelected == otherChip.mIsSelected
                && Objects.equals(mOnClickDelegate == null, otherChip.mOnClickDelegate == null)
                && Objects.equals(mStyle, otherChip.mStyle);
    }

    FilterChip(Builder builder) {
        mTitle = builder.mTitle;
        mStartIcon = builder.mStartIcon;
        mEndIcon = builder.mEndIcon;
        mIsSelected = builder.mIsSelected;
        mOnClickDelegate = builder.mOnClickDelegate;
        mStyle = builder.mStyle;
    }

    /** Constructs an empty instance, used by serialization code. */
    private FilterChip() {
        mTitle = null;
        mStartIcon = null;
        mEndIcon = null;
        mIsSelected = false;
        mOnClickDelegate = null;
        mStyle = null;
    }

    /** A builder of {@link FilterChip}. */
    public static final class Builder {
        @Nullable
        CarText mTitle;
        @Nullable
        CarIcon mStartIcon;
        @Nullable
        CarIcon mEndIcon;
        boolean mIsSelected;
        @Nullable
        OnClickDelegate mOnClickDelegate;
        @Nullable
        FilterChipStyle mStyle;



        /**
         * Sets the title of the chip.
         *
         * <p>A chip without a title <strong>must</strong> set {@link #setStartIcon} OR
         * {@link #setEndIcon} or the {@link #build()} will throw an exception. Without a
         * title, the host will render the chip with the passed in startIcon and/or endIcon
         * (depending on which has been set).
         *
         * <p>The text must conform to
         * {@link androidx.car.app.model.constraints.CarTextConstraints#TEXT_WITH_COLORS}.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} is empty, or if it contains
         *                                  unsupported spans
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            return setTitle(CarText.create(requireNonNull(title)));
        }

        /**
         * Sets the title of the chip, with support for multiple length variants.
         *
         * <p>A chip without a title <strong>must</strong> set {@link #setStartIcon} OR
         * {@link #setEndIcon} or the {@link #build()} will throw an exception. Without a
         * title, the host will render the chip with the passed in startIcon and/or endIcon
         * (depending on which has been set).
         *
         * <p>The text must conform to
         * {@link androidx.car.app.model.constraints.CarTextConstraints#TEXT_WITH_COLORS}.
         *
         * <p>Developers can provide a shorter {@link CarText#addVariant(CharSequence)}
         * variant in case their title length exceeds the chip width limit enforced by the host.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} is empty, or if it contains
         *                                  unsupported spans
         */
        @NonNull
        public Builder setTitle(@NonNull CarText title) {
            CarText titleText = requireNonNull(title);
            if (titleText.isEmpty()) {
                throw new IllegalArgumentException("The title cannot be null or empty");
            }
            TEXT_WITH_COLORS.validateOrThrow(titleText);
            mTitle = titleText;
            return this;
        }

        /**
         * Sets the icon to display at the start of the chip.
         *
         * <p>The icon must conform to
         * {@link androidx.car.app.model.constraints.CarIconConstraints#DEFAULT}.
         *
         * <p>A chip without a startIcon <strong>must</strong> set {@link #setTitle} OR
         * {@link #setEndIcon} or the {@link #build()} will throw an exception. Without a
         * startIcon, the host will render the chip with the passed in title and/or endIcon
         * (depending on which has been set).
         *
         * <p>The icon will attempt to use the most specific tint supplied; this is, it will try to
         * use the first tint found in the following priority:
         * 1. {@link CarIcon.Builder#setTint}
         * 2. {@link FilterChip.Builder#setStyle(FilterChipStyle)}
         * {@link FilterChipStyle.Builder#setContentColor(CarColor)}
         * 3. {@link FilterChipSection.Builder#setStyle(FilterChipStyle)} content color
         * 4. A host supplied color that matches the {@link #setSelected} state
         * Note that if the supplied color fails to meet contrast requirements,
         * a host-supplied fallback that matches the selected state will be used.
         *
         * <p>Rasterized images such as
         * ({@link androidx.core.graphics.drawable.IconCompat#TYPE_BITMAP}) images
         * will not have any tints <strong>unless explicitly supplied through
         * {@link CarIcon.Builder#setTint}.</strong>.
         *
         * @throws NullPointerException if {@code startIcon} is {@code null}
         */
        @NonNull
        public Builder setStartIcon(@NonNull CarIcon startIcon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(startIcon));
            mStartIcon = startIcon;
            return this;
        }

        /**
         * Sets the icon to display at the end of the chip.
         *
         * <p>The icon must conform to
         * {@link androidx.car.app.model.constraints.CarIconConstraints#DEFAULT}.
         *
         * <p>A chip without an endIcon <strong>must</strong> set {@link #setTitle} OR
         * {@link #setStartIcon} or the {@link #build()} will throw an exception. Without an
         * endIcon, the host will render the chip with the passed in title and/or startIcon
         * (depending on which has been set).
         *
         * <p>The icon will attempt to use the most specific tint supplied; this is, it will try to
         * use the first tint found in the following priority:
         * 1. {@link CarIcon.Builder#setTint}
         * 2. {@link FilterChip.Builder#setStyle(FilterChipStyle)}
         * {@link FilterChipStyle.Builder#setContentColor(CarColor)}
         * 3. {@link FilterChipSection.Builder#setStyle(FilterChipStyle)} content color
         * 4. A host supplied color that matches the {@link #setSelected} state
         * Note that if the supplied color fails to meet contrast requirements,
         * a host-supplied fallback that matches the selected state will be used.
         *
         * <p>Rasterized images such as
         * ({@link androidx.core.graphics.drawable.IconCompat#TYPE_BITMAP}) images
         * will not have any tints <strong>unless explicitly supplied through
         * {@link CarIcon.Builder#setTint}.</strong>.
         *
         * @throws NullPointerException if {@code endIcon} is {@code null}
         */
        @NonNull
        public Builder setEndIcon(@NonNull CarIcon endIcon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(endIcon));
            mEndIcon = endIcon;
            return this;
        }

        /**
         * Sets the selected state of the chip, similar to how toggle has a selected state.
         *
         * <p>If the selected state is not set, the default will be false
         *
         * <p>If a custom style is provided through {@link #setStyle(FilterChipStyle)}, changing
         * this selected boolean value will not automatically change the
         * color of this chip. The custom style must also be updated to reflect the selected state.
         * See {@link FilterChip.Builder#setStyle} or {@link FilterChipSection.Builder#setStyle}
         * for more details.
         */
        @NonNull
        public Builder setSelected(boolean isSelected) {
            mIsSelected = isSelected;
            return this;
        }

        /**
         * Sets the {@link OnClickListener} to be called when the chip is clicked.
         *
         * <p>Note that the listener relates to the UI events and will be executed on the main
         * thread using {@link android.os.Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code onClickListener} is {@code null}
         */
        @NonNull
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public Builder setOnClickListener(@NonNull OnClickListener onClickListener) {
            mOnClickDelegate = OnClickDelegateImpl.create(requireNonNull(onClickListener));
            return this;
        }

        /**
         * Sets the style of the chip.
         *
         * <p>This style overrides the style set in the {@link FilterChipSection} if one is
         * provided. Any fields not explicitly set here or in the {@link FilterChipSection}
         * will fall back to the host's default colors. If the style is not set or if the colors
         * do not meet the contrast requirements, the host will set the chip styling to defaults
         * based on the {@link #setSelected(boolean)} state.
         *
         * @throws NullPointerException if {@code style} is {@code null}
         */
        @NonNull
        public Builder setStyle(@NonNull FilterChipStyle style) {
            mStyle = requireNonNull(style);
            return this;
        }

        /**
         * Constructs the {@link FilterChip} defined by this builder.
         *
         * @throws IllegalStateException if the chip does not have a title, start icon, or end icon.
         * @throws IllegalStateException if the chip does not have an {@link OnClickListener}.
         */
        @NonNull
        public FilterChip build() {
            if (mTitle == null && mStartIcon == null && mEndIcon == null) {
                throw new IllegalStateException(
                        "A title, start icon, or end icon must be set for the chip");
            }

            if (mOnClickDelegate == null) {
                throw new IllegalStateException("An OnClickListener must be set");
            }

            return new FilterChip(this);
        }
    }
}
