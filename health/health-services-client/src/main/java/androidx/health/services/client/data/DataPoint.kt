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
import android.os.Parcel
import android.os.Parcelable
import java.time.Duration
import java.time.Instant
import java.util.Objects

/**
 * A data point containing a [value] of type [dataType] from either a single point in time:
 * [DataType.TimeType.SAMPLE], or a range in time: [DataType.TimeType.INTERVAL].
 */
@Suppress("DataClassPrivateConstructor")
public data class DataPoint
internal constructor(
    val dataType: DataType,
    val value: Value,

    /**
     * Elapsed start time of this [DataPoint].
     *
     * This represents the time at which this [DataPoint] originated, as a [Duration] since boot
     * time. This is not exposed as a timestamp as the clock may drift between when the data is
     * generated and when it is read out. Use [getStartInstant] to get the start time of this
     * [DataPoint] as an [Instant].
     */
    val startDurationFromBoot: Duration,

    /**
     * Elapsed end time of this [DataPoint].
     *
     * This represents the time at which this [DataPoint] ends, as a [Duration] since boot time.
     * This is not exposed as a timestamp as the clock may drift between when the data is generated
     * and when it is read out. Use [getStartInstant] to get the start time of this [DataPoint] as
     * an [Instant].
     *
     * For instantaneous data points, this is equal to [startDurationFromBoot].
     */
    val endDurationFromBoot: Duration = startDurationFromBoot,

    /** Returns any provided metadata of this [DataPoint]. */
    val metadata: Bundle = Bundle(),
) : Parcelable {

    init {
        require(dataType.format == value.format) {
            "DataType and Value format must match, but got ${dataType.format} and ${value.format}"
        }
    }

    /**
     * Returns the start [Instant] of this [DataPoint], knowing the time at which the system booted.
     *
     * @param bootInstant the [Instant] at which the system booted, this can be computed by
     * `Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime()) `
     */
    public fun getStartInstant(bootInstant: Instant): Instant {
        return bootInstant.plus(startDurationFromBoot)
    }

    /**
     * Returns the end [Instant] of this [DataPoint], knowing the time at which the system booted.
     *
     * @param bootInstant the [Instant] at which the system booted, this can be computed by
     * `Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())`
     */
    public fun getEndInstant(bootInstant: Instant): Instant {
        return bootInstant.plus(endDurationFromBoot)
    }

    // TODO(b/180612514): Bundle doesn't have equals, so we need to override the data class default.
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is DataPoint) {
            return dataType == other.dataType &&
                value == other.value &&
                startDurationFromBoot == other.startDurationFromBoot &&
                endDurationFromBoot == other.endDurationFromBoot &&
                BundlesUtil.equals(metadata, other.metadata)
        }
        return false
    }

    // TODO(b/180612514): Bundle doesn't have hashCode, so we need to override the data class
    // default.
    override fun hashCode(): Int {
        return Objects.hash(
            dataType,
            value,
            startDurationFromBoot,
            endDurationFromBoot,
            BundlesUtil.hashCode(metadata)
        )
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(dataType, flags)
        dest.writeParcelable(value, flags)
        dest.writeLong(startDurationFromBoot.toNanos())
        dest.writeLong(endDurationFromBoot.toNanos())
        dest.writeBundle(metadata)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataPoint> =
            object : Parcelable.Creator<DataPoint> {
                override fun createFromParcel(parcel: Parcel): DataPoint? {
                    val dataType: DataType =
                        parcel.readParcelable(DataType::class.java.classLoader) ?: return null
                    val value: Value =
                        parcel.readParcelable(Value::class.java.classLoader) ?: return null
                    val startDurationFromBoot = Duration.ofNanos(parcel.readLong())
                    val endDurationFromBoot = Duration.ofNanos(parcel.readLong())
                    val metadata: Bundle? = parcel.readBundle(Bundle::class.java.classLoader)

                    return when (dataType.timeType) {
                        DataType.TimeType.INTERVAL ->
                            createInterval(
                                dataType,
                                value,
                                startDurationFromBoot,
                                endDurationFromBoot,
                                metadata ?: Bundle()
                            )
                        DataType.TimeType.SAMPLE -> {
                            require(endDurationFromBoot.compareTo(startDurationFromBoot) == 0) {
                                "DataType [$dataType] has SAMPLE type, but" +
                                    " start[$startDurationFromBoot]/end[$endDurationFromBoot]" +
                                    " duration from boot are not the same"
                            }
                            createSample(
                                dataType,
                                value,
                                startDurationFromBoot,
                                metadata ?: Bundle()
                            )
                        }
                    }
                }

                override fun newArray(size: Int): Array<DataPoint?> {
                    return arrayOfNulls(size)
                }
            }

        /**
         * Returns a [DataPoint] representing the [value] of type [dataType] from
         * [startDurationFromBoot] to [endDurationFromBoot].
         *
         * @throws IllegalArgumentException if the [DataType.TimeType] of the associated [DataType]
         * is not [DataType.TimeType.INTERVAL], or if data is malformed
         */
        @JvmStatic
        @JvmOverloads
        public fun createInterval(
            dataType: DataType,
            value: Value,
            startDurationFromBoot: Duration,
            endDurationFromBoot: Duration,
            metadata: Bundle = Bundle()
        ): DataPoint {
            require(DataType.TimeType.INTERVAL == dataType.timeType) {
                "DataType $dataType must be of interval type to be created with an interval"
            }

            require(endDurationFromBoot >= startDurationFromBoot) {
                "End timestamp mustn't be earlier than start timestamp, but got" +
                    " $startDurationFromBoot and $endDurationFromBoot"
            }

            return DataPoint(dataType, value, startDurationFromBoot, endDurationFromBoot, metadata)
        }

        /**
         * Returns a [DataPoint] representing the [value] of type [dataType] at [durationFromBoot].
         *
         * @throws IllegalArgumentException if the [DataType.TimeType] of the associated [DataType]
         * is not [DataType.TimeType.SAMPLE], or if data is malformed
         */
        @JvmStatic
        @JvmOverloads
        public fun createSample(
            dataType: DataType,
            value: Value,
            durationFromBoot: Duration,
            metadata: Bundle = Bundle()
        ): DataPoint {
            require(DataType.TimeType.SAMPLE == dataType.timeType) {
                "DataType $dataType must be of sample type to be created with a single timestamp"
            }

            return DataPoint(
                dataType,
                value,
                durationFromBoot,
                endDurationFromBoot = durationFromBoot,
                metadata
            )
        }
    }
}
