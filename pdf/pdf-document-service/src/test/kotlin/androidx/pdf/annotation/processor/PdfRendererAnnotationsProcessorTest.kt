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

package androidx.pdf.annotation.processor

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.DraftEditResult
import androidx.pdf.InsertDraftEditOperation
import androidx.pdf.PdfDocument.Companion.LINEARIZATION_STATUS_NOT_LINEARIZED
import androidx.pdf.RemoveDraftEditOperation
import androidx.pdf.UpdateDraftEditOperation
import androidx.pdf.adapter.FakePdfDocumentRenderer
import androidx.pdf.annotation.createStampAnnotationWithPath
import androidx.pdf.annotation.models.PdfAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfRendererAnnotationsProcessorTest {
    private lateinit var fakeRenderer: FakePdfDocumentRenderer
    private lateinit var processor: PdfRendererAnnotationsProcessor

    @Before
    fun setUp() {
        fakeRenderer =
            FakePdfDocumentRenderer(
                linearizationStatus = LINEARIZATION_STATUS_NOT_LINEARIZED,
                pageCount = 10,
                formType = 0,
            )
        processor = PdfRendererAnnotationsProcessor(fakeRenderer)
    }

    @Test
    fun process_emptyList_returnsSuccessWithEmptyIds() {
        val result = processor.process(emptyList())

        assertThat(result).isInstanceOf(DraftEditResult.Success::class.java)
        assertThat((result as DraftEditResult.Success).ids).isEmpty()
    }

    @Test
    fun process_singleInsertSuccess_returnsSuccessWithId() {
        val pageNum = 0
        val annotation = createFakeAnnotation(pageNum)
        val operation = InsertDraftEditOperation(annotation)

        val result = processor.process(listOf(operation))

        assertThat(result).isInstanceOf(DraftEditResult.Success::class.java)
        val success = result as DraftEditResult.Success
        assertThat(success.ids).containsExactly("1000")

        val page = fakeRenderer.fakePagesMap[pageNum]!!
        assertThat(page.addedAnnotations).hasSize(1)
    }

    @Test
    fun process_multipleOperationsSuccess_returnsSuccessWithOrderedIds() {
        val insertOp = InsertDraftEditOperation(createFakeAnnotation(pageNum = 0))
        val updateOp = UpdateDraftEditOperation("1000", createFakeAnnotation(pageNum = 1))
        val removeOp = RemoveDraftEditOperation("2000", pageNum = 2)

        val result = processor.process(listOf(insertOp, updateOp, removeOp))

        assertThat(result).isInstanceOf(DraftEditResult.Success::class.java)
        val success = result as DraftEditResult.Success
        assertThat(success.ids).containsExactly("1000", "1000", "2000").inOrder()

        assertThat(fakeRenderer.fakePagesMap[0]!!.addedAnnotations).hasSize(1)
        assertThat(fakeRenderer.fakePagesMap[1]!!.updatedAnnotations).hasSize(1)
        assertThat(fakeRenderer.fakePagesMap[2]!!.removedAnnotationIds).containsExactly(2000)
    }

    @Test
    fun process_insertFailsWithNullReturn_returnsFailureWithIndex() {
        val pageNum = 0
        val operation = InsertDraftEditOperation(createFakeAnnotation(pageNum))

        val page = fakeRenderer.fakePagesMap[pageNum]!!
        page.shouldFailInsert = true

        val result = processor.process(listOf(operation))

        assertThat(result).isInstanceOf(DraftEditResult.Failure::class.java)
        val failure = result as DraftEditResult.Failure
        assertThat(failure.failedBatchIndex).isEqualTo(0)
        assertThat(failure.appliedIds).isEmpty()
        assertThat(failure.errorMessage).contains("Failed to add annotation")
    }

    @Test
    fun process_updateFailsWithException_returnsFailureWithIndex() {
        val pageNum = 1
        val operation = UpdateDraftEditOperation("100", createFakeAnnotation(pageNum))

        val page = fakeRenderer.fakePagesMap[pageNum]!!
        page.exceptionToThrow = RuntimeException("Native Error")

        val result = processor.process(listOf(operation))

        assertThat(result).isInstanceOf(DraftEditResult.Failure::class.java)
        val failure = result as DraftEditResult.Failure
        assertThat(failure.failedBatchIndex).isEqualTo(0)
        assertThat(failure.errorMessage).contains("Native Error")
    }

    @Test
    fun process_sequentialOperations_stopsAtFirstFailure_returnsFailureWithPartialIds() {
        val op1 = InsertDraftEditOperation(createFakeAnnotation(pageNum = 0))

        val op2 = InsertDraftEditOperation(createFakeAnnotation(pageNum = 1))
        fakeRenderer.fakePagesMap[1]!!.shouldFailInsert = true

        val op3 = InsertDraftEditOperation(createFakeAnnotation(pageNum = 2))

        val result = processor.process(listOf(op1, op2, op3))

        assertThat(result).isInstanceOf(DraftEditResult.Failure::class.java)
        val failure = result as DraftEditResult.Failure

        assertThat(failure.failedBatchIndex).isEqualTo(1)

        assertThat(failure.appliedIds).containsExactly("1000")

        assertThat(fakeRenderer.fakePagesMap[2]!!.addedAnnotations).isEmpty()
    }

    private fun createFakeAnnotation(pageNum: Int): PdfAnnotation {
        return createStampAnnotationWithPath(pageNum, pathSize = 10)
    }
}
