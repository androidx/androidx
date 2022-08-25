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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
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
     * @param steps number of steps taken between [startDurationFromBoot] and [endDurationFromBoot],
     * Range from 0 to 1000000.
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [steps] is out of range
     */
    @JvmStatic
    public fun steps(
        @IntRange(from = 0, to = 1000000) steps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Long> {
        if (steps in 0..1000000) {
            return IntervalDataPoint(
                dataType = DataType.STEPS,
                value = steps,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("steps value $steps is out of range")
        }
    }

    /**
     * Creates a new [SampleDataPoint] of type [DataType.STEPS_PER_MINUTE] with the given
     * [stepsPerMinute].
     *
     * @param stepsPerMinute step rate at [timeDurationFromBoot], Range from 0 to 1000000
     * @param timeDurationFromBoot the point in time [stepsPerMinute] is accurate
     *
     * @throws IllegalArgumentException if [stepsPerMinute] is out of range
     */
    @JvmStatic
    public fun stepsPerMinute(
        @IntRange(from = 0, to = 1000000) stepsPerMinute: Long,
        timeDurationFromBoot: Duration
    ): SampleDataPoint<Long> {
        if (stepsPerMinute in 0..1000000) {
            return SampleDataPoint(
                dataType = DataType.STEPS_PER_MINUTE,
                value = stepsPerMinute,
                timeDurationFromBoot = timeDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("stepsPerMinute value $stepsPerMinute is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.DISTANCE] with the given [meters].
     *
     * @param meters distance traveled between [startDurationFromBoot] and [endDurationFromBoot]
     * , Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun distance(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.DISTANCE,
                value = meters,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }

    /**
     * Creates a new [CumulativeDataPoint] for [DataType.DISTANCE_TOTAL] with the given [meters].
     *
     * @param meters distance accumulated between [startTime] and [endTime], Range from
     * 0.0 to 1000000.0
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun distanceTotal(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        startTime: Instant,
        endTime: Instant
    ): CumulativeDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return CumulativeDataPoint(
                dataType = DataType.DISTANCE_TOTAL,
                total = meters,
                start = startTime,
                end = endTime
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.ELEVATION_GAIN] with the given [meters].
     *
     * @param meters meters gained between [startDurationFromBoot] and [endDurationFromBoot],
     * Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun elevationGain(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.ELEVATION_GAIN,
                value = meters,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }

    /**
     * Create a new [IntervalDataPoint] of type [DataType.ELEVATION_LOSS] with the given [meters].
     *
     * @param meters meters lost between [startDurationFromBoot] and [endDurationFromBoot],
     * Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun elevationLoss(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.ELEVATION_LOSS,
                value = meters,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }

    /**
     * Creates a new [SampleDataPoint] of type [DataType.ABSOLUTE_ELEVATION] with the given
     * [meters].
     *
     * @param meters absolute elevation in meters at [timeDurationFromBoot], Range
     * from 0.0 to 1000000.0
     * @param timeDurationFromBoot the point in time [stepsPerMinute] is accurate
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun absoluteElevation(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        timeDurationFromBoot: Duration,
    ): SampleDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return SampleDataPoint(
                dataType = DataType.ABSOLUTE_ELEVATION,
                value = meters,
                timeDurationFromBoot = timeDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.ABSOLUTE_ELEVATION_STATS] with the
     * given elevations (in meters).
     *
     * @param minAbsoluteElevationMeters lowest observed elevation in this interval,
     * Range from 0.0 to 1000000.0
     * @param maxAbsoluteElevationMeters highest observed elevation in this interval,
     * Range from 0.0 to 1000000.0
     * @param averageAbsoluteElevationMeters average observed elevation in this interval,
     * Range from 0.0 to 1000000.0
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     *
     * @throws IllegalArgumentException if [minAbsoluteElevationMeters] or
     * [maxAbsoluteElevationMeters] or [averageAbsoluteElevationMeters] are out of range
     */
    @JvmStatic
    public fun absoluteElevationStats(
        @FloatRange(from = 0.0, to = 1000000.0) minAbsoluteElevationMeters: Double,
        @FloatRange(from = 0.0, to = 1000000.0) maxAbsoluteElevationMeters: Double,
        @FloatRange(from = 0.0, to = 1000000.0) averageAbsoluteElevationMeters: Double,
        startTime: Instant,
        endTime: Instant
    ): StatisticalDataPoint<Double> {

        if (minAbsoluteElevationMeters !in 0.0..1000000.0) {
            throw IllegalArgumentException("minAbsoluteElevationMeters value " +
                "$minAbsoluteElevationMeters is out of range")
        }
        if (maxAbsoluteElevationMeters !in 0.0..1000000.0) {
            throw IllegalArgumentException("maxAbsoluteElevationMeters value " +
                "$maxAbsoluteElevationMeters is out of range")
        }
        if (averageAbsoluteElevationMeters !in 0.0..1000000.0) {
            throw IllegalArgumentException("averageAbsoluteElevationMeters value " +
                "$averageAbsoluteElevationMeters is out of range")
        }
        return StatisticalDataPoint(
            dataType = DataType.ABSOLUTE_ELEVATION_STATS,
            min = minAbsoluteElevationMeters,
            max = maxAbsoluteElevationMeters,
            average = averageAbsoluteElevationMeters,
            start = startTime,
            end = endTime,
        )
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.FLOORS] with the given [floors].
     *
     * @param floors floors ascended between [startDurationFromBoot] and [endDurationFromBoot],
     * Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [floors] is out of range
     */
    @JvmStatic
    public fun floors(
        @FloatRange(from = 0.0, to = 1000000.0) floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (floors in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.FLOORS,
                value = floors,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("floors value $floors is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.CALORIES] with the given [kilocalories].
     *
     * @param kilocalories total calories burned (BMR + Active) between [startDurationFromBoot] and
     * [endDurationFromBoot], Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [kilocalories] is out of range
     */
    @JvmStatic
    public fun calories(
        @FloatRange(from = 0.0, to = 1000000.0) kilocalories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (kilocalories in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.CALORIES,
                value = kilocalories,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("kilocalories value $kilocalories is out of range")
        }
    }

    /**
     * Creates a new [CumulativeDataPoint] of type [DataType.CALORIES_TOTAL] with the given
     * [kilocalories] that represents an accumulation over a longer period of time.
     *
     * @param kilocalories total calories burned (BMR + Active) between [startTime] and
     * [endTime], Range from 0.0 to 1000000.0
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     *
     * @throws IllegalArgumentException if [kilocalories] is out of range
     */
    @JvmStatic
    public fun caloriesTotal(
        @FloatRange(from = 0.0, to = 1000000.0) kilocalories: Double,
        startTime: Instant,
        endTime: Instant
    ): CumulativeDataPoint<Double> {
        if (kilocalories in 0.0..1000000.0) {
            return CumulativeDataPoint(
                dataType = DataType.CALORIES_TOTAL,
                total = kilocalories,
                start = startTime,
                end = endTime
            )
        } else {
            throw IllegalArgumentException("kilocalories value $kilocalories is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.SWIMMING_STROKES] with the given
     * [strokes].
     *
     * @param strokes total swimming strokes between [startDurationFromBoot] and
     * [endDurationFromBoot], Range from 0 to 1000000
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [strokes] is out of range
     */
    @JvmStatic
    public fun swimmingStrokes(
        @IntRange(from = 0, to = 1000000) strokes: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Long> {
        if (strokes in 0..1000000) {
            return IntervalDataPoint(
                dataType = DataType.SWIMMING_STROKES,
                value = strokes,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("strokes value $strokes is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.GOLF_SHOT_COUNT] with the given [shots].
     *
     * @param shots golf shots made between [startDurationFromBoot] and [endDurationFromBoot],
     * Range from 0 to 1000000
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [shots] is out of range
     */
    @JvmStatic
    public fun golfShotCount(
        @IntRange(from = 0, to = 1000000) shots: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Long> {
        if (shots in 0..1000000) {
            return IntervalDataPoint(
                dataType = DataType.GOLF_SHOT_COUNT,
                value = shots,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("shots value $shots is out of range")
        }
    }

    /**
     * Creates a new [SampleDataPoint] of type [DataType.LOCATION] with the given [latitude],
     * [longitude], and optionally [altitude], [bearing], and [accuracy].
     *
     * @param latitude latitude at [timeDurationFromBoot], Range from -90.0 to 90.0
     * @param longitude longitude at [timeDurationFromBoot], Range from -180.0 to 180.0
     * @param timeDurationFromBoot the point in time this data was recorded
     * @param altitude optional altitude or [LocationData.ALTITUDE_UNAVAILABLE] at
     * [timeDurationFromBoot]
     * @param bearing optional bearing or [LocationData.BEARING_UNAVAILABLE] at
     * [timeDurationFromBoot], Range from 0.0 (inclusive) to 360.0 (exclusive).
     * Value [LocationData.ALTITUDE_UNAVAILABLE] represents altitude is not available
     * @param accuracy optional [LocationAccuracy] describing this data or `null`
     *
     * @throws IllegalArgumentException if [latitude] or [longitude] or
     * [altitude] or [bearing] are out of range
     */
    @JvmStatic
    @JvmOverloads
    public fun location(
        @FloatRange(from = -90.0, to = 90.0) latitude: Double,
        @FloatRange(from = -180.0, to = 180.0) longitude: Double,
        timeDurationFromBoot: Duration,
        @FloatRange altitude: Double = LocationData.ALTITUDE_UNAVAILABLE,
        @FloatRange(from = -1.0, to = 360.0, toInclusive = false) bearing: Double =
            LocationData.BEARING_UNAVAILABLE,
        accuracy: LocationAccuracy? = null
    ): SampleDataPoint<LocationData> {
        if (latitude !in -90.0..90.0) {
            throw IllegalArgumentException("latitude value $latitude is out of range")
        }
        if (longitude !in -180.0..180.0) {
            throw IllegalArgumentException("longitude value $longitude is out of range")
        }
        if (bearing < -1.0 && bearing >= 360.0) {
            throw IllegalArgumentException("bearing value $bearing is out of range")
        }
        return SampleDataPoint(
            dataType = DataType.LOCATION,
            value = LocationData(latitude, longitude, altitude, bearing),
            timeDurationFromBoot = timeDurationFromBoot,
            accuracy = accuracy
        )
    }

    /**
     * Creates a new [SampleDataPoint] of type [DataType.SPEED] with the given [metersPerSecond].
     *
     * @param metersPerSecond speed in meters per second at [timeDurationFromBoot],
     * Range from 0.0 to 1000000.0
     * @param timeDurationFromBoot the point in time [metersPerSecond] was recorded
     *
     * @throws IllegalArgumentException if [metersPerSecond] is out of range
     */
    @JvmStatic
    public fun speed(
        @FloatRange(from = 0.0, to = 1000000.0) metersPerSecond: Double,
        timeDurationFromBoot: Duration,
    ): SampleDataPoint<Double> {
        if (metersPerSecond in 0.0..1000000.0) {
            return SampleDataPoint(
                dataType = DataType.SPEED,
                value = metersPerSecond,
                timeDurationFromBoot = timeDurationFromBoot,
            )
        } else {
            throw IllegalArgumentException("metersPerSecond value $metersPerSecond is out of range")
        }
    }

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
     * @param bpm heart rate given in beats per minute, Range from 0.0 to 300.0
     * @param timeDurationFromBoot the point in time this data was recorded
     * @param accuracy optional [HeartRateAccuracy] describing this data or `null`
     *
     * @throws IllegalArgumentException if [bpm] is out of range
     */
    @JvmStatic
    @JvmOverloads
    public fun heartRate(
        @FloatRange(from = 0.0, to = 300.0) bpm: Double,
        timeDurationFromBoot: Duration,
        accuracy: HeartRateAccuracy? = null
    ): SampleDataPoint<Double> {
        if (bpm in 0.0..300.0) {
            return SampleDataPoint(
                dataType = DataType.HEART_RATE_BPM,
                value = bpm,
                timeDurationFromBoot = timeDurationFromBoot,
                accuracy = accuracy
            )
        } else {
            throw IllegalArgumentException("bpm value $bpm is out of range")
        }
    }

    /**
     * Creates a new [StatisticalDataPoint] of type [DataType.HEART_RATE_BPM] with the given
     * min/max/average beats per minute.
     *
     * @param minBpm lowest observed heart rate given in beats per minute in this interval,
     * Range from 0.0 to 300.0
     * @param maxBpm highest observed heart rate given in beats per minute in this interval,
     * Range from 0.0 to 300.0
     * @param averageBpm average observed heart rate given in beats per minute in this interval,
     * Range from 0.0 to 300.0
     * @param startTime the point in time this data point begins
     * @param endTime the point in time this data point ends
     *
     * @throws IllegalArgumentException if [minBpm] or [maxBpm] or [averageBpm] are out of range
     */
    @JvmStatic
    public fun heartRateStats(
        @FloatRange(from = 0.0, to = 300.0) minBpm: Double,
        @FloatRange(from = 0.0, to = 300.0) maxBpm: Double,
        @FloatRange(from = 0.0, to = 300.0) averageBpm: Double,
        startTime: Instant,
        endTime: Instant,
    ): StatisticalDataPoint<Double> {
        if (minBpm !in 0.0..300.0) {
            throw IllegalArgumentException("minBpm value $minBpm is out of range")
        }
        if (maxBpm !in 0.0..300.0) {
            throw IllegalArgumentException("maxBpm value $maxBpm is out of range")
        }
        if (averageBpm !in 0.0..300.0) {
            throw IllegalArgumentException("averageBpm value $averageBpm is out of range")
        }
        return StatisticalDataPoint(
            dataType = DataType.HEART_RATE_BPM_STATS,
            min = minBpm,
            max = maxBpm,
            average = averageBpm,
            start = startTime,
            end = endTime
        )
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.STEPS_DAILY] with the given [dailySteps].
     *
     * @param dailySteps number of steps taken today, between [startDurationFromBoot] and
     * [endDurationFromBoot], Range from 0 to 1000000
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [dailySteps] is out of range
     */
    @JvmStatic
    public fun dailySteps(
        @IntRange(from = 0, to = 1000000) dailySteps: Long,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Long> {
        if (dailySteps in 0..1000000) {
            return IntervalDataPoint(
                dataType = DataType.STEPS_DAILY,
                value = dailySteps,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("dailySteps value $dailySteps is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.FLOORS_DAILY] with the given [floors].
     *
     * @param floors number of floors ascended today, between [startDurationFromBoot] and
     * [endDurationFromBoot], Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [floors] is out of range
     */
    @JvmStatic
    public fun dailyFloors(
        @FloatRange(from = 0.0, to = 1000000.0) floors: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Double> {
        if (floors in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.FLOORS_DAILY,
                value = floors,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("floors value $floors is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.CALORIES_DAILY] with the given
     * [calories].
     *
     * @param calories number of calories burned today including both active and passive / BMR,
     * between [startDurationFromBoot] and [endDurationFromBoot], Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [calories] is out of range
     */
    @JvmStatic
    public fun dailyCalories(
        @FloatRange(from = 0.0, to = 1000000.0) calories: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration
    ): IntervalDataPoint<Double> {
        if (calories in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.CALORIES_DAILY,
                value = calories,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("calories value $calories is out of range")
        }
    }

    /**
     * Creates a new [IntervalDataPoint] of type [DataType.DISTANCE_DAILY] with the given [meters].
     *
     * @param meters number of meters traveled today through active/passive exercise between
     * [startDurationFromBoot] and [endDurationFromBoot], Range from 0.0 to 1000000.0
     * @param startDurationFromBoot the point in time this data point begins
     * @param endDurationFromBoot the point in time this data point ends
     *
     * @throws IllegalArgumentException if [meters] is out of range
     */
    @JvmStatic
    public fun dailyDistance(
        @FloatRange(from = 0.0, to = 1000000.0) meters: Double,
        startDurationFromBoot: Duration,
        endDurationFromBoot: Duration,
    ): IntervalDataPoint<Double> {
        if (meters in 0.0..1000000.0) {
            return IntervalDataPoint(
                dataType = DataType.DISTANCE_DAILY,
                value = meters,
                startDurationFromBoot = startDurationFromBoot,
                endDurationFromBoot = endDurationFromBoot
            )
        } else {
            throw IllegalArgumentException("meters value $meters is out of range")
        }
    }
}
