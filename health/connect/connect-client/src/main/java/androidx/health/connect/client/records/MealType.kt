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
import androidx.health.connect.client.records.MealType.MEAL_TYPE_BREAKFAST
import androidx.health.connect.client.records.MealType.MEAL_TYPE_DINNER
import androidx.health.connect.client.records.MealType.MEAL_TYPE_LUNCH
import androidx.health.connect.client.records.MealType.MEAL_TYPE_SNACK
import androidx.health.connect.client.records.MealType.MEAL_TYPE_UNKNOWN

/** Type of meal. */
public object MealType {
    internal const val UNKNOWN = "unknown"
    internal const val BREAKFAST = "breakfast"
    internal const val LUNCH = "lunch"
    internal const val DINNER = "dinner"
    internal const val SNACK = "snack"

    const val MEAL_TYPE_UNKNOWN = 0

    /** Use this for the first meal of the day, usually the morning meal. */
    const val MEAL_TYPE_BREAKFAST = 1

    /** Use this for the noon meal. */
    const val MEAL_TYPE_LUNCH = 2

    /** Use this for last meal of the day, usually the evening meal. */
    const val MEAL_TYPE_DINNER = 3

    /** Any meal outside of the usual three meals per day. */
    const val MEAL_TYPE_SNACK = 4

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmField
    val MEAL_TYPE_STRING_TO_INT_MAP: Map<String, Int> =
        mapOf(
            UNKNOWN to MEAL_TYPE_UNKNOWN,
            BREAKFAST to MEAL_TYPE_BREAKFAST,
            LUNCH to MEAL_TYPE_LUNCH,
            DINNER to MEAL_TYPE_DINNER,
            SNACK to MEAL_TYPE_SNACK,
        )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmField
    val MEAL_TYPE_INT_TO_STRING_MAP = MEAL_TYPE_STRING_TO_INT_MAP.reverse()
}

/**
 * Type of meal.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [MEAL_TYPE_UNKNOWN, MEAL_TYPE_BREAKFAST, MEAL_TYPE_LUNCH, MEAL_TYPE_DINNER, MEAL_TYPE_SNACK]
)
annotation class MealTypes
