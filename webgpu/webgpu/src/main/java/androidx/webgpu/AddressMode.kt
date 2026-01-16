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
            AddressMode.Undefined,
            AddressMode.ClampToEdge,
            AddressMode.Repeat,
            AddressMode.MirrorRepeat,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines the texture addressing mode for a sampler coordinate (u, v, or w). */
public annotation class AddressMode {
    public companion object {

        /** An undefined mode. */
        public const val Undefined: Int = 0x00000000

        /** Clamps the texture coordinate to the edge of the texture. */
        public const val ClampToEdge: Int = 0x00000001

        /** Repeats the texture when the coordinate is outside the 0.0 to 1.0 range. */
        public const val Repeat: Int = 0x00000002

        /** Mirrors and repeats the texture when the coordinate is outside the 0.0 to 1.0 range. */
        public const val MirrorRepeat: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "ClampToEdge",
                0x00000002 to "Repeat",
                0x00000003 to "MirrorRepeat",
            )

        public fun toString(@AddressMode value: Int): String = names[value] ?: value.toString()
    }
}
