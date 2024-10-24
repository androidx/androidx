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

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Objects;

/** Represents a distance value and how it should be displayed in the UI. */
@CarProtocol
@KeepFields
public final class Distance {
    /**
     * Possible units used to display {@link Distance}
     *
     */
    @IntDef({
            UNIT_METERS,
            UNIT_KILOMETERS,
            UNIT_MILES,
            UNIT_FEET,
            UNIT_YARDS,
            UNIT_KILOMETERS_P1,
            UNIT_MILES_P1
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Unit {
    }

    /** Meter unit. */
    @Unit
    public static final int UNIT_METERS = 1;

    /** Kilometer unit. */
    @Unit
    public static final int UNIT_KILOMETERS = 2;

    /**
     * Kilometer unit with the additional requirement that distances of this type be displayed
     * with at least 1 digit of precision after the decimal point (for example, 2.0).
     */
    @Unit
    public static final int UNIT_KILOMETERS_P1 = 3;

    /** Miles unit. */
    @Unit
    public static final int UNIT_MILES = 4;

    /**
     * Mile unit with the additional requirement that distances of this type be displayed with at
     * least 1 digit of precision after the decimal point (for example, 2.0).
     */
    @Unit
    public static final int UNIT_MILES_P1 = 5;

    /** Feet unit. */
    @Unit
    public static final int UNIT_FEET = 6;

    /** Yards unit. */
    @Unit
    public static final int UNIT_YARDS = 7;

    private final double mDisplayDistance;
    @Unit
    private final int mDisplayUnit;

    /**
     * Constructs a new instance of a {@link Distance}.
     *
     * <p>Units with precision requirements, {@link #UNIT_KILOMETERS_P1} and {@link #UNIT_MILES_P1},
     * will always show one decimal digit. All other units will show a decimal digit if needed but
     * will not if the distance is a whole number.
     *
     * <h4>Examples</h4>
     *
     * A display distance of 1.0 with a display unit of {@link #UNIT_KILOMETERS} will display "1
     * km", whereas if the display unit is {@link #UNIT_KILOMETERS_P1} it will display "1.0 km".
     * Note the "km" part of the string in this example depends on the locale the host is
     * configured with.
     *
     * <p>A display distance of 1.46 however will display "1.4 km" for both {@link #UNIT_KILOMETERS}
     * and {@link #UNIT_KILOMETERS} display units.
     *
     * <p>{@link #UNIT_KILOMETERS_P1} and {@link #UNIT_MILES_P1} can be used to provide consistent
     * digit placement for a sequence of distances. For example, as the user is driving and the next
     * turn distance changes, using {@link #UNIT_KILOMETERS_P1} will produce: "2.5 km", "2.0 km",
     * "1.5 km", "1.0 km", and so on.
     *
     * @param displayDistance the distance to display, in the units specified in {@code
     *                        displayUnit}. See {@link #getDisplayDistance()}
     * @param displayUnit     the unit of distance to use when displaying the value in {@code
     *                        displayUnit}. This should be one of the {@code UNIT_*} static
     *                        constants defined in this class. See {@link #getDisplayUnit()}
     * @throws IllegalArgumentException if {@code displayDistance} is negative
     */
    public static @NonNull Distance create(double displayDistance, @Unit int displayUnit) {
        if (displayDistance < 0) {
            throw new IllegalArgumentException("displayDistance must be a positive value");
        }
        return new Distance(displayDistance, displayUnit);
    }

    /**
     * Returns the distance measured in the unit indicated at {@link #getDisplayUnit()}.
     *
     * <p>This distance is for display purposes only and it might be a rounded representation of the
     * actual distance. For example, a distance of 1000 meters could be shown in the following ways:
     *
     * <ul>
     *   <li>Display unit of {@link #UNIT_METERS} and distance of 1000, resulting in a display of
     *       "1000 m".
     *   <li>Display unit of {@link #UNIT_KILOMETERS} and distance of 1, resulting in a
     *       display of "1 km".
     *   <li>Display unit of {@link #UNIT_KILOMETERS_P1} and distance of 1, resulting in a
     *       display of "1.0 km".
     * </ul>
     */
    public double getDisplayDistance() {
        return mDisplayDistance;
    }

    /**
     * Returns the unit that should be used to display the distance value, adjusted to the current
     * user's locale and location. This should match the unit used in {@link #getDisplayDistance()}.
     */
    @Unit
    public int getDisplayUnit() {
        return mDisplayUnit;
    }

    @Override
    public @NonNull String toString() {
        return String.format(Locale.US, "%.04f%s", mDisplayDistance, unitToString(mDisplayUnit));
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDisplayDistance, mDisplayUnit);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Distance)) {
            return false;
        }
        Distance otherDistance = (Distance) other;

        return mDisplayUnit == otherDistance.mDisplayUnit
                && mDisplayDistance == otherDistance.mDisplayDistance;
    }

    private Distance(double displayDistance, @Unit int displayUnit) {
        mDisplayDistance = displayDistance;
        mDisplayUnit = displayUnit;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Distance() {
        mDisplayDistance = 0.0d;
        mDisplayUnit = UNIT_METERS;
    }

    private static String unitToString(@Unit int displayUnit) {
        switch (displayUnit) {
            case UNIT_FEET:
                return "ft";
            case UNIT_KILOMETERS:
                return "km";
            case UNIT_KILOMETERS_P1:
                return "km_p1";
            case UNIT_METERS:
                return "m";
            case UNIT_MILES:
                return "mi";
            case UNIT_MILES_P1:
                return "mi_p1";
            case UNIT_YARDS:
                return "yd";
            default:
                return "?";
        }
    }
}
