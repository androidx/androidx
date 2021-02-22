/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.location;

import static android.os.Build.VERSION_CODES;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * GnssStatus representation that works across all Android versions. See {@link GnssStatus} and
 * {@link GpsStatus}.
 *
 * <p>Note: When used to wrap {@link GpsStatus}, the best performance can be obtained by using a
 * monotonically increasing {@code satelliteIndex} parameter (for instance, by using a loop from
 * 0 to {@link #getSatelliteCount()}). Random access is supported but performance may suffer.
 */
public abstract class GnssStatusCompat {

    // NOTE: CONSTELLATION_* values are copied from GnssStatus.java - any updates there should
    // also be copied to this file.

    /** Unknown constellation type. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_UNKNOWN = GnssStatus.CONSTELLATION_UNKNOWN;
    /** Constellation type constant for GPS. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_GPS = GnssStatus.CONSTELLATION_GPS;
    /** Constellation type constant for SBAS. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_SBAS = GnssStatus.CONSTELLATION_SBAS;
    /** Constellation type constant for Glonass. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_GLONASS = GnssStatus.CONSTELLATION_GLONASS;
    /** Constellation type constant for QZSS. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_QZSS = GnssStatus.CONSTELLATION_QZSS;
    /** Constellation type constant for Beidou. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_BEIDOU = GnssStatus.CONSTELLATION_BEIDOU;
    /** Constellation type constant for Galileo. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_GALILEO = GnssStatus.CONSTELLATION_GALILEO;
    /** Constellation type constant for IRNSS. */
    @SuppressLint("InlinedApi")
    public static final int CONSTELLATION_IRNSS = GnssStatus.CONSTELLATION_IRNSS;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONSTELLATION_UNKNOWN, CONSTELLATION_GPS, CONSTELLATION_SBAS, CONSTELLATION_GLONASS,
            CONSTELLATION_QZSS, CONSTELLATION_BEIDOU, CONSTELLATION_GALILEO, CONSTELLATION_IRNSS})
    public @interface ConstellationType {}

    /**
     * See {@link GnssStatus.Callback}.
     */
    public abstract static class Callback {

        /**
         * See {@link GnssStatus.Callback#onStarted()}.
         */
        public void onStarted() {}

        /**
         * See {@link GnssStatus.Callback#onStopped()}.
         */
        public void onStopped() {}

        /**
         * See {@link GnssStatus.Callback#onFirstFix(int)}.
         */
        public void onFirstFix(@IntRange(from = 0) int ttffMillis) {}

        /**
         * See {@link GnssStatus.Callback#onSatelliteStatusChanged(GnssStatus)}.
         */
        public void onSatelliteStatusChanged(@NonNull GnssStatusCompat status) {}
    }

    /**
     * Wraps the given {@link GnssStatus} as GnssStatusCompat.
     */
    @RequiresApi(VERSION_CODES.N)
    @NonNull
    public static GnssStatusCompat wrap(@NonNull GnssStatus gnssStatus) {
        return new GnssStatusWrapper(gnssStatus);
    }

    /**
     * Wraps the given {@link GpsStatus} as GnssStatusCompat.
     */
    @SuppressLint("ReferencesDeprecated")
    @NonNull
    public static GnssStatusCompat wrap(@NonNull GpsStatus gpsStatus) {
        return new GpsStatusWrapper(gpsStatus);
    }

    // package private to prevent subclassing by clients
    GnssStatusCompat() {}

    /**
     * See {@link GnssStatus#getSatelliteCount()} and {@link GpsStatus#getMaxSatellites()}.
     */
    @IntRange(from = 0)
    public abstract int getSatelliteCount();

    /**
     * See {@link GnssStatus#getConstellationType(int)}. Will always return a value for the GPS
     * constellation below Android N.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @ConstellationType
    public abstract int getConstellationType(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getSvid(int)} and {@link GpsSatellite#getPrn()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @IntRange(from = 1, to = 200)
    public abstract int getSvid(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getCn0DbHz(int)} and {@link GpsSatellite#getSnr()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @FloatRange(from = 0, to = 63)
    public abstract float getCn0DbHz(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getElevationDegrees(int)} and {@link GpsSatellite#getElevation()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @FloatRange(from = -90, to = 90)
    public abstract float getElevationDegrees(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getAzimuthDegrees(int)} and {@link GpsSatellite#getAzimuth()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @FloatRange(from = 0, to = 360)
    public abstract float getAzimuthDegrees(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#hasEphemerisData(int)} and {@link GpsSatellite#hasEphemeris()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    public abstract boolean hasEphemerisData(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#hasAlmanacData(int)} and {@link GpsSatellite#hasAlmanac()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    public abstract boolean hasAlmanacData(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#usedInFix(int)} and {@link GpsSatellite#usedInFix()}.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    public abstract boolean usedInFix(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#hasCarrierFrequencyHz(int)}. This will always return false prior to
     * Android O.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    public abstract boolean hasCarrierFrequencyHz(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getCarrierFrequencyHz(int)}. Behavior is undefined if
     * {@link #hasCarrierFrequencyHz(int)} returns false.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @FloatRange(from = 0)
    public abstract float getCarrierFrequencyHz(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#hasBasebandCn0DbHz(int)}. This will always return false prior to
     * Android R.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    public abstract boolean hasBasebandCn0DbHz(@IntRange(from = 0) int satelliteIndex);

    /**
     * See {@link GnssStatus#getCarrierFrequencyHz(int)}. Behavior is undefined if
     * {@link #hasCarrierFrequencyHz(int)} returns false.
     *
     * @param satelliteIndex A index from zero to {@link #getSatelliteCount()} - 1
     */
    @FloatRange(from = 0, to = 63)
    public abstract float getBasebandCn0DbHz(@IntRange(from = 0) int satelliteIndex);
}
