/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Bundle
import androidx.annotation.Keep
import java.time.Duration

/** Helper class to facilitate working with [DataPoint]s. */
// TODO(b/177504986): Remove all @Keep annotations once we figure out why this class gets stripped
// away by proguard.
@Keep
public object DataPoints {
    /**
     * When using [DataType.LOCATION], the value is represented as `double[]`. The `double` value at
     * this index represents the latitude.
     */
    public const val LOCATION_DATA_POINT_LATITUDE_INDEX: Int = 0

    /**
     * When using [DataType.LOCATION], the value is represented as `double[]`. The `double` value at
     * this index represents the longitude.
     */
    public const val LOCATION_DATA_POINT_LONGITUDE_INDEX: Int = 1

    /**
     * When using [DataType.LOCATION], the value is represented as `double[]`. The `double` value at
     * this index represents the altitude. This value will default to [Double.MAX_VALUE] if it is
     * not available.
     */
    public const val LOCATION_DATA_POINT_ALTITUDE_INDEX: Int = 2

    /**
     * When using [DataType.LOCATION], the value is represented as `double[]`. The `double` value at
     * this index represents the bearing. This value will default to [Double.MAX_VALUE] if it is not
     * available.
     */
    public const val LOCATION_DATA_POINT_BEARING_INDEX: Int = 3

    /** Name of intent extra containing the data points set on pending intent. */
    private const val EXTRA_DATA_POINTS: String = "hs.data_points_list"

    /** Name of intent extra containing whether permissions are granted or not. */
    private const val EXTRA_PERMISSIONS_GRANTED: String = "hs.data_points_has_permissions"

    /** Creates a new [DataPoint] of type [DataType.STEPS] with the given `steps`. */
    @JvmStatic
    @JvmOverloads
    public fun steps(
        steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.STEPS,
            Value.ofLong(steps),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /**
     * Creates a new [DataPoint] of type [DataType.STEPS_PER_MINUTE] with the given
     * `stepsPerMinute`.
     */
    @JvmStatic
    public fun stepsPerMinute(stepsPerMinute: Long, startDurationFromBoot: Duration): DataPoint =
        DataPoint.createSample(
            DataType.STEPS_PER_MINUTE,
            Value.ofLong(stepsPerMinute),
            startDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.DISTANCE] with the given `meters`. */
    @JvmStatic
    @JvmOverloads
    public fun distance(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DISTANCE,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /**
     * Creates a new [DataPoint] of type [DataType.ELEVATION_GAIN] with the given `meters`.
     *
     * @param meters meters gained between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     * @param metadata optional OEM specific data, not intended for broad consumption
     */
    @JvmStatic
    @JvmOverloads
    public fun elevationGain(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.ELEVATION_GAIN,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /**
     * Create a new [DataPoint] of type [DataType.ELEVATION_LOSS] with the given `meters`.
     *
     * @param meters meters lost between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     * @param metadata optional OEM specific data, not intended for broad consumption
     */
    @JvmStatic
    @JvmOverloads
    public fun elevationLoss(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.ELEVATION_LOSS,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /** Creates a new [DataPoint] of type [DataType.ABSOLUTE_ELEVATION] with the given `meters`. */
    @JvmStatic
    @JvmOverloads
    public fun absoluteElevation(
        meters: Double,
        durationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createSample(
            DataType.ABSOLUTE_ELEVATION,
            Value.ofDouble(meters),
            durationFromBoot,
            metadata ?: Bundle()
        )

    /** Creates a new [DataPoint] of type [DataType.FLOORS] with the given `floors`. */
    @JvmStatic
    @JvmOverloads
    public fun floors(
        floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.FLOORS,
            Value.ofDouble(floors),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /** Creates a new [DataPoint] of type [DataType.TOTAL_CALORIES] with the given `kcalories`. */
    @JvmStatic
    @JvmOverloads
    public fun calories(
        kcalories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.TOTAL_CALORIES,
            Value.ofDouble(kcalories),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /** Creates a new [DataPoint] of type [DataType.SWIMMING_STROKES] with the given `strokes`. */
    @JvmStatic
    public fun swimmingStrokes(
        strokes: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.SWIMMING_STROKES,
            Value.ofLong(strokes),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /**
     * Creates a new [DataPoint] of type [DataType.GOLF_SHOT_COUNT] with the given [shots].
     *
     * @param shots golf shots made between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     * @param metadata optional OEM specific data, not intended for broad consumption
     */
    @JvmStatic
    @JvmOverloads
    public fun golfShotCount(
        shots: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createInterval(
            DataType.GOLF_SHOT_COUNT,
            Value.ofLong(shots),
            startDurationFromBoot,
            endDurationFromBoot,
            metadata ?: Bundle()
        )

    /**
     * Creates a new [DataPoint] of type [DataType.LOCATION] with the given `latitude`, `longitude`,
     * `altitude`, `bearing`, and `accuracy`.
     */
    @JvmStatic
    @JvmOverloads
    public fun location(
        latitude: Double,
        longitude: Double,
        altitude: Double = Double.MAX_VALUE,
        bearing: Double = Double.MAX_VALUE,
        durationFromBoot: Duration,
        accuracy: LocationAccuracy? = null
    ): DataPoint =
        DataPoint.createSample(
            DataType.LOCATION,
            Value.ofDoubleArray(latitude, longitude, altitude, bearing),
            durationFromBoot,
            accuracy = accuracy
        )

    /** Creates a new [DataPoint] of type [DataType.SPEED] with the given `metersPerSecond`. */
    @JvmStatic
    @JvmOverloads
    public fun speed(
        metersPerSecond: Double,
        durationFromBoot: Duration,
        metadata: Bundle? = null
    ): DataPoint =
        DataPoint.createSample(
            DataType.SPEED,
            Value.ofDouble(metersPerSecond),
            durationFromBoot,
            metadata ?: Bundle()
        )

    /** Creates a new [DataPoint] of type [DataType.PACE] with the given `millisPerKm`. */
    @JvmStatic
    public fun pace(millisPerKm: Duration, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(
            DataType.PACE,
            Value.ofDouble((millisPerKm.toMillis()).toDouble()),
            durationFromBoot
        )

    /**
     * Creates a new [DataPoint] of type [DataType.HEART_RATE_BPM] with the given `bpm` and
     * `accuracy`.
     */
    @JvmStatic
    @JvmOverloads
    public fun heartRate(
        bpm: Double,
        durationFromBoot: Duration,
        accuracy: HeartRateAccuracy? = null
    ): DataPoint =
        DataPoint.createSample(
            DataType.HEART_RATE_BPM,
            Value.ofDouble(bpm),
            durationFromBoot,
            accuracy = accuracy
        )

    /** Creates a new [DataPoint] of type [DataType.DAILY_STEPS] with the given `steps`. */
    @JvmStatic
    public fun dailySteps(
        steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DAILY_STEPS,
            Value.ofLong(steps),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.DAILY_FLOORS] with the given `floors`. */
    @JvmStatic
    public fun dailyFloors(
        floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DAILY_FLOORS,
            Value.ofDouble(floors),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.DAILY_CALORIES] with the given `calories`. */
    @JvmStatic
    public fun dailyCalories(
        calories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DAILY_CALORIES,
            Value.ofDouble(calories),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.DAILY_DISTANCE] with the given `meters`. */
    @JvmStatic
    public fun dailyDistance(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DAILY_DISTANCE,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot
        )
}
