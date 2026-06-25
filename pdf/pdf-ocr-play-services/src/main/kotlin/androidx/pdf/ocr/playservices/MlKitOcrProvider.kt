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
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.RestrictTo
import androidx.pdf.ocr.OcrProvider
import androidx.pdf.ocr.OcrResult
import androidx.pdf.ocr.OcrText
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Implementation of [OcrProvider] using ML Kit's Text Recognition. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MlKitOcrProvider : OcrProvider {
    private val recognizerDelegate = lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val recognizer by recognizerDelegate

    override suspend fun recognizeText(image: Bitmap): OcrResult? {
        // ML Kit requires images to be at least 32x32 and not recycled.
        require(!image.isRecycled) { "Cannot process a recycled bitmap." }
        require(image.width >= MIN_IMAGE_WIDTH && image.height >= MIN_IMAGE_HEIGHT) {
            "Bitmap width and height should be at least 32!"
        }

        return try {
            val result =
                recognizer.process(InputImage.fromBitmap(image, /*rotationDegrees*/ 0)).await()
            if (result.text.isNotBlank()) MlKitOcrResult(result) else null
        } catch (e: MlKitException) {
            when (e.errorCode) {
                // Handle expected or recoverable errors by returning null (allows fallback)
                MlKitException.UNAVAILABLE,
                MlKitException.NETWORK_ISSUE,
                MlKitException.INTERNAL -> null
                else -> throw e
            }
        }
    }

    override fun close() {
        if (recognizerDelegate.isInitialized()) {
            recognizer.close()
        }
    }

    private companion object {
        const val MIN_IMAGE_WIDTH = 32
        const val MIN_IMAGE_HEIGHT = 32
    }
}

internal class MlKitOcrResult(private val text: Text) : OcrResult {

    private sealed class OcrItem {
        abstract val text: String

        data class Symbol(val symbol: Text.Symbol) : OcrItem() {
            override val text: String
                get() = symbol.text
        }

        data object Space : OcrItem() {
            override val text: String = " "
        }

        data object Newline : OcrItem() {
            override val text: String = "\n"
        }
    }

    private data class SearchData(val text: String, val charToItemIndices: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SearchData) return false

            return text == other.text && charToItemIndices.contentEquals(other.charToItemIndices)
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + charToItemIndices.contentHashCode()
            return result
        }
    }

    /** Flat list of symbols and whitespace in the order they appear in [Text.text]. */
    private val allItems: List<OcrItem> by lazy {
        val list = mutableListOf<OcrItem>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (elementIndex in line.elements.indices) {
                    val element = line.elements[elementIndex]
                    for (symbol in element.symbols) {
                        list.add(OcrItem.Symbol(symbol))
                    }
                    if (elementIndex < line.elements.size - 1) {
                        list.add(OcrItem.Space)
                    }
                }
                list.add(OcrItem.Newline)
            }
        }
        list
    }

    /** Precomputed data used for searching text. */
    private val searchData: SearchData by lazy {
        val textBuilder = StringBuilder()
        val itemIndices = mutableListOf<Int>()
        for (itemIndex in allItems.indices) {
            val item = allItems[itemIndex]
            val textToAppend = if (item is OcrItem.Symbol) item.text else " "
            textBuilder.append(textToAppend)
            repeat(textToAppend.length) { itemIndices.add(itemIndex) }
        }
        SearchData(textBuilder.toString(), itemIndices.toIntArray())
    }

    /** Precomputed [OcrText] containing all text and bounds from the OCR result. */
    private val _allText: OcrText by lazy { buildOcrText(0, allItems.indices.last) }

    override fun getAllText(): OcrText = _allText

    override fun getText(startX: Int, startY: Int, endX: Int, endY: Int): OcrText {
        val startIndex = findClosestSymbolIndex(Point(startX, startY))
        val endIndex = findClosestSymbolIndex(Point(endX, endY))

        if (startIndex == -1 || endIndex == -1) return OcrText("", emptyList())

        val (first, last) =
            if (startIndex <= endIndex) startIndex to endIndex else endIndex to startIndex
        return buildOcrText(first, last)
    }

    override fun getWordAt(x: Int, y: Int): OcrText? {
        val block = text.textBlocks.find { it.boundingBox?.contains(x, y) == true }
        val line = block?.lines?.find { it.boundingBox?.contains(x, y) == true }
        val element = line?.elements?.find { it.boundingBox?.contains(x, y) == true }

        return element?.boundingBox?.let { boundingBox ->
            OcrText(element.text, listOf(boundingBox))
        }
    }

    override fun getSearchBounds(searchTerm: String, ignoreCase: Boolean): List<List<Rect>> {
        if (searchTerm.isEmpty()) return emptyList()
        val (searchableText, charToItemIndices) = searchData
        val results = mutableListOf<List<Rect>>()

        var startIndex = 0
        while (startIndex < searchableText.length) {
            val matchIndex = searchableText.indexOf(searchTerm, startIndex, ignoreCase)
            if (matchIndex == -1) break

            val firstItemIndex = charToItemIndices[matchIndex]
            val lastItemIndex = charToItemIndices[matchIndex + searchTerm.length - 1]

            results.add(buildOcrText(firstItemIndex, lastItemIndex).bounds)
            startIndex = matchIndex + searchTerm.length
        }
        return results
    }

    /** Finds the closest symbol to the [point] by scanning all symbols on the page. */
    private fun findClosestSymbolIndex(point: Point): Int {
        var minIndex = -1
        var minDistSq = Float.MAX_VALUE

        for (i in allItems.indices) {
            val item = allItems[i]
            if (item is OcrItem.Symbol) {
                item.symbol.boundingBox?.let { box ->
                    val dist = box.distSq(point)
                    if (dist < minDistSq) {
                        minDistSq = dist
                        minIndex = i
                    }
                }
            }
        }
        return minIndex
    }

    /** Builds an [OcrText] by iterating through [allItems] between [first] and [last] indices. */
    private fun buildOcrText(first: Int, last: Int): OcrText {
        val resultText = StringBuilder()
        val resultBounds = mutableListOf<Rect>()
        var currentLineRect: Rect? = null

        for (i in first..last) {
            val item = allItems[i]
            resultText.append(item.text)

            when (item) {
                is OcrItem.Symbol -> {
                    item.symbol.boundingBox?.let { box ->
                        if (currentLineRect == null) {
                            currentLineRect = Rect(box)
                        } else {
                            currentLineRect.union(box)
                        }
                    }
                }
                is OcrItem.Newline -> {
                    currentLineRect?.let { resultBounds.add(it) }
                    currentLineRect = null
                }
                else -> {}
            }
        }
        currentLineRect?.let { resultBounds.add(it) }

        return OcrText(resultText.toString(), resultBounds)
    }

    private fun Rect.distSq(point: Point): Float {
        val dx = (centerX() - point.x).toFloat()
        val dy = (centerY() - point.y).toFloat()
        return dx * dx + dy * dy
    }
}
