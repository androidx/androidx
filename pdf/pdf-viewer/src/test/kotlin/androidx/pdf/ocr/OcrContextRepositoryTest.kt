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

package androidx.pdf.ocr

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.pdf.FakePdfDocument
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.KeyedPdfObject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class OcrContextRepositoryTest {

    @Test
    fun testGetOcrContexts_cachesResults() = runTest {
        val pageNum = 0
        val imageBounds = RectF(0f, 0f, 100f, 100f)

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject)))

        var recognizeCount = 0
        val ocrProvider =
            object : OcrProvider {
                override suspend fun recognizeText(image: Bitmap): OcrResult? {
                    recognizeCount++
                    return FakeOcrResult()
                }

                override fun close() {}
            }

        val repository = OcrContextRepository(fakePdfDocument, ocrProvider)

        // First call: should perform OCR
        val contexts1 = repository.getOcrContexts(pageNum)
        assertEquals(1, contexts1.size)
        assertEquals(1, recognizeCount)

        // Second call: should return cached result
        val contexts2 = repository.getOcrContexts(pageNum)
        assertEquals(1, contexts2.size)
        assertEquals(1, recognizeCount)
        assertTrue(contexts1 === contexts2)
    }

    @Test
    fun testGetOcrContexts_concurrentRequests_oneOcrCall() = runTest {
        val pageNum = 0
        val imageBounds = RectF(0f, 0f, 100f, 100f)

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject)))

        var recognizeCount = 0
        val ocrProvider =
            object : OcrProvider {
                override suspend fun recognizeText(image: Bitmap): OcrResult? {
                    delay(100.milliseconds) // Simulate expensive OCR
                    recognizeCount++
                    return FakeOcrResult()
                }

                override fun close() {}
            }

        val repository = OcrContextRepository(fakePdfDocument, ocrProvider)

        // Launch multiple concurrent requests for the same page
        val results =
            awaitAll(
                async { repository.getOcrContexts(pageNum) },
                async { repository.getOcrContexts(pageNum) },
                async { repository.getOcrContexts(pageNum) },
            )

        // All should get the same result
        assertEquals(3, results.size)
        assertTrue(results.all { it === results[0] })

        // But OCR should only be performed once due to lock striping/collapsing
        assertEquals(1, recognizeCount)
    }

    @Test
    fun testGetOcrContexts_multipleImagesOnPage() = runTest {
        val pageNum = 0

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val keyedObject1 = KeyedPdfObject("img1", ImagePdfObject(bitmap, RectF(0f, 0f, 10f, 10f)))
        val keyedObject2 = KeyedPdfObject("img2", ImagePdfObject(bitmap, RectF(20f, 20f, 30f, 30f)))

        val fakePdfDocument =
            FakePdfDocument(
                pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject1, keyedObject2))
            )

        val ocrProvider = FakeOcrProvider(FakeOcrResult())
        val repository = OcrContextRepository(fakePdfDocument, ocrProvider)

        val contexts = repository.getOcrContexts(pageNum)
        assertEquals(2, contexts.size)
        assertEquals(RectF(0f, 0f, 10f, 10f), contexts[0].imageRect)
        assertEquals(RectF(20f, 20f, 30f, 30f), contexts[1].imageRect)
    }
}
