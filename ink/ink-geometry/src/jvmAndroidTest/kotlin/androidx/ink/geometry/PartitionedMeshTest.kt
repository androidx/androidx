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
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PartitionedMeshTest {

    @Test
    fun computeBoundingBox_shouldBeEmpty() {
        val partitionedMesh = PartitionedMesh()

        assertThat(partitionedMesh.computeBoundingBox()).isNull()
    }

    @Test
    fun getRenderGroupCount_whenEmptyShape_shouldBeZero() {
        val partitionedMesh = PartitionedMesh()

        assertThat(partitionedMesh.getRenderGroupCount()).isEqualTo(0)
    }

    @Test
    fun getOutlineCount_whenEmptyShape_shouldThrow() {
        val partitionedMesh = PartitionedMesh()

        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineCount(-1) }
        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineCount(0) }
        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineCount(1) }
    }

    @Test
    fun getOutlineVertexCount_whenEmptyShape_shouldThrow() {
        val partitionedMesh = PartitionedMesh()

        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineVertexCount(-1, 0) }
        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineVertexCount(0, 0) }
        assertFailsWith<IllegalArgumentException> { partitionedMesh.getOutlineVertexCount(1, 0) }
    }

    @Test
    fun populateOutlinePosition_whenEmptyShape_shouldThrow() {
        val partitionedMesh = PartitionedMesh()

        assertFailsWith<IllegalArgumentException> {
            partitionedMesh.populateOutlinePosition(-1, 0, 0, MutableVec())
        }
        assertFailsWith<IllegalArgumentException> {
            partitionedMesh.populateOutlinePosition(0, 0, 0, MutableVec())
        }
        assertFailsWith<IllegalArgumentException> {
            partitionedMesh.populateOutlinePosition(1, 0, 0, MutableVec())
        }
    }

    @Test
    fun toString_returnsAString() {
        val string = PartitionedMesh().toString()

        // Not elaborate checks - this test mainly exists to ensure that toString doesn't crash.
        assertThat(string).contains("PartitionedMesh")
        assertThat(string).contains("bounds")
        assertThat(string).contains("meshes")
        assertThat(string).contains("nativeAddress")
    }

    @Test
    fun populateOutlinePosition_withStrokeShape_shouldBeWithinBounds() {
        val shape = buildTestStrokeShape()

        assertThat(shape.getRenderGroupCount()).isEqualTo(1)
        assertThat(shape.getOutlineCount(0)).isEqualTo(1)
        assertThat(shape.getOutlineVertexCount(0, 0)).isGreaterThan(2)

        val bounds = assertNotNull(shape.computeBoundingBox())

        val p = MutableVec()
        for (outlineVertexIndex in 0 until shape.getOutlineVertexCount(0, 0)) {
            shape.populateOutlinePosition(groupIndex = 0, outlineIndex = 0, outlineVertexIndex, p)
            assertThat(p.x).isAtLeast(bounds.xMin)
            assertThat(p.y).isAtLeast(bounds.yMin)
            assertThat(p.x).isAtMost(bounds.xMax)
            assertThat(p.y).isAtMost(bounds.yMax)
        }
    }

    @Test
    fun populateOutlinePosition_whenBadIndex_shouldThrow() {
        val shape = buildTestStrokeShape()

        val p = MutableVec()
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(-1, 0, 0, p)) }
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(5, 0, 0, p)) }
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(0, -1, 0, p)) }
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(0, 5, 0, p)) }
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(0, 0, -1, p)) }
        assertFailsWith<IllegalArgumentException> { (shape.populateOutlinePosition(0, 1, 999, p)) }
    }

    @Test
    fun meshFormat_forTestShape_isEquivalentToMeshFormatOfFirstMesh() {
        val partitionedMesh = buildTestStrokeShape()
        assertThat(partitionedMesh.getRenderGroupCount()).isEqualTo(1)
        val shapeFormat = partitionedMesh.renderGroupFormat(0)
        val meshes = partitionedMesh.renderGroupMeshes(0)
        assertThat(meshes).isNotEmpty()
        assertThat(shapeFormat).isNotNull()
        assertThat(meshes[0].format.isUnpackedEquivalent(shapeFormat)).isTrue()
    }

    /**
     * Verifies that [PartitionedMesh.coverage] calls the correct JNI method for [PartitionedMesh]
     * and [Triangle].
     */
    @Test
    fun coverage_forPartitionedMeshAndTriangle_callsJniAndReturnsFloat() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(15f, 4f),
                p1 = ImmutableVec(20f, 4f),
                p2 = ImmutableVec(20f, 5f),
            )
        val externalTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 200f),
                p1 = ImmutableVec(300f, 400f),
                p2 = ImmutableVec(100f, 700f),
            )

        assertThat(partitionedMesh.computeCoverage(intersectingTriangle)).isGreaterThan(0f)
        assertThat(partitionedMesh.computeCoverage(externalTriangle)).isEqualTo(0f)
        assertThat(partitionedMesh.computeCoverage(externalTriangle, SCALE_TRANSFORM)).isEqualTo(0f)
    }

    /**
     * Verifies that [PartitionedMesh.coverage] calls the correct JNI method for [PartitionedMesh]
     * and [Box].
     */
    @Test
    fun coverage_forPartitionedMeshAndBox_callsJniAndReturnsFloat() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(0f, 0f), ImmutableVec(100f, 100f))
        val externalBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        assertThat(partitionedMesh.computeCoverage(intersectingBox)).isGreaterThan(0f)
        assertThat(partitionedMesh.computeCoverage(externalBox)).isEqualTo(0f)
        assertThat(partitionedMesh.computeCoverage(externalBox, SCALE_TRANSFORM)).isEqualTo(0f)
    }

    /**
     * Verifies that [PartitionedMesh.coverage] calls the correct JNI method for [PartitionedMesh]
     * and [Parallelogram].
     */
    @Test
    fun coverage_forPartitionedMeshAndParallelogram_callsJniAndReturnsFloat() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(
                center = ImmutableVec(15f, 4f),
                width = 20f,
                height = 9f,
            )
        val externalParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 3f,
                height = 4f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 2f,
            )

        assertThat(partitionedMesh.computeCoverage(intersectingParallelogram)).isGreaterThan(0f)
        assertThat(partitionedMesh.computeCoverage(externalParallelogram)).isEqualTo(0f)
        assertThat(partitionedMesh.computeCoverage(externalParallelogram, SCALE_TRANSFORM))
            .isEqualTo(0f)
    }

    /**
     * Verifies that [PartitionedMesh.coverage] calls the correct JNI method for two
     * [PartitionedMesh]es.
     */
    @Test
    fun coverage_forTwoPartitionedMeshes_callsJniAndReturnsFloat() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(15f, 3f, 15f, 5f)).asImmutable(),
                )
                .shape
        val externalShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(100f, 3f, 200f, 5f)).asImmutable(),
                )
                .shape

        assertThat(partitionedMesh.computeCoverage(intersectingShape)).isGreaterThan(0f)
        assertThat(partitionedMesh.computeCoverage(externalShape)).isEqualTo(0f)
        assertThat(partitionedMesh.computeCoverage(externalShape, SCALE_TRANSFORM)).isEqualTo(0f)
    }

    /**
     * Verifies that [PartitionedMesh.coverageIsGreaterThan] calls the correct JNI method for
     * [PartitionedMesh] and [Triangle].
     */
    @Test
    fun coverageIsGreaterThan_forPartitionedMeshAndTriangle_callsJniAndReturnsFloat() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(15f, 4f),
                p1 = ImmutableVec(20f, 4f),
                p2 = ImmutableVec(20f, 5f),
            )
        val externalTriangle =
            ImmutableTriangle(
                p0 = ImmutableVec(100f, 200f),
                p1 = ImmutableVec(300f, 400f),
                p2 = ImmutableVec(100f, 700f),
            )

        assertThat(partitionedMesh.computeCoverageIsGreaterThan(intersectingTriangle, 0f)).isTrue()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalTriangle, 0f)).isFalse()
        assertThat(
                partitionedMesh.computeCoverageIsGreaterThan(externalTriangle, 0f, SCALE_TRANSFORM)
            )
            .isFalse()
    }

    /**
     * Verifies that [PartitionedMesh.coverageIsGreaterThan] calls the correct JNI method for
     * [PartitionedMesh] and [Box].
     *
     * For this test, `partitionedMesh` consists of triangulation of a straight line [Stroke] from
     * (10, 3) to (20, 5), with the total area of all triangles equal to 180.471. `intersectingBox`
     * intersects three of these triangles with the total area of 103.05. It has a coverage of
     * 103.05 / 180.471 ≈ 0.57. `externalBox` does not intersect with any of the triangles and has a
     * coverage of zero.
     */
    @Test
    fun coverageIsGreaterThan_forPartitionedMeshAndBox_callsJniAndReturnsBoolean() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(10f, 3f), ImmutableVec(15f, 5f))
        val externalBox =
            ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        assertThat(partitionedMesh.computeCoverageIsGreaterThan(intersectingBox, 0f)).isTrue()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalBox, 0f)).isFalse()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalBox, 0f, SCALE_TRANSFORM))
            .isFalse()
    }

    /**
     * Verifies that [PartitionedMesh.coverageIsGreaterThan] calls the correct JNI method for
     * [PartitionedMesh] and [Parallelogram].
     */
    @Test
    fun coverageIsGreaterThan_forPartitionedMeshAndParallelogram_callsJniAndReturnsBoolean() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingParallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(
                center = ImmutableVec(15f, 4f),
                width = 20f,
                height = 9f,
            )
        val externalParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(100f, 200f),
                width = 3f,
                height = 4f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                shearFactor = 2f,
            )

        assertThat(partitionedMesh.computeCoverageIsGreaterThan(intersectingParallelogram, 0f))
            .isTrue()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalParallelogram, 0f))
            .isFalse()
        assertThat(
                partitionedMesh.computeCoverageIsGreaterThan(
                    externalParallelogram,
                    0f,
                    SCALE_TRANSFORM
                )
            )
            .isFalse()
    }

    /**
     * Verifies that [PartitionedMesh.coverage] calls the correct JNI method for two
     * [PartitionedMesh]s.
     *
     * For this test, `partitionedMesh` consists of triangulation of a straight line [Stroke] from
     * (10, 3) to (20, 5), with the total area of all triangles equal to 180.471.
     * `intersectingShape` consists of the triangulation of a straight vertical line [Stroke] from
     * [15, 3] to [15, 5], and intersects with 6 of these triangles with the total area of 106.95.
     * It has a coverage of (106.95) / 180.471 ≈ 0.59. `externalShape` does not intersect with
     * `partitionedMesh`, and has zero coverage.
     */
    @Test
    fun coverageIsGreaterThan_forTwoPartitionedMeshes_callsJniAndReturnsBoolean() {
        val partitionedMesh = buildTestStrokeShape()
        val intersectingShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(15f, 3f, 15f, 5f)).asImmutable(),
                )
                .shape
        val externalShape =
            Stroke(
                    TEST_BRUSH,
                    buildStrokeInputBatchFromPoints(floatArrayOf(100f, 3f, 200f, 5f)).asImmutable(),
                )
                .shape

        assertThat(partitionedMesh.computeCoverageIsGreaterThan(intersectingShape, 0f)).isTrue()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalShape, 0f)).isFalse()
        assertThat(partitionedMesh.computeCoverageIsGreaterThan(externalShape, 0f, SCALE_TRANSFORM))
            .isFalse()
    }

    @Test
    fun initializeSpatialIndex() {
        val partitionedMesh = buildTestStrokeShape()
        assertThat(partitionedMesh.isSpatialIndexInitialized()).isFalse()

        partitionedMesh.initializeSpatialIndex()

        assertThat(partitionedMesh.isSpatialIndexInitialized()).isTrue()
    }

    @Test
    fun isSpatialIndexInitialized_afterGeometryQuery_returnsTrue() {
        val partitionedMesh = buildTestStrokeShape()
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(15f, 4f),
                p1 = ImmutableVec(20f, 4f),
                p2 = ImmutableVec(20f, 5f),
            )
        assertThat(partitionedMesh.isSpatialIndexInitialized()).isFalse()

        assertThat(partitionedMesh.computeCoverage(triangle)).isNotNaN()

        assertThat(partitionedMesh.isSpatialIndexInitialized()).isTrue()
    }

    private fun buildTestStrokeShape(): PartitionedMesh {
        return Stroke(
                TEST_BRUSH,
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable(),
            )
            .shape
    }

    companion object {
        private val SCALE_TRANSFORM = ImmutableAffineTransform(1.2f, 0f, 0f, 0f, 0.4f, 0f)

        private val TEST_BRUSH =
            Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
    }
}
