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

import androidx.annotation.RestrictTo

/**
 * Defines the topology of the indices in a [MeshSubset].
 *
 * This specifies how the index buffer maps vertices to geometric primitives.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshSubsetTopology private constructor(private val name: String) {
    public companion object {
        /** Every three indices form a separate triangle. */
        @JvmField public val TRIANGLES: MeshSubsetTopology = MeshSubsetTopology("TRIANGLES")

        /** Every index after the first two forms a triangle with the previous two indices. */
        @JvmField
        public val TRIANGLE_STRIP: MeshSubsetTopology = MeshSubsetTopology("TRIANGLE_STRIP")
    }

    public override fun toString(): String = name
}

/**
 * Defines a subset of the mesh to draw.
 *
 * A subset defines a contiguous range of indices within the [MeshBuffer]'s index buffer that should
 * be rendered together using a specific topology and a single [Material].
 *
 * @param topology The [MeshSubsetTopology] of the primitives to draw.
 * @param indexOffset The offset (in number of indices, not bytes) to the first index in the index
 *   buffer.
 * @param indexCount The number of indices to draw.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshSubset(
    public val topology: MeshSubsetTopology,
    public val indexOffset: Int,
    public val indexCount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshSubset) return false
        if (topology != other.topology) return false
        if (indexOffset != other.indexOffset) return false
        if (indexCount != other.indexCount) return false
        return true
    }

    override fun hashCode(): Int {
        var result = topology.hashCode()
        result = 31 * result + indexOffset
        result = 31 * result + indexCount
        return result
    }

    override fun toString(): String {
        return "MeshSubset(topology=$topology, indexOffset=$indexOffset, indexCount=$indexCount)"
    }
}
