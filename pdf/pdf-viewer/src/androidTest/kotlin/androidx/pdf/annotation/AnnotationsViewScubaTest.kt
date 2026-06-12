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

package androidx.pdf.annotation

import android.graphics.Color
import android.graphics.RectF
import android.util.SparseArray
import android.widget.FrameLayout
import androidx.pdf.ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_NO_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_SHARED_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_MULTI_PAGE
import androidx.pdf.ANNOTATION_VIEW_SINGLE_SQUARE_NO_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_SQUARE_COMBINED_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_SQUARE_SCALED
import androidx.pdf.ANNOTATION_VIEW_SQUARE_TRANSLATED
import androidx.pdf.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.pdf.annotation.content.KeyedPdfAnnotation
import androidx.pdf.annotation.content.PathPdfObject
import androidx.pdf.annotation.content.PathPdfObject.PathInput
import androidx.pdf.annotation.content.PdfAnnotation
import androidx.pdf.annotation.content.StampAnnotation
import androidx.pdf.assertScreenshot
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.util.UUID
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class AnnotationViewScubaTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testAnnotationView_withSingleSquareAndNoTransform() {
        val square = createSquareAnnotation()
        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(0f, 0f, 500f, 800f)) },
                zoom = 1f,
            )

        setupAndTakeScreenshot(
            viewportState,
            annotations,
            ANNOTATION_VIEW_SINGLE_SQUARE_NO_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withTranslatedSquare() {
        val square = createSquareAnnotation()
        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(50f, 30f, 550f, 830f)) },
                zoom = 1f,
            )

        setupAndTakeScreenshot(viewportState, annotations, ANNOTATION_VIEW_SQUARE_TRANSLATED)
    }

    @Test
    fun testAnnotationView_withScaledSquare() {
        val square = createSquareAnnotation()
        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(0f, 0f, 500f, 800f)) },
                zoom = 2f,
            )

        setupAndTakeScreenshot(viewportState, annotations, ANNOTATION_VIEW_SQUARE_SCALED)
    }

    @Test
    fun testAnnotationView_withCombinedTransformSquare() {
        val square = createSquareAnnotation()
        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(30f, 30f, 530f, 830f)) },
                zoom = 2f,
            )

        setupAndTakeScreenshot(
            viewportState,
            annotations,
            ANNOTATION_VIEW_SQUARE_COMBINED_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withMultipleSquaresOnSamePageAndNoTransform() {
        val square1 = createSquareAnnotation(size = 50f)
        val square2 =
            createSquareAnnotation(size = 50f, color = Color.GREEN, xOffset = 70f, yOffset = 20f)

        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square1, square2)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(0f, 0f, 500f, 800f)) },
                zoom = 1f,
            )

        setupAndTakeScreenshot(
            viewportState,
            annotations,
            ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_NO_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withMultipleSquaresOnSamePageAndSharedTransform() {
        val square1 = createSquareAnnotation(size = 50f)
        val square2 =
            createSquareAnnotation(size = 50f, color = Color.GREEN, xOffset = 70f, yOffset = 20f)

        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square1, square2)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 1,
                pageBounds = SparseArray<RectF>().apply { put(0, RectF(50f, 50f, 550f, 850f)) },
                zoom = 2f,
            )

        setupAndTakeScreenshot(
            viewportState,
            annotations,
            ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_SHARED_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withAnnotationsOnDifferentPages() {
        val square1 =
            createSquareAnnotation(pageNumber = 0, color = Color.RED, xOffset = 10f, yOffset = 10f)
        val square2 =
            createSquareAnnotation(
                pageNumber = 1,
                color = Color.GREEN,
                xOffset = 10f,
                yOffset = 10f,
            )

        val annotations =
            SparseArray<List<KeyedPdfAnnotation>>().apply {
                put(0, createKeyedAnnotations(listOf(square1)))
                put(1, createKeyedAnnotations(listOf(square2)))
            }
        val viewportState =
            PdfViewportState(
                firstVisiblePage = 0,
                visiblePagesCount = 2,
                pageBounds =
                    SparseArray<RectF>().apply {
                        put(0, RectF(0f, 0f, 500f, 800f))
                        put(1, RectF(0f, 500f, 500f, 1300f))
                    },
                zoom = 2f,
            )

        setupAndTakeScreenshot(viewportState, annotations, ANNOTATION_VIEW_MULTI_PAGE)
    }

    private fun setupAndTakeScreenshot(
        viewportState: PdfViewportState,
        annotationData: SparseArray<List<KeyedPdfAnnotation>>,
        screenshotName: String,
    ) {
        setupAnnotationViewInActivity(viewportState, annotationData)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            assertScreenshot(ANNOTATION_VIEW_ID, screenshotRule, screenshotName)
            close()
        }
    }

    private fun setupAnnotationViewInActivity(
        viewportState: PdfViewportState,
        annotationData: SparseArray<List<KeyedPdfAnnotation>>,
    ) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                val layoutParams =
                    FrameLayout.LayoutParams(CONTAINER_VIEW_WIDTH, CONTAINER_VIEW_HEIGHT)
                container.layoutParams = layoutParams

                val annotationView =
                    AnnotationsView(activity).apply {
                        id = ANNOTATION_VIEW_ID
                        this.layoutParams =
                            FrameLayout.LayoutParams(CONTAINER_VIEW_WIDTH, CONTAINER_VIEW_HEIGHT)
                        updateDisplayState(viewportState, annotationData)
                    }
                container.addView(annotationView)
            }
        }
    }

    private fun createSquareAnnotation(
        size: Float = DEFAULT_SQUARE_SIZE,
        pageNumber: Int = 0,
        color: Int = DEFAULT_BRUSH_COLOR,
        xOffset: Float = 0f,
        yOffset: Float = 0f,
    ): StampAnnotation {
        val pathInputs =
            listOf(
                PathInput(xOffset, yOffset, PathInput.MOVE_TO),
                PathInput(xOffset + size, yOffset, PathInput.LINE_TO),
                PathInput(xOffset + size, yOffset + size, PathInput.LINE_TO),
                PathInput(xOffset, yOffset + size, PathInput.LINE_TO),
                PathInput(xOffset, yOffset, PathInput.LINE_TO), // Close the path
            )
        val pathObject = PathPdfObject(brushColor = color, brushWidth = 5f, inputs = pathInputs)
        val bounds = RectF(xOffset, yOffset, xOffset + size, yOffset + size)
        return StampAnnotation(pageNumber, bounds, listOf(pathObject))
    }

    private fun createKeyedAnnotations(annotations: List<PdfAnnotation>): List<KeyedPdfAnnotation> {
        return annotations.map { KeyedPdfAnnotation(key = UUID.randomUUID().toString(), it) }
    }

    companion object {
        const val ANNOTATION_VIEW_ID = 123456789

        const val DEFAULT_SQUARE_SIZE = 100f
        const val DEFAULT_BRUSH_COLOR = Color.BLUE
        const val CONTAINER_VIEW_WIDTH = 500
        const val CONTAINER_VIEW_HEIGHT = 800
    }
}
