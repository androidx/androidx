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
 * <p>The image will be modified to ensure safe a color contrast ratio with the content.
 *
 * <p>Initially only image backgrounds are supported but further options will be added.
 */
@CarProtocol
@KeepFields
@ExperimentalCarApi
@RequiresCarApi(9)
public final class Background {
    private final @Nullable CarIcon mImage;

    Background(Builder builder) {
        mImage = builder.mImage;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Background() {
        mImage = null;
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
        return Objects.equals(mImage, otherBackground.mImage);
    }

    @Override
    public @NonNull String toString() {
        return "[image: " + mImage + "]";
    }

    /** A builder of {@link Background}. */
    public static final class Builder {
        private @Nullable CarIcon mImage;

        /**
         * Sets the {@link CarIcon} that will be displayed as the background.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} is not of type
         * {@link CarIcon#TYPE_CUSTOM}
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
         * Constructs the {@link Background} defined by this builder.
         *
         * @throws IllegalStateException if the image is not set
         */
        public @NonNull Background build() {
            if (mImage == null) {
                throw new IllegalStateException("Image must be set");
            }
            return new Background(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /** Returns a new {@link Builder} with the data from the given {@link Background}
         *  instance. */
        public Builder(@NonNull Background background) {
            mImage = requireNonNull(background).getImage();
        }
    }
}
