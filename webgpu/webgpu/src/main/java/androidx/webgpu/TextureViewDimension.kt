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
            TextureViewDimension.Undefined,
            TextureViewDimension._1D,
            TextureViewDimension._2D,
            TextureViewDimension._2DArray,
            TextureViewDimension.Cube,
            TextureViewDimension.CubeArray,
            TextureViewDimension._3D,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The dimensionality and structure of a texture view. */
public annotation class TextureViewDimension {
    public companion object {

        /** An undefined texture view dimension. */
        public const val Undefined: Int = 0x00000000

        /** View is a 1D texture. */
        public const val _1D: Int = 0x00000001

        /** View is a 2D texture. */
        public const val _2D: Int = 0x00000002

        /** View is an array of 2D textures. */
        public const val _2DArray: Int = 0x00000003

        /** View is a cubemap texture. */
        public const val Cube: Int = 0x00000004

        /** View is an array of cubemap textures. */
        public const val CubeArray: Int = 0x00000005

        /** View is a 3D texture. */
        public const val _3D: Int = 0x00000006
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "_1D",
                0x00000002 to "_2D",
                0x00000003 to "_2DArray",
                0x00000004 to "Cube",
                0x00000005 to "CubeArray",
                0x00000006 to "_3D",
            )

        public fun toString(@TextureViewDimension value: Int): String =
            names[value] ?: value.toString()
    }
}
