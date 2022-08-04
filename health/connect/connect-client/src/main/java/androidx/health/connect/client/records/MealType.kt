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

import androidx.annotation.StringDef

/** Type of meal. */
public object MealType {
    const val UNKNOWN = "unknown"
    /** Use this for the first meal of the day, usually the morning meal. */
    const val BREAKFAST = "breakfast"
    /** Use this for the noon meal. */
    const val LUNCH = "lunch"
    /** Use this for last meal of the day, usually the evening meal. */
    const val DINNER = "dinner"
    /** Any meal outside of the usual three meals per day. */
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
            MealType.UNKNOWN,
            MealType.BREAKFAST,
            MealType.LUNCH,
            MealType.DINNER,
            MealType.SNACK,
        ]
)
annotation class MealTypes
