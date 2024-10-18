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
import android.text.Editable
import android.text.Html.TagHandler
import android.text.Layout
import android.text.Spanned
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.Spanned.SPAN_MARK_MARK
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
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastForEach
import androidx.core.text.HtmlCompat
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.XMLReader

/**
 * Converts a string with HTML tags into [AnnotatedString].
 *
 * If you define your string in the resources, make sure to use HTML-escaped opening brackets
 * <code>&amp;lt;</code> instead of <code><</code>.
 *
 * For a list of supported tags go check
 * [Styling with HTML markup](https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML)
 * guide. Note that bullet lists are not **yet** available.
 *
 * @param htmlString HTML-tagged string to be parsed to construct AnnotatedString
 * @param linkStyles style configuration to be applied to links present in the string in different
 *   styles
 * @param linkInteractionListener a listener that will be attached to links that are present in the
 *   string and triggered when user clicks on those links. When set to null, which is a default, the
 *   system will try to open the corresponding links with the
 *   [androidx.compose.ui.platform.UriHandler] composition local
 *
 * Note that any link style passed directly to this method will be merged with the styles set
 * directly on a HTML-tagged string. For example, if you set a color of the link via the span
 * annotation to "red" but also pass a green color via the [linkStyles], the link will be displayed
 * as green. If, however, you pass a green background via the [linkStyles] instead, the link will be
 * displayed as red on a green background.
 *
 * Example of displaying styled string from resources
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringFromHtml
 * @see LinkAnnotation
 */
fun AnnotatedString.Companion.fromHtml(
    htmlString: String,
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString {
    // Check ContentHandlerReplacementTag kdoc for more details
    val stringToParse = "<$ContentHandlerReplacementTag />$htmlString"
    val spanned =
        HtmlCompat.fromHtml(stringToParse, HtmlCompat.FROM_HTML_MODE_COMPACT, null, TagHandler)
    return spanned.toAnnotatedString(linkStyles, linkInteractionListener)
}

@VisibleForTesting
internal fun Spanned.toAnnotatedString(
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString {
    return AnnotatedString.Builder(capacity = length)
        .append(this)
        .also { it.addSpans(this, linkStyles, linkInteractionListener) }
        .toAnnotatedString()
}

private fun AnnotatedString.Builder.addSpans(
    spanned: Spanned,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener?
) {
    spanned.getSpans(0, length, Any::class.java).forEach { span ->
        val range = TextRange(spanned.getSpanStart(span), spanned.getSpanEnd(span))
        addSpan(span, range.start, range.end, linkStyles, linkInteractionListener)
    }
}

private fun AnnotatedString.Builder.addSpan(
    span: Any,
    start: Int,
    end: Int,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener?
) {
    when (span) {
        is AbsoluteSizeSpan -> {
            // TODO(soboleva) need density object or make dip/px new units in TextUnit
        }
        is AlignmentSpan -> {
            addStyle(span.toParagraphStyle(), start, end)
        }
        is AnnotationSpan -> {
            addStringAnnotation(span.key, span.value, start, end)
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
            span.url?.let { url ->
                val link = LinkAnnotation.Url(url, linkStyles, linkInteractionListener)
                addLink(link, start, end)
            }
        }
    }
}

private fun AlignmentSpan.toParagraphStyle(): ParagraphStyle {
    val alignment =
        when (this.alignment) {
            Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
            Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
            Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
            else -> TextAlign.Unspecified
        }
    return ParagraphStyle(textAlign = alignment)
}

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    /**
     * StyleSpan doc: styles are cumulative -- if both bold and italic are set in separate spans, or
     * if the base style is bold and a span calls for italic, you get bold italic. You can't turn
     * off a style from the base style.
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
    val fontFamily =
        when (family) {
            FontFamily.Cursive.name -> FontFamily.Cursive
            FontFamily.Monospace.name -> FontFamily.Monospace
            FontFamily.SansSerif.name -> FontFamily.SansSerif
            FontFamily.Serif.name -> FontFamily.Serif
            else -> {
                optionalFontFamilyFromName(family)
            }
        }
    return SpanStyle(fontFamily = fontFamily)
}

/**
 * Mirrors [androidx.compose.ui.text.font.PlatformTypefaces.optionalOnDeviceFontFamilyByName]
 * behavior with both font weight and font style being Normal in this case
 */
private fun optionalFontFamilyFromName(familyName: String?): FontFamily? {
    if (familyName.isNullOrEmpty()) return null
    val typeface = Typeface.create(familyName, Typeface.NORMAL)
    return typeface
        .takeIf {
            typeface != Typeface.DEFAULT &&
                typeface != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        ?.let { FontFamily(it) }
}

private val TagHandler =
    object : TagHandler {
        override fun handleTag(
            opening: Boolean,
            tag: String?,
            output: Editable?,
            xmlReader: XMLReader?
        ) {
            if (xmlReader == null || output == null) return

            if (opening && tag == ContentHandlerReplacementTag) {
                val currentContentHandler = xmlReader.contentHandler
                xmlReader.contentHandler = AnnotationContentHandler(currentContentHandler, output)
            }
        }
    }

private class AnnotationContentHandler(
    private val contentHandler: ContentHandler,
    private val output: Editable
) : ContentHandler by contentHandler {
    override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
        if (localName == AnnotationTag) {
            atts?.let { handleAnnotationStart(it) }
        } else {
            contentHandler.startElement(uri, localName, qName, atts)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (localName == AnnotationTag) {
            handleAnnotationEnd()
        } else {
            contentHandler.endElement(uri, localName, qName)
        }
    }

    private fun handleAnnotationStart(attributes: Attributes) {
        // Each annotation can have several key/value attributes. So for
        // <annotation key1=value1 key2=value2>...<annotation>
        // example we will add two [AnnotationSpan]s which we'll later read
        for (i in 0 until attributes.length) {
            val key = attributes.getLocalName(i).orEmpty()
            val value = attributes.getValue(i).orEmpty()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                val start = output.length
                // add temporary AnnotationSpan to the output to read it when handling
                // the closing tag
                output.setSpan(AnnotationSpan(key, value), start, start, SPAN_MARK_MARK)
            }
        }
    }

    private fun handleAnnotationEnd() {
        // iterate through all of the spans that we added when handling the opening tag. Calculate
        // the true position of the span and make a replacement
        output
            .getSpans(0, output.length, AnnotationSpan::class.java)
            .filter { output.getSpanFlags(it) == SPAN_MARK_MARK }
            .fastForEach { annotation ->
                val start = output.getSpanStart(annotation)
                val end = output.length

                output.removeSpan(annotation)
                // only add the annotation if there's a text in between the opening and closing tags
                if (start != end) {
                    output.setSpan(annotation, start, end, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
    }
}

private class AnnotationSpan(val key: String, val value: String)

/**
 * This tag is added at the beginning of a string fed to the HTML parser in order to trigger a
 * TagHandler's callback early on so we can replace the ContentHandler with our own
 * [AnnotationContentHandler]. This is needed to handle the opening <annotation> tags since by the
 * time TagHandler is triggered, the parser already visited and left the opening <annotation> tag
 * which contains the attributes. Note that closing tag doesn't have the attributes and therefore
 * not enough to construct the intermediate [AnnotationSpan] object that is later transformed into
 * [AnnotatedString]'s string annotation.
 */
private const val ContentHandlerReplacementTag = "ContentHandlerReplacementTag"
private const val AnnotationTag = "annotation"
