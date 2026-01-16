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
            CompositeAlphaMode.Auto,
            CompositeAlphaMode.Opaque,
            CompositeAlphaMode.Premultiplied,
            CompositeAlphaMode.Unpremultiplied,
            CompositeAlphaMode.Inherit,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Specifies how the alpha component of a swap chain image is handled during composition. */
public annotation class CompositeAlphaMode {
    public companion object {

        /** The alpha mode is chosen automatically by the implementation. */
        public const val Auto: Int = 0x00000000

        /** The alpha channel is ignored; the surface is treated as fully opaque. */
        public const val Opaque: Int = 0x00000001

        /** The color components are already multiplied by the alpha channel. */
        public const val Premultiplied: Int = 0x00000002

        /** The color components are not multiplied by the alpha channel. */
        public const val Unpremultiplied: Int = 0x00000003

        /** The alpha mode is inherited from the underlying native window/surface. */
        public const val Inherit: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Auto",
                0x00000001 to "Opaque",
                0x00000002 to "Premultiplied",
                0x00000003 to "Unpremultiplied",
                0x00000004 to "Inherit",
            )

        public fun toString(@CompositeAlphaMode value: Int): String =
            names[value] ?: value.toString()
    }
}
