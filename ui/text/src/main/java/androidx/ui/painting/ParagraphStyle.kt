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

package androidx.ui.painting

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextIndent

/**
 * Creates a new ParagraphStyle object.
 *
 * @param textAlign The alignment of the text within the lines of the paragraph.
 *
 * @param textDirection The directionality of the text, Left to Right (LTR) or Right To Left (RTL).
 * This controls the overall directionality of the paragraph, as well as the meaning of
 * [TextAlign.Start] and [TextAlign.End] in the `textAlign` field.
 *
 * @param textIndent Specify how much a paragraph is indented.
 *
 * @param lineHeight The minimum height of the line boxes, as a multiple of the font size.
 */
data class ParagraphStyle constructor(
    val textAlign: TextAlign? = null,
    val textDirection: TextDirection? = null,
    val lineHeight: Float? = null,
    val textIndent: TextIndent? = null
) {
    // TODO(siyamed) uncomment
//    /**
//     * Returns a new paragraph style that is a combination of this style and the given [other] style.
//     *
//     * If the given paragraph style is null, returns this paragraph style.
//     */
//    fun merge(other: ParagraphStyle? = null): ParagraphStyle {
//        if (other == null) return this
//
//        return ParagraphStyle(
//            lineHeight = other.lineHeight ?: this.lineHeight,
//            textIndent = other.textIndent ?: this.textIndent,
//            textAlign = other.textAlign ?: this.textAlign,
//            textDirection = other.textDirection ?: this.textDirection
//        )
//    }
}