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

import dalvik.annotation.optimization.FastNative
import java.util.concurrent.Executor

/** Represents an abstract graphics card on the system. */
public class GPUAdapter private constructor(public val handle: Long) : AutoCloseable {
    /** Gets the set of features supported by the adapter. */
    @FastNative @JvmName("getFeatures") public external fun getFeatures(): GPUSupportedFeatures

    /**
     * Gets detailed information about the adapter.
     *
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("getInfo")
    @Throws(WebGpuException::class)
    public external fun getInfo(): GPUAdapterInfo

    /**
     * Gets the limits supported by the adapter.
     *
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("getLimits")
    @Throws(WebGpuException::class)
    public external fun getLimits(): GPULimits

    /**
     * Checks if a specific feature is supported by the adapter.
     *
     * @param feature The feature to check for support.
     * @return True if the feature is supported, {@code false} otherwise.
     */
    @FastNative
    @JvmName("hasFeature")
    public external fun hasFeature(@FeatureName feature: Int): Boolean

    /** Requests a GPU device object from the adapter asynchronously. */
    @FastNative
    @JvmName("requestDevice")
    @JvmOverloads
    public external fun requestDevice(
        callbackExecutor: java.util.concurrent.Executor,
        descriptor: GPUDeviceDescriptor? = null,
        callback: GPURequestCallback<GPUDevice>,
    ): Unit

    /**
     * Requests a GPU device object from the adapter asynchronously.
     *
     * @param descriptor A descriptor specifying creation options for the device.
     */
    @Throws(WebGpuException::class)
    public suspend fun requestDevice(descriptor: GPUDeviceDescriptor? = null): GPUDevice =
        awaitGPURequest { callback ->
            requestDevice(Executor(Runnable::run), descriptor, callback)
        }

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUAdapter && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
