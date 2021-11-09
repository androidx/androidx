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
import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import java.time.Duration
import java.time.Instant

/**
 * A data point containing a [value] of type [dataType] from either a single point in time:
 * [DataType.TimeType.SAMPLE], or a range in time: [DataType.TimeType.INTERVAL].
 */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class DataPoint
internal constructor(
    public val dataType: DataType,
    public val value: Value,

    /**
     * Elapsed start time of this [DataPoint].
     *
     * This represents the time at which this [DataPoint] originated, as a [Duration] since boot
     * time. This is not exposed as a timestamp as the clock may drift between when the data is
     * generated and when it is read out. Use [getStartInstant] to get the start time of this
     * [DataPoint] as an [Instant].
     */
    public val startDurationFromBoot: Duration,

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
    public val endDurationFromBoot: Duration = startDurationFromBoot,

    /** Returns any provided metadata of this [DataPoint]. */
    public val metadata: Bundle = Bundle(),

    /**
     * Returns the accuracy of this [DataPoint].
     *
     * The specific [DataPointAccuracy] implementation this refers to depends on the [DataType] of
     * the data point. For example, accuracy of [DataType.LOCATION] data points is represented by
     * [LocationAccuracy]. If there is no associated [DataPointAccuracy] for the [DataType], this
     * will return `null`.
     */
    public val accuracy: DataPointAccuracy? = null,
) : ProtoParcelable<DataProto.DataPoint>() {

    internal constructor(
        proto: DataProto.DataPoint
    ) : this(
        DataType(proto.dataType),
        Value(proto.value),
        Duration.ofMillis(proto.startDurationFromBootMs),
        Duration.ofMillis(proto.endDurationFromBootMs),
        BundlesUtil.fromProto(proto.metaData),
        if (proto.hasAccuracy()) DataPointAccuracy.fromProto(proto.accuracy) else null
    )

    /** @hide */
    override val proto: DataProto.DataPoint by lazy {
        val builder =
            DataProto.DataPoint.newBuilder()
                .setDataType(dataType.proto)
                .setValue(value.proto)
                .setStartDurationFromBootMs(startDurationFromBoot.toMillis())
                .setEndDurationFromBootMs(endDurationFromBoot.toMillis())
                .setMetaData(BundlesUtil.toProto(metadata))

        accuracy?.let { builder.setAccuracy(it.proto) }

        builder.build()
    }

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

    override fun toString(): String =
        "DataPoint(" +
            "dataType=$dataType, " +
            "value=$value, " +
            "startDurationFromBoot=$startDurationFromBoot, " +
            "endDurationFromBoot=$endDurationFromBoot, " +
            "accuracy=$accuracy)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataPoint> = newCreator {
            val proto = DataProto.DataPoint.parseFrom(it)
            DataPoint(proto)
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
            metadata: Bundle = Bundle(),
            accuracy: DataPointAccuracy? = null
        ): DataPoint {
            require(DataType.TimeType.INTERVAL == dataType.timeType) {
                "DataType $dataType must be of interval type to be created with an interval"
            }

            require(endDurationFromBoot >= startDurationFromBoot) {
                "End timestamp mustn't be earlier than start timestamp, but got " +
                    "$startDurationFromBoot and $endDurationFromBoot"
            }

            return DataPoint(
                dataType,
                value,
                startDurationFromBoot,
                endDurationFromBoot,
                metadata,
                accuracy
            )
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
            metadata: Bundle = Bundle(),
            accuracy: DataPointAccuracy? = null
        ): DataPoint {
            require(DataType.TimeType.SAMPLE == dataType.timeType) {
                "DataType $dataType must be of sample type to be created with a single timestamp"
            }

            return DataPoint(
                dataType,
                value,
                durationFromBoot,
                endDurationFromBoot = durationFromBoot,
                metadata,
                accuracy
            )
        }
    }
}
