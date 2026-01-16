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

/** Information about a buffer used as a source or destination for a texel copy operation. */
public class GPUTexelCopyBufferInfo
@JvmOverloads
constructor(
    /** The buffer involved in the copy. */
    public var buffer: GPUBuffer,
    /** The data layout of the texel data within the buffer. */
    public var layout: GPUTexelCopyBufferLayout = GPUTexelCopyBufferLayout(),
)
