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

/** A single entry within a bind group, defining a bound resource. */
public class GPUBindGroupEntry
@JvmOverloads
constructor(
    /** The binding index specified in the shader and layout. */
    public var binding: Int,
    /** The buffer to bind, if the resource is a buffer. */
    public var buffer: GPUBuffer? = null,
    /** The offset into the buffer for this binding. */
    public var offset: Long = 0,
    /** The size of the buffer range for this binding. */
    public var size: Long = Constants.WHOLE_SIZE,
    /** The sampler to bind, if the resource is a sampler. */
    public var sampler: GPUSampler? = null,
    public var textureView: GPUTextureView? = null,
)
