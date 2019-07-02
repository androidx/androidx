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
package androidx.ui.engine.text

/**
 * An opaque object that determines the configuration used by
 * [ParagraphBuilder] to position lines within a [Paragraph] of text.
 *
 * Creates a new ParagraphStyle object.
 *
 * @param textAlign The alignment of the text within the lines of the
 *   paragraph. If the last line is ellipsized (see `ellipsis` below), the
 *   alignment is applied to that line after it has been truncated but before
 *   the ellipsis has been added.
 *
 * @param textDirection The directionality of the text, left-to-right (e.g.
 *   Norwegian) or right-to-left (e.g. Hebrew). This controls the overall
 *   directionality of the paragraph, as well as the meaning of
 *   [TextAlign.Start] and [TextAlign.End] in the `textAlign` field.
 *
 * @param textIndent The amount of indentation applied to the affected paragraph. A paragraph is
 *   affected if any of its character is covered by the TextSpan.
 *
 * @param lineHeight The minimum height of the line boxes, as a multiple of the
 *   font size.
 *
 * @param maxLines The maximum number of lines painted. Lines beyond this
 *   number are silently dropped. For example, if `maxLines` is 1, then only
 *   one line is rendered.
 *
 * @param ellipsis Whether to ellipsize overflowing text. If `maxLines` is
 *   not null, ellipsis is applied to the last rendered line, if that line
 *   overflows the width constraints. If `maxLines` is null, it will never
 *   be applied. If ellipsis is null, the system default will be adopted.
 */
data class ParagraphStyle constructor(
    val textAlign: TextAlign? = null,
    val textDirection: TextDirection? = null,
    val textIndent: TextIndent? = null,
    val lineHeight: Float? = null,
    val maxLines: Int? = null,
    val ellipsis: Boolean? = null
) {
    init {
        lineHeight?.let {
            assert(it >= 0f) {
                "lineHeight can't be negative ($it)"
            }
        }
    }
}