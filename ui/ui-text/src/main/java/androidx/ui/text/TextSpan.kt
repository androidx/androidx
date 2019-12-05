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

package androidx.ui.text

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
 */
class TextSpan(
    val style: SpanStyle? = null,
    val text: String? = null,
    val children: MutableList<TextSpan> = mutableListOf()
) {

    /**
     * Walks this text span and its descendants in pre-order and calls [visitor]
     * for each span that has text.
     */
    internal fun visitTextSpan(visitor: (span: TextSpan) -> Boolean): Boolean {
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

    override fun toString(): String {
        val buffer = StringBuilder()
        visitTextSpan { span: TextSpan ->
            buffer.append(span.text)
            true
        }
        return buffer.toString()
    }
}

private fun TextSpan.annotatedStringVisitor(builder: AnnotatedString.Builder) {
    style?.let {
        builder.pushStyle(style)
    }

    text?.let { builder.append(text) }

    for (child in children) {
        child.annotatedStringVisitor(builder)
    }

    style?.let {
        builder.popStyle()
    }
}

/**
 * Convert a [TextSpan] into an [AnnotatedString].
 */
fun TextSpan.toAnnotatedString(): AnnotatedString {
    return with(AnnotatedString.Builder()) {
        annotatedStringVisitor(this)
        toAnnotatedString()
    }
}