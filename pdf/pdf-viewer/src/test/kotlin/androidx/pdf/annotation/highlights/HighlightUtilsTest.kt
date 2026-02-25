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

package androidx.pdf.annotation.highlights

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import androidx.pdf.FakePdfDocument
import androidx.pdf.annotation.highlights.utils.applyTransform
import androidx.pdf.annotation.highlights.utils.calculateHighlightRects
import androidx.pdf.annotation.highlights.utils.computeBoundingBox
import androidx.pdf.annotation.highlights.utils.toPathPdfObjects
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import androidx.pdf.content.PdfPageTextContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class HighlightUtilsTest {

    @Test
    fun testApplyTransform_noTransform_returnsSamePoint() {
        val point = PointF(10f, 20f)
        val matrix = Matrix()
        val transformedPoint = point.applyTransform(matrix)
        assertThat(transformedPoint).isEqualTo(point)
    }

    @Test
    fun testApplyTransform_translate_returnsTranslatedPoint() {
        val point = PointF(10f, 20f)
        val matrix = Matrix().apply { postTranslate(5f, -5f) }
        val transformedPoint = point.applyTransform(matrix)
        assertThat(transformedPoint).isEqualTo(PointF(15f, 15f))
    }

    @Test
    fun testApplyTransform_scale_returnsScaledPoint() {
        val point = PointF(10f, 20f)
        val matrix = Matrix().apply { postScale(2f, 3f) }
        val transformedPoint = point.applyTransform(matrix)
        assertThat(transformedPoint).isEqualTo(PointF(20f, 60f))
    }

    @Test
    fun testComputeBoundingBox_emptyList_returnsEmptyRect() {
        val rects = emptyList<RectF>()
        val boundingBox = rects.computeBoundingBox()
        assertThat(boundingBox.isEmpty).isTrue()
    }

    @Test
    fun testComputeBoundingBox_singleRect_returnsSameRect() {
        val rect = RectF(10f, 10f, 20f, 20f)
        val rects = listOf(rect)
        val boundingBox = rects.computeBoundingBox()
        assertThat(boundingBox).isEqualTo(rect)
    }

    @Test
    fun testComputeBoundingBox_multipleOverlappingRects_returnsCorrectUnion() {
        val rects =
            listOf(RectF(10f, 10f, 30f, 30f), RectF(20f, 20f, 40f, 40f), RectF(0f, 0f, 15f, 15f))
        val boundingBox = rects.computeBoundingBox()
        assertThat(boundingBox).isEqualTo(RectF(0f, 0f, 40f, 40f))
    }

    @Test
    fun testComputeBoundingBox_multipleDisjointRects_returnsCorrectUnion() {
        val rects = listOf(RectF(0f, 0f, 10f, 10f), RectF(20f, 20f, 30f, 30f))
        val boundingBox = rects.computeBoundingBox()
        assertThat(boundingBox).isEqualTo(RectF(0f, 0f, 30f, 30f))
    }

    @Test
    fun testToPathPdfObjects_emptyList_returnsEmptyList() {
        val rects = emptyList<RectF>()
        val pathObjects = rects.toPathPdfObjects(Color.YELLOW)
        assertThat(pathObjects).isEmpty()
    }

    @Test
    fun testToPathPdfObjects_singleRect_returnsCorrectPathObject() {
        val rect = RectF(10f, 20f, 30f, 40f)
        val color = Color.RED
        val pathObjects = listOf(rect).toPathPdfObjects(color)

        assertThat(pathObjects).hasSize(1)
        val pathObject = pathObjects[0]

        assertThat(pathObject.brushColor).isEqualTo(color)
        assertThat(pathObject.brushWidth).isEqualTo(0f)

        val expectedInputs =
            listOf(
                PathInput(10f, 20f, PathInput.MOVE_TO), // left, top
                PathInput(30f, 20f, PathInput.LINE_TO), // right, top
                PathInput(30f, 40f, PathInput.LINE_TO), // right, bottom
                PathInput(10f, 40f, PathInput.LINE_TO), // left, bottom
                PathInput(10f, 20f, PathInput.LINE_TO), // left, top (closed path)
            )
        assertThat(pathObject.inputs).isEqualTo(expectedInputs)
    }

    @Test
    fun testToPathPdfObjects_multipleRects_returnsCorrectPathObjects() {
        val rects = listOf(RectF(0f, 0f, 10f, 10f), RectF(20f, 20f, 30f, 30f))
        val color = Color.BLUE
        val pathObjects = rects.toPathPdfObjects(color)

        assertThat(pathObjects).hasSize(rects.size)

        rects.forEachIndexed { index, rect ->
            val pathObject = pathObjects[index]
            assertThat(pathObject.brushColor).isEqualTo(color)
            assertThat(pathObject.brushWidth).isEqualTo(0f)

            val expectedInputs =
                listOf(
                    PathInput(rect.left, rect.top, PathInput.MOVE_TO),
                    PathInput(rect.right, rect.top, PathInput.LINE_TO),
                    PathInput(rect.right, rect.bottom, PathInput.LINE_TO),
                    PathInput(rect.left, rect.bottom, PathInput.LINE_TO),
                    PathInput(rect.left, rect.top, PathInput.LINE_TO),
                )
            assertThat(pathObject.inputs).isEqualTo(expectedInputs)
        }
    }

    @Test
    fun calculateHighlightRects_withText_returnsCorrectRects() = runTest {
        val pageText =
            PdfPageTextContent(bounds = listOf(RectF(10f, 10f, 100f, 100f)), text = "Sample Text")
        val fakePdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(pageText))
        val startPoint = PointF(20f, 20f)
        val endPoint = PointF(80f, 80f)

        val rects = fakePdfDocument.calculateHighlightRects(0, startPoint, endPoint)

        assertThat(rects).hasSize(1)
        assertThat(rects[0]).isEqualTo(RectF(20f, 20f, 80f, 80f))
    }

    @Test
    fun calculateHighlightRects_noText_returnsEmptyList() = runTest {
        val fakePdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = emptyList())
        val startPoint = PointF(20f, 20f)
        val endPoint = PointF(80f, 80f)

        val rects = fakePdfDocument.calculateHighlightRects(0, startPoint, endPoint)

        assertThat(rects).isEmpty()
    }
}
