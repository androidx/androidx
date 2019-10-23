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

package androidx.serialization

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FIELD

/**
 * Tags a serializable enum value with its serialized ID.
 *
 * A serializable enum must have this annotation on all its values. One value of the enum must be
 * selected as the default value to use if the enum value is unrecognized or missing. Set the ID
 * of this value to [DEFAULT] or zero.
 *
 * ```kotlin
 * enum class MyEnum {
 *    @EnumValue(EnumValue.DEFAULT)
 *    DEFAULT,
 *
 *    @EnumValue(1)
 *    MY_VALUE
 * }
 * ```
 *
 * @property id Integer ID of the enum value.
 *
 * Any integer is a valid enum value ID, but `0` has special meaning as the default value.
 * Additionally, negative values are not recommended as their proto representation is always 10
 * bytes long.
 *
 * Enum value IDs must be unique within an enum class, but may be freely reused between enum
 * classes.
 *
 * To reserve enum value IDs for future use or to prevent unintentional reuse of removed enum
 * value IDs, apply the [Reserved] annotation to the enum class.
 */
@Retention(BINARY)
@Target(FIELD)
annotation class EnumValue(
    @get:JvmName("value") val id: Int
) {
    companion object {
        /**
         * The ID of a default enum value.
         *
         * One value in every enum must be default to be used if a parser encounters an
         * unrecognized or missing enum field.
         */
        const val DEFAULT = 0
    }
}
