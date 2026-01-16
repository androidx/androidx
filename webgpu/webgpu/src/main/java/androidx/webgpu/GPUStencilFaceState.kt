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

/** Defines the state and operations for the front or back face in stencil testing. */
public class GPUStencilFaceState
@JvmOverloads
constructor(
    /** The comparison function to pass the stencil test. */
    @CompareFunction public var compare: Int = CompareFunction.Always,
    @StencilOperation public var failOp: Int = StencilOperation.Keep,
    @StencilOperation public var depthFailOp: Int = StencilOperation.Keep,
    @StencilOperation public var passOp: Int = StencilOperation.Keep,
)
