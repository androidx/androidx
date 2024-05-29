/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.HealthEvent.MetricsEntry
import java.time.Instant

/** Represents a user's health event. */
public class HealthEvent(
    /** Gets the type of event. */
    public val type: Type,

    /** Returns the time of the health event. */
    public val eventTime: Instant,

    /** Gets metrics associated to the event. */
    public val metrics: DataPointContainer,
) {

    /** Health event types. */
    public class Type
    @RestrictTo(Scope.LIBRARY)
    constructor(
        /** Returns a unique identifier for the [Type], as an `int`. */
        public val id: Int,

        /** Returns a human readable name to represent this [Type]. */
        public val name: String
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Type) return false
            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int = id

        override fun toString(): String = name

        internal fun toProto(): Int = id

        public companion object {
            private const val CUSTOM_TYPE_NAME_PREFIX = "health_services.device_private."
            /**
             * The Health Event is unknown, or is represented by a value too new for this library
             * version to parse.
             */
            @JvmField public val UNKNOWN: Type = Type(0, "UNKNOWN")

            /** Health Event signifying the device detected that the user fell. */
            @JvmField public val FALL_DETECTED: Type = Type(3, "FALL_DETECTED")

            @JvmField internal val VALUES: List<Type> = listOf(UNKNOWN, FALL_DETECTED)

            internal fun fromProto(proto: DataProto.HealthEvent.HealthEventType): Type =
                VALUES.firstOrNull { it.id == proto.number } ?: UNKNOWN

            internal fun fromProto(typeId: Int): Type {
                if (isInCustomHealthEventRange(typeId)) {
                    return Type(typeId, CUSTOM_TYPE_NAME_PREFIX + typeId)
                }

                return VALUES.firstOrNull { it.id == typeId } ?: UNKNOWN
            }

            private fun isInCustomHealthEventRange(id: Int) = id in 0x40000..0x4ffff
        }
    }

    internal constructor(
        proto: DataProto.HealthEvent
    ) : this(
        Type.fromProto(proto.healthEventTypeId),
        Instant.ofEpochMilli(proto.eventTimeEpochMs),
        fromHealthEventProto(proto)
    )

    internal val proto: DataProto.HealthEvent =
        DataProto.HealthEvent.newBuilder()
            .setHealthEventTypeId(type.toProto())
            .setEventTimeEpochMs(eventTime.toEpochMilli())
            .addAllMetrics(toEventProtoList(metrics))
            .build()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthEvent) return false
        if (type != other.type) return false
        if (eventTime != other.eventTime) return false
        if (metrics != other.metrics) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + eventTime.hashCode()
        result = 31 * result + metrics.hashCode()
        return result
    }

    internal companion object {
        internal fun toEventProtoList(container: DataPointContainer): List<MetricsEntry> {
            val list = mutableListOf<MetricsEntry>()

            for (entry in container.dataPoints) {
                if (entry.value.isEmpty()) {
                    continue
                }

                when (entry.key.timeType) {
                    DataType.TimeType.SAMPLE -> {
                        list.add(
                            MetricsEntry.newBuilder()
                                .setDataType(entry.key.proto)
                                .addAllDataPoints(entry.value.map { (it as SampleDataPoint).proto })
                                .build()
                        )
                    }
                    DataType.TimeType.INTERVAL -> {
                        list.add(
                            MetricsEntry.newBuilder()
                                .setDataType(entry.key.proto)
                                .addAllDataPoints(
                                    entry.value.map { (it as IntervalDataPoint).proto }
                                )
                                .build()
                        )
                    }
                }
            }
            return list.sortedBy { it.dataType.name } // Required to ensure equals() works
        }

        internal fun fromHealthEventProto(proto: DataProto.HealthEvent): DataPointContainer {
            val dataTypeToDataPoints: Map<DataType<*, *>, List<DataPoint<*>>> =
                proto.metricsList.associate { entry ->
                    DataType.deltaFromProto(entry.dataType) to
                        entry.dataPointsList.map { DataPoint.fromProto(it) }
                }
            return DataPointContainer(dataTypeToDataPoints)
        }
    }
}
