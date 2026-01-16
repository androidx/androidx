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

/** Information about a GPU adapter. */
public class GPUAdapterInfo
@JvmOverloads
constructor(
    /** The name of the vendor (e.g., 'NVIDIA', 'AMD'). */
    public var vendor: String,
    /** The architecture of the adapter (e.g., 'Volta'). */
    public var architecture: String,
    /** The name of the specific device. */
    public var device: String,
    /** A human-readable description of the adapter. */
    public var description: String,
    @AdapterType public var adapterType: Int,
    public var vendorID: Int,
    public var deviceID: Int,
    public var subgroupMinSize: Int,
    public var subgroupMaxSize: Int,
    @BackendType public var backendType: Int = BackendType.Undefined,
)
