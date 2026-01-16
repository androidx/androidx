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
            FeatureName.CoreFeaturesAndLimits,
            FeatureName.DepthClipControl,
            FeatureName.Depth32FloatStencil8,
            FeatureName.TextureCompressionBC,
            FeatureName.TextureCompressionBCSliced3D,
            FeatureName.TextureCompressionETC2,
            FeatureName.TextureCompressionASTC,
            FeatureName.TextureCompressionASTCSliced3D,
            FeatureName.TimestampQuery,
            FeatureName.IndirectFirstInstance,
            FeatureName.ShaderF16,
            FeatureName.RG11B10UfloatRenderable,
            FeatureName.BGRA8UnormStorage,
            FeatureName.Float32Filterable,
            FeatureName.Float32Blendable,
            FeatureName.ClipDistances,
            FeatureName.DualSourceBlending,
            FeatureName.Subgroups,
            FeatureName.TextureFormatsTier1,
            FeatureName.TextureFormatsTier2,
            FeatureName.PrimitiveIndex,
            FeatureName.TextureComponentSwizzle,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** A list of optional features that can be enabled on a device. */
public annotation class FeatureName {
    public companion object {

        /** The base features and limits available on every WebGPU device. */
        public const val CoreFeaturesAndLimits: Int = 0x00000001

        /** Allows controlling depth clipping behavior. */
        public const val DepthClipControl: Int = 0x00000002

        /** Supports the 32-bit float depth and 8-bit stencil texture format. */
        public const val Depth32FloatStencil8: Int = 0x00000003

        /** Supports the BC (Block Compression) texture formats. */
        public const val TextureCompressionBC: Int = 0x00000004

        /** Supports the BC texture formats for sliced 3D textures. */
        public const val TextureCompressionBCSliced3D: Int = 0x00000005

        /** Supports the ETC2 (Ericsson GPUTexture Compression) texture formats. */
        public const val TextureCompressionETC2: Int = 0x00000006

        /** Supports the ASTC (Adaptive Scalable GPUTexture Compression) texture formats. */
        public const val TextureCompressionASTC: Int = 0x00000007

        /** Supports the ASTC texture formats for sliced 3D textures. */
        public const val TextureCompressionASTCSliced3D: Int = 0x00000008

        /** Enables the use of timestamp queries in command buffers. */
        public const val TimestampQuery: Int = 0x00000009

        /** Enables drawing commands to specify the first instance index in an indirect buffer. */
        public const val IndirectFirstInstance: Int = 0x0000000a

        /** Enables the use of 16-bit floating point numbers in shaders. */
        public const val ShaderF16: Int = 0x0000000b
        public const val RG11B10UfloatRenderable: Int = 0x0000000c
        public const val BGRA8UnormStorage: Int = 0x0000000d

        /** Allows linear filtering of textures with $\text{float32}$ formats. */
        public const val Float32Filterable: Int = 0x0000000e

        /** Allows blending operations on $\text{float32}$ color attachments. */
        public const val Float32Blendable: Int = 0x0000000f

        /** Enables user-defined clip distances in the vertex shader stage. */
        public const val ClipDistances: Int = 0x00000010

        /** Enables dual-source blending, where fragment shader outputs two colors for blending. */
        public const val DualSourceBlending: Int = 0x00000011

        /** Enables access to subgroup operations in shaders. */
        public const val Subgroups: Int = 0x00000012
        public const val TextureFormatsTier1: Int = 0x00000013
        public const val TextureFormatsTier2: Int = 0x00000014

        /** Enables reading the primitive index in the fragment shader. */
        public const val PrimitiveIndex: Int = 0x00000015

        /** Enables swizzling of texture components in a texture view. */
        public const val TextureComponentSwizzle: Int = 0x00000016
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "CoreFeaturesAndLimits",
                0x00000002 to "DepthClipControl",
                0x00000003 to "Depth32FloatStencil8",
                0x00000004 to "TextureCompressionBC",
                0x00000005 to "TextureCompressionBCSliced3D",
                0x00000006 to "TextureCompressionETC2",
                0x00000007 to "TextureCompressionASTC",
                0x00000008 to "TextureCompressionASTCSliced3D",
                0x00000009 to "TimestampQuery",
                0x0000000a to "IndirectFirstInstance",
                0x0000000b to "ShaderF16",
                0x0000000c to "RG11B10UfloatRenderable",
                0x0000000d to "BGRA8UnormStorage",
                0x0000000e to "Float32Filterable",
                0x0000000f to "Float32Blendable",
                0x00000010 to "ClipDistances",
                0x00000011 to "DualSourceBlending",
                0x00000012 to "Subgroups",
                0x00000013 to "TextureFormatsTier1",
                0x00000014 to "TextureFormatsTier2",
                0x00000015 to "PrimitiveIndex",
                0x00000016 to "TextureComponentSwizzle",
            )

        public fun toString(@FeatureName value: Int): String = names[value] ?: value.toString()
    }
}
