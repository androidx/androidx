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
            PrimitiveTopology.Undefined,
            PrimitiveTopology.PointList,
            PrimitiveTopology.LineList,
            PrimitiveTopology.LineStrip,
            PrimitiveTopology.TriangleList,
            PrimitiveTopology.TriangleStrip,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The geometric primitive type used for drawing in the render pipeline. */
public annotation class PrimitiveTopology {
    public companion object {

        /** An undefined primitive topology. */
        public const val Undefined: Int = 0x00000000

        /** Each vertex defines a separate point primitive. */
        public const val PointList: Int = 0x00000001

        /** Vertices define an unconnected set of line segments. */
        public const val LineList: Int = 0x00000002

        /** Vertices define a sequence of connected line segments. */
        public const val LineStrip: Int = 0x00000003

        /** Vertices define an unconnected set of triangles. */
        public const val TriangleList: Int = 0x00000004

        /** Vertices define a sequence of connected triangles. */
        public const val TriangleStrip: Int = 0x00000005
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "PointList",
                0x00000002 to "LineList",
                0x00000003 to "LineStrip",
                0x00000004 to "TriangleList",
                0x00000005 to "TriangleStrip",
            )

        public fun toString(@PrimitiveTopology value: Int): String =
            names[value] ?: value.toString()
    }
}
