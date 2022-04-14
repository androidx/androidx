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
import androidx.annotation.StringDef

/**
 * List of supported blood glucose specimen sources (type of body fluid used to measure the blood
 * glucose).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object SpecimenSources {
    const val INTERSTITIAL_FLUID = "interstitial_fluid"
    const val CAPILLARY_BLOOD = "capillary_blood"
    const val PLASMA = "plasma"
    const val SERUM = "serum"
    const val TEARS = "tears"
    const val WHOLE_BLOOD = "whole_blood"
}

/**
 * List of supported blood glucose specimen sources (type of body fluid used to measure the blood
 * glucose).
 *
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            SpecimenSources.INTERSTITIAL_FLUID,
            SpecimenSources.CAPILLARY_BLOOD,
            SpecimenSources.PLASMA,
            SpecimenSources.SERUM,
            SpecimenSources.TEARS,
            SpecimenSources.WHOLE_BLOOD,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class SpecimenSource
