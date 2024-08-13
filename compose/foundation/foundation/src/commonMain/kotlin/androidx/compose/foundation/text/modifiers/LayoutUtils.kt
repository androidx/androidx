/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/** Find the constraints to pass to Paragraph based on all the parameters. */
internal fun finalConstraints(
    constraints: Constraints,
    softWrap: Boolean,
    overflow: TextOverflow,
    maxIntrinsicWidth: Float
): Constraints =
    Constraints.fitPrioritizingWidth(
        minWidth = 0,
        maxWidth = finalMaxWidth(constraints, softWrap, overflow, maxIntrinsicWidth),
        minHeight = 0,
        maxHeight = constraints.maxHeight
    )

/** Find the final max width a Paragraph would use based on all parameters. */
internal fun finalMaxWidth(
    constraints: Constraints,
    softWrap: Boolean,
    overflow: TextOverflow,
    maxIntrinsicWidth: Float
): Int {
    val widthMatters = softWrap || overflow.isEllipsis
    val maxWidth =
        if (widthMatters && constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            Constraints.Infinity
        }

    // if minWidth == maxWidth the width is fixed.
    //    therefore we can pass that value to our paragraph and use it
    // if minWidth != maxWidth there is a range
    //    then we should check if the max intrinsic width is in this range to decide the
    //    width to be passed to Paragraph
    //        if max intrinsic width is between minWidth and maxWidth
    //           we can use it to layout
    //        else if max intrinsic width is greater than maxWidth, we can only use maxWidth
    //        else if max intrinsic width is less than minWidth, we should use minWidth
    return if (constraints.minWidth == maxWidth) {
        maxWidth
    } else {
        maxIntrinsicWidth.ceilToIntPx().coerceIn(constraints.minWidth, maxWidth)
    }
}

/** Find the maxLines to pass to text layout based on all parameters */
internal fun finalMaxLines(softWrap: Boolean, overflow: TextOverflow, maxLinesIn: Int): Int {
    // This is a fallback behavior because native text layout doesn't support multiple
    // ellipsis in one text layout.
    // When softWrap is turned off and overflow is ellipsis, it's expected that each line
    // that exceeds maxWidth will be ellipsized.
    // For example,
    // input text:
    //     "AAAA\nAAAA"
    // maxWidth:
    //     3 * fontSize that only allow 3 characters to be displayed each line.
    // expected output:
    //     AA…
    //     AA…
    // Here we assume there won't be any '\n' character when softWrap is false. And make
    // maxLines 1 to implement the similar behavior.
    val overwriteMaxLines = !softWrap && overflow.isEllipsis
    return if (overwriteMaxLines) 1 else maxLinesIn.coerceAtLeast(1)
}

internal val TextOverflow.isEllipsis: Boolean
    get() {
        return this == TextOverflow.Ellipsis ||
            this == TextOverflow.StartEllipsis ||
            this == TextOverflow.MiddleEllipsis
    }
