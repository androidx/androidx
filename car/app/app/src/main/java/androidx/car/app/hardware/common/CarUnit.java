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
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

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
     * @hide
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

    @CarDistanceUnit
    /** Kilometer unit. */
    public static final int KILOMETER = 3;

    /** Miles unit. */
    @CarDistanceUnit
    public static final int MILE = 4;

    /**
     * Defines the possible volume units from car hardware.
     *
     * @hide
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
     * @hide
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
}
