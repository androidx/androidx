/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.navigation.model;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.util.Objects;

/** A class representing information related to a destination. */
public final class Destination {
    @Keep
    @Nullable
    private final CarText mName;
    @Keep
    @Nullable
    private final CarText mAddress;
    @Keep
    @Nullable
    private final CarIcon mImage;

    /**
     * Constructs a new builder of {@link Destination} with the given name and address.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws NullPointerException if {@code address} is {@code null}.
     */
    @NonNull
    public static Builder builder(@NonNull CharSequence name, @NonNull CharSequence address) {
        return builder().setName(name).setAddress(address);
    }

    /** Constructs a new builder of {@link Destination}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public CarText getName() {
        return mName;
    }

    @Nullable
    public CarText getAddress() {
        return mAddress;
    }

    @Nullable
    public CarIcon getImage() {
        return mImage;
    }

    @Override
    @NonNull
    public String toString() {
        return "[name: "
                + CarText.toShortString(mName)
                + ", address: "
                + CarText.toShortString(mAddress)
                + ", image: "
                + mImage
                + "]";
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Destination)) {
            return false;
        }

        Destination otherDestination = (Destination) other;
        return Objects.equals(mName, otherDestination.mName)
                && Objects.equals(mAddress, otherDestination.mAddress)
                && Objects.equals(mImage, otherDestination.mImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName, mAddress, mImage);
    }

    private Destination(Builder builder) {
        this.mName = builder.mName;
        this.mAddress = builder.mAddress;
        this.mImage = builder.mImage;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Destination() {
        mName = null;
        mAddress = null;
        mImage = null;
    }

    /** A builder of {@link Destination}. */
    public static final class Builder {
        @Nullable
        private CarText mName;
        @Nullable
        private CarText mAddress;
        @Nullable
        private CarIcon mImage;

        /**
         * Sets the destination name formatted for the user's current locale, or {@code null} to not
         * display a destination name.
         */
        @NonNull
        public Builder setName(@Nullable CharSequence name) {
            this.mName = name == null ? null : CarText.create(name);
            return this;
        }

        /**
         * Sets the destination address formatted for the user's current locale, or {@code null}
         * to not
         * display an address.
         */
        @NonNull
        public Builder setAddress(@Nullable CharSequence address) {
            this.mAddress = address == null ? null : CarText.create(address);
            return this;
        }

        /**
         * Sets the destination image to display, or {@code null} to not display an image.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * The provided image should have a maximum size of 64 x 64 dp. If the image exceeds this
         * maximum size in either one of the dimensions, it will be scaled down and centered
         * inside the
         * bounding box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that
         * work with different car screen pixel densities.
         */
        @NonNull
        public Builder setImage(@Nullable CarIcon image) {
            CarIconConstraints.DEFAULT.validateOrThrow(image);
            this.mImage = image;
            return this;
        }

        /**
         * Constructs the {@link Destination} defined by this builder.
         *
         * <p>At least one of the name or the address must be set and not empty.
         *
         * @throws IllegalStateException if both the name and the address are {@code null} or empty.
         * @see #setName(CharSequence)
         * @see #setAddress(CharSequence)
         */
        @NonNull
        public Destination build() {
            if ((mName == null || mName.isEmpty()) && (mAddress == null || mAddress.isEmpty())) {
                throw new IllegalStateException("Both name and address cannot be null or empty");
            }
            return new Destination(this);
        }

        private Builder() {
        }
    }
}
