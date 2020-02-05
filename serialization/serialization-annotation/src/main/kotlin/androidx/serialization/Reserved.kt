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

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Reserves some IDs or names for a message class, enum class, or service interface.
 *
 * Apply this to a message class, enum class, or service interface to reserve member IDs or names.
 * This can be used to reserve unimplemented names or IDs for future use or to ensure that a
 * removed ID is not unintentionally reused in the future.
 *
 * For message classes and service interfaces, the reservation only applies to the annotated
 * class or interface. Its descendants do not inherit its reserved values.
 *
 * ```kotlin
 * @Reserved(1, 2, 3)
 * data class MyMessage(@Field(4) val myField: Int)
 * ```
 *
 * @property ids Message field, enum value, or service action IDs to reserve.
 * @property names Message field, enum value, or service action names to reserve.
 * @property idRanges Ranges of IDs to reserve in bulk.
 */
@Retention(SOURCE)
@Target(CLASS)
annotation class Reserved(
    vararg val ids: Int = [],
    val names: Array<String> = [],
    val idRanges: Array<IdRange> = []
) {
    /**
     * Reserves a block of IDs.
     *
     * Supply instances of this to [idRanges] to reserve ranges of IDs in bulk.
     *
     * ```kotlin
     * @Reserved(idRanges = [Reserved.IdRange(from = 1, to = 9000)])
     * data class MyMessage(@Field(9001) val myField: Int)
     * ```
     *
     * @property from First ID to reserve, inclusive.
     * @property to Last ID to reserve, inclusive.
     */
    @Retention(SOURCE)
    @Target(allowedTargets = [])
    annotation class IdRange(val from: Int, val to: Int)
}
