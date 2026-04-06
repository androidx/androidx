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
import androidx.annotation.MainThread
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.CustomMeshResource as RtCustomMeshResource

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
@ExperimentalCustomMeshApi
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
     * Builder for [CustomMesh].
     *
     * There are two general ways to build a `CustomMesh`:
     * 1. **Using an existing [MeshBuffer]:** This is useful if you are sharing a single buffer
     *    across multiple meshes. You provide the `MeshBuffer` and a list of `MeshSubset`s.
     *
     *    <pre><code class="lang-kotlin">
     *    val mesh = CustomMesh.Builder(session)
     *        .setMeshBuffer(myMeshBuffer)
     *        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, indexCount))
     *        .build()
     *    </code></pre>
     * 2. **Providing raw data directly:** This will implicitly create a `MeshBuffer` for you. You
     *    provide the [VertexLayout] along with the raw vertex and index data. When using this
     *    approach, you have two options for defining the mesh topology:
     *     - You can explicitly add one or more subsets:
     *
     *      <pre><code class="lang-kotlin">
     *      val mesh = CustomMesh.Builder(session)
     *          .setVertexLayout(myLayout)
     *          .addVertexBufferData(myVertexData)
     *          .setIndexData(myIndexData)
     *          .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, subset1Count))
     *          .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, subset1Count, subset2Count))
     *          .build()
     *      </code></pre>
     *     - Or, if the entire mesh uses the same topology, you can define a single subset that
     *       spans all the provided index data:
     *      <pre><code class="lang-kotlin">
     *      val mesh = CustomMesh.Builder(session)
     *          .setVertexLayout(myLayout)
     *          .addVertexBufferData(myVertexData)
     *          .setIndexData(myIndexData)
     *          .setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
     *          .build()
     *      </code></pre>
     *
     * These approaches are mutually exclusive. Providing raw data alongside an existing
     * `MeshBuffer` will result in an [IllegalStateException].
     */
    public class Builder(private val session: Session) {
        private var meshBuffer: MeshBuffer? = null

        private var vertexLayout: VertexLayout? = null
        private val vertexDataList = mutableListOf<ByteBufferRegion>()
        private var indexData: ByteBufferRegion? = null

        private val subsets = mutableListOf<MeshSubset>()
        private var topology: MeshSubsetTopology? = null

        private var boundingBox: BoundingBox? = null

        /**
         * Sets the [MeshBuffer] containing the vertex and index data.
         *
         * This cannot be used in combination with [setVertexLayout], [addVertexBufferData],
         * [setIndexData], or [setSingleSubsetTopology].
         *
         * @throws IllegalStateException if vertex layout, vertex data, index data, or topology have
         *   already been set
         */
        public fun setMeshBuffer(meshBuffer: MeshBuffer): Builder = apply {
            check(
                vertexLayout == null &&
                    vertexDataList.isEmpty() &&
                    indexData == null &&
                    topology == null
            ) {
                "Cannot set MeshBuffer after setting vertex, index data, or topology."
            }
            this.meshBuffer = meshBuffer
        }

        /**
         * Sets the layout of the vertices in the vertex buffer(s).
         *
         * This cannot be used in combination with [setMeshBuffer].
         *
         * @throws IllegalStateException if a [MeshBuffer] has already been set
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setVertexLayout(vertexLayout: VertexLayout): Builder = apply {
            check(meshBuffer == null) { "Cannot set vertex layout after setting MeshBuffer." }
            this.vertexLayout = vertexLayout
        }

        /**
         * Adds vertex data for a single buffer.
         *
         * The order in which this method is called determines the buffer index. The first call
         * provides data for buffer index 0, the second for buffer index 1, etc. The data is copied,
         * so the original [java.nio.ByteBuffer] can be modified or released without affecting the
         * underlying [MeshBuffer].
         *
         * This cannot be used in combination with [setMeshBuffer].
         *
         * @throws IllegalStateException if a [MeshBuffer] has already been set
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun addVertexBufferData(vertexData: ByteBufferRegion): Builder = apply {
            check(meshBuffer == null) { "Cannot set vertex data after setting MeshBuffer." }
            this.vertexDataList.add(vertexData)
        }

        /**
         * Sets the index data.
         *
         * The data is copied, so the original [java.nio.ByteBuffer] can be modified or released
         * without affecting the underlying [MeshBuffer].
         *
         * This cannot be used in combination with [setMeshBuffer].
         *
         * @throws IllegalStateException if a [MeshBuffer] has already been set
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setIndexData(indexData: ByteBufferRegion): Builder = apply {
            check(meshBuffer == null) { "Cannot set index data after setting MeshBuffer." }
            this.indexData = indexData
        }

        /**
         * Adds a [MeshSubset] defining a part of the mesh.
         *
         * This cannot be used in combination with [setSingleSubsetTopology].
         *
         * @throws IllegalStateException if a topology has already been set
         */
        public fun addSubset(subset: MeshSubset): Builder = apply {
            check(topology == null) { "Cannot add subset after setting a single topology." }
            this.subsets.add(subset)
        }

        /**
         * Sets the [MeshSubsetTopology] to use for the entire mesh, defining a single subset that
         * spans all provided index data.
         *
         * This cannot be used in combination with [setMeshBuffer] or [addSubset].
         *
         * @throws IllegalStateException if a [MeshBuffer] has been set, or if subsets have already
         *   been added
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setSingleSubsetTopology(topology: MeshSubsetTopology): Builder = apply {
            check(meshBuffer == null) { "Cannot set topology after setting MeshBuffer." }
            check(subsets.isEmpty()) { "Cannot set topology after adding subsets." }
            this.topology = topology
        }

        /**
         * Sets an optional user-supplied bounding box for culling.
         *
         * If not provided, the auto-computed bounding box of the entire [MeshBuffer] will be used.
         */
        public fun setBounds(bounds: BoundingBox): Builder = apply { this.boundingBox = bounds }

        /**
         * Builds a new [CustomMesh].
         *
         * @throws IllegalStateException if both or neither of `MeshBuffer` and vertex/index data
         *   are provided, or if both or neither of subsets and topology are provided.
         */
        @MainThread
        public fun build(): CustomMesh {
            val hasMeshBuffer = meshBuffer != null
            val hasVertexLayout = vertexLayout != null
            val hasIndexData = indexData != null
            val hasVertexData = vertexDataList.isNotEmpty()

            check(hasMeshBuffer != (hasVertexLayout || hasIndexData || hasVertexData)) {
                "CustomMesh requires exactly one of a MeshBuffer or vertex/index data."
            }

            val finalMeshBuffer =
                if (hasMeshBuffer) {
                    meshBuffer!!
                } else {
                    val layout = checkNotNull(vertexLayout) { "VertexLayout must be provided." }
                    val indices = checkNotNull(indexData) { "Index data must be provided." }
                    check(hasVertexData) {
                        "At least one vertex buffer data region must be provided."
                    }
                    MeshBuffer.create(session, layout, vertexDataList, indices)
                }

            val hasSubsets = subsets.isNotEmpty()
            val hasTopology = topology != null

            check(hasSubsets != hasTopology) {
                "CustomMesh requires either subsets or a single topology, but not both."
            }

            val finalSubsets =
                if (hasSubsets) {
                    subsets.toList()
                } else {
                    val indices =
                        checkNotNull(indexData) {
                            "Using setSingleSubsetTopology requires setting index data directly. If using an existing MeshBuffer, use addSubset instead."
                        }
                    val indexCount = indices.size / BYTES_PER_INDEX
                    val singleTopology = checkNotNull(topology) { "Topology must be provided." }
                    listOf(MeshSubset(singleTopology, 0, indexCount))
                }

            return internalCreate(session, finalMeshBuffer, finalSubsets, boundingBox)
        }
    }
}
