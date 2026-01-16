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

/** Defines the layout of image data within a buffer for copy operations. */
public class GPUTexelCopyBufferLayout
@JvmOverloads
constructor(
    /** The offset in bytes from the start of the buffer. */
    public var offset: Long = 0,
    public var bytesPerRow: Int = Constants.COPY_STRIDE_UNDEFINED,
    public var rowsPerImage: Int = Constants.COPY_STRIDE_UNDEFINED,
)
