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
            TextureSampleType.BindingNotUsed,
            TextureSampleType.Undefined,
            TextureSampleType.Float,
            TextureSampleType.UnfilterableFloat,
            TextureSampleType.Depth,
            TextureSampleType.Sint,
            TextureSampleType.Uint,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The fundamental data type of samples when a texture is bound for sampling in a shader. */
public annotation class TextureSampleType {
    public companion object {

        /** The binding is unused and should be ignored. */
        public const val BindingNotUsed: Int = 0x00000000

        /** An undefined texture sample type. */
        public const val Undefined: Int = 0x00000001

        /** Floating-point sample type (filterable). */
        public const val Float: Int = 0x00000002

        /** Floating-point sample type (unfilterable). */
        public const val UnfilterableFloat: Int = 0x00000003

        /** Depth value sample type (filterable with comparison sampler). */
        public const val Depth: Int = 0x00000004

        /** Signed integer sample type. */
        public const val Sint: Int = 0x00000005

        /** Unsigned integer sample type. */
        public const val Uint: Int = 0x00000006
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "BindingNotUsed",
                0x00000001 to "Undefined",
                0x00000002 to "Float",
                0x00000003 to "UnfilterableFloat",
                0x00000004 to "Depth",
                0x00000005 to "Sint",
                0x00000006 to "Uint",
            )

        public fun toString(@TextureSampleType value: Int): String =
            names[value] ?: value.toString()
    }
}
