/*
 * Copyright 2026 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents a header for a section within a {@link SectionedItemTemplate}.
 *
 * <p>A section header must include a title. It can also include a start icon, an end icon, and
 * can handle click events.
 */
@CarProtocol
@ExperimentalCarApi
@RequiresCarApi(9)
@KeepFields
public final class SectionHeader {
    /**
     * The type of image to display in the section header.
     */
    @RestrictTo(LIBRARY)
    @IntDef(value = {IMAGE_TYPE_SMALL, IMAGE_TYPE_LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SectionHeaderImageType {
    }

    /**
     * Represents a small icon to be displayed in the section header.
     */
    public static final int IMAGE_TYPE_SMALL = 1;

    /**
     * Represents a large sized image to be displayed in the section header.
     */
    public static final int IMAGE_TYPE_LARGE = 2;

    private final @NonNull CarText mTitle;
    private final @Nullable CarIcon mStartIcon;
    @SectionHeaderImageType
    private final int mStartIconType;
    private final @Nullable CarIcon mEndIcon;
    private final @Nullable OnClickDelegate mOnClickDelegate;

    /**
     * Returns the title of the header.
     */
    public @NonNull CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the start icon of the header, or {@code null} if none is set.
     *
     * @see Builder#setStartIcon(CarIcon, int)
     */
    public @Nullable CarIcon getStartIcon() {
        return mStartIcon;
    }

    /**
     * Returns the type of the start icon.
     *
     * @see Builder#setStartIcon(CarIcon, int)
     */
    @SectionHeaderImageType
    public int getStartIconType() {
        return mStartIconType;
    }

    /**
     * Returns the end icon of the header, or {@code null} if none is set.
     *
     * @see Builder#setEndIcon(CarIcon)
     */
    public @Nullable CarIcon getEndIcon() {
        return mEndIcon;
    }

    /**
     * Returns the {@link OnClickDelegate} to handle click events on the header, or {@code null}
     * if the header is not clickable.
     */
    public @Nullable OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    @Override
    public @NonNull String toString() {
        return "SectionHeader { title: " + mTitle + ", startIcon: " + mStartIcon
                + ", startIconType: " + mStartIconType + ", endIcon: " + mEndIcon
                + ", hasClickListener: " + (mOnClickDelegate != null) + " }";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mStartIcon, mStartIconType, mEndIcon,
                mOnClickDelegate == null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SectionHeader)) {
            return false;
        }
        SectionHeader otherHeader = (SectionHeader) other;
        return Objects.equals(mTitle, otherHeader.mTitle)
                && Objects.equals(mStartIcon, otherHeader.mStartIcon)
                && mStartIconType == otherHeader.mStartIconType
                && Objects.equals(mEndIcon, otherHeader.mEndIcon)
                && (mOnClickDelegate == null) == (otherHeader.mOnClickDelegate == null);
    }

    SectionHeader(Builder builder) {
        mTitle = requireNonNull(builder.mTitle);
        mStartIcon = builder.mStartIcon;
        mStartIconType = builder.mStartIconType;
        mEndIcon = builder.mEndIcon;
        mOnClickDelegate = builder.mOnClickDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private SectionHeader() {
        mTitle = CarText.create("");
        mStartIcon = null;
        mStartIconType = IMAGE_TYPE_SMALL;
        mEndIcon = null;
        mOnClickDelegate = null;
    }

    /** A builder of {@link SectionHeader}. */
    public static final class Builder {
        @NonNull
        CarText mTitle;
        @Nullable
        CarIcon mStartIcon;
        @SectionHeaderImageType
        int mStartIconType = IMAGE_TYPE_SMALL;
        @Nullable
        CarIcon mEndIcon;
        @Nullable
        OnClickDelegate mOnClickDelegate;

        /**
         * Sets the start icon of the header and its size type.
         *
         * <p>Only custom icon types (e.g. {@link CarIcon#TYPE_CUSTOM}) are supported.
         *
         * @param startIcon     the {@link CarIcon} to display before the title
         * @param startIconType the type of image to display. See {@link SectionHeader} for
         *                      supported image types (e.g. {@link SectionHeader#IMAGE_TYPE_SMALL}).
         * @throws NullPointerException     if {@code startIcon} is {@code null}
         * @throws IllegalArgumentException if {@code startIcon} is not a custom icon, or if it
         *                                  does not meet the {@link CarIconConstraints#DEFAULT}
         *                                  constraints
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setStartIcon(@NonNull CarIcon startIcon,
                @SectionHeaderImageType int startIconType) {
            requireNonNull(startIcon);
            if (startIcon.getType() != CarIcon.TYPE_CUSTOM) {
                throw new IllegalArgumentException("Only custom icon types are supported");
            }
            CarIconConstraints.DEFAULT.validateOrThrow(startIcon);
            mStartIcon = startIcon;
            mStartIconType = startIconType;
            return this;
        }

        /**
         * Sets the end icon of the header.
         *
         * <p>Only custom icon types (e.g. {@link CarIcon#TYPE_CUSTOM}) are supported.
         *
         * @param endIcon     the {@link CarIcon} to display after the title
         * @throws NullPointerException     if {@code endIcon} is {@code null}
         * @throws IllegalArgumentException if {@code endIcon} is not a custom icon, or if it
         *                                  does not meet the {@link CarIconConstraints#DEFAULT}
         *                                  constraints
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setEndIcon(@NonNull CarIcon endIcon) {
            requireNonNull(endIcon);
            if (endIcon.getType() != CarIcon.TYPE_CUSTOM) {
                throw new IllegalArgumentException("Only custom icon types are supported");
            }
            CarIconConstraints.DEFAULT.validateOrThrow(endIcon);
            mEndIcon = endIcon;
            return this;
        }

        /**
         * Sets the click listener for the header.
         *
         * <p>If a listener is set, the header will become focusable and clickable.
         *
         * @throws NullPointerException if {@code onClickListener} is {@code null}
         */
        @SuppressLint({"ExecutorRegistration", "MissingGetterMatchingBuilder"})
        @CanIgnoreReturnValue
        public @NonNull Builder setOnClickListener(@NonNull OnClickListener onClickListener) {
            mOnClickDelegate = OnClickDelegateImpl.create(onClickListener);
            return this;
        }

        /**
         * Builds the {@link SectionHeader}.
         *
         * @throws IllegalStateException if a title has not been set
         */
        public @NonNull SectionHeader build() {
            return new SectionHeader(this);
        }

        /**
         * Returns a {@link Builder} instance configured with the given {@code title}.
         *
         * <p>The title must be a plain string with no spans other than those supported by
         * {@link CarTextConstraints#TEXT_ONLY}.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public Builder(@NonNull CharSequence title) {
            this(CarText.create(requireNonNull(title)));
        }

        /**
         * Returns a {@link Builder} instance configured with the given {@code title}.
         *
         * <p>The title must be a plain string with no spans other than those supported by
         * {@link CarTextConstraints#TEXT_ONLY}.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        public Builder(@NonNull CarText title) {
            requireNonNull(title);
            if (title.isEmpty()) {
                throw new IllegalArgumentException("The title cannot be empty");
            }
            CarTextConstraints.TEXT_ONLY.validateOrThrow(title);
            mTitle = title;
        }
    }
}
