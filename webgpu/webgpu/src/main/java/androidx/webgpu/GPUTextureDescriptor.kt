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

/** A descriptor for creating a GPU texture. */
public class GPUTextureDescriptor
@JvmOverloads
constructor(
    /** The allowed usages for the texture (e.g., sampled, render_attachment). */
    @TextureUsage public var usage: Int,
    /** The size (width, height, depth/layers) of the texture. */
    public var size: GPUExtent3D,
    /** The label for the texture. */
    public var label: String? = null,
    /** The dimensionality of the texture (1D, 2D, or 3D). */
    @TextureDimension public var dimension: Int = TextureDimension._2D,
    /** The texture format. */
    @TextureFormat public var format: Int = TextureFormat.Undefined,
    public var mipLevelCount: Int = 1,
    public var sampleCount: Int = 1,
    @TextureFormat public var viewFormats: IntArray = intArrayOf(),
)
