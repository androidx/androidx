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

/** A descriptor for configuring a surface. */
public class GPUSurfaceConfiguration
@JvmOverloads
constructor(
    /** The device that will be used to render to the surface. */
    public var device: GPUDevice,
    /** The width of the surface's textures. */
    public var width: Int,
    /** The height of the surface's textures. */
    public var height: Int,
    /** The preferred texture format for the surface's textures. */
    @TextureFormat public var format: Int = TextureFormat.Undefined,
    /** The texture usage flags for textures created by the surface. */
    @TextureUsage public var usage: Int = TextureUsage.RenderAttachment,
    @TextureFormat public var viewFormats: IntArray = intArrayOf(),
    @CompositeAlphaMode public var alphaMode: Int = CompositeAlphaMode.Auto,
    @PresentMode public var presentMode: Int = PresentMode.Fifo,
)
