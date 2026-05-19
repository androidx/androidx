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
import android.graphics.Point
import android.graphics.Rect
import kotlin.math.pow

class FakeOcrProvider(private val fakeResult: OcrResult? = null) : OcrProvider {
    override suspend fun recognizeText(image: Bitmap): OcrResult? = fakeResult

    override fun close() {}
}

class FakeOcrResult(
    private val characters: List<OcrText> = emptyList(),
    private val words: List<OcrText> = emptyList(),
    override val isRtl: Boolean = false,
) : OcrResult {
    override val allText: OcrText =
        OcrText(words.joinToString(" ") { it.text }, words.flatMap { it.bounds })

    override fun getText(startPoint: Point, endPoint: Point): OcrText {
        if (characters.isEmpty()) return OcrText("", emptyList())

        val startIndex = findClosestCharacterIndex(startPoint)
        val endIndex = findClosestCharacterIndex(endPoint)

        val range = if (startIndex <= endIndex) startIndex..endIndex else endIndex..startIndex
        val selectedChars = characters.slice(range)

        return OcrText(
            selectedChars.joinToString("") { it.text },
            selectedChars.flatMap { it.bounds },
        )
    }

    override fun getWordAt(point: Point): OcrText? {
        return words.find { word -> word.bounds.any { it.contains(point.x, point.y) } }
    }

    override fun getSearchBounds(searchTerm: String): List<List<Rect>> {
        return words.filter { it.text.contains(searchTerm, ignoreCase = true) }.map { it.bounds }
    }

    private fun findClosestCharacterIndex(point: Point): Int {
        return characters.indices.minByOrNull { i ->
            val bounds = characters[i].bounds.first()
            val distSq =
                (bounds.centerX() - point.x).toFloat().pow(2) +
                    (bounds.centerY() - point.y).toFloat().pow(2)
            distSq
        } ?: 0
    }
}
