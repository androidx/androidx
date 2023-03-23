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

    @Override
    @NonNull
    public String toString() {
        return "[hasDot: " + mHasDot + ", backgroundColor: " + mBackgroundColor + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHasDot, mBackgroundColor);
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
                && Objects.equals(mBackgroundColor, otherBadge.mBackgroundColor);
    }

    Badge(Builder builder) {
        mHasDot = builder.mHasDot;
        mBackgroundColor = builder.mBackgroundColor;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Badge() {
        mHasDot = false;
        mBackgroundColor = null;
    }

    /** A builder of {@link Badge}. */
    public static final class Builder {
        boolean mHasDot;
        @Nullable
        CarColor mBackgroundColor;

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
         * Constructs the {@link Badge} defined by this builder.
         *
         * @throws IllegalStateException if no property is set on the badge.
         */
        @NonNull
        public Badge build() {
            if (!mHasDot) {
                throw new IllegalStateException("At least one property must be set on the badge");
            }
            return new Badge(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
