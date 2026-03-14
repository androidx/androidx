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

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Defines the visual style of a {@link FilterChip}.
 *
 * <p>This style can be used to customize the appearance of the chip, such as its background and
 * content colors.
 *
 * <p>This style can be set on both the {@link FilterChipSection} to apply to all chips, and/or
 * individually to a single {@link FilterChip}. A common usage pattern is to set a default style on
 * the Section, then override the style of the individual "selected" FilterChips. Remember that
 * these colors will fall back to host defaults if they are unset or they fail contrast checks.
 */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(9)
@KeepFields
public final class FilterChipStyle {
    @Nullable
    private final CarColor mBackgroundColor;
    @Nullable
    private final CarColor mContentColor;
    @Nullable
    private final CarColor mOutlineColor;

    /**
     * Returns the background color of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the content color of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarColor getContentColor() {
        return mContentColor;
    }

    /**
     * Returns the stroke color of the chip, or {@code null} if not set.
     */
    @Nullable
    public CarColor getOutlineColor() {
        return mOutlineColor;
    }

    @Override
    @NonNull
    public String toString() {
        return "FilterChipStyle{"
                + "backgroundColor="
                + mBackgroundColor
                + ", contentColor="
                + mContentColor
                + ", strokeColor="
                + mOutlineColor
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBackgroundColor, mContentColor, mOutlineColor);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FilterChipStyle)) {
            return false;
        }
        FilterChipStyle otherStyle = (FilterChipStyle) other;
        return Objects.equals(mBackgroundColor, otherStyle.mBackgroundColor)
                && Objects.equals(mContentColor, otherStyle.mContentColor)
                && Objects.equals(mOutlineColor, otherStyle.mOutlineColor);
    }

    FilterChipStyle(Builder builder) {
        mBackgroundColor = builder.mBackgroundColor;
        mContentColor = builder.mContentColor;
        mOutlineColor = builder.mOutlineColor;
    }

    /** Constructs an empty instance, used by serialization code. */
    private FilterChipStyle() {
        mBackgroundColor = null;
        mContentColor = null;
        mOutlineColor = null;
    }

    /** A builder of {@link FilterChipStyle}. */
    public static final class Builder {
        @Nullable
        CarColor mBackgroundColor;
        @Nullable
        CarColor mContentColor;
        @Nullable
        CarColor mOutlineColor;

        /**
         * Sets the background color of the chip.
         *
         * <p>If the background color is not set, a host default color will be used,
         * depending on the {@link FilterChip#isSelected()} state.
         *
         * <p>@throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        @NonNull
        public Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            mBackgroundColor = requireNonNull(backgroundColor);
            return this;
        }

        /**
         * Sets the content color of the chip.
         *
         * <p>If the content color is not set, a host default color will be used,
         * depending on the {@link FilterChip#isSelected()} state.
         *
         * <p>The content color is used for the text and icons within the chip.
         *
         * <p>This color is contrast checked against the background color.
         * If it does not meet a minimum contrast threshold, this color will be ignored and a
         * host-provided fallback color will be used.
         *
         * @throws NullPointerException if {@code contentColor} is {@code null}
         */
        @NonNull
        public Builder setContentColor(@NonNull CarColor contentColor) {
            mContentColor = requireNonNull(contentColor);
            return this;
        }

        /**
         * Sets the stroke color of the chip.
         *
         * <p>If the content color is not set, a host default color will be used.
         *
         * <p>@throws NullPointerException if {@code strokeColor} is {@code null}
         */
        @NonNull
        public Builder setOutlineColor(@NonNull CarColor outlineColor) {
            mOutlineColor = requireNonNull(outlineColor);
            return this;
        }

        /**
         * Constructs the {@link FilterChipStyle} defined by this builder.
         */
        @NonNull
        public FilterChipStyle build() {
            return new FilterChipStyle(this);
        }
    }
}
