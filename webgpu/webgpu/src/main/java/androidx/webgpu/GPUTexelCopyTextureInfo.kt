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

/** Information about a texture used as a source or destination for a texel copy operation. */
public class GPUTexelCopyTextureInfo
@JvmOverloads
constructor(
    /** The texture involved in the copy. */
    public var texture: GPUTexture,
    public var mipLevel: Int = 0,
    /** The origin (x, y, z/layer) within the texture where the copy starts. */
    public var origin: GPUOrigin3D = GPUOrigin3D(),
    /** The aspect of the texture (color, depth, or stencil) to copy. */
    @TextureAspect public var aspect: Int = TextureAspect.All,
)
