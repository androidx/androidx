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

/** Defines the depth and stencil attachment configuration for a render pass. */
public class GPURenderPassDepthStencilAttachment
@JvmOverloads
constructor(
    /** The texture view used as the depth/stencil attachment. */
    public var view: GPUTextureView,
    @LoadOp public var depthLoadOp: Int = LoadOp.Undefined,
    @StoreOp public var depthStoreOp: Int = StoreOp.Undefined,
    public var depthClearValue: Float = Constants.DEPTH_CLEAR_VALUE_UNDEFINED,
    public var depthReadOnly: Boolean = false,
    @LoadOp public var stencilLoadOp: Int = LoadOp.Undefined,
    @StoreOp public var stencilStoreOp: Int = StoreOp.Undefined,
    public var stencilClearValue: Int = 0,
    public var stencilReadOnly: Boolean = false,
)
