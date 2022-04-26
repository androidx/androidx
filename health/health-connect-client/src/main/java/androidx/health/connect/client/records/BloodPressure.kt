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

import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the blood pressure of a user. Each record represents a single instantaneous blood
 * pressure reading.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BloodPressure(
    /**
     * Systolic blood pressure measurement, in millimetres of mercury (mmHg). Required field. Valid
     * range: 20-200.
     */
    public val systolicMillimetersOfMercury: Double,
    /**
     * Diastolic blood pressure measurement, in millimetres of mercury (mmHg). Required field. Valid
     * range: 10-180.
     */
    public val diastolicMillimetersOfMercury: Double,
    /**
     * The user's body position when the measurement was taken. Optional field. Allowed values:
     * [BodyPosition].
     */
    @property:BodyPosition public val bodyPosition: String? = null,
    /**
     * The arm and part of the arm where the measurement was taken. Optional field. Allowed values:
     * [BloodPressureMeasurementLocation].
     */
    @property:BloodPressureMeasurementLocation public val measurementLocation: String? = null,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BloodPressure) return false

        if (systolicMillimetersOfMercury != other.systolicMillimetersOfMercury) return false
        if (diastolicMillimetersOfMercury != other.diastolicMillimetersOfMercury) return false
        if (bodyPosition != other.bodyPosition) return false
        if (measurementLocation != other.measurementLocation) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + systolicMillimetersOfMercury.hashCode()
        result = 31 * result + diastolicMillimetersOfMercury.hashCode()
        result = 31 * result + bodyPosition.hashCode()
        result = 31 * result + measurementLocation.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val BLOOD_PRESSURE_NAME = "BloodPressure"
        private const val SYSTOLIC_FIELD_NAME = "systolic"
        private const val DIASTOLIC_FIELD_NAME = "diastolic"

        /**
         * Metric identifier to retrieve average systolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SYSTOLIC_AVG: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.AVERAGE,
                SYSTOLIC_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve minimum systolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SYSTOLIC_MIN: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.MINIMUM,
                SYSTOLIC_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve maximum systolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SYSTOLIC_MAX: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.MAXIMUM,
                SYSTOLIC_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve average diastolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val DIASTOLIC_AVG: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.AVERAGE,
                DIASTOLIC_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve minimum diastolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val DIASTOLIC_MIN: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.MINIMUM,
                DIASTOLIC_FIELD_NAME
            )

        /**
         * Metric identifier to retrieve maximum diastolic from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val DIASTOLIC_MAX: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                BLOOD_PRESSURE_NAME,
                AggregateMetric.AggregationType.MAXIMUM,
                DIASTOLIC_FIELD_NAME
            )
    }
}
