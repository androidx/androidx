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
package androidx.health.connect.client.records

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the user's resting heart rate. Each record represents a single instantaneous
 * measurement.
 */
public class RestingHeartRateRecord(
    /** Heart beats per minute. Required field. Validation range: 1-300. */
    public val beatsPerMinute: Long,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    init {
        requireNonNegative(value = beatsPerMinute, name = "beatsPerMinute")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RestingHeartRateRecord) return false

        if (beatsPerMinute != other.beatsPerMinute) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + beatsPerMinute.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val REST_HEART_RATE_TYPE_NAME = "RestingHeartRate"
        private const val BPM_FIELD_NAME = "bpm"

        /**
         * Metric identifier to retrieve the average resting heart rate from [AggregationResult].
         */
        @JvmField
        val BPM_AVG: AggregateMetric<Long> =
            AggregateMetric.longMetric(
                REST_HEART_RATE_TYPE_NAME,
                AggregateMetric.AggregationType.AVERAGE,
                BPM_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve the minimum resting heart rate from [AggregationResult].
         */
        @JvmField
        val BPM_MIN: AggregateMetric<Long> =
            AggregateMetric.longMetric(
                REST_HEART_RATE_TYPE_NAME,
                AggregateMetric.AggregationType.MINIMUM,
                BPM_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve the maximum resting heart rate from [AggregationResult].
         */
        @JvmField
        val BPM_MAX: AggregateMetric<Long> =
            AggregateMetric.longMetric(
                REST_HEART_RATE_TYPE_NAME,
                AggregateMetric.AggregationType.MAXIMUM,
                BPM_FIELD_NAME
            )
    }
}
