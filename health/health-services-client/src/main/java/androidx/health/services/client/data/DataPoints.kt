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

import android.content.Intent
import androidx.annotation.Keep
import java.time.Duration
import java.util.ArrayList

/** Helper class to facilitate working with [DataPoint] s. */
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
     * this index represents the altitude. This is an optional index and there is no guarantee that
     * this index will be present.
     */
    public const val LOCATION_DATA_POINT_ALTITUDE_INDEX: Int = 2

    /** Name of intent extra containing the data points set on pending intent. */
    private const val EXTRA_DATA_POINTS: String = "hs.data_points_list"

    /** Name of intent extra containing whether permissions are granted or not. */
    private const val EXTRA_PERMISSIONS_GRANTED: String = "hs.data_points_has_permissions"

    /** Retrieves the [DataPoint] s that are contained in the given [Intent], if any. */
    @JvmStatic
    @Keep
    public fun getDataPoints(intent: Intent): List<DataPoint> =
        intent.getParcelableArrayListExtra(EXTRA_DATA_POINTS) ?: listOf()

    /** Puts the given [DataPoint] s in the given [Intent]. */
    @JvmStatic
    public fun putDataPoints(intent: Intent, dataPoints: Collection<DataPoint>) {
        val copy = ArrayList(dataPoints)
        intent.putParcelableArrayListExtra(EXTRA_DATA_POINTS, copy)
    }

    /** Sets whether [DataPoint] permissions are `granted` in the given [Intent]. */
    @JvmStatic
    public fun putPermissionsGranted(intent: Intent, granted: Boolean) {
        intent.putExtra(EXTRA_PERMISSIONS_GRANTED, granted)
    }

    /** Retrieves whether permissions are granted in this [Intent]. */
    @JvmStatic
    public fun getPermissionsGranted(intent: Intent): Boolean =
        intent.getBooleanExtra(EXTRA_PERMISSIONS_GRANTED, true)

    /** Creates a new [DataPoint] of type [DataType.STEPS] with the given `steps`. */
    @JvmStatic
    public fun steps(
        steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.STEPS,
            Value.ofLong(steps),
            startDurationFromBoot,
            endDurationFromBoot
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
    public fun distance(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.DISTANCE,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.ELEVATION] with the given `meters`. */
    @JvmStatic
    public fun elevation(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.ELEVATION,
            Value.ofDouble(meters),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.ALTITUDE] with the given `meters`. */
    @JvmStatic
    public fun altitude(meters: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.ALTITUDE, Value.ofDouble(meters), durationFromBoot)

    /** Creates a new [DataPoint] of type [DataType.FLOORS] with the given `floors`. */
    @JvmStatic
    public fun floors(
        floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.FLOORS,
            Value.ofDouble(floors),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.TOTAL_CALORIES] with the given `kcalories`. */
    @JvmStatic
    public fun calories(
        kcalories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.TOTAL_CALORIES,
            Value.ofDouble(kcalories),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.SWIMMING_STROKES] with the given `kcalories`. */
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
     * Creates a new [DataPoint] of type [DataType.LOCATION] with the given `latitude` and
     * `longitude`.
     */
    @JvmStatic
    public fun location(
        latitude: Double,
        longitude: Double,
        durationFromBoot: Duration
    ): DataPoint =
        DataPoint.createSample(
            DataType.LOCATION,
            Value.ofDoubleArray(latitude, longitude),
            durationFromBoot
        )

    /**
     * Creates a new [DataPoint] of type [DataType.LOCATION] with the given `latitude`, `longitude`
     * and `altitude`.
     */
    @JvmStatic
    public fun location(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        durationFromBoot: Duration
    ): DataPoint =
        DataPoint.createSample(
            DataType.LOCATION,
            Value.ofDoubleArray(latitude, longitude, altitude),
            durationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.SPEED] with the given `metersPerSecond`. */
    @JvmStatic
    public fun speed(metersPerSecond: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.SPEED, Value.ofDouble(metersPerSecond), durationFromBoot)

    /** Creates a new [DataPoint] of type [DataType.PACE] with the given `millisPerKm`. */
    @JvmStatic
    public fun pace(millisPerKm: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.PACE, Value.ofDouble(millisPerKm), durationFromBoot)

    /** Creates a new [DataPoint] of type [DataType.HEART_RATE_BPM] with the given `bpm`. */
    @JvmStatic
    public fun heartRate(bpm: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.HEART_RATE_BPM, Value.ofDouble(bpm), durationFromBoot)

    /** Creates a new [DataPoint] of type [DataType.SPO2] with the given `percent`. */
    @JvmStatic
    public fun spo2(percent: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.SPO2, Value.ofDouble(percent), durationFromBoot)

    /**
     * Creates a new [DataPoint] of type [DataType.AGGREGATE_DISTANCE] with the given `distance`.
     */
    @JvmStatic
    public fun aggregateDistance(
        distance: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.AGGREGATE_DISTANCE,
            Value.ofDouble(distance),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.AGGREGATE_STEP_COUNT] with the given `steps`. */
    @JvmStatic
    public fun aggregateSteps(
        steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.AGGREGATE_STEP_COUNT,
            Value.ofLong(steps),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /**
     * Creates a new [DataPoint] of type [DataType.AGGREGATE_CALORIES_EXPENDED] with the given
     * `kcalories`.
     */
    @JvmStatic
    public fun aggregateCalories(
        kcalories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.AGGREGATE_CALORIES_EXPENDED,
            Value.ofDouble(kcalories),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /**
     * Creates a new [DataPoint] of type [DataType.AGGREGATE_SWIMMING_STROKE_COUNT] with the given
     * `swimmingStrokes`.
     */
    @JvmStatic
    public fun aggregateSwimmingStrokes(
        swimmingStrokes: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): DataPoint =
        DataPoint.createInterval(
            DataType.AGGREGATE_SWIMMING_STROKE_COUNT,
            Value.ofLong(swimmingStrokes),
            startDurationFromBoot,
            endDurationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.AVERAGE_PACE] with the given `millisPerKm`. */
    @JvmStatic
    public fun averagePace(millisPerKm: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(DataType.AVERAGE_PACE, Value.ofDouble(millisPerKm), durationFromBoot)

    /**
     * Creates a new [DataPoint] of type [DataType.AVERAGE_SPEED] with the given `metersPerSecond`.
     */
    @JvmStatic
    public fun averageSpeed(metersPerSecond: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(
            DataType.AVERAGE_SPEED,
            Value.ofDouble(metersPerSecond),
            durationFromBoot
        )

    /** Creates a new [DataPoint] of type [DataType.MAX_SPEED] with the given `metersPerSecond`. */
    @JvmStatic
    public fun maxSpeed(metersPerSecond: Double, durationFromBoot: Duration): DataPoint =
        DataPoint.createSample(
            DataType.MAX_SPEED,
            Value.ofDouble(metersPerSecond),
            durationFromBoot
        )
}
