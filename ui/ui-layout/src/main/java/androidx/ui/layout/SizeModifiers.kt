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

import androidx.ui.core.Constraints
import androidx.ui.core.DensityScope
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.coerceIn
import androidx.ui.core.enforce
import androidx.ui.core.isFinite

/**
 * A layout modifier that sets the exact width and height of the component.
 *
 * @sample androidx.ui.layout.samples.SimpleSizeModifier
 *
 * If the [Size] modifier's target layout is measured with constraints that do not allow the
 * specified width and/or height, the size will be coerced inside the incoming constraints.
 */
fun Size(width: Dp, height: Dp): LayoutModifier {
    require(width.isFinite() && height.isFinite()) {
        "Width and height parameters should be finite."
    }
    return SizeModifier(DpConstraints.tightConstraints(width = width, height = height))
}

/**
 * A layout modifier that sets the minimum width and height of the component.
 *
 * If the [MinSize] modifier's target layout is measured with constraints that do not allow the
 * specified minimum width and/or height, the minimum size will be coerced inside the incoming
 * constraints.
 */
fun MinSize(minWidth: Dp, minHeight: Dp): LayoutModifier {
    require(minWidth.isFinite() && minHeight.isFinite()) {
        "MinWidth and minHeight parameters should be finite."
    }
    return SizeModifier(DpConstraints(minWidth = minWidth, minHeight = minHeight))
}

/**
 * A layout modifier that sets the maximum width and height of the component.
 *
 * If the [MaxSize] modifier's target layout is measured with constraints that do not allow the
 * specified maximum width and/or height, the maximum size will be coerced inside the incoming
 * constraints.
 */
fun MaxSize(maxWidth: Dp, maxHeight: Dp): LayoutModifier =
    SizeModifier(DpConstraints(maxWidth = maxWidth, maxHeight = maxHeight))

/**
 * A layout modifier that sets the exact width of the component.
 *
 * @sample androidx.ui.layout.samples.SimpleWidthModifier
 *
 * If the [Width] modifier's target layout is measured with constraints that do not allow the
 * specified width, the width will be coerced inside the incoming constraints.
 */
fun Width(value: Dp): LayoutModifier {
    require(value.isFinite()) { "Width value parameter should be finite." }
    return SizeModifier(DpConstraints.tightConstraintsForWidth(value))
}

/**
 * A layout modifier that sets the minimum width of the component.
 *
 * If the [MinWidth] modifier's target layout is measured with constraints that do not allow the
 * specified minimum width, the minimum width will be coerced inside the incoming constraints.
 */
fun MinWidth(value: Dp): LayoutModifier {
    require(value.isFinite()) { "MinWidth value parameter should be finite." }
    return SizeModifier(DpConstraints(minWidth = value))
}

/**
 * A layout modifier that sets the maximum width of the component.
 *
 * If the [MaxWidth] modifier's target layout is measured with constraints that do not allow the
 * specified maximum width, the maximum width will be coerced inside the incoming constraints.
 */
fun MaxWidth(value: Dp): LayoutModifier = SizeModifier(DpConstraints(maxWidth = value))

/**
 * A layout modifier that sets the exact height of the component.
 *
 * @sample androidx.ui.layout.samples.SimpleHeightModifier
 *
 * If the [Height] modifier's target layout is measured with constraints that do not allow the
 * specified height, the height will be coerced inside the incoming constraints.
 */
fun Height(value: Dp): LayoutModifier {
    require(value.isFinite()) { "Height value parameter should be finite." }
    return SizeModifier(DpConstraints.tightConstraintsForHeight(value))
}

/**
 * A layout modifier that sets the minimum height of the component.
 *
 * If the [MinHeight] modifier's target layout is measured with constraints that do not allow the
 * specified minimum height, the minimum height will be coerced inside the incoming constraints.
 */
fun MinHeight(value: Dp): LayoutModifier {
    require(value.isFinite()) { "MinHeight value parameter should be finite." }
    return SizeModifier(DpConstraints(minHeight = value))
}

/**
 * A layout modifier that sets the maximum height of the component.
 *
 * If the [MaxHeight] modifier's target layout is measured with constraints that do not allow the
 * specified maximum height, the maximum height will be coerced inside the incomingL constraints.
 */
fun MaxHeight(value: Dp): LayoutModifier = SizeModifier(DpConstraints(maxHeight = value))

private data class SizeModifier(private val modifierConstraints: DpConstraints) : LayoutModifier {
    override fun DensityScope.modifyConstraints(constraints: Constraints) =
        Constraints(modifierConstraints).enforce(constraints)

    override fun DensityScope.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.minIntrinsicWidth(height).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

    override fun DensityScope.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.maxIntrinsicWidth(height).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

    override fun DensityScope.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.minIntrinsicHeight(width).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minHeight, constraints.maxHeight)
        }

    override fun DensityScope.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.maxIntrinsicHeight(width).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minHeight, constraints.maxHeight)
        }
}