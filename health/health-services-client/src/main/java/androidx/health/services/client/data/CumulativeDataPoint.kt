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

import androidx.health.services.client.proto.DataProto
import java.time.Instant

/**
 * A [DataPoint] containing a cumulative [total] for the type [dataType] between [start] and [end].
 * Unlike [IntervalDataPoint], this is guaranteed to increase over time (assuming the same [start]
 * value.) For example, an [IntervalDataPoint] for [DataType.STEPS]
 */
class CumulativeDataPoint<T : Number>(
    /** The [DataType] this [DataPoint] represents. */
    dataType: AggregateDataType<T, CumulativeDataPoint<T>>,
    /** The accumulated value between [start] and [end]. */
    val total: T,
    /** The beginning of the time period this [DataPoint] represents. */
    val start: Instant,
    /** The end of the time period this [DataPoint] represents. */
    val end: Instant
) : DataPoint<T>(dataType) {

    internal val proto: DataProto.AggregateDataPoint =
        DataProto.AggregateDataPoint.newBuilder()
            .setCumulativeDataPoint(
                DataProto.AggregateDataPoint.CumulativeDataPoint.newBuilder()
                    .setDataType(dataType.proto)
                    .setStartTimeEpochMs(start.toEpochMilli())
                    .setEndTimeEpochMs(end.toEpochMilli())
                    .setTotal(dataType.toProtoFromValue(total))
            ).build()

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(
            proto: DataProto.AggregateDataPoint.CumulativeDataPoint
        ): CumulativeDataPoint<*> {
            val dataType =
                DataType.aggregateFromProto(proto.dataType)
                    as AggregateDataType<Number, CumulativeDataPoint<Number>>
            return CumulativeDataPoint(
                dataType,
                dataType.toValueFromProto(proto.total),
                start = Instant.ofEpochMilli(proto.startTimeEpochMs),
                end = Instant.ofEpochMilli(proto.endTimeEpochMs)
            )
        }
    }
}
