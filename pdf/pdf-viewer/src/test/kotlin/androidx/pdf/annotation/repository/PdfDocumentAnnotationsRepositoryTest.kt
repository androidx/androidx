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

package androidx.pdf.annotation.repository

import android.graphics.Point
import androidx.pdf.FakePdfDocument
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.TestPdfAnnotation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfDocumentAnnotationsRepositoryTest {

    private fun createAnnotation(key: String): KeyedPdfAnnotation {
        val content = TestPdfAnnotation(pageNum = 1)
        return KeyedPdfAnnotation(key, content)
    }

    @Test
    fun getAnnotationsForPage_validPage_returnsCorrectAnnotations() = runTest {
        val page0Annotations = listOf(createAnnotation("id_1"), createAnnotation("id_2"))
        val page1Annotations = listOf(createAnnotation("id_3"))

        val fakeMap = mapOf(0 to page0Annotations, 1 to page1Annotations)

        val fakeDoc =
            FakePdfDocument(
                pages = listOf(Point(100, 200), Point(100, 200)),
                annotationsPerPage = fakeMap,
            )
        val repository = PdfDocumentAnnotationsRepository(fakeDoc)

        val resultPage0 = repository.getAnnotationsForPage(0)
        val resultPage1 = repository.getAnnotationsForPage(1)

        assertThat(resultPage0).containsExactlyElementsIn(page0Annotations)
        assertThat(resultPage1).containsExactlyElementsIn(page1Annotations)
    }

    @Test
    fun getAnnotationsForPage_pageWithNoAnnotations_returnsEmptyList() = runTest {
        val fakeDoc = FakePdfDocument(pages = listOf(Point(100, 200)))
        val repository = PdfDocumentAnnotationsRepository(fakeDoc)

        val result = repository.getAnnotationsForPage(0)

        assertThat(result).isEmpty()
    }

    @Test
    fun getAnnotationsForPage_sequentialCalls_usesCache() = runTest {
        val page0Annotations = listOf(createAnnotation("id_1"))
        val fakeMap = mapOf(0 to page0Annotations)

        val fakeDoc =
            spy(FakePdfDocument(pages = listOf(Point(100, 200)), annotationsPerPage = fakeMap))

        val repository = PdfDocumentAnnotationsRepository(fakeDoc)

        val result1 = repository.getAnnotationsForPage(0)
        val result2 = repository.getAnnotationsForPage(0)
        val result3 = repository.getAnnotationsForPage(0)

        assertThat(result1).isEqualTo(page0Annotations)
        assertThat(result2).isEqualTo(page0Annotations)

        verify(fakeDoc, times(1)).getAnnotationsForPage(0)
    }

    @Test
    fun getAnnotationsForPage_differentPages_fetchesIndependently() = runTest {
        val fakeDoc = spy(FakePdfDocument(pages = listOf(Point(100, 100), Point(100, 100))))
        val repository = PdfDocumentAnnotationsRepository(fakeDoc)

        repository.getAnnotationsForPage(0)
        repository.getAnnotationsForPage(1)

        repository.getAnnotationsForPage(0)

        verify(fakeDoc, times(1)).getAnnotationsForPage(0)
        verify(fakeDoc, times(1)).getAnnotationsForPage(1)
    }

    @Test
    fun getAnnotationsForPage_concurrentAccessSamePage_fetchesOnce() {
        open class DelayedFakePdfDocument :
            FakePdfDocument(
                pages = listOf(Point(100, 100)),
                annotationsPerPage = mapOf(0 to listOf(createAnnotation("id_1"))),
            ) {
            override suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> {
                delay(50)
                return super.getAnnotationsForPage(pageNum)
            }
        }

        runBlocking {
            val slowFakeDoc = DelayedFakePdfDocument()

            val spyDoc = spy(slowFakeDoc)
            val repository = PdfDocumentAnnotationsRepository(spyDoc)

            val jobs = (1..50).map { async(Dispatchers.IO) { repository.getAnnotationsForPage(0) } }

            val results = jobs.awaitAll()

            assertThat(results).hasSize(50)
            assertThat(results.first()).isNotEmpty()

            verify(spyDoc, times(1)).getAnnotationsForPage(0)
        }
    }

    @Test
    fun getAnnotationsForPage_concurrentAccessLockStriping_handlesCollisions() {
        runBlocking {
            val fakeDoc = spy(FakePdfDocument(pages = List(20) { Point(100, 100) }))
            val repository = PdfDocumentAnnotationsRepository(fakeDoc)

            val job1 = async(Dispatchers.IO) { repository.getAnnotationsForPage(0) }
            val job2 = async(Dispatchers.IO) { repository.getAnnotationsForPage(1) }

            awaitAll(job1, job2)

            verify(fakeDoc, times(1)).getAnnotationsForPage(0)
            verify(fakeDoc, times(1)).getAnnotationsForPage(1)
        }
    }

    @Test
    fun clear_clearsCache() = runTest {
        val page0Annotations = listOf(createAnnotation("id_1"), createAnnotation("id_2"))

        val fakeMap = mapOf(0 to page0Annotations)

        val fakeDoc =
            FakePdfDocument(
                pages = listOf(Point(100, 200), Point(100, 200)),
                annotationsPerPage = fakeMap,
            )
        val repository = PdfDocumentAnnotationsRepository(fakeDoc)

        val resultPage0 = repository.getAnnotationsForPage(0)
        assertThat(resultPage0).isNotEmpty()
        assertThat(resultPage0.size).isEqualTo(2)
        assertThat(repository.isCacheEmpty()).isFalse()

        repository.clear()

        assertThat(repository.isCacheEmpty()).isTrue()
    }
}
