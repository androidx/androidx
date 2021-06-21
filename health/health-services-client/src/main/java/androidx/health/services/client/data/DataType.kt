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

import android.os.Parcel
import android.os.Parcelable

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
public data class DataType(
    /** Returns the name of this [DataType], e.g. `"Steps"`. */
    val name: String,
    /** Returns the [TimeType] of this [DataType]. */
    val timeType: TimeType,
    /** Returns the expected format for a [Value] of this [DataType]. */
    val format: Int,
) : Parcelable {

    /**
     * Whether the `DataType` corresponds to a measurement spanning an interval, or a sample at a
     * single point in time.
     */
    public enum class TimeType {
        INTERVAL,
        SAMPLE
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeString(name)
            writeString(timeType.name)
            writeInt(format)
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataType> =
            object : Parcelable.Creator<DataType> {
                override fun createFromParcel(parcel: Parcel): DataType? {
                    return DataType(
                        parcel.readString() ?: return null,
                        TimeType.valueOf(parcel.readString() ?: return null),
                        parcel.readInt()
                    )
                }

                override fun newArray(size: Int): Array<DataType?> {
                    return arrayOfNulls(size)
                }
            }

        /** Current altitude expressed in meters in `double` format. */
        @JvmField
        public val ALTITUDE: DataType = DataType("Altitude", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** A distance delta between each reading expressed in meters in `double` format. */
        @JvmField
        public val DISTANCE: DataType = DataType("Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta during an exercise over which the user was traveling down a decline,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val DECLINE_TIME: DataType =
            DataType("Decline Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * A distance delta traveled over declining ground between each reading expressed in meters
         * in `double` format.
         */
        @JvmField
        public val DECLINE_DISTANCE: DataType =
            DataType("Decline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta during an exercise over which the user was traveling across flat ground,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val FLAT_TIME: DataType = DataType("Flat Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * A distance delta traveled over flat ground between each reading expressed in meters in
         * `double` format.
         */
        @JvmField
        public val FLAT_DISTANCE: DataType =
            DataType("Flat Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * A duration delta during an exercise over which the user was traveling up an incline,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val INCLINE_TIME: DataType =
            DataType("Incline Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * A distance delta traveled over inclining ground between each reading expressed in meters
         * in `double` format.
         */
        @JvmField
        public val INCLINE_DISTANCE: DataType =
            DataType("Incline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** An elevation delta between each reading expressed in meters in `double` format. */
        @JvmField
        public val ELEVATION: DataType =
            DataType("Elevation", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** Absolute elevation between each reading expressed in meters in `double` format. */
        @JvmField
        public val ABSOLUTE_ELEVATION: DataType =
            DataType("Absolute Elevation", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** Number of floors climbed between each reading in `double` format */
        @JvmField
        public val FLOORS: DataType = DataType("Floors", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** Current heart rate, in beats per minute in `double` format. */
        @JvmField
        public val HEART_RATE_BPM: DataType =
            DataType("HeartRate", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /**
         * Current latitude, longitude and optionally, altitude in `double[]` format. Latitude at
         * index [DataPoints.LOCATION_DATA_POINT_LATITUDE_INDEX], longitude at index
         * [DataPoints.LOCATION_DATA_POINT_LONGITUDE_INDEX] and if available, altitude at index
         * [DataPoints.LOCATION_DATA_POINT_ALTITUDE_INDEX]
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

        /** The aggregate distance over a period of time expressed in meters in `double` format. */
        @JvmField
        public val AGGREGATE_DISTANCE: DataType =
            DataType("Aggregate Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The aggregate flat distance over a period of time expressed in meters in `double` format.
         */
        @JvmField
        public val AGGREGATE_FLAT_DISTANCE: DataType =
            DataType("Aggregate Flat Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The aggregate incline distance over a period of time expressed in meters in `double`
         * format.
         */
        @JvmField
        public val AGGREGATE_INCLINE_DISTANCE: DataType =
            DataType("Aggregate Incline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The aggregate incline distance over a period of time expressed in meters in `double`
         * format.
         */
        @JvmField
        public val AGGREGATE_DECLINE_DISTANCE: DataType =
            DataType("Aggregate Decline Distance", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /**
         * The aggregate duration for an exercise when the user was traveling on flat ground,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val AGGREGATE_FLAT_TIME: DataType =
            DataType("Aggregate Flat Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * The aggregate duration for an exercise when the user was traveling up an incline,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val AGGREGATE_INCLINE_TIME: DataType =
            DataType("Aggregate Incline Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * The aggregate duration for an exercise when the user was traveling down a decline,
         * expressed in seconds in `long` format.
         */
        @JvmField
        public val AGGREGATE_DECLINE_TIME: DataType =
            DataType("Aggregate Decline Time", TimeType.INTERVAL, Value.FORMAT_LONG)

        /**
         * The aggregate total calories (including basal rate and activity) expended over a period
         * of time in `double` format.
         */
        @JvmField
        public val AGGREGATE_CALORIES_EXPENDED: DataType =
            DataType("Aggregate Calories", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** The aggregate step count over a period of time in `long` format. */
        @JvmField
        public val AGGREGATE_STEP_COUNT: DataType =
            DataType("Aggregate Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The aggregate walking step count over a period of time in `long` format. */
        @JvmField
        public val AGGREGATE_WALKING_STEP_COUNT: DataType =
            DataType("Aggregate Walking Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The aggregate running step count over a period of time in `long` format. */
        @JvmField
        public val AGGREGATE_RUNNING_STEP_COUNT: DataType =
            DataType("Aggregate Running Steps", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The aggregate swimming stroke count over a period of time in `long` format. */
        @JvmField
        public val AGGREGATE_SWIMMING_STROKE_COUNT: DataType =
            DataType("Aggregate Swimming Strokes", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The aggregate elevation over a period of time in meters in `double` format. */
        @JvmField
        public val AGGREGATE_ELEVATION: DataType =
            DataType("Aggregate Elevation", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** The number of floors climbed over a period of time in `double` format */
        @JvmField
        public val AGGREGATE_FLOORS: DataType =
            DataType("Aggregate Floors", TimeType.INTERVAL, Value.FORMAT_DOUBLE)

        /** The average pace over a period of time in millisec/km in `double` format. */
        @JvmField
        public val AVERAGE_PACE: DataType =
            DataType("Average Pace", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The average speed over a period of time in meters/second in `double` format. */
        @JvmField
        public val AVERAGE_SPEED: DataType =
            DataType("Average Speed", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The average heart rate over a period of time in beats/minute in `double` format. */
        @JvmField
        public val AVERAGE_HEART_RATE_BPM: DataType =
            DataType("Average Heart Rate BPM", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The maximum altitude over a period of time in meters in `double` format. */
        @JvmField
        public val MAX_ALTITUDE: DataType =
            DataType("Max Altitude", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The minimum altitude over a period of time in meters in `double` format. */
        @JvmField
        public val MIN_ALTITUDE: DataType =
            DataType("Min Altitude", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The maximum pace over a period of time in millisec/km in `double` format. */
        @JvmField
        public val MAX_PACE: DataType = DataType("Max Pace", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The maximum speed over a period of time in meters/second in `double` format. */
        @JvmField
        public val MAX_SPEED: DataType = DataType("Max Speed", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /** The maximum instantaneous heart rate in beats/minute in `double` format. */
        @JvmField
        public val MAX_HEART_RATE_BPM: DataType =
            DataType("Max Heart Rate", TimeType.SAMPLE, Value.FORMAT_DOUBLE)

        /**
         * The duration during which the user was resting during an Exercise in seconds in `long`
         * format.
         */
        @JvmField
        public val RESTING_EXERCISE_DURATION: DataType =
            DataType("Resting Exercise Duration", TimeType.SAMPLE, Value.FORMAT_LONG)

        /** The duration of the time the Exercise was ACTIVE in seconds in `long` format. */
        @JvmField
        public val ACTIVE_EXERCISE_DURATION: DataType =
            DataType("Active Exercise Duration", TimeType.SAMPLE, Value.FORMAT_LONG)

        /** Count of swimming laps ins `long` format. */
        @JvmField
        public val SWIMMING_LAP_COUNT: DataType =
            DataType("Swim Lap Count", TimeType.INTERVAL, Value.FORMAT_LONG)

        /** The current rep count of the exercise in `long` format. */
        @JvmField
        public val REP_COUNT: DataType = DataType("Rep Count", TimeType.INTERVAL, Value.FORMAT_LONG)
    }
}
