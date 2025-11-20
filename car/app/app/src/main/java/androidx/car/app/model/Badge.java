/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a badge that can be displayed as an overlay on top of an image.
 *
 * <p>A badge denotes some sort of call to action, notification, alert, etc. An example is
 * badging of application icons in a launcher to add a number which denotes how many active
 * notifications that application has.
 *
 * <p>Badges may have a dot which is a colored circle appearing in a different location to the icon.
 */
@CarProtocol
@ExperimentalCarApi
@KeepFields
public class Badge {
    private final boolean mHasDot;
    private final @Nullable CarColor mDotColor;
    private final @Nullable CarIcon mIcon;
    private final @Nullable CarColor mIconBackgroundColor;

    /**
     * Returns whether the badge has a dot.
     *
     * @see Builder#setHasDot(boolean)
     */
    public boolean hasDot() {
        return mHasDot;
    }

    /**
     * Returns the dot color.
     */
    public @Nullable CarColor getDotColor() {
        return mDotColor;
    }

    /**
     * Returns the dot background color.
     *
     * @deprecated use {@link #getDotColor()} instead.
     */
    @Deprecated
    public @Nullable CarColor getBackgroundColor() {
        return mDotColor;
    }

    /**
     * Returns the badge icon.
     *
     * @see Builder#setIcon(CarIcon)
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the icon background color.
     */
    public @Nullable CarColor getIconBackgroundColor() {
        return mIconBackgroundColor;
    }

    @Override
    public @NonNull String toString() {
        return "[hasDot: " + mHasDot
                + ", dotColor: " + mDotColor
                + ", icon: " + mIcon
                + ", iconBackgroundColor: " + mIconBackgroundColor + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHasDot, mDotColor, mIcon, mIconBackgroundColor);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Badge)) {
            return false;
        }
        Badge otherBadge = (Badge) other;

        return mHasDot == otherBadge.mHasDot
                && Objects.equals(mDotColor, otherBadge.mDotColor)
                && Objects.equals(mIcon, otherBadge.mIcon)
                && Objects.equals(mIconBackgroundColor, otherBadge.mIconBackgroundColor);
    }

    Badge(Builder builder) {
        mHasDot = builder.mHasDot;
        mDotColor = builder.mDotColor;
        mIcon = builder.mIcon;
        mIconBackgroundColor = builder.mIconBackgroundColor;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Badge() {
        mHasDot = false;
        mDotColor = null;
        mIcon = null;
        mIconBackgroundColor = null;
    }

    /** A builder of {@link Badge}. */
    public static final class Builder {
        boolean mHasDot;
        @Nullable
        CarColor mDotColor;
        @Nullable
        CarIcon mIcon;
        @Nullable
        CarColor mIconBackgroundColor;

        /**
         * Enables a circular dot that denotes some sort of alert, notification, etc.
         */
        public @NonNull Builder setHasDot(boolean hasDot) {
            mHasDot = hasDot;
            return this;
        }

        /**
         * Sets the color of the dot to the given {@code color}.
         */
        public @NonNull Builder setDotColor(@NonNull CarColor color) {
            mDotColor = color;
            return this;
        }

        /**
         * Sets the color of the dot to the given {@code backgroundColor}.
         *
         * @deprecated use {@link #setDotColor(CarColor)} instead.
         */
        @Deprecated
        public @NonNull Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            mDotColor = backgroundColor;
            return this;
        }

        /**
         * Sets an icon to be displayed as a badge.
         *
         * <p>An icon badge gives context about the associated element on which it is displayed. For
         * example, a work profile icon badge is displayed with an app icon to indicate that
         * it is a work app.
         */
        public @NonNull Builder setIcon(@NonNull CarIcon icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets the color of the icon background to the given {@code color}.
         */
        public @NonNull Builder setIconBackgroundColor(@NonNull CarColor color) {
            mIconBackgroundColor = color;
            return this;
        }

        /**
         * Constructs the {@link Badge} defined by this builder.
         *
         * @throws IllegalStateException if the badge doesn't have a dot or an icon.
         * @throws IllegalStateException if the a dot color is set but the badge has no dot.
         * @throws IllegalStateException if the a icon background color is set but the badge has
         * no icon.
         */
        public @NonNull Badge build() {
            if (!mHasDot && mIcon == null) {
                throw new IllegalStateException("A badge must have a dot or an icon set");
            }
            if (!mHasDot && mDotColor != null) {
                throw new IllegalStateException("The dot must be enabled to set the dot "
                        + "color.");
            }
            if (mIcon == null && mIconBackgroundColor != null) {
                throw new IllegalStateException("The icon must be set to set the icon background "
                        + "color.");
            }
            return new Badge(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
