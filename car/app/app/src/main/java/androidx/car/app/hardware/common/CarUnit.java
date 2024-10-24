/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Units such as speed, distance and volume for car hardware measurements and display. */
@CarProtocol
@RequiresCarApi(3)
public final class CarUnit {
    /**
     * Defines the possible distance units from car hardware.
     *
     */
    @IntDef({
            MILLIMETER,
            METER,
            KILOMETER,
            MILE,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @RestrictTo(LIBRARY)
    public @interface CarDistanceUnit {
    }

    /** Millimeter unit. */
    @CarDistanceUnit
    public static final int MILLIMETER = 1;

    /** Meter unit. */
    @CarDistanceUnit
    public static final int METER = 2;

    /** Kilometer unit. */
    @CarDistanceUnit
    public static final int KILOMETER = 3;

    /** Miles unit. */
    @CarDistanceUnit
    public static final int MILE = 4;

    /**
     * Defines the possible volume units from car hardware.
     *
     */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    @IntDef({
            MILLILITER,
            LITER,
            US_GALLON,
            IMPERIAL_GALLON
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @RestrictTo(LIBRARY)
    public @interface CarVolumeUnit {
    }

    /** Milliliter unit. */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    @CarVolumeUnit
    public static final int MILLILITER = 201;

    /** Liter unit. */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    @CarVolumeUnit
    public static final int LITER = 202;

    /** US Gallon unit. */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    @CarVolumeUnit
    public static final int US_GALLON = 203;

    /** Imperial Gallon unit. */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    @CarVolumeUnit
    public static final int IMPERIAL_GALLON = 204;

    /**
     * Defines the possible distance units from car hardware.
     *
     */
    @IntDef({
            METERS_PER_SEC,
            KILOMETERS_PER_HOUR,
            MILES_PER_HOUR,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @RestrictTo(LIBRARY)
    public @interface CarSpeedUnit {
    }

    /** Meters per second unit. */
    @CarSpeedUnit
    public static final int METERS_PER_SEC = 101;

    /** Kilometers per hour unit. */
    @CarSpeedUnit
    public static final int KILOMETERS_PER_HOUR = 102;

    /** Miles per hour unit. */
    @CarSpeedUnit
    public static final int MILES_PER_HOUR = 103;

    private CarUnit() {}

    /** Get a user friendly representation of the unit. */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static @NonNull String toString(int unit) {
        switch (unit) {
            case MILLIMETER:
                return "MILLIMETER";
            case METER:
                return "METER";
            case KILOMETER:
                return "KILOMETER";
            case MILE:
                return "MILE";
            case MILLILITER:
                return "MILLILITER";
            case LITER:
                return "LITER";
            case US_GALLON :
                return "US_GALLON ";
            case IMPERIAL_GALLON:
                return "IMPERIAL_GALLON";
            case METERS_PER_SEC:
                return "METERS_PER_SEC";
            case KILOMETERS_PER_HOUR:
                return "KILOMETERS_PER_HOUR";
            case MILES_PER_HOUR :
                return "MILES_PER_HOUR ";
            default:
                return "UNKNOWN";
        }
    }
}
