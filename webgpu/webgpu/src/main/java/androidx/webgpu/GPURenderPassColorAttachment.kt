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

/** Defines a single color attachment configuration for a render pass. */
public class GPURenderPassColorAttachment
@JvmOverloads
constructor(
    public var clearValue: GPUColor,
    /** The texture view used as the color attachment, or {@code null} if discarded. */
    public var view: GPUTextureView? = null,
    public var depthSlice: Int = Constants.DEPTH_SLICE_UNDEFINED,
    public var resolveTarget: GPUTextureView? = null,
    @LoadOp public var loadOp: Int = LoadOp.Undefined,
    @StoreOp public var storeOp: Int = StoreOp.Undefined,
)
