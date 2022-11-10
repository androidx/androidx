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
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.core.location.altitude.impl.AltitudeConverter;

import java.io.IOException;

/**
 * Converts altitudes reported above the World Geodetic System 1984 (WGS84) reference ellipsoid
 * into ones above Mean Sea Level.
 *
 * <p>Reference:
 *
 * <pre>
 * Brian Julian and Michael Angermann.
 * "Resource efficient and accurate altitude conversion to Mean Sea Level."
 * 2023 IEEE/ION Position, Location and Navigation Symposium (PLANS).
 * </pre>
 */
public final class AltitudeConverterCompat {

    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    @Nullable
    private static AltitudeConverter sAltitudeConverter;

    /** Prevents instantiation. */
    private AltitudeConverterCompat() {
    }

    /**
     * Adds a Mean Sea Level altitude to the {@code location}. In addition, adds a Mean Sea Level
     * altitude accuracy if the {@code location} has a finite and non-negative vertical accuracy;
     * otherwise, does not add a corresponding accuracy.
     *
     * <p>Must be called off the main thread as data may be loaded from raw assets.
     *
     * @throws IOException              if an I/O error occurs when loading data from raw assets.
     * @throws IllegalArgumentException if the {@code location} has an invalid latitude, longitude,
     *                                  or altitude above WGS84. Specifically, the latitude must be
     *                                  between -90 and 90 (both inclusive), the longitude must be
     *                                  between -180 and 180 (both inclusive), and the altitude
     *                                  above WGS84 must be finite.
     */
    @WorkerThread
    public static void addMslAltitudeToLocation(@NonNull Context context,
            @NonNull Location location) throws IOException {
        if (Build.VERSION.SDK_INT >= 34) {
            Api34Impl.addMslAltitudeToLocation(context, location);
            return;
        }

        AltitudeConverter altitudeConverter;
        synchronized (sLock) {
            if (sAltitudeConverter == null) {
                sAltitudeConverter = new AltitudeConverter();
            }
            altitudeConverter = sAltitudeConverter;
        }
        altitudeConverter.addMslAltitudeToLocation(context, location);
    }

    @RequiresApi(34)
    private static class Api34Impl {

        private static final Object sLock = new Object();

        @GuardedBy("sLock")
        @Nullable
        private static Object sAltitudeConverter;

        /** Prevents instantiation. */
        private Api34Impl() {
        }

        @DoNotInline
        static void addMslAltitudeToLocation(@NonNull Context context,
                @NonNull Location location) throws IOException {
            android.location.altitude.AltitudeConverter altitudeConverter;
            synchronized (sLock) {
                if (sAltitudeConverter == null) {
                    sAltitudeConverter = new android.location.altitude.AltitudeConverter();
                }
                altitudeConverter =
                        (android.location.altitude.AltitudeConverter) sAltitudeConverter;
            }
            altitudeConverter.addMslAltitudeToLocation(context, location);
        }
    }
}
