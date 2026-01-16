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
            SamplerBindingType.BindingNotUsed,
            SamplerBindingType.Undefined,
            SamplerBindingType.Filtering,
            SamplerBindingType.NonFiltering,
            SamplerBindingType.Comparison,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The type of a sampler binding in a bind group layout. */
public annotation class SamplerBindingType {
    public companion object {

        /** The binding is unused and should be ignored. */
        public const val BindingNotUsed: Int = 0x00000000

        /** An undefined sampler binding type. */
        public const val Undefined: Int = 0x00000001

        /** A sampler that supports linear filtering. */
        public const val Filtering: Int = 0x00000002

        /**
         * A sampler that only supports nearest filtering (e.g., for non-filterable float textures).
         */
        public const val NonFiltering: Int = 0x00000003

        /** A sampler used for comparison operations (e.g., for depth textures). */
        public const val Comparison: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "BindingNotUsed",
                0x00000001 to "Undefined",
                0x00000002 to "Filtering",
                0x00000003 to "NonFiltering",
                0x00000004 to "Comparison",
            )

        public fun toString(@SamplerBindingType value: Int): String =
            names[value] ?: value.toString()
    }
}
