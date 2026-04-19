/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ink.geometry

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.NativePointer

/**
 * A read-only mesh made up of vertices and triangles. Vertices have a position, and optionally
 * additional non-geometric attributes. The vertices may be packed internally to store attributes
 * more efficiently, but data accessed through this class, unless otherwise noted, is in its
 * unpacked form for ease of use. This unpacked form corresponds to whatever coordinate space was
 * used to construct this object, such as stroke coordinates for a Stroke object.
 *
 * This is not meant to be constructed directly by developers. The primary constructor is to have a
 * new instance of this class manage a native `ink::Mesh` instance created by another Strokes API
 * utility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class Mesh private constructor(pointerAlloc: () -> Long) {

    /**
     * This provides the raw pointer address of an `ink::Mesh` that has been heap allocated to be
     * owned solely by this JVM [Mesh] object. The C++ `Mesh` object is cheap to copy because
     * internally it keeps a `shared_ptr` to its (immutable) data.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long by NativePointer(pointerAlloc, MeshNative::free)

    public val format: MeshFormat =
        MeshFormat.wrapNative { MeshNative.newCopyOfFormat(nativePointer) }

    /** The number of vertices in the mesh. */
    public val vertexCount: Int = MeshNative.getVertexCount(nativePointer)

    /** The number of bytes per vertex in the mesh. */
    public val vertexStride: Int = MeshNative.getVertexStride(nativePointer)

    /**
     * The number of triangles represented in the mesh. The number of triangle indices is therefore
     * 3 * [triangleCount].
     */
    public val triangleCount: Int = MeshNative.getTriangleCount(nativePointer)

    /** The bounding box of the vertex positions. */
    public val bounds: Box? by lazy { MeshNative.createBounds(nativePointer) }

    /** The transforms used to convert packed attributes into their actual values. */
    public val vertexAttributeUnpackingParams: List<MeshAttributeUnpackingParams> = run {
        val attributeCount = MeshNative.getAttributeCount(nativePointer)
        (0 until attributeCount).map {
            val offsets = FloatArray(MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS)
            val scales = FloatArray(MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS)
            val componentCount =
                MeshNative.fillAttributeUnpackingParams(nativePointer, it, offsets, scales)
            MeshAttributeUnpackingParams.create(
                offsets.sliceArray((0 until componentCount)),
                scales.sliceArray((0 until componentCount)),
            )
        }
    }

    /**
     * Only for tests - creates a new empty [Mesh]. Since a [Mesh] is immutable, this serves no
     * practical purpose outside of tests.
     */
    @VisibleForTesting internal constructor() : this(MeshNative::createEmpty)

    /**
     * Retrieve the vertex position from index [vertexIndex] (which can be up to, but not including,
     * [vertexCount]). The resulting x/y position of that vertex will be put into [outPosition],
     * which can be pre-allocated and reused to avoid allocations where appropriate.
     */
    public fun fillPosition(@IntRange(from = 0) vertexIndex: Int, outPosition: MutableVec) {
        require(vertexIndex >= 0 && vertexIndex < vertexCount) {
            "vertexIndex=$vertexIndex must be between 0 and vertexCount=$vertexCount."
        }
        MeshNative.fillPosition(nativePointer, vertexIndex, outPosition)
    }

    override fun toString(): String {
        return "Mesh(bounds=$bounds, vertexCount=$vertexCount, nativePointer=$nativePointer)"
    }

    /** Declared primarily as a target for extension functions. */
    public companion object {
        // The maximum number of components in [MeshAttributeUnpackingParams].
        internal const val MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS = 4

        /**
         * Construct a [Mesh], taking a callback that heap-allocates and returns a pointer to a C++
         * `Mesh`.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(pointerAlloc: () -> Long): Mesh = Mesh(pointerAlloc)

        /**
         * Construct a [Mesh] by copying the heap-allocated `Mesh` at the given native pointer.
         *
         * @param otherNativePointer The native pointer to a C++ `Mesh` to copy.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun copyFromNative(otherNativePointer: Long): Mesh = Mesh {
            MeshNative.newCopy(otherNativePointer)
        }
    }
}

expect internal object MeshNative {
    @VisibleForTesting fun createEmpty(): Long

    fun newCopy(otherNativePointer: Long): Long

    fun free(nativePointer: Long)

    fun getVertexCount(nativePointer: Long): Int

    fun getVertexStride(nativePointer: Long): Int

    fun getTriangleCount(nativePointer: Long): Int

    fun getAttributeCount(nativePointer: Long): Int

    /**
     * Returns a new [ImmutableBox] with the bounding box of the mesh at [nativePointer] if
     * non-empty, or null if the mesh is empty.
     */
    fun createBounds(nativePointer: Long): ImmutableBox?

    /**
     * Set the given [offsets] and [scales] arrays (each of which must have at least
     * [Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS] elements) to the unpacking transform offsets
     * and scales for the attribute with the given [attributeIndex].
     *
     * @return The number of elements of [offsets] and [scales] that have been filled. This is the
     *   number of [MeshAttributeUnpackingParams.ComponentUnpackingParams] in the
     *   [MeshAttributeUnpackingParams] that should be created for this attribute.
     */
    fun fillAttributeUnpackingParams(
        nativePointer: Long,
        attributeIndex: Int,
        offsets: FloatArray,
        scales: FloatArray,
    ): Int

    /**
     * Return the address of a newly allocated copy of the `ink::MeshFormat` belonging to this mesh.
     */
    fun newCopyOfFormat(nativePointer: Long): Long

    fun fillPosition(nativePointer: Long, vertexIndex: Int, outPosition: MutableVec)
}
