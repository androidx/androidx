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
import androidx.car.app.model.constraints.CarColorConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private final @Nullable CarColor mColor;
    private final @Nullable CarColor mTrackColor;
    @StrokeCap.StrokeCapType
    private final int mStrokeCap;

    /** Returns the color of the progress bar, or {@code null} if not set. */
    public @Nullable CarColor getColor() {
        return mColor;
    }

    /** Returns the color of the track behind the indicator, or {@code null} if not set. */
    public @Nullable CarColor getTrackColor() {
        return mTrackColor;
    }

    /** Returns the stroke cap of the progress bar, or {@link StrokeCap#DEFAULT} if not set. */
    @StrokeCap.StrokeCapType
    public int getStrokeCap() {
        return mStrokeCap;
    }

    @Override
    @NonNull
    public String toString() {
        return "CarProgressBarStyle{"
                + "color="
                + mColor
                + ", trackColor="
                + mTrackColor
                + ", strokeCap="
                + mStrokeCap
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mColor, mTrackColor, mStrokeCap);
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
                && Objects.equals(mTrackColor, otherStyle.mTrackColor)
                && mStrokeCap == otherStyle.mStrokeCap;
    }

    private CarProgressBarStyle(Builder builder) {
        mColor = builder.mColor;
        mTrackColor = builder.mTrackColor;
        mStrokeCap = builder.mStrokeCap;
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarProgressBarStyle() {
        mColor = null;
        mTrackColor = null;
        mStrokeCap = StrokeCap.DEFAULT;
    }

    /** A builder of {@link CarProgressBarStyle}. */
    public static final class Builder {
        private @Nullable CarColor mColor;
        private @Nullable CarColor mTrackColor;
        @StrokeCap.StrokeCapType
        private int mStrokeCap = StrokeCap.DEFAULT;

        /**
         * Sets the color of the actual progress bar. That is, the part of the component that
         * reflects progress and which fully encompasses the component when progress is complete.
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
         * Sets the color of the track over which the progress bar is drawn.
         *
         * <p>If a color is not set, or if the provided color does not pass a contrast check, the
         * host will use a default color.
         */
        public @NonNull Builder setTrackColor(@Nullable CarColor color) {
            if (color != null) {
                CarColorConstraints.UNCONSTRAINED.validateOrThrow(color);
            }
            mTrackColor = color;
            return this;
        }

        /**
         * Sets the stroke cap of the progress bar.
         *
         * <p>If unset, the host will use a default shape defined by the system.
         */
        public @NonNull Builder setStrokeCap(@StrokeCap.StrokeCapType int strokeCap) {
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
            mTrackColor = style.mTrackColor;
            mStrokeCap = style.mStrokeCap;
        }

        /** Constructs the {@link CarProgressBarStyle} defined by this builder. */
        @NonNull
        public CarProgressBarStyle build() {
            return new CarProgressBarStyle(this);
        }
    }
}
