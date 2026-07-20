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
 * Defines the visual style of a {@link Tab}.
 *
 * <p>This style can be used to customize the appearance of the visual container around a
 * tab, such as its shape (corner radius).
 *
 * <p>Custom styles will fall back to host defaults if they are unset.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class TabStyle {
    private final @Nullable Shape mShape;

    /**
     * Returns the {@link Shape} of the container, or {@code null} if not set.
     */
    public @Nullable Shape getShape() {
        return mShape;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TabStyle)) {
            return false;
        }
        TabStyle that = (TabStyle) other;
        return Objects.equals(mShape, that.mShape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape);
    }

    @Override
    public @NonNull String toString() {
        return "TabStyle { shape: "
                + mShape
                + " }";
    }

    private TabStyle(@NonNull Builder builder) {
        mShape = builder.mShape;
    }

    /** For serialization. */
    private TabStyle() {
        mShape = null;
    }

    /** A builder for {@link TabStyle}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable Shape mShape;

        /**
         * Sets the {@link Shape} of the container.
         *
         * @throws NullPointerException if {@code shape} is {@code null}
         */
        public @NonNull Builder setShape(@NonNull Shape shape) {
            mShape = requireNonNull(shape);
            return this;
        }

        /**
         * Constructs a {@link TabStyle} from the current state of this builder.
         *
         * @throws IllegalStateException if shape is {@code null}.
         */
        public @NonNull TabStyle build() {
            if (mShape == null) {
                throw new IllegalStateException("A shape must be set for a TabStyle");
            }
            return new TabStyle(this);
        }

        /** Returns an empty {@link TabStyle.Builder} instance. */
        public Builder() {
        }

        /** Creates a new {@link TabStyle.Builder}, populated from the input {@link TabStyle}.*/
        public Builder(@NonNull TabStyle tabStyle) {
            requireNonNull(tabStyle);
            mShape = tabStyle.getShape();
        }
    }
}
