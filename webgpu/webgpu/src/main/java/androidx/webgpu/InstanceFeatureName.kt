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
            InstanceFeatureName.TimedWaitAny,
            InstanceFeatureName.ShaderSourceSPIRV,
            InstanceFeatureName.MultipleDevicesPerAdapter,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** A list of optional features that can be enabled on a WebGPU instance. */
public annotation class InstanceFeatureName {
    public companion object {

        /** Enables waiting with a timeout for any of a set of futures to complete. */
        public const val TimedWaitAny: Int = 0x00000001

        /** Allows specifying SPIR-V binary code directly when creating a shader module. */
        public const val ShaderSourceSPIRV: Int = 0x00000002

        /** Enables support for creating multiple GPU devices from a single adapter. */
        public const val MultipleDevicesPerAdapter: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "TimedWaitAny",
                0x00000002 to "ShaderSourceSPIRV",
                0x00000003 to "MultipleDevicesPerAdapter",
            )

        public fun toString(@InstanceFeatureName value: Int): String =
            names[value] ?: value.toString()
    }
}
