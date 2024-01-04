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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Capture user's VO2 max score and optionally the measurement method. */
public class Vo2MaxRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** Maximal aerobic capacity (VO2 max) in milliliters. Required field. Valid range: 0-100. */
    public val vo2MillilitersPerMinuteKilogram: Double,
    /**
     * VO2 max measurement method. Optional field. Allowed values: [MeasurementMethod].
     *
     * @see MeasurementMethod
     */
    @property:MeasurementMethods public val measurementMethod: Int = MEASUREMENT_METHOD_OTHER,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    init {
        requireNonNegative(
            value = vo2MillilitersPerMinuteKilogram,
            name = "vo2MillilitersPerMinuteKilogram"
        )
        vo2MillilitersPerMinuteKilogram.requireNotMore(
            100.0,
            name = "vo2MillilitersPerMinuteKilogram"
        )
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vo2MaxRecord) return false

        if (vo2MillilitersPerMinuteKilogram != other.vo2MillilitersPerMinuteKilogram) return false
        if (measurementMethod != other.measurementMethod) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + vo2MillilitersPerMinuteKilogram.hashCode()
        result = 31 * result + measurementMethod
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        const val MEASUREMENT_METHOD_OTHER = 0
        const val MEASUREMENT_METHOD_METABOLIC_CART = 1
        const val MEASUREMENT_METHOD_HEART_RATE_RATIO = 2
        const val MEASUREMENT_METHOD_COOPER_TEST = 3
        const val MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST = 4
        const val MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST = 5

        /** Internal mappings useful for interoperability between integers and strings. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MEASUREMENT_METHOD_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                MeasurementMethod.OTHER to MEASUREMENT_METHOD_OTHER,
                MeasurementMethod.METABOLIC_CART to MEASUREMENT_METHOD_METABOLIC_CART,
                MeasurementMethod.HEART_RATE_RATIO to MEASUREMENT_METHOD_HEART_RATE_RATIO,
                MeasurementMethod.COOPER_TEST to MEASUREMENT_METHOD_COOPER_TEST,
                MeasurementMethod.MULTISTAGE_FITNESS_TEST to
                    MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
                MeasurementMethod.ROCKPORT_FITNESS_TEST to MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MEASUREMENT_METHOD_INT_TO_STRING_MAP = MEASUREMENT_METHOD_STRING_TO_INT_MAP.reverse()
    }

    /** VO2 max (maximal aerobic capacity) measurement method. */
    internal object MeasurementMethod {
        const val METABOLIC_CART = "metabolic_cart"
        const val HEART_RATE_RATIO = "heart_rate_ratio"
        const val COOPER_TEST = "cooper_test"
        const val MULTISTAGE_FITNESS_TEST = "multistage_fitness_test"
        const val ROCKPORT_FITNESS_TEST = "rockport_fitness_test"
        const val OTHER = "other"
    }

    /**
     * VO2 max (maximal aerobic capacity) measurement method.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                MEASUREMENT_METHOD_OTHER,
                MEASUREMENT_METHOD_METABOLIC_CART,
                MEASUREMENT_METHOD_HEART_RATE_RATIO,
                MEASUREMENT_METHOD_COOPER_TEST,
                MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
                MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class MeasurementMethods
}
