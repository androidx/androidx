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

package androidx.pdf.annotation.operations

import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.operations.KeyedAnnotationOperation.OperationType
import androidx.pdf.annotation.registry.FakeAnnotationHandleRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SessionAnnotationOperationsTrackerTest {

    private lateinit var registry: FakeAnnotationHandleRegistry
    private lateinit var tracker: SessionAnnotationOperationsTracker

    // Mocks/Fakes
    private val annotationA: PdfAnnotation = mock()
    private val annotationB: PdfAnnotation = mock()

    private val keyA = "key_A"
    private val keyB = "key_B"

    @Before
    fun setup() {
        registry = FakeAnnotationHandleRegistry()
        tracker = SessionAnnotationOperationsTracker(registry)
    }

    // region 1. BASIC CRUD & SQUASHING LOGIC

    @Test
    fun addEntry_addOperation_shouldBeRecorded() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)

        val snapshot = tracker.getSnapshot()
        assertThat(snapshot).hasSize(1)

        val op = snapshot.first()
        assertThat(op.operationType).isEqualTo(OperationType.ADD)
        assertThat(op.keyedAnnotation.key).isEqualTo(keyA)
    }

    @Test
    fun addEntry_addThenUpdate_shouldSquashToSingleAdd() {
        // 1. Add A
        tracker.addEntry(OperationType.ADD, keyA, annotationA)

        // 2. Update A with content B
        tracker.addEntry(OperationType.UPDATE, keyA, annotationB)

        val snapshot = tracker.getSnapshot()

        // Assert: Squashed to 1 entry
        assertThat(snapshot).hasSize(1)
        // Assert: Type is still ADD
        assertThat(snapshot[0].operationType).isEqualTo(OperationType.ADD)
        // Assert: Content is now B
        assertThat(snapshot[0].keyedAnnotation.annotation).isEqualTo(annotationB)
    }

    @Test
    fun addEntry_addThenRemove_shouldSquashToSingleRemoveEntry() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)

        val snapshot = tracker.getSnapshot()
        assertThat(snapshot).isNotEmpty()
        assertThat(snapshot.size).isEqualTo(1)
        assertThat(snapshot[0].operationType).isEqualTo(OperationType.REMOVE)
    }

    @Test
    fun addEntry_updateThenUpdate_shouldSquashToSingleUpdate() {
        // Assume A existed on server, so we start with an UPDATE
        tracker.addEntry(OperationType.UPDATE, keyA, annotationA)
        tracker.addEntry(OperationType.UPDATE, keyA, annotationB)

        val snapshot = tracker.getSnapshot()

        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(OperationType.UPDATE)
        assertThat(snapshot[0].keyedAnnotation.annotation).isEqualTo(annotationB)
    }

    @Test
    fun addEntry_updateThenRemove_shouldSquashToRemove() {
        tracker.addEntry(OperationType.UPDATE, keyA, annotationA)
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)

        val snapshot = tracker.getSnapshot()

        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(OperationType.REMOVE)
    }

    @Test
    fun addEntry_removeThenAdd_shouldResultInAdd() {
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        tracker.addEntry(OperationType.ADD, keyA, annotationB)

        val snapshot = tracker.getSnapshot()

        assertThat(snapshot).hasSize(1)
        assertThat(snapshot[0].operationType).isEqualTo(OperationType.ADD)
        assertThat(snapshot[0].keyedAnnotation.annotation).isEqualTo(annotationB)
    }

    // endregion

    // region 2. ORDERING (Z-INDEX)

    @Test
    fun getSnapshot_multipleAdditions_shouldPreserveInsertionOrder() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        tracker.addEntry(OperationType.ADD, keyB, annotationB)

        val snapshot = tracker.getSnapshot()

        assertThat(snapshot).hasSize(2)
        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(keyA) // Bottom
        assertThat(snapshot[1].keyedAnnotation.key).isEqualTo(keyB) // Top
    }

    @Test
    fun addEntry_updateExistingItem_shouldMoveToEnd() {
        // Setup: A, then B
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        tracker.addEntry(OperationType.ADD, keyB, annotationB)

        // Act: Update A
        tracker.addEntry(OperationType.UPDATE, keyA, annotationA)

        val snapshot = tracker.getSnapshot()

        // Assert: A is now after B
        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(keyB)
        assertThat(snapshot[1].keyedAnnotation.key).isEqualTo(keyA)
    }

    @Test
    fun addEntry_addRemoveReAdd_shouldMoveToEnd() {
        // Setup: A, then B
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        tracker.addEntry(OperationType.ADD, keyB, annotationB)

        // Remove A (A is gone)
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)

        // Re-add A (Should be new top)
        tracker.addEntry(OperationType.ADD, keyA, annotationA)

        val snapshot = tracker.getSnapshot()

        assertThat(snapshot[0].keyedAnnotation.key).isEqualTo(keyB)
        assertThat(snapshot[1].keyedAnnotation.key).isEqualTo(keyA)
    }

    // endregion

    // region 3. INVALID TRANSITIONS

    @Test
    fun addEntry_addThenAdd_shouldThrowIllegalStateException() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)

        val exception =
            assertThrows(IllegalStateException::class.java) {
                tracker.addEntry(OperationType.ADD, keyA, annotationA)
            }

        assertThat(exception).hasMessageThat().contains("Cannot transition from ADD to ADD")
    }

    @Test
    fun addEntry_removeThenUpdate_shouldThrowIllegalStateException() {
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)

        assertThrows(IllegalStateException::class.java) {
            tracker.addEntry(OperationType.UPDATE, keyA, annotationA)
        }
    }

    @Test
    fun addEntry_removeThenRemove_shouldThrowIllegalStateException() {
        // Assuming your Enum logic defines REMOVE -> REMOVE as invalid
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)

        assertThrows(IllegalStateException::class.java) {
            tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        }
    }

    // endregion

    // region 4. CONCURRENCY

    @Test
    fun addEntry_concurrentWrites_shouldRemainConsistent() = runBlocking {
        val opsCount = 1000

        // Launch 1000 coroutines running in parallel IO dispatcher
        val jobs =
            (1..opsCount).map { i ->
                async(Dispatchers.IO) {
                    // Each thread adds a unique key
                    tracker.addEntry(OperationType.ADD, "key_$i", annotationA)
                }
            }

        jobs.awaitAll()

        val snapshot = tracker.getSnapshot()
        assertThat(snapshot).hasSize(opsCount)
    }

    @Test
    fun getSnapshot_concurrentReadWrite_shouldNotThrowConcurrentModificationException() {
        runBlocking {
            // This test tries to crash the tracker by reading while writing
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

            // Writer
            jobs +=
                async(Dispatchers.IO) {
                    repeat(100) { i -> tracker.addEntry(OperationType.ADD, "key_$i", annotationA) }
                }

            // Reader
            jobs +=
                async(Dispatchers.IO) {
                    repeat(100) {
                        // This iterates the values, which often causes crashes if not synchronized
                        tracker.getSnapshot()
                    }
                }

            jobs.awaitAll()
            // If we reach here without crashing, the test passes
        }
    }

    @Test
    fun isDeleted_nonExistentKey_returnsFalse() {
        assertThat(tracker.isDeleted("unknown")).isFalse()
    }

    @Test
    fun isDeleted_afterRemoveOperation_returnsTrue() {
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        assertThat(tracker.isDeleted(keyA)).isTrue()
    }

    @Test
    fun isDeleted_afterAddThenRemove_returnsTrue() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        assertThat(tracker.isDeleted(keyA)).isTrue()
    }

    @Test
    fun isDeleted_afterUpdateThenRemove_returnsTrue() {
        tracker.addEntry(OperationType.UPDATE, keyA, annotationA)
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        assertThat(tracker.isDeleted(keyA)).isTrue()
    }

    @Test
    fun getUpdatedContent_afterUpdateOperation_returnsNewContent() {
        tracker.addEntry(OperationType.UPDATE, keyA, annotationB)
        assertThat(tracker.getUpdatedAnnotation(keyA)).isEqualTo(annotationB)
    }

    @Test
    fun getUpdatedContent_afterAddOperation_returnsNull() {
        tracker.addEntry(OperationType.ADD, keyA, annotationA)
        assertThat(tracker.getUpdatedAnnotation(keyA)).isNull()
    }

    @Test
    fun getUpdatedContent_afterRemoveOperation_returnsNull() {
        tracker.addEntry(OperationType.REMOVE, keyA, annotationA)
        assertThat(tracker.getUpdatedAnnotation(keyA)).isNull()
    }
}
