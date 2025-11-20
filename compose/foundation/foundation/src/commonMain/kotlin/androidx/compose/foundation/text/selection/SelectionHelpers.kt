/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection

/**
 * Get the text direction for a given offset.
 *
 * This simply calls [TextLayoutResult.getBidiRunDirection] with one exception, if the offset is an
 * empty line, then we defer to [TextLayoutResult.multiParagraph] and
 * [androidx.compose.ui.text.MultiParagraph.getParagraphDirection]. This is because an empty line
 * always resolves to LTR, even if the paragraph is RTL.
 */
// TODO(b/295197585)
//   Can this logic be moved to a new method in `androidx.compose.ui.text.Paragraph`?
internal fun TextLayoutResult.getTextDirectionForOffset(offset: Int): ResolvedTextDirection =
    if (isOffsetAnEmptyLine(offset)) getParagraphDirection(offset) else getBidiRunDirection(offset)

private fun TextLayoutResult.isOffsetAnEmptyLine(offset: Int): Boolean =
    layoutInput.text.isEmpty() ||
        getLineForOffset(offset).let { currentLine ->
            // verify the previous and next offsets either don't exist because they're at a boundary
            // or that they are different lines than the current line.
            (offset == 0 || currentLine != getLineForOffset(offset - 1)) &&
                (offset == layoutInput.text.length || currentLine != getLineForOffset(offset + 1))
        }
