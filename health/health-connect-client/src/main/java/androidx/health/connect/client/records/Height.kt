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
import androidx.health.connect.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the user's height in meters. */
public class Height(
    /** Height in meters. Required field. Valid range: 0-3. */
    public val heightMeters: Double,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    init {
        requireNonNegative(value = heightMeters, name = "heightMeters")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Height) return false

        if (heightMeters != other.heightMeters) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + heightMeters.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val HEIGHT_NAME = "Height"
        private const val HEIGHT_FIELD_NAME = "height"

        /**
         * Metric identifier to retrieve the average height from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val HEIGHT_AVG: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                HEIGHT_NAME,
                AggregateMetric.AggregationType.AVERAGE,
                HEIGHT_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve minimum height from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val HEIGHT_MIN: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                HEIGHT_NAME,
                AggregateMetric.AggregationType.MINIMUM,
                HEIGHT_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve the maximum height from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val HEIGHT_MAX: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                HEIGHT_NAME,
                AggregateMetric.AggregationType.MAXIMUM,
                HEIGHT_FIELD_NAME
            )
    }
}
