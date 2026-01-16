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
            TextureDimension.Undefined,
            TextureDimension._1D,
            TextureDimension._2D,
            TextureDimension._3D,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The dimensionality of a texture resource (e.g., 1D, 2D, or 3D). */
public annotation class TextureDimension {
    public companion object {

        /** An undefined texture dimension. */
        public const val Undefined: Int = 0x00000000

        /** One-dimensional texture. */
        public const val _1D: Int = 0x00000001

        /** Two-dimensional texture (the default). */
        public const val _2D: Int = 0x00000002

        /** Three-dimensional texture. */
        public const val _3D: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "_1D",
                0x00000002 to "_2D",
                0x00000003 to "_3D",
            )

        public fun toString(@TextureDimension value: Int): String = names[value] ?: value.toString()
    }
}
