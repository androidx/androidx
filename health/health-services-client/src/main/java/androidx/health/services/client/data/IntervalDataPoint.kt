/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.health.services.client.proto.DataProto
import java.time.Duration
import java.time.Instant

/** Data point that includes just the delta from the previous data point for [dataType]. */
class IntervalDataPoint<T : Any>(
    /** The [DataType] this [DataPoint] represents. */
    override val dataType: DataType<T, out IntervalDataPoint<T>>,
    /** The value of this data point. */
    val value: T,
    /** The beginning of the time period this [DataPoint] represents. */
    val startDurationFromBoot: Duration,
    /** The end of the time period this [DataPoint] represents. */
    val endDurationFromBoot: Duration,
    /** OEM specific data. In general, this should not be relied upon by non-preloaded apps. */
    val metadata: Bundle = Bundle(),
    /** Accuracy of this DataPoint. */
    val accuracy: DataPointAccuracy? = null,
) : DataPoint<T>(dataType) {

    internal val proto: DataProto.DataPoint = getDataPointProto()

    private fun getDataPointProto(): DataProto.DataPoint {
        val builder =
            DataProto.DataPoint.newBuilder()
                .setDataType(dataType.proto)
                .setValue(dataType.toProtoFromValue(value))
                .setStartDurationFromBootMs(startDurationFromBoot.toMillis())
                .setEndDurationFromBootMs(endDurationFromBoot.toMillis())
                .setMetaData(BundlesUtil.toProto(metadata))

        accuracy?.let { builder.setAccuracy(it.proto) }

        return builder.build()
    }

    /**
     * Returns the start [Instant] of this [DataPoint], knowing the time at which the system booted.
     *
     * @param bootInstant the [Instant] at which the system booted, this can be computed by
     * `Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime()) `
     */
    fun getStartInstant(bootInstant: Instant): Instant {
        return bootInstant.plus(startDurationFromBoot)
    }

    /**
     * Returns the end [Instant] of this [DataPoint], knowing the time at which the system booted.
     *
     * @param bootInstant the [Instant] at which the system booted, this can be computed by
     * `Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())`
     */
    fun getEndInstant(bootInstant: Instant): Instant {
        return bootInstant.plus(endDurationFromBoot)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntervalDataPoint<*>) return false

        if (dataType != other.dataType) return false
        if (value != other.value) return false
        if (startDurationFromBoot != other.startDurationFromBoot) return false
        if (endDurationFromBoot != other.endDurationFromBoot) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + dataType.hashCode()
        result = (31 * result + startDurationFromBoot.toNanos()).toInt()
        result = (31 * result + endDurationFromBoot.toNanos()).toInt()
        return result
    }

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(proto: DataProto.DataPoint): IntervalDataPoint<*> {
            val dataType =
                DataType.deltaFromProto(proto.dataType) as DataType<Any, IntervalDataPoint<Any>>
            return IntervalDataPoint(
                dataType,
                dataType.toValueFromProto(proto.value),
                Duration.ofMillis(proto.startDurationFromBootMs),
                Duration.ofMillis(proto.endDurationFromBootMs),
                metadata = BundlesUtil.fromProto(proto.metaData),
                accuracy = if (proto.hasAccuracy()) {
                    DataPointAccuracy.fromProto(proto.accuracy)
                } else {
                    null
                },
            )
        }
    }
}
