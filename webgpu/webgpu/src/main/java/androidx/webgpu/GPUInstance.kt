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

/** The entry point for the WebGPU API; used for adapter and surface discovery/creation. */
public class GPUInstance private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Creates a surface object representing a drawing target (e.g., a window).
     *
     * @param descriptor A descriptor specifying creation options for the surface.
     * @return The newly created surface.
     */
    @FastNative
    @JvmName("createSurface")
    @JvmOverloads
    public external fun createSurface(
        descriptor: GPUSurfaceDescriptor = GPUSurfaceDescriptor()
    ): GPUSurface

    /** Gets the set of WGSL language features supported by the instance. */
    @FastNative
    @JvmName("getWGSLLanguageFeatures")
    public external fun getWGSLLanguageFeatures(): GPUSupportedWGSLLanguageFeatures

    /**
     * Checks if a specific WGSL language feature is supported.
     *
     * @param feature The WGSL language feature to query.
     * @return True if the feature is supported, {@code false} otherwise.
     */
    @FastNative
    @JvmName("hasWGSLLanguageFeature")
    public external fun hasWGSLLanguageFeature(@WGSLLanguageFeatureName feature: Int): Boolean

    /** Processes all pending WebGPU events, including invoking completed callbacks. */
    @FastNative @JvmName("processEvents") public external fun processEvents(): Unit

    /** Asynchronously requests a suitable GPU adapter. */
    @FastNative
    @JvmName("requestAdapter")
    @JvmOverloads
    public external fun requestAdapter(
        callbackExecutor: java.util.concurrent.Executor,
        options: GPURequestAdapterOptions? = null,
        callback: GPURequestCallback<GPUAdapter>,
    ): Unit

    /**
     * Asynchronously requests a suitable GPU adapter.
     *
     * @param options Options for selecting the adapter.
     */
    @Throws(WebGpuException::class)
    public suspend fun requestAdapter(options: GPURequestAdapterOptions? = null): GPUAdapter =
        awaitGPURequest { callback ->
            requestAdapter(Executor(Runnable::run), options, callback)
        }

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUInstance && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
