/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt

/**
 * Declares a callback, [block], that allows controlling size and placement.The incoming measurement
 * [Constraints] may override values provided by [block], forcing the content to be either smaller
 * or larger.
 *
 * @param block called during layout to determine the size of blocks. This l
 */
@ExperimentalFoundationStyleApi
public fun Modifier.placement(block: PlacementModifierScope.() -> Unit): Modifier =
    this then PlacementElement(block)

/**
 * The scope to the [block][Modifier.placement] callback in of the [placement][Modifier.placement]
 * modifier.
 */
@ExperimentalFoundationStyleApi
public class PlacementModifierScope {
    /**
     * Offsets the component horizontally from its original calculated left position. Positive
     * values shift the component to the right, negative to the left.
     */
    public var left: Dp = Dp.Unspecified

    /**
     * Offsets the component horizontally from its original calculated right position. Positive
     * values shift the component to the left (further from the right edge), negative to the right.
     */
    public var right: Dp = Dp.Unspecified

    /**
     * Offsets the component vertically from its original calculated top position. Positive values
     * shift the component downwards, negative upwards.
     */
    public var top: Dp = Dp.Unspecified

    /**
     * Offsets the component vertically from its original calculated bottom position. Positive
     * values shift the component upwards (further from the bottom edge), negative downwards.
     */
    public var bottom: Dp = Dp.Unspecified

    /**
     * Constrains the minimum width of the component. The component's width, including padding, will
     * be at least this value.
     *
     * If both [minWidth] and [maxWidth] are specified and [minWidth] is greater than [maxWidth]
     * [minWidth] will be clamped to [maxWidth].
     *
     * If [left] and/or [right] are specified, [maxWidth] is clamped to the remaining horizontal
     * space remaining in the constraints after the [left] and [right] are removed.
     */
    public var minWidth: Dp = Dp.Unspecified

    /**
     * Constrains the maximum width of the component. The component's width, including padding, will
     * be at most this value.
     *
     * If [maxWidth] is negative it is treated as being `0.dp`.
     *
     * If [left] and/or [right] are specified, [maxWidth] is clamped to the remaining horizontal
     * space remaining in the constraints after the [left] and [right] are removed.
     */
    public var maxWidth: Dp = Dp.Unspecified

    /**
     * Constrains the minimum height of the component. The component's height, including padding,
     * will be at least this value.
     *
     * If both [minHeight] and [maxHeight] are specified and [minHeight] is greater than [maxHeight]
     * [minHeight] will be clamped to [maxHeight].
     *
     * If [top] and/or [bottom] are specified, [minHeight] is clamped to the remaining vertical
     * space remaining in the constraints in the constraints after the [top] and [bottom] are
     * removed.
     */
    public var minHeight: Dp = Dp.Unspecified

    /**
     * Constrains the maximum height of the component. The component's height, including padding,
     * will be at most this value.
     *
     * If [maxHeight] is negative it is treated as being `0.dp`.
     *
     * If [top] and/or [bottom] are specified, [maxHeight] is clamped to the remaining vertical
     * space remaining in the constraints after the [top] and [bottom] are removed.
     */
    public var maxHeight: Dp = Dp.Unspecified

    /**
     * Sets the preferred width of the component. The actual size will also depend on the parent's
     * constraints and other modifiers.
     */
    public var width: Breadth = Breadth.None

    /**
     * Sets the preferred height of the component. The actual size will also depend on the parent's
     * constraints and other modifiers.
     */
    public var height: Breadth = Breadth.None
}

@ExperimentalFoundationStyleApi
internal class PlacementElement(val block: PlacementModifierScope.() -> Unit) :
    ModifierNodeElement<PlacementNode>() {
    override fun create(): PlacementNode = PlacementNode(block)

    override fun update(node: PlacementNode) {
        node.block = block
    }

    override fun hashCode(): Int = block.hashCode()

    override fun equals(other: Any?): Boolean = other is PlacementElement && other.block === block

    override fun InspectorInfo.inspectableProperties() {
        name = "placement"
        properties["block"] = block
    }
}

@ExperimentalFoundationStyleApi
internal class PlacementNode(var block: PlacementModifierScope.() -> Unit) :
    DelegatingNode(), LayoutModifierNode, ObserverModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val scope = PlacementModifierScope().apply { observeReads { block() } }
        val start = scope.left.toPx()
        val end = scope.right.toPx()
        val top = scope.top.toPx()
        val bottom = scope.bottom.toPx()

        val horizontal = (start + end).fastRoundToInt()
        val vertical = (top + bottom).fastRoundToInt()

        var minWidth = (constraints.minWidth - horizontal).fastCoerceAtLeast(0)
        var maxWidth = addMaxWithMinimum(constraints.maxWidth, horizontal)
        var minHeight = (constraints.minHeight - vertical).fastCoerceAtLeast(0)
        var maxHeight = addMaxWithMinimum(constraints.maxHeight, vertical)

        // Resolve width constraints from Style
        var styleMinWidth = 0
        var styleMaxWidth = Constraints.Infinity

        scope.maxWidth.ifSpecified {
            styleMaxWidth = it.toPx().fastRoundToInt().fastCoerceAtLeast(0)
        }
        scope.minWidth.ifSpecified {
            styleMinWidth = it.toPx().fastRoundToInt().fastCoerceIn(0, styleMaxWidth)
        }
        scope.width.ifDistance {
            styleMinWidth = it.toPx().fastRoundToInt().fastCoerceIn(styleMinWidth, styleMaxWidth)
            styleMaxWidth = styleMinWidth
        }

        // Apply style width constraints to adjusted incoming constraints
        minWidth =
            if (styleMinWidth == 0) {
                minWidth
            } else {
                styleMinWidth.fastCoerceIn(minWidth, maxWidth)
            }
        maxWidth =
            if (styleMaxWidth == Constraints.Infinity) {
                maxWidth
            } else {
                styleMaxWidth.fastCoerceIn(minWidth, maxWidth)
            }

        // Handle fractional width, after adjusting for the incoming constraints
        scope.width.ifFraction {
            if (constraints.hasBoundedWidth) {
                val width = (maxWidth * it).fastRoundToInt().fastCoerceIn(minWidth, maxWidth)
                minWidth = width
                maxWidth = width
            } else if (scope.left.isSpecified && scope.right.isSpecified) {
                minWidth = maxWidth
            }
        }

        // Resolve height constraints from Style
        var styleMinHeight = 0
        var styleMaxHeight = Constraints.Infinity

        scope.maxHeight.ifSpecified {
            styleMaxHeight = it.toPx().fastRoundToInt().fastCoerceAtLeast(0)
        }
        scope.minHeight.ifSpecified {
            styleMinHeight = it.toPx().fastRoundToInt().fastCoerceIn(0, styleMaxHeight)
        }
        scope.height.ifDistance {
            styleMinHeight = it.toPx().fastRoundToInt().fastCoerceIn(styleMinHeight, styleMaxHeight)
            styleMaxHeight = styleMinHeight
        }

        // Apply style height constraints to adjusted incoming constraints
        minHeight =
            if (styleMinHeight == 0) {
                minHeight
            } else {
                styleMinHeight.fastCoerceIn(minHeight, maxHeight)
            }
        maxHeight =
            if (styleMaxHeight == Constraints.Infinity) {
                maxHeight
            } else {
                styleMaxHeight.fastCoerceIn(minHeight, maxHeight)
            }

        // Handle fractional height, after adjusting for the incoming constraints
        scope.height.ifFraction {
            if (constraints.hasBoundedHeight) {
                val height = (maxHeight * it).fastRoundToInt().fastCoerceIn(minHeight, maxHeight)
                minHeight = height
                maxHeight = height
            } else if (scope.top.isSpecified && scope.bottom.isSpecified) {
                minHeight = maxHeight
            }
        }

        val placeable = measurable.measure(Constraints(minWidth, maxWidth, minHeight, maxHeight))
        return layout(placeable.width + horizontal, placeable.height + vertical) {
            val x =
                if (scope.left.isUnspecified && scope.right.isSpecified) {
                    constraints.maxWidth - placeable.width - end.fastRoundToInt()
                } else {
                    start.fastRoundToInt()
                }
            val y =
                if (scope.bottom.isSpecified && scope.top.isUnspecified) {
                    constraints.maxHeight - placeable.height - bottom.fastRoundToInt()
                } else {
                    top.fastRoundToInt()
                }
            placeable.place(x, y)
        }
    }

    override fun onObservedReadsChanged() {
        invalidateMeasurement()
        invalidatePlacement()
    }
}

private inline fun Dp.ifSpecified(block: (Dp) -> Unit) {
    if (isSpecified) block(this)
}

@ExperimentalFoundationStyleApi
private inline fun Breadth.ifDistance(block: (Dp) -> Unit) {
    if (this is Breadth.Distance) block(value)
}

@ExperimentalFoundationStyleApi
private inline fun Breadth.ifFraction(block: (Float) -> Unit) {
    if (this is Breadth.Fraction) block(value)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun addMaxWithMinimum(max: Int, value: Int): Int {
    return if (max == Constraints.Infinity) {
        max
    } else {
        (max + value).fastCoerceAtLeast(0)
    }
}

/**
 * Maps properties set in a style and resolved by [styleResolver] to values passed to the
 * [Modifier.placement] modifier.
 *
 * NOTE: [stylePlacement] is a transitionary modifier that will be removed before styles becomes
 * stable. It helps transition away from the experimental version of styles in 1.11 and 1.12.
 *
 * The properties used by this modifier are being replace by design-system specific properties
 * defined in design system layers above foundation, such as Material.
 */
@ExperimentalFoundationStyleApi
public fun Modifier.stylePlacement(styleResolver: StyleResolver): Modifier = placement {
    styleResolver.resolve {
        ifSet(topProperty) { top = it }
        ifSet(bottomProperty) { bottom = it }
        ifSet(leftProperty) { left = it }
        ifSet(rightProperty) { right = it }
        ifSet(minHeightProperty) { minHeight = it }
        ifSet(maxHeightProperty) { maxHeight = it }
        ifSet(minWidthProperty) { minWidth = it }
        ifSet(maxWidthProperty) { maxWidth = it }
        ifSet(widthProperty) { width = it }
        ifSet(heightProperty) { height = it }
    }
}
