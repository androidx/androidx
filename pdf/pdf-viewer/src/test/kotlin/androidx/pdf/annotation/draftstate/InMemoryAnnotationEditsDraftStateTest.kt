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

package androidx.pdf.annotation.draftstate

import android.graphics.RectF
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.annotation.models.StampAnnotationTest.Companion.getSampleStampAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class InMemoryAnnotationEditsDraftStateTest {

    private lateinit var draftState: InMemoryAnnotationEditsDraftState

    @Before
    fun setUp() {
        draftState = InMemoryAnnotationEditsDraftState()
    }

    @Test
    fun addDraftAnnotation_returnsUniqueHandleId() {
        val pageNum = 0
        val annotation = getSampleStampAnnotation(pageNum)

        val handleId1 = draftState.addDraftAnnotation(annotation)
        val handleId2 = draftState.addDraftAnnotation(annotation)

        assertThat(handleId1).isNotEmpty()
        assertThat(handleId2).isNotEmpty()
        assertThat(handleId1).isNotEqualTo(handleId2)
    }

    @Test
    fun getDraftAnnotation_validHandle_returnsCorrectAnnotation() {
        val pageNum = 1
        val expectedAnnotation = getSampleStampAnnotation(pageNum)
        val handleId = draftState.addDraftAnnotation(expectedAnnotation)

        val retrieved = draftState.getDraftAnnotation(pageNum, handleId)

        assertThat(retrieved).isNotNull()
        assertStampAnnotationEquals(expectedAnnotation, retrieved as StampAnnotation)
    }

    @Test
    fun getDraftAnnotation_invalidHandle_returnsNull() {
        // Page exists but handle does not
        val pageNum = 0
        draftState.addDraftAnnotation(getSampleStampAnnotation(pageNum))

        val retrieved = draftState.getDraftAnnotation(pageNum, "non_existent_handle")
        assertThat(retrieved).isNull()
    }

    @Test
    fun getDraftAnnotation_nonExistentPage_returnsNull() {
        val retrieved = draftState.getDraftAnnotation(99, "any_handle")
        assertThat(retrieved).isNull()
    }

    @Test
    fun getDraftAnnotations_returnsKeyedAnnotationsInInsertionOrder() {
        val pageNum = 0
        val bounds1 = RectF(0f, 0f, 10f, 10f)
        val bounds2 = RectF(20f, 20f, 30f, 30f)
        val annot1 = getSampleStampAnnotation(pageNum, bounds1)
        val annot2 = getSampleStampAnnotation(pageNum, bounds2)

        val handle1 = draftState.addDraftAnnotation(annot1)
        val handle2 = draftState.addDraftAnnotation(annot2)

        val results = draftState.getDraftAnnotations(pageNum)

        assertThat(results).hasSize(2)

        // Verify Order
        assertThat(results[0].key).isEqualTo(handle1)
        assertStampAnnotationEquals(annot1, results[0].annotation as StampAnnotation)

        assertThat(results[1].key).isEqualTo(handle2)
        assertStampAnnotationEquals(annot2, results[1].annotation as StampAnnotation)
    }

    @Test
    fun getDraftAnnotations_emptyPage_returnsEmptyList() {
        assertThat(draftState.getDraftAnnotations(99)).isEmpty()
    }

    @Test
    fun updateDraftAnnotation_updatesAndReturnsOldAnnotation() {
        val pageNum = 0
        val originalBounds = RectF(0f, 0f, 10f, 10f)
        val original = getSampleStampAnnotation(pageNum, originalBounds)
        val handle = draftState.addDraftAnnotation(original)

        val updatedBounds = RectF(50f, 50f, 60f, 60f)
        val updated = getSampleStampAnnotation(pageNum, updatedBounds)

        // Act
        val returnedOld = draftState.updateDraftAnnotation(pageNum, handle, updated)

        // Assert return value is the old one
        assertStampAnnotationEquals(original, returnedOld as StampAnnotation)

        // Assert state is updated
        val storedNew = draftState.getDraftAnnotation(pageNum, handle)
        assertStampAnnotationEquals(updated, storedNew as StampAnnotation)
    }

    @Test(expected = NoSuchElementException::class)
    fun updateDraftAnnotation_invalidHandle_throwsException() {
        val pageNum = 0
        // Ensure page map exists
        draftState.addDraftAnnotation(getSampleStampAnnotation(pageNum))

        val updatePayload = getSampleStampAnnotation(pageNum)
        draftState.updateDraftAnnotation(pageNum, "fake_id", updatePayload)
    }

    @Test(expected = NoSuchElementException::class)
    fun updateDraftAnnotation_nonExistentPage_throwsException() {
        draftState.updateDraftAnnotation(5, "any_id", getSampleStampAnnotation(5))
    }

    @Test
    fun removeAnnotation_removesAndReturnsAnnotation() {
        val pageNum = 0
        val annot = getSampleStampAnnotation(pageNum)
        val handle = draftState.addDraftAnnotation(annot)

        // Ensure it exists
        assertThat(draftState.getDraftAnnotations(pageNum)).hasSize(1)

        // Act
        val removed = draftState.removeAnnotation(pageNum, handle)

        // Assert
        assertStampAnnotationEquals(annot, removed as StampAnnotation)
        assertThat(draftState.getDraftAnnotation(pageNum, handle)).isNull()
        assertThat(draftState.getDraftAnnotations(pageNum)).isEmpty()
    }

    @Test(expected = NoSuchElementException::class)
    fun removeAnnotation_invalidHandle_throwsException() {
        val pageNum = 0
        draftState.addDraftAnnotation(getSampleStampAnnotation(pageNum))

        draftState.removeAnnotation(pageNum, "non_existent_handle")
    }

    @Test(expected = NoSuchElementException::class)
    fun removeAnnotation_nonExistentPage_throwsException() {
        draftState.removeAnnotation(99, "any_id")
    }

    private fun assertStampAnnotationEquals(
        expectedAnnotation: StampAnnotation,
        actualAnnotation: StampAnnotation,
    ) {
        assertThat(actualAnnotation.bounds).isEqualTo(expectedAnnotation.bounds)
        assertThat(actualAnnotation.pageNum).isEqualTo(expectedAnnotation.pageNum)
        assertThat(actualAnnotation.pdfObjects).hasSize(expectedAnnotation.pdfObjects.size)
        for (i in expectedAnnotation.pdfObjects.indices) {
            val actualPathPdfObject = actualAnnotation.pdfObjects[i] as PathPdfObject
            val expectedPathPdfObject = expectedAnnotation.pdfObjects[i] as PathPdfObject
            assertThat(actualPathPdfObject.brushColor).isEqualTo(expectedPathPdfObject.brushColor)
            assertThat(actualPathPdfObject.inputs).hasSize(expectedPathPdfObject.inputs.size)
            for (j in actualPathPdfObject.inputs.indices) {
                val actualPathInput = actualPathPdfObject.inputs[j]
                val expectedPathInput = expectedPathPdfObject.inputs[j]
                assertThat(actualPathInput.x).isEqualTo(expectedPathInput.x)
                assertThat(actualPathInput.y).isEqualTo(expectedPathInput.y)
            }
        }
    }
}
