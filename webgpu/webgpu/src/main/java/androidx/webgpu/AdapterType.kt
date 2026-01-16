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
        [AdapterType.DiscreteGPU, AdapterType.IntegratedGPU, AdapterType.CPU, AdapterType.Unknown]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Represents the type of GPU adapter. */
public annotation class AdapterType {
    public companion object {

        /** A high-power, dedicated GPU, typically found in desktop machines. */
        public const val DiscreteGPU: Int = 0x00000001

        /** A lower-power, integrated GPU, often sharing memory with the CPU. */
        public const val IntegratedGPU: Int = 0x00000002

        /** A software adapter running on the CPU. */
        public const val CPU: Int = 0x00000003

        /** The adapter type is unknown or could not be determined. */
        public const val Unknown: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "DiscreteGPU",
                0x00000002 to "IntegratedGPU",
                0x00000003 to "CPU",
                0x00000004 to "Unknown",
            )

        public fun toString(@AdapterType value: Int): String = names[value] ?: value.toString()
    }
}
