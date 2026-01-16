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

/** The required configuration for a storage texture binding in a bind group layout. */
public class GPUStorageTextureBindingLayout
@JvmOverloads
constructor(
    /** The shader access mode (read-only, write-only, or read-write). */
    @StorageTextureAccess public var access: Int = StorageTextureAccess.WriteOnly,
    /** The storage texture format. */
    @TextureFormat public var format: Int = TextureFormat.Undefined,
    @TextureViewDimension public var viewDimension: Int = TextureViewDimension._2D,
)
