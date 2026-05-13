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

import androidx.annotation.MainThread
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.MeshBufferResource as RtMeshBufferResource
import java.nio.ByteBuffer

/**
 * A container holding raw vertex and index data for custom meshes.
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
 * @property vertexLayout The [VertexLayout] describing the structure of the vertex data.
 */
@ExperimentalCustomMeshApi
public class MeshBuffer
private constructor(
    private val resource: RtMeshBufferResource,
    public val vertexLayout: VertexLayout,
    private val session: Session,
) : AutoCloseable {

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
         * @param vertexData The vertex data regions, one for each buffer index used in the layout.
         *   The data is copied and the original data in the ByteBuffer can be released or modified
         *   without affecting the [MeshBuffer].
         * @param indexData The index data region. The data is copied and the original data in the
         *   ByteBuffer can be released or modified without affecting the [MeshBuffer].
         * @return A new [MeshBuffer].
         * @throws IllegalArgumentException if `vertexData` does not contain a buffer for each
         *   buffer index used in the layout, or if any of the `ByteBufferRegion`s are empty.
         */
        @MainThread
        @JvmStatic
        public fun create(
            session: Session,
            vertexLayout: VertexLayout,
            vertexData: List<ByteBufferRegion>,
            indexData: ByteBufferRegion,
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

            require(vertexData.size == vertexLayout.buffers.size) {
                "vertexData size must match the number of buffers in VertexLayout."
            }

            for (i in vertexData.indices) {
                require(vertexData[i].size > 0) { "vertexData[$i] must be non-empty." }
            }
            require(indexData.size > 0) { "indexData must be non-empty." }

            val vertexBuffers = Array<ByteBuffer>(vertexData.size) { vertexData[it].buffer }
            val vertexDataOffsets = IntArray(vertexData.size) { vertexData[it].offset }
            val vertexDataSizes = IntArray(vertexData.size) { vertexData[it].size }

            val resource =
                runtime.createMeshBuffer(
                    attributeIds,
                    attributeTypes,
                    bufferIndices,
                    byteOffsets,
                    byteStrides,
                    0,
                    0,
                    vertexBuffers,
                    vertexDataOffsets,
                    vertexDataSizes,
                    indexData.buffer,
                    indexData.offset,
                    indexData.size,
                )

            return MeshBuffer(resource, vertexLayout, session)
        }
    }
}
