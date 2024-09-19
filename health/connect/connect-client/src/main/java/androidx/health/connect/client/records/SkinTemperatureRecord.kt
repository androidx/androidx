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
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.celsius
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the skin temperature of a user. Each record can represent a series of measurements of
 * temperature differences.
 *
 * @param startTime Start time of the record.
 * @param startZoneOffset User experienced zone offset at [startTime], or null if unknown. Providing
 *   these will help history aggregations results stay consistent should user travel. Queries with
 *   user experienced time filters will assume system current zone offset if the information is
 *   absent.
 * @param endTime End time of the record.
 * @param endZoneOffset User experienced zone offset at [endTime], or null if unknown. Providing
 *   these will help history aggregations results stay consistent should user travel. Queries with
 *   user experienced time filters will assume system current zone offset if the information is
 *   absent.
 * @param deltas a list of skin temperature [Delta]. If [baseline] is set, these values are expected
 *   to be relative to it.
 * @param baseline Temperature in [Temperature] unit. Optional field, null by default. Valid range:
 *   0-100 Celsius degrees.
 * @param measurementLocation indicates the location on the body from which the temperature reading
 *   was taken. Optional field, [MEASUREMENT_LOCATION_UNKNOWN] by default. Allowed values:
 *   [SkinTemperatureMeasurementLocation].
 * @param metadata set of common metadata associated with the written record.
 * @throws IllegalArgumentException if [startTime] > [endTime] or [deltas] are not within the record
 *   time range or baseline is not within [MIN_TEMPERATURE], [MAX_TEMPERATURE].
 */
class SkinTemperatureRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    val deltas: List<Delta>,
    val baseline: Temperature? = null,
    @SkinTemperatureMeasurementLocation val measurementLocation: Int = MEASUREMENT_LOCATION_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        if (baseline != null) {
            baseline.requireNotLess(other = MIN_TEMPERATURE, "temperature")
            baseline.requireNotMore(other = MAX_TEMPERATURE, "temperature")
        }

        if (deltas.isNotEmpty()) {
            // check all deltas are within parent record duration
            require(!deltas.minBy { it.time }.time.isBefore(startTime)) {
                "deltas can not be out of parent time range."
            }
            require(deltas.maxBy { it.time }.time.isBefore(endTime)) {
                "deltas can not be out of parent time range."
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkinTemperatureRecord) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (baseline != other.baseline) return false
        if (measurementLocation != other.measurementLocation) return false
        if (deltas != other.deltas) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + (baseline?.hashCode() ?: 0)
        result = 31 * result + measurementLocation.hashCode()
        result = 31 * result + deltas.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "SkinTemperatureRecord(startTime=$startTime, startZoneOffset=$startZoneOffset, endTime=$endTime, endZoneOffset=$endZoneOffset, deltas=$deltas, baseline=$baseline, measurementLocation=$measurementLocation, metadata=$metadata)"
    }

    companion object {
        private val MIN_TEMPERATURE = 0.celsius
        private val MAX_TEMPERATURE = 100.celsius

        /** Use this if the location is unknown. */
        const val MEASUREMENT_LOCATION_UNKNOWN: Int = 0
        /** Skin temperature measurement was taken from finger. */
        const val MEASUREMENT_LOCATION_FINGER: Int = 1
        /** Skin temperature measurement was taken from toe. */
        const val MEASUREMENT_LOCATION_TOE: Int = 2
        /** Skin temperature measurement was taken from wrist. */
        const val MEASUREMENT_LOCATION_WRIST: Int = 3

        /** Internal mappings useful for interoperability between integers and strings. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MEASUREMENT_LOCATION_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                "finger" to MEASUREMENT_LOCATION_FINGER,
                "toe" to MEASUREMENT_LOCATION_TOE,
                "wrist" to MEASUREMENT_LOCATION_WRIST,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MEASUREMENT_LOCATION_INT_TO_STRING_MAP =
            MEASUREMENT_LOCATION_STRING_TO_INT_MAP.reverse()

        /** Measurement location of the skin temperature. */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            value =
                [
                    MEASUREMENT_LOCATION_UNKNOWN,
                    MEASUREMENT_LOCATION_FINGER,
                    MEASUREMENT_LOCATION_TOE,
                    MEASUREMENT_LOCATION_WRIST,
                ]
        )
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        annotation class SkinTemperatureMeasurementLocation
    }

    /**
     * Represents a skin temperature delta entry of [SkinTemperatureRecord].
     *
     * @param time The point in time when the measurement was taken.
     * @param delta delta temperature difference. Valid range: -30 to 30 Celsius degrees.
     * @throws IllegalArgumentException if delta is not within [MIN_DELTA_TEMPERATURE] and
     *   [MAX_DELTA_TEMPERATURE], both inclusive.
     * @see SkinTemperatureRecord
     * @see TemperatureDelta
     */
    public class Delta(val time: Instant, val delta: TemperatureDelta) {

        init {
            delta.requireNotLess(other = MIN_DELTA_TEMPERATURE, "delta")
            delta.requireNotMore(other = MAX_DELTA_TEMPERATURE, "delta")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Delta

            if (time != other.time) return false
            if (delta != other.delta) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + delta.hashCode()
            return result
        }

        override fun toString(): String {
            return "Delta(time=$time, delta=$delta)"
        }

        private companion object {
            private val MIN_DELTA_TEMPERATURE = TemperatureDelta.celsius(-30.0)
            private val MAX_DELTA_TEMPERATURE = TemperatureDelta.celsius(30.0)
        }
    }
}
