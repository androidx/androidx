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

/** The required configuration for a buffer binding in a bind group layout. */
public class GPUBufferBindingLayout
@JvmOverloads
constructor(
    /** The type of buffer access (uniform, storage, read-only storage). */
    @BufferBindingType public var type: Int = BufferBindingType.Uniform,
    public var hasDynamicOffset: Boolean = false,
    public var minBindingSize: Long = 0,
)
