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

package androidx.pdf.annotation.manager

import androidx.pdf.annotation.AnnotationHandleIdGenerator.composeAnnotationId
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.draftstate.FakeAnnotationEditsDraftState
import androidx.pdf.annotation.models.TestPdfAnnotation
import androidx.pdf.annotation.operations.FakeAnnotationOperationsTracker
import androidx.pdf.annotation.operations.KeyedAnnotationOperation
import androidx.pdf.annotation.registry.FakeAnnotationHandleRegistry
import androidx.pdf.annotation.repository.FakeAnnotationsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfDocumentAnnotationsManagerTest {

    private lateinit var manager: PdfDocumentAnnotationsManager
    private lateinit var draftState: FakeAnnotationEditsDraftState
    private lateinit var repository: FakeAnnotationsRepository
    private lateinit var handleRegistry: FakeAnnotationHandleRegistry
    private lateinit var operationsTracker: FakeAnnotationOperationsTracker

    private val pageNum = 0

    // Test Data
    private val annotA = TestPdfAnnotation(pageNum = pageNum) // Basic Content A
    private val annotB = TestPdfAnnotation(pageNum = pageNum) // Basic Content B
    private val updatedAnnotA = TestPdfAnnotation(pageNum = pageNum) // Updated Content A

    @Before
    fun setup() {
        draftState = FakeAnnotationEditsDraftState()
        repository = FakeAnnotationsRepository()
        handleRegistry = FakeAnnotationHandleRegistry()
        operationsTracker = FakeAnnotationOperationsTracker()

        manager =
            PdfDocumentAnnotationsManager(draftState, repository, handleRegistry, operationsTracker)
    }

    @Test
    fun getAnnotations_noData_returnsEmptyList() = runTest {
        val result = manager.getAnnotations(pageNum)
        assertThat(result).isEmpty()
    }

    @Test
    fun getAnnotations_onlyPersisted_returnsMappedAnnotations() = runTest {
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))

        val result = manager.getAnnotations(pageNum)

        assertThat(result).hasSize(1)
        assertThat(result[0].annotation).isEqualTo(annotA)
        // Verify a handle was generated
        val handleId = handleRegistry.getHandleId(pageNum, sourceId)
        assertThat(result[0].key).isEqualTo(handleId)
    }

    @Test
    fun getAnnotations_onlyDrafts_returnsDraftAnnotations() = runTest {
        manager.addAnnotation(annotB) // This adds to draft state internally

        val result = manager.getAnnotations(pageNum)

        assertThat(result).hasSize(1)
        assertThat(result[0].annotation).isEqualTo(annotB)
    }

    @Test
    fun getAnnotations_persistedAndDrafts_returnsPersistedThenDrafts() = runTest {
        // Setup Persisted
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))

        // Setup Draft
        manager.addAnnotation(annotB)

        val result = manager.getAnnotations(pageNum)

        assertThat(result).hasSize(2)
        // Order Verification: Persisted (A) then Draft (B)
        assertThat(result[0].annotation).isEqualTo(annotA)
        assertThat(result[1].annotation).isEqualTo(annotB)
    }

    @Test
    fun removeAnnotation_persistedAnnotation_addsTombstoneToTracker() = runTest {
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))

        // We need the handle ID to compose the ID passed to removeAnnotation
        val composedId = handleRegistry.getHandleId(pageNum, sourceId)

        val removed = manager.removeAnnotation(composedId)

        assertThat(removed).isEqualTo(annotA)

        // Verify Tracker has REMOVE op
        val snapshot = operationsTracker.getSnapshot()
        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType)
            .isEqualTo(KeyedAnnotationOperation.OperationType.REMOVE)
        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(composedId)

        // Verify getAnnotations hides it
        val visible = manager.getAnnotations(pageNum)
        assertThat(visible).isEmpty()
    }

    @Test
    fun removeAnnotation_persistedWithLocalUpdate_removesUpdatedContent() = runTest {
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))
        val composedId = handleRegistry.getHandleId(pageNum, sourceId)

        // Update first
        manager.updateAnnotation(composedId, updatedAnnotA)

        // Remove
        val removed = manager.removeAnnotation(composedId)

        // Should return the UPDATED content, not original
        assertThat(removed).isEqualTo(updatedAnnotA)

        // Verify Tracker has REMOVE op
        assertThat(operationsTracker.isDeleted(composedId)).isTrue()
    }

    @Test
    fun updateAnnotation_persistedAnnotation_updatesTrackerAndReconciles() = runTest {
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))
        val composedId = handleRegistry.getHandleId(pageNum, sourceId)

        val previous = manager.updateAnnotation(composedId, updatedAnnotA)

        assertThat(previous).isEqualTo(annotA)

        // Verify Tracker
        val updatedContent = operationsTracker.getUpdatedAnnotation(composedId)
        assertThat(updatedContent).isEqualTo(updatedAnnotA)

        // Verify getAnnotations returns updated content with same handle
        val result = manager.getAnnotations(pageNum)
        assertThat(result).hasSize(1)
        assertThat(result[0].key).isEqualTo(composedId)
        assertThat(result[0].annotation).isEqualTo(updatedAnnotA)
    }

    @Test
    fun updateAnnotation_persistedAnnotationMultipleTimes_returnsLastUpdatedValue() = runTest {
        val sourceId = "source_1"
        repository.seedAnnotations(pageNum, listOf(KeyedPdfAnnotation(sourceId, annotA)))
        val composedId = handleRegistry.getHandleId(pageNum, sourceId)

        manager.updateAnnotation(composedId, updatedAnnotA)
        val previous2 = manager.updateAnnotation(composedId, annotB)

        // The previous value for the second update should be the result of the first update
        assertThat(previous2).isEqualTo(updatedAnnotA)

        // Final state check
        val result = manager.getAnnotations(pageNum)
        assertThat(result[0].annotation).isEqualTo(annotB)
    }

    @Test
    fun removeAnnotation_invalidId_throwsException() = runTest {
        // Not in draft, not in registry/repo
        val invalidId = composeAnnotationId(pageNum, id = "fake_handle")

        assertThrows(Exception::class.java) { runBlocking { manager.removeAnnotation(invalidId) } }
    }

    @Test
    fun updateAnnotation_invalidId_throwsException() = runTest {
        val invalidId = composeAnnotationId(pageNum, id = "fake_handle")

        assertThrows(Exception::class.java) {
            runBlocking { manager.updateAnnotation(invalidId, annotA) }
        }
    }

    @Test
    fun addAnnotation_pdfAnnotation_addsToDraftAndTracker() = runTest {
        val draftId = manager.addAnnotation(annotA)

        // Verify it was added to draft state
        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        assertThat(draftAnnotations).hasSize(1)
        assertThat(draftAnnotations[0].key).isEqualTo(draftId)
        assertThat(draftAnnotations[0].annotation).isEqualTo(annotA)

        // Verify ADD operation was logged in tracker
        val snapshot = operationsTracker.getSnapshot()
        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(KeyedAnnotationOperation.OperationType.ADD)
        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(draftId)
        assertThat(snapshot[0].keyedAnnotation.annotation).isEqualTo(annotA)
    }

    @Test
    fun addAnnotation_keyedPdfAnnotation_newKey_addsToDraftAndTracker() = runTest {
        // A KeyedAnnotation with a key that is NOT in the registry (effectively a new draft)
        val newKey = "new_draft_id"
        val keyedAnnot = KeyedPdfAnnotation(newKey, annotA)

        val returnedId = manager.addAnnotation(keyedAnnot)

        // Verify it delegates to draft state to ensure ID uniqueness/registration
        // (The manager implementation handles checking registry and falling back to draft)
        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        assertThat(draftAnnotations).hasSize(1)
        assertThat(draftAnnotations[0].annotation).isEqualTo(annotA)
        // The returned ID might differ if the draft state regenerates it,
        // but typically it respects the input or returns the valid handle.
        assertThat(returnedId).isEqualTo(draftAnnotations[0].key)

        // Verify ADD operation logged
        val snapshot = operationsTracker.getSnapshot()
        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(KeyedAnnotationOperation.OperationType.ADD)
        assertThat(snapshot[0].keyedAnnotation.annotation).isEqualTo(annotA)
    }

    @Test
    fun addAnnotation_keyedPdfAnnotation_existingKey_addsToTrackerOnly() = runTest {
        // Simulate a scenario where we are re-adding a persisted annotation (e.g. Undo delete)
        val sourceId = "source_1"
        // Pre-register this ID so the manager knows it's an existing handle
        val handleId = handleRegistry.getHandleId(pageNum, sourceId)

        val keyedAnnot = KeyedPdfAnnotation(handleId, annotA)

        val returnedId = manager.addAnnotation(keyedAnnot)

        // It should NOT add to draft state because the registry recognized the ID
        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        assertThat(draftAnnotations).isEmpty()

        // It MUST add to tracker (e.g. to reverse a previous delete)
        val snapshot = operationsTracker.getSnapshot()
        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(KeyedAnnotationOperation.OperationType.ADD)
        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(handleId)
        assertThat(returnedId).isEqualTo(handleId)
    }

    @Test
    fun discardChanges_singleExistingAnnotation_clearsRegistryAndRepository() = runTest {
        val sourceId = "source_1"
        val keyedAnnotA = KeyedPdfAnnotation(sourceId, annotA)

        repository.seedAnnotations(pageNum, listOf(keyedAnnotA))
        val handleId = handleRegistry.getHandleId(pageNum, sourceId)

        // Validate that the annotation is added to the repository
        val pageAnnotations = manager.getAnnotations(pageNum)
        val maskedKeyedAnnotA = KeyedPdfAnnotation(handleId, annotA)
        assertThat(pageAnnotations.size).isEqualTo(1)
        assertThat(pageAnnotations[0]).isEqualTo(maskedKeyedAnnotA)

        // Act
        manager.discardChanges()

        // Validate that the annotation is removed from the repository
        assertThat(repository.getAnnotationsForPage(pageNum)).isEmpty()
        assertThat(handleRegistry.getSourceId(handleId)).isNull()
    }

    @Test
    fun discardChanges_singleExistingAnnotationIsUpdated_clearsTracker() = runTest {
        val sourceId = "source_1"
        val keyedAnnotA = KeyedPdfAnnotation(sourceId, annotA)

        repository.seedAnnotations(pageNum, listOf(keyedAnnotA))
        val handleId = handleRegistry.getHandleId(pageNum, sourceId)

        // Validate that the annotation is added to the repository
        val pageAnnotations = manager.getAnnotations(pageNum)
        val maskedKeyedAnnotA = KeyedPdfAnnotation(handleId, annotA)
        assertThat(pageAnnotations.size).isEqualTo(1)
        assertThat(pageAnnotations[0]).isEqualTo(maskedKeyedAnnotA)

        // Update the annotation
        manager.updateAnnotation(handleId, updatedAnnotA)

        // Validate operations tracker
        assertThat(operationsTracker.getUpdatedAnnotation(handleId)).isEqualTo(updatedAnnotA)

        // Act
        manager.discardChanges()

        // Validate that the annotation is removed from the repository
        assertThat(repository.getAnnotationsForPage(pageNum)).isEmpty()
        assertThat(handleRegistry.getSourceId(handleId)).isNull()
        assertThat(operationsTracker.getUpdatedAnnotation(handleId)).isNull()
    }

    @Test
    fun discardChanges_singleDraftAnnotation_clearsDraftState() = runTest {
        val newKey = "new_draft_id"
        val keyedAnnot = KeyedPdfAnnotation(newKey, annotA)

        val returnedId = manager.addAnnotation(keyedAnnot)

        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        assertThat(draftAnnotations).hasSize(1)
        assertThat(draftAnnotations[0]).isEqualTo(keyedAnnot)
        assertThat(returnedId).isEqualTo(draftAnnotations[0].key)

        // Act
        manager.discardChanges()

        assertThat(draftState.getDraftAnnotations(pageNum)).isEmpty()
    }

    @Test
    fun discardChanges_singleNewAnnot_singleExistingAnnotationIsUpdated_clearsAll() = runTest {
        // New annotation
        val newKey = "new_draft_id"
        val keyedAnnot = KeyedPdfAnnotation(newKey, annotA)

        val returnedId = manager.addAnnotation(keyedAnnot)

        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        assertThat(draftAnnotations).hasSize(1)
        assertThat(draftAnnotations[0]).isEqualTo(keyedAnnot)
        assertThat(returnedId).isEqualTo(draftAnnotations[0].key)

        // Existing annotation key
        val sourceId = "source_1"
        val keyedAnnotB = KeyedPdfAnnotation(sourceId, annotB)

        repository.seedAnnotations(pageNum, listOf(keyedAnnotB))
        val handleId = handleRegistry.getHandleId(pageNum, sourceId)

        // Validate that the annotation is added to the repository
        val pageAnnotations = manager.getAnnotations(pageNum)
        val maskedKeyedAnnotB = KeyedPdfAnnotation(handleId, annotB)
        assertThat(pageAnnotations.size).isEqualTo(2)
        assertThat(pageAnnotations[0]).isEqualTo(maskedKeyedAnnotB)

        // Update the annotation
        manager.updateAnnotation(handleId, updatedAnnotA)

        // Act
        manager.discardChanges()

        // Assert
        assertThat(repository.getAnnotationsForPage(pageNum)).isEmpty()
        assertThat(handleRegistry.getSourceId(handleId)).isNull()
        assertThat(operationsTracker.getUpdatedAnnotation(handleId)).isNull()
        assertThat(draftState.getDraftAnnotations(pageNum)).isEmpty()
    }
}
