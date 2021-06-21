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

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.util.Objects;

/** A class representing information related to a destination. */
@CarProtocol
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
     * Returns the name of the destination or {@code null} if not set.
     *
     * @see Builder#setName(CharSequence)
     */
    @Nullable
    public CarText getName() {
        return mName;
    }

    /**
     * Returns the address of the destination or {@code null} if not set.
     *
     * @see Builder#setAddress(CharSequence)
     */
    @Nullable
    public CarText getAddress() {
        return mAddress;
    }

    /**
     * Returns an image to display with the destination or {@code null} if not set.
     *
     * @see Builder#setImage(CarIcon)
     */
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

    Destination(Builder builder) {
        mName = builder.mName;
        mAddress = builder.mAddress;
        mImage = builder.mImage;
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
        CarText mName;
        @Nullable
        CarText mAddress;
        @Nullable
        CarIcon mImage;

        /**
         * Sets the destination name formatted for the user's current locale.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code name} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setName(@NonNull CharSequence name) {
            mName = CarText.create(requireNonNull(name));
            return this;
        }

        /**
         * Sets the destination address formatted for the user's current locale.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code address} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setAddress(@NonNull CharSequence address) {
            mAddress = CarText.create(requireNonNull(address));
            return this;
        }

        /**
         * Sets the destination image to display.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * images targeting a 128 x 128 dp bounding box. If the image exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @NonNull
        public Builder setImage(@NonNull CarIcon image) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(image));
            mImage = image;
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

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
