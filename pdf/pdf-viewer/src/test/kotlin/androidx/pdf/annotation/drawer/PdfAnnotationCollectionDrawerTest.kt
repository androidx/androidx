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
import android.graphics.Matrix
import android.util.SparseArray
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.models.PdfAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfAnnotationCollectionDrawerTest {

    private lateinit var canvas: Canvas
    private lateinit var fakeAnnotationDrawerFactory: FakePdfAnnotationDrawerFactory
    private lateinit var pdfDocumentAnnotationsDrawerImpl: PdfDocumentAnnotationsDrawerImpl

    @Before
    fun setUp() {
        canvas = Canvas()
        fakeAnnotationDrawerFactory = FakePdfAnnotationDrawerFactory()
        pdfDocumentAnnotationsDrawerImpl =
            PdfDocumentAnnotationsDrawerImpl(fakeAnnotationDrawerFactory)
    }

    @Test
    fun draw_emptyPageData_noFactoryInteraction() {
        val emptyPageData = SparseArray<PageAnnotationsData>()
        pdfDocumentAnnotationsDrawerImpl.draw(pagesAnnotationData = emptyPageData, canvas = canvas)

        assertThat(fakeAnnotationDrawerFactory.creationLog).isEmpty()
        assertThat(fakeAnnotationDrawerFactory.createdDrawers).isEmpty()
    }

    @Test
    fun draw_pageDataWithNoAnnotations_noFactoryInteraction() {
        val pagesAnnotationDataWithEmptyAnnotations =
            SparseArray<PageAnnotationsData>().apply {
                put(0, PageAnnotationsData(annotations = emptyList(), transform = Matrix()))
            }
        pdfDocumentAnnotationsDrawerImpl.draw(
            pagesAnnotationData = pagesAnnotationDataWithEmptyAnnotations,
            canvas = canvas,
        )

        assertThat(fakeAnnotationDrawerFactory.creationLog).isEmpty()
        assertThat(fakeAnnotationDrawerFactory.createdDrawers).isEmpty()
    }

    @Test
    fun draw_singleAnnotationOnSinglePage_delegatesToFactoryAndDrawer() {
        val annotation = TestAnnotation(pageNum = 0)
        val pageTransform = Matrix().apply { setScale(2.0f, 2.0f) }
        val pagesAnnotationData =
            SparseArray<PageAnnotationsData>().apply {
                put(
                    0,
                    PageAnnotationsData(annotations = listOf(annotation), transform = pageTransform),
                )
            }

        pdfDocumentAnnotationsDrawerImpl.draw(
            pagesAnnotationData = pagesAnnotationData,
            canvas = canvas,
        )

        // Verify factory was called to create a drawer for the annotation
        assertThat(fakeAnnotationDrawerFactory.creationLog).containsExactly(annotation)

        // Verify the created drawer's draw method was called correctly
        val drawer = fakeAnnotationDrawerFactory.createdDrawers[annotation]
        assertThat(drawer).isNotNull()
        drawer?.assertDrawInvocation(
            expectedAnnotation = annotation,
            expectedCanvas = canvas,
            expectedTransform = pageTransform,
        )
    }

    @Test
    fun draw_multipleAnnotationsOnSinglePage_delegatesToFactoryAndDrawers() {
        val annotation1 = TestAnnotation(pageNum = 0)
        val annotation2 = TestAnnotation(pageNum = 0)
        val pageTransform = Matrix().apply { setTranslate(10f, 20f) }
        val pagesAnnotationData =
            SparseArray<PageAnnotationsData>().apply {
                put(
                    0,
                    PageAnnotationsData(
                        annotations = listOf(annotation1, annotation2),
                        transform = pageTransform,
                    ),
                )
            }

        pdfDocumentAnnotationsDrawerImpl.draw(
            pagesAnnotationData = pagesAnnotationData,
            canvas = canvas,
        )

        // Verify factory was called for each annotation in order
        assertThat(fakeAnnotationDrawerFactory.creationLog)
            .containsExactly(annotation1, annotation2)
            .inOrder()

        // Verify each created drawer was called correctly
        fakeAnnotationDrawerFactory.createdDrawers[annotation1]?.assertDrawInvocation(
            expectedAnnotation = annotation1,
            expectedCanvas = canvas,
            expectedTransform = pageTransform,
        )
        fakeAnnotationDrawerFactory.createdDrawers[annotation2]?.assertDrawInvocation(
            expectedAnnotation = annotation2,
            expectedCanvas = canvas,
            expectedTransform = pageTransform,
        )
    }

    @Test
    fun draw_annotationsAcrossMultiplePages_delegatesWithCorrectTransforms() {
        val annotationPage0 = TestAnnotation(pageNum = 0)
        val page0Transform = Matrix().apply { setTranslate(10f, 10f) }

        val annotationPage2First = TestAnnotation(pageNum = 2)
        val annotationPage2Second = TestAnnotation(pageNum = 2)
        val page2Transform =
            Matrix().apply {
                setScale(2f, 2f)
                postRotate(90f)
            }

        val pagesAnnotationData =
            SparseArray<PageAnnotationsData>().apply {
                put(
                    0,
                    PageAnnotationsData(
                        annotations = listOf(annotationPage0),
                        transform = page0Transform,
                    ),
                )
                // Page 1 has no annotations
                put(
                    1,
                    PageAnnotationsData(
                        annotations = emptyList(),
                        transform = Matrix().apply { setScale(1.5f, 1.5f) },
                    ),
                )
                put(
                    2,
                    PageAnnotationsData(
                        annotations = listOf(annotationPage2First, annotationPage2Second),
                        transform = page2Transform,
                    ),
                )
            }

        pdfDocumentAnnotationsDrawerImpl.draw(
            pagesAnnotationData = pagesAnnotationData,
            canvas = canvas,
        )

        // Verify factory was called for each annotation in the correct order
        assertThat(fakeAnnotationDrawerFactory.creationLog)
            .containsExactly(annotationPage0, annotationPage2First, annotationPage2Second)
            .inOrder()

        // Verify drawers were called with correct page-specific transforms
        fakeAnnotationDrawerFactory.createdDrawers[annotationPage0]?.assertDrawInvocation(
            expectedAnnotation = annotationPage0,
            expectedCanvas = canvas,
            expectedTransform = page0Transform,
        )
        fakeAnnotationDrawerFactory.createdDrawers[annotationPage2First]?.assertDrawInvocation(
            expectedAnnotation = annotationPage2First,
            expectedCanvas = canvas,
            expectedTransform = page2Transform,
        )
        fakeAnnotationDrawerFactory.createdDrawers[annotationPage2Second]?.assertDrawInvocation(
            expectedAnnotation = annotationPage2Second,
            expectedCanvas = canvas,
            expectedTransform = page2Transform,
        )
    }

    // --- Helper classes and methods ---

    private data class TestAnnotation(override val pageNum: Int) : PdfAnnotation(pageNum)

    /**
     * Fake implementation of [PdfAnnotationDrawerFactory] for testing. It records created drawers
     * and the annotations they were created for.
     */
    private class FakePdfAnnotationDrawerFactory : PdfAnnotationDrawerFactory {
        val createdDrawers = mutableMapOf<PdfAnnotation, FakePdfAnnotationDrawer<PdfAnnotation>>()
        val creationLog = mutableListOf<PdfAnnotation>()

        override fun create(pdfAnnotation: PdfAnnotation): PdfAnnotationDrawer<PdfAnnotation> {
            creationLog.add(pdfAnnotation)
            return FakePdfAnnotationDrawer<PdfAnnotation>().also {
                createdDrawers[pdfAnnotation] = it
            }
        }
    }

    /**
     * Fake implementation of [PdfAnnotationDrawer] for testing. It records the arguments of the
     * `draw` method invocations.
     */
    private class FakePdfAnnotationDrawer<T : PdfAnnotation> : PdfAnnotationDrawer<T> {
        data class DrawInvocation<T : PdfAnnotation>(
            val pdfAnnotation: T,
            val canvas: Canvas,
            val transform: Matrix,
        )

        val drawInvocations = mutableListOf<DrawInvocation<T>>()

        override fun draw(pdfAnnotation: T, canvas: Canvas, transform: Matrix) {
            drawInvocations.add(DrawInvocation(pdfAnnotation, canvas, transform))
        }

        /**
         * Asserts that this drawer's `draw` method was invoked exactly once with the expected
         * parameters.
         */
        fun assertDrawInvocation(
            expectedAnnotation: T,
            expectedCanvas: Canvas,
            expectedTransform: Matrix,
        ) {
            assertThat(drawInvocations).hasSize(1)
            val invocation = drawInvocations.single()
            assertThat(invocation.pdfAnnotation).isEqualTo(expectedAnnotation)
            assertThat(invocation.canvas).isSameInstanceAs(expectedCanvas)
            assertThat(invocation.transform).isEqualTo(expectedTransform)
        }
    }
}
