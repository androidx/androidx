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

package androidx.pdf.annotation.drawer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowCanvas
import org.robolectric.shadows.ShadowPath

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PathPdfObjectDrawerTest {

    private lateinit var canvas: Canvas
    private lateinit var shadowCanvas: ShadowCanvas

    @Before
    fun setUp() {
        canvas = Canvas()
        shadowCanvas = Shadows.shadowOf(canvas)
    }

    @Test
    fun draw_emptyInputs_doesNotDrawAnything() {
        val emptyPathObject = createPathPdfObject(inputs = emptyList())

        PathPdfObjectDrawer.draw(emptyPathObject, canvas, Matrix())

        assertThat(shadowCanvas.pathPaintHistoryCount).isEqualTo(0)
    }

    @Test
    fun draw_validInputs_setsPaintPropertiesAndDrawsPath() {
        val pathInputs =
            listOf(
                PathInput(x = 10f, y = 10f, PathInput.MOVE_TO),
                PathInput(x = 50f, y = 10f, PathInput.LINE_TO),
            )
        val brushColor = Color.BLUE
        val brushWidth = 8f
        val pathPdfObject =
            createPathPdfObject(
                brushColor = brushColor,
                brushWidth = brushWidth,
                inputs = pathInputs,
            )
        val expectedPath =
            Path().apply {
                moveTo(10f, 10f)
                lineTo(50f, 10f)
            }

        PathPdfObjectDrawer.draw(pathPdfObject, canvas, Matrix())

        PathPdfObjectDrawer.paint.apply {
            assertThat(color).isEqualTo(brushColor)
            assertThat(strokeWidth).isEqualTo(brushWidth)
            assertThat(style).isEqualTo(android.graphics.Paint.Style.FILL)
            assertThat(isAntiAlias).isTrue()
        }

        assertThat(shadowCanvas.pathPaintHistoryCount).isEqualTo(1)

        val drawnPath = shadowCanvas.getDrawnPath(0)
        val shadowDrawnPath = Shadows.shadowOf(drawnPath)
        val shadowExpectedPath = Shadows.shadowOf(expectedPath)

        assertThat(shadowDrawnPath.points).isEqualTo(shadowExpectedPath.points)
    }

    @Test
    fun createPath_emptyInputs_returnsEmptyPath() {
        val pathObject = createPathPdfObject(inputs = emptyList())
        val path = pathObject.createPath()
        assertThat(path.isEmpty).isTrue()
    }

    @Test
    fun createPath_singlePointInput_createsPathWithMoveTo() {
        val startX = 10f
        val startY = 20f
        val inputPoint = PathInput(x = startX, y = startY, PathInput.MOVE_TO)
        val pathObject = createPathPdfObject(inputs = listOf(inputPoint))

        val path = pathObject.createPath()
        val shadowPath = Shadows.shadowOf(path)
        val pathPoints = shadowPath.points

        assertThat(path.isEmpty).isFalse()
        assertThat(pathPoints).hasSize(1)
        assertPathPoint(pathPoints.first(), ShadowPath.Point.Type.MOVE_TO, inputPoint)
    }

    @Test
    fun createPath_multiplePoints_chainsMoveToAndLineToOperations() {
        val inputPoints =
            listOf(
                PathInput(x = 0f, y = 0f, PathInput.MOVE_TO),
                PathInput(x = 10f, y = 0f, PathInput.LINE_TO),
                PathInput(x = 10f, y = 20f, PathInput.LINE_TO),
            )
        val pathObject = createPathPdfObject(inputs = inputPoints)

        val path = pathObject.createPath()
        val shadowPath = Shadows.shadowOf(path)
        val pathPoints = shadowPath.points

        assertThat(path.isEmpty).isFalse()
        assertThat(pathPoints).hasSize(inputPoints.size)

        assertPathPoint(pathPoints.first(), ShadowPath.Point.Type.MOVE_TO, inputPoints.first())

        inputPoints.drop(1).forEachIndexed { index, expectedPoint ->
            assertPathPoint(
                pathPoints[index + 1], // Offset by 1 because we dropped the first inputPoint
                ShadowPath.Point.Type.LINE_TO,
                expectedPoint,
            )
        }
    }

    private fun assertPathPoint(
        point: ShadowPath.Point,
        expectedType: ShadowPath.Point.Type,
        expectedPoint: PathInput,
    ) {
        assertThat(point.type).isEqualTo(expectedType)
        assertThat(point.x).isEqualTo(expectedPoint.x)
        assertThat(point.y).isEqualTo(expectedPoint.y)
    }

    private companion object {
        private const val DEFAULT_BRUSH_COLOR = Color.RED
        private const val DEFAULT_BRUSH_WIDTH = 5f

        private fun createPathPdfObject(
            brushColor: Int = DEFAULT_BRUSH_COLOR,
            brushWidth: Float = DEFAULT_BRUSH_WIDTH,
            inputs: List<PathInput>,
        ): PathPdfObject {
            return PathPdfObject(brushColor = brushColor, brushWidth = brushWidth, inputs = inputs)
        }
    }
}
