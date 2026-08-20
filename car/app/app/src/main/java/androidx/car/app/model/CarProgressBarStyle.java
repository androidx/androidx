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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarColorConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Defines the visual style of a {@link CarProgressBar}.
 *
 * <p>This style can be used to customize the appearance of the progress bar, such as its color
 * or stroke cap.
 *
 * <p>Custom styles will fall back to host defaults if they are unset,
 * or if they fail host-enforced contrast requirements.
 */
@CarProtocol
@KeepFields
@RequiresCarApi(9)
@ExperimentalCarApi
public final class CarProgressBarStyle {
    /**
     * Styles to use for line endings.
     */
    @RestrictTo(LIBRARY)
    @IntDef(value = {STROKE_CAP_DEFAULT, STROKE_CAP_ROUND, STROKE_CAP_SQUARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StrokeCap {
    }

    /**
     * The default stroke cap style defined by the system.
     */
    public static final int STROKE_CAP_DEFAULT = 0;
    /**
     * Begin and end contours with a semicircle extension.
     */
    public static final int STROKE_CAP_ROUND = 1;
    /**
     * Begin and end contours with a half square extension.
     */
    public static final int STROKE_CAP_SQUARE = 2;

    private final @Nullable CarColor mColor;
    @StrokeCap
    private final int mStrokeCap;

    /** Returns the color of the progress bar, or {@code null} if not set. */
    public @Nullable CarColor getColor() {
        return mColor;
    }

    /** Returns the stroke cap of the progress bar. */
    @StrokeCap
    public int getStrokeCap() {
        return mStrokeCap;
    }

    @Override
    @NonNull
    public String toString() {
        return "CarProgressBarStyle{"
                + "color="
                + mColor
                + ", strokeCap="
                + mStrokeCap
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mColor, mStrokeCap);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarProgressBarStyle)) {
            return false;
        }
        CarProgressBarStyle otherStyle = (CarProgressBarStyle) other;
        return Objects.equals(mColor, otherStyle.mColor)
                && mStrokeCap == otherStyle.mStrokeCap;
    }

    private CarProgressBarStyle(Builder builder) {
        mColor = builder.mColor;
        mStrokeCap = builder.mStrokeCap;
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarProgressBarStyle() {
        mColor = null;
        mStrokeCap = STROKE_CAP_DEFAULT;
    }

    /** A builder of {@link CarProgressBarStyle}. */
    public static final class Builder {
        private @Nullable CarColor mColor;
        @StrokeCap
        private int mStrokeCap = CarProgressBarStyle.STROKE_CAP_DEFAULT;

        /**
         * Sets the color of the progress bar.
         *
         * <p>If a color is not set, or if the provided color does not pass a contrast check, the
         * host will use a default color.
         */
        public @NonNull Builder setColor(@Nullable CarColor color) {
            if (color != null) {
                CarColorConstraints.UNCONSTRAINED.validateOrThrow(color);
            }
            mColor = color;
            return this;
        }

        /**
         * Sets the stroke cap of the progress bar.
         *
         * <p>If a default stroke cap is set, the host will use a default shape defined by the
         * system.
         */
        public @NonNull Builder setStrokeCap(@StrokeCap int strokeCap) {
            mStrokeCap = strokeCap;
            return this;
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /** Creates a new {@link Builder}, populated from the input {@link CarProgressBarStyle}. */
        public Builder(@NonNull CarProgressBarStyle style) {
            requireNonNull(style);
            mColor = style.mColor;
            mStrokeCap = style.mStrokeCap;
        }

        /** Constructs the {@link CarProgressBarStyle} defined by this builder. */
        @NonNull
        public CarProgressBarStyle build() {
            return new CarProgressBarStyle(this);
        }
    }
}
