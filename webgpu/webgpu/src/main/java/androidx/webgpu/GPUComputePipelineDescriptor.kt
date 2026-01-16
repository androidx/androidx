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

/** A descriptor for creating a compute pipeline. */
public class GPUComputePipelineDescriptor
@JvmOverloads
constructor(
    /** The entry point and configuration for the compute shader stage. */
    public var compute: GPUComputeState,
    /** The label for the compute pipeline. */
    public var label: String? = null,
    /** The layout of the bind groups and push constants used by the pipeline. */
    public var layout: GPUPipelineLayout? = null,
)
