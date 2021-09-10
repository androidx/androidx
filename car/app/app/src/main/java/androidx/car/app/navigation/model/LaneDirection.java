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

package androidx.car.app.navigation.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Defines the possible directions a driver can go when using a particular lane at a particular step
 * in the navigation.
 *
 * <p>These directions can be combined and sent to the host to display a lane configuration to the
 * user.
 *
 * @see Lane
 */
@CarProtocol
public final class LaneDirection {
    /**
     * Turn amount and direction.
     *
     * @hide
     */
    @IntDef({
            SHAPE_UNKNOWN,
            SHAPE_STRAIGHT,
            SHAPE_SLIGHT_LEFT,
            SHAPE_SLIGHT_RIGHT,
            SHAPE_NORMAL_LEFT,
            SHAPE_NORMAL_RIGHT,
            SHAPE_SHARP_LEFT,
            SHAPE_SHARP_RIGHT,
            SHAPE_U_TURN_LEFT,
            SHAPE_U_TURN_RIGHT
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Shape {
    }

    /** The shape is unknown, in which case no lane information should be shown. */
    @Shape
    public static final int SHAPE_UNKNOWN = 1;

    /** No turn. */
    @Shape
    public static final int SHAPE_STRAIGHT = 2;

    /** Slight left turn, from 10 (included) to 45 (excluded) degrees. */
    @Shape
    public static final int SHAPE_SLIGHT_LEFT = 3;

    /** Slight right turn, from 10 (included) to 45 (excluded) degrees. */
    @Shape
    public static final int SHAPE_SLIGHT_RIGHT = 4;

    /** Regular left turn, from 45 (included) to 135 (excluded) degrees. */
    @Shape
    public static final int SHAPE_NORMAL_LEFT = 5;

    /** Regular right turn, from 45 (included) to 135 (excluded) degrees. */
    @Shape
    public static final int SHAPE_NORMAL_RIGHT = 6;

    /** Sharp left turn, from 135 (included) to 175 (excluded) degrees. */
    @Shape
    public static final int SHAPE_SHARP_LEFT = 7;

    /** Sharp right turn, from 135 (included) to 175 (excluded) degrees. */
    @Shape
    public static final int SHAPE_SHARP_RIGHT = 8;

    /**
     * A left turn onto the opposite side of the same street, from 175 (included) to 180 (included)
     * degrees
     */
    @Shape
    public static final int SHAPE_U_TURN_LEFT = 9;

    /**
     * A right turn onto the opposite side of the same street, from 175 (included) to 180 (included)
     * degrees
     */
    @Shape
    public static final int SHAPE_U_TURN_RIGHT = 10;

    @Keep
    @Shape
    private final int mShape;
    @Keep
    private final boolean mIsRecommended;

    /**
     * Constructs a new instance of a {@link LaneDirection}.
     *
     * @param shape         one of the {@code SHAPE_*} static constants defined in this class
     * @param isRecommended indicates whether the {@link LaneDirection} is the one the driver should
     *                      take in order to stay on the navigation route
     */
    @NonNull
    public static LaneDirection create(@Shape int shape, boolean isRecommended) {
        return new LaneDirection(shape, isRecommended);
    }

    /** Returns shape of this lane direction. */
    @Shape
    public int getShape() {
        return mShape;
    }

    /**
     * Returns whether this is a direction the driver should take in order to stay on the navigation
     * route.
     */
    public boolean isRecommended() {
        return mIsRecommended;
    }

    @Override
    @NonNull
    public String toString() {
        return "[shape: " + mShape + ", isRecommended: " + mIsRecommended + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mIsRecommended);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LaneDirection)) {
            return false;
        }

        LaneDirection otherDirection = (LaneDirection) other;
        return mShape == otherDirection.mShape && mIsRecommended == otherDirection.mIsRecommended;
    }

    private LaneDirection(@Shape int shape, boolean isRecommended) {
        mShape = shape;
        mIsRecommended = isRecommended;
    }

    /** Constructs an empty instance, used by serialization code. */
    private LaneDirection() {
        mShape = SHAPE_UNKNOWN;
        mIsRecommended = false;
    }
}
