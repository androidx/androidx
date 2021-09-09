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
import java.time.Instant

/** Helper class to facilitate working with [DataPoint] s. */
// TODO(b/177504986): Remove all @Keep annotations once we figure out why this class gets stripped
// away by proguard.
@Keep
public object AggregateDataPoints {

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.ABSOLUTE_ELEVATION] with the given
     * elevations in meters.
     */
    @JvmStatic
    public fun aggregateAbsoluteElevation(
        minAbsElevation: Double,
        maxAbsElevation: Double,
        avgAbsElevation: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.ABSOLUTE_ELEVATION,
            Value.ofDouble(minAbsElevation),
            Value.ofDouble(maxAbsElevation),
            Value.ofDouble(avgAbsElevation)
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

    /** Creates a new [AggregateDataPoint] for the [DataType.DISTANCE] with the given `distance`. */
    @JvmStatic
    public fun aggregateDistance(
        distance: Double,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        CumulativeDataPoint(startTime, endTime, DataType.DISTANCE, Value.ofDouble(distance))

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
        minMillisPerKm: Double,
        maxMillisPerKm: Double,
        avgMillisPerKm: Double,
        startTime: Instant,
        endTime: Instant
    ): AggregateDataPoint =
        StatisticalDataPoint(
            startTime,
            endTime,
            DataType.PACE,
            Value.ofDouble(minMillisPerKm),
            Value.ofDouble(maxMillisPerKm),
            Value.ofDouble(avgMillisPerKm)
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
