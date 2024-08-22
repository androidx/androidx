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

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader

/**
 * Contains functions for intersection of ink geometry classes. For Kotlin callers, these are
 * available as extension functions on the geometry classes themselves.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public object Intersection {

    init {
        NativeLoader.load()
    }

    /**
     * Returns true if the point (a [Vec]), intersects [other]; this only occurs when the two points
     * are equal.
     */
    @JvmStatic public fun Vec.intersects(other: Vec): Boolean = (this == other)

    /**
     * Returns true when the point (a [Vec]) intersects with a [Segment]. All points on the segment,
     * including endpoints, intersect with the segment.
     */
    @JvmStatic
    public fun Vec.intersects(segment: Segment): Boolean {
        return nativeVecSegmentIntersects(
            vecX = this.x,
            vecY = this.y,
            segmentStartX = segment.start.x,
            segmentStartY = segment.start.y,
            segmentEndX = segment.end.x,
            segmentEndY = segment.end.y,
        )
    }

    /**
     * Returns true when the point (a [Vec]) intersects with a [Triangle]. All points on the
     * boundary of the triangle (including its vertices) and in the interior of the triangle
     * intersect with it.
     */
    @JvmStatic
    public fun Vec.intersects(triangle: Triangle): Boolean {
        return nativeVecTriangleIntersects(
            vecX = this.x,
            vecY = this.y,
            triangleP0X = triangle.p0.x,
            triangleP0Y = triangle.p0.y,
            triangleP1X = triangle.p1.x,
            triangleP1Y = triangle.p1.y,
            triangleP2X = triangle.p2.x,
            triangleP2Y = triangle.p2.y,
        )
    }

    /**
     * Returns true when the point (a [Vec]) intersects with a [Parallelogram]. All points on the
     * boundary of the parallelogram (including its vertices) and in the interior of the
     * parallelogram intersect with it.
     */
    @JvmStatic
    public fun Vec.intersects(parallelogram: Parallelogram): Boolean {
        return nativeVecParallelogramIntersects(
            vecX = this.x,
            vecY = this.y,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
        )
    }

    /**
     * Returns true when the point (a [Vec]) intersects with a [Box]. All points on the boundary of
     * the box (including its vertices) and in the interior of the box intersect with it.
     */
    @JvmStatic
    public fun Vec.intersects(box: Box): Boolean {
        return nativeVecBoxIntersects(
            vecX = this.x,
            vecY = this.y,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
        )
    }

    /**
     * Returns true when the point (a [Vec]) intersects with [mesh]. [meshToPoint] transforms the
     * coordinate space of [mesh] to the coordinate space that the intersection should be checked in
     * (that of the point). All points along the boundary of the [mesh] and the [mesh]s interior are
     * considered for intersection.
     *
     * Performance note: it is expensive to apply a transform to a mesh. To avoid unnecessary
     * calculations, the inverse of [meshToPoint] is used to perform the mathematically equivalent
     * intersection of the point in [mesh]’s object coordinates.
     */
    @JvmStatic
    public fun Vec.intersects(mesh: PartitionedMesh, meshToPoint: AffineTransform): Boolean {
        return nativeMeshVecIntersects(
            nativeMeshAddress = mesh.getNativeAddress(),
            vecX = this.x,
            vecY = this.y,
            meshToVecA = meshToPoint.m00,
            meshToVecB = meshToPoint.m10,
            meshToVecC = meshToPoint.m20,
            meshToVecD = meshToPoint.m01,
            meshToVecE = meshToPoint.m11,
            meshToVecF = meshToPoint.m21,
        )
    }

    /**
     * Returns true when a [Segment] intersects with another [Segment] --- when this segment has at
     * least one point (including the [start] and [end] points) in common with another [Segment].
     */
    @JvmStatic
    public fun Segment.intersects(other: Segment): Boolean {
        // Return true without calling the native code when this [Segment] and [other] are equal ---
        // i.e. have same endpoints.
        if (this == other) return true
        return nativeSegmentSegmentIntersects(
            segment1StartX = this.start.x,
            segment1StartY = this.start.y,
            segment1EndX = this.end.x,
            segment1EndY = this.end.y,
            segment2StartX = other.start.x,
            segment2StartY = other.start.y,
            segment2EndX = other.end.x,
            segment2EndY = other.end.y,
        )
    }

    /**
     * Returns true when a [Segment] intersects with a [Triangle] --- when this segment has at least
     * one point in common with the [Triangle]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Segment.intersects(triangle: Triangle): Boolean {
        return nativeSegmentTriangleIntersects(
            segmentStartX = this.start.x,
            segmentStartY = this.start.y,
            segmentEndX = this.end.x,
            segmentEndY = this.end.y,
            triangleP0X = triangle.p0.x,
            triangleP0Y = triangle.p0.y,
            triangleP1X = triangle.p1.x,
            triangleP1Y = triangle.p1.y,
            triangleP2X = triangle.p2.x,
            triangleP2Y = triangle.p2.y,
        )
    }

    /**
     * Returns true when a [Segment] intersects with a [Box] --- when this segment has at least one
     * point in common with the [Box]'s interior, edges, or vertices
     */
    @JvmStatic
    public fun Segment.intersects(box: Box): Boolean {
        return nativeSegmentBoxIntersects(
            segmentStartX = this.start.x,
            segmentStartY = this.start.y,
            segmentEndX = this.end.x,
            segmentEndY = this.end.y,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
        )
    }

    /**
     * Returns true when a [Segment] intersects with a [Parallelogram] --- when this segment has at
     * least one point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Segment.intersects(parallelogram: Parallelogram): Boolean {
        return nativeSegmentParallelogramIntersects(
            segmentStartX = this.start.x,
            segmentStartY = this.start.y,
            segmentEndX = this.end.x,
            segmentEndY = this.end.y,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
        )
    }

    /**
     * Returns true when a [Segment] intersects with a [PartitionedMesh].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToSegment] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun Segment.intersects(mesh: PartitionedMesh, meshToSegment: AffineTransform): Boolean {
        return nativeMeshSegmentIntersects(
            nativeMeshAddress = mesh.getNativeAddress(),
            segmentStartX = this.start.x,
            segmentStartY = this.start.y,
            segmentEndX = this.end.x,
            segmentEndY = this.end.y,
            meshToSegmentA = meshToSegment.m00,
            meshToSegmentB = meshToSegment.m10,
            meshToSegmentC = meshToSegment.m20,
            meshToSegmentD = meshToSegment.m01,
            meshToSegmentE = meshToSegment.m11,
            meshToSegmentF = meshToSegment.m21,
        )
    }

    /**
     * Returns true when a [Triangle] intersects with [other] --- When this triangle has at least
     * one point in common with [other]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Triangle.intersects(other: Triangle): Boolean {
        // Return true without calling the native code when this [Triangle] and [other] are equal
        // ---
        // i.e. have same corners.
        if (this == other) return true
        return nativeTriangleTriangleIntersects(
            triangle1P0X = this.p0.x,
            triangle1P0Y = this.p0.y,
            triangle1P1X = this.p1.x,
            triangle1P1Y = this.p1.y,
            triangle1P2X = this.p2.x,
            triangle1P2Y = this.p2.y,
            triangle2P0X = other.p0.x,
            triangle2P0Y = other.p0.y,
            triangle2P1X = other.p1.x,
            triangle2P1Y = other.p1.y,
            triangle2P2X = other.p2.x,
            triangle2P2Y = other.p2.y,
        )
    }

    /**
     * Returns true when a [Triangle] intersects with a [Box] --- When this triangle has at least
     * one point in common with the [Box]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Triangle.intersects(box: Box): Boolean {
        return nativeTriangleBoxIntersects(
            triangleP0X = this.p0.x,
            triangleP0Y = this.p0.y,
            triangleP1X = this.p1.x,
            triangleP1Y = this.p1.y,
            triangleP2X = this.p2.x,
            triangleP2Y = this.p2.y,
            boxXMin = box.xMin,
            boxYMin = box.yMin,
            boxXMax = box.xMax,
            boxYMax = box.yMax,
        )
    }

    /**
     * Returns true when a [Triangle] intersects with a [Parallelogram] --- When this triangle has
     * at least one point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Triangle.intersects(parallelogram: Parallelogram): Boolean {
        return nativeTriangleParallelogramIntersects(
            triangleP0X = this.p0.x,
            triangleP0Y = this.p0.y,
            triangleP1X = this.p1.x,
            triangleP1Y = this.p1.y,
            triangleP2X = this.p2.x,
            triangleP2Y = this.p2.y,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
        )
    }

    /**
     * Returns true when a [Triangle] intersects with a [PartitionedMesh].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToTriangle] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun Triangle.intersects(
        mesh: PartitionedMesh,
        meshToTriangle: AffineTransform
    ): Boolean {
        return nativeMeshTriangleIntersects(
            nativeMeshAddress = mesh.getNativeAddress(),
            triangleP0X = this.p0.x,
            triangleP0Y = this.p0.y,
            triangleP1X = this.p1.x,
            triangleP1Y = this.p1.y,
            triangleP2X = this.p2.x,
            triangleP2Y = this.p2.y,
            meshToTriangleA = meshToTriangle.m00,
            meshToTriangleB = meshToTriangle.m10,
            meshToTriangleC = meshToTriangle.m20,
            meshToTriangleD = meshToTriangle.m01,
            meshToTriangleE = meshToTriangle.m11,
            meshToTriangleF = meshToTriangle.m21,
        )
    }

    /**
     * Returns true when a [Box] intersects with [other] --- When it has at least one point in
     * common with [other]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Box.intersects(other: Box): Boolean {
        // Return true without calling the native code when this [Box] and [other] are equal ---
        // i.e. have same [xMin], [yMin], [xMax] and [yMax].
        if (this == other) return true
        return nativeBoxBoxIntersects(
            box1XMin = this.xMin,
            box1YMin = this.yMin,
            box1XMax = this.xMax,
            box1YMax = this.yMax,
            box2XMin = other.xMin,
            box2YMin = other.yMin,
            box2XMax = other.xMax,
            box2YMax = other.yMax,
        )
    }

    /**
     * Returns true when a [Box] intersects with a [Parallelogram] --- When it has at least one
     * point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Box.intersects(parallelogram: Parallelogram): Boolean {
        return nativeBoxParallelogramIntersects(
            boxXMin = this.xMin,
            boxYMin = this.yMin,
            boxXMax = this.xMax,
            boxYMax = this.yMax,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
        )
    }

    /**
     * Returns true when a [Box] intersects with a [PartitionedMesh].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToBox] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun Box.intersects(mesh: PartitionedMesh, meshToBox: AffineTransform): Boolean {
        return nativeMeshBoxIntersects(
            nativeMeshAddress = mesh.getNativeAddress(),
            boxXMin = this.xMin,
            boxYMin = this.yMin,
            boxXMax = this.xMax,
            boxYMax = this.yMax,
            meshToBoxA = meshToBox.m00,
            meshToBoxB = meshToBox.m10,
            meshToBoxC = meshToBox.m20,
            meshToBoxD = meshToBox.m01,
            meshToBoxE = meshToBox.m11,
            meshToBoxF = meshToBox.m21,
        )
    }

    /**
     * Returns true when a [Parallelogram] intersects with [other] --- When it has at least one
     * point in common with [other]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Parallelogram.intersects(other: Parallelogram): Boolean {
        // Return true without calling the native code when this [Parallelogram] and [other] are
        // equal
        // --- i.e.
        // when they have same parameters like [center], [width], [height], [rotation] and
        // [shearFactor].
        if (this == other) return true
        return nativeParallelogramParallelogramIntersects(
            parallelogram1CenterX = this.center.x,
            parallelogram1CenterY = this.center.y,
            parallelogram1Width = this.width,
            parallelogram1Height = this.height,
            parallelogram1AngleInRadian = this.rotation,
            parallelogram1ShearFactor = this.shearFactor,
            parallelogram2CenterX = other.center.x,
            parallelogram2CenterY = other.center.y,
            parallelogram2Width = other.width,
            parallelogram2Height = other.height,
            parallelogram2AngleInRadian = other.rotation,
            parallelogram2ShearFactor = other.shearFactor,
        )
    }

    /**
     * RReturns true when a [Parallelogram] intersects with a [PartitionedMesh].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToParallelogram] transform as an argument. This transform maps from the
     * [PartitionedMesh]'s coordinate space to the coordinate space that the intersection should be
     * checked in.
     */
    @JvmStatic
    public fun Parallelogram.intersects(
        mesh: PartitionedMesh,
        meshToParallelogram: AffineTransform,
    ): Boolean {
        return nativeMeshParallelogramIntersects(
            nativeMeshAddress = mesh.getNativeAddress(),
            parallelogramCenterX = this.center.x,
            parallelogramCenterY = this.center.y,
            parallelogramWidth = this.width,
            parallelogramHeight = this.height,
            parallelogramAngleInRadian = this.rotation,
            parallelogramShearFactor = this.shearFactor,
            meshToParallelogramA = meshToParallelogram.m00,
            meshToParallelogramB = meshToParallelogram.m10,
            meshToParallelogramC = meshToParallelogram.m20,
            meshToParallelogramD = meshToParallelogram.m01,
            meshToParallelogramE = meshToParallelogram.m11,
            meshToParallelogramF = meshToParallelogram.m21,
        )
    }

    /**
     * Returns true when a [PartitionedMesh] intersects with a [PartitionedMesh].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes two
     * [AffineTransform] objects: [thisToCommonTransForm] and [otherToCommonTransform]. These
     * transforms map from the respective [PartitionedMesh]s' coordinate spaces to the common
     * coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(
        other: PartitionedMesh,
        thisToCommonTransForm: AffineTransform,
        otherToCommonTransform: AffineTransform,
    ): Boolean {
        return nativeMeshModeledShapeIntersects(
            thisModeledShapeAddress = this.getNativeAddress(),
            otherModeledShapeAddress = other.getNativeAddress(),
            thisToCommonTransformA = thisToCommonTransForm.m00,
            thisToCommonTransformB = thisToCommonTransForm.m10,
            thisToCommonTransformC = thisToCommonTransForm.m20,
            thisToCommonTransformD = thisToCommonTransForm.m01,
            thisToCommonTransformE = thisToCommonTransForm.m11,
            thisToCommonTransformF = thisToCommonTransForm.m21,
            otherToCommonTransformA = otherToCommonTransform.m00,
            otherToCommonTransformB = otherToCommonTransform.m10,
            otherToCommonTransformC = otherToCommonTransform.m20,
            otherToCommonTransformD = otherToCommonTransform.m01,
            otherToCommonTransformE = otherToCommonTransform.m11,
            otherToCommonTransformF = otherToCommonTransform.m21,
        )
    }

    /**
     * Returns true when the [Segment] intersects with a point (a [Vec]). All points on the segment,
     * including endpoints, intersect with the segment.
     */
    @JvmStatic public fun Segment.intersects(point: Vec): Boolean = point.intersects(this)

    /**
     * Returns true when the [Triangle] intersects with a point (a [Vec]). All points on the
     * boundary of the triangle (including its vertices) and in the interior of the triangle
     * intersect with it.
     */
    @JvmStatic public fun Triangle.intersects(point: Vec): Boolean = point.intersects(this)

    /**
     * Returns true when a [Triangle] intersects with a [Segment] --- when the [segment] has at
     * least one point in common with the [Triangle]'s interior, edges, or vertices.
     */
    @JvmStatic public fun Triangle.intersects(segment: Segment): Boolean = segment.intersects(this)

    /**
     * Returns true when the [Parallelogram] intersects with a point (a [Vec]). All points on the
     * boundary of the parallelogram (including its vertices) and in the interior of the
     * parallelogram intersect with it.
     */
    @JvmStatic public fun Parallelogram.intersects(point: Vec): Boolean = point.intersects(this)

    /**
     * Returns true when a [Parallelogram] intersects with a [Segment] --- when the [segment] has at
     * least one point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Parallelogram.intersects(segment: Segment): Boolean = segment.intersects(this)

    /**
     * Returns true when a [Parallelogram] intersects with a [Triangle] --- When the [triangle] has
     * at least one point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic
    public fun Parallelogram.intersects(triangle: Triangle): Boolean = triangle.intersects(this)

    /**
     * Returns true when a [Parallelogram] intersects with a [Box] --- When the [box] has at least
     * one point in common with the [Parallelogram]'s interior, edges, or vertices.
     */
    @JvmStatic public fun Parallelogram.intersects(box: Box): Boolean = box.intersects(this)

    /**
     * Returns true when the [Box] intersects with a point (a [Vec]). All points on the boundary of
     * the box (including its vertices) and in the interior of the box intersect with it.
     */
    @JvmStatic public fun Box.intersects(point: Vec): Boolean = point.intersects(this)

    /**
     * Returns true when a [Box] intersects with a [Segment] --- when the [segment] has at least one
     * point in common with the [Box]'s interior, edges, or vertices
     */
    @JvmStatic public fun Box.intersects(segment: Segment): Boolean = segment.intersects(this)

    /**
     * Returns true when a [Box] intersects with a [Triangle] --- When the [triangle] has at least
     * one point in common with the [Box]'s interior, edges, or vertices.
     */
    @JvmStatic public fun Box.intersects(triangle: Triangle): Boolean = triangle.intersects(this)

    /**
     * Returns true when the [PartitionedMesh] intersects with [point]. [meshToPoint] transforms the
     * coordinate space of [mesh] to the coordinate space that the intersection should be checked in
     * (that of the [point]). All points along the boundary of the [mesh] and the [mesh]s interior
     * are considered for intersection.
     *
     * Performance note: it is expensive to apply a transform to a mesh. To avoid unnecessary
     * calculations, the inverse of [meshToPoint] is used to perform the mathematically equivalent
     * intersection of the point in [mesh]’s object coordinates.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(point: Vec, meshToPoint: AffineTransform): Boolean =
        point.intersects(this, meshToPoint)

    /**
     * Returns true when a [PartitionedMesh] intersects with a [Segment].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToSegment] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(
        segment: Segment,
        meshToSegment: AffineTransform
    ): Boolean = segment.intersects(this, meshToSegment)

    /**
     * Returns true when a [PartitionedMesh] intersects with a [Triangle].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToTriangle] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(
        triangle: Triangle,
        meshToTriangle: AffineTransform,
    ): Boolean = triangle.intersects(this, meshToTriangle)

    /**
     * Returns true when a [PartitionedMesh] intersects with a [Box].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToBox] transform as an argument. This transform maps from the [PartitionedMesh]'s
     * coordinate space to the coordinate space that the intersection should be checked in.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(box: Box, meshToBox: AffineTransform): Boolean =
        box.intersects(this, meshToBox)

    /**
     * Returns true when a [PartitionedMesh] intersects with a [Parallelogram].
     *
     * Note that, because it is expensive to apply a transform to a mesh, this method takes a
     * [meshToParallelogram] transform as an argument. This transform maps from the
     * [PartitionedMesh]'s coordinate space to the coordinate space that the intersection should be
     * checked in.
     */
    @JvmStatic
    public fun PartitionedMesh.intersects(
        parallelogram: Parallelogram,
        meshToParallelogram: AffineTransform,
    ): Boolean = parallelogram.intersects(this, meshToParallelogram)

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeVecSegmentIntersects(
        vecX: Float,
        vecY: Float,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeVecTriangleIntersects(
        vecX: Float,
        vecY: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeVecParallelogramIntersects(
        vecX: Float,
        vecY: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeVecBoxIntersects(
        vecX: Float,
        vecY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeSegmentSegmentIntersects(
        segment1StartX: Float,
        segment1StartY: Float,
        segment1EndX: Float,
        segment1EndY: Float,
        segment2StartX: Float,
        segment2StartY: Float,
        segment2EndX: Float,
        segment2EndY: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeSegmentTriangleIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeSegmentBoxIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeSegmentParallelogramIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeTriangleTriangleIntersects(
        triangle1P0X: Float,
        triangle1P0Y: Float,
        triangle1P1X: Float,
        triangle1P1Y: Float,
        triangle1P2X: Float,
        triangle1P2Y: Float,
        triangle2P0X: Float,
        triangle2P0Y: Float,
        triangle2P1X: Float,
        triangle2P1Y: Float,
        triangle2P2X: Float,
        triangle2P2Y: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeTriangleBoxIntersects(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeTriangleParallelogramIntersects(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeBoxBoxIntersects(
        box1XMin: Float,
        box1YMin: Float,
        box1XMax: Float,
        box1YMax: Float,
        box2XMin: Float,
        box2YMin: Float,
        box2XMax: Float,
        box2YMax: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeBoxParallelogramIntersects(
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeParallelogramParallelogramIntersects(
        parallelogram1CenterX: Float,
        parallelogram1CenterY: Float,
        parallelogram1Width: Float,
        parallelogram1Height: Float,
        parallelogram1AngleInRadian: Float,
        parallelogram1ShearFactor: Float,
        parallelogram2CenterX: Float,
        parallelogram2CenterY: Float,
        parallelogram2Width: Float,
        parallelogram2Height: Float,
        parallelogram2AngleInRadian: Float,
        parallelogram2ShearFactor: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshVecIntersects(
        nativeMeshAddress: Long,
        vecX: Float,
        vecY: Float,
        meshToVecA: Float,
        meshToVecB: Float,
        meshToVecC: Float,
        meshToVecD: Float,
        meshToVecE: Float,
        meshToVecF: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshSegmentIntersects(
        nativeMeshAddress: Long,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        meshToSegmentA: Float,
        meshToSegmentB: Float,
        meshToSegmentC: Float,
        meshToSegmentD: Float,
        meshToSegmentE: Float,
        meshToSegmentF: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshTriangleIntersects(
        nativeMeshAddress: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        meshToTriangleA: Float,
        meshToTriangleB: Float,
        meshToTriangleC: Float,
        meshToTriangleD: Float,
        meshToTriangleE: Float,
        meshToTriangleF: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshBoxIntersects(
        nativeMeshAddress: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        meshToBoxA: Float,
        meshToBoxB: Float,
        meshToBoxC: Float,
        meshToBoxD: Float,
        meshToBoxE: Float,
        meshToBoxF: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshParallelogramIntersects(
        nativeMeshAddress: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        meshToParallelogramA: Float,
        meshToParallelogramB: Float,
        meshToParallelogramC: Float,
        meshToParallelogramD: Float,
        meshToParallelogramE: Float,
        meshToParallelogramF: Float,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeMeshModeledShapeIntersects(
        thisModeledShapeAddress: Long,
        otherModeledShapeAddress: Long,
        thisToCommonTransformA: Float,
        thisToCommonTransformB: Float,
        thisToCommonTransformC: Float,
        thisToCommonTransformD: Float,
        thisToCommonTransformE: Float,
        thisToCommonTransformF: Float,
        otherToCommonTransformA: Float,
        otherToCommonTransformB: Float,
        otherToCommonTransformC: Float,
        otherToCommonTransformD: Float,
        otherToCommonTransformE: Float,
        otherToCommonTransformF: Float,
    ): Boolean
}
