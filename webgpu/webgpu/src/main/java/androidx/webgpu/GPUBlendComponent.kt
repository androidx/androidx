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

/** Defines the operation and factors for one component (color or alpha) in blending. */
public class GPUBlendComponent
@JvmOverloads
constructor(
    /** The blending operation to perform. */
    @BlendOperation public var operation: Int = BlendOperation.Add,
    @BlendFactor public var srcFactor: Int = BlendFactor.One,
    @BlendFactor public var dstFactor: Int = BlendFactor.Zero,
)
