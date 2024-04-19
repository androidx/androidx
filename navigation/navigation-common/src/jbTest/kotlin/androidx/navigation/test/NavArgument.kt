/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.test

import androidx.navigation.NavArgument
import androidx.navigation.NavType.Companion.IntType
import androidx.navigation.NavType.Companion.StringArrayType
import androidx.navigation.NavType.Companion.StringType

// region IntType
fun intArgument() = NavArgument.Builder().setType(IntType).build()

fun intArgument(
    defaultValue: Int
) = NavArgument.Builder().setType(IntType)
    .setDefaultValue(defaultValue)
    .build()
// endregion

// region StringType
fun stringArgument() = NavArgument.Builder().setType(StringType)
    .setIsNullable(false)
    .build()

fun stringArgument(
    defaultValue: String
) = NavArgument.Builder().setType(StringType)
    .setDefaultValue(defaultValue)
    .build()

fun nullableStringArgument() = NavArgument.Builder().setType(StringType)
    .setIsNullable(true)
    .build()

fun nullableStringArgument(
    defaultValue: String?
) = NavArgument.Builder().setType(StringType)
    .setIsNullable(true)
    .setDefaultValue(defaultValue)
    .build()
// endregion

// region StringArrayType
fun stringArrayArgument(
    defaultValue: Array<String>?
) = NavArgument.Builder().setType(StringArrayType)
    .setIsNullable(true)
    .setDefaultValue(defaultValue)
    .build()
// endregion
