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

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidGraphicsConversionExtensionsTest {
    @Test
    fun populateMatrix_resultingMatrixIsAffine() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val matrix = Matrix()
        affineTransform.populateMatrix(matrix)
        assertThat(matrix.isAffine).isTrue()
    }

    @Test
    fun populateMatrix_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // First, apply the affineTransform to an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        // Then, populate an android.graphics.Matrix from the affineTransform and perform the
        // equivalent
        // operation.
        val matrix = Matrix()
        affineTransform.populateMatrix(matrix)
        val vecFloatArray = floatArrayOf(inputVec.x, inputVec.y)
        matrix.mapPoints(vecFloatArray)

        assertThat(outputVec).isEqualTo(ImmutableVec(vecFloatArray[0], vecFloatArray[1]))
    }

    @Test
    fun toMatrix_resultingMatrixIsAffine() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val matrix = affineTransform.toMatrix()
        assertThat(matrix.isAffine).isTrue()
    }

    @Test
    fun toMatrix_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // First, apply the affineTransform to an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        // Then, populate an android.graphics.Matrix from the affineTransform and perform the
        // equivalent
        // operation.
        val matrix = affineTransform.toMatrix()
        val vecFloatArray = floatArrayOf(inputVec.x, inputVec.y)
        matrix.mapPoints(vecFloatArray)

        assertThat(outputVec).isEqualTo(ImmutableVec(vecFloatArray[0], vecFloatArray[1]))
    }

    @Test
    fun ImmutableAffineTransform_from_resultsInEquivalentVecTransformations() {
        val matrix = Matrix()
        matrix.setValues(floatArrayOf(A, B, C, D, E, F, 0f, 0f, 1f))
        // First, apply the matrix to the values of an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val vecFloatArray = floatArrayOf(inputVec.x, inputVec.y)
        matrix.mapPoints(vecFloatArray)

        // Then, populate an ImmutableAffineTransform from the matrix and perform the equivalent
        // operation.
        val affineTransform = ImmutableAffineTransform.from(matrix)
        val outputVec = MutableVec()
        affineTransform?.applyTransform(inputVec, outputVec)

        assertThat(outputVec).isEqualTo(ImmutableVec(vecFloatArray[0], vecFloatArray[1]))
    }

    @Test
    fun MutableAffineTransform_populateFrom_resultsInEquivalentVecTransformations() {
        val matrix = Matrix()
        matrix.setValues(floatArrayOf(A, B, C, D, E, F, 0f, 0f, 1f))
        // First, apply the matrix to the values of an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val vecFloatArray = floatArrayOf(inputVec.x, inputVec.y)
        matrix.mapPoints(vecFloatArray)

        // Then, populate an MutableAffineTransform from the matrix and perform the equivalent
        // operation.
        val affineTransform = MutableAffineTransform()
        affineTransform.populateFrom(matrix)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        assertThat(outputVec).isEqualTo(ImmutableVec(vecFloatArray[0], vecFloatArray[1]))
    }

    @Test
    fun MutableAffineTransform_populateFrom_throwsForNonAffineMatrix() {
        val matrix = Matrix()
        matrix.setValues(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val affineTransform = MutableAffineTransform()
        assertFailsWith<IllegalArgumentException> { affineTransform.populateFrom(matrix) }
    }

    @Test
    fun toPointF_resultingPointIsEquivalent() {
        val vec = ImmutableVec(1f, 2f)
        val point = vec.toPointF()
        assertThat(point.x).isEqualTo(vec.x)
        assertThat(point.y).isEqualTo(vec.y)
    }

    @Test
    fun populatePointF_resultingPointIsEquivalent() {
        val vec = ImmutableVec(1f, 2f)
        val point = PointF()
        assertThat(vec.populatePointF(point)).isSameInstanceAs(point)
        assertThat(point.x).isEqualTo(vec.x)
        assertThat(point.y).isEqualTo(vec.y)
    }

    @Test
    fun from_resultingVecIsEquivalentToPointF() {
        val point = PointF(1f, 2f)
        val vec = ImmutableVec.from(point)

        assertThat(vec.x).isEqualTo(point.x)
        assertThat(vec.y).isEqualTo(point.y)
    }

    @Test
    fun populateFrom_resultingVecIsEquivalentToPointF() {
        val point = PointF(1f, 2f)
        val vec = MutableVec()
        assertThat(vec.populateFrom(point)).isSameInstanceAs(vec)

        assertThat(vec.x).isEqualTo(point.x)
        assertThat(vec.y).isEqualTo(point.y)
    }

    @Test
    fun toRectF_resultingRectIsEquivalent() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val rect = box.toRectF()

        assertThat(rect.left).isEqualTo(box.xMin)
        assertThat(rect.top).isEqualTo(box.yMin)
        assertThat(rect.right).isEqualTo(box.xMax)
        assertThat(rect.bottom).isEqualTo(box.yMax)
    }

    @Test
    fun populateRectF_resultingRectIsEquivalent() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val rect = RectF()
        assertThat(box.populateRectF(rect)).isSameInstanceAs(rect)

        assertThat(rect.left).isEqualTo(box.xMin)
        assertThat(rect.top).isEqualTo(box.yMin)
        assertThat(rect.right).isEqualTo(box.xMax)
        assertThat(rect.bottom).isEqualTo(box.yMax)
    }

    @Test
    fun from_resultingBoxIsEquivalentToRectF() {
        val rect = RectF(1f, 2f, 3f, 4f)
        val box = ImmutableBox.from(rect)

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun from_resultIsNullForEmptyRectF() {
        val rect = RectF()
        val box = ImmutableBox.from(rect)

        assertThat(box).isNull()
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsEquivalentToRectF() {
        val rect = RectF(1f, 2f, 3f, 4f)
        val boxAccumulator = BoxAccumulator()
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isNotNull()

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsUnchangedForEmptyRectF() {
        // Malformed rectF
        val rect = RectF(5f, 8f, 2f, 1f)
        val startingBox = MutableBox().setXBounds(1F, 2F).setYBounds(3F, 4F)
        val boxAccumulator = BoxAccumulator(startingBox)
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isEqualTo(startingBox)
    }

    @Test
    fun outlinesToPath_returnsCorrectListOfPath() {
        val partitionedMesh = buildTestStrokeShape()
        val path = partitionedMesh.outlinesToPath(0)
        assertThat(path.isEmpty).isFalse()
        // For point-by-point test, see the AndroidGraphicsConversionExtensionsEmulatorTest.
    }

    @Test
    fun outlinesToPath_returnsOneListWithEmptyPathForEmptyShape() {
        val partitionedMesh = buildEmptyTestStrokeShape()

        val path = partitionedMesh.outlinesToPath(0)
        assertThat(path.isEmpty).isTrue()
    }

    @Test
    fun populatePathFromOutlines_returnsCorrectPath() {
        val partitionedMesh = buildTestStrokeShape()
        val path = Path()
        assertThat(partitionedMesh.populateOutlines(0, path)).isSameInstanceAs(path)
        assertThat(path.isEmpty).isFalse()
        // For point-by-point test, see the AndroidGraphicsConversionExtensionsEmulatorTest.
    }

    @Test
    fun populatePathFromOutlines_returnsWithEmptyPathForEmptyShape() {
        val partitionedMesh = buildEmptyTestStrokeShape()

        val path = Path()
        partitionedMesh.populateOutlines(0, path)
        assertThat(path.isEmpty).isTrue()
    }

    private fun buildTestStrokeShape(): PartitionedMesh {
        return Stroke(
                TEST_BRUSH,
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable(),
            )
            .shape
    }

    private fun buildEmptyTestStrokeShape(): PartitionedMesh {
        return Stroke(TEST_BRUSH, buildStrokeInputBatchFromPoints(floatArrayOf()).asImmutable())
            .shape
    }

    companion object {
        private const val A = 1f
        private const val B = 2f
        private const val C = -3f
        private const val D = -4f
        private const val E = 5f
        private const val F = 6f

        private val TEST_BRUSH =
            Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
    }
}
