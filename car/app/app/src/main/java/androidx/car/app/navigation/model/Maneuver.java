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

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/** Information about a maneuver that the driver will be required to perform. */
// TODO(b/154671667): Update when host(s) updates or a scheme for auto sync is established.
@CarProtocol
public final class Maneuver {
    /**
     * Possible maneuver types.
     *
     * @hide
     */
    @IntDef({
            TYPE_UNKNOWN,
            TYPE_DEPART,
            TYPE_NAME_CHANGE,
            TYPE_KEEP_LEFT,
            TYPE_KEEP_RIGHT,
            TYPE_TURN_SLIGHT_LEFT,
            TYPE_TURN_SLIGHT_RIGHT,
            TYPE_TURN_NORMAL_LEFT,
            TYPE_TURN_NORMAL_RIGHT,
            TYPE_TURN_SHARP_LEFT,
            TYPE_TURN_SHARP_RIGHT,
            TYPE_U_TURN_LEFT,
            TYPE_U_TURN_RIGHT,
            TYPE_ON_RAMP_SLIGHT_LEFT,
            TYPE_ON_RAMP_SLIGHT_RIGHT,
            TYPE_ON_RAMP_NORMAL_LEFT,
            TYPE_ON_RAMP_NORMAL_RIGHT,
            TYPE_ON_RAMP_SHARP_LEFT,
            TYPE_ON_RAMP_SHARP_RIGHT,
            TYPE_ON_RAMP_U_TURN_LEFT,
            TYPE_ON_RAMP_U_TURN_RIGHT,
            TYPE_OFF_RAMP_SLIGHT_LEFT,
            TYPE_OFF_RAMP_SLIGHT_RIGHT,
            TYPE_OFF_RAMP_NORMAL_LEFT,
            TYPE_OFF_RAMP_NORMAL_RIGHT,
            TYPE_FORK_LEFT,
            TYPE_FORK_RIGHT,
            TYPE_MERGE_LEFT,
            TYPE_MERGE_RIGHT,
            TYPE_MERGE_SIDE_UNSPECIFIED,
            TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
            TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE,
            TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
            TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE,
            TYPE_STRAIGHT,
            TYPE_FERRY_BOAT,
            TYPE_FERRY_TRAIN,
            TYPE_DESTINATION,
            TYPE_DESTINATION_STRAIGHT,
            TYPE_DESTINATION_LEFT,
            TYPE_DESTINATION_RIGHT,
            TYPE_ROUNDABOUT_ENTER_CW,
            TYPE_ROUNDABOUT_EXIT_CW,
            TYPE_ROUNDABOUT_ENTER_CCW,
            TYPE_ROUNDABOUT_EXIT_CCW,
            TYPE_FERRY_BOAT_LEFT,
            TYPE_FERRY_BOAT_RIGHT,
            TYPE_FERRY_TRAIN_LEFT,
            TYPE_FERRY_TRAIN_RIGHT,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Type {
    }

    /**
     * Maneuver type is unknown, no maneuver information should be displayed.
     *
     * <p>This type may be interpreted differently depending on the consumer. In some
     * cases the previous maneuver will continue to be shown while in others no maneuver will be
     * shown at all.
     */
    @Type
    public static final int TYPE_UNKNOWN = 0;

    /**
     * Starting point of the navigation.
     *
     * <p>For example, "Start driving on Main St."
     */
    @Type
    public static final int TYPE_DEPART = 1;

    /**
     * No turn, but the street name changes.
     *
     * <p>For example, "Continue on Main St."
     */
    @Type
    public static final int TYPE_NAME_CHANGE = 2;

    /**
     * No turn, from 0 (included) to 10 (excluded) degrees.
     *
     * <p>This is used in contrast to {@link #TYPE_STRAIGHT} for disambiguating cases where there is
     * more than one option to go into the same general direction.
     */
    @Type
    public static final int TYPE_KEEP_LEFT = 3;

    /**
     * No turn, from 0 (included) to 10 (excluded) degrees.
     *
     * <p>This is used in contrast to {@link #TYPE_STRAIGHT} for disambiguating cases where there is
     * more than one option to go into the same general direction.
     */
    @Type
    public static final int TYPE_KEEP_RIGHT = 4;

    /** Slight left turn at an intersection, from 10 (included) to 45 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_SLIGHT_LEFT = 5;

    /** Slight right turn at an intersection, from 10 (included) to 45 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_SLIGHT_RIGHT = 6;

    /** Regular left turn at an intersection, from 45 (included) to 135 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_NORMAL_LEFT = 7;

    /** Regular right turn at an intersection, from 45 (included) to 135 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_NORMAL_RIGHT = 8;

    /** Sharp left turn at an intersection, from 135 (included) to 175 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_SHARP_LEFT = 9;

    /** Sharp right turn at an intersection, from 135 (included) to 175 (excluded) degrees. */
    @Type
    public static final int TYPE_TURN_SHARP_RIGHT = 10;

    /**
     * Left turn onto the opposite side of the same street, from 175 (included) to 180 (included)
     * degrees.
     */
    @Type
    public static final int TYPE_U_TURN_LEFT = 11;

    /**
     * A right turn onto the opposite side of the same street, from 175 (included) to 180 (included)
     * degrees.
     */
    @Type
    public static final int TYPE_U_TURN_RIGHT = 12;

    /**
     * Slight left turn to enter a turnpike or freeway, from 10 (included) to 45 (excluded) degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_SLIGHT_LEFT = 13;

    /**
     * Slight right turn to enter a turnpike or freeway, from 10 (included) to 45 (excluded)
     * degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_SLIGHT_RIGHT = 14;

    /**
     * Regular left turn to enter a turnpike or freeway, from 45 (included) to 135 (excluded)
     * degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_NORMAL_LEFT = 15;

    /**
     * Regular right turn to enter a turnpike or freeway, from 45 (included) to 135 (excluded)
     * degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_NORMAL_RIGHT = 16;

    /**
     * Sharp left turn to enter a turnpike or freeway, from 135 (included) to 175 (excluded)
     * degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_SHARP_LEFT = 17;

    /**
     * Sharp right turn to enter a turnpike or freeway, from 135 (included) to 175 (excluded)
     * degrees.
     */
    @Type
    public static final int TYPE_ON_RAMP_SHARP_RIGHT = 18;

    /**
     * Left turn onto the opposite side of the same street to enter a turnpike or freeway, from 175
     * (included) to 180 (included).
     */
    @Type
    public static final int TYPE_ON_RAMP_U_TURN_LEFT = 19;

    /**
     * Right turn onto the opposite side of the same street to enter a turnpike or freeway, from 175
     * (included) to 180 (included).
     */
    @Type
    public static final int TYPE_ON_RAMP_U_TURN_RIGHT = 20;

    /** A left turn to exit a turnpike or freeway, from 10 (included) to 45 (excluded) degrees. */
    @Type
    public static final int TYPE_OFF_RAMP_SLIGHT_LEFT = 21;

    /** A right turn to exit a turnpike or freeway, from 10 (included) to 45 (excluded) degrees. */
    @Type
    public static final int TYPE_OFF_RAMP_SLIGHT_RIGHT = 22;

    /** A left turn to exit a turnpike or freeway, from 45 (included) to 135 (excluded) degrees. */
    @Type
    public static final int TYPE_OFF_RAMP_NORMAL_LEFT = 23;

    /** A left right to exit a turnpike or freeway, from 45 (included) to 135 (excluded) degrees. */
    @Type
    public static final int TYPE_OFF_RAMP_NORMAL_RIGHT = 24;

    /**
     * Keep to the left as the road diverges.
     *
     * <p>For example, this is used to indicate "Keep left at the fork".
     */
    @Type
    public static final int TYPE_FORK_LEFT = 25;

    /**
     * Keep to the right as the road diverges.
     *
     * <p>For example, this is used to indicate "Keep right at the fork".
     */
    @Type
    public static final int TYPE_FORK_RIGHT = 26;

    /**
     * Current road joins another on the left.
     *
     * <p>For example, this is used to indicate "Merge left onto Main St.".
     */
    @Type
    public static final int TYPE_MERGE_LEFT = 27;

    /**
     * Current road joins another on the right.
     *
     * <p>For example, this is used to indicate "Merge left onto Main St.".
     */
    @Type
    public static final int TYPE_MERGE_RIGHT = 28;

    /**
     * Current road joins another without direction specified.
     *
     * <p>For example, this is used to indicate "Merge onto Main St.".
     */
    @Type
    public static final int TYPE_MERGE_SIDE_UNSPECIFIED = 29;

    /**
     * Enter a clockwise roundabout and take the Nth exit.
     *
     * <p>The exit number must be passed when created the maneuver.
     *
     * <p>For example, this is used to indicate "At the roundabout, take the Nth exit".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW = 32;

    /**
     * Enter a clockwise roundabout and take the Nth exit after angle A degrees.
     *
     * <p>The exit number and angle must be passed when creating the maneuver.
     *
     * <p>For example, this is used to indicate "At the roundabout, take the Nth exit".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE = 33;

    /**
     * Enter a counter-clockwise roundabout and take the Nth exit.
     *
     * <p>The exit number must be passed when created the maneuver.
     *
     * <p>For example, this is used to indicate "At the roundabout, take the Nth exit".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW = 34;

    /**
     * Enter a counter-clockwise roundabout and take the Nth exit after angle A degrees.
     *
     * <p>The exit number and angle must be passed when creating the maneuver.
     *
     * <p>For example, this is used to indicate "At the roundabout, take a sharp right at the Nth
     * exit".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE = 35;

    /** Driver should steer straight. */
    @Type
    public static final int TYPE_STRAIGHT = 36;

    /**
     * Drive towards a boat ferry for vehicles, where the entrance is straight ahead or in an
     * unknown direction.
     *
     * <p>For example, this is used to indicate "Take the ferry".
     */
    @Type
    public static final int TYPE_FERRY_BOAT = 37;

    /**
     * Drive towards a train ferry for vehicles (e.g. "Take the train"), where the entrance is
     * straight ahead or in an unknown direction.
     */
    @Type
    public static final int TYPE_FERRY_TRAIN = 38;

    /** Arrival at a destination. */
    @Type
    public static final int TYPE_DESTINATION = 39;

    /** Arrival to a destination located straight ahead. */
    @Type
    public static final int TYPE_DESTINATION_STRAIGHT = 40;

    /** Arrival to a destination located to the left side of the road. */
    @Type
    public static final int TYPE_DESTINATION_LEFT = 41;

    /** Arrival to a destination located to the right side of the road. */
    @Type
    public static final int TYPE_DESTINATION_RIGHT = 42;

    /**
     * Entrance to a clockwise roundabout on which the current road ends.
     *
     * <p>For example, this is used to indicate "Enter the roundabout".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_CW = 43;

    /**
     * Used when leaving a clockwise roundabout when the step starts in it.
     *
     * <p>For example, this is used to indicate "Exit the roundabout".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_EXIT_CW = 44;

    /**
     * Entrance to a counter-clockwise roundabout on which the current road ends.
     *
     * <p>For example, this is used to indicate "Enter the roundabout".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_ENTER_CCW = 45;

    /**
     * Used when leaving a counter-clockwise roundabout when the step starts in it.
     *
     * <p>For example, this is used to indicate "Exit the roundabout".
     */
    @Type
    public static final int TYPE_ROUNDABOUT_EXIT_CCW = 46;

    /**
     * Drive towards a boat ferry for vehicles, where the entrance is to the left.
     *
     * <p>For example, this is used to indicate "Take the ferry".
     */
    @Type
    public static final int TYPE_FERRY_BOAT_LEFT = 47;

    /**
     * Drive towards a boat ferry for vehicles, where the entrance is to the right.
     *
     * <p>For example, this is used to indicate "Take the ferry".
     */
    @Type
    public static final int TYPE_FERRY_BOAT_RIGHT = 48;

    /**
     * Drive towards a train ferry for vehicles (e.g. "Take the train"), where the entrance is to
     * the
     * left.
     */
    @Type
    public static final int TYPE_FERRY_TRAIN_LEFT = 49;

    /**
     * Drive towards a train ferry for vehicles (e.g. "Take the train"), where the entrance is to
     * the
     * right.
     */
    @Type
    public static final int TYPE_FERRY_TRAIN_RIGHT = 50;

    @Keep
    @Type
    private final int mType;
    @Keep
    private final int mRoundaboutExitNumber;
    @Keep
    private final int mRoundaboutExitAngle;
    @Keep
    @Nullable
    private final CarIcon mIcon;

    /**
     * Returns the maneuver type.
     *
     * <p>Required to be set at all times.
     */
    @Type
    public int getType() {
        return mType;
    }

    /**
     * Returns the roundabout exit number, starting from 1 to designate the first exit after joining
     * the roundabout, and increasing in circulation order. Only relevant if the type is any
     * variation of {@code TYPE_ROUNDABOUT_ENTER_AND_EXIT_*}.
     *
     * <p>For example, if the driver is joining a counter-clockwise roundabout with 4 exits, then
     * the exit to the right would be exit #1, the one straight ahead would be exit #2, the one
     * to the left would be exit #3 and the one used by the driver to join the roundabout would
     * be exit #4.
     *
     * <p>Required when the type is a roundabout.
     */
    public int getRoundaboutExitNumber() {
        return mRoundaboutExitNumber;
    }

    /**
     * Returns the roundabout exit angle in degrees to designate the amount of distance to travel
     * around the roundabout. Only relevant if the type is {@link
     * #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE} or {@link
     * #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE}.
     *
     * <p>For example, if the drive is joining a counter-clockwise roundabout with equally spaced
     * exits then the exit to the right would be at 45 degrees, the one straight ahead would be
     * at 90 degrees, the one to the left would at 270 degrees and the one used by the driver to
     * join the roundabout would be at 360 degrees.
     *
     * <p>The angle can also be set for irregular roundabouts. For example a roundabout with three
     * exits at 90, 270 and 360 degrees could also have the desired exit angle specified.
     *
     * <p>Required with the type is a roundabout with an angle.
     */
    public int getRoundaboutExitAngle() {
        return mRoundaboutExitAngle;
    }

    /**
     * Returns the icon for the maneuver.
     *
     * <p>Optional field that when not set may be shown in the target display by a generic image
     * representing the specific maneuver.
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    @Override
    @NonNull
    public String toString() {
        return "[type: "
                + mType
                + ", exit #: "
                + mRoundaboutExitNumber
                + ", exit angle: "
                + mRoundaboutExitAngle
                + ", icon: "
                + mIcon
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mRoundaboutExitNumber, mRoundaboutExitAngle, mIcon);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Maneuver)) {
            return false;
        }

        Maneuver otherManeuver = (Maneuver) other;
        return mType == otherManeuver.mType
                && mRoundaboutExitNumber == otherManeuver.mRoundaboutExitNumber
                && mRoundaboutExitAngle == otherManeuver.mRoundaboutExitAngle
                && Objects.equals(mIcon, otherManeuver.mIcon);
    }

    Maneuver(@Type int type, int roundaboutExitNumber, int roundaboutExitAngle,
            @Nullable CarIcon icon) {
        mType = type;
        mRoundaboutExitNumber = roundaboutExitNumber;
        mRoundaboutExitAngle = roundaboutExitAngle;
        CarIconConstraints.DEFAULT.validateOrThrow(icon);
        mIcon = icon;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Maneuver() {
        mType = TYPE_UNKNOWN;
        mRoundaboutExitNumber = 0;
        mRoundaboutExitAngle = 0;
        mIcon = null;
    }

    static boolean isValidType(@Type int type) {
        return (type >= TYPE_UNKNOWN && type <= TYPE_FERRY_TRAIN_RIGHT);
    }

    static boolean isValidTypeWithExitNumber(@Type int type) {
        return (type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW
                || type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
                || type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE
                || type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE);
    }

    static boolean isValidTypeWithExitAngle(@Type int type) {
        return (type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE
                || type == TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE);
    }

    /** A builder of {@link Maneuver}. */
    public static final class Builder {
        @Type
        private final int mType;
        private boolean mIsRoundaboutExitNumberSet;
        private int mRoundaboutExitNumber;
        private boolean mIsRoundaboutExitAngleSet;
        private int mRoundaboutExitAngle;
        @Nullable
        private CarIcon mIcon;

        /**
         * Constructs a new instance of a {@link Builder}.
         *
         * <p>The type should be chosen to reflect the closest semantic meaning of the maneuver.
         * In some cases, an exact type match is not possible, but choosing a similar or slightly
         * more general type is preferred. Using {@link #TYPE_UNKNOWN} is allowed, but some head
         * units will not display any information in that case.
         *
         * @param type one of the {@code TYPE_*} static constants defined in this class
         * @throws IllegalArgumentException if {@code type} is not a valid maneuver type
         */
        public Builder(@Type int type) {
            if (!isValidType(type)) {
                throw new IllegalArgumentException("Maneuver must have a valid type");
            }
            mType = type;
        }

        /**
         * Sets an image representing the maneuver.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * icons targeting a 128 x 128 dp bounding box. If the icon exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        @NonNull
        public Builder setIcon(@NonNull CarIcon icon) {
            mIcon = requireNonNull(icon);
            return this;
        }

        /**
         * Sets an exit number for roundabout maneuvers.
         *
         * <p>Use for when {@code type} is {@link #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW}, {@link
         * #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW},
         * {@link #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE} or
         * {@link #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE}. The {@code
         * roundaboutExitNumber} starts from 1 to designate the first exit after joining the
         * roundabout, and increases in circulation order.
         *
         * <p>For example, if the driver is joining a counter-clockwise roundabout with 4 exits,
         * then the exit to the right would be exit #1, the one straight ahead would be exit #2,
         * the one to the left would be exit #3 and the one used by the driver to join the
         * roundabout would be exit #4.
         *
         * @throws IllegalArgumentException if {@code type} does not include a exit number, or
         *                                  if {@code roundaboutExitNumber} is not greater than
         *                                  zero
         */
        @NonNull
        public Builder setRoundaboutExitNumber(@IntRange(from = 1) int roundaboutExitNumber) {
            if (!isValidTypeWithExitNumber(mType)) {
                throw new IllegalArgumentException(
                        "Maneuver does not include roundaboutExitNumber");
            }
            if (roundaboutExitNumber < 1) {
                throw new IllegalArgumentException("Maneuver must include a valid exit number");
            }
            mIsRoundaboutExitNumberSet = true;
            mRoundaboutExitNumber = roundaboutExitNumber;
            return this;
        }

        /**
         * Sets an exit angle for roundabout maneuvers.
         *
         * <p>Use for when {@code type} is {@link #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE} or
         * {@link #TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE}. The {@code roundaboutExitAngle}
         * represents the degrees traveled in circulation from the entrance to the exit.
         *
         * <p>For example, in a 4 exit example, if all the exits are equally spaced then exit 1
         * would be at 90 degrees, exit 2 at 180, exit 3 at 270 and exit 4 at 360. However if the
         * exits are irregular then a different angle could be provided.
         *
         * @throws IllegalArgumentException if {@code type} does not include a exit angle or if
         *                                  {@code roundaboutExitAngle} is not greater than zero
         *                                  and less than or equal to 360 degrees
         */
        @NonNull
        public Builder setRoundaboutExitAngle(
                @IntRange(from = 1, to = 360) int roundaboutExitAngle) {
            if (!isValidTypeWithExitAngle(mType)) {
                throw new IllegalArgumentException("Maneuver does not include roundaboutExitAngle");
            }
            if (roundaboutExitAngle < 1 || roundaboutExitAngle > 360) {
                throw new IllegalArgumentException("Maneuver must include a valid exit angle");
            }
            mIsRoundaboutExitAngleSet = true;
            mRoundaboutExitAngle = roundaboutExitAngle;
            return this;
        }

        /**
         * Constructs the {@link Maneuver} defined by this builder.
         *
         * @throws IllegalArgumentException if {@code type} includes an exit number and one has
         *                                  not been set, or if it includes an exit angle and one
         *                                  has not been set
         */
        @NonNull
        public Maneuver build() {
            if (isValidTypeWithExitNumber(mType) && !mIsRoundaboutExitNumberSet) {
                throw new IllegalArgumentException("Maneuver missing roundaboutExitNumber");
            }
            if (isValidTypeWithExitAngle(mType) && !mIsRoundaboutExitAngleSet) {
                throw new IllegalArgumentException("Maneuver missing roundaboutExitAngle");
            }
            return new Maneuver(mType, mRoundaboutExitNumber, mRoundaboutExitAngle, mIcon);
        }
    }
}
