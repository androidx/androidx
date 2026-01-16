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

/** Defines how the color components of a texture are remapped for a texture view. */
public class GPUTextureComponentSwizzle
@JvmOverloads
constructor(
    /** The source component for the view's red channel. */
    @ComponentSwizzle public var r: Int = ComponentSwizzle.R,
    /** The source component for the view's green channel. */
    @ComponentSwizzle public var g: Int = ComponentSwizzle.G,
    /** The source component for the view's blue channel. */
    @ComponentSwizzle public var b: Int = ComponentSwizzle.B,
    /** The source component for the view's alpha channel. */
    @ComponentSwizzle public var a: Int = ComponentSwizzle.A,
)
