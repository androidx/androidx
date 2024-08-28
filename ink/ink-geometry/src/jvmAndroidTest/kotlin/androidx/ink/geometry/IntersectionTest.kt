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

import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntersectionTest {

    @Test
    fun intersects_whenSamePoint_returnsTrue() {
        val point = ImmutableVec(1f, 2f)
        val other = ImmutableVec(1f, 2f)

        assertThat(point.intersects(point)).isTrue()
        assertThat(point.intersects(other)).isTrue()
    }

    @Test
    fun intersects_whenDifferentPoints_returnsFalse() {
        val point = ImmutableVec(1f, 2f)
        val other = ImmutableVec(3f, 4f)

        assertThat(point.intersects(other)).isFalse()
    }

    @Test
    fun intersects_whenPointSegmentIntersects_returnsTrue() {
        val startPoint = ImmutableVec(3f, 2f)
        val endPoint = ImmutableVec(9f, 5f)
        val midPoint = MutableVec(5f, 3f)
        val segment =
            ImmutableSegment(
                start = ImmutableVec(startPoint.x, startPoint.y),
                end = ImmutableVec(endPoint.x, endPoint.y),
            )

        assertThat(startPoint.intersects(segment)).isTrue()
        assertThat(endPoint.intersects(segment)).isTrue()
        assertThat(midPoint.intersects(segment)).isTrue()
        assertThat(segment.intersects(startPoint)).isTrue()
        assertThat(segment.intersects(endPoint)).isTrue()
        assertThat(segment.intersects(midPoint)).isTrue()
    }

    @Test
    fun intersects_whenPointSegmentDoesNotIntersect_returnsFalse() {
        val segment = ImmutableSegment(start = ImmutableVec(3f, 2f), end = ImmutableVec(9f, 5f))
        val lowerPoint = ImmutableVec(2f, 1f)
        val higherPoint = ImmutableVec(11f, 12f)
        val nearPoint = MutableVec(2f, 1.7f)

        assertThat(lowerPoint.intersects(segment)).isFalse()
        assertThat(higherPoint.intersects(segment)).isFalse()
        assertThat(nearPoint.intersects(segment)).isFalse()
        assertThat(segment.intersects(lowerPoint)).isFalse()
        assertThat(segment.intersects(higherPoint)).isFalse()
        assertThat(segment.intersects(nearPoint)).isFalse()
    }

    @Test
    fun intersects_whenPointTriangleIntersects_returnsTrue() {
        val p0 = ImmutableVec(2f, 1f)
        val p1 = ImmutableVec(10f, 0f)
        val p2 = ImmutableVec(6f, 5f)
        val p0p1midpoint = ImmutableVec(6f, 0.5f)
        val p1p2midpoint = MutableVec(8f, 2.5f)
        val p2p0midpoint = ImmutableVec(4f, 3f)
        val interiorPoint1 = ImmutableVec(7f, 2f)
        val interiorPoint2 = MutableVec(2.1f, 1f)
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(p0.x, p0.y),
                p1 = ImmutableVec(p1.x, p1.y),
                p2 = ImmutableVec(p2.x, p2.y),
            )

        // A triangle trivially intersects with vertices
        assertThat(p0.intersects(triangle)).isTrue()
        assertThat(p1.intersects(triangle)).isTrue()
        assertThat(p2.intersects(triangle)).isTrue()
        assertThat(triangle.intersects(p0)).isTrue()
        assertThat(triangle.intersects(p1)).isTrue()
        assertThat(triangle.intersects(p2)).isTrue()
        // A triangle intersects with points on its edges
        assertThat(p0p1midpoint.intersects(triangle)).isTrue()
        assertThat(p1p2midpoint.intersects(triangle)).isTrue()
        assertThat(p2p0midpoint.intersects(triangle)).isTrue()
        assertThat(triangle.intersects(p0p1midpoint)).isTrue()
        assertThat(triangle.intersects(p1p2midpoint)).isTrue()
        assertThat(triangle.intersects(p2p0midpoint)).isTrue()
        // A triangle intersects with interior points
        assertThat(interiorPoint1.intersects(triangle)).isTrue()
        assertThat(interiorPoint2.intersects(triangle)).isTrue()
        assertThat(triangle.intersects(interiorPoint1)).isTrue()
        assertThat(triangle.intersects(interiorPoint2)).isTrue()
    }

    @Test
    fun intersects_whenPointTriangleDoesNotIntersect_returnsFalse() {
        val p0 = ImmutableVec(1.22f, 0.97f)
        val p1 = ImmutableVec(10f, 0f)
        val p2 = ImmutableVec(0f, 10f)
        val leftPoint = ImmutableVec(-0.2f, 0f)
        val bottomPoint = MutableVec(0f, -0.1f)
        val farPoint1 = ImmutableVec(107f, 100f)
        val farPoint2 = MutableVec(-12f, -20f)
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(p0.x, p0.y),
                p1 = ImmutableVec(p1.x, p1.y),
                p2 = ImmutableVec(p2.x, p2.y),
            )

        assertThat(leftPoint.intersects(triangle)).isFalse()
        assertThat(bottomPoint.intersects(triangle)).isFalse()
        assertThat(farPoint1.intersects(triangle)).isFalse()
        assertThat(farPoint2.intersects(triangle)).isFalse()
        assertThat(triangle.intersects(leftPoint)).isFalse()
        assertThat(triangle.intersects(bottomPoint)).isFalse()
        assertThat(triangle.intersects(farPoint1)).isFalse()
        assertThat(triangle.intersects(farPoint2)).isFalse()
    }

    /**
     * Verifies that a skewed parallelogram with horizontal axis aligned has the correct
     * intersection with different vertices
     *
     * ```
     *     ----
     *    /   /
     *   /   /
     *   ----
     * ```
     */
    @Test
    fun intersects_whenPointParallelogramIntersects_returnsTrue() {
        val center = ImmutableVec(3f, 2f)
        val width = 4f
        val height = 6f
        val shearFactor = 1f // = cotangent(PI/4), represents a 45-degree shear
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(center.x, center.y),
                width = width,
                height = height,
                rotation = Angle.ZERO,
                shearFactor = shearFactor,
            )
        val vertex0 = ImmutableVec(1f, 0f)
        val vertex1 = MutableVec(3f, 0f)
        val vertex2 = ImmutableVec(3f, 2f)
        val vertex3 = MutableVec(5f, 2f)
        val midPoint0 = ImmutableVec(2f, 0f)
        val midPoint1 = MutableVec(2f, 1f)
        val midPoint2 = ImmutableVec(4f, 2f)
        val midPoint3 = MutableVec(4f, 1f)
        val interiorPoint = ImmutableVec(3f, 1f)

        assertThat(vertex0.intersects(parallelogram)).isTrue()
        assertThat(vertex1.intersects(parallelogram)).isTrue()
        assertThat(vertex2.intersects(parallelogram)).isTrue()
        assertThat(vertex3.intersects(parallelogram)).isTrue()
        assertThat(midPoint0.intersects(parallelogram)).isTrue()
        assertThat(midPoint1.intersects(parallelogram)).isTrue()
        assertThat(midPoint2.intersects(parallelogram)).isTrue()
        assertThat(midPoint3.intersects(parallelogram)).isTrue()
        assertThat(interiorPoint.intersects(parallelogram)).isTrue()
        assertThat(parallelogram.intersects(vertex0)).isTrue()
        assertThat(parallelogram.intersects(vertex1)).isTrue()
        assertThat(parallelogram.intersects(vertex2)).isTrue()
        assertThat(parallelogram.intersects(vertex3)).isTrue()
        assertThat(parallelogram.intersects(midPoint0)).isTrue()
        assertThat(parallelogram.intersects(midPoint1)).isTrue()
        assertThat(parallelogram.intersects(midPoint2)).isTrue()
        assertThat(parallelogram.intersects(midPoint3)).isTrue()
        assertThat(parallelogram.intersects(interiorPoint)).isTrue()
    }

    /**
     * Verifies that the original vertices, (11, 1), (9, 1), (9, -1), (11, -1) of a parallelogram
     * rotated by 45 degrees no longer intersect with it.
     *
     * ```
     *    /\
     *   /  \
     *   \  /
     *    \/
     * ```
     */
    @Test
    fun intersects_whenPointParallelogramDoesNotIntersect_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 0f),
                width = 1f,
                height = 1f,
                rotation = Angle.HALF_TURN_RADIANS / 4f,
                shearFactor = 0f,
            )
        val vertex0 = ImmutableVec(11f, 1f)
        val vertex1 = MutableVec(9f, 1f)
        val vertex2 = ImmutableVec(9f, -1f)
        val vertex3 = MutableVec(11f, -1f)
        val farPoint = ImmutableVec(100f, 100f)

        assertThat(vertex0.intersects(parallelogram)).isFalse()
        assertThat(vertex1.intersects(parallelogram)).isFalse()
        assertThat(vertex2.intersects(parallelogram)).isFalse()
        assertThat(vertex3.intersects(parallelogram)).isFalse()
        assertThat(farPoint.intersects(parallelogram)).isFalse()
        assertThat(parallelogram.intersects(vertex0)).isFalse()
        assertThat(parallelogram.intersects(vertex1)).isFalse()
        assertThat(parallelogram.intersects(vertex2)).isFalse()
        assertThat(parallelogram.intersects(vertex3)).isFalse()
        assertThat(parallelogram.intersects(farPoint)).isFalse()
    }

    @Test
    fun intersects_whenPointBoxIntersects_returnsTrue() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(3.5f, 10.9f), ImmutableVec(2.5f, 1.1f))
        val vertex0 = ImmutableVec(3.5f, 10.9f)
        val vertex1 = MutableVec(3.5f, 1.1f)
        val vertex2 = ImmutableVec(2.5f, 10.9f)
        val vertex3 = MutableVec(2.5f, 1.1f)
        val midPoint0 = ImmutableVec(3f, 10.9f)
        val midPoint1 = MutableVec(3f, 1.1f)
        val midPoint2 = ImmutableVec(3.5f, 6f)
        val midPoint3 = MutableVec(2.5f, 6f)
        val interiorPoint = ImmutableVec(3.1f, 6.4f)

        assertThat(vertex0.intersects(rect)).isTrue()
        assertThat(vertex1.intersects(rect)).isTrue()
        assertThat(vertex2.intersects(rect)).isTrue()
        assertThat(vertex3.intersects(rect)).isTrue()
        assertThat(midPoint0.intersects(rect)).isTrue()
        assertThat(midPoint1.intersects(rect)).isTrue()
        assertThat(midPoint2.intersects(rect)).isTrue()
        assertThat(midPoint3.intersects(rect)).isTrue()
        assertThat(interiorPoint.intersects(rect)).isTrue()
        assertThat(rect.intersects(vertex0)).isTrue()
        assertThat(rect.intersects(vertex1)).isTrue()
        assertThat(rect.intersects(vertex2)).isTrue()
        assertThat(rect.intersects(vertex3)).isTrue()
        assertThat(rect.intersects(midPoint0)).isTrue()
        assertThat(rect.intersects(midPoint1)).isTrue()
        assertThat(rect.intersects(midPoint2)).isTrue()
        assertThat(rect.intersects(midPoint3)).isTrue()
        assertThat(rect.intersects(interiorPoint)).isTrue()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for [PartitionedMesh] and [Point].
     *
     * For this test, the [PartitionedMesh] consists of triangulation of a straight line [Stroke]
     * from (10, 3) to (20, 5), consisting of 126 triangles. `intersectingPoint` intersects with at
     * least one of those triangles, while `nonIntersectingPoint` does not intersect with any
     * triangle.
     */
    @Test
    fun intersects_forPointAndPartitionedMesh_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingPoint = MutableVec(15f, 4f)
        val nonIntersectingPoint = ImmutableVec(100f, 200f)

        assertThat(mesh.intersects(intersectingPoint, SCALE_TRANSFORM)).isTrue()
        assertThat(mesh.intersects(nonIntersectingPoint, SCALE_TRANSFORM)).isFalse()
        assertThat(intersectingPoint.intersects(mesh, AffineTransform.IDENTITY)).isTrue()
        assertThat(nonIntersectingPoint.intersects(mesh, AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun intersects_whenPointBoxDoesNotIntersect_returnsFalse() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(-1f, 3.2f), ImmutableVec(7f, 11.8f))
        val closeExteriorPoint = ImmutableVec(7.1f, 3.2f)
        val farExteriorPoint = ImmutableVec(-10f, -100f)

        assertThat(closeExteriorPoint.intersects(rect)).isFalse()
        assertThat(farExteriorPoint.intersects(rect)).isFalse()
        assertThat(rect.intersects(closeExteriorPoint)).isFalse()
        assertThat(rect.intersects(farExteriorPoint)).isFalse()
    }

    @Test
    fun intersects_forEqualSegments_returnsTrue() {
        val segment1 = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val segment2 = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))

        assertThat(segment1.intersects(segment1)).isTrue()
        assertThat(segment1.intersects(segment2)).isTrue()
    }

    @Test
    fun intersects_whenSegmentSegmentIntersects_returnsTrue() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val startPointIntersection =
            ImmutableSegment(start = ImmutableVec(-3f, 4f), end = ImmutableVec(1f, 3.2f))
        val endIntersection =
            ImmutableSegment(start = ImmutableVec(7f, 4.64f), end = ImmutableVec(9f, 5f))
        val middleIntersection =
            ImmutableSegment(start = ImmutableVec(0f, -10f), end = ImmutableVec(0f, 10f))

        assertThat(segment.intersects(startPointIntersection)).isTrue()
        assertThat(segment.intersects(endIntersection)).isTrue()
        assertThat(segment.intersects(middleIntersection)).isTrue()
        assertThat(startPointIntersection.intersects(segment)).isTrue()
        assertThat(endIntersection.intersects(segment)).isTrue()
        assertThat(middleIntersection.intersects(segment)).isTrue()
    }

    @Test
    fun intersects_whenSegmentSegmentDoesNotIntersect_returnsFalse() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val closeSegment =
            ImmutableSegment(start = ImmutableVec(-0.9f, 3.2f), end = ImmutableVec(-3f, -4f))
        val farSegment =
            ImmutableSegment(start = ImmutableVec(100f, 2f), end = ImmutableVec(10f, 5f))

        assertThat(segment.intersects(closeSegment)).isFalse()
        assertThat(segment.intersects(farSegment)).isFalse()
        assertThat(closeSegment.intersects(segment)).isFalse()
        assertThat(farSegment.intersects(segment)).isFalse()
    }

    @Test
    fun intersects_whenSegmentTriangleIntersects_returnsTrue() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val triangleWithCommonP0 =
            ImmutableTriangle(
                p0 = ImmutableVec(-1f, 3.2f),
                p1 = ImmutableVec(-1f, 10f),
                p2 = ImmutableVec(4f, 7.1f),
            )
        val triangleWithCommonP1 =
            ImmutableTriangle(
                p0 = ImmutableVec(9f, 4f),
                p1 = ImmutableVec(9f, 5f),
                p2 = ImmutableVec(11f, 4.5f),
            )
        val containingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(-100f, -100f),
                p1 = ImmutableVec(-100f, 100f),
                p2 = ImmutableVec(50f, 50f),
            )

        assertThat(segment.intersects(triangleWithCommonP0)).isTrue()
        assertThat(segment.intersects(triangleWithCommonP1)).isTrue()
        assertThat(segment.intersects(containingTriangle)).isTrue()
        assertThat(triangleWithCommonP0.intersects(segment)).isTrue()
        assertThat(triangleWithCommonP1.intersects(segment)).isTrue()
        assertThat(containingTriangle.intersects(segment)).isTrue()
    }

    @Test
    fun intersects_whenSegmentTriangleDoesNotIntersects_returnsFalse() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val closeTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(-0.9f, 3.2f),
                p1 = ImmutableVec(-3f, -4f),
                p2 = ImmutableVec(-2.3f, -10f),
            )
        val farTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 2f),
                p1 = ImmutableVec(100f, 5f),
                p2 = ImmutableVec(102f, 3f),
            )

        assertThat(segment.intersects(closeTriangle)).isFalse()
        assertThat(segment.intersects(farTriangle)).isFalse()
        assertThat(closeTriangle.intersects(segment)).isFalse()
        assertThat(farTriangle.intersects(segment)).isFalse()
    }

    @Test
    fun intersects_whenSegmentBoxIntersects_returnsTrue() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val rectWithCommonMinPoint =
            ImmutableBox.fromTwoPoints(ImmutableVec(-1f, 3.2f), ImmutableVec(-10f, 0f))
        val rectWithCommonMaxPoint =
            ImmutableBox.fromTwoPoints(ImmutableVec(9f, 5f), ImmutableVec(20f, 11.4f))
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(0f, 1f), ImmutableVec(8f, 21f))

        assertThat(segment.intersects(rectWithCommonMinPoint)).isTrue()
        assertThat(segment.intersects(rectWithCommonMaxPoint)).isTrue()
        assertThat(segment.intersects(intersectingBox)).isTrue()
        assertThat(rectWithCommonMinPoint.intersects(segment)).isTrue()
        assertThat(rectWithCommonMaxPoint.intersects(segment)).isTrue()
        assertThat(intersectingBox.intersects(segment)).isTrue()
    }

    @Test
    fun intersects_whenSegmentBoxDoesNotIntersect_returnsFalse() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val closeBox = ImmutableBox.fromTwoPoints(ImmutableVec(9.1f, 5f), ImmutableVec(10f, 6f))
        val farBox = ImmutableBox.fromTwoPoints(ImmutableVec(-10f, -2f), ImmutableVec(-21f, -8f))

        assertThat(segment.intersects(closeBox)).isFalse()
        assertThat(segment.intersects(farBox)).isFalse()
        assertThat(closeBox.intersects(segment)).isFalse()
        assertThat(farBox.intersects(segment)).isFalse()
    }

    @Test
    fun intersects_whenSegmentParallelogramIntersects_returnsTrue() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val parallelogramWithCommonVertex =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(1f, 6.2f),
                width = 4f,
                height = 6f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(4f, 4.1f),
                width = 4f,
                height = 6f,
                rotation = Angle.ZERO,
                shearFactor = 1f,
            )

        assertThat(segment.intersects(parallelogramWithCommonVertex)).isTrue()
        assertThat(segment.intersects(intersectingParallelogram)).isTrue()
        assertThat(parallelogramWithCommonVertex.intersects(segment)).isTrue()
        assertThat(intersectingParallelogram.intersects(segment)).isTrue()
    }

    @Test
    fun intersects_whenSegmentParallelogramDoesNotIntersect_returnsFalse() {
        val segment = ImmutableSegment(start = ImmutableVec(-1f, 3.2f), end = ImmutableVec(9f, 5f))
        val closeParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10.1f, 7f),
                width = 2f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val farParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(-100f, -103.1f),
                width = 4f,
                height = 7.2f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 1f,
            )

        assertThat(segment.intersects(closeParallelogram)).isFalse()
        assertThat(segment.intersects(farParallelogram)).isFalse()
        assertThat(closeParallelogram.intersects(segment)).isFalse()
        assertThat(farParallelogram.intersects(segment)).isFalse()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for [PartitionedMesh] and [Segment].
     *
     * For this test, the [PartitionedMesh] consists of triangulation of a straight line [Stroke]
     * from (10, 3) to (20, 5), consisting of 126 triangles. `intersectingSegment` intersects with
     * at least one of those triangles, while `nonIntersectingSegment` does not intersect with any
     * triangle.
     */
    @Test
    fun intersects_forSegmentAndPartitionedMesh_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingSegment =
            ImmutableSegment(start = ImmutableVec(14f, 3f), end = ImmutableVec(16f, 5f))
        val nonIntersectingSegment =
            ImmutableSegment(start = ImmutableVec(100f, 200f), end = ImmutableVec(300f, 400f))

        assertThat(mesh.intersects(intersectingSegment, SCALE_TRANSFORM)).isTrue()
        assertThat(mesh.intersects(nonIntersectingSegment, SCALE_TRANSFORM)).isFalse()
        assertThat(intersectingSegment.intersects(mesh, AffineTransform.IDENTITY)).isTrue()
        assertThat(nonIntersectingSegment.intersects(mesh, AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun intersects_forEqualTriangles_returnsTrue() {
        val triangle1 =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val triangle2 =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )

        assertThat(triangle1.intersects(triangle1)).isTrue()
        assertThat(triangle1.intersects(triangle2)).isTrue()
        assertThat(triangle2.intersects(triangle1)).isTrue()
    }

    @Test
    fun intersects_whenTriangleTriangleIntersects_returnsTrue() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val triangleWithCommonP0 =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(-10f, -31.6f),
                p2 = ImmutableVec(-4.2f, -10f),
            )
        val triangleWithCommonEdge =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 107.5f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val intersectingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(-1f, 16f),
                p1 = ImmutableVec(6f, 17f),
                p2 = ImmutableVec(10f, 0f),
            )

        assertThat(triangle.intersects(triangleWithCommonP0)).isTrue()
        assertThat(triangle.intersects(triangleWithCommonEdge)).isTrue()
        assertThat(triangle.intersects(intersectingTriangle)).isTrue()
        assertThat(triangleWithCommonP0.intersects(triangle)).isTrue()
        assertThat(triangleWithCommonEdge.intersects(triangle)).isTrue()
        assertThat(intersectingTriangle.intersects(triangle)).isTrue()
    }

    @Test
    fun intersects_whenTriangleTriangleDoesNotIntersect_returnsFalse() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val closeTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 0.9f),
                p1 = ImmutableVec(-10f, -29.3f),
                p2 = ImmutableVec(0f, -8f),
            )
        val farTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 2f),
                p1 = ImmutableVec(105f, 2f),
                p2 = ImmutableVec(102f, 4f),
            )

        assertThat(triangle.intersects(closeTriangle)).isFalse()
        assertThat(triangle.intersects(farTriangle)).isFalse()
        assertThat(closeTriangle.intersects(triangle)).isFalse()
        assertThat(farTriangle.intersects(triangle)).isFalse()
    }

    @Test
    fun intersects_whenTriangleBoxIntersects_returnsTrue() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val rectWithCommonP2 =
            ImmutableBox.fromTwoPoints(ImmutableVec(4.2f, 10f), ImmutableVec(7.9f, 19.2f))
        val rectWithCommonEdge =
            ImmutableBox.fromTwoPoints(ImmutableVec(-10f, 1f), ImmutableVec(0f, 31.6f))
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 20f), ImmutableVec(6.5f, 31.9f))

        assertThat(triangle.intersects(rectWithCommonP2)).isTrue()
        assertThat(triangle.intersects(rectWithCommonEdge)).isTrue()
        assertThat(triangle.intersects(intersectingBox)).isTrue()
        assertThat(rectWithCommonP2.intersects(triangle)).isTrue()
        assertThat(rectWithCommonEdge.intersects(triangle)).isTrue()
        assertThat(intersectingBox.intersects(triangle)).isTrue()
    }

    @Test
    fun intersects_whenTriangleBoxDoesNotIntersect_returnsFalse() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val closeBox = ImmutableBox.fromTwoPoints(ImmutableVec(0f, 0.9f), ImmutableVec(-51.1f, -2f))
        val farBox = ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        assertThat(triangle.intersects(closeBox)).isFalse()
        assertThat(triangle.intersects(farBox)).isFalse()
        assertThat(closeBox.intersects(triangle)).isFalse()
        assertThat(farBox.intersects(triangle)).isFalse()
    }

    @Test
    fun intersects_whenTriangleParallelogramIntersects_returnsTrue() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val parallelogramWithCommonP1 =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(1.5f, 32.6f),
                width = 3f,
                height = 2f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val parallelogramWithCommonEdge =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(-1f, 16.3f),
                width = 2f,
                height = 15.3f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(2.1f, 17.4f),
                width = 10f,
                height = 19.4f,
                rotation = Angle.ZERO,
                shearFactor = 1f,
            )

        assertThat(triangle.intersects(parallelogramWithCommonP1)).isTrue()
        assertThat(triangle.intersects(parallelogramWithCommonEdge)).isTrue()
        assertThat(triangle.intersects(intersectingParallelogram)).isTrue()
        assertThat(parallelogramWithCommonP1.intersects(triangle)).isTrue()
        assertThat(parallelogramWithCommonEdge.intersects(triangle)).isTrue()
        assertThat(intersectingParallelogram.intersects(triangle)).isTrue()
    }

    @Test
    fun intersects_whenTriangleParallelogramDoesNotIntersect_returnsFalse() {
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(0f, 31.6f),
                p2 = ImmutableVec(4.2f, 10f),
            )
        val closeParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(-5.1f, 2f),
                width = 10f,
                height = 13.2f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val farParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 0.6f,
                height = 2.3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 0f,
            )

        assertThat(triangle.intersects(closeParallelogram)).isFalse()
        assertThat(triangle.intersects(farParallelogram)).isFalse()
        assertThat(closeParallelogram.intersects(triangle)).isFalse()
        assertThat(farParallelogram.intersects(triangle)).isFalse()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for [PartitionedMesh] and [Triangle].
     *
     * For this test, the [PartitionedMesh] consists of triangulation of a straight line [Stroke]
     * from (10, 3) to (20, 5), consisting of 126 triangles. `intersectingTriangle` intersects with
     * at least one of those triangles, while `nonIntersectingTriangle` does not intersect with any
     * triangle.
     */
    @Test
    fun intersects_forTriangleAndPartitionedMesh_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(0f, 1f),
                p1 = ImmutableVec(10f, 3f),
                p2 = ImmutableVec(5f, 20f),
            )
        val nonIntersectingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 200f),
                p1 = ImmutableVec(300f, 400f),
                p2 = ImmutableVec(200f, 600f),
            )

        assertThat(mesh.intersects(intersectingTriangle, AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(nonIntersectingTriangle, SCALE_TRANSFORM)).isFalse()
        assertThat(intersectingTriangle.intersects(mesh, AffineTransform.IDENTITY)).isTrue()
        assertThat(nonIntersectingTriangle.intersects(mesh, SCALE_TRANSFORM)).isFalse()
    }

    @Test
    fun intersects_forEqualBoxs_returnsTrue() {
        val rect1 = ImmutableBox.fromTwoPoints(ImmutableVec(0f, 1f), ImmutableVec(31.6f, 10f))
        val rect2 = ImmutableBox.fromTwoPoints(ImmutableVec(0f, 1f), ImmutableVec(31.6f, 10f))

        assertThat(rect1.intersects(rect1)).isTrue()
        assertThat(rect1.intersects(rect2)).isTrue()
        assertThat(rect2.intersects(rect1)).isTrue()
    }

    @Test
    fun intersects_whenBoxBoxIntersects_returnsTrue() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 1f), ImmutableVec(31.6f, 10f))
        val rectWithCommonVertex =
            ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 1f), ImmutableVec(-3f, -6.5f))
        val rectWithCommonEdge =
            ImmutableBox.fromTwoPoints(ImmutableVec(31.6f, 5f), ImmutableVec(67.9f, 2f))
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(6.7f, 3f), ImmutableVec(20f, 100.2f))

        assertThat(rect.intersects(rectWithCommonVertex)).isTrue()
        assertThat(rect.intersects(rectWithCommonEdge)).isTrue()
        assertThat(rect.intersects(intersectingBox)).isTrue()
        assertThat(rectWithCommonVertex.intersects(rect)).isTrue()
        assertThat(rectWithCommonEdge.intersects(rect)).isTrue()
        assertThat(intersectingBox.intersects(rect)).isTrue()
    }

    @Test
    fun intersects_whenBoxBoxDoesNotIntersect_returnsFalse() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 1f), ImmutableVec(31.6f, 10f))
        val closeBox = ImmutableBox.fromTwoPoints(ImmutableVec(2f, 1f), ImmutableVec(-10f, -11f))
        val farBox = ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        assertThat(rect.intersects(closeBox)).isFalse()
        assertThat(rect.intersects(farBox)).isFalse()
        assertThat(closeBox.intersects(rect)).isFalse()
        assertThat(farBox.intersects(rect)).isFalse()
    }

    @Test
    fun intersects_whenBoxParallelogramIntersects_returnsTrue() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 1f), ImmutableVec(31.6f, 10f))
        val parallelogramWithCommonVertex =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(26.6f, 8f),
                width = 10f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val parallelogramWithCommonEdge =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 0f),
                width = 10f,
                height = 2f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 5f),
                width = 6f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 1f,
            )

        assertThat(rect.intersects(parallelogramWithCommonVertex)).isTrue()
        assertThat(rect.intersects(parallelogramWithCommonEdge)).isTrue()
        assertThat(rect.intersects(intersectingParallelogram)).isTrue()
        assertThat(parallelogramWithCommonVertex.intersects(rect)).isTrue()
        assertThat(parallelogramWithCommonEdge.intersects(rect)).isTrue()
        assertThat(intersectingParallelogram.intersects(rect)).isTrue()
    }

    @Test
    fun intersects_whenBoxParallelogramDoesNotIntersect_returnsFalse() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(2.1f, 1f), ImmutableVec(31.6f, 10f))
        val closeParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(0f, 1f),
                width = 4f,
                height = 10f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val farParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 0.6f,
                height = 2.3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 0f,
            )

        assertThat(rect.intersects(closeParallelogram)).isFalse()
        assertThat(rect.intersects(farParallelogram)).isFalse()
        assertThat(closeParallelogram.intersects(rect)).isFalse()
        assertThat(farParallelogram.intersects(rect)).isFalse()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for [PartitionedMesh] and [Box].
     *
     * For this test, the [PartitionedMesh] consists of triangulation of a straight line [Stroke]
     * from (10, 3) to (20, 5), consisting of 126 triangles. `intersectingBox` intersects with at
     * least one of those triangles, while `nonIntersectingBox` does not intersect with any
     * triangle.
     */
    @Test
    fun intersects_forBoxAndPartitionedMesh_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(15f, 4f), ImmutableVec(20f, 5f))
        val nonIntersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        assertThat(mesh.intersects(intersectingBox, AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(nonIntersectingBox, AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(nonIntersectingBox, SCALE_TRANSFORM)).isFalse()
        assertThat(intersectingBox.intersects(mesh, AffineTransform.IDENTITY)).isTrue()
        assertThat(nonIntersectingBox.intersects(mesh, AffineTransform.IDENTITY)).isFalse()
        assertThat(nonIntersectingBox.intersects(mesh, AffineTransform.IDENTITY)).isFalse()
        assertThat(nonIntersectingBox.intersects(mesh, SCALE_TRANSFORM)).isFalse()
    }

    @Test
    fun intersects_forEqualsParallelograms_returnsTrue() {
        val parallelogram1 =
            ImmutableParallelogram.fromCenterAndDimensions(
                center = ImmutableVec(0f, 1f),
                width = 4f,
                height = 10f,
            )
        val parallelogram2 =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(0f, 1f),
                width = 4f,
                height = 10f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )

        assertThat(parallelogram1.intersects(parallelogram1)).isTrue()
        assertThat(parallelogram1.intersects(parallelogram2)).isTrue()
        assertThat(parallelogram2.intersects(parallelogram1)).isTrue()
    }

    @Test
    fun intersects_whenParallelogramParallelogramIntersects_returnsTrue() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 20f),
                width = 6f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val parallelogramWithCommonVertex =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(6f, 16f),
                width = 2f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val parallelogramWithCommonEdge =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 30f),
                width = 200f,
                height = 16f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(10f, 20f),
                2.9f,
                2.1f,
                Angle.HALF_TURN_RADIANS / 4f,
                0f,
            )

        assertThat(parallelogram.intersects(parallelogramWithCommonVertex)).isTrue()
        assertThat(parallelogram.intersects(parallelogramWithCommonEdge)).isTrue()
        assertThat(parallelogram.intersects(intersectingParallelogram)).isTrue()
        assertThat(parallelogramWithCommonVertex.intersects(parallelogram)).isTrue()
        assertThat(parallelogramWithCommonEdge.intersects(parallelogram)).isTrue()
        assertThat(intersectingParallelogram.intersects(parallelogram)).isTrue()
    }

    @Test
    fun intersects_whenParallelogramParallelogramDoesNotIntersects_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 20f),
                width = 6f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val closeParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(0.9f, 20f),
                width = 12f,
                height = 4f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )
        val farParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 0.6f,
                height = 2.3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 0f,
            )

        assertThat(parallelogram.intersects(closeParallelogram)).isFalse()
        assertThat(parallelogram.intersects(farParallelogram)).isFalse()
        assertThat(closeParallelogram.intersects(parallelogram)).isFalse()
        assertThat(farParallelogram.intersects(parallelogram)).isFalse()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for [PartitionedMesh] and
     * [Parallelogram].
     *
     * For this test, the [PartitionedMesh] consists of triangulation of a straight line [Stroke]
     * from (10, 3) to (20, 5), consisting of 126 triangles. `intersectingParallelogram` intersects
     * with at least one of those triangles, while `nonIntersectingParallelogram` does not intersect
     * with any triangle.
     */
    @Test
    fun intersects_forParallelogramAndPartitionedMesh_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(
                center = ImmutableVec(15f, 4f),
                width = 3f,
                height = 2f,
            )
        val nonIntersectingParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 300f,
                height = 400f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 1f,
            )

        assertThat(mesh.intersects(intersectingParallelogram, AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(nonIntersectingParallelogram, AffineTransform.IDENTITY))
            .isFalse()
        assertThat(mesh.intersects(nonIntersectingParallelogram, SCALE_TRANSFORM)).isFalse()
        assertThat(intersectingParallelogram.intersects(mesh, AffineTransform.IDENTITY)).isTrue()
        assertThat(nonIntersectingParallelogram.intersects(mesh, AffineTransform.IDENTITY))
            .isFalse()
        assertThat(nonIntersectingParallelogram.intersects(mesh, SCALE_TRANSFORM)).isFalse()
    }

    /**
     * Verifies that [intersects] calls the correct JNI method for two [PartitionedMesh]s.
     *
     * For this test, `mesh` consists of triangulation of a straight line [Stroke] from (10, 3) to
     * (20, 5), consisting of 126 triangles. `intersectingShape` consists of triangulation of a
     * straight line [Stroke] from (14, 3) to (14, 5), and intersects with at least one of these
     * triangles. `nonIntersectingShape` consists of triangulation of a straight line [Stroke] from
     * (100, 3) to (200, 5), and does not intersect with any triangle.
     */
    @Test
    fun intersects_forTwoPartitionedMeshes_callsJniAndReturnsBool() {
        val mesh = buildTestStrokeShape()
        val intersectingShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(14f, 3f, 14f, 5f)).asImmutable(),
                )
                .shape
        val nonIntersectingShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(100f, 3f, 200f, 5f)).asImmutable(),
                )
                .shape

        assertThat(
                mesh.intersects(
                    intersectingShape,
                    AffineTransform.IDENTITY,
                    AffineTransform.IDENTITY
                )
            )
            .isTrue()
        assertThat(
                mesh.intersects(
                    nonIntersectingShape,
                    AffineTransform.IDENTITY,
                    AffineTransform.IDENTITY
                )
            )
            .isFalse()
        assertThat(
                intersectingShape.intersects(
                    mesh,
                    AffineTransform.IDENTITY,
                    AffineTransform.IDENTITY
                )
            )
            .isTrue()
        assertThat(
                nonIntersectingShape.intersects(
                    mesh,
                    AffineTransform.IDENTITY,
                    AffineTransform.IDENTITY
                )
            )
            .isFalse()
        assertThat(nonIntersectingShape.intersects(mesh, AffineTransform.IDENTITY, SCALE_TRANSFORM))
            .isFalse()
        assertThat(nonIntersectingShape.intersects(mesh, SCALE_TRANSFORM, AffineTransform.IDENTITY))
            .isFalse()
        assertThat(nonIntersectingShape.intersects(mesh, SCALE_TRANSFORM, SCALE_TRANSFORM))
            .isFalse()
    }

    private fun buildTestStrokeShape(): PartitionedMesh {
        return Stroke(
                TEST_BRUSH,
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable(),
            )
            .shape
    }

    companion object {
        private val SCALE_TRANSFORM = ImmutableAffineTransform(2f, 0f, 0f, 0f, 5f, 0f)

        private val TEST_BRUSH =
            Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
    }
}
