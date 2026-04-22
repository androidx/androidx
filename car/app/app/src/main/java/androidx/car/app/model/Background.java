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

import static java.util.Objects.requireNonNull;

import android.graphics.Color;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A background for a user interface component.
 *
 * <p>The image or color will be modified to ensure safe a color contrast ratio with the content.
 *
 * <p>The background can be set as either a solid color using {@link CarColor} or an image
 * using {@link CarIcon}. Only one of these can be set at a time.
 */
@CarProtocol
@KeepFields
@ExperimentalCarApi
@RequiresCarApi(9)
public final class Background {
    /**
     * A transparent background.
     */
    public static final @NonNull Background TRANSPARENT =
            new Background.Builder().setColor(
                    CarColor.createCustom(Color.TRANSPARENT, Color.TRANSPARENT)).build();

    private final @Nullable CarColor mColor;
    private final @Nullable CarIcon mImage;

    Background(Builder builder) {
        mColor = builder.mColor;
        mImage = builder.mImage;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Background() {
        mColor = null;
        mImage = null;
    }

    /**
     * Returns the {@link CarColor} used for the background, or {@code null} if not set.
     */
    public @Nullable CarColor getColor() {
        return mColor;
    }

    /**
     * Returns the {@link CarIcon} associated with this background, or {@code null} if not set.
     */
    public @Nullable CarIcon getImage() {
        return mImage;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Background)) {
            return false;
        }
        Background otherBackground = (Background) other;
        return Objects.equals(mImage, otherBackground.mImage) && Objects.equals(mColor,
                otherBackground.mColor);
    }

    @Override
    public @NonNull String toString() {
        return "Background [image: " + mImage + ", color: " + mColor + "]";
    }

    /** A builder of {@link Background}. */
    public static final class Builder {
        private @Nullable CarIcon mImage;
        private @Nullable CarColor mColor;

        /**
         * Sets the {@link CarIcon} that will be displayed as the background.
         *
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} is not of type
         *                                  {@link CarIcon#TYPE_CUSTOM}
         */
        public @NonNull Builder setImage(@NonNull CarIcon image) {
            requireNonNull(image);
            if (image.getType() != CarIcon.TYPE_CUSTOM) {
                throw new IllegalArgumentException("Only custom images are supported for "
                        + "background");
            }
            mImage = image;
            return this;
        }

        /**
         * Sets the {@link CarColor} to be used for the background.
         *
         * @throws NullPointerException if {@code color} is {@code null}
         */
        public @NonNull Builder setColor(@NonNull CarColor color) {
            mColor = requireNonNull(color);
            return this;
        }

        /**
         * Constructs the {@link Background} defined by this builder.
         *
         * <p>Ensures that exactly one of either an image or a color is set for the background.
         *
         * @throws IllegalStateException if neither an image nor a color is set,
         *                               or if both an image and a color are set.
         */
        public @NonNull Background build() {
            // Check if both are null OR both are set using an equality check
            if ((mImage == null) == (mColor == null)) {
                throw new IllegalStateException(
                        "Background must have exactly one property set: either Image or Color."
                );
            }
            return new Background(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new {@link Builder} with the data from the given {@link Background}
         * instance.
         */
        public Builder(@NonNull Background background) {
            mImage = requireNonNull(background).getImage();
        }
    }
}
