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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import java.util.Objects;

/**
 * Represents a badge that can be displayed as an overlay on top of an image.
 *
 * <p>A badge denotes some sort of call to action, notification, alert, etc. An example is
 * badging of application icons in a launcher to add a number at the top right corner which
 * denotes how many active notifications that application has.
 */
@CarProtocol
@ExperimentalCarApi
@KeepFields
@RequiresCarApi(7)
public class Badge {

    private final boolean mHasDot;
    @Nullable
    private final CarColor mBackgroundColor;
    @Nullable
    private final CarIcon mIcon;

    /**
     * Returns whether the badge has a dot.
     *
     * @see Builder#setHasDot(boolean)
     */
    public boolean hasDot() {
        return mHasDot;
    }

    /**
     * Returns the dot background color.
     */
    @Nullable
    public CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the badge icon.
     *
     * @see Builder#setIcon(CarIcon)
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    @Override
    @NonNull
    public String toString() {
        return "[hasDot: " + mHasDot
                + ", backgroundColor: " + mBackgroundColor
                + ", icon: " + mIcon + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHasDot, mBackgroundColor, mIcon);
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
                && Objects.equals(mBackgroundColor, otherBadge.mBackgroundColor)
                && Objects.equals(mIcon, otherBadge.mIcon);
    }

    Badge(Builder builder) {
        mHasDot = builder.mHasDot;
        mBackgroundColor = builder.mBackgroundColor;
        mIcon = builder.mIcon;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Badge() {
        mHasDot = false;
        mBackgroundColor = null;
        mIcon = null;
    }

    /** A builder of {@link Badge}. */
    public static final class Builder {
        boolean mHasDot;
        @Nullable
        CarColor mBackgroundColor;
        @Nullable
        CarIcon mIcon;

        /**
         * Enables a circular dot that denotes some sort of alert, notification, etc.
         */
        @NonNull
        public Builder setHasDot(boolean hasDot) {
            mHasDot = hasDot;
            return this;
        }

        /**
         * Sets the color of the dot to the given {@code backgroundColor}.
         */
        @NonNull
        public Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets an icon to be displayed as a badge.
         *
         * <p>An icon badge gives context about the associated element on which it is displayed. For
         * example, a work profile icon badge is displayed with an app icon to indicate that
         * it is a work app.
         */
        @NonNull
        public Builder setIcon(@NonNull CarIcon icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Constructs the {@link Badge} defined by this builder.
         *
         * @throws IllegalStateException if the badge doesn't have a dot or an icon.
         */
        @NonNull
        public Badge build() {
            if (!mHasDot && mIcon == null) {
                throw new IllegalStateException("A badge must have a dot or an icon set");
            }
            if (!mHasDot && mBackgroundColor != null) {
                throw new IllegalStateException("The dot must be enabled to set the background "
                        + "color.");
            }
            return new Badge(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
