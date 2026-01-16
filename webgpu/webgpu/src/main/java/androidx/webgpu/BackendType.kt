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
            BackendType.Undefined,
            BackendType.Null,
            BackendType.WebGPU,
            BackendType.D3D11,
            BackendType.D3D12,
            BackendType.Metal,
            BackendType.Vulkan,
            BackendType.OpenGL,
            BackendType.OpenGLES,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Specifies the underlying graphics API used by the adapter. */
public annotation class BackendType {
    public companion object {

        /** An undefined backend type. */
        public const val Undefined: Int = 0x00000000

        /** The default WebGPU backend. */
        public const val Null: Int = 0x00000001

        /** The native WebGPU implementation itself. */
        public const val WebGPU: Int = 0x00000002

        /** Direct3D 11 backend. */
        public const val D3D11: Int = 0x00000003

        /** Direct3D 12 backend. */
        public const val D3D12: Int = 0x00000004

        /** Apple's Metal backend. */
        public const val Metal: Int = 0x00000005

        /** Khronos' Vulkan backend. */
        public const val Vulkan: Int = 0x00000006

        /** OpenGL backend. */
        public const val OpenGL: Int = 0x00000007

        /** OpenGL ES backend. */
        public const val OpenGLES: Int = 0x00000008
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "Null",
                0x00000002 to "WebGPU",
                0x00000003 to "D3D11",
                0x00000004 to "D3D12",
                0x00000005 to "Metal",
                0x00000006 to "Vulkan",
                0x00000007 to "OpenGL",
                0x00000008 to "OpenGLES",
            )

        public fun toString(@BackendType value: Int): String = names[value] ?: value.toString()
    }
}
