/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.text

import android.graphics.Typeface
import android.os.Build
import android.text.Annotation
import android.text.Layout
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.core.text.HtmlCompat

actual fun String.parseAsHtml(): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return spanned.toAnnotatedString()
}

private fun Spanned.toAnnotatedString(): AnnotatedString {
    return AnnotatedString.Builder(capacity = length)
        .append(this)
        .also { it.addSpans(this) }
        .toAnnotatedString()
}

private fun AnnotatedString.Builder.addSpans(spanned: Spanned) {
    spanned.getSpans(0, length, Any::class.java).forEach { span ->
        val range = TextRange(spanned.getSpanStart(span), spanned.getSpanEnd(span))
        addSpan(span, range.start, range.end)
    }
}

private fun AnnotatedString.Builder.addSpan(span: Any, start: Int, end: Int) {
    when (span) {
        is AbsoluteSizeSpan -> {
            // TODO(soboleva) need density object or make dip/px new units in TextUnit
        }
        is AlignmentSpan -> {
            addStyle(span.toParagraphStyle(), start, end)
        }
        is Annotation -> {
            // TODO(soboleva) handle this via TagHandler
        }
        is BackgroundColorSpan -> {
            addStyle(SpanStyle(background = Color(span.backgroundColor)), start, end)
        }
        is ForegroundColorSpan -> {
            addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        }
        is RelativeSizeSpan -> {
            addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
        }
        is StrikethroughSpan -> {
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        }
        is StyleSpan -> {
            span.toSpanStyle()?.let { addStyle(it, start, end) }
        }
        is SubscriptSpan -> {
            addStyle(SpanStyle(baselineShift = BaselineShift.Subscript), start, end)
        }
        is SuperscriptSpan -> {
            addStyle(SpanStyle(baselineShift = BaselineShift.Superscript), start, end)
        }
        is TypefaceSpan -> {
            addStyle(span.toSpanStyle(), start, end)
        }
        is UnderlineSpan -> {
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        }
        is URLSpan -> {
            span.url?.let {
                addLink(LinkAnnotation.Url(it), start, end)
            }
        }
    }
}

private fun AlignmentSpan.toParagraphStyle(): ParagraphStyle {
    val alignment = when (this.alignment) {
        Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
        Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
        Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
        else -> TextAlign.Unspecified
    }
    return ParagraphStyle(textAlign = alignment)
}

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    /** StyleSpan doc: styles are cumulative -- if both bold and italic are set in
     * separate spans, or if the base style is bold and a span calls for italic,
     * you get bold italic.  You can't turn off a style from the base style.
     */
    return when (style) {
        Typeface.BOLD -> {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        Typeface.ITALIC -> {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        Typeface.BOLD_ITALIC -> {
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
        else -> null
    }
}

private fun TypefaceSpan.toSpanStyle(): SpanStyle {
    var fontFamily: FontFamily? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        fontFamily = Api28Impl.createFontFamilyFromTypeface(this)
    }
    if (fontFamily == null) fontFamily = when (family) {
        null -> null
        FontFamily.Cursive.name -> FontFamily.Cursive
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        FontFamily.Serif.name -> FontFamily.Serif
        else -> FontFamily.Default
    }
    return SpanStyle(fontFamily = fontFamily)
}

@RequiresApi(28)
private object Api28Impl {
    @DoNotInline
    fun createFontFamilyFromTypeface(typefaceSpan: TypefaceSpan) =
        typefaceSpan.typeface?.let { FontFamily(it) }
}
