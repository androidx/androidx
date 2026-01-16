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

/** A descriptor used to create a GPU buffer. */
public class GPUBufferDescriptor
@JvmOverloads
constructor(
    /** The allowed usages for the buffer (e.g., vertex, uniform, copy_dst). */
    @BufferUsage public var usage: Int,
    /** The size of the buffer in bytes. */
    public var size: Long,
    /** The label for the buffer. */
    public var label: String? = null,
    public var mappedAtCreation: Boolean = false,
)
