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

package androidx.pdf.annotation.models

import android.graphics.RectF
import android.os.Parcel
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StampAnnotationTest {

    @Test
    fun createStampAnnotation_parcellingUnparcelingFromPdfAnnotationParcel() {

        val expectedStampAnnotation = getSampleStampAnnotation()

        // Parcel the expected StampAnnotation object.
        val parcel = Parcel.obtain()
        expectedStampAnnotation.writeToParcel(parcel, 0)
        // Reset parcel data position for reading.
        parcel.setDataPosition(0)

        // Unparcel the object using PdfAnnotation.CREATOR
        val actualAnnotation = PdfAnnotation.CREATOR.createFromParcel(parcel)
        assert(actualAnnotation is StampAnnotation)

        // Assert that the unparceled annotation's properties match the original.
        if (actualAnnotation is StampAnnotation) {
            assertStampAnnotationEquals(expectedStampAnnotation, actualAnnotation)
        }
        parcel.recycle()
    }

    companion object {
        private fun getPathObject(): PathPdfObject {
            return PathPdfObject(
                brushColor = 255,
                brushWidth = 10f,
                inputs =
                    listOf(
                        PathPdfObject.PathInput(10f, 10f),
                        PathPdfObject.PathInput(20f, 20f),
                        PathPdfObject.PathInput(30f, 30f),
                        PathPdfObject.PathInput(40f, 40f),
                        PathPdfObject.PathInput(50f, 50f),
                    ),
            )
        }

        internal fun getSampleStampAnnotation(
            pageNum: Int = 0,
            bounds: RectF = RectF(0f, 0f, 100f, 100f),
        ): StampAnnotation {
            return StampAnnotation(pageNum, bounds, listOf(getPathObject()))
        }

        internal fun assertStampAnnotationEquals(
            expectedAnnotation: StampAnnotation,
            actualAnnotation: StampAnnotation,
        ) {
            assertEquals(expectedAnnotation.bounds, actualAnnotation.bounds)
            assertEquals(expectedAnnotation.pageNum, actualAnnotation.pageNum)
            // Assert that the list of PdfObjects within the annotation is the same
            assertEquals(expectedAnnotation.pdfObjects.size, actualAnnotation.pdfObjects.size)
            for (i in expectedAnnotation.pdfObjects.indices) {
                val actualPathPdfObject = actualAnnotation.pdfObjects[i] as PathPdfObject
                val expectedPathPdfObject = expectedAnnotation.pdfObjects[i] as PathPdfObject
                assertEquals(actualPathPdfObject.brushColor, expectedPathPdfObject.brushColor)
                assertEquals(actualPathPdfObject.brushWidth, expectedPathPdfObject.brushWidth)
                // Assert that the list of PathInputs within the PathPdfObject is the same
                assertEquals(actualPathPdfObject.inputs.size, expectedPathPdfObject.inputs.size)
                for (j in actualPathPdfObject.inputs.indices) {
                    val actualPathInput = actualPathPdfObject.inputs[j]
                    val expectedPathInput = expectedPathPdfObject.inputs[j]
                    assertEquals(actualPathInput.x, expectedPathInput.x)
                    assertEquals(actualPathInput.y, expectedPathInput.y)
                }
            }
        }
    }
}
