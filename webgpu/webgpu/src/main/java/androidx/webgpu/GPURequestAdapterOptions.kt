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

/** Options for selecting a GPU adapter. */
public class GPURequestAdapterOptions
@JvmOverloads
constructor(
    @FeatureLevel public var featureLevel: Int = FeatureLevel.Core,
    @PowerPreference public var powerPreference: Int = PowerPreference.Undefined,
    public var forceFallbackAdapter: Boolean = false,
    @BackendType public var backendType: Int = BackendType.Undefined,
    public var compatibleSurface: GPUSurface? = null,
    public var requestAdapterWebXROptions: GPURequestAdapterWebXROptions? = null,
)
