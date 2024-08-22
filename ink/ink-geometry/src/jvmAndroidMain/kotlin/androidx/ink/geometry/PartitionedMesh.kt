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
import androidx.ink.geometry.internal.getValue
import androidx.ink.geometry.internal.threadLocal
import androidx.ink.nativeloader.NativeLoader

/**
 * An immutable† complex shape expressed as a set of triangles. This is used to represent the shape
 * of a stroke or other complex objects see [MeshCreation]. The mesh may be divided into multiple
 * partitions, which enables certain brush effects (e.g. "multi-coat"), and allows ink to create
 * strokes requiring greater than 216 triangles (which must be rendered in multiple passes).
 *
 * A PartitionedMesh may optionally have one or more "outlines", which are polylines that traverse
 * some or all of the vertices in the mesh; these are used for path-based rendering of strokes. This
 * supports disjoint meshes such as dashed lines.
 *
 * PartitionedMesh provides fast intersection and coverage testing by use of an internal spatial
 * index.
 *
 * † PartitionedMesh is technically not immutable, as the spatial index is lazily instantiated;
 * however, from the perspective of a caller, its properties do not change over the course of its
 * lifetime. The entire object is thread-safe.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class PartitionedMesh
/** Only for use within the ink library. Constructs a [PartitionedMesh] from native pointer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    /**
     * This is the raw pointer address of an `ink::ModeledShape` that has been heap allocated to be
     * owned solely by this JVM [PartitionedMesh] object. Although the `ink::ModeledShape` is owned
     * exclusively by this [PartitionedMesh] object, it may be a copy of another
     * `ink::ModeledShape`, where it has a copy of fairly lightweight metadata but shares ownership
     * of the more heavyweight `ink::Mesh` objects. This class is responsible for freeing the
     * `ink::ModeledShape` through its [finalize] method.
     */
    private var nativeAddress: Long
) {

    /**
     * Only for use within the ink library. Returns the native pointer held by this
     * [PartitionedMesh].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun getNativeAddress(): Long = nativeAddress

    private val scratchIntArray by threadLocal { IntArray(2) }

    /**
     * Only for tests - creates a new empty [PartitionedMesh]. Since a [PartitionedMesh] is
     * immutable, this serves no practical purpose outside of tests.
     */
    @VisibleForTesting internal constructor() : this(ModeledShapeNative.alloc())

    /**
     * The number of render groups in this mesh. Each outline in the [PartitionedMesh] belongs to
     * exactly one render group, which are numbered in z-order: the group with index zero should be
     * rendered on bottom; the group with the highest index should be rendered on top.
     */
    @IntRange(from = 0)
    public val renderGroupCount: Int =
        ModeledShapeNative.getRenderGroupCount(nativeAddress).also { check(it >= 0) }

    /** The [Mesh] objects that make up this shape. */
    private val meshesByGroup: List<List<Mesh>> = buildList {
        for (groupIndex in 0 until renderGroupCount) {
            val nativeAddressesOfMeshes =
                ModeledShapeNative.getNativeAddressesOfMeshes(nativeAddress, groupIndex)
            add(nativeAddressesOfMeshes.map(::Mesh))
        }
    }

    /**
     * The minimum bounding box of the [PartitionedMesh]. This will be null if the [PartitionedMesh]
     * is empty.
     */
    public val bounds: Box? = run {
        val envelope = BoxAccumulator()
        for (meshes in meshesByGroup) {
            for (mesh in meshes) {
                envelope.add(mesh.bounds)
            }
        }
        envelope.box
    }

    /** Returns the [MeshFormat] used for each [Mesh] in the specified render group. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun renderGroupFormat(@IntRange(from = 0) groupIndex: Int): MeshFormat {
        require(groupIndex >= 0 && groupIndex < renderGroupCount) {
            "groupIndex=$groupIndex must be between 0 and renderGroupCount=${renderGroupCount}"
        }
        return MeshFormat(ModeledShapeNative.getRenderGroupFormat(nativeAddress, groupIndex))
    }

    /**
     * Returns the meshes that make up render group [groupIndex], listed in z-order (the first mesh
     * in the span should be rendered on bottom; the last mesh should be rendered on top).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun renderGroupMeshes(@IntRange(from = 0) groupIndex: Int): List<Mesh> {
        require(groupIndex >= 0 && groupIndex < renderGroupCount) {
            "groupIndex=$groupIndex must be between 0 and renderGroupCount=${renderGroupCount}"
        }
        return meshesByGroup[groupIndex]
    }

    /** The number of outlines that comprise this shape. */
    @IntRange(from = 0)
    public fun outlineCount(@IntRange(from = 0) groupIndex: Int): Int {
        require(groupIndex >= 0 && groupIndex < renderGroupCount) {
            "groupIndex=$groupIndex must be between 0 and renderGroupCount=${renderGroupCount}"
        }
        return ModeledShapeNative.getOutlineCount(nativeAddress, groupIndex).also { check(it >= 0) }
    }

    /**
     * The number of vertices that are in the outline at index [outlineIndex], and within the render
     * group at [groupIndex].
     */
    @IntRange(from = 0)
    public fun outlineVertexCount(
        @IntRange(from = 0) groupIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
    ): Int {
        require(outlineIndex >= 0 && outlineIndex < outlineCount(groupIndex)) {
            "outlineIndex=$outlineIndex must be between 0 and outlineCount=${outlineCount(groupIndex)}"
        }
        return ModeledShapeNative.getOutlineVertexCount(nativeAddress, groupIndex, outlineIndex)
            .also { check(it >= 0) }
    }

    /**
     * Retrieve the outline vertex position from the outline at index [outlineIndex] (which can be
     * up to, but not including, [outlineCount]), and the vertex from within that outline at index
     * [outlineVertexIndex] (which can be up to, but not including, the result of calling
     * [outlineVertexCount] with [outlineIndex]). The resulting x/y position of that outline vertex
     * will be put into [outPosition], which can be pre-allocated and reused to avoid allocations.
     */
    public fun populateOutlinePosition(
        @IntRange(from = 0) groupIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
        @IntRange(from = 0) outlineVertexIndex: Int,
        outPosition: MutableVec,
    ) {
        val outlineVertexCount = outlineVertexCount(groupIndex, outlineIndex)
        require(outlineVertexIndex >= 0 && outlineVertexIndex < outlineVertexCount) {
            "outlineVertexIndex=$outlineVertexIndex must be between 0 and " +
                "outlineVertexCount($outlineVertexIndex)=$outlineVertexCount"
        }
        ModeledShapeNative.fillOutlineMeshIndexAndMeshVertexIndex(
            nativeAddress,
            groupIndex,
            outlineIndex,
            outlineVertexIndex,
            scratchIntArray,
        )
        val (meshIndex, meshVertexIndex) = scratchIntArray
        val mesh = meshesByGroup[groupIndex][meshIndex]
        mesh.fillPosition(meshVertexIndex, outPosition)
    }

    /**
     * Computes an approximate measure of what portion of this [PartitionedMesh] is covered by or
     * overlaps with [triangle]. This is calculated by finding the sum of areas of the triangles
     * that intersect the given [triangle], and dividing that by the sum of the areas of all
     * triangles in the [PartitionedMesh], all in the [PartitionedMesh]'s coordinate space.
     * Triangles in the [PartitionedMesh] that overlap each other (e.g. in the case of a stroke that
     * loops back over itself) are counted individually. Note that, if any triangles have negative
     * area (due to winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute
     * value of their area will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to the
     * [IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun coverage(
        triangle: Triangle,
        triangleToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        ModeledShapeNative.modeledShapeTriangleCoverage(
            nativeAddress = nativeAddress,
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
     * winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute value of their
     * area will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [PartitionedMesh]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun coverage(box: Box, boxToThis: AffineTransform = AffineTransform.IDENTITY): Float =
        ModeledShapeNative.modeledShapeBoxCoverage(
            nativeAddress = nativeAddress,
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
     * area (due to winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute
     * value of their area will be used instead.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [PartitionedMesh]'s coordinate space, which
     * defaults to the [IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun coverage(
        parallelogram: Parallelogram,
        parallelogramToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        ModeledShapeNative.modeledShapeParallelogramCoverage(
            nativeAddress = nativeAddress,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
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
     * area (due to winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute
     * value of their area will be used instead.t
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to the
     * [IDENTITY].
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun coverage(
        other: PartitionedMesh,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        ModeledShapeNative.modeledShapeModeledShapeCoverage(
            thisShapeNativeAddress = nativeAddress,
            otherShapeNativeAddress = other.nativeAddress,
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
     * this.coverage(triangle, triangleToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to the
     * [IDENTITY].
     */
    @JvmOverloads
    public fun coverageIsGreaterThan(
        triangle: Triangle,
        coverageThreshold: Float,
        triangleToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        ModeledShapeNative.modeledShapeTriangleCoverageIsGreaterThan(
            nativeAddress = nativeAddress,
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
     * this.coverage(box, boxToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [PartitionedMesh]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
    public fun coverageIsGreaterThan(
        box: Box,
        coverageThreshold: Float,
        boxToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        ModeledShapeNative.modeledShapeBoxCoverageIsGreaterThan(
            nativeAddress = nativeAddress,
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
     * this.coverage(parallelogram, parallelogramToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [PartitionedMesh]'s coordinate space, which
     * defaults to the [IDENTITY].
     */
    @JvmOverloads
    public fun coverageIsGreaterThan(
        parallelogram: Parallelogram,
        coverageThreshold: Float,
        parallelogramToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        ModeledShapeNative.modeledShapeParallelogramCoverageIsGreaterThan(
            nativeAddress = nativeAddress,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
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
     * this.coverage(other, otherShapeToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [PartitionedMesh], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [PartitionedMesh]'s coordinate space, which defaults to the
     * [IDENTITY].
     */
    @JvmOverloads
    public fun coverageIsGreaterThan(
        other: PartitionedMesh,
        coverageThreshold: Float,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        ModeledShapeNative.modeledShapeModeledShapeCoverageIsGreaterThan(
            thisShapeNativeAddress = nativeAddress,
            otherShapeNativeAddress = other.nativeAddress,
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
        ModeledShapeNative.initializeSpatialIndex(nativeAddress)

    /** Returns true if this MutableEnvelope's spatial index has been initialized. */
    @VisibleForTesting
    internal fun isSpatialIndexInitialized(): Boolean =
        ModeledShapeNative.isSpatialIndexInitialized(nativeAddress)

    override fun toString(): String {
        val address = java.lang.Long.toHexString(nativeAddress)
        return "PartitionedMesh(bounds=$bounds, meshesByGroup=$meshesByGroup, nativeAddress=$address)"
    }

    protected fun finalize() {
        // NOMUTANTS--Not tested post garbage collection.
        if (nativeAddress == 0L) return
        ModeledShapeNative.free(nativeAddress)
        nativeAddress = 0L
    }

    /** Declared as a target for extension functions. */
    public companion object
}

/**
 * Helper object to contain native JNI calls. The alternative to this is putting the methods in
 * [PartitionedMesh] itself (passes down an unused `jobject`, and doesn't work for native calls used
 * by constructors), or in [PartitionedMesh.Companion] (makes the `JNI_METHOD` naming less clear).
 */
private object ModeledShapeNative {

    init {
        NativeLoader.load()
    }

    external fun alloc(): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun free(
        nativeAddress: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun getNativeAddressesOfMeshes(
        nativeAddress: Long,
        groupIndex: Int
    ): LongArray // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun getRenderGroupCount(
        nativeAddress: Long
    ): Int // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun getRenderGroupFormat(
        nativeAddress: Long,
        groupIndex: Int
    ): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun getOutlineCount(
        nativeAddress: Long,
        groupIndex: Int
    ): Int // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun getOutlineVertexCount(nativeAddress: Long, groupIndex: Int, outlineIndex: Int): Int

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun fillOutlineMeshIndexAndMeshVertexIndex(
        nativeAddress: Long,
        groupIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outMeshIndexAndMeshVertexIndex: IntArray,
    )

    /**
     * JNI method to construct C++ [ModeledShape] and [Triangle] objects and calculate coverage
     * using them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeTriangleCoverage(
        nativeAddress: Long,
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

    /**
     * JNI method to construct C++ [ModeledShape] and [Triangle] objects and calculate coverage
     * using them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeBoxCoverage(
        nativeAddress: Long,
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

    /**
     * JNI method to construct C++ [ModeledShape] and [Parallelogram] objects and calculate coverage
     * using them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeParallelogramCoverage(
        nativeAddress: Long,
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

    /** JNI method to construct C++ two [ModeledShape] objects and calculate coverage using them. */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeModeledShapeCoverage(
        thisShapeNativeAddress: Long,
        otherShapeNativeAddress: Long,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Float

    /**
     * JNI method to construct C++ [ModeledShape] and [Triangle] objects and call native
     * [coverageIsGreaterThan] on them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeTriangleCoverageIsGreaterThan(
        nativeAddress: Long,
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

    /**
     * JNI method to construct C++ [ModeledShape] and [Box] objects and call native
     * [coverageIsGreaterThan] on them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeBoxCoverageIsGreaterThan(
        nativeAddress: Long,
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

    /**
     * JNI method to construct C++ [ModeledShape] and [Parallelogram] objects and call native
     * [coverageIsGreaterThan] on them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeParallelogramCoverageIsGreaterThan(
        nativeAddress: Long,
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

    /**
     * JNI method to construct two C++ [ModeledShape] objects and call native
     * [coverageIsGreaterThan] on them.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun modeledShapeModeledShapeCoverageIsGreaterThan(
        thisShapeNativeAddress: Long,
        otherShapeNativeAddress: Long,
        coverageThreshold: Float,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Boolean

    external fun initializeSpatialIndex(
        nativeAddress: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    external fun isSpatialIndexInitialized(
        nativeAddress: Long
    ): Boolean // TODO: b/355248266 - @Keep must go in Proguard config file instead.
}
