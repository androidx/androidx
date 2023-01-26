/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.location.altitude;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;

/**
 * Converts altitudes reported above the World Geodetic System 1984 (WGS84) reference ellipsoid
 * into ones above Mean Sea Level.
 *
 * @hide
 */
public final class AltitudeConverterCompat {

    private static final double MAX_ABS_VALID_LATITUDE = 90;
    private static final double MAX_ABS_VALID_LONGITUDE = 180;

    /** Prevents instantiation. */
    private AltitudeConverterCompat() {
    }

    /**
     * Returns a {@link ListenableFuture} that, upon success, adds a Mean Sea Level altitude to the
     * {@code location} in {@link Location#getExtras()}. In addition, adds a Mean Sea Level altitude
     * accuracy if the {@code location} has a valid vertical accuracy.
     *
     * <p>The {@link ListenableFuture} leaves the {@code location} unchanged if and only if any
     * of the following are true:
     *
     * <ul>
     *   <li>the {@code location} has an invalid latitude, longitude, or altitude above WGS84
     *   <li>an I/O error occurs when loading data from raw assets via {@code context}.
     * </ul>
     *
     * <p>NOTE: Currently throws {@link UnsupportedOperationException} for a valid {@code location}.
     *
     * @hide
     */
    @NonNull
    public static ListenableFuture<Location> addMslAltitudeAsync(
            @NonNull Context context,
            @NonNull Location location) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(location);
        validate(location);
        throw new UnsupportedOperationException("addMslAltitude method not implemented.");
    }

    /**
     * Throws an {@link IllegalArgumentException} if the {@code location} has an invalid latitude,
     * longitude, or altitude above WGS84.
     */
    private static void validate(@NonNull Location location) {
        Preconditions.checkArgument(isFiniteAndAtAbsMost(location.getLatitude(),
                MAX_ABS_VALID_LATITUDE), "Location must contain a valid latitude.");
        Preconditions.checkArgument(isFiniteAndAtAbsMost(location.getLongitude(),
                MAX_ABS_VALID_LONGITUDE), "Location must contain a valid longitude.");
        Preconditions.checkArgument(location.hasAltitude() && isFinite(location.getAltitude()),
                "Location must contain a valid altitude above WGS84.");
    }

    private static boolean isFiniteAndAtAbsMost(double value, double rhs) {
        return isFinite(value) && Math.abs(value) <= rhs;
    }

    private static boolean isFinite(double value) {
        return !Double.isInfinite(value) && !Double.isNaN(value);
    }
}
