/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.NativePointer
import kotlin.jvm.JvmOverloads

/**
 * An immutable** complex shape expressed as a set of triangles. This is used to represent the shape
 * of a stroke or other complex objects. The mesh may be divided into multiple partitions, which
 * enables certain brush effects (e.g. "multi-coat"), and allows ink to create strokes using greater
 * than 2^16 triangles (which must be rendered in multiple passes).
 *
 * A [PartitionedMesh] may optionally have one or more "outlines", which are polylines that traverse
 * some or all of the vertices in the mesh; these are used for path-based rendering of strokes. This
 * supports disjoint meshes such as dashed lines.
 *
 * [PartitionedMesh] provides fast intersection and coverage testing by use of an internal spatial
 * index.
 *
 * ** [PartitionedMesh] is technically not immutable, as the spatial index is lazily instantiated;
 * however, from the perspective of a caller, its properties do not change over the course of its
 * lifetime. The entire object is thread-safe.
 */
public class PartitionedMesh private constructor(nativeAlloc: () -> Long) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long by NativePointer(nativeAlloc, PartitionedMeshNative::free)

    /**
     * Only for tests - creates a new empty [PartitionedMesh]. Since a [PartitionedMesh] is
     * immutable, this serves no practical purpose outside of tests.
     */
    @VisibleForTesting internal constructor() : this(PartitionedMeshNative::createEmptyForTesting)

    /**
     * Only for tests - creates a new [PartitionedMesh] from a single triangle. For non-test
     * purposes, it would make more sense to use the geometry operations that take [Triangle] as
     * input.
     */
    @VisibleForTesting
    internal constructor(
        triangle: Triangle
    ) : this({
        PartitionedMeshNative.createFromTriangleForTesting(
            triangle.p0.x,
            triangle.p0.y,
            triangle.p1.x,
            triangle.p1.y,
            triangle.p2.x,
            triangle.p2.y,
        )
    })

    /**
     * Returns the number of render groups in this mesh. Each outline in the [PartitionedMesh]
     * belongs to exactly one render group, which are numbered in z-order: the group with index zero
     * should be rendered on bottom; the group with the highest index should be rendered on top.
     */
    @IntRange(from = 0)
    public fun getRenderGroupCount(): Int =
        PartitionedMeshNative.getRenderGroupCount(nativePointer).also { check(it >= 0) }

    /** The [Mesh] objects that make up this shape. */
    private val meshesByGroup: List<List<Mesh>> =
        (0 until getRenderGroupCount()).map { groupIndex ->
            PartitionedMeshNative.getRenderGroupMeshPointers(nativePointer, groupIndex).map {
                // Each Kotlin `Mesh` object gets a new heap-allocated copy of the underlying C++
                // object,
                // but the C++ object is cheap to copy since it internally keeps a `shared_ptr` to
                // its
                // (immutable) data.
                Mesh.copyFromNative(it)
            }
        }

    private val bounds: Box? by lazy { PartitionedMeshNative.createBounds(nativePointer) }

    /**
     * Returns the minimum bounding box of the [PartitionedMesh]. This will be null if the
     * [PartitionedMesh] is empty.
     */
    public fun computeBoundingBox(): Box? = bounds

    /** Returns the [MeshFormat] used for each [Mesh] in the specified render group. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun renderGroupFormat(@IntRange(from = 0) groupIndex: Int): MeshFormat {
        require(groupIndex >= 0 && groupIndex < getRenderGroupCount()) {
            "groupIndex=$groupIndex must be between 0 and getRenderGroupCount()=${getRenderGroupCount()}"
        }
        return MeshFormat.wrapNative {
            PartitionedMeshNative.newCopyOfRenderGroupFormat(nativePointer, groupIndex)
        }
    }

    /**
     * Returns the meshes that make up render group [groupIndex], listed in z-order (the first mesh
     * in the span should be rendered on bottom; the last mesh should be rendered on top).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun renderGroupMeshes(@IntRange(from = 0) groupIndex: Int): List<Mesh> {
        require(groupIndex >= 0 && groupIndex < getRenderGroupCount()) {
            "groupIndex=$groupIndex must be between 0 and getRenderGroupCount()=${getRenderGroupCount()}"
        }
        return meshesByGroup[groupIndex]
    }

    /**
     * Returns the number of outlines of the mesh for the render group at [groupIndex].
     *
     * Groups with discontinuous geometry will always have multiple outlines, but even continuous
     * geometry may be drawn with multiple overlapping outlines when this improves rendering quality
     * or performance.
     */
    @IntRange(from = 0)
    public fun getOutlineCount(@IntRange(from = 0) groupIndex: Int): Int {
        require(groupIndex >= 0 && groupIndex < getRenderGroupCount()) {
            "groupIndex=$groupIndex must be between 0 and getRenderGroupCount()=${getRenderGroupCount()}"
        }
        return PartitionedMeshNative.getOutlineCount(nativePointer, groupIndex).also {
            check(it >= 0)
        }
    }

    /**
     * Returns the number of vertices that are in the outline at [outlineIndex] in the render group
     * at [groupIndex].
     */
    @IntRange(from = 0)
    public fun getOutlineVertexCount(
        @IntRange(from = 0) groupIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
    ): Int {
        require(outlineIndex >= 0 && outlineIndex < getOutlineCount(groupIndex)) {
            "outlineIndex=$outlineIndex must be between 0 and getOutlineCount=${getOutlineCount(groupIndex)}"
        }
        return PartitionedMeshNative.getOutlineVertexCount(nativePointer, groupIndex, outlineIndex)
            .also { check(it >= 0) }
    }

    /**
     * Populates [outPosition] with the position of the outline vertex at [outlineVertexIndex] in
     * the outline at [outlineIndex] in the render group at [groupIndex], and returns [outPosition].
     * [groupIndex] must be less than [getRenderGroupCount], [outlineIndex] must be less
     * [getOutlineVertexCount] for [groupIndex], and [outlineVertexIndex] must be less than
     * [getOutlineVertexCount] for [groupIndex] and [outlineIndex].
     */
    public fun populateOutlinePosition(
        @IntRange(from = 0) groupIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
        @IntRange(from = 0) outlineVertexIndex: Int,
        outPosition: MutableVec,
    ): MutableVec {
        val outlineVertexCount = getOutlineVertexCount(groupIndex, outlineIndex)
        require(outlineVertexIndex >= 0 && outlineVertexIndex < outlineVertexCount) {
            "outlineVertexIndex=$outlineVertexIndex must be between 0 and " +
                "outlineVertexCount($outlineVertexIndex)=$outlineVertexCount"
        }
        PartitionedMeshNative.populateOutlineVertexPosition(
            nativePointer,
            groupIndex,
            outlineIndex,
            outlineVertexIndex,
            outPosition,
        )
        return outPosition
    }

    /**
     * Computes an approximate measure of what portion of this [PartitionedMesh] is covered by or
     * overlaps with [triangle]. This is calculated by finding the sum of areas of the triangles
     * that intersect the given [triangle], and dividing that by the sum of the areas of all
     * triangles in the [PartitionedMesh], all in the [PartitionedMesh]'s coordinate space.
     * Triangles in the [PartitionedMesh] that overlap each other (e.g. in the case of a stroke that
     * loops back over itself) are counted individually. Note that, if any triangles have negative
     * area (due to winding, see [Triangle.computeSignedArea]), the absolute value of their area
     * will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to
     * [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun computeCoverage(
        triangle: Triangle,
        triangleToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        PartitionedMeshNative.triangleCoverage(
            nativePointer = nativePointer,
            triangleP0X = triangle.p0.x,
            triangleP0Y = triangle.p0.y,
            triangleP1X = triangle.p1.x,
            triangleP1Y = triangle.p1.y,
            triangleP2X = triangle.p2.x,
            triangleP2Y = triangle.p2.y,
            triangleToThisTransformA = triangleToThis.m00,
            triangleToThisTransformB = triangleToThis.m10,
            triangleToThisTransformC = triangleToThis.m20,
            triangleToThisTransformD = triangleToThis.m01,
            triangleToThisTransformE = triangleToThis.m11,
            triangleToThisTransformF = triangleToThis.m21,
        )

    /**
     * Computes an approximate measure of what portion of this [PartitionedMesh] is covered by or
     * overlaps with [box]. This is calculated by finding the sum of areas of the triangles that
     * intersect the given [box], and dividing that by the sum of the areas of all triangles in the
     * [PartitionedMesh], all in the [PartitionedMesh]'s coordinate space. Triangles in the
     * [PartitionedMesh] that overlap each other (e.g. in the case of a stroke that loops back over
     * itself) are counted individually. Note that, if any triangles have negative area (due to
     * winding, see [Triangle.computeSignedArea]), the absolute value of their area will be used
     * instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [PartitionedMesh]'s coordinate space, which defaults to [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun computeCoverage(
        box: Box,
        boxToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        PartitionedMeshNative.boxCoverage(
            nativePointer = nativePointer,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
            boxToThisTransformA = boxToThis.m00,
            boxToThisTransformB = boxToThis.m10,
            boxToThisTransformC = boxToThis.m20,
            boxToThisTransformD = boxToThis.m01,
            boxToThisTransformE = boxToThis.m11,
            boxToThisTransformF = boxToThis.m21,
        )

    /**
     * Computes an approximate measure of what portion of this [PartitionedMesh] is covered by or
     * overlaps with [parallelogram]. This is calculated by finding the sum of areas of the
     * triangles that intersect the given [parallelogram], and dividing that by the sum of the areas
     * of all triangles in the [PartitionedMesh], all in the [PartitionedMesh]'s coordinate space.
     * Triangles in the [PartitionedMesh] that overlap each other (e.g. in the case of a stroke that
     * loops back over itself) are counted individually. Note that, if any triangles have negative
     * area (due to winding, see [Triangle.computeSignedArea]), the absolute value of their area
     * will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [PartitionedMesh]'s coordinate space, which
     * defaults to [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun computeCoverage(
        parallelogram: Parallelogram,
        parallelogramToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        PartitionedMeshNative.parallelogramCoverage(
            nativePointer = nativePointer,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotationDegrees,
            parallelogramShearFactor = parallelogram.skew,
            parallelogramToThisTransformA = parallelogramToThis.m00,
            parallelogramToThisTransformB = parallelogramToThis.m10,
            parallelogramToThisTransformC = parallelogramToThis.m20,
            parallelogramToThisTransformD = parallelogramToThis.m01,
            parallelogramToThisTransformE = parallelogramToThis.m11,
            parallelogramToThisTransformF = parallelogramToThis.m21,
        )

    /**
     * Computes an approximate measure of what portion of this [PartitionedMesh] is covered by or
     * overlaps with the [other] [PartitionedMesh]. This is calculated by finding the sum of areas
     * of the triangles that intersect [other], and dividing that by the sum of the areas of all
     * triangles in the [PartitionedMesh], all in the [PartitionedMesh]'s coordinate space.
     * Triangles in the [PartitionedMesh] that overlap each other (e.g. in the case of a stroke that
     * loops back over itself) are counted individually. Note that, if any triangles have negative
     * area (due to winding, see [Triangle.computeSignedArea]), the absolute value of their area
     * will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to
     * [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun computeCoverage(
        other: PartitionedMesh,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        PartitionedMeshNative.partitionedMeshCoverage(
            nativePointer = nativePointer,
            otherShapeNativePointer = other.nativePointer,
            otherShapeToThisTransformA = otherShapeToThis.m00,
            otherShapeToThisTransformB = otherShapeToThis.m10,
            otherShapeToThisTransformC = otherShapeToThis.m20,
            otherShapeToThisTransformD = otherShapeToThis.m01,
            otherShapeToThisTransformE = otherShapeToThis.m11,
            otherShapeToThisTransformF = otherShapeToThis.m21,
        )

    /**
     * Returns true if the approximate portion of the [PartitionedMesh] covered by [triangle] is
     * greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * computeCoverage(triangle, triangleToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to
     * [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    public fun computeCoverageIsGreaterThan(
        triangle: Triangle,
        coverageThreshold: Float,
        triangleToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        PartitionedMeshNative.triangleCoverageIsGreaterThan(
            nativePointer = nativePointer,
            triangleP0X = triangle.p0.x,
            triangleP0Y = triangle.p0.y,
            triangleP1X = triangle.p1.x,
            triangleP1Y = triangle.p1.y,
            triangleP2X = triangle.p2.x,
            triangleP2Y = triangle.p2.y,
            coverageThreshold = coverageThreshold,
            triangleToThisTransformA = triangleToThis.m00,
            triangleToThisTransformB = triangleToThis.m10,
            triangleToThisTransformC = triangleToThis.m20,
            triangleToThisTransformD = triangleToThis.m01,
            triangleToThisTransformE = triangleToThis.m11,
            triangleToThisTransformF = triangleToThis.m21,
        )

    /**
     * Returns true if the approximate portion of the [PartitionedMesh] covered by [box] is greater
     * than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * computeCoverage(box, boxToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [PartitionedMesh]'s coordinate space, which defaults to [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    public fun computeCoverageIsGreaterThan(
        box: Box,
        coverageThreshold: Float,
        boxToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        PartitionedMeshNative.boxCoverageIsGreaterThan(
            nativePointer = nativePointer,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
            coverageThreshold = coverageThreshold,
            boxToThisTransformA = boxToThis.m00,
            boxToThisTransformB = boxToThis.m10,
            boxToThisTransformC = boxToThis.m20,
            boxToThisTransformD = boxToThis.m01,
            boxToThisTransformE = boxToThis.m11,
            boxToThisTransformF = boxToThis.m21,
        )

    /**
     * Returns true if the approximate portion of the [PartitionedMesh] covered by [parallelogram]
     * is greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * computeCoverage(parallelogram, parallelogramToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [PartitionedMesh]'s coordinate space, which
     * defaults to [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    public fun computeCoverageIsGreaterThan(
        parallelogram: Parallelogram,
        coverageThreshold: Float,
        parallelogramToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        PartitionedMeshNative.parallelogramCoverageIsGreaterThan(
            nativePointer = nativePointer,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotationDegrees,
            parallelogramShearFactor = parallelogram.skew,
            coverageThreshold = coverageThreshold,
            parallelogramToThisTransformA = parallelogramToThis.m00,
            parallelogramToThisTransformB = parallelogramToThis.m10,
            parallelogramToThisTransformC = parallelogramToThis.m20,
            parallelogramToThisTransformD = parallelogramToThis.m01,
            parallelogramToThisTransformE = parallelogramToThis.m11,
            parallelogramToThisTransformF = parallelogramToThis.m21,
        )

    /**
     * Returns true if the approximate portion of this [PartitionedMesh] covered by the [other]
     * [PartitionedMesh] is greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * computeCoverage(other, otherShapeToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to
     * [AffineTransform.IDENTITY].
     */
    @JvmOverloads
    public fun computeCoverageIsGreaterThan(
        other: PartitionedMesh,
        coverageThreshold: Float,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        PartitionedMeshNative.partitionedMeshCoverageIsGreaterThan(
            nativePointer = nativePointer,
            otherShapeNativePointer = other.nativePointer,
            coverageThreshold = coverageThreshold,
            otherShapeToThisTransformA = otherShapeToThis.m00,
            otherShapeToThisTransformB = otherShapeToThis.m10,
            otherShapeToThisTransformC = otherShapeToThis.m20,
            otherShapeToThisTransformD = otherShapeToThis.m01,
            otherShapeToThisTransformE = otherShapeToThis.m11,
            otherShapeToThisTransformF = otherShapeToThis.m21,
        )

    /**
     * Initializes this MutableEnvelope's spatial index for geometry queries. If a geometry query is
     * made with this shape and the spatial index is not currently initialized, it will be
     * initialized in real time to satisfy that query.
     */
    public fun initializeSpatialIndex(): Unit =
        PartitionedMeshNative.initializeSpatialIndex(nativePointer)

    /** Returns true if this MutableEnvelope's spatial index has been initialized. */
    @VisibleForTesting
    internal fun isSpatialIndexInitialized(): Boolean =
        PartitionedMeshNative.isSpatialIndexInitialized(nativePointer)

    override fun toString(): String {
        return "PartitionedMesh(bounds=${computeBoundingBox()}, meshesByGroup=$meshesByGroup)"
    }

    /** Declared as a target for extension functions. */
    public companion object {
        /**
         * Construct a [PartitionedMesh], taking a callback that heap-allocates and returns a
         * pointer to a C++ `PartitionedMesh`.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(nativeAlloc: () -> Long): PartitionedMesh =
            PartitionedMesh(nativeAlloc)
    }
}

/** Helper object to contain native JNI calls. */
expect internal object PartitionedMeshNative {

    fun createEmptyForTesting(): Long

    fun createFromTriangleForTesting(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Long

    fun free(nativePointer: Long)

    fun getRenderGroupMeshPointers(nativePointer: Long, groupIndex: Int): LongArray

    fun getRenderGroupCount(nativePointer: Long): Int

    fun newCopyOfRenderGroupFormat(nativePointer: Long, groupIndex: Int): Long

    fun getOutlineCount(nativePointer: Long, groupIndex: Int): Int

    fun getOutlineVertexCount(nativePointer: Long, groupIndex: Int, outlineIndex: Int): Int

    fun populateOutlineVertexPosition(
        nativePointer: Long,
        groupIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    )

    /**
     * Returns a new [ImmutableBox] with the bounding box of the mesh at [nativePointer] if
     * non-empty, or null if the mesh is empty.
     */
    fun createBounds(nativePointer: Long): ImmutableBox?

    fun triangleCoverage(
        nativePointer: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        triangleToThisTransformA: Float,
        triangleToThisTransformB: Float,
        triangleToThisTransformC: Float,
        triangleToThisTransformD: Float,
        triangleToThisTransformE: Float,
        triangleToThisTransformF: Float,
    ): Float

    fun boxCoverage(
        nativePointer: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        boxToThisTransformA: Float,
        boxToThisTransformB: Float,
        boxToThisTransformC: Float,
        boxToThisTransformD: Float,
        boxToThisTransformE: Float,
        boxToThisTransformF: Float,
    ): Float

    fun parallelogramCoverage(
        nativePointer: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        parallelogramToThisTransformA: Float,
        parallelogramToThisTransformB: Float,
        parallelogramToThisTransformC: Float,
        parallelogramToThisTransformD: Float,
        parallelogramToThisTransformE: Float,
        parallelogramToThisTransformF: Float,
    ): Float

    fun partitionedMeshCoverage(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Float

    fun triangleCoverageIsGreaterThan(
        nativePointer: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        coverageThreshold: Float,
        triangleToThisTransformA: Float,
        triangleToThisTransformB: Float,
        triangleToThisTransformC: Float,
        triangleToThisTransformD: Float,
        triangleToThisTransformE: Float,
        triangleToThisTransformF: Float,
    ): Boolean

    fun boxCoverageIsGreaterThan(
        nativePointer: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        coverageThreshold: Float,
        boxToThisTransformA: Float,
        boxToThisTransformB: Float,
        boxToThisTransformC: Float,
        boxToThisTransformD: Float,
        boxToThisTransformE: Float,
        boxToThisTransformF: Float,
    ): Boolean

    fun parallelogramCoverageIsGreaterThan(
        nativePointer: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        coverageThreshold: Float,
        parallelogramToThisTransformA: Float,
        parallelogramToThisTransformB: Float,
        parallelogramToThisTransformC: Float,
        parallelogramToThisTransformD: Float,
        parallelogramToThisTransformE: Float,
        parallelogramToThisTransformF: Float,
    ): Boolean

    fun partitionedMeshCoverageIsGreaterThan(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        coverageThreshold: Float,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Boolean

    fun initializeSpatialIndex(nativePointer: Long)

    fun isSpatialIndexInitialized(nativePointer: Long): Boolean
}
