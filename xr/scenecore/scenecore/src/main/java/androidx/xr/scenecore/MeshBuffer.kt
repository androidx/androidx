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
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.MeshBufferResource as RtMeshBufferResource
import java.nio.ByteBuffer

/**
 * A container holding raw vertex and index data.
 *
 * A `MeshBuffer` contains one or more vertex buffers and an index buffer. The vertex buffers
 * contain the vertex data according to the provided [VertexLayout]. The index buffer contains the
 * indices of the vertices that form the primitives of the mesh.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
         */
        @MainThread
        public fun create(
            session: Session,
            vertexLayout: VertexLayout,
            vertexData: Array<ByteBufferRegion>,
            indexData: ByteBufferRegion,
        ): MeshBuffer {
            val runtime = session.renderingRuntime

            val attributeIds = IntArray(vertexLayout.attributes.size)
            val attributeTypes = IntArray(vertexLayout.attributes.size)
            val bufferIndices = ByteArray(vertexLayout.attributes.size)

            for (i in vertexLayout.attributes.indices) {
                val attr = vertexLayout.attributes[i]
                attributeIds[i] = getRtVertexAttribute(attr.attribute)
                attributeTypes[i] = getRtVertexAttributeType(attr.type)
                bufferIndices[i] = attr.bufferIndex.toByte()
            }

            val vertexBuffers = Array<ByteBuffer>(vertexData.size) { vertexData[it].buffer }
            val vertexDataOffsets = IntArray(vertexData.size) { vertexData[it].offset }
            val vertexDataSizes = IntArray(vertexData.size) { vertexData[it].size }

            val resource =
                runtime.createMeshBuffer(
                    attributeIds,
                    attributeTypes,
                    bufferIndices,
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
