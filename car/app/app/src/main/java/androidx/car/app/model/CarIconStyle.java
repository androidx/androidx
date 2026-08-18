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

import androidx.annotation.OptIn;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarColorConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Defines the visual styling applied to a {@link CarIcon}, including tinting behavior and geometric
 * shapes.
 *
 * <p>In general, icon styling falls into two categories:
 *
 * <ul>
 *   <li><b>Tinted:</b> Styled with {@link #TINTED}, which instructs the host vehicle system to
 *       automatically theme-tint the asset with {@link CarColor#DEFAULT} or applies an explicit
 *       solid {@link CarColor} tint configured via {@link Builder#setTint(CarColor)}.
 *   <li><b>Original (Not Tinted):</b> Styled with {@link #ORIGINAL}, which preserves the icon's
 *       original colors without allowing host-side tinting (e.g., user avatars, media album art,
 *       photos, or un-tinted brand graphics).
 * </ul>
 */
@CarProtocol
@KeepFields
@OptIn(markerClass = ExperimentalCarApi.class)
public class CarIconStyle {

    /**
     * Visual styling for icons that should be tinted.
     *
     * <p>By default, this instructs the host to automatically tint the icon or allows the dev to
     * provide an explicit custom tint. To apply an explicit custom tint color use {@link
     * Builder#Builder(CarIconStyle)} with {@link #TINTED} and call {@link
     * Builder#setTint(CarColor)}.
     */
    @NonNull public static final CarIconStyle TINTED = new CarIconStyle(CarColor.DEFAULT, null);

    /**
     * Visual styling for icons that should retain their original colors without allowing host-side
     * tinting.
     *
     * <p>Instructs the host vehicle system to preserve the full original colors of the image
     * without host tinting interference. Use this for assets such as user avatars, media album art,
     * photos, or un-tinted logos.
     */
    @NonNull public static final CarIconStyle ORIGINAL = new CarIconStyle(null, null);

    @Nullable private final CarColor mTint;
    @Nullable private final Shape mShape;

    // Internal constructor to initialize base static constants
    CarIconStyle(@Nullable CarColor tint, @Nullable Shape shape) {
        this.mTint = tint;
        this.mShape = shape;
    }

    private CarIconStyle(Builder builder) {
        this.mTint = builder.mTint;
        this.mShape = builder.mShape;
    }

    /** Constructs an empty instance for serialization. */
    private CarIconStyle() {
        this.mShape = null;
        this.mTint = null;
    }

    /**
     * Returns the explicit {@link CarColor} tint defined in this style (such as {@link
     * CarColor#DEFAULT} for auto-tinting or a custom tint), or {@code null} if no tint is applied.
     */
    public @Nullable CarColor getTint() {
        return mTint;
    }

    /** Returns the {@link Shape} defined in this style, or {@code null} if none is set. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public @Nullable Shape getShape() {
        return mShape;
    }

    @Override
    public @NonNull String toString() {
        return "CarIconStyle { tint: " + mTint + ", shape: " + mShape + " }";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTint, mShape);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarIconStyle)) {
            return false;
        }
        CarIconStyle that = (CarIconStyle) other;
        return Objects.equals(mTint, that.mTint) && Objects.equals(mShape, that.mShape);
    }

    /** A builder of {@link CarIconStyle}. */
    public static final class Builder {
        @Nullable private CarColor mTint;
        @Nullable private Shape mShape;

        /**
         * Creates a builder for a custom CarIconStyle based on an existing base style contract.
         *
         * @param baseStyle The base style contract to inherit from, typically {@link
         *     CarIconStyle#TINTED} or {@link CarIconStyle#ORIGINAL}.
         */
        public Builder(@NonNull CarIconStyle baseStyle) {
            requireNonNull(baseStyle);
            this.mTint = baseStyle.mTint;
            this.mShape = baseStyle.mShape;
        }

        /** Sets the geometric {@link Shape} boundary to crop or frame the icon graphic. */
        @RequiresCarApi(9)
        @ExperimentalCarApi
        @NonNull
        public Builder setShape(@NonNull Shape shape) {
            this.mShape = requireNonNull(shape);
            return this;
        }

        /**
         * Sets an explicit custom solid {@link CarColor} tint for single-color assets.
         *
         * <p>This will apply a solid color tint rather than leaving the image as-is. Use {@code
         * CarIconStyle.Builder(CarIconStyle.ORIGINAL)}` to create an un-tinted style
         */
        @NonNull
        public Builder setTint(@NonNull CarColor tint) {
            CarColorConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(tint));
            this.mTint = tint;
            return this;
        }

        /** Constructs the {@link CarIconStyle} defined by this builder. */
        @NonNull
        public CarIconStyle build() {
            return new CarIconStyle(this);
        }
    }
}
