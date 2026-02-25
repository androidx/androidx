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

import androidx.pdf.DraftEditResult
import androidx.pdf.EditsDraft
import androidx.pdf.MutableEditsDraft
import androidx.pdf.PdfEditApplyException
import androidx.pdf.TestDraftEditOperation
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.MAX_BATCH_SIZE_IN_BYTES
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.unflatten
import androidx.pdf.service.FakePdfDocumentRemote
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class BatchPdfAnnotationsProcessorTest {
    private lateinit var processor: BatchPdfAnnotationsProcessor
    private lateinit var fakeRemoteDocument: FakePdfDocumentRemote

    @Before
    fun setUp() {
        fakeRemoteDocument = FakePdfDocumentRemote()
        processor = BatchPdfAnnotationsProcessor(fakeRemoteDocument)
    }

    @Test
    fun process_singleBatchSuccess_returnsAllIds() {
        val draft = createDraftWithOperations(count = 3)
        fakeRemoteDocument.setBehavior(DraftEditResult.Success(listOf("id0", "id1", "id2")))

        val result = mutableListOf<String>()
        processor.process(draft) { result.addAll(it.map { appliedEdit -> appliedEdit.editId }) }

        assertThat(result).containsExactly("id0", "id1", "id2").inOrder()
    }

    @Test
    fun process_multipleBatchesSuccess_returnsAllCombinedIds() {
        val numOperations = 5
        val draft =
            createDraftWithOperations(
                count = numOperations,
                simulatedSizePerOperation = MAX_BATCH_SIZE_IN_BYTES / numOperations,
            )

        // We need to configure the fake to handle sequential calls
        fakeRemoteDocument.setSequentialBehaviors(
            DraftEditResult.Success(listOf("id0", "id1", "id2", "id3")),
            DraftEditResult.Success(listOf("id4")),
        )

        val result = mutableListOf<String>()
        processor.process(draft) { result.addAll(it.map { appliedEdit -> appliedEdit.editId }) }

        assertThat(result).containsExactly("id0", "id1", "id2", "id3", "id4").inOrder()
    }

    @Test
    fun process_firstBatchFailure_throwsExceptionWithNoAppliedIds() {
        val draft = createDraftWithOperations(count = 3)
        fakeRemoteDocument.setBehavior(
            DraftEditResult.Failure(
                failedBatchIndex = 0,
                appliedIds = emptyList(),
                errorMessage = "Fail",
            )
        )

        val exception =
            assertThrows(PdfEditApplyException::class.java) { processor.process(draft) {} }

        assertThat(exception.failureIndex).isEqualTo(0)
        assertThat(exception.appliedEditIds).isEmpty()
    }

    @Test
    fun process_firstBatchPartialFailure_throwsExceptionWithPartialIds() {
        val draft = createDraftWithOperations(count = 3)
        // Batch fails at index 1 (2nd item)
        fakeRemoteDocument.setBehavior(
            DraftEditResult.Failure(
                failedBatchIndex = 1,
                appliedIds = listOf("id0"),
                errorMessage = "Fail",
            )
        )

        val exception =
            assertThrows(PdfEditApplyException::class.java) { processor.process(draft) {} }

        assertThat(exception.failureIndex).isEqualTo(1)
        assertThat(exception.appliedEditIds).containsExactly("id0")
    }

    @Test
    fun process_secondBatchFailure_throwsExceptionWithFirstBatchIds() {
        val randomNoise = 1000
        val numOperations = 10
        val numBatches = 2
        val draft =
            createDraftWithOperations(
                count = numOperations,
                simulatedSizePerOperation =
                    ((MAX_BATCH_SIZE_IN_BYTES * numBatches) - randomNoise) / numOperations,
            )

        val expectedIds = listOf("id0", "id1", "id2", "id3", "id4")

        fakeRemoteDocument.setSequentialBehaviors(
            DraftEditResult.Success(expectedIds),
            DraftEditResult.Failure(
                failedBatchIndex = 0,
                appliedIds = emptyList(),
                errorMessage = "Fail",
            ),
        )

        val exception =
            assertThrows(PdfEditApplyException::class.java) { processor.process(draft) {} }

        assertThat(exception.failureIndex).isEqualTo(5)
        assertThat(exception.appliedEditIds).isEqualTo(expectedIds)
    }

    @Test
    fun process_emptyList_returnsEmptyList() {
        val emptyDraft = MutableEditsDraft().toEditsDraft()
        val result = processor.process(emptyDraft) {}
        assertThat(result).isEmpty()
    }

    @Test
    fun unflatten_listUnderLimit_returnsSingleBatch() {
        // Create small item
        val item = TestDraftEditOperation("1", 100)
        val list = listOf(item)
        val limit = 200

        val batches = list.unflatten(limit)

        assertThat(batches).hasSize(1)
        assertThat(batches[0]).containsExactly(item)
    }

    @Test
    fun unflatten_listOverLimit_splitsIntoMultipleBatches() {
        // Items size 100 each, Limit 150 -> Should be 1 item per batch
        val item1 = TestDraftEditOperation("1", 100)
        val item2 = TestDraftEditOperation("2", 100)
        val list = listOf(item1, item2)
        val limit = 150

        val batches = list.unflatten(limit)

        assertThat(batches).hasSize(2)
        assertThat(batches[0]).containsExactly(item1)
        assertThat(batches[1]).containsExactly(item2)
    }

    @Test
    fun unflatten_itemExceedsLimit_omitsItem() {
        val smallItem = TestDraftEditOperation("small", 50)
        val hugeItem = TestDraftEditOperation("huge", 200)
        val list = listOf(smallItem, hugeItem)
        val limit = 100

        val batches = list.unflatten(limit)

        assertThat(batches).hasSize(1)
        assertThat(batches[0]).containsExactly(smallItem)
        // hugeItem is dropped
    }

    @Test
    fun unflatten_accumulatesItemsUntilLimit() {
        val item1 = TestDraftEditOperation("1", 30)
        val item2 = TestDraftEditOperation("2", 30)
        val item3 = TestDraftEditOperation("3", 30) // 120 total
        val list = listOf(item1, item2, item3)
        val limit = 100

        // Should fit item1(40) + item2(40) = 80 in batch 1.
        // item3(40) goes to batch 2.
        val batches = list.unflatten(limit)

        assertThat(batches).hasSize(2)
        assertThat(batches[0]).containsExactly(item1, item2)
        assertThat(batches[1]).containsExactly(item3)
    }

    private fun createDraftWithOperations(
        count: Int,
        simulatedSizePerOperation: Int = 100,
    ): EditsDraft {
        val draft = MutableEditsDraft()
        repeat(count) { i ->
            draft.addOperation(TestDraftEditOperation("id$i", simulatedSizePerOperation))
        }
        return draft.toEditsDraft()
    }
}
