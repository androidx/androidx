/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.pdf.annotation.models.KeyedPdfObject
import androidx.pdf.annotation.randomizePathPdfObject
import androidx.pdf.models.PageObjectsProvider
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PageObjectsPaginatorTest {
    @Test
    fun test_getPageObjects_emptyResults_returnsNull() {
        val objectsProvider =
            object : PageObjectsProvider {
                override fun getPageObjects(pageNum: Int, types: Long): List<KeyedPdfObject> =
                    emptyList()
            }
        val pageObjectsPaginator = PageObjectsPaginator(pageNum = 0, objectsProvider, types = -1L)

        val results = pageObjectsPaginator.getPageObjects()

        assertThat(results).isNull()
    }

    @Test
    fun test_getPageObjects_singleBatch_returnsSingleTotalBatchCount() {
        val objectDataList = createKeyedPdfObjectList(numObjects = 1, pathLength = 10)
        val objectsProvider =
            object : PageObjectsProvider {
                override fun getPageObjects(pageNum: Int, types: Long): List<KeyedPdfObject> =
                    objectDataList
            }
        val pageObjectsPaginator = PageObjectsPaginator(pageNum = 0, objectsProvider, types = -1L)

        val results = pageObjectsPaginator.getPageObjects()

        assertThat(results).isNotNull()
        assertThat(results!!.objects).isEqualTo(objectDataList)
        assertThat(results.currentBatchIndex).isEqualTo(0)
        assertThat(results.totalBatchCount).isEqualTo(1)
    }

    @Test
    fun test_getPageObjects_multipleBatches_returnsCorrectTotalBatchCount() {
        // Large pathLength to trigger multiple batches
        // Each KeyedPdfObject will be approx 600KB:
        // - UUID Key: ~100 bytes
        // - PathPdfObject (Header + Color + Width + Size): 16 bytes
        // - 50,000 PathInputs (12 bytes each): 600,000 bytes
        // Total for 2 objects: ~1.2MB, which exceeds the 1MB MAX_BATCH_SIZE_IN_BYTES.
        val objectDataList = createKeyedPdfObjectList(numObjects = 2, pathLength = 50000)
        val objectsProvider =
            object : PageObjectsProvider {
                override fun getPageObjects(pageNum: Int, types: Long): List<KeyedPdfObject> =
                    objectDataList
            }
        val pageObjectsPaginator = PageObjectsPaginator(pageNum = 0, objectsProvider, types = -1L)

        val firstBatch = pageObjectsPaginator.getPageObjects(0)
        val secondBatch = pageObjectsPaginator.getPageObjects(1)

        assertThat(firstBatch).isNotNull()
        assertThat(firstBatch!!.currentBatchIndex).isEqualTo(0)
        assertThat(firstBatch.totalBatchCount).isAtLeast(2)

        assertThat(secondBatch).isNotNull()
        assertThat(secondBatch!!.currentBatchIndex).isEqualTo(1)
        assertThat(secondBatch.totalBatchCount).isAtLeast(2)

        val allObjects = firstBatch.objects + secondBatch.objects
        assertThat(allObjects).isNotEmpty()
    }

    private fun createKeyedPdfObjectList(numObjects: Int, pathLength: Int): List<KeyedPdfObject> {
        return List(numObjects) { createDummyKeyedPdfObject(pathLength) }
    }

    private fun createDummyKeyedPdfObject(pathLength: Int): KeyedPdfObject {
        val pdfObject = randomizePathPdfObject(pathLength)
        return KeyedPdfObject(key = UUID.randomUUID().toString(), pdfObject)
    }
}
