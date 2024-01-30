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

/**
 * Find the constraints to pass to Paragraph based on all the parameters.
 */
internal fun finalConstraints(
    constraints: Constraints,
    softWrap: Boolean,
    overflow: TextOverflow,
    maxIntrinsicWidth: Float
): Constraints = Constraints(
        maxWidth = finalMaxWidth(constraints, softWrap, overflow, maxIntrinsicWidth),
        maxHeight = constraints.maxHeight
    )

/**
 * Find the final max width a Paragraph would use based on all parameters.
 */
internal fun finalMaxWidth(
    constraints: Constraints,
    softWrap: Boolean,
    overflow: TextOverflow,
    maxIntrinsicWidth: Float
): Int {
    val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
    val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
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

/**
 * Find the maxLines to pass to text layout based on all parameters
 */
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
    val overwriteMaxLines = !softWrap && overflow == TextOverflow.Ellipsis
    return if (overwriteMaxLines) 1 else maxLinesIn.coerceAtLeast(1)
}

private const val BigConstraintValue = (1 shl 18) - 1
private const val MediumConstraintValue = (1 shl 16) - 1
private const val SmallConstraintValue = (1 shl 15) - 1
private const val TinyConstraintValue = (1 shl 13) - 1

/**
 * Make constraints that never throw from being too large. Prefer to keep accurate width information
 * first, then constrain height based on the size of width.
 *
 * This will return a Constraint with the same or smaller dimensions than the passed (width, height)
 *
 * see b/312294386 for more details
 *
 * This particular logic is text specific, so not generalizing.
 *
 * @param width desired width (has priority)
 * @param height desired height (uses the remaining bits after width)
 *
 * @return a safe Constraint that never throws for running out of bits
 */
internal fun Constraints.Companion.fixedCoerceHeightAndWidthForBits(
    width: Int,
    height: Int
): Constraints {
    val safeWidth = minOf(width, BigConstraintValue - 1)
    val safeHeight = when {
        safeWidth < TinyConstraintValue -> minOf(height, BigConstraintValue - 1)
        safeWidth < SmallConstraintValue -> minOf(height, MediumConstraintValue - 1)
        safeWidth < MediumConstraintValue -> minOf(height, SmallConstraintValue - 1)
        else -> minOf(height, TinyConstraintValue - 1)
    }
    return fixed(safeWidth, safeHeight)
}
