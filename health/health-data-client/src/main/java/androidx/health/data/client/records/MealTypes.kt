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

/** Type of meal. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object MealTypes {
    const val UNKNOWN = "unknown"
    const val BREAKFAST = "breakfast"
    const val LUNCH = "lunch"
    const val DINNER = "dinner"
    const val SNACK = "snack"
}

/**
 * Type of meal.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            MealTypes.UNKNOWN,
            MealTypes.BREAKFAST,
            MealTypes.LUNCH,
            MealTypes.DINNER,
            MealTypes.SNACK,
        ]
)
annotation class MealType
