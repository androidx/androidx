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
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TextSpan(
    val style: TextStyle? = null,
    val text: String? = null,
    val children: MutableList<TextSpan> = mutableListOf()
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
     * Describe the difference between this text span and another, in terms ofhow much damage it
     * will make to the rendering. The comparison is deep.
     */
    internal fun compareTo(other: TextSpan): RenderComparison {
        if (this === other) {
            return RenderComparison.IDENTICAL
        }
        if (other.text != text ||
            children.size != other.children.size ||
            (style == null) != (other.style == null)) {
            return RenderComparison.LAYOUT
        }
        var result: RenderComparison = RenderComparison.IDENTICAL
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
    stringBuilder: java.lang.StringBuilder,
    styles: MutableList<RecordInternal>
) {
    val styleSpan = style?.let {
        val span = RecordInternal(it, stringBuilder.length, -1)
        styles.add(span)
        span
    }

    text?.let { stringBuilder.append(text) }
    for (child in children) {
        child.annotatedStringVisitor(stringBuilder, styles)
    }

    if (styleSpan != null) {
        styleSpan.end = stringBuilder.length
    }
}

/**
 * Convert a [TextSpan] into an [AnnotatedString].
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun TextSpan.toAnnotatedString(): AnnotatedString {
    val stringBuilder = java.lang.StringBuilder()
    val tempRecords = mutableListOf<RecordInternal>()
    annotatedStringVisitor(stringBuilder, tempRecords)
    val records = tempRecords.map { AnnotatedString.Item(it.style, it.start, it.end) }
    return AnnotatedString(stringBuilder.toString(), records)
}