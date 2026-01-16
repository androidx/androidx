/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.annotation

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import android.util.SparseArray
import android.view.MotionEvent
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AnnotationsLocatorTest {

    private lateinit var annotationsLocator: AnnotationsLocator
    private lateinit var context: Context
    private lateinit var pageInfoProvider: FakePageInfoProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pageInfoProvider = FakePageInfoProvider()
        annotationsLocator = AnnotationsLocator(context, pageInfoProvider)
    }

    @Test
    fun findAnnotations_hitDetected_returnsListWithAnnotation() {
        val annotationBounds = RectF(100f, 100f, 200f, 200f)
        val annotation = createStampAnnotation(annotationBounds)
        val annotationsData = createAnnotationsData(listOf(annotation))

        // Simulate Touch inside bounds at (150, 150)
        val event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 150f, 150f)

        val results = annotationsLocator.findAnnotations(annotationsData, event)

        assertThat(results).isNotEmpty()
        assertThat(results).hasSize(1)
        assertThat(results[0].annotation).isEqualTo(annotation)
    }

    @Test
    fun findAnnotations_outsideBounds_returnsEmptyList() {
        val annotationBounds = RectF(100f, 100f, 200f, 200f)
        val annotation = createStampAnnotation(annotationBounds)
        val annotationsData = createAnnotationsData(listOf(annotation))

        // Simulate Touch outside bounds at (300, 300)
        val event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 300f, 300f)

        val results = annotationsLocator.findAnnotations(annotationsData, event)

        assertThat(results).isEmpty()
    }

    @Test
    fun findAnnotations_swipeGesture_hitDetected() {
        val annotationBounds = RectF(100f, 100f, 200f, 200f)
        val annotation = createStampAnnotation(annotationBounds)
        val annotationsData = createAnnotationsData(listOf(annotation))

        // Simulate ACTION_MOVE (Swipe) over the annotation
        val event = obtainMotionEvent(MotionEvent.ACTION_MOVE, 150f, 150f)

        val results = annotationsLocator.findAnnotations(annotationsData, event)

        assertThat(results).hasSize(1)
        assertThat(results[0].annotation).isEqualTo(annotation)
    }

    @Test
    fun findAnnotations_overlappingAnnotations_returnsCorrectOrder() {
        // Both annotations share the same bounds, but one is logically "on top" (higher index)
        val bounds = RectF(100f, 100f, 200f, 200f)
        val bottomAnnotation = createStampAnnotation(bounds) // Z-Index 0
        val topAnnotation = createStampAnnotation(bounds) // Z-Index 1

        // The list order represents Z-order: index 0 is bottom, last index is top
        val annotationsData = createAnnotationsData(listOf(bottomAnnotation, topAnnotation))

        // Simulate Touch inside the overlapping bounds
        val event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 150f, 150f)

        val results = annotationsLocator.findAnnotations(annotationsData, event)

        assertThat(results).hasSize(2)
        // Verify reverse Z-order (Top-most element should be first in the returned list)
        assertThat(results[0].annotation).isEqualTo(topAnnotation)
        assertThat(results[1].annotation).isEqualTo(bottomAnnotation)
    }

    @Test
    fun findAnnotations_noAnnotationsOnPage_returnsEmptyList() {
        val annotationsData = createAnnotationsData(emptyList())
        val event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 150f, 150f)

        val results = annotationsLocator.findAnnotations(annotationsData, event)

        assertThat(results).isEmpty()
    }

    // --- Helpers ---

    private fun createAnnotationsData(
        annotations: List<PdfAnnotation>
    ): SparseArray<PageAnnotationsData> {
        val keyedAnnotations =
            annotations.map { KeyedPdfAnnotation(key = UUID.randomUUID().toString(), it) }

        // FakePageInfoProvider always returns pageNum = 0
        val data = PageAnnotationsData(keyedAnnotations, Matrix())
        val sparseArray = SparseArray<PageAnnotationsData>()
        sparseArray.put(0, data)
        return sparseArray
    }

    private fun createStampAnnotation(bounds: RectF): StampAnnotation {
        val width = bounds.width()
        val height = bounds.height()

        // Mock a simple rectangular path slightly inset from bounds
        val pathInputs =
            listOf(
                PathPdfObject.PathInput(bounds.left + width / 4, bounds.top + height / 4),
                PathPdfObject.PathInput(bounds.right - width / 4, bounds.top + height / 4),
                PathPdfObject.PathInput(bounds.right - width / 4, bounds.bottom - height / 4),
                PathPdfObject.PathInput(bounds.left + width / 4, bounds.bottom - height / 4),
                PathPdfObject.PathInput(bounds.left + width / 4, bounds.top + height / 4),
            )

        val pathObject = PathPdfObject(Color.RED, 10f, pathInputs)
        return StampAnnotation(0, bounds, listOf(pathObject))
    }

    private fun obtainMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, action, x, y, 0)
    }
}
