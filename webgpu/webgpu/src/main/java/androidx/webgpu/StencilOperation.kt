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
            StencilOperation.Undefined,
            StencilOperation.Keep,
            StencilOperation.Zero,
            StencilOperation.Replace,
            StencilOperation.Invert,
            StencilOperation.IncrementClamp,
            StencilOperation.DecrementClamp,
            StencilOperation.IncrementWrap,
            StencilOperation.DecrementWrap,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The operation performed on the stencil value in the depth/stencil test. */
public annotation class StencilOperation {
    public companion object {

        /** An undefined stencil operation. */
        public const val Undefined: Int = 0x00000000

        /** Keeps the current stencil value. */
        public const val Keep: Int = 0x00000001

        /** Sets the stencil value to 0. */
        public const val Zero: Int = 0x00000002

        /** Replaces the stencil value with the stencil reference value. */
        public const val Replace: Int = 0x00000003

        /** Bitwise inverts the stencil value. */
        public const val Invert: Int = 0x00000004

        /** Increments the stencil value, clamping to the maximum representable value. */
        public const val IncrementClamp: Int = 0x00000005

        /** Decrements the stencil value, clamping to 0. */
        public const val DecrementClamp: Int = 0x00000006

        /** Increments the stencil value, wrapping to 0 on overflow. */
        public const val IncrementWrap: Int = 0x00000007

        /**
         * Decrements the stencil value, wrapping to the maximum representable value on underflow.
         */
        public const val DecrementWrap: Int = 0x00000008
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "Keep",
                0x00000002 to "Zero",
                0x00000003 to "Replace",
                0x00000004 to "Invert",
                0x00000005 to "IncrementClamp",
                0x00000006 to "DecrementClamp",
                0x00000007 to "IncrementWrap",
                0x00000008 to "DecrementWrap",
            )

        public fun toString(@StencilOperation value: Int): String = names[value] ?: value.toString()
    }
}
