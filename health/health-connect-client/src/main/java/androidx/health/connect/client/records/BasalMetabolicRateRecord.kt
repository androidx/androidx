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
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Power
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the BMR of a user. Each record represents the energy a user would burn if at rest all
 * day, based on their height and weight.
 */
public class BasalMetabolicRateRecord(
    /** Basal metabolic rate, in [Power] unit. Required field. Valid range: 0-10000 kcal/day. */
    public val basalMetabolicRate: Power,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {

    init {
        basalMetabolicRate.requireNotLess(other = basalMetabolicRate.zero(), name = "bmr")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasalMetabolicRateRecord) return false

        if (basalMetabolicRate != other.basalMetabolicRate) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = basalMetabolicRate.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val BASAL_CALORIES_TYPE_NAME = "BasalCaloriesBurned"
        private const val ENERGY_FIELD_NAME = "energy"

        /**
         * Metric identifier to retrieve the total basal calories burned from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val BASAL_CALORIES_TOTAL: AggregateMetric<Energy> =
            AggregateMetric.doubleMetric(
                dataTypeName = BASAL_CALORIES_TYPE_NAME,
                aggregationType = AggregateMetric.AggregationType.TOTAL,
                fieldName = ENERGY_FIELD_NAME,
                mapper = Energy::calories,
            )
    }
}
