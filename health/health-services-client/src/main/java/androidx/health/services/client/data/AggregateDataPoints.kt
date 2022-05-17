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

import androidx.annotation.Keep
import java.time.Duration
import java.time.Instant

/** Helper class to facilitate working with [DataPoint]s. */
// TODO(b/177504986): Remove all @Keep annotations once we figure out why this class gets stripped
// away by proguard.
@Keep
public object AggregateDataPoints {

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.ABSOLUTE_ELEVATION] with the given
     * elevations (in meters).
     */
    @JvmStatic
    public fun aggregateAbsoluteElevation(
        minAbsElevationMeters: Double,
        maxAbsElevationMeters: Double,
        avgAbsElevationMeters: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.ABSOLUTE_ELEVATION,
            Value.ofDouble(minAbsElevationMeters),
            Value.ofDouble(maxAbsElevationMeters),
            Value.ofDouble(avgAbsElevationMeters)
        )

    /**
     * Creates a new [AggregateDataPoint] of type [DataType.TOTAL_CALORIES] with the given
     * `kcalories`.
     */
    @JvmStatic
    public fun aggregateCalories(
        kcalories: Double,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(startTime, endTime, DataType.TOTAL_CALORIES, Value.ofDouble(kcalories))

    /** Creates a new [AggregateDataPoint] for the [DataType.DISTANCE] with the given `meters`. */
    @JvmStatic
    public fun aggregateDistance(
        meters: Double,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(startTime, endTime, DataType.DISTANCE, Value.ofDouble(meters))

    /**
     * Creates a new [AggregateDataPoint] for the [DataType.ELEVATION_GAIN] with the given
     * `gainMeters`.
     */
    @JvmStatic
    public fun aggregateElevationGain(
        gainMeters: Double,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(startTime, endTime, DataType.ELEVATION_GAIN, Value.ofDouble(gainMeters))

    /**
     * Creates a new [AggregateDataPoint] of type [DataType.HEART_RATE_BPM] with the given `bpm`s.
     */
    @JvmStatic
    public fun aggregateHeartRate(
        minBpm: Double,
        maxBpm: Double,
        avgBpm: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.HEART_RATE_BPM,
            Value.ofDouble(minBpm),
            Value.ofDouble(maxBpm),
            Value.ofDouble(avgBpm)
        )

    /** Creates a new [AggregateDataPoint] of type [DataType.PACE] with the given `millisPerKm`. */
    @JvmStatic
    public fun aggregatePace(
        minMillisPerKm: Duration,
        maxMillisPerKm: Duration,
        avgMillisPerKm: Duration,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.PACE,
            Value.ofDouble((minMillisPerKm.toMillis()).toDouble()),
            Value.ofDouble((maxMillisPerKm.toMillis()).toDouble()),
            Value.ofDouble((avgMillisPerKm.toMillis()).toDouble())
        )

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.SPEED] with the given
     * `metersPerSecond`.
     */
    @JvmStatic
    public fun aggregateSpeed(
        minMetersPerSecond: Double,
        maxMetersPerSecond: Double,
        avgMetersPerSecond: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.SPEED,
            Value.ofDouble(minMetersPerSecond),
            Value.ofDouble(maxMetersPerSecond),
            Value.ofDouble(avgMetersPerSecond)
        )

    /** Creates a new [AggregateDataPoint] of type [DataType.STEPS] with the given `steps`. */
    @JvmStatic
    public fun aggregateSteps(
        steps: Long,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(startTime, endTime, DataType.STEPS, Value.ofLong(steps))

    /**
     * Creates a new [AggregateDataPoint] of type [DataType.STEPS_PER_MINUTE] with the given
     * `steps`.
     *
     * @param minStepsPerMinute minimum number of steps per minute between [startTime] and [endTime]
     * @param maxStepsPerMinute maximum number of steps per minute between [startTime] and [endTime]
     * @param avgStepsPerMinute average number of steps per minute between [startTime] and [endTime]
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     */
    @JvmStatic
    public fun aggregateStepsPerMinute(
        minStepsPerMinute: Long,
        maxStepsPerMinute: Long,
        avgStepsPerMinute: Long,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.STEPS_PER_MINUTE,
            Value.ofLong(minStepsPerMinute),
            Value.ofLong(maxStepsPerMinute),
            Value.ofLong(avgStepsPerMinute)
        )

    /**
     * Creates a new [DataPoint] of type [DataType.SWIMMING_STROKES] with the given
     * `swimmingStrokes`.
     */
    @JvmStatic
    public fun aggregateSwimmingStrokes(
        swimmingStrokes: Long,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(
            startTime,
            endTime,
            DataType.SWIMMING_STROKES,
            Value.ofLong(swimmingStrokes)
        )
}
