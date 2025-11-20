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

import androidx.pdf.annotation.createPdfAnnotationDataList
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.unflatten
import androidx.pdf.service.FakePdfDocumentRemote
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class BatchPdfAnnotationsProcessorTest {
    @Test
    fun test_process_singleBatchSuccessfully_returnsAnnotationResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations = createPdfAnnotationDataList(numAnnots = 1, pathLength = 10)

        // Act
        val result = processor.process(annotations)

        // Assert
        assertThat(result.success.size).isEqualTo(1)
        assertThat(result.success[0].editId).isEqualTo(annotations[0].editId)
    }

    @Test
    fun test_process_multipleBatchesSuccessfully_returnsAnnotationResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations = createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000)

        // Act
        val result = processor.process(annotations)

        // Assert
        assertThat(result.success.size).isEqualTo(2)
        assertThat(result.success[0].editId).isEqualTo(annotations[0].editId)
        assertThat(result.success[1].editId).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_process_multipleBatchesWithSingleFailure_returnsAnnotationResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 0.5f)

        // Act
        val result = processor.process(annotations)

        // Assert
        assertThat(result.success.size).isEqualTo(1)
        assertThat(result.failures.size).isEqualTo(1)
        assertThat(result.failures[0]).isEqualTo(annotations[0].annotation)
        assertThat(result.success[0].editId).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_process_multipleBatchesWithAllFailures_returnsAnnotationResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 1f)

        // Act
        val result = processor.process(annotations)

        // Assert
        assertThat(result.failures.size).isEqualTo(2)
        assertThat(result.failures[0]).isEqualTo(annotations[0].annotation)
        assertThat(result.failures[1]).isEqualTo(annotations[1].annotation)
    }

    @Test
    fun test_processAddEdit_multipleBatchesWithSingleFailure_returnsAnnotationResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)

        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 0.5f)

        // Act
        val result = processor.processAddEdits(annotations)

        // Assert
        assertThat(result.success.size).isEqualTo(1)
        assertThat(result.failures.size).isEqualTo(1)
        assertThat(result.failures[0]).isEqualTo(annotations[0].editId)
        assertThat(result.success[0].jetpackId).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_processAddEdits_multipleBatchesWithAllFailures_returnsAddEditResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 1f)

        // Act
        val result = processor.processAddEdits(annotations)

        // Assert
        assertThat(result.failures.size).isEqualTo(2)
        assertThat(result.failures[0]).isEqualTo(annotations[0].editId)
        assertThat(result.failures[1]).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_processUpdateEdits_multipleBatchesWithSingleFailure_returnsModifyEditResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)

        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 0.5f)

        // Act
        val result = processor.processAddEdits(annotations)

        // Assert
        assertThat(result.success.size).isEqualTo(1)
        assertThat(result.failures.size).isEqualTo(1)
        assertThat(result.failures[0]).isEqualTo(annotations[0].editId)
        assertThat(result.success[0].jetpackId).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_processUpdateEdits_multipleBatchesWithAllFailures_returnsModifyEditResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)
        val annotations =
            createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 1f)

        // Act
        val result = processor.processUpdateEdits(annotations)

        // Assert
        assertThat(result.failures.size).isEqualTo(2)
        assertThat(result.failures[0]).isEqualTo(annotations[0].editId)
        assertThat(result.failures[1]).isEqualTo(annotations[1].editId)
    }

    @Test
    fun test_processRemoveEdits_multipleBatchesWithSingleFailure_returnsModifyEditResult() {
        // Arrange
        val remoteDocument = FakePdfDocumentRemote()
        val processor = BatchPdfAnnotationsProcessor(remoteDocument)

        val editIds =
            listOf(
                EditId(-1, "invalid-edit"),
                EditId(1, "editId1"),
                EditId(2, "editId1"),
                EditId(3, "editId1"),
            )
        createPdfAnnotationDataList(numAnnots = 2, pathLength = 10000, invalidRatio = 0.5f)

        // Act
        val result = processor.processRemoveEdits(editIds)

        // Assert
        assertThat(result.success.size).isEqualTo(3)
        assertThat(result.failures.size).isEqualTo(1)
        assertThat(result.failures[0]).isEqualTo(editIds[0])
        assertThat(result.success[0]).isEqualTo(editIds[1])
        assertThat(result.success[1]).isEqualTo(editIds[2])
        assertThat(result.success[2]).isEqualTo(editIds[3])
    }

    @Test
    fun test_unflatten_singleBatch_returnsSingleSublist() {
        // Arrange
        val maxSizeInBytes = 1200
        val annotationDataList = createPdfAnnotationDataList(numAnnots = 1, pathLength = 10)

        // Act
        val result = annotationDataList.unflatten(maxSizeInBytes)

        // Assert
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo(annotationDataList)
    }

    @Test
    fun test_unflatten_multipleBatches_returnsMultipleSublists() {
        // Arrange
        val maxSizeInBytes = 2500
        val annotationDataList = createPdfAnnotationDataList(numAnnots = 3, pathLength = 10)

        // Act
        val result = annotationDataList.unflatten(maxSizeInBytes)

        // Assert
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].size).isEqualTo(2)
        assertThat(result[0]).isEqualTo(listOf(annotationDataList[0], annotationDataList[1]))
        assertThat(result[1].size).isEqualTo(1)
        assertThat(result[1]).isEqualTo(listOf(annotationDataList[2]))
    }

    @Test
    fun test_unflatten_exactSizeLimit_returnsSingleSublist() {
        // Arrange
        val maxSizeInBytes = 1072
        val annotationDataList = createPdfAnnotationDataList(numAnnots = 1, pathLength = 10)

        // Act
        val result = annotationDataList.unflatten(maxSizeInBytes)

        // Assert
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo(annotationDataList)
    }

    @Test
    fun test_unflatten_firstItemExceedsLimit_returnsSingleItemSublist() {
        // Arrange
        val maxSizeInBytes = 1000
        val annotationDataList = createPdfAnnotationDataList(numAnnots = 2, pathLength = 10)

        // Act
        val result = annotationDataList.unflatten(maxSizeInBytes)

        // Assert
        assertThat(result.size).isEqualTo(0)
    }
}
