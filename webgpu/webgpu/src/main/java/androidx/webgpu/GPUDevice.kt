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

/** The primary interface for interacting with the GPU, used to create most resources. */
public class GPUDevice private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Creates a new bind group.
     *
     * @param descriptor The descriptor for the bind group.
     * @return The newly created bind group.
     */
    @FastNative
    @JvmName("createBindGroup")
    public external fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup

    /**
     * Creates a new bind group layout.
     *
     * @param descriptor The descriptor for the bind group layout.
     * @return The newly created bind group layout.
     */
    @FastNative
    @JvmName("createBindGroupLayout")
    @JvmOverloads
    public external fun createBindGroupLayout(
        descriptor: GPUBindGroupLayoutDescriptor = GPUBindGroupLayoutDescriptor()
    ): GPUBindGroupLayout

    /**
     * Creates a new GPU buffer.
     *
     * @param descriptor The descriptor for the buffer.
     * @return The newly created buffer, or {@code null} on failure.
     */
    @FastNative
    @JvmName("createBuffer")
    public external fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer

    /**
     * Creates a new command encoder to record command buffers.
     *
     * @param descriptor The descriptor for the command encoder.
     * @return The newly created command encoder.
     */
    @FastNative
    @JvmName("createCommandEncoder")
    @JvmOverloads
    public external fun createCommandEncoder(
        descriptor: GPUCommandEncoderDescriptor? = null
    ): GPUCommandEncoder

    /**
     * Creates a new compute pipeline synchronously.
     *
     * @param descriptor The descriptor for the compute pipeline.
     * @return The newly created compute pipeline.
     */
    @FastNative
    @JvmName("createComputePipeline")
    public external fun createComputePipeline(
        descriptor: GPUComputePipelineDescriptor
    ): GPUComputePipeline

    /** Creates a new compute pipeline asynchronously. */
    @FastNative
    @JvmName("createComputePipelineAsync")
    public external fun createComputePipelineAsync(
        descriptor: GPUComputePipelineDescriptor,
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<GPUComputePipeline>,
    ): Unit

    /**
     * Creates a new compute pipeline asynchronously.
     *
     * @param descriptor The descriptor for the compute pipeline.
     */
    @Throws(WebGpuException::class)
    public suspend fun createComputePipelineAndAwait(
        descriptor: GPUComputePipelineDescriptor
    ): GPUComputePipeline = awaitGPURequest { callback ->
        createComputePipelineAsync(descriptor, Executor(Runnable::run), callback)
    }

    /**
     * Creates a new pipeline layout.
     *
     * @param descriptor The descriptor for the pipeline layout.
     * @return The newly created pipeline layout.
     */
    @FastNative
    @JvmName("createPipelineLayout")
    public external fun createPipelineLayout(
        descriptor: GPUPipelineLayoutDescriptor
    ): GPUPipelineLayout

    /**
     * Creates a new query set.
     *
     * @param descriptor The descriptor for the query set.
     * @return The newly created query set.
     */
    @FastNative
    @JvmName("createQuerySet")
    public external fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet

    /**
     * Creates a new render bundle encoder to record render bundles.
     *
     * @param descriptor The descriptor for the render bundle encoder.
     * @return The newly created render bundle encoder.
     */
    @FastNative
    @JvmName("createRenderBundleEncoder")
    public external fun createRenderBundleEncoder(
        descriptor: GPURenderBundleEncoderDescriptor
    ): GPURenderBundleEncoder

    /**
     * Creates a new render pipeline synchronously.
     *
     * @param descriptor The descriptor for the render pipeline.
     * @return The newly created render pipeline.
     */
    @FastNative
    @JvmName("createRenderPipeline")
    public external fun createRenderPipeline(
        descriptor: GPURenderPipelineDescriptor
    ): GPURenderPipeline

    /** Creates a new render pipeline asynchronously. */
    @FastNative
    @JvmName("createRenderPipelineAsync")
    public external fun createRenderPipelineAsync(
        descriptor: GPURenderPipelineDescriptor,
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<GPURenderPipeline>,
    ): Unit

    /**
     * Creates a new render pipeline asynchronously.
     *
     * @param descriptor The descriptor for the render pipeline.
     */
    @Throws(WebGpuException::class)
    public suspend fun createRenderPipelineAndAwait(
        descriptor: GPURenderPipelineDescriptor
    ): GPURenderPipeline = awaitGPURequest { callback ->
        createRenderPipelineAsync(descriptor, Executor(Runnable::run), callback)
    }

    /**
     * Creates a new sampler.
     *
     * @param descriptor The descriptor for the sampler.
     * @return The newly created sampler.
     */
    @FastNative
    @JvmName("createSampler")
    @JvmOverloads
    public external fun createSampler(descriptor: GPUSamplerDescriptor? = null): GPUSampler

    /**
     * Creates a new shader module.
     *
     * @param descriptor The descriptor for the shader module.
     * @return The newly created shader module.
     */
    @FastNative
    @JvmName("createShaderModule")
    @JvmOverloads
    public external fun createShaderModule(
        descriptor: GPUShaderModuleDescriptor = GPUShaderModuleDescriptor()
    ): GPUShaderModule

    /**
     * Creates a new GPU texture.
     *
     * @param descriptor The descriptor for the texture.
     * @return The newly created texture.
     */
    @FastNative
    @JvmName("createTexture")
    public external fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture

    /** Destroys the device and frees its resources. The device becomes lost. */
    @FastNative @JvmName("destroy") public external fun destroy(): Unit

    /**
     * Gets information about the adapter that was used to create this device.
     *
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("getAdapterInfo")
    @Throws(WebGpuException::class)
    public external fun getAdapterInfo(): GPUAdapterInfo

    /** Gets the set of features supported by the device. */
    @FastNative @JvmName("getFeatures") public external fun getFeatures(): GPUSupportedFeatures

    /**
     * Gets the limits supported by the device.
     *
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("getLimits")
    @Throws(WebGpuException::class)
    public external fun getLimits(): GPULimits

    /**
     * Gets the queue object for submitting commands to the GPU.
     *
     * @return The device's queue.
     */
    @FastNative @JvmName("getQueue") public external fun getQueue(): GPUQueue

    @get:JvmName("queue")
    public val queue: GPUQueue
        get() = getQueue()

    /**
     * Checks if a specific feature is enabled on the device.
     *
     * @param feature The feature to check for support.
     * @return True if the feature is enabled, {@code false} otherwise.
     */
    @FastNative
    @JvmName("hasFeature")
    public external fun hasFeature(@FeatureName feature: Int): Boolean

    /** Pops the current error scope from the stack asynchronously and returns a possible error. */
    @FastNative
    @JvmName("popErrorScope")
    public external fun popErrorScope(
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<@ErrorType Int>,
    ): Unit

    /** Pops the current error scope from the stack asynchronously and returns a possible error. */
    @Throws(WebGpuException::class, WebGpuRuntimeException::class)
    public suspend fun popErrorScope(): @ErrorType Int = awaitGPURequest { callback ->
        popErrorScope(Executor(Runnable::run), callback)
    }

    /**
     * Pushes a new error scope onto the device's error scope stack.
     *
     * @param filter The type of errors to filter and capture in the new scope.
     */
    @FastNative
    @JvmName("pushErrorScope")
    public external fun pushErrorScope(@ErrorFilter filter: Int): Unit

    /**
     * Sets a debug label for the device.
     *
     * @param label The label to assign to the device.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUDevice && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
