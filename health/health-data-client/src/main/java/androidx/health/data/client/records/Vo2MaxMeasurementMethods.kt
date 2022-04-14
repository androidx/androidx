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

/** VO2 max (maximal aerobic capacity) measurement method. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object Vo2MaxMeasurementMethods {
    const val METABOLIC_CART = "metabolic_cart"
    const val HEART_RATE_RATIO = "heart_rate_ratio"
    const val COOPER_TEST = "cooper_test"
    const val MULTISTAGE_FITNESS_TEST = "multistage_fitness_test"
    const val ROCKPORT_FITNESS_TEST = "rockport_fitness_test"
    const val OTHER = "other"
}

/**
 * VO2 max (maximal aerobic capacity) measurement method.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            Vo2MaxMeasurementMethods.METABOLIC_CART,
            Vo2MaxMeasurementMethods.HEART_RATE_RATIO,
            Vo2MaxMeasurementMethods.COOPER_TEST,
            Vo2MaxMeasurementMethods.MULTISTAGE_FITNESS_TEST,
            Vo2MaxMeasurementMethods.ROCKPORT_FITNESS_TEST,
            Vo2MaxMeasurementMethods.OTHER,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class Vo2MaxMeasurementMethod
