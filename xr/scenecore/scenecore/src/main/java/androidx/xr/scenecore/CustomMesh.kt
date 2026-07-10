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

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.CustomMeshResource as RtCustomMeshResource
import java.nio.ByteBuffer

/**
 * An immutable resource that defines the structure of a renderable mesh.
 *
 * A `CustomMesh` is composed of a [MeshBuffer] and a list of [MeshSubsets][MeshSubset]. Each
 * `MeshSubset` defines a part of the mesh that can be rendered with a single [Material].
 *
 * @property meshBuffer The [MeshBuffer] containing the vertex and index data for this mesh.
 * @property subsets The list of [MeshSubsets][MeshSubset] defining the parts of the mesh.
 * @property bounds The bounding box of the mesh, used for culling.
 */
public class CustomMesh
private constructor(
    private val resource: RtCustomMeshResource,
    public val meshBuffer: MeshBuffer,
    public val subsets: List<MeshSubset>,
    public val bounds: BoundingBox,
    private val session: Session,
) : AutoCloseable {

    /**
     * Closes the given [CustomMesh].
     *
     * The [CustomMesh] can be explicitly closed at anytime or garbage collected. An exception will
     * be thrown if the [CustomMesh] is used after being closed.
     *
     * @throws IllegalStateException if the resource has already been closed.
     */
    @MainThread
    override fun close() {
        session.renderingRuntime.destroyCustomMesh(resource)
    }

    internal fun getResource(): RtCustomMeshResource {
        return resource
    }

    public companion object {
        // Only 32-bit indices are currently supported.
        private const val BYTES_PER_INDEX = 4

        private fun getRtMeshSubsetTopology(topology: MeshSubsetTopology): Int =
            when (topology) {
                MeshSubsetTopology.TRIANGLES -> RtCustomMeshResource.Topology.TRIANGLES
                MeshSubsetTopology.TRIANGLE_STRIP -> RtCustomMeshResource.Topology.TRIANGLE_STRIP
                else -> throw IllegalArgumentException("Unknown MeshSubsetTopology")
            }

        @MainThread
        private fun internalCreate(
            session: Session,
            meshBuffer: MeshBuffer,
            subsets: List<MeshSubset>,
            boundingBox: BoundingBox? = null,
        ): CustomMesh {
            val runtime = session.renderingRuntime

            val subsetOffsets = IntArray(subsets.size)
            val subsetCounts = IntArray(subsets.size)
            val subsetTopologies = IntArray(subsets.size)

            for (i in subsets.indices) {
                subsetOffsets[i] = subsets[i].indexOffset
                subsetCounts[i] = subsets[i].indexCount
                subsetTopologies[i] = getRtMeshSubsetTopology(subsets[i].topology)
            }

            val centerX = boundingBox?.center?.x ?: 0f
            val centerY = boundingBox?.center?.y ?: 0f
            val centerZ = boundingBox?.center?.z ?: 0f
            // If no bounding box is provided, set the half extents to -1 to indicate that
            // the mesh should use the auto-computed bounding box of the mesh buffer.
            val halfExtentX = boundingBox?.halfExtents?.width ?: -1f
            val halfExtentY = boundingBox?.halfExtents?.height ?: -1f
            val halfExtentZ = boundingBox?.halfExtents?.depth ?: -1f

            val resource =
                runtime.createCustomMesh(
                    meshBuffer.getResource(),
                    subsetOffsets,
                    subsetCounts,
                    subsetTopologies,
                    centerX,
                    centerY,
                    centerZ,
                    halfExtentX,
                    halfExtentY,
                    halfExtentZ,
                )

            val finalBounds = runtime.getCustomMeshBoundingBox(resource)
            return CustomMesh(resource, meshBuffer, subsets.toList(), finalBounds, session)
        }
    }

    /**
     * Builder for [CustomMesh] using an existing [MeshBuffer].
     *
     * This is useful if you are sharing a single buffer across multiple meshes.
     * <pre><code class="lang-kotlin">
     * val mesh = CustomMesh.BuilderFromMeshBuffer(session, myMeshBuffer)
     *     .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, indexCount))
     *     .build()
     * </code></pre>
     */
    public class BuilderFromMeshBuffer(
        private val session: Session,
        private val meshBuffer: MeshBuffer,
    ) {
        private val subsets = mutableListOf<MeshSubset>()
        private var boundingBox: BoundingBox? = null

        /** Adds a [MeshSubset] defining a part of the mesh. */
        public fun addSubset(subset: MeshSubset): BuilderFromMeshBuffer = apply {
            this.subsets.add(subset)
        }

        /**
         * Adds a [MeshSubset] defining a part of the mesh using the specified topology, index
         * offset, and index count.
         *
         * @throws IllegalArgumentException if [indexOffset] or [indexCount] is negative
         */
        public fun addSubset(
            topology: MeshSubsetTopology,
            @IntRange(from = 0) indexOffset: Int,
            @IntRange(from = 0) indexCount: Int,
        ): BuilderFromMeshBuffer = apply {
            this.subsets.add(MeshSubset(topology, indexOffset, indexCount))
        }

        /**
         * Sets an optional user-supplied bounding box for culling.
         *
         * If not provided, the auto-computed bounding box of the entire [MeshBuffer] will be used.
         */
        public fun setBounds(bounds: BoundingBox): BuilderFromMeshBuffer = apply {
            this.boundingBox = bounds
        }

        /**
         * Builds a new [CustomMesh].
         *
         * @throws IllegalStateException if no subsets have been added.
         */
        @MainThread
        public fun build(): CustomMesh {
            check(subsets.isNotEmpty()) { "CustomMesh requires at least one subset." }
            return internalCreate(session, meshBuffer, subsets.toList(), boundingBox)
        }
    }

    /**
     * Builder for [CustomMesh] providing raw data directly.
     *
     * This will implicitly create a `MeshBuffer` for you. You provide the [VertexLayout] along with
     * the raw vertex and index data:
     * <pre><code class="lang-kotlin">
     * val builder = CustomMesh.BuilderFromMeshData(session, myLayout)
     *     .addVertexData(myVertexData)
     *     .setIndexData(myIndexData)
     * </code></pre>
     *
     * From here, you have two options for defining the mesh topology:
     * - You can explicitly add one or more subsets:
     *
     *   <pre><code class="lang-kotlin">
     *   builder.addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, subset1Count))
     *   builder.addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, subset1Count, subset2Count))
     *   </code></pre>
     * - Or, if the entire mesh uses the same topology, you can define a single subset that spans
     *   all the provided index data:
     *
     *   <pre><code class="lang-kotlin">
     *   builder.setTopology(MeshSubsetTopology.TRIANGLES)
     *   </code></pre>
     *
     * Finally, build the mesh:
     * <pre><code class="lang-kotlin">
     * val mesh = builder.build()
     * </code></pre>
     */
    public class BuilderFromMeshData(
        private val session: Session,
        private val vertexLayout: VertexLayout,
    ) {
        private val vertexDataList = mutableListOf<ByteBufferRegion>()
        private var indexData: ByteBufferRegion? = null

        private val subsets = mutableListOf<MeshSubset>()
        private var topology: MeshSubsetTopology? = null

        private var boundingBox: BoundingBox? = null

        /**
         * Adds vertex data for a single buffer.
         *
         * The order in which this method is called determines the buffer index. The first call
         * provides data for buffer index 0, the second for buffer index 1, etc. The data is copied
         * during [build], so the original [ByteBuffer] can be modified or released after [build]
         * without affecting the underlying [MeshBuffer].
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun addVertexData(vertexData: ByteBufferRegion): BuilderFromMeshData = apply {
            this.vertexDataList.add(vertexData)
        }

        /**
         * Adds vertex data for a single buffer using the provided [ByteBuffer], `offset`, and
         * `size`.
         *
         * The order in which this method is called determines the buffer index. The first call
         * provides data for buffer index 0, the second for buffer index 1, etc. The data is copied
         * during [build], so the original [ByteBuffer] can be modified or released after [build]
         * without affecting the underlying [MeshBuffer].
         *
         * @param buffer containing the data
         * @param offset absolute starting position within the buffer (ignoring its current
         *   position) in bytes (defaults to 0)
         * @param size number of bytes in the region (defaults to the remaining capacity of the
         *   buffer after the given offset)
         * @throws IllegalArgumentException if `offset` is negative, if `size` is zero or negative,
         *   if `offset` exceeds `buffer.capacity()`, or if `offset + size` exceeds
         *   `buffer.capacity()`.
         */
        @JvmOverloads
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun addVertexData(
            buffer: ByteBuffer,
            @IntRange(from = 0) offset: Int = 0,
            @IntRange(from = 1) size: Int = buffer.capacity() - offset,
        ): BuilderFromMeshData = apply {
            require(offset >= 0) { "offset must not be negative." }
            require(offset <= buffer.capacity()) { "offset must not exceed buffer capacity." }
            require(size > 0) { "size must be greater than zero." }
            this.vertexDataList.add(ByteBufferRegion(buffer, offset, size))
        }

        /**
         * Sets the index data.
         *
         * The data is copied during [build], so the original [ByteBuffer] can be modified or
         * released after [build] without affecting the underlying [MeshBuffer].
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setIndexData(indexData: ByteBufferRegion): BuilderFromMeshData = apply {
            this.indexData = indexData
        }

        /**
         * Sets the index data using the provided [ByteBuffer], `offset`, and `size`.
         *
         * The data is copied during [build], so the original [ByteBuffer] can be modified or
         * released after [build] without affecting the underlying [MeshBuffer].
         *
         * @param buffer containing the data
         * @param offset absolute starting position within the buffer (ignoring its current
         *   position) in bytes (defaults to 0)
         * @param size number of bytes in the region (defaults to the remaining capacity of the
         *   buffer after the given offset)
         * @throws IllegalArgumentException if `offset` is negative, if `size` is zero or negative,
         *   if `offset` exceeds `buffer.capacity()`, or if `offset + size` exceeds
         *   `buffer.capacity()`.
         */
        @JvmOverloads
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setIndexData(
            buffer: ByteBuffer,
            @IntRange(from = 0) offset: Int = 0,
            @IntRange(from = 1) size: Int = buffer.capacity() - offset,
        ): BuilderFromMeshData = apply {
            require(offset >= 0) { "offset must not be negative." }
            require(offset <= buffer.capacity()) { "offset must not exceed buffer capacity." }
            require(size > 0) { "size must be greater than zero." }
            this.indexData = ByteBufferRegion(buffer, offset, size)
        }

        /**
         * Adds a [MeshSubset] defining a part of the mesh.
         *
         * This cannot be used in combination with [setTopology].
         *
         * @throws IllegalStateException if a topology has already been set
         */
        public fun addSubset(subset: MeshSubset): BuilderFromMeshData = apply {
            check(topology == null) { "Cannot add subset after setting a single topology." }
            this.subsets.add(subset)
        }

        /**
         * Adds a [MeshSubset] defining a part of the mesh using the specified topology, index
         * offset, and index count.
         *
         * This cannot be used in combination with [setTopology].
         *
         * @throws IllegalStateException if a topology has already been set
         * @throws IllegalArgumentException if [indexOffset] or [indexCount] is negative
         */
        public fun addSubset(
            topology: MeshSubsetTopology,
            @IntRange(from = 0) indexOffset: Int,
            @IntRange(from = 0) indexCount: Int,
        ): BuilderFromMeshData = apply {
            check(this.topology == null) { "Cannot add subset after setting a single topology." }
            this.subsets.add(MeshSubset(topology, indexOffset, indexCount))
        }

        /**
         * Sets the [MeshSubsetTopology] to use for the entire mesh, defining a single subset that
         * spans all provided index data.
         *
         * This cannot be used in combination with [addSubset].
         *
         * @throws IllegalStateException if subsets have already been added
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setTopology(topology: MeshSubsetTopology): BuilderFromMeshData = apply {
            check(subsets.isEmpty()) { "Cannot set topology after adding subsets." }
            this.topology = topology
        }

        /**
         * Sets an optional user-supplied bounding box for culling.
         *
         * If not provided, the auto-computed bounding box of the entire [MeshBuffer] will be used.
         */
        public fun setBounds(bounds: BoundingBox): BuilderFromMeshData = apply {
            this.boundingBox = bounds
        }

        /**
         * Builds a new [CustomMesh].
         *
         * @throws IllegalStateException if index data or vertex data are missing, or if both or
         *   neither of subsets and topology are provided.
         */
        @MainThread
        public fun build(): CustomMesh {
            val indices = checkNotNull(indexData) { "Index data must be provided." }
            check(vertexDataList.isNotEmpty()) {
                "At least one vertex buffer data region must be provided."
            }

            val meshBuffer = MeshBuffer.create(session, vertexLayout, vertexDataList, indices)

            val hasSubsets = subsets.isNotEmpty()
            val hasTopology = topology != null

            check(hasSubsets != hasTopology) {
                "CustomMesh requires either subsets or a single topology, but not both."
            }

            val finalSubsets =
                if (hasSubsets) {
                    subsets.toList()
                } else {
                    val indexCount = indices.size / BYTES_PER_INDEX
                    val singleTopology = checkNotNull(topology) { "Topology must be provided." }
                    listOf(MeshSubset(singleTopology, 0, indexCount))
                }

            return internalCreate(session, meshBuffer, finalSubsets, boundingBox)
        }
    }
}
