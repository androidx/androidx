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

/** A descriptor for creating a render bundle encoder. */
public class GPURenderBundleEncoderDescriptor
@JvmOverloads
constructor(
    /** The label for the render bundle encoder. */
    public var label: String? = null,
    @TextureFormat public var colorFormats: IntArray = intArrayOf(),
    @TextureFormat public var depthStencilFormat: Int = TextureFormat.Undefined,
    public var sampleCount: Int = 1,
    public var depthReadOnly: Boolean = false,
    public var stencilReadOnly: Boolean = false,
)
