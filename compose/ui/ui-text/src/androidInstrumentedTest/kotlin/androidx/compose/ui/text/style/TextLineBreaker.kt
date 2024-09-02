/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.style

import androidx.compose.ui.text.AndroidParagraph
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ceilToInt
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry

open class TextLineBreaker {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private val defaultHyphens = Hyphens.None
    private val defaultLineBreak = LineBreak.Simple
    private val density = Density(density = 1f)

    private fun paragraph(
        text: String,
        textStyle: TextStyle,
        maxWidth: Int = Constraints.Infinity
    ): Paragraph {
        return AndroidParagraph(
            text = text,
            spanStyles = listOf(),
            placeholders = listOf(),
            style = textStyle,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            constraints =
                Constraints(maxWidth = maxWidth, maxHeight = Float.POSITIVE_INFINITY.ceilToInt()),
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    fun breakTextIntoLines(
        text: String,
        hyphens: Hyphens = defaultHyphens,
        lineBreak: LineBreak = defaultLineBreak,
        maxWidth: Int
    ): List<String> {
        val layoutResult =
            paragraph(
                text = text,
                textStyle = TextStyle(hyphens = hyphens, lineBreak = lineBreak),
                maxWidth = maxWidth
            )

        return (0 until layoutResult.lineCount).map { lineIndex ->
            text.substring(layoutResult.getLineStart(lineIndex), layoutResult.getLineEnd(lineIndex))
        }
    }
}
