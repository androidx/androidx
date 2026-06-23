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
import androidx.car.app.model.constraints.BackgroundConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Defines the visual style of a {@link Banner}.
 *
 * <p>This style can be used to customize the appearance of the visual container around a
 * banner, such as its shape (corner radius) and background color.
 *
 * <p>Custom styles will fall back to host defaults if they are unset,
 * or if they fail host-enforced contrast requirements.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class BannerStyle {
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
        if (!(other instanceof BannerStyle)) {
            return false;
        }
        BannerStyle that = (BannerStyle) other;
        return Objects.equals(mShape, that.mShape)
                && Objects.equals(mBackground, that.mBackground);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mBackground);
    }

    @Override
    public @NonNull String toString() {
        return "BannerStyle { shape: " + mShape + ", background: " + mBackground + " }";
    }

    private BannerStyle(@NonNull Builder builder) {
        mShape = builder.mShape;
        mBackground = builder.mBackground;
    }

    /** For serialization. */
    private BannerStyle() {
        mShape = null;
        mBackground = null;
    }

    /** A builder for {@link BannerStyle}. */
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
        public @NonNull Builder setBackground(@NonNull Background background) {
            BackgroundConstraints.COLOR_ONLY.validateOrThrow(requireNonNull(background));
            mBackground = background;
            return this;
        }

        /**
         * Constructs a {@link BannerStyle} from the current state of this builder.
         *
         * @throws IllegalStateException if both shape and background are {@code null}
         */
        public @NonNull BannerStyle build() {
            if (mShape == null && mBackground == null) {
                throw new IllegalStateException(
                        "Either a shape or a background must be set for a BannerStyle");
            }

            return new BannerStyle(this);
        }
    }
}
