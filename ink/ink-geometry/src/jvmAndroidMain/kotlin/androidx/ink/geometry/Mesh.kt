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
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.Collections

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
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class Mesh
private constructor(
    /**
     * This is the raw pointer address of an `ink::Mesh` that has been heap allocated to be owned
     * solely by this JVM [Mesh] object. The C++ `Mesh` object is cheap to copy because internally
     * it keeps a `shared_ptr` to its (immutable) data. This class is responsible for freeing the
     * `Mesh` through its [finalize] method.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long
) {

    public val format: MeshFormat = MeshFormat.wrapNative(MeshNative.newCopyOfFormat(nativePointer))

    /**
     * Read-only access to the raw data of the vertices of the [Mesh]. Every [vertexStride] bytes in
     * this data represents another vertex. This is a direct buffer, so it is a reference to native
     * data rather than JVM-managed data, in order to avoid copying for performance reasons. The
     * data is exposed this way (direct buffer, packed) primarily for efficient rendering - most
     * non-rendering data access should go through other methods on this class, which more cleanly
     * hide details of the packed format.
     *
     * DO NOT hold a reference to this object independently of this [Mesh] object - if the [Mesh]
     * becomes unused and garbage collected, then the native data referred to by this buffer may
     * become invalid.
     */
    public val rawVertexData: ByteBuffer =
        (MeshNative.createRawVertexBuffer(nativePointer) ?: ByteBuffer.allocate(0))
            .asReadOnlyBuffer()

    /** The number of bytes used to represent a vertex in the [rawVertexData]. */
    public val vertexStride: Int = MeshNative.getVertexStride(nativePointer)

    /** The number of vertices in the mesh. */
    public val vertexCount: Int = MeshNative.getVertexCount(nativePointer)

    /**
     * Read-only access to the raw data of the triangle indices of the [Mesh]. Every element in this
     * buffer represents another triangle index, with 3 triangle indices making up each triangle.
     * This is a direct buffer, so it is a reference to native data rather than JVM-managed data, in
     * order to avoid copying for performance reasons. The data is exposed as a direct buffer
     * primarily for efficient rendering - most non-rendering data access should go through other
     * methods on this class.
     *
     * The data type of each triangle index is **unsigned**, either a 16-bit [UShort] or a 32-bit
     * [UInt]. Check [triangleIndexStride] to determine which.
     *
     * DO NOT hold a reference to this object independently of this [Mesh] object - if the [Mesh]
     * becomes unused and garbage collected, then the native data referred to by this buffer may
     * become invalid.
     */
    public val rawTriangleIndexData: ShortBuffer =
        (MeshNative.createRawTriangleIndexBuffer(nativePointer) ?: ByteBuffer.allocate(0))
            .asReadOnlyBuffer()
            .asShortBuffer()

    /**
     * The number of triangles represented in [rawTriangleIndexData]. The number of triangle indices
     * is therefore 3 * [triangleCount].
     */
    public val triangleCount: Int = MeshNative.getTriangleCount(nativePointer)

    /** The bounding box of the vertex positions. */
    public val bounds: Box? =
        BoxAccumulator().apply { MeshNative.fillBounds(nativePointer, this) }.box

    /** The transforms used to convert packed attributes into their actual values. */
    public val vertexAttributeUnpackingParams: List<MeshAttributeUnpackingParams> = run {
        val attributeCount = MeshNative.getAttributeCount(nativePointer)
        Collections.unmodifiableList(
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
        )
    }

    /**
     * Only for tests - creates a new empty [Mesh]. Since a [Mesh] is immutable, this serves no
     * practical purpose outside of tests.
     */
    @VisibleForTesting internal constructor() : this(MeshNative.createEmpty())

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

    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        MeshNative.free(nativePointer)
    }

    /** Declared primarily as a target for extension functions. */
    public companion object {
        // The maximum number of components in [MeshAttributeUnpackingParams].
        private const val MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS = 4

        /** Construct a [Mesh] from an unowned heap-allocated native pointer to a C++ `Mesh`. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): Mesh = Mesh(unownedNativePointer)
    }
}

/**
 * Helper object to contain native JNI calls. The alternative to this is putting the methods in
 * [Mesh] itself (doesn't work for native calls used by constructors), or in [Mesh.Companion] (makes
 * the `JNI_METHOD` naming a little less clear).
 */
@UsedByNative
private object MeshNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun free(nativePointer: Long)

    /**
     * Returns a direct [ByteBuffer] wrapped around the contents of `ink::Mesh::RawVertexData`. It
     * will be writeable, so be sure to only expose a read-only wrapper of it.
     */
    @UsedByNative external fun createRawVertexBuffer(nativePointer: Long): ByteBuffer?

    @UsedByNative external fun getVertexStride(nativePointer: Long): Int

    @UsedByNative external fun getVertexCount(nativePointer: Long): Int

    /** Like [createRawVertexBuffer], but with `ink::Mesh::RawIndexData`. */
    @UsedByNative external fun createRawTriangleIndexBuffer(nativePointer: Long): ByteBuffer?

    @UsedByNative external fun getTriangleCount(nativePointer: Long): Int

    @UsedByNative external fun getAttributeCount(nativePointer: Long): Int

    /**
     * Sets the given [BoxAccumulator] to the bounds of the mesh, including resetting the object if
     * the mesh has no bounds.
     */
    @UsedByNative external fun fillBounds(nativePointer: Long, boxAccumulator: BoxAccumulator)

    /**
     * Set the given [offsets] and [scales] arrays (each of which must have at least
     * [Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS] elements) to the unpacking transform offsets
     * and scales for the attribute with the given [attributeIndex].
     *
     * @return The number of elements of [offsets] and [scales] that have been filled. This is the
     *   number of [ComponentUnpackingParams] in the [MeshAttributeUnpackingParams] that should be
     *   created for this attribute.
     */
    @UsedByNative
    external fun fillAttributeUnpackingParams(
        nativePointer: Long,
        attributeIndex: Int,
        offsets: FloatArray,
        scales: FloatArray,
    ): Int

    /**
     * Return the address of a newly allocated copy of the `ink::MeshFormat` belonging to this mesh.
     */
    @UsedByNative external fun newCopyOfFormat(nativePointer: Long): Long

    @UsedByNative
    external fun fillPosition(nativePointer: Long, vertexIndex: Int, outPosition: MutableVec)

    @VisibleForTesting @UsedByNative external fun createEmpty(): Long
}
