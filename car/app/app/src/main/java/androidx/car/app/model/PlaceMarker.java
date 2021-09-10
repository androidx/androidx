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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.constraints.CarColorConstraints;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/** Describes how a place is to be displayed on a map. */
@CarProtocol
public final class PlaceMarker {
    /**
     * Describes the type of image a marker icon represents.
     *
     * @hide
     */
    @IntDef(value = {TYPE_ICON, TYPE_IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface MarkerIconType {
    }

    /**
     * Represents a marker icon.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 64 x 64 dp bounding box. If necessary, the icon will be scaled down while
     * preserving its aspect ratio.
     *
     * <p>A tint color is expected to be provided via {@link CarIcon.Builder#setTint}. Otherwise, a
     * default tint color as determined by the host will be applied.
     */
    public static final int TYPE_ICON = 0;

    /**
     * Represents a marker image.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 72 x 72 dp bounding box. If necessary, the icon will be scaled down while
     * preserving its aspect ratio.
     */
    public static final int TYPE_IMAGE = 1;

    private static final int MAX_LABEL_LENGTH = 3;

    @Keep
    @Nullable
    private final CarIcon mIcon;
    @Keep
    @Nullable
    private final CarText mLabel;
    @Keep
    @Nullable
    private final CarColor mColor;
    @Keep
    @MarkerIconType
    private final int mIconType;

    /**
     * Returns the {@link CarIcon} associated with this marker or {@code null} if not set.
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the type of icon used with this marker.
     */
    @MarkerIconType
    public int getIconType() {
        return mIconType;
    }

    /**
     * Returns the text that should be rendered as the marker's content or {@code null} if one
     * is not set.
     *
     * <p>Note that a {@link PlaceMarker} can only display either an icon or a text label. If
     * both are set, then {@link #getIcon()} will take precedence.
     */
    @Nullable
    public CarText getLabel() {
        return mLabel;
    }

    /**
     * Returns the marker color or {@code null} if not set.
     *
     * <p>See {@link Builder#setColor} on rules related to how the color is applied.
     */
    @Nullable
    public CarColor getColor() {
        return mColor;
    }

    @NonNull
    @Override
    public String toString() {
        return "["
                + (mIcon != null
                ? mIcon.toString()
                : mLabel != null ? CarText.toShortString(mLabel) : super.toString())
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIcon, mLabel, mColor, mIconType);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlaceMarker)) {
            return false;
        }
        PlaceMarker otherMarker = (PlaceMarker) other;

        return Objects.equals(mIcon, otherMarker.mIcon)
                && Objects.equals(mLabel, otherMarker.mLabel)
                && Objects.equals(mColor, otherMarker.mColor)
                && mIconType == otherMarker.mIconType;
    }

    PlaceMarker(@NonNull Builder builder) {
        mIcon = builder.mIcon;
        mIconType = builder.mIconType;
        mLabel = builder.mLabel;
        mColor = builder.mColor;
    }

    /** Private empty constructor used by serialization code. */
    private PlaceMarker() {
        mIcon = null;
        mIconType = TYPE_ICON;
        mLabel = null;
        mColor = null;
    }

    /** A builder of {@link PlaceMarker}. */
    public static final class Builder {
        @Nullable
        CarIcon mIcon;
        @Nullable
        CarText mLabel;
        @Nullable
        CarColor mColor;
        @MarkerIconType
        int mIconType = TYPE_ICON;

        /**
         * Sets the icon to display in the marker.
         *
         * <p>Unless set with this method, the marker will not have an icon.
         *
         * <p>If a label is specified with {@link #setLabel}, the icon will take precedence over it.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * If the input icon's size exceeds the sizing requirements for the given icon type in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @param icon     the {@link CarIcon} to display inside the marker
         * @param iconType one of {@link #TYPE_ICON} or {@link #TYPE_IMAGE}
         * @throws NullPointerException if the {@code icon} is {@code null}
         */
        @NonNull
        public Builder setIcon(@NonNull CarIcon icon, @MarkerIconType int iconType) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(icon));
            mIcon = icon;
            mIconType = iconType;
            return this;
        }

        /**
         * Sets the text that should be displayed as the marker's content.
         *
         * <p>Unless set with this method, the marker will not have a label.
         *
         * <p>If an icon is specified with {@link #setIcon}, the icon will take precedence.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @param label the text to display inside of the marker. The string must have a maximum
         *              size of 3 characters. Set to {@code null} to let the host choose a
         *              labelling scheme (for example, using a sequence of numbers)
         * @throws NullPointerException if the {@code label} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setLabel(@NonNull CharSequence label) {
            if (requireNonNull(label).length() > MAX_LABEL_LENGTH) {
                throw new IllegalArgumentException(
                        "Marker label cannot contain more than " + MAX_LABEL_LENGTH
                                + " characters");
            }

            mLabel = CarText.create(label);
            return this;
        }

        /**
         * Sets the color that should be used for the marker on the map.
         *
         * <p>This color is applied in the following cases:
         *
         * <ul>
         *   <li>When the {@link PlaceMarker} is displayed on the map, the pin enclosing the icon or
         *       label will be painted using the given color.
         *   <li>When the {@link PlaceMarker} is displayed on the list, the color will be applied
         *       if the content is a label. A label rendered inside a map's pin cannot be colored
         *       and will always use the default color as chosen by the host.
         * </ul>
         *
         * <p>Unless set with this method, the host will use a default color for the marker.
         *
         * <p>The host may  ignore this color and use the default instead if the color does not
         * pass the contrast requirements.
         *
         * <p>A color cannot be set if the marker's icon type is of {@link #TYPE_IMAGE}.
         *
         * @throws NullPointerException if the {@code color} is {@code null}
         */
        @NonNull
        public Builder setColor(@NonNull CarColor color) {
            CarColorConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(color));
            mColor = color;
            return this;
        }

        /**
         * Constructs the {@link PlaceMarker} defined by this builder.
         *
         * @throws IllegalStateException if the icon is of the type {@link #TYPE_IMAGE} and a a
         *                               color is set
         */
        @NonNull
        public PlaceMarker build() {
            if (mColor != null && (mIcon != null && mIconType == TYPE_IMAGE)) {
                throw new IllegalStateException("Color cannot be set for icon set with TYPE_IMAGE");
            }

            return new PlaceMarker(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
