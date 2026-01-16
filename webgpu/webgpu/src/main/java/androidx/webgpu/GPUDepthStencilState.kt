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

/** Defines the state for depth and stencil testing and operations. */
public class GPUDepthStencilState
@JvmOverloads
constructor(
    /** The texture format of the depth/stencil attachment. */
    @TextureFormat public var format: Int = TextureFormat.Undefined,
    @OptionalBool public var depthWriteEnabled: Int = OptionalBool.Undefined,
    @CompareFunction public var depthCompare: Int = CompareFunction.Undefined,
    public var stencilFront: GPUStencilFaceState = GPUStencilFaceState(),
    public var stencilBack: GPUStencilFaceState = GPUStencilFaceState(),
    public var stencilReadMask: Int = -0x7FFFFFFF,
    public var stencilWriteMask: Int = -0x7FFFFFFF,
    public var depthBias: Int = 0,
    public var depthBiasSlopeScale: Float = 0.0f,
    public var depthBiasClamp: Float = 0.0f,
)
