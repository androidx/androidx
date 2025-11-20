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

package androidx.pdf.manager

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.FakeEditablePdfDocument
import androidx.pdf.adapter.FakePdfDocumentRenderer
import androidx.pdf.adapter.FakePdfPage
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.annotation.createStampAnnotationWithPath
import androidx.pdf.annotation.manager.InMemoryAnnotationsManager
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditOperation
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.annotation.processor.PdfRendererAnnotationsProcessor
import androidx.pdf.util.createDummyUri
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class InMemoryAnnotationsManagerTest {

    private lateinit var fakeUri: Uri
    private lateinit var fakeDocument: FakeEditablePdfDocument
    private lateinit var annotationsManager: InMemoryAnnotationsManager

    private lateinit var pdfRendererAnnotationsProcessor: PdfRendererAnnotationsProcessor
    private lateinit var pdfDocumentRenderer: PdfDocumentRenderer

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    @Before
    fun setUp() {
        fakeUri = createDummyUri("test.pdf")
        fakeDocument = FakeEditablePdfDocument(uri = fakeUri, pageCount = 10)
        pdfDocumentRenderer =
            FakePdfDocumentRenderer(isLinearized = false, pageCount = 10, formType = 1)
        pdfRendererAnnotationsProcessor = PdfRendererAnnotationsProcessor(pdfDocumentRenderer)
        annotationsManager =
            InMemoryAnnotationsManager(::getEditsForPage, pdfRendererAnnotationsProcessor)
    }

    @Test
    fun getAnnotationsForPage_noAnnotations_returnsEmptyList() = runTest {
        val annotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotations).isEmpty()
    }

    @Test
    fun getAnnotationsForPage_existingAnnotations_returnsAnnotationsFromDocument() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        val annotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].editId.pageNum).isEqualTo(0)
    }

    @Test
    fun getAnnotationsForPage_invokeGetTwice_fetchesOnlyOnce() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        fakeDocument.addAnnotationToPage(1, existingAnnotation)

        // Call twice
        annotationsManager.getAnnotationsForPage(1)
        val annotations = annotationsManager.getAnnotationsForPage(1)

        assertThat(annotations).hasSize(1)
        assertThat(fakeDocument.getAnnotationsForPageCallCount[1]).isEqualTo(1)
    }

    @Test
    fun getAnnotationsForPage_concurrentCallsSamePage_fetchesOnlyOnce() = runTest {
        val pageNum = 2
        fakeDocument.addAnnotationToPage(pageNum, createStampAnnotationWithPath(pageNum, 1))

        val manager =
            InMemoryAnnotationsManager(
                { page ->
                    delay(100)
                    fakeDocument.getEditsForPage<PdfAnnotationData>(page).map { it.annotation }
                },
                pdfRendererAnnotationsProcessor,
            )

        val deferred1 = async { manager.getAnnotationsForPage(pageNum) }
        val deferred2 = async { manager.getAnnotationsForPage(pageNum) }
        awaitAll(deferred1, deferred2)

        assertThat(fakeDocument.getAnnotationsForPageCallCount[pageNum]).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getAnnotationsForPage_concurrentCallsDifferentPages_notBlocked() = runTest {
        val pageNum1 = 3
        val pageNum2 = 4
        fakeDocument.addAnnotationToPage(pageNum1, createStampAnnotationWithPath(pageNum1, 1))
        fakeDocument.addAnnotationToPage(pageNum2, createStampAnnotationWithPath(pageNum2, 1))

        val manager =
            InMemoryAnnotationsManager(
                { page ->
                    delay(100)
                    fakeDocument.getEditsForPage<PdfAnnotationData>(page).map { it.annotation }
                },
                pdfRendererAnnotationsProcessor,
            )

        val deferred1 = async { manager.getAnnotationsForPage(pageNum1) }
        val deferred2 = async { manager.getAnnotationsForPage(pageNum2) }
        awaitAll(deferred1, deferred2)

        assertThat(fakeDocument.getAnnotationsForPageCallCount[pageNum1]).isEqualTo(1)
        assertThat(fakeDocument.getAnnotationsForPageCallCount[pageNum2]).isEqualTo(1)
        assertThat(testScheduler.currentTime).isLessThan(200)
    }

    @Test
    fun addAnnotation_addsToDraftState_andIsReturnedByGetAnnotations() = runTest {
        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId = annotationsManager.addAnnotation(newAnnotation)
        val annotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].editId).isEqualTo(editId)
        assertThat(fakeDocument.getAnnotationsForPageCallCount[0]).isEqualTo(1)
    }

    @Test
    fun addAnnotation_existingAndAdded_returnsBoth() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        // Fetch existing first to populate cache and draft
        annotationsManager.getAnnotationsForPage(0)

        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        annotationsManager.addAnnotation(newAnnotation)

        val allAnnotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(allAnnotations).hasSize(2)
    }

    @Test
    fun addAnnotation_returnsUniqueEditId() {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val annotation2 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val editId2 = annotationsManager.addAnnotation(annotation2)

        assertThat(editId1).isNotNull()
        assertThat(editId2).isNotNull()
        assertThat(editId1).isNotEqualTo(editId2)
    }

    @Test
    fun getSnapshot_empty_returnsEmptyState() = runTest {
        val snapshot = annotationsManager.getSnapshot()
        assertThat(snapshot.editsByPage).isEmpty()
    }

    @Test
    fun getFullAnnotationStateSnapshot_withAnnotations_returnsCorrectState() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        fakeDocument.addAnnotationToPage(0, existingAnnotation)
        val existingAnnots = annotationsManager.getAnnotationsForPage(0)
        val editId = annotationsManager.addAnnotation(newAnnotation)

        val snapshot = annotationsManager.getSnapshot()
        val allEdits = snapshot.editsByPage

        assertThat(existingAnnots).hasSize(1)
        assertThat(allEdits.size).isEqualTo(1) // Only single entry in the map
        assertThat(allEdits[0]).isNotNull()
        assertThat(allEdits[0]).hasSize(2)
        assertThat(allEdits[0]!!.map { it.id }).contains(editId)
    }

    @Test
    fun getAnnotationsForPage_multiplePages_annotationsAreSeparate() = runTest {
        val annotationPage0 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val annotationPage1 = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val newAnnotationPage0 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, annotationPage0)
        fakeDocument.addAnnotationToPage(1, annotationPage1)

        // Get page 0, then add to page 0
        val annotations0Initial = annotationsManager.getAnnotationsForPage(0)
        val editId = annotationsManager.addAnnotation(newAnnotationPage0)

        // Get page 1
        val annotations1 = annotationsManager.getAnnotationsForPage(1)
        val annotations0Final = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations0Initial.size).isEqualTo(1)
        assertThat(annotations1.size).isEqualTo(1)
        assertThat(annotations0Final.size).isEqualTo(2)
        assertThat(annotations0Final.map { it.editId.value }).contains(editId.value)
    }

    @Test
    fun removeAnnotation_removesAnnotation() = runTest {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val annotationsInitial = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsInitial.size).isEqualTo(1)

        annotationsManager.removeAnnotation(editId1)
        val annotationsFinal = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsFinal.size).isEqualTo(0)
    }

    @Test
    fun removeAnnotation_editIdNotPresent() = runTest {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val annotationsInitial = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsInitial.size).isEqualTo(1)

        annotationsManager.removeAnnotation(editId1)

        assertThrows(NoSuchElementException::class.java) {
            annotationsManager.removeAnnotation(editId1)
        }
    }

    @Test
    fun updateAnnotation_updatesAnnotation() = runTest {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val annotation2 = createStampAnnotationWithPath(pageNum = 0, pathSize = 20)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val annotationsInitial = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsInitial.size).isEqualTo(1)

        annotationsManager.updateAnnotation(editId1, annotation2)
        val annotationsFinal = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsFinal.size).isEqualTo(1)
        assertThat(annotationsFinal[0].editId).isEqualTo(editId1)
        assertThat((annotationsFinal[0].annotation as StampAnnotation).pdfObjects.size)
            .isEqualTo(20)
    }

    @Test
    fun updateAnnotation_editIdNotPresent() = runTest {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val annotationsInitial = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsInitial.size).isEqualTo(1)

        annotationsManager.removeAnnotation(editId1)

        assertThrows(NoSuchElementException::class.java) {
            annotationsManager.updateAnnotation(editId1, annotation1)
        }
    }

    @Test
    fun clearUncommittedEdits_withNoUncommittedEdits_doesNotChangeAnnotations() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        // Fetch existing annotations to populate the cache and draft state.
        val initialAnnotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(initialAnnotations).hasSize(1)

        annotationsManager.clearUncommittedEdits()

        val annotationsAfterClear = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsAfterClear).hasSize(1)
        assertThat(annotationsAfterClear[0].annotation).isEqualTo(existingAnnotation)
    }

    @Test
    fun clearUncommittedEdits_withAddedAnnotation_removesTheAddedAnnotation() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        // Fetch existing annotations to populate the cache and draft state.
        annotationsManager.getAnnotationsForPage(0)

        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 15)
        annotationsManager.addAnnotation(newAnnotation)

        var annotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotations).hasSize(2)

        annotationsManager.clearUncommittedEdits()

        annotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].annotation).isEqualTo(existingAnnotation)
        assertThat((annotations[0].annotation as StampAnnotation).pdfObjects.size).isEqualTo(10)
    }

    @Test
    fun clearUncommittedEdits_withRemovedExistingAnnotation_restoresTheAnnotation() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        // Fetch and cache
        val initialAnnotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(initialAnnotations).hasSize(1)
        val editIdToRemove = initialAnnotations[0].editId

        // Remove the existing annotation (this is an uncommitted edit)
        annotationsManager.removeAnnotation(editIdToRemove)
        assertThat(annotationsManager.getAnnotationsForPage(0)).isEmpty()

        annotationsManager.clearUncommittedEdits()

        val annotationsAfterClear = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsAfterClear).hasSize(1)
        assertThat(annotationsAfterClear[0].annotation).isEqualTo(existingAnnotation)
    }

    @Test
    fun clearUncommittedEdits_withUpdatedExistingAnnotation_revertsTheUpdate() = runTest {
        val originalAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, originalAnnotation)

        // Fetch and cache
        val initialAnnotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(initialAnnotations).hasSize(1)
        val editIdToUpdate = initialAnnotations[0].editId

        // Update the existing annotation (this is an uncommitted edit)
        val updatedAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 20)
        annotationsManager.updateAnnotation(editIdToUpdate, updatedAnnotation)

        val annotationsAfterUpdate = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsAfterUpdate).hasSize(1)
        assertThat(annotationsAfterUpdate[0].annotation).isEqualTo(updatedAnnotation)

        annotationsManager.clearUncommittedEdits()

        val annotationsAfterClear = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsAfterClear).hasSize(1)
        assertThat(annotationsAfterClear[0].annotation).isEqualTo(originalAnnotation)
    }

    @Test
    fun clearUncommittedEdits_withNoExistingAnnotations_clearsAllAddedAnnotations() = runTest {
        val newAnnotationPage0 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        annotationsManager.addAnnotation(newAnnotationPage0)

        val newAnnotationPage1 = createStampAnnotationWithPath(pageNum = 1, pathSize = 5)
        annotationsManager.addAnnotation(newAnnotationPage1)

        // Initial fetch to populate caches and make document aware
        var annotationsPage0 = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsPage0).hasSize(1)
        var annotationsPage1 = annotationsManager.getAnnotationsForPage(1)
        assertThat(annotationsPage1).hasSize(1)

        annotationsManager.clearUncommittedEdits()

        annotationsPage0 = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotationsPage0).isEmpty()
        annotationsPage1 = annotationsManager.getAnnotationsForPage(1)
        assertThat(annotationsPage1).isEmpty()
    }

    @Test
    fun commitEdits_noEdits_successfullyCommitted() = runTest {
        assertThat(annotationsManager.operationsToCommit).isEmpty()
        annotationsManager.commitEdits()
        assertThat(annotationsManager.operationsToCommit).isEmpty()
    }

    @Test
    fun addEdits_addMultipleEdits_operationsToCommitUpdated() = runTest {
        val operationCount = 5
        repeat(operationCount) {
            val annotationsCount = Random.nextInt(1, 20)
            repeat(annotationsCount) {
                val annotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
                annotationsManager.addAnnotation(annotation)
            }

            val operationsToCommit = annotationsManager.operationsToCommit
            assertThat(operationsToCommit.size).isEqualTo(annotationsCount)
            operationsToCommit.forEach { assertThat(it.op).isEqualTo(EditOperation.Add) }
            annotationsManager.commitEdits()
            assertThat(annotationsManager.operationsToCommit).isEmpty()
        }
    }

    @Test
    fun performEdits_addRandomEdits_operationsToCommitUpdated() = runTest {
        val operationCount = 10
        val pageNum = 0
        val existingAnnotations = mutableListOf<PdfAnnotationData>()

        repeat(10) {
            val annotation =
                createStampAnnotationWithPath(pageNum = pageNum, pathSize = Random.nextInt(5, 15))
            val editId = annotationsManager.addAnnotation(annotation)
            existingAnnotations.add(PdfAnnotationData(editId, annotation))
        }
        assertThat(annotationsManager.operationsToCommit.size).isEqualTo(10)
        annotationsManager.commitEdits()
        assertThat(annotationsManager.operationsToCommit).isEmpty()

        var expectedOperationsToCommit = 0

        repeat(operationCount) {
            val operationType = Random.nextInt(3) // 0: Add, 1: Remove, 2: Update

            when (operationType) {
                0 -> { // Add
                    val annotation =
                        createStampAnnotationWithPath(
                            pageNum = pageNum,
                            pathSize = Random.nextInt(5, 15),
                        )
                    annotationsManager.addAnnotation(annotation)
                    expectedOperationsToCommit++
                }
                1 -> { // Remove
                    if (annotationsManager.getAnnotationsForPage(pageNum).isNotEmpty()) {
                        val annotationToRemove =
                            annotationsManager.getAnnotationsForPage(pageNum).random()
                        annotationsManager.removeAnnotation(annotationToRemove.editId)
                        expectedOperationsToCommit++
                    }
                }
                2 -> { // Update
                    if (annotationsManager.getAnnotationsForPage(pageNum).isNotEmpty()) {
                        val annotationToUpdate =
                            annotationsManager.getAnnotationsForPage(pageNum).random()
                        val updatedAnnotation =
                            createStampAnnotationWithPath(
                                pageNum = pageNum,
                                pathSize = Random.nextInt(5, 15),
                            )
                        annotationsManager.updateAnnotation(
                            annotationToUpdate.editId,
                            updatedAnnotation,
                        )
                        expectedOperationsToCommit++
                    }
                }
            }
            assertThat(annotationsManager.operationsToCommit.size)
                .isEqualTo(expectedOperationsToCommit)
        }
        annotationsManager.commitEdits()
        assertThat(annotationsManager.operationsToCommit).isEmpty()
    }

    @Test
    fun commitEdits_addEditsGetEdits_successfullyCommitted() = runTest {
        val annotation1 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        annotationsManager.addAnnotation(annotation1)
        val annotation2 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        annotationsManager.addAnnotation(annotation2)

        annotationsManager.commitEdits()

        val pdfPage = pdfDocumentRenderer.openPage(1, true) as FakePdfPage
        val actualAnnotations = pdfPage.getPageAnnotations()
        assertThat(actualAnnotations.size).isEqualTo(2)
        pdfPage.annotationsOperation.forEach {
            assertThat(it.operation).isEqualTo(EditOperation.Add)
        }
    }

    @Test
    fun commitEdits_orderOfOperations_successfullyCommitted() = runTest {
        val annotation1 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        val editId1 = annotationsManager.addAnnotation(annotation1)
        annotationsManager.removeAnnotation(editId1)
        val annotation2 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        val editId2 = annotationsManager.addAnnotation(annotation2)
        val annotation3 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        annotationsManager.updateAnnotation(editId2, annotation3)

        annotationsManager.commitEdits()

        val pdfPage = pdfDocumentRenderer.openPage(1, true) as FakePdfPage
        val actualAnnotations = pdfPage.getPageAnnotations()
        assertThat(actualAnnotations.size).isEqualTo(1)
        assertThat(pdfPage.annotationsOperation.size).isEqualTo(4)
        assertThat(pdfPage.annotationsOperation[0].operation).isEqualTo(EditOperation.Add)
        assertThat(pdfPage.annotationsOperation[1].operation).isEqualTo(EditOperation.Add)
        assertThat(pdfPage.annotationsOperation[2].operation).isEqualTo(EditOperation.Update)
        assertThat(pdfPage.annotationsOperation[3].operation).isEqualTo(EditOperation.Remove)
    }

    @Test
    fun commitEdits_throwException_aospIdNotPresent() = runTest {
        val annotation1 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))

        assertThrows(NoSuchElementException::class.java) {
            annotationsManager.removeAnnotation(EditId(1, "non existent id"))
        }
        assertThrows(NoSuchElementException::class.java) {
            annotationsManager.updateAnnotation(EditId(1, "non existent id"), annotation1)
        }
    }

    @Test
    fun commitEdits_verifyAospIds_successfullyCommitted() = runTest {
        val annotation1 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        val annotation2 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        val annotation3 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))
        val annotationTemp3 =
            createStampAnnotationWithPath(pageNum = 1, pathSize = Random.nextInt(5, 15))

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val editId2 = annotationsManager.addAnnotation(annotation2)
        val editId3 = annotationsManager.addAnnotation(annotationTemp3)
        annotationsManager.updateAnnotation(editId3, annotation3)
        annotationsManager.commitEdits()

        // Assert three add and one update operation
        val pdfPage = pdfDocumentRenderer.openPage(1, true) as FakePdfPage
        val actualAnnotations = pdfPage.getPageAnnotations()
        assertThat(actualAnnotations.size).isEqualTo(3)
        assertThat(pdfPage.annotationsOperation.size).isEqualTo(4)

        val annotations = listOf(annotation1, annotation2, annotation3)
        val editIds = listOf(editId1, editId2, editId3)

        // Remove and commit one by one and assert the state
        editIds.forEachIndexed { i, editId ->
            val pdfPage = pdfDocumentRenderer.openPage(1, true) as FakePdfPage
            val actualAnnotation = annotationsManager.removeAnnotation(editId)
            annotationsManager.commitEdits()
            val actualAnnotations = pdfPage.getPageAnnotations()

            // Check operation is added
            assertThat(pdfPage.annotationsOperation.size).isEqualTo(4 + (i + 1))
            assertThat(pdfPage.annotationsOperation[4 + i].operation)
                .isEqualTo(EditOperation.Remove)

            // Check annotation is removed
            assertThat(actualAnnotations.size).isEqualTo(3 - (i + 1))
            assertThat(actualAnnotation).isEqualTo(annotations[i])
        }
    }

    @Test
    fun commitEdits_purgesModifiedPagesCache() = runTest {
        val annotation1 =
            createStampAnnotationWithPath(pageNum = 2, pathSize = Random.nextInt(5, 15))
        val annotation2 =
            createStampAnnotationWithPath(pageNum = 3, pathSize = Random.nextInt(5, 15))
        val annotation3 =
            createStampAnnotationWithPath(pageNum = 4, pathSize = Random.nextInt(5, 15))

        for (i in 1..5) {
            annotationsManager.existingAnnotationsPerPage[i] = listOf(annotation1, annotation2)
        }

        annotationsManager.addAnnotation(annotation1)
        val removeAospId = annotationsManager.addAnnotation(annotation2)
        annotationsManager.removeAnnotation(removeAospId)
        val updateAospId = annotationsManager.addAnnotation(annotation3)
        annotationsManager.updateAnnotation(updateAospId, annotation2)

        // check earlier cache exists
        for (i in 1..5) {
            assertThat(annotationsManager.existingAnnotationsPerPage[i]).isNotEmpty()
        }

        annotationsManager.commitEdits()

        // check cache purged for pages with operations
        assertThat(annotationsManager.existingAnnotationsPerPage[1]).isNotEmpty()
        for (i in 2..4) {
            assertThat(annotationsManager.existingAnnotationsPerPage[i]).isNull()
        }
        assertThat(annotationsManager.existingAnnotationsPerPage[5]).isNotEmpty()
    }

    @Test
    fun commitEdits_randomEdits_purgesOnlyModifiedPagesCache() = runTest {
        val pageCount = 5
        val modifiedPages = mutableSetOf<Int>()

        // Pre-populate and cache annotations for all pages
        (0 until pageCount).forEach { pageNum ->
            val annotation = createStampAnnotationWithPath(pageNum, 5)
            annotationsManager.addAnnotation(annotation)
            annotationsManager.commitEdits()
            annotationsManager.getAnnotationsForPage(pageNum) // This will cache the annotations
            assertThat(annotationsManager.existingAnnotationsPerPage[pageNum]).isNotNull()
        }

        // Perform 20 random edits
        repeat(20) {
            val pageNum = Random.nextInt(pageCount)
            modifiedPages.add(pageNum)

            val operationType = Random.nextInt(3) // 0: Add, 1: Remove, 2: Update
            val annotationsOnPage = annotationsManager.annotationEditsDraftState.getEdits(pageNum)
            when (operationType) {
                0 -> { // Add
                    val annotation = createStampAnnotationWithPath(pageNum, Random.nextInt(5, 15))
                    annotationsManager.addAnnotation(annotation)
                }
                1 -> { // Remove
                    if (annotationsOnPage.isNotEmpty()) {
                        val annotationToRemove = annotationsOnPage.random()
                        annotationsManager.removeAnnotation(annotationToRemove.editId)
                    }
                }
                2 -> { // Update
                    if (annotationsOnPage.isNotEmpty()) {
                        val annotationToUpdate = annotationsOnPage.random()
                        val updatedAnnotation =
                            createStampAnnotationWithPath(pageNum, Random.nextInt(5, 15))
                        annotationsManager.updateAnnotation(
                            annotationToUpdate.editId,
                            updatedAnnotation,
                        )
                    }
                }
            }

            // Commit the edits
            annotationsManager.commitEdits()

            // Verify caches of modified pages are purged, without affecting other pages.
            (0 until pageCount).forEach { pageNum ->
                if (modifiedPages.contains(pageNum)) {
                    assertThat(annotationsManager.existingAnnotationsPerPage[pageNum]).isNull()
                } else {
                    assertThat(annotationsManager.existingAnnotationsPerPage[pageNum]).isNotNull()
                }
            }
        }
    }

    private suspend fun getEditsForPage(pageNum: Int): List<PdfAnnotation> =
        fakeDocument.getEditsForPage<PdfAnnotationData>(pageNum).map { it.annotation }
}
