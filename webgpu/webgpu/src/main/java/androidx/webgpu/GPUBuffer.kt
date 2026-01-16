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

/** A chunk of memory allocated on the GPU, used for vertex data, uniforms, storage, etc. */
public class GPUBuffer private constructor(public val handle: Long) : AutoCloseable {
    /** Immediately destroys the buffer resource. */
    @FastNative @JvmName("destroy") public external fun destroy(): Unit

    /**
     * Returns an immutable pointer to the mapped memory range for reading.
     *
     * @param offset The offset in bytes of the mapped range to retrieve.
     * @param size The size in bytes of the mapped range to retrieve. Can be whole_map_size.
     * @return An immutable pointer to the mapped buffer memory.
     */
    @FastNative
    @JvmName("getConstMappedRange")
    @JvmOverloads
    public external fun getConstMappedRange(
        offset: Long = 0,
        size: Long = Constants.WHOLE_MAP_SIZE,
    ): ByteBuffer

    /**
     * Returns a mutable pointer to the mapped memory range for writing.
     *
     * @param offset The offset in bytes of the mapped range to retrieve.
     * @param size The size in bytes of the mapped range to retrieve. Can be whole_map_size.
     * @return A mutable pointer to the mapped buffer memory.
     */
    @FastNative
    @JvmName("getMappedRange")
    @JvmOverloads
    public external fun getMappedRange(
        offset: Long = 0,
        size: Long = Constants.WHOLE_MAP_SIZE,
    ): ByteBuffer

    /**
     * Gets the current mapping state of the buffer.
     *
     * @return The buffer's map state.
     */
    @FastNative @JvmName("getMapState") @BufferMapState public external fun getMapState(): Int

    @get:JvmName("mapState")
    public val mapState: Int
        get() = getMapState()

    /**
     * Gets the size of the buffer in bytes.
     *
     * @return The size of the buffer.
     */
    @FastNative @JvmName("getSize") public external fun getSize(): Long

    @get:JvmName("size")
    public val size: Long
        get() = getSize()

    /**
     * Gets the usage flags the buffer was created with.
     *
     * @return The buffer's usage flags.
     */
    @FastNative @JvmName("getUsage") @BufferUsage public external fun getUsage(): Int

    @get:JvmName("usage")
    public val usage: Int
        get() = getUsage()

    /** Requests to map a range of the buffer into CPU-accessible memory asynchronously. */
    @FastNative
    @JvmName("mapAsync")
    public external fun mapAsync(
        @MapMode mode: Int,
        offset: Long,
        size: Long,
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<Unit>,
    ): Unit

    /**
     * Requests to map a range of the buffer into CPU-accessible memory asynchronously.
     *
     * @param mode The desired access mode for the mapping.
     * @param offset The offset in bytes where the mapping should start.
     * @param size The size in bytes of the range to map. Can be whole_map_size.
     */
    @Throws(WebGpuException::class)
    public suspend fun mapAndAwait(@MapMode mode: Int, offset: Long, size: Long): Unit =
        awaitGPURequest { callback ->
            mapAsync(mode, offset, size, Executor(Runnable::run), callback)
        }

    /**
     * Reads data from a mapped buffer into CPU memory.
     *
     * @param offset The offset in the mapped buffer to read from.
     * @param data A pointer to the destination CPU memory.
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("readMappedRange")
    @Throws(WebGpuException::class)
    public external fun readMappedRange(offset: Long, data: java.nio.ByteBuffer): Unit

    /**
     * Sets a debug label for the buffer.
     *
     * @param label The label to assign to the buffer.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /**
     * Unmaps the buffer, making it inaccessible by the CPU and potentially usable by the GPU again.
     */
    @FastNative @JvmName("unmap") public external fun unmap(): Unit

    /**
     * Writes data from CPU memory into a mapped buffer.
     *
     * @param offset The offset in the mapped buffer to write to.
     * @param data A pointer to the source CPU memory.
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("writeMappedRange")
    @Throws(WebGpuException::class)
    public external fun writeMappedRange(offset: Long, data: java.nio.ByteBuffer): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUBuffer && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
