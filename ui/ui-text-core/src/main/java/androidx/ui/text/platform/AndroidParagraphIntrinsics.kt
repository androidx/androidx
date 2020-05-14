/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.platform

import android.graphics.Paint
import android.text.TextPaint
import androidx.ui.unit.Density
import androidx.ui.text.AnnotatedString
import androidx.ui.text.ParagraphIntrinsics
import androidx.ui.text.Placeholder
import androidx.ui.text.SpanStyle
import androidx.ui.text.TextStyle

internal class AndroidParagraphIntrinsics(
    val text: String,
    val style: TextStyle,
    val spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val typefaceAdapter: TypefaceAdapter,
    val density: Density
) : ParagraphIntrinsics {

    internal val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    internal val charSequence: CharSequence

    internal val layoutIntrinsics: LayoutIntrinsics

    override val maxIntrinsicWidth: Float
        get() = layoutIntrinsics.maxIntrinsicWidth

    override val minIntrinsicWidth: Float
        get() = layoutIntrinsics.minIntrinsicWidth

    internal val textDirectionHeuristic = style.textDirectionAlgorithm?.let {
        resolveTextDirectionHeuristics(style.textDirectionAlgorithm)
    } ?: throw IllegalArgumentException(
        "TextStyle.textDirectionAlgorithm should not be null"
    )

    init {
        val notAppliedStyle = textPaint.applySpanStyle(
            style.toSpanStyle(),
            typefaceAdapter,
            density
        )

        charSequence = createStyledText(
            text = text,
            contextFontSize = textPaint.textSize,
            lineHeight = style.lineHeight,
            textIndent = style.textIndent,
            spanStyles = listOf(
                AnnotatedString.Range(
                    notAppliedStyle,
                    0,
                    text.length
                )
            ) + spanStyles,
            placeholders = placeholders,
            density = density,
            typefaceAdapter = typefaceAdapter
        )

        layoutIntrinsics = LayoutIntrinsics(charSequence, textPaint, textDirectionHeuristic)
    }
}
