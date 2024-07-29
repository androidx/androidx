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

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.geometry.internal.getValue
import androidx.ink.geometry.internal.threadLocal
import androidx.ink.nativeloader.NativeLoader

/**
 * A triangulated shape, consisting of zero or more non-empty [Mesh]es, which may be indexed for
 * faster geometric queries. These meshes are divided among zero or more "render groups"; all the
 * meshes in a render group must have the same [MeshFormat], and can thus be rendered together. A
 * [ModeledShape] also optionally carries one or more "outlines", which are (potentially incomplete)
 * traversals of the vertices in the meshes, which could be used e.g. for path-based rendering. Note
 * that these render groups and outlines are ignored for the purposes of geometric queries; they
 * exist only for rendering purposes.
 *
 * This is not meant to be constructed directly by developers. The primary constructor is to have a
 * new instance of this class manage a native `ink::ModeledShape` instance created by another
 * Strokes API utility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class ModeledShape
/** Only for use within the ink library. Constructs a [ModeledShape] from native pointer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    /**
     * This is the raw pointer address of an `ink::ModeledShape` that has been heap allocated to be
     * owned solely by this JVM [ModeledShape] object. Although the `ink::ModeledShape` is owned
     * exclusively by this [ModeledShape] object, it may be a copy of another `ink::ModeledShape`,
     * where it has a copy of fairly lightweight metadata but shares ownership of the more
     * heavyweight `ink::Mesh` objects. This class is responsible for freeing the
     * `ink::ModeledShape` through its [finalize] method.
     */
    private var nativeAddress: Long
) {

    /**
     * Only for use within the ink library. Returns the native pointer held by this [ModeledShape].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun getNativeAddress(): Long = nativeAddress

    private val scratchIntArray by threadLocal { IntArray(2) }

    /**
     * Only for tests - creates a new empty [ModeledShape]. Since a [ModeledShape] is immutable,
     * this serves no practical purpose outside of tests.
     */
    @VisibleForTesting internal constructor() : this(ModeledShapeNative.alloc())

    /**
     * Returns the number of render groups in this shape. Each mesh in the [ModeledShape] belongs to
     * exactly one render group, and all meshes in the same render group will have the same
     * [MeshFormat] (and can thus be rendered together). The render groups are numbered in z-order
     * (the group with index zero should be rendered on bottom; the group with the highest index
     * should be rendered on top).
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

    /** The axis-aligned, rectangular region occupied by the [meshes] of this shape. */
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
     * The number of vertices in the outline at index [outlineIndex], which can be up to (but not
     * including) [outlineCount].
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
    public fun fillOutlinePosition(
        @IntRange(from = 0) groupIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
        @IntRange(from = 0) outlineVertexIndex: Int,
        outPosition: MutablePoint,
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

    override fun toString(): String {
        val address = java.lang.Long.toHexString(nativeAddress)
        return "ModeledShape(bounds=$bounds, meshesByGroup=$meshesByGroup, nativeAddress=$address)"
    }

    /**
     * Computes an approximate measure of what portion of this [ModeledShape] is covered by or
     * overlaps with [triangle]. This is calculated by finding the sum of areas of the triangles
     * that intersect the given [triangle], and dividing that by the sum of the areas of all
     * triangles in the [ModeledShape], all in the [ModeledShape]'s coordinate space. Triangles in
     * the [ModeledShape] that overlap each other (e.g. in the case of a stroke that loops back over
     * itself) are counted individually. Note that, if any triangles have negative area (due to
     * winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute value of their
     * area will be used instead.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
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
            triangleToThisTransformA = triangleToThis.a,
            triangleToThisTransformB = triangleToThis.b,
            triangleToThisTransformC = triangleToThis.c,
            triangleToThisTransformD = triangleToThis.d,
            triangleToThisTransformE = triangleToThis.e,
            triangleToThisTransformF = triangleToThis.f,
        )

    /**
     * Computes an approximate measure of what portion of this [ModeledShape] is covered by or
     * overlaps with [box]. This is calculated by finding the sum of areas of the triangles that
     * intersect the given [box], and dividing that by the sum of the areas of all triangles in the
     * [ModeledShape], all in the [ModeledShape]'s coordinate space. Triangles in the [ModeledShape]
     * that overlap each other (e.g. in the case of a stroke that loops back over itself) are
     * counted individually. Note that, if any triangles have negative area (due to winding, see
     * [com.google.inputmethod.ink.Triangle.signedArea]), the absolute value of their area will be
     * used instead.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
    public fun coverage(box: Box, boxToThis: AffineTransform = AffineTransform.IDENTITY): Float =
        ModeledShapeNative.modeledShapeBoxCoverage(
            nativeAddress = nativeAddress,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
            boxToThisTransformA = boxToThis.a,
            boxToThisTransformB = boxToThis.b,
            boxToThisTransformC = boxToThis.c,
            boxToThisTransformD = boxToThis.d,
            boxToThisTransformE = boxToThis.e,
            boxToThisTransformF = boxToThis.f,
        )

    /**
     * Computes an approximate measure of what portion of this [ModeledShape] is covered by or
     * overlaps with [parallelogram]. This is calculated by finding the sum of areas of the
     * triangles that intersect the given [parallelogram], and dividing that by the sum of the areas
     * of all triangles in the [ModeledShape], all in the [ModeledShape]'s coordinate space.
     * Triangles in the [ModeledShape] that overlap each other (e.g. in the case of a stroke that
     * loops back over itself) are counted individually. Note that, if any triangles have negative
     * area (due to winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute
     * value of their area will be used instead.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [ModeledShape]'s coordinate space, which defaults
     * to the [IDENTITY].
     */
    @JvmOverloads
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
            parallelogramToThisTransformA = parallelogramToThis.a,
            parallelogramToThisTransformB = parallelogramToThis.b,
            parallelogramToThisTransformC = parallelogramToThis.c,
            parallelogramToThisTransformD = parallelogramToThis.d,
            parallelogramToThisTransformE = parallelogramToThis.e,
            parallelogramToThisTransformF = parallelogramToThis.f,
        )

    /**
     * Computes an approximate measure of what portion of this [ModeledShape] is covered by or
     * overlaps with the [other] [ModeledShape]. This is calculated by finding the sum of areas of
     * the triangles that intersect [other], and dividing that by the sum of the areas of all
     * triangles in the [ModeledShape], all in the [ModeledShape]'s coordinate space. Triangles in
     * the [ModeledShape] that overlap each other (e.g. in the case of a stroke that loops back over
     * itself) are counted individually. Note that, if any triangles have negative area (due to
     * winding, see [com.google.inputmethod.ink.Triangle.signedArea]), the absolute value of their
     * area will be used instead.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
    public fun coverage(
        other: ModeledShape,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Float =
        ModeledShapeNative.modeledShapeModeledShapeCoverage(
            thisShapeNativeAddress = nativeAddress,
            otherShapeNativeAddress = other.nativeAddress,
            otherShapeToThisTransformA = otherShapeToThis.a,
            otherShapeToThisTransformB = otherShapeToThis.b,
            otherShapeToThisTransformC = otherShapeToThis.c,
            otherShapeToThisTransformD = otherShapeToThis.d,
            otherShapeToThisTransformE = otherShapeToThis.e,
            otherShapeToThisTransformF = otherShapeToThis.f,
        )

    /**
     * Returns true if the approximate portion of the [ModeledShape] covered by [triangle] is
     * greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * this.coverage(triangle, triangleToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [triangleToThis] contains the transform that maps from [triangle]'s
     * coordinate space to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
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
            triangleToThisTransformA = triangleToThis.a,
            triangleToThisTransformB = triangleToThis.b,
            triangleToThisTransformC = triangleToThis.c,
            triangleToThisTransformD = triangleToThis.d,
            triangleToThisTransformE = triangleToThis.e,
            triangleToThisTransformF = triangleToThis.f,
        )

    /**
     * Returns true if the approximate portion of the [ModeledShape] covered by [box] is greater
     * than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * this.coverage(box, boxToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [boxToThis] contains the transform that maps from [box]'s coordinate space
     * to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
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
            boxToThisTransformA = boxToThis.a,
            boxToThisTransformB = boxToThis.b,
            boxToThisTransformC = boxToThis.c,
            boxToThisTransformD = boxToThis.d,
            boxToThisTransformE = boxToThis.e,
            boxToThisTransformF = boxToThis.f,
        )

    /**
     * Returns true if the approximate portion of the [ModeledShape] covered by [parallelogram] is
     * greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * this.coverage(parallelogram, parallelogramToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [parallelogramToThis] contains the transform that maps from
     * [parallelogram]'s coordinate space to this [ModeledShape]'s coordinate space, which defaults
     * to the [IDENTITY].
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
            parallelogramToThisTransformA = parallelogramToThis.a,
            parallelogramToThisTransformB = parallelogramToThis.b,
            parallelogramToThisTransformC = parallelogramToThis.c,
            parallelogramToThisTransformD = parallelogramToThis.d,
            parallelogramToThisTransformE = parallelogramToThis.e,
            parallelogramToThisTransformF = parallelogramToThis.f,
        )

    /**
     * Returns true if the approximate portion of this [ModeledShape] covered by the [other]
     * [ModeledShape] is greater than [coverageThreshold].
     *
     * This is equivalent to:
     * ```
     * this.coverage(other, otherShapeToThis) > coverageThreshold
     * ```
     *
     * but may be faster.
     *
     * On an empty [ModeledShape], this will always return 0.
     *
     * Optional argument [otherShapeToThis] contains the transform that maps from [other]'s
     * coordinate space to this [ModeledShape]'s coordinate space, which defaults to the [IDENTITY].
     */
    @JvmOverloads
    public fun coverageIsGreaterThan(
        other: ModeledShape,
        coverageThreshold: Float,
        otherShapeToThis: AffineTransform = AffineTransform.IDENTITY,
    ): Boolean =
        ModeledShapeNative.modeledShapeModeledShapeCoverageIsGreaterThan(
            thisShapeNativeAddress = nativeAddress,
            otherShapeNativeAddress = other.nativeAddress,
            coverageThreshold = coverageThreshold,
            otherShapeToThisTransformA = otherShapeToThis.a,
            otherShapeToThisTransformB = otherShapeToThis.b,
            otherShapeToThisTransformC = otherShapeToThis.c,
            otherShapeToThisTransformD = otherShapeToThis.d,
            otherShapeToThisTransformE = otherShapeToThis.e,
            otherShapeToThisTransformF = otherShapeToThis.f,
        )

    /**
     * Initializes this MutableEnvelope's spatial index for geometry queries. If a geometry query is
     * made with this shape and the spatial index is not currently initialized, it will be
     * initialized in real time to satisfy that query.
     */
    public fun initializeSpatialIndex(): Unit =
        ModeledShapeNative.initializeSpatialIndex(nativeAddress)

    /** Returns true if this MutableEnvelope's spatial index has been initialized. */
    public fun isSpatialIndexInitialized(): Boolean =
        ModeledShapeNative.isSpatialIndexInitialized(nativeAddress)

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
 * [ModeledShape] itself (passes down an unused `jobject`, and doesn't work for native calls used by
 * constructors), or in [ModeledShape.Companion] (makes the `JNI_METHOD` naming less clear).
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
