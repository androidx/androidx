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

import android.util.Log
import androidx.health.services.client.data.DataType.Companion.DISTANCE
import androidx.health.services.client.data.DataType.TimeType
import androidx.health.services.client.proto.ByteString
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_INTERVAL
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_SAMPLE
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_UNKNOWN

/**
 * [DataType] that represents a granular, non-aggregated point in time. This will map to
 * [IntervalDataPoint]s and [SampleDataPoint]s.
 */
class DeltaDataType<T : Any, D : DataPoint<T>>(
    name: String,
    timeType: TimeType,
    valueClass: Class<T>
) : DataType<T, D>(name, timeType, valueClass, isAggregate = false)

/**
 * [DataType] that represents aggregated data. This will map to [CumulativeDataPoint]s and
 * [StatisticalDataPoint]s.
 */
class AggregateDataType<T : Number, D : DataPoint<T>>(
    name: String,
    timeType: TimeType,
    valueClass: Class<T>,
) : DataType<T, D>(name, timeType, valueClass, isAggregate = true)

/**
 * A data type is a representation of health data managed by Health Services.
 *
 * A [DataType] specifies the type of the values inside of a [DataPoint]. Health Services defines
 * data types for instantaneous observations (Samples / [SampleDataPoint], e.g. heart rate) and data
 * types for a change between readings (Intervals / [IntervalDataPoint], e.g. distance).
 *
 * Health services also allows specifying aggregated versions of many data types, which will allow
 * the developer to get e.g. a running total of intervals ([CumulativeDataPoint]) or statistics like
 * min/max/average on samples ([StatisticalDataPoint]).
 *
 * Note: the data type defines only the representation and format of the data, and not how it's
 * being collected, the sensor being used, or the parameters of the collection. As an example,
 * [DISTANCE] may come from GPS location if available, or steps if not available.
 */
@Suppress("ParcelCreator")
abstract class DataType<T : Any, D : DataPoint<T>>(
    /** Returns the name of this [DataType], e.g. `"Steps"`. */
    val name: String,

    /** Returns the [TimeType] of this [DataType]. */
    internal val timeType: TimeType,

    /** Returns the underlying [Class] of this [DataType]. */
    val valueClass: Class<T>,

    /**
     * Returns `true` if this will be represented by [StatisticalDataPoint] or
     * [CumulativeDataPoint], otherwise `false`.
     */
    internal val isAggregate: Boolean,
) {

    /**
     * Whether the [DataType] corresponds to a measurement spanning an interval, or a sample at a
     * single point in time.
     */
    public class TimeType private constructor(public val id: Int, public val name: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TimeType) return false
            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int = id

        override fun toString(): String = name

        internal fun toProto(): DataProto.DataType.TimeType =
            when (this) {
                INTERVAL -> TIME_TYPE_INTERVAL
                SAMPLE -> TIME_TYPE_SAMPLE
                else -> TIME_TYPE_UNKNOWN
            }

        companion object {
            /** The [TimeType] is unknown or this library is too old to know about it. */
            @JvmField val UNKNOWN: TimeType = TimeType(0, "UNKNOWN")

            /**
             * TimeType that indicates the DataType has a value that represents an interval of time
             * with a beginning and end. For example, number of steps taken over a span of time.
             */
            @JvmField val INTERVAL: TimeType = TimeType(1, "INTERVAL")

            /**
             * TimeType that indicates the DataType has a value that represents a single point in
             * time. For example, heart rate reading at a specific time.
             */
            @JvmField val SAMPLE: TimeType = TimeType(2, "SAMPLE")

            internal fun fromProto(proto: DataProto.DataType.TimeType): TimeType =
                when (proto) {
                    TIME_TYPE_INTERVAL -> INTERVAL
                    TIME_TYPE_SAMPLE -> SAMPLE
                    TIME_TYPE_UNKNOWN -> UNKNOWN
                }
        }
    }

    override fun toString(): String =
        "DataType(" +
            "name=$name," +
            " timeType=$timeType," +
            " class=${valueClass.simpleName}," +
            " isAggregate=$isAggregate" +
            ")"

    internal val proto: DataProto.DataType =
        DataProto.DataType.newBuilder()
            .setName(name)
            .setTimeType(timeType.toProto())
            .setFormat(classToValueFormat())
            .build()

    internal fun toProtoFromValue(
        value: T,
    ): DataProto.Value {
        val builder = DataProto.Value.newBuilder()
        when (valueClass.kotlin) {
            Long::class -> builder.longVal = value as Long
            Double::class -> builder.doubleVal = value as Double
            Boolean::class -> builder.boolVal = value as Boolean
            ByteArray::class -> builder.byteArrayVal = ByteString.copyFrom(value as ByteArray)
            DoubleArray::class ->
                builder.doubleArrayVal =
                    DataProto.Value.DoubleArray.newBuilder()
                        .addAllDoubleArray((value as DoubleArray).toList())
                        .build()
            LocationData::class -> (value as LocationData).addToValueProtoBuilder(builder)
            else -> Log.w(TAG, "Unexpected value class ${valueClass.simpleName}")
        }

        return builder.build()
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun toValueFromProto(proto: DataProto.Value): T {
        return when (valueClass.kotlin) {
            Long::class -> proto.longVal
            Double::class -> proto.doubleVal
            Boolean::class -> proto.boolVal
            ByteArray::class -> proto.byteArrayVal?.toByteArray()
            DoubleArray::class -> proto.doubleArrayVal
            LocationData::class -> LocationData.fromDataProtoValue(proto)
            else -> throw UnsupportedOperationException("Cannot retrieve value for $valueClass")
        }
            as T
    }

    private fun classToValueFormat(): Int {
        return when (valueClass.kotlin) {
            Double::class -> FORMAT_DOUBLE
            Long::class -> FORMAT_LONG
            Boolean::class -> FORMAT_BOOLEAN
            DoubleArray::class -> FORMAT_DOUBLE_ARRAY
            ByteArray::class -> FORMAT_BYTE_ARRAY
            LocationData::class -> FORMAT_DOUBLE_ARRAY
            else ->
                throw UnsupportedOperationException("No IPC format available for class $valueClass")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataType<*, *>

        if (name != other.name) return false
        if (timeType != other.timeType) return false
        if (isAggregate != other.isAggregate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + timeType.hashCode()
        result = 31 * result + isAggregate.hashCode()
        return result
    }

    companion object {
        private const val TAG = "DataType"

        private inline fun <reified T : Number> createIntervalDataType(
            name: String
        ): DeltaDataType<T, IntervalDataPoint<T>> =
            DeltaDataType(name, TimeType.INTERVAL, T::class.java)

        private inline fun <reified T : Number> createSampleDataType(
            name: String
        ): DeltaDataType<T, SampleDataPoint<T>> =
            DeltaDataType(name, TimeType.SAMPLE, T::class.java)

        private inline fun <reified T : Number> createStatsDataType(
            name: String
        ): AggregateDataType<T, StatisticalDataPoint<T>> =
            AggregateDataType(name, TimeType.SAMPLE, T::class.java)

        private inline fun <reified T : Number> createCumulativeDataType(
            name: String
        ): AggregateDataType<T, CumulativeDataPoint<T>> =
            AggregateDataType(name, TimeType.INTERVAL, T::class.java)

        /**
         * A measure of the gain in elevation since the last update expressed in meters. Elevation
         * losses are not counted in this metric (so it will only be positive or 0).
         */
        @JvmField
        val ELEVATION_GAIN: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Elevation Gain")

        /**
         * A measure of the total gain in elevation since the start of an active exercise expressed
         * in meters. Elevation losses are not counted in this metric (so it will only be positive
         * or 0).
         */
        @JvmField
        val ELEVATION_GAIN_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Elevation Gain")

        /**
         * A measure of the loss in elevation since the last update expressed in meters. Elevation
         * gains are not counted in this metric (so it will only be positive or 0).
         */
        @JvmField
        val ELEVATION_LOSS: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Elevation Loss")

        /**
         * A measure of the total loss in elevation since the start of an active exercise expressed
         * in meters. Elevation gains are not counted in this metric (so it will only be positive or
         * 0).
         */
        @JvmField
        val ELEVATION_LOSS_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Elevation Loss")

        /** Absolute elevation at a specific point in time expressed in meters. */
        @JvmField
        val ABSOLUTE_ELEVATION: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("Absolute Elevation")

        /**
         * Statistical information about the absolute elevation over the course of the active
         * exercise expressed in meters.
         */
        @JvmField
        val ABSOLUTE_ELEVATION_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Absolute Elevation")

        /** A distance delta between each reading expressed in meters. */
        @JvmField
        val DISTANCE: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Distance")

        /** Total distance since the start of the active exercise expressed in meters. */
        @JvmField
        val DISTANCE_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Distance")

        /** Distance traveled over declining ground between each reading expressed in meters. */
        @JvmField
        val DECLINE_DISTANCE: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Decline Distance")

        /**
         * The total distance traveled over declining ground between each reading since the start of
         * the active exercise expressed in meters.
         */
        @JvmField
        val DECLINE_DISTANCE_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Decline Distance")

        /**
         * The amount of time the user spent traveling over declining ground since the last update,
         * expressed in seconds.
         */
        @JvmField
        val DECLINE_DURATION: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Decline Duration")

        /**
         * Total duration the user spent traveling over declining ground since the start of the
         * active exercise, expressed in seconds.
         */
        @JvmField
        val DECLINE_DURATION_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Decline Duration")

        /** The distance traveled over flat since the last update expressed in meters. */
        @JvmField
        val FLAT_GROUND_DISTANCE: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Flat Ground Distance")

        /**
         * The total distance traveled over flat ground since the start of the active exercise
         * expressed in meters.
         */
        @JvmField
        val FLAT_GROUND_DISTANCE_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Flat Ground Distance")

        /**
         * The amount of time the user spent traveling over flat ground since the last update,
         * expressed in seconds.
         */
        @JvmField
        val FLAT_GROUND_DURATION: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Flat Ground Duration")

        /**
         * The total duration the user spent traveling over flat ground since the start of the
         * active exercise, expressed in seconds.
         */
        @JvmField
        val FLAT_GROUND_DURATION_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Flat Ground Duration")

        /**
         * The number of golf shots taken since the last update, where a golf shot consists of
         * swinging the club and hitting the ball.
         */
        @JvmField
        val GOLF_SHOT_COUNT: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Golf Shot Count")

        /**
         * The total number of golf shots taken since the start of the current active exercise,
         * where a golf shot consists swinging the club and hitting the ball.
         */
        @JvmField
        val GOLF_SHOT_COUNT_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Golf Shot Count")

        /**
         * The distance traveled over inclining ground since the last update expressed in meters.
         */
        @JvmField
        val INCLINE_DISTANCE: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Incline Distance")

        /**
         * The total distance traveled over inclining since the start of the active exercise
         * expressed in meters.
         */
        @JvmField
        val INCLINE_DISTANCE_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Incline Distance")

        /**
         * The amount of time the user spent traveling over inclining ground since the last update,
         * expressed in seconds.
         */
        @JvmField
        val INCLINE_DURATION: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Incline Duration")

        /**
         * Total amount of time the user spent traveling over inclining ground since the start of
         * the active exercise, expressed in seconds.
         */
        @JvmField
        val INCLINE_DURATION_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Incline Duration")

        /**
         * Number of floors climbed since the last update. Note that partial floors are supported,
         * so this is represented as a [Double].
         */
        @JvmField
        val FLOORS: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Floors")

        /**
         * Total number of floors climbed since the start of the active exercise. Note that partial
         * floors are supported, so this is represented as a [Double].
         */
        @JvmField
        val FLOORS_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Floors")

        /**
         * Current heart rate, in beats per minute.
         *
         * Accuracy for a [DataPoint] of type [DataType.HEART_RATE_BPM] is represented by
         * [HeartRateAccuracy].
         */
        @JvmField
        val HEART_RATE_BPM: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("HeartRate")

        /**
         * Statistics on heart rate since the start of the current exercise, expressed in beats per
         * minute.
         */
        @JvmField
        val HEART_RATE_BPM_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("HeartRate")

        /**
         * Latitude, longitude and optionally, altitude and bearing at a specific point in time.
         *
         * Accuracy for a [DataPoint] of type [LOCATION] is represented by [LocationAccuracy].
         */
        @JvmField
        val LOCATION: DeltaDataType<LocationData, SampleDataPoint<LocationData>> =
            DeltaDataType("Location", TimeType.SAMPLE, LocationData::class.java)

        /** Speed at a specific point in time, expressed as meters/second. */
        @JvmField
        val SPEED: DeltaDataType<Double, SampleDataPoint<Double>> = createSampleDataType("Speed")

        /**
         * Statistics on speed since the start of the active exercise, expressed in meters/second.
         */
        @JvmField
        val SPEED_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Speed")

        /**
         * Maximum rate of oxygen consumption measured at a specific point in time. Valid range
         * `0f` - `100f`.
         */
        @JvmField
        val VO2_MAX: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("VO2 Max")

        /**
         * Statistics on maximum rate of oxygen consumption measured since the start of an exercise.
         * Valid range `0f` - `100f`.
         */
        @JvmField
        val VO2_MAX_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("VO2 Max")

        /** Number of steps taken since the last update. */
        @JvmField
        val STEPS: DeltaDataType<Long, IntervalDataPoint<Long>> = createIntervalDataType("Steps")

        /** Total steps taken since the start of the active exercise. */
        @JvmField
        val STEPS_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Steps")

        /** Number of steps taken while walking since the last update. */
        @JvmField
        val WALKING_STEPS: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Walking Steps")

        /**
         * Total number of steps taken while walking since the start of the current active exercise.
         */
        @JvmField
        val WALKING_STEPS_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Walking Steps")

        /** Number of steps taken while running since the last update. */
        @JvmField
        val RUNNING_STEPS: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Running Steps")

        /** Number of steps taken while running since the start of the current active exercise. */
        @JvmField
        val RUNNING_STEPS_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Running Steps")

        /** Step rate in steps/minute at a given point in time. */
        @JvmField
        val STEPS_PER_MINUTE: DeltaDataType<Long, SampleDataPoint<Long>> =
            createSampleDataType("Step per minute")

        /**
         * Statistics on step rate in steps/minute since the beginning of the current active
         * exercise.
         */
        @JvmField
        val STEPS_PER_MINUTE_STATS: AggregateDataType<Long, StatisticalDataPoint<Long>> =
            createStatsDataType("Step per minute")

        /** Number of swimming strokes taken since the last update. */
        @JvmField
        val SWIMMING_STROKES: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Swimming Strokes")

        /**
         * Total number of swimming strokes taken since the start of the current active exercise.
         */
        @JvmField
        val SWIMMING_STROKES_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Swimming Strokes")

        /** Number of calories burned (including basal rate and activity) since the last update. */
        @JvmField
        val CALORIES: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Calories")

        /**
         * Total number of calories burned (including basal rate and activity) since the start of
         * the current active exercise.
         */
        @JvmField
        val CALORIES_TOTAL: AggregateDataType<Double, CumulativeDataPoint<Double>> =
            createCumulativeDataType("Calories")

        /**
         * Pace at a specific point in time. Will be 0 if the user stops moving, otherwise the value
         * will be in milliseconds/kilometer.
         */
        @JvmField
        val PACE: DeltaDataType<Double, SampleDataPoint<Double>> = createSampleDataType("Pace")

        /**
         * Statistics on pace since the start of the current exercise. A value of 0 indicates the
         * user stopped moving, otherwise the value will be in milliseconds/kilometer.
         */
        @JvmField
        val PACE_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Pace")

        /**
         * The number of seconds the user has been resting during an exercise since the last update.
         */
        @JvmField
        val RESTING_EXERCISE_DURATION: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Resting Exercise Duration")

        /** The total number of seconds the user has been resting during the active exercise. */
        @JvmField
        val RESTING_EXERCISE_DURATION_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Resting Exercise Duration")

        /**
         * The total time the Exercise was [ExerciseState.ACTIVE] in seconds.
         *
         * **_Note_: this [DataType] is only intended to be used in conjunction with exercise goals.
         * [DataPoint]s will not be delivered for this [DataType]. If you want to query the active
         * duration, you should use [ExerciseUpdate.activeDuration] which is available in every
         * [ExerciseUpdate].**
         */
        @JvmField
        val ACTIVE_EXERCISE_DURATION_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Active Exercise Duration")

        /** Count of swimming laps since the last update. */
        @JvmField
        val SWIMMING_LAP_COUNT: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Swim Lap Count")

        /** Count of swimming laps since the start of the current active exercise. */
        @JvmField
        val SWIMMING_LAP_COUNT_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Swim Lap Count")

        /** The number of repetitions of an exercise performed since the last update. */
        @JvmField
        val REP_COUNT: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Rep Count")

        /**
         * The number of repetitions of an exercise performed since the start of the current active
         * exercise.
         */
        @JvmField
        val REP_COUNT_TOTAL: AggregateDataType<Long, CumulativeDataPoint<Long>> =
            createCumulativeDataType("Rep Count")

        /**
         * The amount of time during a single step that the runner's foot was in contact with the
         * ground in milliseconds in `long` format.
         */
        @JvmField
        val GROUND_CONTACT_TIME: DeltaDataType<Long, SampleDataPoint<Long>> =
            createSampleDataType("Ground Contact Time")

        /**
         * Statistics on the amount of time during a single step that the runner's foot was in
         * contact with the ground in milliseconds in `long` format.
         */
        @JvmField
        val GROUND_CONTACT_TIME_STATS: AggregateDataType<Long, StatisticalDataPoint<Long>> =
            createStatsDataType("Ground Contact Time")

        /**
         * Distance the center of mass moves up-and-down with each step in centimeters in `double`
         * format.
         */
        @JvmField
        val VERTICAL_OSCILLATION: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("Vertical Oscillation")

        /**
         * Statistic on distance the center of mass moves up-and-down with each step in centimeters
         * in `double` format.
         */
        @JvmField
        val VERTICAL_OSCILLATION_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Vertical Oscillation")

        /**
         * Vertical oscillation / stride length.
         *
         * For example, a vertical oscillation of 5.0cm and stride length .8m (80 cm) would have a
         * vertical ratio of 0.625.
         */
        @JvmField
        val VERTICAL_RATIO: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("Vertical Ratio")

        /**
         * Statistics on vertical oscillation / stride length.
         *
         * For example, a vertical oscillation of 5.0cm and stride length .8m (80 cm) would have a
         * vertical ratio of 0.625.
         */
        @JvmField
        val VERTICAL_RATIO_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Vertical Ratio")

        /** Distance covered by a single step in meters in `double` format. */
        @JvmField
        val STRIDE_LENGTH: DeltaDataType<Double, SampleDataPoint<Double>> =
            createSampleDataType("Stride Length")

        /** Statistics on distance covered by a single step in meters in `double` format. */
        @JvmField
        val STRIDE_LENGTH_STATS: AggregateDataType<Double, StatisticalDataPoint<Double>> =
            createStatsDataType("Stride Length")

        /**
         * The total step count over a day, where the previous day ends and a new day begins at
         * 12:00 AM local time. Each [DataPoint] of this type will cover the interval from the start
         * of day to now. In the event of time-zone shifts, the interval may be greater than 24hrs.
         */
        @JvmField
        val STEPS_DAILY: DeltaDataType<Long, IntervalDataPoint<Long>> =
            createIntervalDataType("Daily Steps")

        /**
         * The total number floors climbed over a day, where the previous day ends and a new day
         * begins at 12:00 AM local time. Each DataPoint of this type will cover the interval from
         * the start of day to now. In the event of time-zone shifts, the interval may be greater
         * than 24hrs.
         */
        @JvmField
        val FLOORS_DAILY: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Daily Floors")

        /**
         * The total gain in elevation over a day expressed in meters in `double` format, where the
         * previous day ends and a new day begins at 12:00 AM local time. Elevation losses are not
         * counted in this metric (so it will only be positive or 0). Each DataPoint of this type
         * will cover the interval from the start of day to now. In the event of time-zone shifts,
         * the interval might be greater than 24hrs.
         */
        @JvmField
        val ELEVATION_GAIN_DAILY: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Daily Elevation Gain")

        /**
         * The total number of calories over a day (including both BMR and active calories), where
         * the previous day ends and a new day begins at 12:00 AM local time. Each [DataPoint] of
         * this type will cover the interval from the start of day to now. In the event of time-zone
         * shifts, the interval might be greater than 24hrs.
         */
        @JvmField
        val CALORIES_DAILY: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Daily Calories")

        /**
         * The total distance over a day, where the previous day ends and a new day begins at 12:00
         * AM local time. Each DataPoint of this type will cover the interval from the start of day
         * to now. In the event of time-zone shifts, the interval may be greater than 24hrs.
         */
        @JvmField
        val DISTANCE_DAILY: DeltaDataType<Double, IntervalDataPoint<Double>> =
            createIntervalDataType("Daily Distance")

        internal val deltaDataTypes: Set<DeltaDataType<*, *>> =
            setOf(
                ABSOLUTE_ELEVATION,
                CALORIES,
                CALORIES_DAILY,
                DISTANCE_DAILY,
                ELEVATION_GAIN_DAILY,
                FLOORS_DAILY,
                STEPS_DAILY,
                DECLINE_DISTANCE,
                DECLINE_DURATION,
                DISTANCE,
                ELEVATION_GAIN,
                ELEVATION_LOSS,
                FLAT_GROUND_DISTANCE,
                FLAT_GROUND_DURATION,
                FLOORS,
                GOLF_SHOT_COUNT,
                GROUND_CONTACT_TIME,
                HEART_RATE_BPM,
                INCLINE_DISTANCE,
                INCLINE_DURATION,
                LOCATION,
                PACE,
                REP_COUNT,
                RESTING_EXERCISE_DURATION,
                RUNNING_STEPS,
                SPEED,
                STEPS,
                STEPS_PER_MINUTE,
                STRIDE_LENGTH,
                SWIMMING_LAP_COUNT,
                SWIMMING_STROKES,
                VERTICAL_OSCILLATION,
                VERTICAL_RATIO,
                VO2_MAX,
                WALKING_STEPS,
            )

        internal val aggregateDataTypes: Set<AggregateDataType<*, *>> =
            setOf(
                ABSOLUTE_ELEVATION_STATS,
                ACTIVE_EXERCISE_DURATION_TOTAL,
                CALORIES_TOTAL,
                DECLINE_DISTANCE_TOTAL,
                DECLINE_DURATION_TOTAL,
                DISTANCE_TOTAL,
                ELEVATION_GAIN_TOTAL,
                ELEVATION_LOSS_TOTAL,
                FLAT_GROUND_DISTANCE_TOTAL,
                FLAT_GROUND_DURATION_TOTAL,
                FLOORS_TOTAL,
                GOLF_SHOT_COUNT_TOTAL,
                GROUND_CONTACT_TIME_STATS,
                HEART_RATE_BPM_STATS,
                INCLINE_DISTANCE_TOTAL,
                INCLINE_DURATION_TOTAL,
                PACE_STATS,
                REP_COUNT_TOTAL,
                RESTING_EXERCISE_DURATION_TOTAL,
                RUNNING_STEPS_TOTAL,
                SPEED_STATS,
                STEPS_PER_MINUTE_STATS,
                STEPS_TOTAL,
                STRIDE_LENGTH_STATS,
                SWIMMING_LAP_COUNT_TOTAL,
                SWIMMING_STROKES_TOTAL,
                VERTICAL_OSCILLATION_STATS,
                VERTICAL_RATIO_STATS,
                VO2_MAX_STATS,
                WALKING_STEPS_TOTAL,
            )

        private val namesOfDeltasWithNoAggregate =
            deltaDataTypes.map { it.name } subtract aggregateDataTypes.map { it.name }.toSet()

        private val namesOfAggregatesWithNoDelta =
            aggregateDataTypes.map { it.name } subtract deltaDataTypes.map { it.name }.toSet()

        /** The format used for a [DataProto.Value] represented as a [Double]. */
        internal const val FORMAT_DOUBLE: Int = 1

        /** The format used for a [DataProto.Value] represented as an [Long]. */
        internal const val FORMAT_LONG: Int = 2

        /** The format used for a [DataProto.Value] represented as an [Boolean]. */
        internal const val FORMAT_BOOLEAN: Int = 4

        /** The format used for a [DataProto.Value] represented as a [DoubleArray]. */
        internal const val FORMAT_DOUBLE_ARRAY: Int = 3

        /** The format used for a [DataProto.Value] represented as a [ByteArray]. */
        internal const val FORMAT_BYTE_ARRAY: Int = 5

        /** A name prefix for custom data types. */
        internal const val CUSTOM_DATA_TYPE_PREFIX = "health_services.device_private"

        @Suppress("UNCHECKED_CAST")
        internal fun aggregateFromProto(
            proto: DataProto.DataType
        ): AggregateDataType<out Number, out DataPoint<out Number>> =
            aggregateDataTypes.firstOrNull { it.name == proto.name }
                ?: AggregateDataType(
                    proto.name,
                    TimeType.fromProto(proto.timeType),
                    protoDataTypeToClass(proto) as Class<Number>
                )

        internal fun deltaFromProto(
            proto: DataProto.DataType
        ): DeltaDataType<out Any, out DataPoint<out Any>> =
            deltaDataTypes.firstOrNull { it.name == proto.name }
                ?: DeltaDataType(
                    proto.name,
                    TimeType.fromProto(proto.timeType),
                    protoDataTypeToClass(proto)
                )

        internal fun deltaAndAggregateFromProto(
            proto: DataProto.DataType
        ): List<DataType<out Any, out DataPoint<out Any>>> {
            val list = mutableListOf<DataType<out Any, out DataPoint<out Any>>>()

            val isCustom = proto.name.startsWith(CUSTOM_DATA_TYPE_PREFIX)

            if (isCustom || !namesOfAggregatesWithNoDelta.contains(proto.name)) {
                list += deltaFromProto(proto)
            }
            if (!isCustom && !namesOfDeltasWithNoAggregate.contains(proto.name)) {
                list += aggregateFromProto(proto)
            }

            return list
        }

        private fun protoDataTypeToClass(proto: DataProto.DataType) =
            when (proto.format) {
                FORMAT_DOUBLE -> Double::class.java
                FORMAT_LONG -> Long::class.java
                FORMAT_BOOLEAN -> Boolean::class.java
                FORMAT_DOUBLE_ARRAY -> {
                    if (proto.name == LOCATION.name) LOCATION.valueClass
                    else DoubleArray::class.java
                }
                FORMAT_BYTE_ARRAY -> ByteArray::class.java
                else -> Nothing::class.java
            }
    }
}
