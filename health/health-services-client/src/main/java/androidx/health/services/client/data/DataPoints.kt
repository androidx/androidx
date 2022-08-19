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

import java.time.Duration
import java.time.Instant

/**
 * Helper class to facilitate creating [DataPoint]s. In general, this should not be needed outside
 * of tests.
 */
// TODO(b/177504986): Remove all @Keep annotations once we figure out why this class gets stripped
// away by proguard.
internal object DataPoints {

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.STEPS] with the given [steps].
     *
     * @param steps number of steps taken between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun steps(
        steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Long> =
        IntervalDataPoint(
            dataType = DataType.STEPS,
            value = steps,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.STEPS_PER_MINUTE] with the given
     * [stepsPerMinute].
     *
     * @param stepsPerMinute step rate at [timeDurationFromBoot]
     * @param timeDurationFromBoot the point in time [stepsPerMinute] is accurate
     */
    @JvmStatic
    public fun stepsPerMinute(
        stepsPerMinute: Long,
        timeDurationFromBoot: Duration
    ): SampleDataPoint<Long> =
        SampleDataPoint(
            dataType = DataType.STEPS_PER_MINUTE,
            value = stepsPerMinute,
            timeDurationFromBoot = timeDurationFromBoot
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.DISTANCE] with the given [meters].
     *
     * @param meters distance traveled between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun distance(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.DISTANCE,
            value = meters,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [CumulativeDataPoint] for [DataType.DISTANCE_TOTAL] with the given [meters].
     *
     * @param meters distance accumulated between [startTime] and [endTime]
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     */
    @JvmStatic
    public fun distanceTotal(
        meters: Double,
        startTime: Instant,
        endTime: Instant
    ): CumulativeDataPoint<Double> =
        CumulativeDataPoint(
            dataType = DataType.DISTANCE_TOTAL,
            total = meters,
            start = startTime,
            end = endTime
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.ELEVATION_GAIN] with the given [meters].
     *
     * @param meters meters gained between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun elevationGain(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.ELEVATION_GAIN,
            value = meters,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Create a new [IntervalDataPoint] of type [DataType.ELEVATION_LOSS] with the given [meters].
     *
     * @param meters meters lost between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun elevationLoss(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.ELEVATION_LOSS,
            value = meters,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.ABSOLUTE_ELEVATION] with the given
     * [meters].
     *
     * @param meters absolute elevation in meters at [timeDurationFromBoot]
     * @param timeDurationFromBoot the point in time [stepsPerMinute] is accurate
     */
    @JvmStatic
    public fun absoluteElevation(
        meters: Double,
        timeDurationFromBoot: Duration,
    ): SampleDataPoint<Double> =
        SampleDataPoint(
            dataType = DataType.ABSOLUTE_ELEVATION,
            value = meters,
            timeDurationFromBoot = timeDurationFromBoot,
        )

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.ABSOLUTE_ELEVATION_STATS] with the
     * given elevations (in meters).
     *
     * @param minAbsoluteElevationMeters lowest observed elevation in this interval
     * @param maxAbsoluteElevationMeters highest observed elevation in this interval
     * @param averageAbsoluteElevationMeters average observed elevation in this interval
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     */
    @JvmStatic
    public fun absoluteElevationStats(
        minAbsoluteElevationMeters: Double,
        maxAbsoluteElevationMeters: Double,
        averageAbsoluteElevationMeters: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint<Double> =
        StatisticalDataPoint(
            dataType = DataType.ABSOLUTE_ELEVATION_STATS,
            min = minAbsoluteElevationMeters,
            max = maxAbsoluteElevationMeters,
            average = averageAbsoluteElevationMeters,
            start = startTime,
            end = endTime,
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.FLOORS] with the given [floors].
     *
     * @param floors floors ascended between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun floors(
        floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.FLOORS,
            value = floors,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.CALORIES] with the given [kilocalories].
     *
     * @param kilocalories total calories burned (BMR + Active) between [startDurationFromBoot] and
     * [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun calories(
        kilocalories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.CALORIES,
            value = kilocalories,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [CumulativeDataPoint] of type [DataType.CALORIES_TOTAL] with the given
     * [kilocalories] that represents an accumulation over a longer period of time.
     *
     * @param kilocalories total calories burned (BMR + Active) between [startTime] and
     * [endTime]
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     */
    @JvmStatic
    public fun caloriesTotal(
        kilocalories: Double,
        startTime: Instant,
        endTime: Instant
    ): CumulativeDataPoint<Double> =
        CumulativeDataPoint(
            dataType = DataType.CALORIES_TOTAL,
            total = kilocalories,
            start = startTime,
            end = endTime
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.SWIMMING_STROKES] with the given
     * [strokes].
     *
     * @param strokes total swimming strokes between [startDurationFromBoot] and
     * [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun swimmingStrokes(
        strokes: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Long> =
        IntervalDataPoint(
            dataType = DataType.SWIMMING_STROKES,
            value = strokes,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.GOLF_SHOT_COUNT] with the given [shots].
     *
     * @param shots golf shots made between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun golfShotCount(
        shots: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Long> =
        IntervalDataPoint(
            dataType = DataType.GOLF_SHOT_COUNT,
            value = shots,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot,
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.LOCATION] with the given [latitude],
     * [longitude], and optionally [altitude], [bearing], and [accuracy].
     *
     * @param latitude latitude at [timeDurationFromBoot]
     * @param longitude longitude at [timeDurationFromBoot]
     * @param timeDurationFromBoot the point in time this data was recorded
     * @param altitude optional altitude or `null` at [timeDurationFromBoot]
     * @param bearing optional bearing or `null` at [timeDurationFromBoot]
     * @param accuracy optional [LocationAccuracy] describing this data or `null`
     */
    @JvmStatic
    @JvmOverloads
    public fun location(
        latitude: Double,
        longitude: Double,
        timeDurationFromBoot: Duration,
        altitude: Double? = null,
        bearing: Double? = null,
        accuracy: LocationAccuracy? = null
    ): SampleDataPoint<LocationData> =
        SampleDataPoint(
            dataType = DataType.LOCATION,
            value = LocationData(latitude, longitude, altitude, bearing),
            timeDurationFromBoot = timeDurationFromBoot,
            accuracy = accuracy
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.SPEED] with the given [metersPerSecond].
     *
     * @param metersPerSecond speed in meters per second at [timeDurationFromBoot]
     * @param timeDurationFromBoot the point in time [metersPerSecond] was recorded
     */
    @JvmStatic
    public fun speed(
        metersPerSecond: Double,
        timeDurationFromBoot: Duration,
    ): SampleDataPoint<Double> =
        SampleDataPoint(
            dataType = DataType.SPEED,
            value = metersPerSecond,
            timeDurationFromBoot = timeDurationFromBoot,
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.PACE] with the given
     * [durationPerKilometer].
     *
     * @param durationPerKilometer pace in terms of time per kilometer at [timeDurationFromBoot]
     * @param timeDurationFromBoot the point in time [durationPerKilometer] was recorded
     */
    @JvmStatic
    public fun pace(
        durationPerKilometer: Duration,
        timeDurationFromBoot: Duration
    ): SampleDataPoint<Double> =
        SampleDataPoint(
            dataType = DataType.PACE,
            value = (durationPerKilometer.toMillis()).toDouble(),
            timeDurationFromBoot = timeDurationFromBoot
        )

    /**
     * Creates a new [SampleDataPoint] of type [DataType.HEART_RATE_BPM] with the given [bpm] and
     * [accuracy].
     *
     * @param bpm heart rate given in beats per minute
     * @param timeDurationFromBoot the point in time this data was recorded
     * @param accuracy optional [HeartRateAccuracy] describing this data or `null`
     */
    @JvmStatic
    @JvmOverloads
    public fun heartRate(
        bpm: Double,
        timeDurationFromBoot: Duration,
        accuracy: HeartRateAccuracy? = null
    ): SampleDataPoint<Double> =
        SampleDataPoint(
            dataType = DataType.HEART_RATE_BPM,
            value = bpm,
            timeDurationFromBoot = timeDurationFromBoot,
            accuracy = accuracy
        )

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.HEART_RATE_BPM] with the given
     * min/max/average beats per minute.
     *
     * @param minBpm lowest observed heart rate given in beats per minute in this interval
     * @param maxBpm highest observed heart rate given in beats per minute in this interval
     * @param averageBpm average observed heart rate given in beats per minute in this interval
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     */
    @JvmStatic
    public fun heartRateStats(
        minBpm: Double,
        maxBpm: Double,
        averageBpm: Double,
        startTime: Instant,
        endTime: Instant,
    ): StatisticalDataPoint<Double> =
        StatisticalDataPoint(
            dataType = DataType.HEART_RATE_BPM_STATS,
            min = minBpm,
            max = maxBpm,
            average = averageBpm,
            start = startTime,
            end = endTime
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.STEPS_DAILY] with the given [dailySteps].
     *
     * @param dailySteps number of steps taken today, between [startDurationFromBoot] and
     * [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun dailySteps(
        dailySteps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Long> =
        IntervalDataPoint(
            dataType = DataType.STEPS_DAILY,
            value = dailySteps,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.FLOORS_DAILY] with the given [floors].
     *
     * @param floors number of floors ascended today, between [startDurationFromBoot] and
     * [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun dailyFloors(
        floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.FLOORS_DAILY,
            value = floors,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.CALORIES_DAILY] with the given
     * [calories].
     *
     * @param calories number of calories burned today including both active and passive / BMR,
     * between [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun dailyCalories(
        calories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.CALORIES_DAILY,
            value = calories,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot
        )

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.DISTANCE_DAILY] with the given [meters].
     *
     * @param meters number of meters traveled today through active/passive exercise between
     * [startDurationFromBoot] and [endDurationFromBoot]
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     */
    @JvmStatic
    public fun dailyDistance(
        meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> =
        IntervalDataPoint(
            dataType = DataType.DISTANCE_DAILY,
            value = meters,
            startDurationFromBoot = startDurationFromBoot,
            endDurationFromBoot = endDurationFromBoot
        )
}
