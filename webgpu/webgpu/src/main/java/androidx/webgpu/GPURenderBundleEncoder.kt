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

/** Used to record rendering commands into a render bundle. */
public class GPURenderBundleEncoder private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Draws primitives without an index buffer.
     *
     * @param vertexCount The number of vertices to draw.
     * @param instanceCount The number of instances to draw.
     * @param firstVertex The index of the first vertex to draw.
     * @param firstInstance The index of the first instance to draw.
     */
    @FastNative
    @JvmName("draw")
    @JvmOverloads
    public external fun draw(
        vertexCount: Int,
        instanceCount: Int = 1,
        firstVertex: Int = 0,
        firstInstance: Int = 0,
    ): Unit

    /**
     * Draws primitives using an index buffer.
     *
     * @param indexCount The number of indices to use.
     * @param instanceCount The number of instances to draw.
     * @param firstIndex The index of the first element in the index buffer.
     * @param baseVertex A signed integer added to each index value read from the index buffer.
     * @param firstInstance The index of the first instance to draw.
     */
    @FastNative
    @JvmName("drawIndexed")
    @JvmOverloads
    public external fun drawIndexed(
        indexCount: Int,
        instanceCount: Int = 1,
        firstIndex: Int = 0,
        baseVertex: Int = 0,
        firstInstance: Int = 0,
    ): Unit

    /**
     * Draws primitives using an index buffer with arguments from a buffer.
     *
     * @param indirectBuffer The buffer containing the indexed draw arguments.
     * @param indirectOffset The offset in the buffer where indexed draw arguments start.
     */
    @FastNative
    @JvmName("drawIndexedIndirect")
    public external fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: Long): Unit

    /**
     * Draws primitives without an index buffer using arguments from a buffer.
     *
     * @param indirectBuffer The buffer containing the draw arguments.
     * @param indirectOffset The offset in the buffer where draw arguments start.
     */
    @FastNative
    @JvmName("drawIndirect")
    public external fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: Long): Unit

    /**
     * Finalizes the recorded commands and creates an immutable render bundle.
     *
     * @param descriptor The descriptor for the resulting render bundle.
     * @return The generated render bundle.
     */
    @FastNative
    @JvmName("finish")
    @JvmOverloads
    public external fun finish(descriptor: GPURenderBundleDescriptor? = null): GPURenderBundle

    /**
     * Inserts a debug marker command into the bundle.
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
     * Sets the bind group for a given index.
     *
     * @param groupIndex The index of the bind group to set.
     * @param group The bind group object to set, or {@code null} to unbind.
     * @param dynamicOffsets An array of dynamic offsets for uniform/storage buffers.
     */
    @FastNative
    @JvmName("setBindGroup")
    @JvmOverloads
    public external fun setBindGroup(
        groupIndex: Int,
        group: GPUBindGroup? = null,
        dynamicOffsets: IntArray = intArrayOf(),
    ): Unit

    /**
     * Binds an index buffer to be used for indexed drawing.
     *
     * @param buffer The index buffer.
     * @param format The format of the indices in the buffer.
     * @param offset The offset in the buffer to start reading index data.
     * @param size The size of the index buffer range to use.
     */
    @FastNative
    @JvmName("setIndexBuffer")
    @JvmOverloads
    public external fun setIndexBuffer(
        buffer: GPUBuffer,
        @IndexFormat format: Int = IndexFormat.Undefined,
        offset: Long = 0,
        size: Long = Constants.WHOLE_SIZE,
    ): Unit

    /**
     * Sets a debug label for the render bundle encoder.
     *
     * @param label The label to assign to the render bundle encoder.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /**
     * Sets the active render pipeline.
     *
     * @param pipeline The render pipeline to use for subsequent drawing calls.
     */
    @FastNative
    @JvmName("setPipeline")
    public external fun setPipeline(pipeline: GPURenderPipeline): Unit

    /**
     * Binds a vertex buffer to a specific slot.
     *
     * @param slot The vertex buffer slot index.
     * @param buffer The buffer to bind to the slot.
     * @param offset The offset in the buffer to start reading vertex data.
     * @param size The size of the vertex buffer range to use.
     */
    @FastNative
    @JvmName("setVertexBuffer")
    @JvmOverloads
    public external fun setVertexBuffer(
        slot: Int,
        buffer: GPUBuffer? = null,
        offset: Long = 0,
        size: Long = Constants.WHOLE_SIZE,
    ): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean =
        other is GPURenderBundleEncoder && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
