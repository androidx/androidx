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

/** Where on the user's body a temperature measurement was taken from. */
public object BodyTemperatureMeasurementLocation {
    const val MEASUREMENT_LOCATION_UNKNOWN = 0
    const val MEASUREMENT_LOCATION_ARMPIT = 1
    const val MEASUREMENT_LOCATION_FINGER = 2
    const val MEASUREMENT_LOCATION_FOREHEAD = 3
    const val MEASUREMENT_LOCATION_MOUTH = 4
    const val MEASUREMENT_LOCATION_RECTUM = 5
    const val MEASUREMENT_LOCATION_TEMPORAL_ARTERY = 6
    const val MEASUREMENT_LOCATION_TOE = 7
    const val MEASUREMENT_LOCATION_EAR = 8
    const val MEASUREMENT_LOCATION_WRIST = 9
    const val MEASUREMENT_LOCATION_VAGINA = 10

    internal const val ARMPIT = "armpit"
    internal const val FINGER = "finger"
    internal const val FOREHEAD = "forehead"
    internal const val MOUTH = "mouth"
    internal const val RECTUM = "rectum"
    internal const val TEMPORAL_ARTERY = "temporal_artery"
    internal const val TOE = "toe"
    internal const val EAR = "ear"
    internal const val WRIST = "wrist"
    internal const val VAGINA = "vagina"

    /** Internal mappings useful for interoperability between integers and strings. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmField
    val MEASUREMENT_LOCATION_STRING_TO_INT_MAP: Map<String, Int> =
        mapOf(
            ARMPIT to MEASUREMENT_LOCATION_ARMPIT,
            FINGER to MEASUREMENT_LOCATION_FINGER,
            FOREHEAD to MEASUREMENT_LOCATION_FOREHEAD,
            MOUTH to MEASUREMENT_LOCATION_MOUTH,
            RECTUM to MEASUREMENT_LOCATION_RECTUM,
            TEMPORAL_ARTERY to MEASUREMENT_LOCATION_TEMPORAL_ARTERY,
            TOE to MEASUREMENT_LOCATION_TOE,
            EAR to MEASUREMENT_LOCATION_EAR,
            WRIST to MEASUREMENT_LOCATION_WRIST,
            VAGINA to MEASUREMENT_LOCATION_VAGINA
        )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmField
    val MEASUREMENT_LOCATION_INT_TO_STRING_MAP = MEASUREMENT_LOCATION_STRING_TO_INT_MAP.reverse()
}

/**
 * Where on the user's body a temperature measurement was taken from.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FOREHEAD,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TEMPORAL_ARTERY,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST,
            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_VAGINA
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class BodyTemperatureMeasurementLocations
