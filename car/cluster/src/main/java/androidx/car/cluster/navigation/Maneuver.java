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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Information about a maneuver that the driver will be required to perform.
 */
@VersionedParcelize
public final class Maneuver implements VersionedParcelable {
    /**
     * Possible maneuver types.
     */
    public enum Type {
        /**
         * Maneuver type is unknown to the OEM cluster rendering service, in which case the OEM
         * cluster rendering service shouldn't show any maneuver information.
         */
        UNKNOWN,
        /**
         * Starting point of the navigation (e.g. "Start driving on Main St.")
         */
        DEPART,
        /**
         * No turn, but the street name changes (e.g. "Continue on Main St.")
         */
        NAME_CHANGE,
        /**
         * No turn (0-10 degrees). Used to say "Keep left/right". Note that this
         * is used in contrast to {@link Type#STRAIGHT} for disambiguating cases where there
         * is more than one option to go into the same general direction.
         */
        KEEP_LEFT,
        /**
         * @see #KEEP_LEFT
         */
        KEEP_RIGHT,
        /**
         * Slight turn at an intersection (10-45 degrees).
         */
        TURN_SLIGHT_LEFT,
        /**
         * @see #TURN_SLIGHT_LEFT
         */
        TURN_SLIGHT_RIGHT,
        /**
         * Regular turn at an intersection (45-135 degrees).
         */
        TURN_NORMAL_LEFT,
        /**
         * @see #TURN_NORMAL_LEFT
         */
        TURN_NORMAL_RIGHT,
        /**
         * Sharp turn at an intersection (135-175 degrees).
         */
        TURN_SHARP_LEFT,
        /**
         * @see #TURN_SHARP_LEFT
         */
        TURN_SHARP_RIGHT,
        /**
         * A turn onto the opposite side of the same street (175-180 degrees).
         */
        U_TURN_LEFT,
        /**
         * @see #U_TURN_LEFT
         */
        U_TURN_RIGHT,
        /**
         * Slight turn (10-45 degrees) to enter a turnpike or freeway.
         */
        ON_RAMP_SLIGHT_LEFT,
        /**
         * @see #ON_RAMP_SLIGHT_LEFT
         */
        ON_RAMP_SLIGHT_RIGHT,
        /**
         * Regular turn (45-135 degrees) to enter a turnpike or freeway.
         */
        ON_RAMP_NORMAL_LEFT,
        /**
         * @see #ON_RAMP_NORMAL_LEFT
         */
        ON_RAMP_NORMAL_RIGHT,
        /**
         * Sharp turn (135-175 degrees) to enter a turnpike or freeway.
         */
        ON_RAMP_SHARP_LEFT,
        /**
         * @see #ON_RAMP_SHARP_LEFT
         */
        ON_RAMP_SHARP_RIGHT,
        /**
         * A turn onto the opposite side of the same street (175-180 degrees) to enter a turnpike or
         * freeway.
         */
        ON_RAMP_U_TURN_LEFT,
        /**
         * @see #ON_RAMP_U_TURN_LEFT
         */
        ON_RAMP_U_TURN_RIGHT,
        /**
         * Slight turn (10-45 degrees) to exit a turnpike or freeway.
         */
        OFF_RAMP_SLIGHT_LEFT,
        /**
         * @see #OFF_RAMP_SLIGHT_LEFT
         */
        OFF_RAMP_SLIGHT_RIGHT,
        /**
         * Normal turn (45-135 degrees) to exit a turnpike or freeway.
         */
        OFF_RAMP_NORMAL_LEFT,
        /**
         * @see #OFF_RAMP_NORMAL_LEFT
         */
        OFF_RAMP_NORMAL_RIGHT,
        /**
         * Road diverges (e.g. "Keep left at the fork").
         */
        FORK_LEFT,
        /**
         * @see #FORK_LEFT
         */
        FORK_RIGHT,
        /**
         * Current road joins another (e.g. "Merge left onto Main St.").
         */
        MERGE_LEFT,
        /**
         * @see #MERGE_LEFT
         */
        MERGE_RIGHT,
        /**
         * Roundabout entrance on which the current road ends (e.g. "Enter the roundabout").
         */
        ROUNDABOUT_ENTER,
        /**
         * Used when leaving a roundabout when the step starts in it (e.g. "Exit the roundabout").
         */
        ROUNDABOUT_EXIT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at a sharp angle to the right (135-175 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_SHARP_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at a normal angle to the right (45-135 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_NORMAL_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at slight angle to the right (10-45 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_SLIGHT_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is straight ahead (0-10 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_STRAIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at sharp angle towards the left (135-175 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_SHARP_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at normal angle towards the left (45-135 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_NORMAL_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is at slight angle towards the left (10-45 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_SLIGHT_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a clockwise roundabout
         * (as seen from above) where the exit is on the opposite side of the road (175-180
         * degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CW_U_TURN,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at sharp angle to the right
         * (135-175 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_SHARP_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at normal angle to the right
         * (45-135 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_NORMAL_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at slight angle towards the right
         * (10-45 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_SLIGHT_RIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is straight ahead (0-10 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_STRAIGHT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at sharp angle towards the left
         * (135-175 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_SHARP_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at normal angle towards the left
         * (45-135 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_NORMAL_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is at slight angle towards the left
         * (10-45 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_SLIGHT_LEFT,
        /**
         * Entrance and exit (e.g. "At the roundabout, take Nth exit") on a counter-clockwise
         * roundabout (as seen from above) where the exit is on the opposite side of the road
         * (175-180 degrees).
         */
        ROUNDABOUT_ENTER_AND_EXIT_CCW_U_TURN,
        /**
         * Driver should steer straight.
         */
        STRAIGHT,
        /**
         * Drive towards a boat ferry for vehicles (e.g. "Take the ferry").
         */
        FERRY_BOAT,
        /**
         * Drive towards a train ferry for vehicles (e.g. "Take the train").
         */
        FERRY_TRAIN,
        /**
         * Arrival at a destination.
         */
        DESTINATION,
        /**
         * Arrival at a destination located straight ahead.
         */
        DESTINATION_STRAIGHT,
        /**
         * Arrival at a destination located on the right side of the road.
         */
        DESTINATION_LEFT,
        /**
         * @see #DESTINATION_LEFT
         */
        DESTINATION_RIGHT,
    }

    @ParcelField(1)
    EnumWrapper<Type> mType;
    @ParcelField(2)
    int mRoundaboutExitNumber;
    @ParcelField(3)
    ImageReference mIcon;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Maneuver() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Maneuver(@NonNull EnumWrapper<Type> type, int roundaboutExitNumber,
            @Nullable ImageReference icon) {
        mType = type;
        mRoundaboutExitNumber = roundaboutExitNumber;
        mIcon = icon;
    }

    /**
     * Builder for creating a {@link Maneuver}
     */
    public static final class Builder {
        private EnumWrapper<Type> mType;
        private int mRoundaboutExitNumber;
        private ImageReference mIcon;


        /**
         * Sets the {@link Type} of maneuver, and any fallback values that could be used by the
         * consumer if the type is unknown to it.
         *
         * @param type Main maneuver type
         * @param fallbackTypes Variations of {@code type}, in case the consumer of this API doesn't
         *                      know the main one (used for backward compatibility). For example,
         *                      if the main type is {@link Type#OFF_RAMP_NORMAL_LEFT}, a fallback
         *                      type could be {@link Type#TURN_NORMAL_LEFT}.
         */
        @NonNull
        public Builder setType(@NonNull Type type, @NonNull Type ... fallbackTypes) {
            mType = EnumWrapper.of(type, fallbackTypes);
            return this;
        }

        /**
         * Sets the roundabout exit number, starting from 1 to designate the first exit after
         * joining
         * the roundabout, and increasing in circulation order. Only relevant if
         * {@link #getType()} is
         * {@link Type#ROUNDABOUT_EXIT} or any variation of ROUNDABOUT_ENTER_AND_EXIT.
         *
         * @return this object for chaining
         * @see #getRoundaboutExitNumber() for more details.
         */
        @NonNull
        public Builder setRoundaboutExitNumber(int roundaboutExitNumber) {
            mRoundaboutExitNumber = roundaboutExitNumber;
            return this;
        }

        /**
         * Sets a reference to an image presenting this maneuver. The provided image must be
         * optimized to be presented in a square canvas (aspect ratio of 1:1).
         */
        @NonNull
        public Builder setIcon(@Nullable ImageReference icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Returns a {@link Maneuver} built with the provided information.
         */
        @NonNull
        public Maneuver build() {
            return new Maneuver(mType, mRoundaboutExitNumber, mIcon);
        }
    }

    /**
     * Returns the maneuver type.
     */
    @NonNull
    public Type getType() {
        return EnumWrapper.getValue(mType, Type.UNKNOWN);
    }

    /**
     * Returns the roundabout exit number, starting from 1 to designate the first exit after joining
     * the roundabout, and increasing in circulation order. Only relevant if {@link #getType()} is
     * {@link Type#ROUNDABOUT_EXIT} or any variation of ROUNDABOUT_ENTER_AND_EXIT.
     * <p>
     * For example, if the driver is joining a counter-clockwise roundabout with 4 exits, then the
     * exit to the right would be exit #1, the one straight ahead would be exit #2, the one to the
     * left would be exit #3 and the one used by the driver to join the roundabout would be exit #4.
     */
    public int getRoundaboutExitNumber() {
        return mRoundaboutExitNumber;
    }

    /**
     * Returns a reference to an image representing this maneuver, or null if image representation
     * is not available. This image is optimized to be displayed in a square canvas (aspect ratio of
     * 1:1).
     */
    @Nullable
    public ImageReference getIcon() {
        return mIcon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Maneuver maneuver = (Maneuver) o;
        return getRoundaboutExitNumber() == maneuver.getRoundaboutExitNumber()
                && Objects.equals(getType(), maneuver.getType())
                && Objects.equals(getIcon(), maneuver.getIcon());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getRoundaboutExitNumber(), getIcon());
    }

    // DefaultLocale suppressed as this method is only offered for debugging purposes.
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{type: %s, roundaboutExitNumer: %d, icon: %s}", mType,
                mRoundaboutExitNumber, mIcon);
    }
}
