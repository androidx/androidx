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
            WGSLLanguageFeatureName.ReadonlyAndReadwriteStorageTextures,
            WGSLLanguageFeatureName.Packed4x8IntegerDotProduct,
            WGSLLanguageFeatureName.UnrestrictedPointerParameters,
            WGSLLanguageFeatureName.PointerCompositeAccess,
            WGSLLanguageFeatureName.UniformBufferStandardLayout,
            WGSLLanguageFeatureName.SubgroupId,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** A list of advanced, optional features available in the WebGPU Shading Language (WGSL). */
public annotation class WGSLLanguageFeatureName {
    public companion object {
        public const val ReadonlyAndReadwriteStorageTextures: Int = 0x00000001
        public const val Packed4x8IntegerDotProduct: Int = 0x00000002

        /** Allows passing unrestricted pointers as parameters to WGSL functions. */
        public const val UnrestrictedPointerParameters: Int = 0x00000003

        /** Allows composite access on pointers within a WGSL shader. */
        public const val PointerCompositeAccess: Int = 0x00000004

        /** Enforces standard memory layout rules for uniform buffers in WGSL. */
        public const val UniformBufferStandardLayout: Int = 0x00000005

        /** Enables access to the subgroup ID within a compute shader. */
        public const val SubgroupId: Int = 0x00000006
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "ReadonlyAndReadwriteStorageTextures",
                0x00000002 to "Packed4x8IntegerDotProduct",
                0x00000003 to "UnrestrictedPointerParameters",
                0x00000004 to "PointerCompositeAccess",
                0x00000005 to "UniformBufferStandardLayout",
                0x00000006 to "SubgroupId",
            )

        public fun toString(@WGSLLanguageFeatureName value: Int): String =
            names[value] ?: value.toString()
    }
}
