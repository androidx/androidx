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

package androidx.ui.core

import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx

/**
 * A [Modifier.Element] that changes the way a UI component is measured and laid out.
 */
interface LayoutModifier : Modifier.Element {
    /**
     * Modifies [constraints] for performing measurement of the modified layout element.
     */
    fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Constraints = constraints

    /**
     * Modifies the layout direction to be used for measurement and layout by the modified element.
     */
    fun Density.modifyLayoutDirection(layoutDirection: LayoutDirection) = layoutDirection

    /**
     * Returns the container size of a modified layout element given the original container
     * measurement [constraints] and the measured [childSize].
     */
    fun Density.modifySize(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        childSize: IntPxSize
    ): IntPxSize = childSize

    /**
     * Determines the modified minimum intrinsic width of [measurable].
     * See [Measurable.minIntrinsicWidth].
     */
    fun Density.minIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth =
            measurable.minIntrinsicWidth(modifyConstraints(constraints, layoutDirection).maxHeight)
        return modifySize(constraints, layoutDirection, IntPxSize(layoutWidth, height)).width
    }

    /**
     * Determines the modified maximum intrinsic width of [measurable].
     * See [Measurable.maxIntrinsicWidth].
     */
    fun Density.maxIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth =
            measurable.maxIntrinsicWidth(modifyConstraints(constraints, layoutDirection).maxHeight)
        return modifySize(constraints, layoutDirection, IntPxSize(layoutWidth, height)).width
    }

    /**
     * Determines the modified minimum intrinsic height of [measurable].
     * See [Measurable.minIntrinsicHeight].
     */
    fun Density.minIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight =
            measurable.minIntrinsicHeight(modifyConstraints(constraints, layoutDirection).maxWidth)
        return modifySize(constraints, layoutDirection, IntPxSize(width, layoutHeight)).height
    }

    /**
     * Determines the modified maximum intrinsic height of [measurable].
     * See [Measurable.maxIntrinsicHeight].
     */
    fun Density.maxIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight =
            measurable.maxIntrinsicHeight(modifyConstraints(constraints, layoutDirection).maxWidth)
        return modifySize(constraints, layoutDirection, IntPxSize(width, layoutHeight)).height
    }

    /**
     * Returns the position of a modified child of size [childSize] within a container of
     * size [containerSize].
     */
    fun Density.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize,
        layoutDirection: LayoutDirection
    ): IntPxPosition = if (layoutDirection == LayoutDirection.Ltr) {
        IntPxPosition.Origin
    } else {
        IntPxPosition(containerSize.width - childSize.width, 0.ipx)
    }

    /**
     * Returns the modified position of [line] given its unmodified [value].
     */
    fun Density.modifyAlignmentLine(
        line: AlignmentLine,
        value: IntPx?,
        layoutDirection: LayoutDirection
    ): IntPx? = value
}