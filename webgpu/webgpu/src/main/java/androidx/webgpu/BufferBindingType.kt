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
@IntDef(
    value =
        [
            BufferBindingType.BindingNotUsed,
            BufferBindingType.Undefined,
            BufferBindingType.Uniform,
            BufferBindingType.Storage,
            BufferBindingType.ReadOnlyStorage,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The type of a buffer binding in a bind group layout. */
public annotation class BufferBindingType {
    public companion object {

        /** The binding is unused and should be ignored. */
        public const val BindingNotUsed: Int = 0x00000000

        /** An undefined buffer binding type. */
        public const val Undefined: Int = 0x00000001

        /** The buffer is used as a uniform buffer (read-only in shaders). */
        public const val Uniform: Int = 0x00000002

        /** The buffer is used as a storage buffer (read/write access in shaders). */
        public const val Storage: Int = 0x00000003

        /** The buffer is used as a read-only storage buffer. */
        public const val ReadOnlyStorage: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "BindingNotUsed",
                0x00000001 to "Undefined",
                0x00000002 to "Uniform",
                0x00000003 to "Storage",
                0x00000004 to "ReadOnlyStorage",
            )

        public fun toString(@BufferBindingType value: Int): String =
            names[value] ?: value.toString()
    }
}
