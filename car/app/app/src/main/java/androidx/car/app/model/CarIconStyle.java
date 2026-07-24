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
 * Holds properties related ONLY to the visual properties of {@link CarIcon}.
 */
@CarProtocol
@KeepFields
@OptIn(markerClass = ExperimentalCarApi.class)
public class CarIconStyle {

    private final @Nullable CarColor mTint;
    private final @Nullable Shape mShape;

    /**
     * Returns the tint of the icon or {@code null} if not set.
     */
    public @Nullable CarColor getTint() {
        return mTint;
    }

    /**
     * Returns the {@link Shape} of the icon or {@code null} if not set.
     */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public @Nullable Shape getShape() {
        return mShape;
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

    @Override
    public int hashCode() {
        return Objects.hash(mTint, mShape);
    }

    @Override
    public @NonNull String toString() {
        return "CarIconStyle { tint: " + mTint + ", shape: " + mShape + " }";
    }

    private CarIconStyle(@NonNull Builder builder) {
        mTint = builder.mTint;
        mShape = builder.mShape;
    }

    /** For serialization. */
    private CarIconStyle() {
        mTint = null;
        mShape = null;
    }

    /** A builder for {@link CarIconStyle}. */
    public static final class Builder {
        @Nullable CarColor mTint;
        @Nullable Shape mShape;

        /**
         * Sets the {@link Shape} for the container.
         *
         * @throws NullPointerException if {@code shape} is {@code null}
         */
        public @NonNull Builder setTint(@NonNull CarColor tint) {
            CarColorConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(tint));
            mTint = tint;
            return this;
        }

        /**
         * Sets the {@link Shape} for the container.
         *
         * @throws NullPointerException if {@code shape} is {@code null}
         */
        @RequiresCarApi(9)
        @ExperimentalCarApi
        public @NonNull Builder setShape(@NonNull Shape shape) {
            mShape = requireNonNull(shape);
            return this;
        }

        /**
         * Constructs a {@link CarIconStyle} from the current state of this builder.
         *
         * @throws IllegalStateException if both shape is {@code null}
         */
        public @NonNull CarIconStyle build() {
            if (mTint == null && mShape == null) {
                throw new IllegalStateException(
                        "Either a tint or a shape must be set for a CarIconStyle");
            }

            return new CarIconStyle(this);
        }

        /**
         * Returns a {@link CarIconStyle.Builder} instance with unset properties.
         */
        public Builder() {
            mTint = null;
            mShape = null;
        }

        /**
         * Returns a {@link CarIconStyle.Builder} instance configured with the same data as the
         * given
         * {@link CarIconStyle} instance.
         *
         * @throws NullPointerException if {@code style} is {@code null}
         */
        public Builder(@NonNull CarIconStyle style) {
            requireNonNull(style);
            mTint = style.getTint();
            mShape = style.getShape();
        }
    }
}
