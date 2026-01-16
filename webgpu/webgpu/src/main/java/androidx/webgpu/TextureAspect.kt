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
            TextureAspect.Undefined,
            TextureAspect.All,
            TextureAspect.StencilOnly,
            TextureAspect.DepthOnly,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Specifies which part of a texture's data (color, depth, stencil) is being accessed. */
public annotation class TextureAspect {
    public companion object {

        /** An undefined texture aspect. */
        public const val Undefined: Int = 0x00000000

        /** Accesses all aspects (color, depth, and stencil if present). */
        public const val All: Int = 0x00000001

        /** Accesses only the stencil aspect of a depth/stencil texture. */
        public const val StencilOnly: Int = 0x00000002

        /** Accesses only the depth aspect of a depth/stencil texture. */
        public const val DepthOnly: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "All",
                0x00000002 to "StencilOnly",
                0x00000003 to "DepthOnly",
            )

        public fun toString(@TextureAspect value: Int): String = names[value] ?: value.toString()
    }
}
