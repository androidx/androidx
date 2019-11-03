/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.app;

import android.text.format.DateUtils;

/**
 * Imported from frameworks/base/services/core/java/com/android/server/TwilightCalculator.java
 *
 * <p>Calculates the sunrise and sunsets times for a given location.</p>
 */
class TwilightCalculator {

    private static TwilightCalculator sInstance;

    static TwilightCalculator getInstance() {
        if (sInstance == null) {
            sInstance = new TwilightCalculator();
        }
        return sInstance;
    }

    /** Value of {@link #state} if it is currently day */
    public static final int DAY = 0;

    /** Value of {@link #state} if it is currently night */
    public static final int NIGHT = 1;

    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0f);

    // element for calculating solar transit.
    private static final float J0 = 0.0009f;

    // correction for civil twilight
    @SuppressWarnings("FloatingPointLiteralPrecision")
    private static final float ALTIDUTE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f;

    // coefficients for calculating Equation of Center.
    private static final float C1 = 0.0334196f;
    private static final float C2 = 0.000349066f;
    private static final float C3 = 0.000005236f;

    @SuppressWarnings("FloatingPointLiteralPrecision")
    private static final float OBLIQUITY = 0.40927971f;

    // Java time on Jan 1, 2000 12:00 UTC.
    private static final long UTC_2000 = 946728000000L;

    /**
     * Time of sunset (civil twilight) in milliseconds or -1 in the case the day
     * or night never ends.
     */
    public long sunset;

    /**
     * Time of sunrise (civil twilight) in milliseconds or -1 in the case the
     * day or night never ends.
     */
    public long sunrise;

    /**
     * Current state
     */
    public int state;

    /**
     * calculates the civil twilight bases on time and geo-coordinates.
     *
     * @param time time in milliseconds.
     * @param latitude latitude in degrees.
     * @param longitude latitude in degrees.
     */
    @SuppressWarnings("FloatingPointLiteralPrecision")
    public void calculateTwilight(long time, double latitude, double longitude) {
        final float daysSince2000 = (float) (time - UTC_2000) / DateUtils.DAY_IN_MILLIS;

        // mean anomaly
        final float meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f;

        // true anomaly
        final double trueAnomaly = meanAnomaly + C1 * Math.sin(meanAnomaly) + C2
                * Math.sin(2 * meanAnomaly) + C3 * Math.sin(3 * meanAnomaly);

        // ecliptic longitude
        final double solarLng = trueAnomaly + 1.796593063d + Math.PI;

        // solar transit in days since 2000
        final double arcLongitude = -longitude / 360;
        float n = Math.round(daysSince2000 - J0 - arcLongitude);
        double solarTransitJ2000 = n + J0 + arcLongitude + 0.0053d * Math.sin(meanAnomaly)
                + -0.0069d * Math.sin(2 * solarLng);

        // declination of sun
        double solarDec = Math.asin(Math.sin(solarLng) * Math.sin(OBLIQUITY));

        final double latRad = latitude * DEGREES_TO_RADIANS;

        double cosHourAngle = (Math.sin(ALTIDUTE_CORRECTION_CIVIL_TWILIGHT) - Math.sin(latRad)
                * Math.sin(solarDec)) / (Math.cos(latRad) * Math.cos(solarDec));
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            state = NIGHT;
            sunset = -1;
            sunrise = -1;
            return;
        } else if (cosHourAngle <= -1) {
            state = DAY;
            sunset = -1;
            sunrise = -1;
            return;
        }

        float hourAngle = (float) (Math.acos(cosHourAngle) / (2 * Math.PI));

        sunset = Math.round((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;
        sunrise = Math.round((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;

        if (sunrise < time && sunset > time) {
            state = DAY;
        } else {
            state = NIGHT;
        }
    }

}