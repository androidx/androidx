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
 * Defines the visual style of a {@link Tab}.
 *
 * <p>This style can be used to customize the appearance of the visual container around a
 * tab, such as its shape (corner radius), selected background color, and text color.
 *
 * <p>Custom styles will fall back to host defaults if they are unset,
 * or if they fail host-enforced contrast requirements.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class TabStyle {
    private final @Nullable Shape mShape;
    private final @Nullable CarColor mSelectedBackgroundColor;
    private final @Nullable CarColor mTextColor;


    /**
     * Returns the {@link Shape} of the container, or {@code null} if not set.
     */
    public @Nullable Shape getShape() {
        return mShape;
    }

    /**
     * Returns the {@link CarColor} of the selected tab container background, or {@code null} if
     * not set.
     */
    public @Nullable CarColor getSelectedBackgroundColor() {
        return mSelectedBackgroundColor;
    }

    /**
     * Returns the {@link CarColor} of the text within the tab container, or {@code null} if
     * not set.
     */
    public @Nullable CarColor getTextColor() {
        return mTextColor;
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
        return Objects.equals(mShape, that.mShape)
                && Objects.equals(mSelectedBackgroundColor, that.mSelectedBackgroundColor)
                && Objects.equals(mTextColor, that.mTextColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mSelectedBackgroundColor, mTextColor);
    }

    @Override
    public @NonNull String toString() {
        return "TabStyle { shape: "
                + mShape
                + ", selectedBackgroundColor: "
                + mSelectedBackgroundColor
                + ", textColor: "
                + mTextColor
                + " }";
    }

    private TabStyle(@NonNull Builder builder) {
        mShape = builder.mShape;
        mSelectedBackgroundColor = builder.mSelectedBackgroundColor;
        mTextColor = builder.mTextColor;
    }

    /** For serialization. */
    private TabStyle() {
        mShape = null;
        mSelectedBackgroundColor = null;
        mTextColor = null;
    }

    /** A builder for {@link TabStyle}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable Shape mShape;
        @Nullable CarColor mSelectedBackgroundColor;
        @Nullable CarColor mTextColor;

        /**
         * Sets the {@link Shape} of the container, or {@code null} to clear the shape.
         *
         * <p>If a shape is not set or is cleared, the container shape will fall back to host
         * defaults.
         */
        public @NonNull Builder setShape(@Nullable Shape shape) {
            mShape = shape;
            return this;
        }

        /**
         * Sets the {@link CarColor} of the tab indicator background when selected, or {@code null}
         * to clear the color.
         *
         * <p>If a selected background color is not set or is cleared, the background color will
         * fall back to host defaults.
         *
         * <p>Custom background colors are applied only when the tab is active (selected). On an
         * unselected tab, the background color is ignored by the host and defaults to transparent.
         *
         * @throws IllegalArgumentException if {@code selectedBackgroundColor} contains an
         *                                  unsupported color type
         */
        public @NonNull Builder setSelectedBackgroundColor(
                @Nullable CarColor selectedBackgroundColor) {
            if (selectedBackgroundColor != null) {
                CarColorConstraints.UNCONSTRAINED.validateOrThrow(selectedBackgroundColor);
            }
            mSelectedBackgroundColor = selectedBackgroundColor;
            return this;
        }

        /**
         * Sets the {@link CarColor} for text displayed within the tab container, or {@code null}
         * to clear the color.
         *
         * <p>If a text color is not set or is cleared, the text color will fall back to host
         * defaults.
         *
         * <p>To customize the color of the tab's icon, see
         * {@link CarIconStyle.Builder#setTint(CarColor)}.
         *
         * @throws IllegalArgumentException if {@code textColor} contains an unsupported color
         *                                  type
         */
        public @NonNull Builder setTextColor(
                @Nullable CarColor textColor) {
            if (textColor != null) {
                CarColorConstraints.UNCONSTRAINED.validateOrThrow(textColor);
            }
            mTextColor = textColor;
            return this;
        }

        /**
         * Constructs a {@link TabStyle} from the current state of this builder.
         *
         * @throws IllegalStateException if shape, selected background color, and text color are
         * all unset.
         */
        public @NonNull TabStyle build() {
            if (mShape == null && mSelectedBackgroundColor == null && mTextColor == null) {
                throw new IllegalStateException(
                        "Either a shape, selected background, or text color must be set for a"
                                + " TabStyle");
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
            mSelectedBackgroundColor = tabStyle.getSelectedBackgroundColor();
            mTextColor = tabStyle.getTextColor();
        }
    }
}
