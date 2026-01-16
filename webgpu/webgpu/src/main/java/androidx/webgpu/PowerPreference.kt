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
    value = [PowerPreference.Undefined, PowerPreference.LowPower, PowerPreference.HighPerformance]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Hints for the desired GPU power consumption and performance characteristics. */
public annotation class PowerPreference {
    public companion object {

        /** An undefined power preference. */
        public const val Undefined: Int = 0x00000000

        /** Prefers an adapter that minimizes power consumption. */
        public const val LowPower: Int = 0x00000001

        /** Prefers an adapter that maximizes performance. */
        public const val HighPerformance: Int = 0x00000002
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "LowPower",
                0x00000002 to "HighPerformance",
            )

        public fun toString(@PowerPreference value: Int): String = names[value] ?: value.toString()
    }
}
