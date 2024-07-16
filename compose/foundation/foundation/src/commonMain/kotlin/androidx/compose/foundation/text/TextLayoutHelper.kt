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

package androidx.compose.foundation.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Returns true if the this TextLayoutResult can be reused for given parameters.
 *
 * @param text a text to be used for computing text layout.
 * @param style a text style to be used for computing text layout.
 * @param placeholders a list of [Placeholder]s to be used for computing text layout.
 * @param maxLines a maximum number of lines to be used for computing text layout.
 * @param softWrap whether doing softwrap or not to be used for computing text layout.
 * @param overflow an overflow type to be used for computing text layout.
 * @param density a density to be used for computing text layout.
 * @param layoutDirection a layout direction to be used for computing text layout.
 * @param constraints a constraint to be used for computing text layout.
 */
internal fun TextLayoutResult.canReuse(
    text: AnnotatedString,
    style: TextStyle,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    softWrap: Boolean,
    overflow: TextOverflow,
    density: Density,
    layoutDirection: LayoutDirection,
    fontFamilyResolver: FontFamily.Resolver,
    constraints: Constraints
): Boolean {

    // NOTE(text-perf-review): might make sense to short-circuit instance equality here

    // Check if this is created from the same parameter.
    val layoutInput = this.layoutInput
    if (multiParagraph.intrinsics.hasStaleResolvedFonts) {
        // one of the resolved fonts has updated, and this MultiParagraph is no longer valid for
        // measure or display
        return false
    }
    if (!(
        layoutInput.text == text &&
            layoutInput.style.hasSameLayoutAffectingAttributes(style) &&
            layoutInput.placeholders == placeholders &&
            layoutInput.maxLines == maxLines &&
            layoutInput.softWrap == softWrap &&
            layoutInput.overflow == overflow &&
            layoutInput.density == density &&
            layoutInput.layoutDirection == layoutDirection &&
            layoutInput.fontFamilyResolver == fontFamilyResolver
        )
    ) {
        return false
    }

    // Check the given constraints can produces the same result.
    if (constraints.minWidth != layoutInput.constraints.minWidth) return false

    if (!(softWrap || overflow == TextOverflow.Ellipsis)) {
        // If width does not matter, we can result the same layout.
        return true
    }
    return constraints.maxWidth == layoutInput.constraints.maxWidth &&
        constraints.maxHeight == layoutInput.constraints.maxHeight
}

/** Returns whether the given pixel position is inside the selection. */
internal fun TextLayoutResult.isPositionInsideSelection(
    position: Offset,
    selectionRange: TextRange?,
): Boolean {
    if ((selectionRange == null) || selectionRange.collapsed) return false

    fun isOffsetSelectedAndContainsPosition(offset: Int) =
        selectionRange.contains(offset) && getBoundingBox(offset).contains(position)

    // getOffsetForPosition returns the index at which the cursor should be placed when the
    // given position is clicked. This means that when position is to the right of the center of
    // a glyph it will return the index of the next glyph. So we test both the index it returns
    // and the previous index.
    val offset = getOffsetForPosition(position)
    return isOffsetSelectedAndContainsPosition(offset) ||
        isOffsetSelectedAndContainsPosition(offset - 1)
}
