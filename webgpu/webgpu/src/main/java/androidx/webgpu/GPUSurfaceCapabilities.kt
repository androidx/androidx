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

/** A structure containing the presentation capabilities of a surface for a given adapter. */
public class GPUSurfaceCapabilities
@JvmOverloads
constructor(
    /** The supported texture usage flags for textures created from the surface. */
    @TextureUsage public var usages: Int,
    /** An array of supported texture formats for the surface. */
    @TextureFormat public var formats: IntArray = intArrayOf(),
    @PresentMode public var presentModes: IntArray = intArrayOf(),
    @CompositeAlphaMode public var alphaModes: IntArray = intArrayOf(),
)
