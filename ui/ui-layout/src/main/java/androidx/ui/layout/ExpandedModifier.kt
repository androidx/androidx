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

package androidx.ui.layout

import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.DensityScope
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.withTight

/**
 * A layout modifier that forces a target component to fill all available width.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedWidthModifier
 */
val ExpandedWidth: LayoutModifier = ExpandedModifier.ExpandedWidth

/**
 * A layout modifier that forces a a target component to fill all available height.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedHeightModifier
 */
val ExpandedHeight: LayoutModifier = ExpandedModifier.ExpandedHeight

/**
 * A layout modifier that forces a target component to fill all available space.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedModifier
 */
val Expanded: LayoutModifier = ExpandedModifier.Expanded

/**
 * A layout modifier that forces a target component to occupy all available space in a given
 * dimension.
 */
private sealed class ExpandedModifier : LayoutModifier {
    object ExpandedWidth : ExpandedModifier() {
        override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
            return if (constraints.hasBoundedWidth) {
                constraints.withTight(width = constraints.maxWidth)
            } else {
                constraints
            }
        }
    }

    object ExpandedHeight : ExpandedModifier() {
        override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
            return if (constraints.hasBoundedHeight) {
                constraints.withTight(height = constraints.maxHeight)
            } else {
                constraints
            }
        }
    }

    object Expanded : ExpandedModifier() {
        override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
            return if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                Constraints.tightConstraints(constraints.maxWidth, constraints.maxHeight)
            } else {
                constraints
            }
        }
    }

    abstract override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints

    override fun DensityScope.modifySize(
        constraints: Constraints,
        childSize: IntPxSize
    ): IntPxSize = childSize

    override fun DensityScope.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.minIntrinsicWidth(height)

    override fun DensityScope.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.maxIntrinsicWidth(height)

    override fun DensityScope.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.minIntrinsicHeight(width)

    override fun DensityScope.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.maxIntrinsicHeight(width)

    override fun DensityScope.modifyPosition(
        childPosition: IntPxPosition,
        childSize: IntPxSize,
        containerSize: IntPxSize
    ): IntPxPosition = childPosition

    override fun DensityScope.modifyAlignmentLine(line: AlignmentLine, value: IntPx?): IntPx? =
        value
}