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

/** A structure representing the width, height, and depth/layer count of a 3D region. */
public class GPUExtent3D
@JvmOverloads
constructor(
    /** The width of the extent. */
    public var width: Int,
    /** The height of the extent. */
    public var height: Int = 1,
    public var depthOrArrayLayers: Int = 1,
)
