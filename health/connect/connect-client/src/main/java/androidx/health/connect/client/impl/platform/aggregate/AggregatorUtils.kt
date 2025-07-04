/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.impl.platform.div
import androidx.health.connect.client.impl.platform.duration
import androidx.health.connect.client.impl.platform.isWithin
import androidx.health.connect.client.impl.platform.minus
import androidx.health.connect.client.impl.platform.toInstantWithDefaultZoneFallback
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SeriesRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import java.time.Instant
import kotlin.math.max

internal object AggregatorUtils {

    internal fun contributesToAggregation(record: Record, timeRange: TimeRange<*>): Boolean {
        return when (record) {
            is BloodPressureRecord -> record.time.isWithin(timeRange, record.zoneOffset)
            is NutritionRecord -> record.transFat != null && sliceFactor(record, timeRange) > 0
            is SeriesRecord<*> ->
                record.samples.any { it.time.isWithin(timeRange, record.startZoneOffset) }
            else ->
                error(
                    "Unsupported record type for aggregation fallback: ${RECORDS_CLASS_NAME_MAP[record::class]}"
                )
        }
    }

    internal fun sliceFactor(record: IntervalRecord, timeRange: TimeRange<*>): Double {
        val rangeStartTime =
            when (timeRange) {
                is InstantTimeRange -> timeRange.startTime
                is LocalTimeRange ->
                    timeRange.startTime.toInstantWithDefaultZoneFallback(record.startZoneOffset)
            }

        val rangeEndTime =
            when (timeRange) {
                is InstantTimeRange -> timeRange.endTime
                is LocalTimeRange ->
                    timeRange.endTime.toInstantWithDefaultZoneFallback(record.endZoneOffset)
            }

        return max(
            0.0,
            (minOf(record.endTime, rangeEndTime) - maxOf(record.startTime, rangeStartTime)) /
                record.duration,
        )
    }

    // Series records referenced here all have time and value
    // The extension values below are added because the SeriesRecord interface doesn't expose them
    internal val Any.time: Instant
        get() =
            when (this) {
                is CyclingPedalingCadenceRecord.Sample -> time
                is SpeedRecord.Sample -> time
                is StepsCadenceRecord.Sample -> time
                else -> error("Unsupported type for time: $this")
            }

    internal val Any.value: Double
        get() =
            when (this) {
                is CyclingPedalingCadenceRecord.Sample -> revolutionsPerMinute
                is SpeedRecord.Sample -> speed.inMetersPerSecond
                is StepsCadenceRecord.Sample -> rate
                else -> error("Unsupported type for value: $this")
            }
}
