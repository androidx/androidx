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
            SType.ShaderSourceSPIRV,
            SType.ShaderSourceWGSL,
            SType.RenderPassMaxDrawCount,
            SType.SurfaceSourceMetalLayer,
            SType.SurfaceSourceWindowsHWND,
            SType.SurfaceSourceXlibWindow,
            SType.SurfaceSourceWaylandSurface,
            SType.SurfaceSourceAndroidNativeWindow,
            SType.SurfaceSourceXCBWindow,
            SType.SurfaceColorManagement,
            SType.RequestAdapterWebXROptions,
            SType.TextureComponentSwizzleDescriptor,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Specifies the type of an extensible structure (s-Type). */
public annotation class SType {
    public companion object {

        /** Extension structure for providing SPIR-V shader source. */
        public const val ShaderSourceSPIRV: Int = 0x00000001

        /** Extension structure for providing WGSL shader source. */
        public const val ShaderSourceWGSL: Int = 0x00000002

        /** Extension structure for overriding the maximum draw count in a render pass. */
        public const val RenderPassMaxDrawCount: Int = 0x00000003

        /** Extension structure for creating a surface from a Metal layer. */
        public const val SurfaceSourceMetalLayer: Int = 0x00000004

        /** Extension structure for creating a surface from a Windows HWND. */
        public const val SurfaceSourceWindowsHWND: Int = 0x00000005

        /** Extension structure for creating a surface from an Xlib window. */
        public const val SurfaceSourceXlibWindow: Int = 0x00000006

        /** Extension structure for creating a surface from a Wayland surface. */
        public const val SurfaceSourceWaylandSurface: Int = 0x00000007

        /** Extension structure for creating a surface from an Android native window. */
        public const val SurfaceSourceAndroidNativeWindow: Int = 0x00000008

        /** Extension structure for creating a surface from an XCB window. */
        public const val SurfaceSourceXCBWindow: Int = 0x00000009

        /** Extension structure for configuring surface color management. */
        public const val SurfaceColorManagement: Int = 0x0000000a

        /** Extension structure for adding WebXR options to adapter requests. */
        public const val RequestAdapterWebXROptions: Int = 0x0000000b
        public const val TextureComponentSwizzleDescriptor: Int = 0x0000000c
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "ShaderSourceSPIRV",
                0x00000002 to "ShaderSourceWGSL",
                0x00000003 to "RenderPassMaxDrawCount",
                0x00000004 to "SurfaceSourceMetalLayer",
                0x00000005 to "SurfaceSourceWindowsHWND",
                0x00000006 to "SurfaceSourceXlibWindow",
                0x00000007 to "SurfaceSourceWaylandSurface",
                0x00000008 to "SurfaceSourceAndroidNativeWindow",
                0x00000009 to "SurfaceSourceXCBWindow",
                0x0000000a to "SurfaceColorManagement",
                0x0000000b to "RequestAdapterWebXROptions",
                0x0000000c to "TextureComponentSwizzleDescriptor",
            )

        public fun toString(@SType value: Int): String = names[value] ?: value.toString()
    }
}
