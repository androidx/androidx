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

/** A descriptor for creating a sampler. */
public class GPUSamplerDescriptor
@JvmOverloads
constructor(
    /** The label for the sampler. */
    public var label: String? = null,
    @AddressMode public var addressModeU: Int = AddressMode.ClampToEdge,
    @AddressMode public var addressModeV: Int = AddressMode.ClampToEdge,
    @AddressMode public var addressModeW: Int = AddressMode.ClampToEdge,
    @FilterMode public var magFilter: Int = FilterMode.Nearest,
    @FilterMode public var minFilter: Int = FilterMode.Nearest,
    @MipmapFilterMode public var mipmapFilter: Int = MipmapFilterMode.Nearest,
    public var lodMinClamp: Float = 0.0f,
    public var lodMaxClamp: Float = 32.0f,
    /** The comparison function for comparison samplers (e.g., for depth textures). */
    @CompareFunction public var compare: Int = CompareFunction.Undefined,
    public var maxAnisotropy: Short = 1,
)
