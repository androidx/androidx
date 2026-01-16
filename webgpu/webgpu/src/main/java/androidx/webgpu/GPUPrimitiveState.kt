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

/** Defines the state for primitive assembly (e.g., topology, culling, winding order). */
public class GPUPrimitiveState
@JvmOverloads
constructor(
    /** The type of primitive to render (e.g., triangle-list, line-strip). */
    @PrimitiveTopology public var topology: Int = PrimitiveTopology.TriangleList,
    @IndexFormat public var stripIndexFormat: Int = IndexFormat.Undefined,
    @FrontFace public var frontFace: Int = FrontFace.CCW,
    @CullMode public var cullMode: Int = CullMode.None,
    public var unclippedDepth: Boolean = false,
)
