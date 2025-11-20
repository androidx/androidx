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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.offset
import androidx.xr.glimmer.requirePreconditionNotNull

internal sealed interface ListLayoutProperties {
    val orientation: Orientation
    val horizontalArrangement: Arrangement.Horizontal?
    val verticalArrangement: Arrangement.Vertical?
    val horizontalAlignment: Alignment.Horizontal?
    val verticalAlignment: Alignment.Vertical?
    val contentConstraints: Constraints
    val beforeContentPadding: Int
    val afterContentPadding: Int
    val mainAxisAvailableSize: Int
    val spaceBetweenItems: Int
    val totalVerticalPadding: Int
    val totalHorizontalPadding: Int
    val layoutDirection: LayoutDirection
    val reverseLayout: Boolean
    val visualOffset: IntOffset
}

internal val ListLayoutProperties.isVertical: Boolean
    get() = orientation == Orientation.Vertical

private class GlimmerListLayoutProperties(
    override val orientation: Orientation,
    override val horizontalArrangement: Arrangement.Horizontal?,
    override val verticalArrangement: Arrangement.Vertical?,
    override val horizontalAlignment: Alignment.Horizontal?,
    override val verticalAlignment: Alignment.Vertical?,
    override val layoutDirection: LayoutDirection,
    override val reverseLayout: Boolean,
    private val topPadding: Int,
    private val bottomPadding: Int,
    private val startPadding: Int,
    private val endPadding: Int,
    val containerConstraints: Constraints,
    override val spaceBetweenItems: Int,
) : ListLayoutProperties {

    override val totalVerticalPadding: Int
        get() = topPadding + bottomPadding

    override val totalHorizontalPadding: Int
        get() = startPadding + endPadding

    private val totalMainAxisPadding: Int
        get() = if (isVertical) totalVerticalPadding else totalHorizontalPadding

    override val beforeContentPadding: Int
        get() =
            when {
                isVertical && !reverseLayout -> topPadding
                isVertical && reverseLayout -> bottomPadding
                !isVertical && !reverseLayout -> startPadding
                else -> endPadding // !isVertical && reverseLayout
            }

    override val afterContentPadding: Int
        get() = totalMainAxisPadding - beforeContentPadding

    override val contentConstraints: Constraints
        get() = containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

    // can be negative if the content padding is larger than the max size from constraints
    override val mainAxisAvailableSize: Int
        get() =
            if (isVertical) {
                containerConstraints.maxHeight - totalVerticalPadding
            } else {
                containerConstraints.maxWidth - totalHorizontalPadding
            }

    override val visualOffset: IntOffset
        get() =
            if (!reverseLayout || mainAxisAvailableSize > 0) {
                IntOffset(startPadding, topPadding)
            } else {
                // When layout is reversed and paddings together take >100% of the available
                // space,
                // layout size is coerced to 0 when positioning. To take that space into
                // account,
                // we offset start padding by negative space between paddings.
                IntOffset(
                    if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                    if (isVertical) topPadding + mainAxisAvailableSize else topPadding,
                )
            }
}

internal fun Density.resolveLayoutProperties(
    orientation: Orientation,
    contentPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    reverseLayout: Boolean,
    horizontalArrangement: Arrangement.Horizontal?,
    verticalArrangement: Arrangement.Vertical?,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    containerConstraints: Constraints,
): ListLayoutProperties {
    val isVertical = orientation == Orientation.Vertical
    // resolve content paddings
    val startPadding =
        if (isVertical) {
            contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
        } else {
            // in horizontal configuration, padding is reversed by placeRelative
            contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        }

    val endPadding =
        if (isVertical) {
            contentPadding.calculateRightPadding(layoutDirection).roundToPx()
        } else {
            // in horizontal configuration, padding is reversed by placeRelative
            contentPadding.calculateEndPadding(layoutDirection).roundToPx()
        }
    val topPadding = contentPadding.calculateTopPadding().roundToPx()
    val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()

    val spaceBetweenItemsDp =
        if (isVertical) {
            requirePreconditionNotNull(verticalArrangement) {
                    "null verticalArrangement when isVertical == true"
                }
                .spacing
        } else {
            requirePreconditionNotNull(horizontalArrangement) {
                    "null horizontalAlignment when isVertical == false"
                }
                .spacing
        }
    val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

    return GlimmerListLayoutProperties(
        orientation = orientation,
        reverseLayout = reverseLayout,
        spaceBetweenItems = spaceBetweenItems,
        startPadding = startPadding,
        endPadding = endPadding,
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        containerConstraints = containerConstraints,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        layoutDirection = layoutDirection,
    )
}
