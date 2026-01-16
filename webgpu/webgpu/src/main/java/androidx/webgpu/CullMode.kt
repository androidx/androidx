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
@IntDef(value = [CullMode.Undefined, CullMode.None, CullMode.Front, CullMode.Back])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines the face of primitives (front or back) to be culled (discarded). */
public annotation class CullMode {
    public companion object {

        /** An undefined cull mode. */
        public const val Undefined: Int = 0x00000000

        /** No culling is performed; all faces are drawn. */
        public const val None: Int = 0x00000001

        /** Cull front-facing primitives. */
        public const val Front: Int = 0x00000002

        /** Cull back-facing primitives. */
        public const val Back: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "None",
                0x00000002 to "Front",
                0x00000003 to "Back",
            )

        public fun toString(@CullMode value: Int): String = names[value] ?: value.toString()
    }
}
