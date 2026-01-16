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
    flag = true,
    value =
        [
            ColorWriteMask.None,
            ColorWriteMask.Red,
            ColorWriteMask.Green,
            ColorWriteMask.Blue,
            ColorWriteMask.Alpha,
            ColorWriteMask.All,
        ],
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Bitmask specifying which color channels are writable in a render target. */
public annotation class ColorWriteMask {
    public companion object {

        /** No color channels are writable. */
        public const val None: Int = 0x00000000

        /** Allows writing to the red channel. */
        public const val Red: Int = 0x00000001

        /** Allows writing to the green channel. */
        public const val Green: Int = 0x00000002

        /** Allows writing to the blue channel. */
        public const val Blue: Int = 0x00000004

        /** Allows writing to the alpha channel. */
        public const val Alpha: Int = 0x00000008

        /** Allows writing to all color channels (red, green, blue, and alpha). */
        public const val All: Int = 0x0000000f
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "None",
                0x00000001 to "Red",
                0x00000002 to "Green",
                0x00000004 to "Blue",
                0x00000008 to "Alpha",
                0x0000000f to "All",
            )

        public fun toString(@ColorWriteMask value: Int): String = names[value] ?: value.toString()
    }
}
