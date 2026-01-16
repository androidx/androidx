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
import java.nio.ByteBuffer
import java.util.concurrent.Executor

/** Used to submit recorded command buffers to the GPU for execution. */
public class GPUQueue private constructor(public val handle: Long) : AutoCloseable {
    /** Registers a callback to be invoked when all previously submitted work completes. */
    @FastNative
    @JvmName("onSubmittedWorkDone")
    public external fun onSubmittedWorkDone(
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<Unit>,
    ): Unit

    /** Registers a callback to be invoked when all previously submitted work completes. */
    @Throws(WebGpuException::class)
    public suspend fun onSubmittedWorkDone(): Unit = awaitGPURequest { callback ->
        onSubmittedWorkDone(Executor(Runnable::run), callback)
    }

    /**
     * Sets a debug label for the queue.
     *
     * @param label The label to assign to the queue.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /**
     * Submits a list of command buffers for execution on the GPU.
     *
     * @param commands An array of command buffers to submit.
     */
    @FastNative
    @JvmName("submit")
    @JvmOverloads
    public external fun submit(commands: Array<GPUCommandBuffer> = arrayOf()): Unit

    /**
     * Synchronously writes data from CPU memory to a GPU buffer.
     *
     * @param buffer The destination buffer.
     * @param bufferOffset The offset in the buffer to start writing.
     * @param data A pointer to the source CPU data.
     */
    @FastNative
    @JvmName("writeBuffer")
    public external fun writeBuffer(
        buffer: GPUBuffer,
        bufferOffset: Long,
        data: java.nio.ByteBuffer,
    ): Unit

    /**
     * Synchronously writes data from CPU memory to a GPU texture.
     *
     * @param destination Information about the destination texture and coordinates.
     * @param data A pointer to the source CPU data.
     * @param writeSize The size (width, height, depth/layers) of the region to write.
     * @param dataLayout The layout of the data in CPU memory.
     */
    @FastNative
    @JvmName("writeTexture")
    @JvmOverloads
    public external fun writeTexture(
        destination: GPUTexelCopyTextureInfo,
        data: java.nio.ByteBuffer,
        writeSize: GPUExtent3D,
        dataLayout: GPUTexelCopyBufferLayout = GPUTexelCopyBufferLayout(),
    ): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUQueue && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
