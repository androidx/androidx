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

/** Temporal relationship of data point to a meal. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object RelationToMeals {
    const val GENERAL = "general"
    const val FASTING = "fasting"
    const val BEFORE_MEAL = "before_meal"
    const val AFTER_MEAL = "after_meal"
}

/**
 * Temporal relationship of data point to a meal.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            RelationToMeals.GENERAL,
            RelationToMeals.FASTING,
            RelationToMeals.BEFORE_MEAL,
            RelationToMeals.AFTER_MEAL,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class RelationToMeal
