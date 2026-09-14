/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.scenecore.runtime.MeshBufferResource as RtMeshBufferResource
import java.nio.ByteBuffer

/**
 * Container holding raw vertex and index data for custom meshes.
 *
 * A `MeshBuffer` represents a compilation of one or more vertex buffers and an index buffer in the
 * underlying rendering engine. The vertex buffers contain the vertex data according to the provided
 * [VertexLayout], and the index buffer contains the indices of the vertices that form the
 * primitives of the mesh.
 *
 * The raw buffer data is provided during the creation of the `MeshBuffer` and is immediately copied
 * into the rendering engine's memory. As a result, the `MeshBuffer` does not retain references to
 * the original [ByteBuffer] objects, which can be modified or released by the application after
 * creation.
 *
 * @property vertexLayout [VertexLayout] describing the structure of the vertex data
 * @property meshUsage [MeshUsage] indicating the usage mode and mutability of this buffer
 */
public class MeshBuffer
private constructor(
    private val resource: RtMeshBufferResource,
    public val vertexLayout: VertexLayout,
    private val vertexCount: Int,
    private val indexCount: Int,
    private val session: Session,
    @get:JvmName("getMeshUsage") public val meshUsage: MeshUsage = MeshUsage.STATIC,
) : AutoCloseable {

    /**
     * Describes the intended usage and mutability of a [MeshBuffer].
     *
     * Indicates how the buffer was configured at creation time:
     * * [STATIC] indicates the mesh data is immutable and optimized for GPU performance.
     * * [DYNAMIC] indicates the mesh data can be updated at runtime via [updateVertexData] and
     *   [updateIndexData].
     */
    public class MeshUsage private constructor(private val value: Int) {
        public companion object {
            /**
             * Indicates mesh data is set once and never modified.
             *
             * Optimized for maximum GPU rendering performance.
             */
            @JvmField public val STATIC: MeshUsage = MeshUsage(0)

            /**
             * Indicates mesh data is modified occasionally at runtime.
             *
             * Balanced for both CPU writes and GPU reads.
             */
            @JvmField public val DYNAMIC: MeshUsage = MeshUsage(1)
        }
    }

    /**
     * Updates vertex data for a specific buffer index in the [MeshBuffer].
     *
     * This operation is only allowed if [MeshBuffer.meshUsage] is [MeshUsage.DYNAMIC].
     *
     * @param bufferIndex index of the vertex buffer to update in [VertexLayout.buffers]
     * @param vertexData source [ByteBufferRegion] containing the new vertex data
     * @param offsetInBytes starting offset in bytes in the target buffer where the data should be
     *   written
     * @throws IllegalStateException if the buffer is closed, or was not created as dynamic
     * @throws IllegalArgumentException if [bufferIndex] is out of bounds, or if [offsetInBytes]
     *   plus [vertexData] size exceeds the pre-allocated capacity
     */
    @MainThread
    @JvmOverloads
    public fun updateVertexData(
        @IntRange(from = 0) bufferIndex: Int,
        vertexData: ByteBufferRegion,
        @IntRange(from = 0) offsetInBytes: Int = 0,
    ) {
        check(meshUsage == MeshUsage.DYNAMIC) { "MeshBuffer is not dynamic." }
        require(bufferIndex in vertexLayout.buffers.indices) { "bufferIndex out of bounds." }
        require(offsetInBytes >= 0) { "offsetInBytes must be non-negative." }
        require(
            offsetInBytes.toLong() + vertexData.size <=
                vertexCount.toLong() * vertexLayout.buffers[bufferIndex].byteStride
        ) {
            "Update exceeds pre-allocated capacity."
        }
        session.renderingRuntime.updateMeshBufferVertexData(
            resource,
            bufferIndex,
            vertexData.buffer,
            vertexData.offset,
            vertexData.size,
            offsetInBytes,
        )
    }

    /**
     * Updates index data in the [MeshBuffer].
     *
     * This operation is only allowed if [MeshBuffer.meshUsage] is [MeshUsage.DYNAMIC].
     *
     * @param indexData source [ByteBufferRegion] containing the new 32-bit index data
     * @param offsetInBytes starting offset in bytes in the target buffer where the data should be
     *   written
     * @throws IllegalStateException if the buffer is closed, or was not created as dynamic
     * @throws IllegalArgumentException if [offsetInBytes] plus [indexData] size exceeds the
     *   pre-allocated capacity
     */
    @MainThread
    @JvmOverloads
    public fun updateIndexData(
        indexData: ByteBufferRegion,
        @IntRange(from = 0) offsetInBytes: Int = 0,
    ) {
        check(meshUsage == MeshUsage.DYNAMIC) { "MeshBuffer is not dynamic." }
        require(offsetInBytes >= 0) { "offsetInBytes must be non-negative." }
        require(offsetInBytes.toLong() + indexData.size <= indexCount.toLong() * BYTES_PER_INDEX) {
            "Update exceeds pre-allocated index capacity."
        }
        session.renderingRuntime.updateMeshBufferIndexData(
            resource,
            indexData.buffer,
            indexData.offset,
            indexData.size,
            offsetInBytes,
        )
    }

    /**
     * Closes the given [MeshBuffer].
     *
     * The [MeshBuffer] can be explicitly closed at anytime or garbage collected. An exception will
     * be thrown if the [MeshBuffer] is used after being closed.
     *
     * @throws IllegalStateException if the resource has already been closed.
     */
    @MainThread
    override fun close() {
        session.renderingRuntime.destroyMeshBuffer(resource)
    }

    internal fun getResource(): RtMeshBufferResource {
        return resource
    }

    public companion object {
        // Only 32-bit indices are currently supported.
        internal const val BYTES_PER_INDEX = 4

        private fun getRtVertexAttribute(attribute: VertexAttribute): Int =
            when (attribute) {
                VertexAttribute.POSITION -> RtMeshBufferResource.VertexAttribute.POSITION
                VertexAttribute.NORMAL -> RtMeshBufferResource.VertexAttribute.NORMAL
                VertexAttribute.COLOR -> RtMeshBufferResource.VertexAttribute.COLOR
                VertexAttribute.UV0 -> RtMeshBufferResource.VertexAttribute.UV0
                VertexAttribute.UV1 -> RtMeshBufferResource.VertexAttribute.UV1
                VertexAttribute.BONE_INDICES -> RtMeshBufferResource.VertexAttribute.BONE_INDICES
                VertexAttribute.BONE_WEIGHTS -> RtMeshBufferResource.VertexAttribute.BONE_WEIGHTS
                else -> throw IllegalArgumentException("Unknown VertexAttribute")
            }

        private fun getRtVertexAttributeType(type: VertexAttributeType): Int =
            when (type) {
                VertexAttributeType.FLOAT -> RtMeshBufferResource.VertexAttributeType.FLOAT
                VertexAttributeType.FLOAT2 -> RtMeshBufferResource.VertexAttributeType.FLOAT2
                VertexAttributeType.FLOAT3 -> RtMeshBufferResource.VertexAttributeType.FLOAT3
                VertexAttributeType.FLOAT4 -> RtMeshBufferResource.VertexAttributeType.FLOAT4
                VertexAttributeType.UBYTE4_NORM ->
                    RtMeshBufferResource.VertexAttributeType.UBYTE4_NORM
                VertexAttributeType.UBYTE4 -> RtMeshBufferResource.VertexAttributeType.UBYTE4
                else -> throw IllegalArgumentException("Unknown VertexAttributeType")
            }

        /**
         * Creates a new [MeshBuffer].
         *
         * @param session The session to use for creating the MeshBuffer.
         * @param vertexLayout The layout of the vertices in the vertex buffer(s).
         * @param vertexCount The total number of vertices allocated in memory.
         * @param indexCount The total number of indices allocated in memory.
         * @param vertexData The vertex data regions, one for each buffer index used in the layout.
         *   The data is copied and the original data in the [ByteBuffer] can be released or
         *   modified without affecting the [MeshBuffer].
         * @param indexData The index data region. The indices must be 32-bit unsigned values. The
         *   data is copied and the original data in the [ByteBuffer] can be released or modified
         *   without affecting the [MeshBuffer].
         * @return A new [MeshBuffer].
         * @throws IllegalArgumentException if `vertexData` does not contain a buffer for each
         *   buffer index used in the layout, or if any of the `ByteBufferRegion`s are empty.
         */
        @MainThread
        private fun createInternal(
            session: Session,
            vertexLayout: VertexLayout,
            vertexCount: Int,
            indexCount: Int,
            vertexData: List<ByteBufferRegion?>? = null,
            indexData: ByteBufferRegion? = null,
            meshUsage: MeshUsage = MeshUsage.STATIC,
        ): MeshBuffer {
            val runtime = session.renderingRuntime

            val numAttributes = vertexLayout.buffers.sumOf { it.attributes.size }
            val attributeIds = IntArray(numAttributes)
            val attributeTypes = IntArray(numAttributes)
            val bufferIndices = ByteArray(numAttributes)
            val byteOffsets = IntArray(numAttributes)
            val byteStrides = IntArray(vertexLayout.buffers.size)

            var attrIndex = 0
            for (bufIndex in vertexLayout.buffers.indices) {
                val bufferLayout = vertexLayout.buffers[bufIndex]
                byteStrides[bufIndex] = if (bufferLayout.stride < 0) 0 else bufferLayout.stride
                for (attr in bufferLayout.attributes) {
                    attributeIds[attrIndex] = getRtVertexAttribute(attr.attribute)
                    attributeTypes[attrIndex] = getRtVertexAttributeType(attr.type)
                    bufferIndices[attrIndex] = bufIndex.toByte()
                    byteOffsets[attrIndex] = attr.offset
                    attrIndex++
                }
            }

            var vertexBuffers: Array<ByteBuffer>? = null
            var vertexDataOffsets: IntArray? = null
            var vertexDataSizes: IntArray? = null
            if (vertexData != null) {
                val numBuffers = vertexLayout.buffers.size
                vertexDataOffsets = IntArray(numBuffers)
                vertexDataSizes = IntArray(numBuffers)
                vertexBuffers =
                    Array(numBuffers) { i ->
                        val region = vertexData.getOrNull(i)
                        if (region != null) {
                            vertexDataOffsets[i] = region.offset
                            vertexDataSizes[i] = region.size
                            region.buffer
                        } else {
                            ByteBuffer.allocateDirect(0)
                        }
                    }
            }

            val resource =
                runtime.createMeshBuffer(
                    attributeIds,
                    attributeTypes,
                    bufferIndices,
                    byteOffsets,
                    byteStrides,
                    vertexCount,
                    indexCount,
                    vertexBuffers,
                    vertexDataOffsets,
                    vertexDataSizes,
                    indexData?.buffer,
                    indexData?.offset ?: 0,
                    indexData?.size ?: 0,
                )

            return MeshBuffer(resource, vertexLayout, vertexCount, indexCount, session, meshUsage)
        }

        /**
         * Creates a new static [MeshBuffer].
         *
         * The raw buffer data is copied immediately into rendering engine memory. The original
         * [ByteBuffer] objects can be modified or released after creation without affecting the
         * mesh buffer.
         *
         * @param session session to use for creating the MeshBuffer
         * @param vertexLayout layout of the vertices in the vertex buffer(s)
         * @param vertexData vertex data regions, one for each buffer index in [vertexLayout]
         * @param indexData index data region containing 32-bit unsigned indices
         * @return a new static [MeshBuffer]
         * @throws IllegalArgumentException if [vertexData] does not contain a buffer for each
         *   buffer index in [vertexLayout], or if any [ByteBufferRegion] is empty
         */
        @MainThread
        @JvmStatic
        public fun create(
            session: Session,
            vertexLayout: VertexLayout,
            vertexData: List<ByteBufferRegion>,
            indexData: ByteBufferRegion,
        ): MeshBuffer {
            require(vertexData.size == vertexLayout.buffers.size) {
                "vertexData size must match the number of buffers in VertexLayout."
            }

            for (i in vertexData.indices) {
                require(vertexData[i].size > 0) { "vertexData[$i] must be non-empty." }
            }
            require(indexData.size > 0) { "indexData must be non-empty." }

            return createInternal(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 0,
                indexCount = 0,
                vertexData = vertexData,
                indexData = indexData,
                meshUsage = MeshUsage.STATIC,
            )
        }

        /**
         * Creates a new dynamic [MeshBuffer] supporting runtime updates.
         *
         * The [vertexCount] and [indexCount] parameters define the fixed maximum capacity of the
         * underlying GPU buffers. The allocated buffer cannot be resized after creation. Any
         * runtime updates via [updateVertexData] or [updateIndexData] that exceed these capacities
         * will throw an [IllegalArgumentException].
         *
         * If initial vertex or index data is omitted or partially provided (smaller than the
         * allocated capacities), the unpopulated regions will be filled with zeros.
         *
         * **Note:** Automatic bounding box calculation only considers the initial vertex data
         * supplied at creation time and is never recalculated on subsequent dynamic updates. When
         * working with dynamic meshes, set a bounding box on the mesh that covers all possible
         * vertex positions across the mesh's lifetime.
         *
         * @param session session to use for creating the MeshBuffer
         * @param vertexLayout layout of the vertices in the vertex buffer(s)
         * @param vertexCount fixed maximum capacity of vertices allocated in memory
         * @param indexCount fixed maximum capacity of indices allocated in memory
         * @param vertexData optional list of [ByteBufferRegion] containing initial vertex data, one
         *   for each buffer index in [vertexLayout]
         * @param indexData optional [ByteBufferRegion] containing initial 32-bit index data
         * @return a new [MeshBuffer]
         * @throws IllegalArgumentException if [vertexCount] or [indexCount] is non-positive, or if
         *   [vertexData] is provided and its size does not match the number of buffers in
         *   [vertexLayout]
         */
        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
        @MainThread
        @JvmStatic
        @JvmOverloads
        public fun createDynamic(
            session: Session,
            vertexLayout: VertexLayout,
            @IntRange(from = 1) vertexCount: Int,
            @IntRange(from = 1) indexCount: Int,
            vertexData: List<ByteBufferRegion?>? = null,
            indexData: ByteBufferRegion? = null,
        ): MeshBuffer {
            require(vertexCount > 0) { "vertexCount must be positive" }
            require(indexCount > 0) { "indexCount must be positive" }
            if (vertexData != null) {
                require(vertexData.size == vertexLayout.buffers.size) {
                    "vertexData size must match the number of buffers in VertexLayout."
                }
            }

            return createInternal(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = vertexCount,
                indexCount = indexCount,
                vertexData = vertexData,
                indexData = indexData,
                meshUsage = MeshUsage.DYNAMIC,
            )
        }
    }
}
