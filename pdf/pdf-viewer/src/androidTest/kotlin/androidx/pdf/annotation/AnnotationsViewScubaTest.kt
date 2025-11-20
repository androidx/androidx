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
import android.graphics.Matrix
import android.graphics.RectF
import android.util.SparseArray
import android.widget.FrameLayout
import androidx.pdf.ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_NO_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_SHARED_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_MULTI_PAGE_DIFFERENT_TRANSFORMS
import androidx.pdf.ANNOTATION_VIEW_SINGLE_SQUARE_NO_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_SQUARE_COMBINED_TRANSFORM
import androidx.pdf.ANNOTATION_VIEW_SQUARE_SCALED
import androidx.pdf.ANNOTATION_VIEW_SQUARE_TRANSLATED
import androidx.pdf.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.assertScreenshot
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
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
        val pageData = createPageRenderData(annotations = listOf(square))
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }

        setupAndTakeScreenshot(annotationsOnPage, ANNOTATION_VIEW_SINGLE_SQUARE_NO_TRANSFORM)
    }

    @Test
    fun testAnnotationView_withTranslatedSquare() {
        val square = createSquareAnnotation()
        val translationMatrix = Matrix().apply { postTranslate(50f, 30f) }

        val pageData =
            createPageRenderData(annotations = listOf(square), transform = translationMatrix)
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }

        setupAndTakeScreenshot(annotationsOnPage, ANNOTATION_VIEW_SQUARE_TRANSLATED)
    }

    @Test
    fun testAnnotationView_withScaledSquare() {
        val square = createSquareAnnotation()
        val scaleMatrix = Matrix().apply { postScale(2f, 2f) }

        val pageData = createPageRenderData(annotations = listOf(square), transform = scaleMatrix)
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }

        setupAndTakeScreenshot(annotationsOnPage, ANNOTATION_VIEW_SQUARE_SCALED)
    }

    @Test
    fun testAnnotationView_withCombinedTransformSquare() {
        val square = createSquareAnnotation()
        val transformMatrix =
            Matrix().apply {
                postScale(2f, 2f)
                postTranslate(30f, 30f)
            }

        val pageData =
            createPageRenderData(annotations = listOf(square), transform = transformMatrix)
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }

        setupAndTakeScreenshot(annotationsOnPage, ANNOTATION_VIEW_SQUARE_COMBINED_TRANSFORM)
    }

    @Test
    fun testAnnotationView_withMultipleSquaresOnSamePageAndNoTransform() {
        val square1 = createSquareAnnotation(size = 50f)
        val square2 =
            createSquareAnnotation(size = 50f, color = Color.GREEN, xOffset = 70f, yOffset = 20f)

        val pageData = createPageRenderData(annotations = listOf(square1, square2))
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }

        setupAndTakeScreenshot(
            annotationsOnPage,
            ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_NO_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withMultipleSquaresOnSamePageAndSharedTransform() {
        val square1 = createSquareAnnotation(size = 50f)
        val square2 =
            createSquareAnnotation(size = 50f, color = Color.GREEN, xOffset = 70f, yOffset = 20f)

        // This single transform will apply to all annotations on this page
        val sharedTransform =
            Matrix().apply {
                postScale(2f, 2f)
                postTranslate(50f, 50f)
            }
        val pageData =
            createPageRenderData(
                annotations = listOf(square1, square2),
                transform = sharedTransform,
            )
        val annotationsOnPage = SparseArray<PageAnnotationsData>().apply { put(0, pageData) }
        setupAndTakeScreenshot(
            annotationsOnPage,
            ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_SHARED_TRANSFORM,
        )
    }

    @Test
    fun testAnnotationView_withAnnotationsOnDifferentPagesAndDifferentTransforms() {
        val square1 =
            createSquareAnnotation(pageNumber = 0, color = Color.RED, xOffset = 10f, yOffset = 10f)
        val transformationMatrix1 = Matrix().apply { postScale(1.5f, 1.5f) }
        val pageData0 =
            createPageRenderData(annotations = listOf(square1), transform = transformationMatrix1)

        val square2 =
            createSquareAnnotation(
                pageNumber = 1,
                color = Color.GREEN,
                xOffset = 10f,
                yOffset = 10f,
            )
        val transformationMatrix2 =
            Matrix().apply {
                postScale(2f, 2f)
                postTranslate(0f, 500f) // Let's assume page size as 200*500
            }
        val pageData1 =
            createPageRenderData(annotations = listOf(square2), transform = transformationMatrix2)

        val annotationsAcrossPages =
            SparseArray<PageAnnotationsData>().apply {
                put(0, pageData0)
                put(1, pageData1)
            }
        setupAndTakeScreenshot(
            annotationsAcrossPages,
            ANNOTATION_VIEW_MULTI_PAGE_DIFFERENT_TRANSFORMS,
        )
    }

    private fun setupAndTakeScreenshot(
        annotationData: SparseArray<PageAnnotationsData>,
        screenshotName: String,
    ) {
        setupAnnotationViewInActivity(annotationData)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            assertScreenshot(ANNOTATION_VIEW_ID, screenshotRule, screenshotName)
            close()
        }
    }

    private fun setupAnnotationViewInActivity(annotationData: SparseArray<PageAnnotationsData>) {
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
                        this.annotations = annotationData
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
                PathPdfObject.PathInput(xOffset, yOffset),
                PathPdfObject.PathInput(xOffset + size, yOffset),
                PathPdfObject.PathInput(xOffset + size, yOffset + size),
                PathPdfObject.PathInput(xOffset, yOffset + size),
                PathPdfObject.PathInput(xOffset, yOffset), // Close the path
            )
        val pathObject = PathPdfObject(brushColor = color, brushWidth = 5f, inputs = pathInputs)
        val bounds = RectF(xOffset, yOffset, xOffset + size, yOffset + size)
        return StampAnnotation(pageNumber, bounds, listOf(pathObject))
    }

    private fun createPageRenderData(
        annotations: List<PdfAnnotation>,
        transform: Matrix = Matrix(),
    ): PageAnnotationsData {
        return PageAnnotationsData(annotations, transform)
    }

    companion object {
        const val ANNOTATION_VIEW_ID = 123456789

        const val DEFAULT_SQUARE_SIZE = 100f
        const val DEFAULT_BRUSH_COLOR = Color.BLUE
        const val CONTAINER_VIEW_WIDTH = 500
        const val CONTAINER_VIEW_HEIGHT = 800
    }
}
