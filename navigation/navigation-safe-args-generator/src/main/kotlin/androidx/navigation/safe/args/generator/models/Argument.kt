/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.models

import androidx.navigation.safe.args.generator.NavType
import androidx.navigation.safe.args.generator.NullValue
import androidx.navigation.safe.args.generator.WritableValue
import androidx.navigation.safe.args.generator.ext.joinToCamelCaseAsVar

data class Argument(
    val name: String,
    val type: NavType,
    val defaultValue: WritableValue? = null,
    val isNullable: Boolean = false
) {
    init {
        if (isNullable && !type.allowsNullable()) {
            throw IllegalArgumentException(
                "Argument is nullable but type $type " +
                    "cannot be nullable."
            )
        }
        if (!isNullable && defaultValue == NullValue) {
            throw IllegalArgumentException("Argument has null value but is not nullable.")
        }
    }

    val sanitizedName = name.split("[^a-zA-Z0-9]".toRegex())
        .map { it.trim() }.joinToCamelCaseAsVar()

    fun isOptional() = defaultValue != null

    operator fun component5() = sanitizedName
}
