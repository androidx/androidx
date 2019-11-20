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

import androidx.compose.Immutable
import androidx.ui.core.TextUnit
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent

/**
 * Paragraph styling configuration for a paragraph. The difference between [TextStyle] and
 * `ParagraphStyle` is that, `ParagraphStyle` can be applied to a whole [Paragraph] while
 * [TextStyle] can be applied at the character level.
 * Once a portion of the text is marked with a `ParagraphStyle`, that portion will be separated from
 * the remaining as if a line feed character was added.
 *
 * @sample androidx.ui.text.samples.ParagraphStyleSample
 * @sample androidx.ui.text.samples.ParagraphStyleAnnotatedStringsSample
 *
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param textDirectionAlgorithm The algorithm to be used to resolve the final text direction:
 * Left To Right or Right To Left.
 * @param textIndent The indentation of the paragraph.
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 *
 * @see [Paragraph]
 * @see [AnnotatedString]
 */
@Immutable
data class ParagraphStyle constructor(
    val textAlign: TextAlign? = null,
    val textDirectionAlgorithm: TextDirectionAlgorithm? = null,
    val lineHeight: TextUnit = TextUnit.Inherit,
    val textIndent: TextIndent? = null
) {
    init {
        if (lineHeight != TextUnit.Inherit) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            assert(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    /**
     * Returns a new paragraph style that is a combination of this style and the given [other]
     * style.
     *
     * If the given paragraph style is null, returns this paragraph style.
     */
    fun merge(other: ParagraphStyle? = null): ParagraphStyle {
        if (other == null) return this

        return ParagraphStyle(
            lineHeight = if (other.lineHeight == TextUnit.Inherit) {
                this.lineHeight
            } else {
                other.lineHeight
            },
            textIndent = other.textIndent ?: this.textIndent,
            textAlign = other.textAlign ?: this.textAlign,
            textDirectionAlgorithm = other.textDirectionAlgorithm
        )
    }
}
