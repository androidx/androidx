/*
* Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.annotation.RestrictTo
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.basictypes.RenderComparison

/**
 * A [TextSpan] object can be styled using its [style] property.
 * The style will be applied to the [text] and the [children].
 *
 * For the object to be useful, at least one of [text] or [children] should be set.
 *
 * @param style The style to apply to the [text] and the [children].
 *
 * @param text The text contained in the span. If both [text] and [children] are non-null, the text
 *   will precede the children.
 *
 * @param children Additional spans to include as children. If both [text] and [children] are
 *   non-null, the text will precede the children. The list must not contain any nulls.
 *
 * @param recognizer A gesture recognizer that will receive events that hit this text span.
 * @hide
 */
// TODO(haoyuchang) Make TextSpan immutable.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TextSpan(
    var style: TextStyle? = null,
    var text: String? = null,
    val children: MutableList<TextSpan> = mutableListOf()/*,
    val recognizer: GestureRecognizer? = null*/
) {

    /**
     * Walks this text span and its descendants in pre-order and calls [visitor]
     * for each span that has text.
     */
    fun visitTextSpan(visitor: (span: TextSpan) -> Boolean): Boolean {
        if (text != null) {
            if (!visitor(this)) {
                return false
            }
        }
        for (child in children) {
            if (!child.visitTextSpan(visitor)) {
                return false
            }
        }
        return true
    }

    /** Returns the text span that contains the given position in the text. */
    fun getSpanForPosition(position: TextPosition): TextSpan? {
        val affinity: TextAffinity = position.affinity
        val targetOffset: Int = position.offset
        var offset = 0
        var result: TextSpan? = null
        visitTextSpan {
                span: TextSpan ->
            assert(result == null)
            val endOffset: Int = offset + span.text!!.length
            if (targetOffset == offset && affinity == TextAffinity.downstream ||
                targetOffset > offset && targetOffset < endOffset ||
                targetOffset == endOffset && affinity == TextAffinity.upstream) {
                result = span
            }
            offset = endOffset
            true
        }
        return result
    }

    /**
     * Flattens the [TextSpan] tree into a single string.
     *
     * Styles are not honored in this process.
     */
    fun toPlainText(): String {
        val buffer = StringBuilder()
        visitTextSpan {
                span: TextSpan ->
            buffer.append(span.text)
            true
        }
        return buffer.toString()
    }

    /**
     * Returns the UTF-16 code unit at the given index in the flattened string.
     *
     * Returns null if the index is out of bounds.
     */
    // TODO(Migration/qqd): VERY IMPORTANT the name is weird, and what it does is also weird.
//    fun codeUnitAt(index: Int): Int? {
//        if (index < 0)
//            return null
//        var offset: Int? = 0
//        var result: Int? = null
//        visitTextSpan {
//                span: TextSpan ->
//            if (index - offset!! < span.text!!.length) {
//                // Flutter only considered BMP (Basic Multilingual Plane or Plane 0), so we only
//                // return the high-surrogate (index 0) in the UTF-16 representation array resulting
//                // from Character.toChars.
//                val codePoint = span.text[index - offset]
//                val utf16Array = Character.toChars(codePoint.toInt())
//                result = utf16Array[0].toInt()
//                false
//            }
//            offset += span.text?.length
//            true
//        }
//        return result
//    }

    /**
     * Describe the difference between this text span and another, in terms ofhow much damage it
     * will make to the rendering. The comparison is deep.
     */
    fun compareTo(other: TextSpan): RenderComparison {
        if (this === other) {
            return RenderComparison.IDENTICAL
        }
        if (other.text != text ||
            children.size != other.children.size ||
            (style == null) != (other.style == null)) {
            return RenderComparison.LAYOUT
        }
        var result: RenderComparison = RenderComparison.IDENTICAL
            // TODO(siyamed) add recognizer
            /*if (recognizer == other.recognizer) RenderComparison.IDENTICAL
            else RenderComparison.METADATA*/
        style?.let {
            val candidate: RenderComparison = it.compareTo(other.style!!)
            if (candidate.ordinal > result.ordinal) {
                result = candidate
            }
            if (result == RenderComparison.LAYOUT) {
                return result
            }
        }

        children.forEachIndexed { index, child ->
            val candidate: RenderComparison = child.compareTo(other.children[index])
            if (candidate.ordinal > result.ordinal) {
                result = candidate
            }
            if (result == RenderComparison.LAYOUT) {
                return result
            }
        }
        return result
    }
}

private data class RecordInternal(
    val style: TextStyle,
    val start: Int,
    var end: Int
)

private fun TextSpan.annotatedStringVisitor(
    includeRootStyle: Boolean,
    stringBuilder: java.lang.StringBuilder,
    styles: MutableList<RecordInternal>
) {
    val styleSpan = if (includeRootStyle) {
        style?.let {
            val span = RecordInternal(it, stringBuilder.length, -1)
            styles.add(span)
            span
        }
    } else null

    text?.let { stringBuilder.append(text) }
    for (child in children) {
        child.annotatedStringVisitor(true, stringBuilder, styles)
    }

    if (styleSpan != null) {
        styleSpan.end = stringBuilder.length
    }
}

/**
 * Convert a [TextSpan] into an [AnnotatedString].
 * @param includeRootStyle whether to attach the text style in the root [TextSpan] to the output
 *  [AnnotatedString]. It's useful when the top level [TextStyle] is used as global text style setting.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun TextSpan.toAnnotatedString(includeRootStyle: Boolean = true): AnnotatedString {
    val stringBuilder = java.lang.StringBuilder()
    val tempRecords = mutableListOf<RecordInternal>()
    annotatedStringVisitor(includeRootStyle, stringBuilder, tempRecords)
    val records = tempRecords.map { AnnotatedString.Item(it.style, it.start, it.end) }
    return AnnotatedString(stringBuilder.toString(), records)
}