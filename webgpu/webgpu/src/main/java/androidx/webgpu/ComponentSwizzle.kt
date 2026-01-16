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
            ComponentSwizzle.Undefined,
            ComponentSwizzle.Zero,
            ComponentSwizzle.One,
            ComponentSwizzle.R,
            ComponentSwizzle.G,
            ComponentSwizzle.B,
            ComponentSwizzle.A,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines how texture components (R, G, B, A) are swizzled in a texture view. */
public annotation class ComponentSwizzle {
    public companion object {

        /** An undefined swizzle value. */
        public const val Undefined: Int = 0x00000000

        /** The component value is fixed to 0.0. */
        public const val Zero: Int = 0x00000001

        /** The component value is fixed to 1.0. */
        public const val One: Int = 0x00000002

        /** The value of the red channel is used. */
        public const val R: Int = 0x00000003

        /** The value of the green channel is used. */
        public const val G: Int = 0x00000004

        /** The value of the blue channel is used. */
        public const val B: Int = 0x00000005

        /** The value of the alpha channel is used. */
        public const val A: Int = 0x00000006
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "Zero",
                0x00000002 to "One",
                0x00000003 to "R",
                0x00000004 to "G",
                0x00000005 to "B",
                0x00000006 to "A",
            )

        public fun toString(@ComponentSwizzle value: Int): String = names[value] ?: value.toString()
    }
}
