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
@IntDef(value = [FrontFace.Undefined, FrontFace.CCW, FrontFace.CW])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/**
 * Defines the winding order (clockwise or counter-clockwise) that determines a front-facing
 * primitive.
 */
public annotation class FrontFace {
    public companion object {

        /** An undefined front face winding order. */
        public const val Undefined: Int = 0x00000000

        /** Counter-clockwise winding order is considered front-facing. */
        public const val CCW: Int = 0x00000001

        /** Clockwise winding order is considered front-facing. */
        public const val CW: Int = 0x00000002
        internal val names: Map<Int, String> =
            mapOf(0x00000000 to "Undefined", 0x00000001 to "CCW", 0x00000002 to "CW")

        public fun toString(@FrontFace value: Int): String = names[value] ?: value.toString()
    }
}
