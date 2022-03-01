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
package androidx.health.data.client.records

import androidx.annotation.RestrictTo
import androidx.health.data.client.aggregate.LongAggregateMetric
import androidx.health.data.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the user's heart rate. Each record represents a single instantaneous measurement. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HeartRate(
    /** Heart beats per minute. Required field. Validation range: 1-300. */
    public val bpm: Long,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeartRate) return false

        if (bpm != other.bpm) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + bpm.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        /** Metric identifier to retrieve average heart rate from [AggregateDataRow]. */
        @JvmStatic
        val HEART_RATE_BPM_AVG: LongAggregateMetric = LongAggregateMetric("HeartRate", "avg", "bpm")

        /** Metric identifier to retrieve minimum heart rate from [AggregateDataRow]. */
        @JvmStatic
        val HEART_RATE_BPM_MIN: LongAggregateMetric = LongAggregateMetric("HeartRate", "min", "bpm")

        /** Metric identifier to retrieve maximum heart rate from [AggregateDataRow]. */
        @JvmStatic
        val HEART_RATE_BPM_MAX: LongAggregateMetric = LongAggregateMetric("HeartRate", "max", "bpm")
    }
}
