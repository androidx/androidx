/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.car.app.model.constraints.BackgroundConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Holds properties related ONLY to the visual container around a {@link CondensedItem}.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class CondensedItemStyle {
    private final @Nullable Shape mShape;
    private final @Nullable Background mBackground;

    /**
     * Returns the {@link Shape} of the container, or {@code null} if not set.
     */
    public @Nullable Shape getShape() {
        return mShape;
    }

    /**
     * Returns the {@link Background} of the container, or {@code null} if not set.
     */
    public @Nullable Background getBackground() {
        return mBackground;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CondensedItemStyle)) {
            return false;
        }
        CondensedItemStyle that = (CondensedItemStyle) other;
        return Objects.equals(mShape, that.mShape)
                && Objects.equals(mBackground, that.mBackground);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mBackground);
    }

    @Override
    public @NonNull String toString() {
        return "CondensedItemStyle { shape: " + mShape + ", background: " + mBackground + " }";
    }

    private CondensedItemStyle(@NonNull Builder builder) {
        mShape = builder.mShape;
        mBackground = builder.mBackground;
    }

    /** For serialization. */
    private CondensedItemStyle() {
        mShape = null;
        mBackground = null;
    }

    /** A builder for {@link CondensedItemStyle}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable Shape mShape;
        @Nullable Background mBackground;

        /**
         * Sets the {@link Shape} for the container.
         *
         * @throws NullPointerException if {@code shape} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setShape(@NonNull Shape shape) {
            mShape = requireNonNull(shape);
            return this;
        }

        /**
         * Sets the {@link Background} for the container.
         *
         * <p>{@code background} must conform to {@link BackgroundConstraints.COLOR_ONLY}.
         *
         * @throws NullPointerException     if {@code background} is {@code null}
         * @throws IllegalArgumentException if {@code background} contains unsupported type
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setBackground(@NonNull Background background) {
            BackgroundConstraints.COLOR_ONLY.validateOrThrow(background);
            mBackground = requireNonNull(background);
            return this;
        }

        /**
         * Constructs a {@link CondensedItemStyle} from the current state of this builder.
         *
         * @throws IllegalStateException if both shape and background are {@code null}
         */
        public @NonNull CondensedItemStyle build() {
            if (mShape == null && mBackground == null) {
                throw new IllegalStateException(
                        "Either a shape or a background must be set for a CondensedItemStyle");
            }


            return new CondensedItemStyle(this);
        }
    }
}
