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
 * Data point that represents statistics on [SampleDataPoint]s between [start] and [end], though it
 * is not required to request samples separately.
 */
class StatisticalDataPoint<T : Number>(
    /** The [DataType] this [DataPoint] represents. */
    dataType: AggregateDataType<T, StatisticalDataPoint<T>>,
    /** The minimum observed value between [start] and [end]. */
    val min: T,
    /** The maximum observed value between [start] and [end]. */
    val max: T,
    /** The average observed value between [start] and [end]. */
    val average: T,
    /** The beginning of time this point covers.  */
    val start: Instant,
    /** The end time this point covers.  */
    val end: Instant
) : DataPoint<T>(dataType) {

    internal val proto: DataProto.AggregateDataPoint =
        DataProto.AggregateDataPoint.newBuilder()
            .setStatisticalDataPoint(
                DataProto.AggregateDataPoint.StatisticalDataPoint.newBuilder()
                    .setDataType(dataType.proto)
                    .setMinValue(dataType.toProtoFromValue(min))
                    .setMaxValue(dataType.toProtoFromValue(max))
                    .setAvgValue(dataType.toProtoFromValue(average))
                    .setStartTimeEpochMs(start.toEpochMilli())
                    .setEndTimeEpochMs(end.toEpochMilli())
            ).build()

    companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(
            proto: DataProto.AggregateDataPoint.StatisticalDataPoint
        ): StatisticalDataPoint<*> {
            val dataType =
                DataType.aggregateFromProto(proto.dataType)
                    as AggregateDataType<Number, StatisticalDataPoint<Number>>
            return StatisticalDataPoint(
                dataType,
                min = dataType.toValueFromProto(proto.minValue),
                max = dataType.toValueFromProto(proto.maxValue),
                average = dataType.toValueFromProto(proto.avgValue),
                start = Instant.ofEpochMilli(proto.startTimeEpochMs),
                end = Instant.ofEpochMilli(proto.endTimeEpochMs)
            )
        }
    }
}
