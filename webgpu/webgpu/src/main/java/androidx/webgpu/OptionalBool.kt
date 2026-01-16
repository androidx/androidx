/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webgpu

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@IntDef(value = [OptionalBool.False, OptionalBool.True, OptionalBool.Undefined])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** A type used to represent an optional boolean value. */
public annotation class OptionalBool {
    public companion object {

        /** The boolean value is {@code false}. */
        public const val False: Int = 0x00000000

        /** The boolean value is {@code true}. */
        public const val True: Int = 0x00000001

        /** The boolean value is undefined or unspecified. */
        public const val Undefined: Int = 0x00000002
        internal val names: Map<Int, String> =
            mapOf(0x00000000 to "False", 0x00000001 to "True", 0x00000002 to "Undefined")

        public fun toString(@OptionalBool value: Int): String = names[value] ?: value.toString()
    }
}
