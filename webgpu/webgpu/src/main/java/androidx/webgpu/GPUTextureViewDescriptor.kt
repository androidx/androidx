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

/** A descriptor for creating a texture view. */
public class GPUTextureViewDescriptor
@JvmOverloads
constructor(
    /** The intended usage of the texture view. */
    @TextureUsage public var usage: Int,
    /** The label for the texture view. */
    public var label: String? = null,
    /** The format of the data accessed through the view. */
    @TextureFormat public var format: Int = TextureFormat.Undefined,
    /** The dimensionality of the view (1D, 2D, 2D_array, cube, cube_array, or 3D). */
    @TextureViewDimension public var dimension: Int = TextureViewDimension.Undefined,
    public var baseMipLevel: Int = 0,
    public var mipLevelCount: Int = Constants.MIP_LEVEL_COUNT_UNDEFINED,
    public var baseArrayLayer: Int = 0,
    public var arrayLayerCount: Int = Constants.ARRAY_LAYER_COUNT_UNDEFINED,
    /** The aspect of the texture (color, depth, or stencil) visible through this view. */
    @TextureAspect public var aspect: Int = TextureAspect.All,
    /** Extension for specifying texture component swizzling when creating a texture view. */
    public var textureComponentSwizzleDescriptor: GPUTextureComponentSwizzleDescriptor? = null,
)
