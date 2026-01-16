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

/** Used to record rendering commands within a command encoder, operating on render attachments. */
public class GPURenderPassEncoder private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Begins an occlusion query in the current render pass.
     *
     * @param queryIndex The index in the occlusion query set to record the starting sample count.
     */
    @FastNative
    @JvmName("beginOcclusionQuery")
    public external fun beginOcclusionQuery(queryIndex: Int): Unit

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

    /** Ends the render pass. */
    @FastNative @JvmName("end") public external fun end(): Unit

    /** Ends the current occlusion query. */
    @FastNative @JvmName("endOcclusionQuery") public external fun endOcclusionQuery(): Unit

    /**
     * Executes a set of pre-recorded render bundles.
     *
     * @param bundles An array of render bundles to execute.
     */
    @FastNative
    @JvmName("executeBundles")
    @JvmOverloads
    public external fun executeBundles(bundles: Array<GPURenderBundle> = arrayOf()): Unit

    /**
     * Inserts a debug marker command into the pass.
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
     * Sets the blend constant color for the render pass.
     *
     * @param color The constant color value to use as a blend factor.
     */
    @FastNative
    @JvmName("setBlendConstant")
    public external fun setBlendConstant(color: GPUColor): Unit

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
     * Sets a debug label for the render pass encoder.
     *
     * @param label The label to assign to the render pass encoder.
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
     * Sets the scissor rectangle for the render pass.
     *
     * @param x The x coordinate of the top-left corner of the scissor rectangle.
     * @param y The y coordinate of the top-left corner of the scissor rectangle.
     * @param width The width of the scissor rectangle.
     * @param height The height of the scissor rectangle.
     */
    @FastNative
    @JvmName("setScissorRect")
    public external fun setScissorRect(x: Int, y: Int, width: Int, height: Int): Unit

    /**
     * Sets the stencil reference value for the render pass.
     *
     * @param reference The stencil reference value to use in stencil testing.
     */
    @FastNative
    @JvmName("setStencilReference")
    public external fun setStencilReference(reference: Int): Unit

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

    /**
     * Sets the viewport region for the render pass.
     *
     * @param x The x coordinate of the top-left corner of the viewport.
     * @param y The y coordinate of the top-left corner of the viewport.
     * @param width The width of the viewport.
     * @param height The height of the viewport.
     * @param minDepth The minimum depth of the viewport.
     * @param maxDepth The maximum depth of the viewport.
     */
    @FastNative
    @JvmName("setViewport")
    public external fun setViewport(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        minDepth: Float,
        maxDepth: Float,
    ): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean =
        other is GPURenderPassEncoder && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
