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
@file:JvmName("Functions")

package androidx.webgpu

import dalvik.annotation.optimization.FastNative

public object GPU {

    /**
     * Creates a new WebGPU instance object.
     *
     * @param descriptor A descriptor specifying creation options for the instance.
     * @return The newly created WebGPU instance.
     */
    @FastNative
    public external fun createInstance(descriptor: GPUInstanceDescriptor? = null): GPUInstance

    /**
     * Gets the set of instance features supported by the implementation.
     *
     * @param features A structure to be filled with the supported instance features.
     */
    @FastNative
    public external fun getInstanceFeatures(
        features: GPUSupportedInstanceFeatures
    ): GPUSupportedInstanceFeatures

    /**
     * Gets the limits supported by the instance.
     *
     * @param limits A structure to be filled with the instance limits.
     * @return Status code of the operation.
     */
    @FastNative
    @Throws(WebGpuException::class)
    public external fun getInstanceLimits(
        limits: GPUInstanceLimits = GPUInstanceLimits()
    ): GPUInstanceLimits

    /**
     * Checks if a specific instance feature is supported by the implementation.
     *
     * @param feature The instance feature to query.
     * @return True if the feature is supported, {@code false} otherwise.
     */
    @FastNative public external fun hasInstanceFeature(@InstanceFeatureName feature: Int): Boolean
}
