/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink.util

import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.pdf.annotation.models.PathPdfObject
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.collections.forEach
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class StrokeExtensionsTest {

    @Test
    fun getBounds_singlePoint_calculatesCorrectBounds() {
        // Arrange
        val inputs = listOf(createStrokeInput(x = 10f, y = 20f))
        val stroke = createStroke(inputs)
        val expectedBounds = RectF(10f, 20f, 10f, 20f)

        // Act
        val bounds = stroke.getBounds()

        // Assert
        assertThat(bounds.isCloseTo(expectedBounds, DEFAULT_BRUSH.size)).isTrue()
    }

    @Test
    fun getBounds_multiplePoints_calculatesCorrectBounds() {
        // Arrange
        val inputs =
            listOf(
                createStrokeInput(x = 10f, y = 20f, eventTime = 0L),
                createStrokeInput(x = 30f, y = 40f, eventTime = 100L),
                createStrokeInput(x = 5f, y = 15f, eventTime = 200L),
                createStrokeInput(x = 35f, y = 45f, eventTime = 300L),
            )
        val stroke = createStroke(inputs)
        val expectedBounds = RectF(5f, 15f, 35f, 45f)

        // Act
        val bounds = stroke.getBounds()

        // Assert
        assertThat(bounds.isCloseTo(expectedBounds, DEFAULT_BRUSH.size)).isTrue()
    }

    @Test
    fun toStampAnnotation_convertsStrokeToStampAnnotationCorrectly() {
        // Arrange
        val pageNum = 3
        val inputs =
            listOf(
                createStrokeInput(x = 10f, y = 20f, eventTime = 0L),
                createStrokeInput(x = 30f, y = 40f, eventTime = 100L),
            )
        val brush =
            Brush.createWithColorIntArgb(
                family = StockBrushes.pressurePen(),
                colorIntArgb = Color.Blue.toArgb(),
                size = 1f,
                epsilon = 0.1f,
            )
        val stroke = createStroke(inputs, brush)
        val expectedBounds = RectF(10f, 20f, 30f, 40f)
        val expectedPdfPoints = listOf(PointF(10f, 20f), PointF(30f, 40f))

        // Act
        val stampAnnotation = stroke.toStampAnnotation(pageNum)

        // Assert
        assertThat(stampAnnotation.pageNum).isEqualTo(pageNum)
        assertThat(stampAnnotation.bounds.isCloseTo(expectedBounds, brush.size)).isTrue()
        assertThat(stampAnnotation.pdfObjects).hasSize(1)

        val pathObject = stampAnnotation.pdfObjects.first() as? PathPdfObject
        assertThat(pathObject).isNotNull()
        pathObject?.let {
            assertThat(it.brushColor).isEqualTo(brush.colorIntArgb)
            assertThat(it.brushWidth).isEqualTo(brush.size)
            assertThat(it.inputs).isNotEmpty()
            assertPointsCloseToExpected(it.inputs, expectedPdfPoints, brush.size)
        }
    }

    private fun assertPointsCloseToExpected(
        actualPoints: List<PathPdfObject.PathInput>,
        expectedPoints: List<PointF>,
        tolerance: Float,
    ) {
        // Verify points in the path outline are close to expected coordinates (within tolerance).
        expectedPoints.forEach { expectedPoint ->
            assertThat(actualPoints.any { it.isCloseTo(expectedPoint, tolerance) }).isTrue()
        }
    }

    private fun PathPdfObject.PathInput.isCloseTo(point: PointF, tolerance: Float): Boolean {
        return abs(x - point.x) < tolerance && abs(y - point.y) < tolerance
    }

    private fun RectF.isCloseTo(expectedRectF: RectF, tolerance: Float): Boolean {
        return abs(left - expectedRectF.left) < tolerance &&
            abs(top - expectedRectF.top) < tolerance &&
            abs(right - expectedRectF.right) < tolerance &&
            abs(bottom - expectedRectF.bottom) < tolerance
    }

    private fun createStroke(inputs: List<StrokeInput>, brush: Brush = DEFAULT_BRUSH): Stroke {
        val inputBatch = MutableStrokeInputBatch().add(inputs)
        return Stroke(brush, inputBatch)
    }

    private fun createStrokeInput(x: Float, y: Float, eventTime: Long = 0L): StrokeInput {
        return StrokeInput().apply { update(x, y, eventTime) }
    }

    private companion object {
        private val DEFAULT_BRUSH =
            Brush.createWithColorIntArgb(
                family = StockBrushes.pressurePen(),
                colorIntArgb = Color.Black.toArgb(),
                size = 5f,
                epsilon = 0.1f,
            )
    }
}
