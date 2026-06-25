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

package androidx.pdf.ocr.playservices

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.text.Text
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MlKitOcrProviderTest {

    private lateinit var provider: MlKitOcrProvider

    @Before
    fun setUp() {
        provider = MlKitOcrProvider()
    }

    @After
    fun tearDown() {
        provider.close()
    }

    @Test
    fun recognizeText_withRecycledBitmap_throwsException() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()

        assertFailsWith<IllegalArgumentException> { provider.recognizeText(bitmap) }
    }

    @Test
    fun recognizeText_withTooSmallBitmap_throwsException() = runTest {
        val bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888)

        assertFailsWith<IllegalArgumentException> { provider.recognizeText(bitmap) }
    }

    @Test
    fun getAllText_returnsFullTextAndLineBounds() {
        val ocrResult = createMockOcrResult()

        val allText = ocrResult.getAllText()

        assertThat(allText.text).isEqualTo("Hello World\nKotlin\n")
        assertThat(allText.bounds)
            .containsExactly(Rect(10, 10, 220, 50), Rect(10, 60, 130, 100))
            .inOrder()
    }

    @Test
    fun getWordAt_returnsCorrectWordAndBounds() {
        val ocrResult = createMockOcrResult()

        val wordAtHello = ocrResult.getWordAt(20, 30)
        assertThat(wordAtHello?.text).isEqualTo("Hello")
        assertThat(wordAtHello?.bounds).containsExactly(Rect(10, 10, 110, 50))

        val wordAtWorld = ocrResult.getWordAt(130, 30)
        assertThat(wordAtWorld?.text).isEqualTo("World")
        assertThat(wordAtWorld?.bounds).containsExactly(Rect(120, 10, 220, 50))

        assertThat(ocrResult.getWordAt(300, 300)).isNull()
    }

    @Test
    fun getSearchBounds_returnsOccurrences() {
        val ocrResult = createMockOcrResult()

        val searchResults = ocrResult.getSearchBounds("o")

        assertThat(searchResults).hasSize(3)
        assertThat(searchResults[0]).containsExactly(Rect(90, 10, 110, 50))
        assertThat(searchResults[1]).containsExactly(Rect(140, 10, 160, 50))
        assertThat(searchResults[2]).containsExactly(Rect(30, 60, 50, 100))
    }

    @Test
    fun getSearchBounds_respectsIgnoreCase() {
        val ocrResult = createMockOcrResult()

        assertThat(ocrResult.getSearchBounds("world", ignoreCase = false)).isEmpty()
        assertThat(ocrResult.getSearchBounds("world", ignoreCase = true)).hasSize(1)
    }

    @Test
    fun getText_returnsSelectionRange() {
        val ocrResult = createMockOcrResult()

        val rangeText = ocrResult.getText(20, 30, 130, 30)

        assertThat(rangeText.text).isEqualTo("Hello W")
        assertThat(rangeText.bounds).containsExactly(Rect(10, 10, 140, 50))
    }

    private fun createMockOcrResult(): MlKitOcrResult {
        // Line 1: "Hello" at (10, 10, 110, 50), "World" at (120, 10, 220, 50)
        val helloElement = createMockElement("Hello", Rect(10, 10, 110, 50))
        val worldElement = createMockElement("World", Rect(120, 10, 220, 50))
        val line1 = createMockLine(Rect(10, 10, 220, 50), listOf(helloElement, worldElement))

        // Line 2: "Kotlin" at (10, 60, 130, 100)
        val kotlinElement = createMockElement("Kotlin", Rect(10, 60, 130, 100))
        val line2 = createMockLine(Rect(10, 60, 130, 100), listOf(kotlinElement))

        // Block 1
        val block1 = createMockBlock(Rect(10, 10, 220, 100), listOf(line1, line2))

        val mockText = mock<Text>().apply { whenever(textBlocks).thenReturn(listOf(block1)) }
        return MlKitOcrResult(mockText)
    }

    private fun createMockSymbol(char: String, rect: Rect) =
        mock<Text.Symbol>().apply {
            whenever(text).thenReturn(char)
            whenever(boundingBox).thenReturn(rect)
        }

    private fun createMockElement(word: String, rect: Rect): Text.Element {
        val charWidth = if (word.isNotEmpty()) rect.width() / word.length else 0
        val symbols =
            word.mapIndexed { index, char ->
                val symbolRect =
                    Rect(
                        rect.left + index * charWidth,
                        rect.top,
                        rect.left + (index + 1) * charWidth,
                        rect.bottom,
                    )
                createMockSymbol(char.toString(), symbolRect)
            }
        return mock<Text.Element>().apply {
            whenever(text).thenReturn(word)
            whenever(boundingBox).thenReturn(rect)
            whenever(this.symbols).thenReturn(symbols)
        }
    }

    private fun createMockLine(rect: Rect, elements: List<Text.Element>) =
        mock<Text.Line>().apply {
            whenever(boundingBox).thenReturn(rect)
            whenever(this.elements).thenReturn(elements)
        }

    private fun createMockBlock(rect: Rect, lines: List<Text.Line>) =
        mock<Text.TextBlock>().apply {
            whenever(boundingBox).thenReturn(rect)
            whenever(this.lines).thenReturn(lines)
        }
}
