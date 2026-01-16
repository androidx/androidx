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

/** Used to record a sequence of GPU commands and produce a command buffer. */
public class GPUCommandEncoder private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Starts recording a compute pass.
     *
     * @param descriptor The descriptor for the compute pass.
     * @return A compute pass encoder for recording compute commands.
     */
    @FastNative
    @JvmName("beginComputePass")
    @JvmOverloads
    public external fun beginComputePass(
        descriptor: GPUComputePassDescriptor? = null
    ): GPUComputePassEncoder

    /**
     * Starts recording a render pass.
     *
     * @param descriptor The descriptor for the render pass.
     * @return A render pass encoder for recording rendering commands.
     */
    @FastNative
    @JvmName("beginRenderPass")
    public external fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder

    /**
     * Clears a range of a buffer to zero.
     *
     * @param buffer The buffer to clear.
     * @param offset The starting offset in bytes to clear from.
     * @param size The size in bytes to clear.
     */
    @FastNative
    @JvmName("clearBuffer")
    @JvmOverloads
    public external fun clearBuffer(
        buffer: GPUBuffer,
        offset: Long = 0,
        size: Long = Constants.WHOLE_SIZE,
    ): Unit

    /**
     * Copies data from one buffer to another.
     *
     * @param source The source buffer.
     * @param sourceOffset The starting offset in bytes in the source buffer.
     * @param destination The destination buffer.
     * @param destinationOffset The starting offset in bytes in the destination buffer.
     * @param size The size in bytes to copy.
     */
    @FastNative
    @JvmName("copyBufferToBuffer")
    public external fun copyBufferToBuffer(
        source: GPUBuffer,
        sourceOffset: Long,
        destination: GPUBuffer,
        destinationOffset: Long,
        size: Long,
    ): Unit

    /**
     * Copies data from a buffer into a texture.
     *
     * @param source Information about the source buffer and data layout.
     * @param destination Information about the destination texture and coordinates.
     * @param copySize The size (width, height, depth/layers) of the region to copy.
     */
    @FastNative
    @JvmName("copyBufferToTexture")
    public external fun copyBufferToTexture(
        source: GPUTexelCopyBufferInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ): Unit

    /**
     * Copies data from a texture into a buffer.
     *
     * @param source Information about the source texture and coordinates.
     * @param destination Information about the destination buffer and data layout.
     * @param copySize The size (width, height, depth/layers) of the region to copy.
     */
    @FastNative
    @JvmName("copyTextureToBuffer")
    public external fun copyTextureToBuffer(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyBufferInfo,
        copySize: GPUExtent3D,
    ): Unit

    /**
     * Copies data from one texture to another.
     *
     * @param source Information about the source texture and coordinates.
     * @param destination Information about the destination texture and coordinates.
     * @param copySize The size (width, height, depth/layers) of the region to copy.
     */
    @FastNative
    @JvmName("copyTextureToTexture")
    public external fun copyTextureToTexture(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ): Unit

    /**
     * Finalizes the recorded commands and creates an immutable command buffer.
     *
     * @param descriptor The descriptor for the resulting command buffer.
     * @return The generated command buffer.
     */
    @FastNative
    @JvmName("finish")
    @JvmOverloads
    public external fun finish(descriptor: GPUCommandBufferDescriptor? = null): GPUCommandBuffer

    /**
     * Inserts a debug marker command into the command stream.
     *
     * @param markerLabel The label for the debug marker.
     */
    @FastNative
    @JvmName("insertDebugMarker")
    public external fun insertDebugMarker(markerLabel: String): Unit

    /** Ends the most recently pushed debug group. */
    @FastNative @JvmName("popDebugGroup") public external fun popDebugGroup(): Unit

    /**
     * Starts a new named debug group.
     *
     * @param groupLabel The label for the debug group.
     */
    @FastNative
    @JvmName("pushDebugGroup")
    public external fun pushDebugGroup(groupLabel: String): Unit

    /**
     * Writes the results of a range of queries into a destination buffer.
     *
     * @param querySet The query set containing the queries to resolve.
     * @param firstQuery The index of the first query to resolve.
     * @param queryCount The number of queries to resolve.
     * @param destination The buffer to write the resolved query results to.
     * @param destinationOffset The offset in the destination buffer to start writing.
     */
    @FastNative
    @JvmName("resolveQuerySet")
    public external fun resolveQuerySet(
        querySet: GPUQuerySet,
        firstQuery: Int,
        queryCount: Int,
        destination: GPUBuffer,
        destinationOffset: Long,
    ): Unit

    /**
     * Sets a debug label for the command encoder.
     *
     * @param label The label to assign to the command encoder.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /**
     * Writes a GPU timestamp into a query set at the current point in the command stream.
     *
     * @param querySet The query set to write the timestamp to.
     * @param queryIndex The index in the query set to write the timestamp to.
     */
    @FastNative
    @JvmName("writeTimestamp")
    public external fun writeTimestamp(querySet: GPUQuerySet, queryIndex: Int): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUCommandEncoder && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
