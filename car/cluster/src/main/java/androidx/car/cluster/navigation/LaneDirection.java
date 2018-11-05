/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * One of the possible directions a driver can go when using a particular lane at a particular
 * step in the navigation. This can be used by the consumer to choose what icon or icons to combine
 * in order to display a lane configuration to the user.
 */
@VersionedParcelize
public final class LaneDirection implements VersionedParcelable {
    /**
     * Turn amount and direction.
     */
    public enum Shape {
        /**
         * The shape is unknown to the consumer, in which case the consumer shouldn't show any
         * lane information at all.
         */
        UNKNOWN,
        /**
         * No turn.
         */
        STRAIGHT,
        /**
         * Slight turn (10-45 degrees).
         */
        SLIGHT_LEFT,
        /**
         * @see #SLIGHT_LEFT
         */
        SLIGHT_RIGHT,
        /**
         * Regular turn (45-135 degrees).
         */
        NORMAL_LEFT,
        /**
         * @see #NORMAL_LEFT
         */
        NORMAL_RIGHT,
        /**
         * Sharp turn (135-175 degrees).
         */
        SHARP_LEFT,
        /**
         * @see #SHARP_LEFT
         */
        SHARP_RIGHT,
        /**
         * A turn onto the opposite side of the same street (175-180 degrees).
         */
        U_TURN_LEFT,
        /**
         * @see #U_TURN_LEFT
         */
        U_TURN_RIGHT,
    }


    @ParcelField(1)
    EnumWrapper<Shape> mShape;
    @ParcelField(2)
    boolean mHighlighted;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    LaneDirection() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    LaneDirection(@NonNull EnumWrapper<Shape> shape, boolean highlighted) {
        mShape = shape;
        mHighlighted = highlighted;
    }

    /**
     * Builder for creating a {@link LaneDirection}
     */
    public static final class Builder {
        private EnumWrapper<Shape> mShape;
        private boolean mHighlighted;

        /**
         * Sets the {@link Shape} of this lane direction, and any fallback values that could be used
         * by the consumer if the shape is unknown to it.
         *
         * @param shape lane direction shape
         * @param fallbacks Variations of {@code shape}, in case the consumer of this API doesn't
         *                  know the main one (used for backward compatibility).
         * @return this object for chaining
         */
        @NonNull
        public Builder setShape(@NonNull Shape shape, @NonNull Shape ... fallbacks) {
            mShape = EnumWrapper.of(shape, fallbacks);
            return this;
        }

        /**
         * Sets whether this a direction the driver could take in order to stay in the navigation
         * route.
         *
         * @param highlighted true if this is a recommended lane direction, or false otherwise.
         * @return this object for chaining
         */
        @NonNull
        public Builder setHighlighted(boolean highlighted) {
            mHighlighted = highlighted;
            return this;
        }

        /**
         * Returns a {@link LaneDirection} built with the provided information.
         */
        @NonNull
        public LaneDirection build() {
            return new LaneDirection(mShape, mHighlighted);
        }

    }

    /**
     * Returns shape of this lane direction.
     */
    @NonNull
    public Shape getShape() {
        return EnumWrapper.getValue(mShape, Shape.UNKNOWN);
    }

    /**
     * Returns whether this is a direction the driver should take in order to stay in the
     * navigation route.
     */
    public boolean isHighlighted() {
        return mHighlighted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LaneDirection laneDirection = (LaneDirection) o;
        return Objects.equals(getShape(), laneDirection.getShape())
                && isHighlighted() == laneDirection.isHighlighted();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShape(), isHighlighted());
    }

    @Override
    public String toString() {
        return String.format("{shape: %s, highlighted: %s}", mShape, mHighlighted);
    }
}
