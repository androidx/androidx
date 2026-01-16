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

/** A single entry within a bind group layout, defining a resource binding slot. */
public class GPUBindGroupLayoutEntry
@JvmOverloads
constructor(
    /** The binding index corresponding to a `@binding` attribute in the shader. */
    public var binding: Int,
    /** The shader stages where this binding is visible. */
    @ShaderStage public var visibility: Int,
    public var bindingArraySize: Int = 0,
    /** The required constraints if the binding is a buffer. */
    public var buffer: GPUBufferBindingLayout =
        GPUBufferBindingLayout(type = BufferBindingType.BindingNotUsed),
    /** The required constraints if the binding is a sampler. */
    public var sampler: GPUSamplerBindingLayout =
        GPUSamplerBindingLayout(type = SamplerBindingType.BindingNotUsed),
    /** The required constraints if the binding is a sampled texture. */
    public var texture: GPUTextureBindingLayout =
        GPUTextureBindingLayout(
            sampleType = TextureSampleType.BindingNotUsed,
            viewDimension = TextureViewDimension.Undefined,
        ),
    public var storageTexture: GPUStorageTextureBindingLayout =
        GPUStorageTextureBindingLayout(
            access = StorageTextureAccess.BindingNotUsed,
            format = TextureFormat.Undefined,
            viewDimension = TextureViewDimension.Undefined,
        ),
)
