/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.car.app.hardware.info;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Information about the vehicle's exterior dimensions reported in millimeters. */
@CarProtocol
@RequiresCarApi(7)
@KeepFields
public class ExteriorDimensions {
    /** The index for vehicle height in millimeters. */
    public static final int HEIGHT_INDEX = 0;
    /** The index for vehicle length in millimeters. */
    public static final int LENGTH_INDEX = 1;
    /** The index for vehicle width in millimeters. */
    public static final int WIDTH_INDEX = 2;
    /** The index for vehicle width including mirrors in millimeters. */
    public static final int WIDTH_INCLUDING_MIRRORS_INDEX = 3;
    /** The index for vehicle wheel base in millimeters. */
    public static final int WHEEL_BASE_INDEX = 4;
    /** The index for vehicle front track width in millimeters. */
    public static final int TRACK_WIDTH_FRONT_INDEX = 5;
    /** The index for vehicle rear track width in millimeters. */
    public static final int TRACK_WIDTH_REAR_INDEX = 6;
    /** The index for vehicle curb to curb turning radius in millimeters. */
    public static final int CURB_TO_CURB_TURNING_RADIUS_INDEX = 7;

    private final @NonNull CarValue<Integer[]> mExteriorDimensions;

    /** Creates a default ExteriorDimensions instance that report "unknown" as the value. */
    public ExteriorDimensions() {
        mExteriorDimensions = CarValue.UNKNOWN_INTEGER_ARRAY;
    }

    /** Creates an ExteriorDimensions instance with the given car value. */
    public ExteriorDimensions(@NonNull CarValue<Integer[]> exteriorDimensions) {
        mExteriorDimensions = exteriorDimensions;
    }

    /**
     * Returns the vehicle's exterior dimensions in millimeters. This information is reported
     * as-is from the manufacturer and is keyed off of the indexes defined in this class, as
     * documented in
     * <a href="https://developer.android.com/reference/android/car/VehiclePropertyIds#INFO_EXTERIOR_DIMENSIONS">VehiclePropertyIds#INFO_EXTERIOR_DIMENSIONS</a>.
     */
    public @NonNull CarValue<Integer[]> getExteriorDimensions() {
        return mExteriorDimensions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mExteriorDimensions);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExteriorDimensions)) {
            return false;
        }
        ExteriorDimensions otherDim = (ExteriorDimensions) other;

        return mExteriorDimensions.equals(otherDim.mExteriorDimensions);
    }

    @Override
    public @NonNull String toString() {
        return "[ exteriorDimensions: " + mExteriorDimensions + " ]";
    }
}
