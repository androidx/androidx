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
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.PageAnnotationsProvider
import androidx.pdf.annotation.createKeyedPdfAnnotationList
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PageAnnotationPaginatorTest {
    @Test
    fun test_getPageAnnotations_emptyResults_returnsNull() {
        val annotationsProvider =
            object : PageAnnotationsProvider {
                override fun getPageAnnotations(pageNum: Int): List<KeyedPdfAnnotation> =
                    emptyList()
            }
        val pageAnnotationsPaginator = PageAnnotationsPaginator(pageNum = 0, annotationsProvider)

        val results = pageAnnotationsPaginator.getPageAnnotations()

        assertThat(results).isNull()
    }

    @Test
    fun test_getPageAnnotations_singleBatch_returnsSingleTotalBatchCount() {
        val annotationDataList = createKeyedPdfAnnotationList(numAnnots = 1, pathLength = 10)
        val annotationsProvider =
            object : PageAnnotationsProvider {
                override fun getPageAnnotations(pageNum: Int): List<KeyedPdfAnnotation> =
                    annotationDataList
            }
        val pageAnnotationsPaginator = PageAnnotationsPaginator(pageNum = 0, annotationsProvider)

        val results = pageAnnotationsPaginator.getPageAnnotations()

        assertThat(results).isNotNull()
        assertThat(results!!.annotations).isEqualTo(annotationDataList)
        assertThat(results.currentBatchIndex).isEqualTo(0)
        assertThat(results.totalBatchCount).isEqualTo(1)
    }

    @Test
    fun test_getPageAnnotations_multipleBatches_returnsCorrectTotalBatchCount() {
        val annotationDataList = createKeyedPdfAnnotationList(numAnnots = 2, pathLength = 5000)
        val annotationsProvider =
            object : PageAnnotationsProvider {
                override fun getPageAnnotations(pageNum: Int): List<KeyedPdfAnnotation> =
                    annotationDataList
            }
        val pageAnnotationsPaginator = PageAnnotationsPaginator(pageNum = 0, annotationsProvider)

        val firstBatch = pageAnnotationsPaginator.getPageAnnotations(0)
        val secondBatch = pageAnnotationsPaginator.getPageAnnotations(1)

        val expectedFirstBatchAnnotations = listOf(annotationDataList[0])
        val expectedSecondBatchAnnotations = listOf(annotationDataList[1])

        assertThat(firstBatch).isNotNull()
        assertThat(firstBatch!!.annotations).isEqualTo(expectedFirstBatchAnnotations)
        assertThat(firstBatch.currentBatchIndex).isEqualTo(0)
        assertThat(firstBatch.totalBatchCount).isEqualTo(2)

        assertThat(secondBatch).isNotNull()
        assertThat(secondBatch!!.annotations).isEqualTo(expectedSecondBatchAnnotations)
        assertThat(secondBatch.currentBatchIndex).isEqualTo(1)
        assertThat(secondBatch.totalBatchCount).isEqualTo(2)
    }
}
