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

import android.os.Parcelable
import androidx.health.services.client.data.DataType.TimeType
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_INTERVAL
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_SAMPLE
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_UNKNOWN

/**
 * A data type is a representation of health data managed by Health Services.
 *
 * A [DataType] specifies the format of the values inside a [DataPoint]. Health Services defines
 * data types for instantaneous observations [TimeType.SAMPLE](e.g. heart rate) and data types for
 * change between readings [TimeType.INTERVAL](e.g. distance).
 *
 * Note: the data type defines only the representation and format of the data, and not how it's
 * being collected, the sensor being used, or the parameters of the collection.
 */
@Suppress("ParcelCreator")
public class DataType(
    /** Returns the name of this [DataType], e.g. `"Steps"`. */
    public val name: String,
    /** Returns the [TimeType] of this [DataType]. */
    public val timeType: TimeType,
    /** Returns the expected format for a [Value] of this [DataType]. */
    public val format: Int,
) : ProtoParcelable<DataProto.DataType>() {
    /** @hide */
    public constructor(
        proto: DataProto.DataType
    ) : this(
        proto.name,
        TimeType.fromProto(proto.timeType)
            ?: throw IllegalStateException("Invalid TimeType: ${proto.timeType}"),
        proto.format
    )

    /**
     * Whether the `DataType` corresponds to a measurement spanning an interval, or a sample at a
     * single point in time.
     */
    public enum class TimeType {
        INTERVAL,
        SAMPLE;

        /** @hide */
        internal fun toProto(): DataProto.DataType.TimeType =
            when (this) {
                INTERVAL -> TIME_TYPE_INTERVAL
                SAMPLE -> TIME_TYPE_SAMPLE
            }

        internal companion object {
            /** @hide */
            internal fun fromProto(proto: DataProto.DataType.TimeType): TimeType? =
                when (proto) {
                    TIME_TYPE_INTERVAL -> INTERVAL
                    TIME_TYPE_SAMPLE -> SAMPLE
                    TIME_TYPE_UNKNOWN -> null
                }
        }
    }

    override fun toString(): String = "DataType(name=$name, timeType=$timeType, format=$format)"

    /** @hide */
    override val proto: DataProto.DataType by lazy {
        DataProto.DataType.newBuilder()
            .setName(name)
            .setTimeType(timeType.toProto())
            .setFormat(format)
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataType> = newCreator {
            val proto = DataProto.DataType.parseFrom(it)
            DataType(proto)
        }

        /**
         * A measure of the gain in elevation expressed in meters in `double` format. Elevation
         * losses are not counted in this metric (so it will only be positive or 0).
         */
        @JvmField
        public val ELEVATION_GAIN: DataType =
            DataType("Elevation Gain", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** Absolute elevation between each reading expressed in meters in `double` format. */
        @JvmField
        public val ABSOLUTE_ELEVATION: DataType =
            DataType("Absolute Elevation", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** A distance delta between each reading expressed in meters in `double` format. */
        @JvmField
        public val DISTANCE: DataType = DataType("Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A distance delta traveled over declining ground between each reading expressed in meters
         * in `double` format.
         */
        @JvmField
        public val DECLINE_DISTANCE: DataType =
            DataType("Decline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta representing the amount of time the user spent traveling over declining
         * ground during the interval, expressed in seconds in `long` format.
         */
        @JvmField
        public val DECLINE_DURATION: DataType =
            DataType("Decline Duration", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * A distance delta traveled over flat ground between each reading expressed in meters in
         * `double` format.
         */
        @JvmField
        public val FLAT_GROUND_DISTANCE: DataType =
            DataType("Flat Ground Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta representing the amount of time the user spent traveling over flat
         * ground during the interval, expressed in seconds in `long` format.
         */
        @JvmField
        public val FLAT_GROUND_DURATION: DataType =
            DataType("Flat Ground Duration", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * A distance delta traveled over inclining ground between each reading expressed in meters
         * in `double` format.
         */
        @JvmField
        public val INCLINE_DISTANCE: DataType =
            DataType("Incline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta representing the amount of time the user spent traveling over inclining
         * ground during the interval, expressed in seconds in `long` format.
         */
        @JvmField
        public val INCLINE_DURATION: DataType =
            DataType("Incline Duration", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** Number of floors climbed between each reading in `double` format */
        @JvmField
        public val FLOORS: DataType = DataType("Floors", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * Current heart rate, in beats per minute in `double` format.
         *
         * Accuracy for a [DataPoint] of type [DataType.HEART_RATE_BPM] is represented by
         * [HrAccuracy].
         */
        @JvmField
        public val HEART_RATE_BPM: DataType =
            DataType("HeartRate", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /**
         * Current latitude, longitude and optionally, altitude in `double[]` format. Latitude at
         * index [DataPoints.LOCATION_DATA_POINT_LATITUDE_INDEX], longitude at index
         * [DataPoints.LOCATION_DATA_POINT_LONGITUDE_INDEX] and if available, altitude at index
         * [DataPoints.LOCATION_DATA_POINT_ALTITUDE_INDEX]
         *
         * Accuracy for a [DataPoint] of type [DataType.LOCATION] is represented by
         * [LocationAccuracy].
         */
        @JvmField
        public val LOCATION: DataType =
            DataType("Location", TimeType.SAMPLE, Value.FORMAT_DOUBLE_ARRAY)

        /** Current speed over time. In meters/second in `double` format. */
        @JvmField
        public val SPEED: DataType = DataType("Speed", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** Percentage of oxygen in the blood in `double` format. Valid range `0f` - `100f`. */
        @JvmField public val SPO2: DataType = DataType("SpO2", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** Rate of oxygen consumption in `double` format. Valid range `0f` - `100f`. */
        @JvmField public val VO2: DataType = DataType("VO2", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /**
         * Maximum rate of oxygen consumption measured during incremental exercise in `double`
         * format. Valid range `0f` - `100f`.
         */
        @JvmField
        public val VO2_MAX: DataType = DataType("VO2 Max", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** Delta of steps between each reading in `long` format. */
        @JvmField
        public val STEPS: DataType = DataType("Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** Delta of walking steps between each reading in `long` format. */
        @JvmField
        public val WALKING_STEPS: DataType =
            DataType("Walking Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** Delta of running steps between each reading in `long` format. */
        @JvmField
        public val RUNNING_STEPS: DataType =
            DataType("Running Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** Current step rate in steps/minute in `long` format. */
        @JvmField
        public val STEPS_PER_MINUTE: DataType =
            DataType("Step per minute", TimeType.SAMPLE, Value.FORMAT_LONG)

        /** Delta of strokes between each reading of swimming strokes in `long` format. */
        @JvmField
        public val SWIMMING_STROKES: DataType =
            DataType("Swimming Strokes", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * Delta of total calories (including basal rate and activity) between each reading in
         * `double` format.
         */
        @JvmField
        public val TOTAL_CALORIES: DataType =
            DataType("Calories", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** Current pace. In millisec/km in `double` format. */
        @JvmField public val PACE: DataType = DataType("Pace", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /**
         * The duration during which the user was resting during an Exercise in seconds in `long`
         * format.
         */
        @JvmField
        public val RESTING_EXERCISE_DURATION: DataType =
            DataType("Resting Exercise Duration", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The duration of the time the Exercise was ACTIVE in seconds in `long` format. */
        @JvmField
        public val ACTIVE_EXERCISE_DURATION: DataType =
            DataType("Active Exercise Duration", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** Count of swimming laps ins `long` format. */
        @JvmField
        public val SWIMMING_LAP_COUNT: DataType =
            DataType("Swim Lap Count", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The current rep count of the exercise in `long` format. */
        @JvmField
        public val REP_COUNT: DataType = DataType("Rep Count", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * The total step count over a day in `long` format, where the previous day ends and a new
         * day begins at 12:00 AM local time. Each DataPoint of this type will cover the interval
         * from the start of day to now. In the event of time-zone shifts, the interval might be
         * greater than 24hrs.
         */
        @JvmField
        public val DAILY_STEPS: DataType =
            DataType("Daily Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * The total number floors climbed over a day in `double` format, where the previous day
         * ends and a new day begins at 12:00 AM local time. Each DataPoint of this type will cover
         * the interval from the start of day to now. In the event of time-zone shifts, the interval
         * might be greater than 24hrs.
         */
        @JvmField
        public val DAILY_FLOORS: DataType =
            DataType("Daily Floors", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The total number calories over a day in `double` format, where the previous day ends and
         * a new day begins at 12:00 AM local time. Each DataPoint of this type will cover the
         * interval from the start of day to now. In the event of time-zone shifts, the interval
         * might be greater than 24hrs.
         */
        @JvmField
        public val DAILY_CALORIES: DataType =
            DataType("Daily Calories", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The total distance over a day in `double` format, where the previous day ends and a new
         * day begins at 12:00 AM local time. Each DataPoint of this type will cover the interval
         * from the start of day to now. In the event of time-zone shifts, the interval might be
         * greater than 24hrs.
         */
        @JvmField
        public val DAILY_DISTANCE: DataType =
            DataType("Daily Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)
    }
}
