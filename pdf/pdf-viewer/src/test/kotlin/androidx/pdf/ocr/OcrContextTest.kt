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

import android.graphics.Rect
import android.graphics.RectF
import androidx.pdf.Dimension
import androidx.pdf.PdfPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class OcrContextTest {

    @Test
    fun testSearch_mapsCoordinatesToPdf() = runTest {
        val pageNum = 0
        // Mapping: PDF (10,10) -> Image (0,0); PDF (60,60) -> Image (100,100)
        val imageRect = RectF(10f, 10f, 60f, 60f)
        val bitmapSize = Dimension(100, 100)

        // Text in Image.
        val query = "test"
        val ocrBounds = Rect(20, 20, 40, 40)

        val ocrResult = FakeOcrResult(words = listOf(OcrText(query, listOf(ocrBounds))))
        val context = OcrContext(ocrResult, pageNum, imageRect, bitmapSize)

        val results = context.search(query)
        assertEquals(1, results.size)
        val matchBounds = results[0][0]

        assertEquals(20f, matchBounds.left)
        assertEquals(20f, matchBounds.top)
        assertEquals(30f, matchBounds.right)
        assertEquals(30f, matchBounds.bottom)
    }

    @Test
    fun testGetText_mapsCoordinatesToImage() {
        val imageRect = RectF(0f, 0f, 50f, 50f)
        val bitmapSize = Dimension(100, 100)

        val word1 = OcrText("hello", listOf(Rect(0, 0, 50, 50)))
        val word2 = OcrText("world", listOf(Rect(60, 0, 110, 50)))

        // Character level for selection
        val chars =
            "hello world"
                .mapIndexed { i, c ->
                    OcrText(c.toString(), listOf(Rect(i * 10, 0, (i + 1) * 10, 50)))
                }

        val ocrResult = FakeOcrResult(characters = chars, words = listOf(word1, word2))
        val context = OcrContext(ocrResult, 0, imageRect, bitmapSize)

        val result = context.getText(PdfPoint(0, 17.5f, 12.5f), PdfPoint(0, 37.5f, 12.5f))
        assertEquals("lo wo", result.text)
    }

    @Test
    fun testGetWordAt_returnsWordAtPoint() {
        val imageRect = RectF(50f, 50f, 100f, 100f)
        val bitmapSize = Dimension(100, 100)

        val word = OcrText("test", listOf(Rect(10, 10, 20, 20)))

        val ocrResult = FakeOcrResult(words = listOf(word))
        val context = OcrContext(ocrResult, 0, imageRect, bitmapSize)

        val result = context.getWordAt(PdfPoint(0, 57.5f, 57.5f))
        assertEquals("test", result?.text)

        // Point outside the image
        assertNull(context.getWordAt(PdfPoint(0, 25f, 30f)))

        // Point inside image but no word
        assertNull(context.getWordAt(PdfPoint(0, 50f, 50f)))
    }

    @Test
    fun testMapOcrTextToPdfBounds() {
        val imageRect = RectF(10f, 10f, 110f, 110f)
        val bitmapSize = Dimension(100, 100)

        val ocrText = OcrText("test", listOf(Rect(0, 0, 100, 100)))
        val context = OcrContext(FakeOcrResult(), 0, imageRect, bitmapSize)

        val pdfBounds = context.mapOcrTextToPdfBounds(ocrText)
        assertEquals(1, pdfBounds.size)
        assertEquals(10f, pdfBounds[0].left)
        assertEquals(10f, pdfBounds[0].top)
        assertEquals(110f, pdfBounds[0].right)
        assertEquals(110f, pdfBounds[0].bottom)
    }
}
