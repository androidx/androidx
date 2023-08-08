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

/**
 * Data point that represents a piece of data that was valid at a single point in time, for example
 * heart rate or speed.
 */
public class SampleDataPoint<T : Any>(
    /** The [DataType] this [DataPoint] represents. */
    public override val dataType: DataType<T, SampleDataPoint<T>>,
    /** The value of this data point. */
    public val value: T,
    /** The time this [DataPoint] represents. */
    public val timeDurationFromBoot: Duration,
    /** OEM specific data. In general, this should not be relied upon by non-preloaded apps. */
    public val metadata: Bundle = Bundle(),
    /** Accuracy of this DataPoint. */
    public val accuracy: DataPointAccuracy? = null,
) : DataPoint<T>(dataType) {

    internal val proto: DataProto.DataPoint = getDataPointProto()

    private fun getDataPointProto(): DataProto.DataPoint {
        val builder =
            DataProto.DataPoint.newBuilder()
                .setDataType(dataType.proto)
                .setValue(dataType.toProtoFromValue(value))
                .setStartDurationFromBootMs(timeDurationFromBoot.toMillis())
                .setEndDurationFromBootMs(timeDurationFromBoot.toMillis())
                .setMetaData(BundlesUtil.toProto(metadata))

        accuracy?.let { builder.setAccuracy(it.proto) }

        return builder.build()
    }

    /**
     * Returns the time [Instant] of this [DataPoint], knowing the time at which the system booted.
     *
     * @param bootInstant the [Instant] at which the system booted, this can be computed by
     * `Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime()) `
     */
    public fun getTimeInstant(bootInstant: Instant): Instant {
        return bootInstant.plus(timeDurationFromBoot)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SampleDataPoint<*>) return false

        if (dataType != other.dataType) return false
        if (value != other.value) return false
        if (accuracy != other.accuracy) return false
        if (timeDurationFromBoot != other.timeDurationFromBoot) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + timeDurationFromBoot.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (accuracy?.hashCode() ?: 0)
        return result
    }

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(proto: DataProto.DataPoint): SampleDataPoint<*> {
            val dataType =
                DataType.deltaFromProto(proto.dataType) as DataType<Any, SampleDataPoint<Any>>
            return SampleDataPoint(
                dataType,
                dataType.toValueFromProto(proto.value),
                Duration.ofMillis(proto.startDurationFromBootMs),
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
