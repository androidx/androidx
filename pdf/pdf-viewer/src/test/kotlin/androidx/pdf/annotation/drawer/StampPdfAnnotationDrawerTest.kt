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
import android.graphics.RectF
import androidx.core.graphics.transform
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.models.StampAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class StampPdfAnnotationDrawerTest {

    private lateinit var canvas: Canvas
    private lateinit var transform: Matrix
    private lateinit var fakePdfObjectDrawerFactory: FakePdfObjectDrawerFactory
    private lateinit var stampPdfAnnotationDrawer: StampPdfAnnotationDrawer

    @Before
    fun setUp() {
        canvas = Canvas()
        transform = Matrix()
        fakePdfObjectDrawerFactory = FakePdfObjectDrawerFactory()
        stampPdfAnnotationDrawer = StampPdfAnnotationDrawer(fakePdfObjectDrawerFactory)
    }

    @Test
    fun draw_withEmptyPdfObjects_doesNotCreateDrawers() {
        val stampAnnotation = createStampAnnotation(pdfObjects = emptyList())

        stampPdfAnnotationDrawer.draw(stampAnnotation, canvas, transform)

        assertThat(fakePdfObjectDrawerFactory.createdDrawers).isEmpty()
    }

    @Test
    fun draw_withSinglePdfObject_delegatesDrawingToCreatedDrawer() {
        val pathObject = createPathPdfObject()
        val stampAnnotation = createStampAnnotation(pdfObjects = listOf(pathObject))

        stampPdfAnnotationDrawer.draw(stampAnnotation, canvas, transform)

        val fakeDrawer = fakePdfObjectDrawerFactory.createdDrawers[pathObject]
        assertFakeDrawerInvocation(fakeDrawer, pathObject, canvas, transform)
    }

    @Test
    fun draw_withMultiplePdfObjects_delegatesDrawingToTheirRespectiveDrawers() {
        val pathObject1 = createPathPdfObject(color = 1)
        val pathObject2 = createPathPdfObject(color = 2)
        val stampAnnotation = createStampAnnotation(pdfObjects = listOf(pathObject1, pathObject2))

        stampPdfAnnotationDrawer.draw(stampAnnotation, canvas, transform)

        assertThat(fakePdfObjectDrawerFactory.createdDrawers).hasSize(2)

        // Verify drawer for pathObject1
        val fakeDrawer1 = fakePdfObjectDrawerFactory.createdDrawers[pathObject1]
        assertFakeDrawerInvocation(fakeDrawer1, pathObject1, canvas, transform)

        // Verify drawer for pathObject2
        val fakeDrawer2 = fakePdfObjectDrawerFactory.createdDrawers[pathObject2]
        assertFakeDrawerInvocation(fakeDrawer2, pathObject2, canvas, transform)
    }

    /**
     * Asserts that the given [FakePdfObjectDrawer] was created, invoked correctly once, and with
     * the expected parameters.
     */
    private fun assertFakeDrawerInvocation(
        fakeDrawer: FakePdfObjectDrawer<out PdfObject>?,
        expectedPdfObject: PdfObject,
        expectedCanvas: Canvas,
        expectedTransform: Matrix,
    ) {
        assertThat(fakeDrawer).isNotNull()
        fakeDrawer?.let { drawer ->
            assertThat(drawer.drawInvocations).hasSize(1)
            val invocation = drawer.drawInvocations.single()

            assertThat(invocation.pdfObject).isEqualTo(expectedPdfObject)
            assertThat(invocation.canvas).isSameInstanceAs(expectedCanvas)
            assertThat(invocation.transform).isEqualTo(expectedTransform)
        }
    }

    private companion object {
        fun createStampAnnotation(
            pageNum: Int = 0,
            bounds: RectF = RectF(0f, 0f, 10f, 10f),
            pdfObjects: List<PdfObject>,
        ): StampAnnotation = StampAnnotation(pageNum, bounds, pdfObjects)

        fun createPathPdfObject(
            color: Int = 1,
            width: Float = 2f,
            inputs: List<PathPdfObject.PathInput> = emptyList(),
        ): PathPdfObject = PathPdfObject(brushColor = color, brushWidth = width, inputs = inputs)
    }
}
