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
 * distributed under the License is distributed on an "AS IS" BASIS,yout
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
 * A [Modifier.Element] that changes how its wrapped content is measured and laid out.
 * It has the same measurement and layout functionality as the [androidx.ui.core.Layout]
 * component, while wrapping exactly one layout due to it being a modifier. In contrast,
 * the [androidx.ui.core.Layout] component is used to define the layout behavior of
 * multiple children.
 *
 * @see androidx.ui.core.Layout
 */
interface LayoutModifier2 : Modifier.Element {
    /**
     * The function used to measure the modifier. The [measurable] corresponds to the
     * wrapped content, and it can be measured with the desired constraints according
     * to the logic of the [LayoutModifier2]. The modifier needs to choose its own
     * size, which can depend on the size chosen by the wrapped content (the obtained
     * [Placeable]), if the wrapped content was measured. The size needs to be returned
     * as part of a [MeasureScope.LayoutResult], alongside the placement logic of the
     * [Placeable], which defines how the wrapped content should be positioned inside
     * the [LayoutModifier2]. A convenient way to create the [MeasureScope.LayoutResult]
     * is to use the [MeasureScope.layout] factory function.
     */
    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.LayoutResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
     */
    fun Density.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.minWidth(
        this@LayoutModifier2,
        this,
        measurable,
        height,
        layoutDirection
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun Density.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.minHeight(
        this@LayoutModifier2,
        this,
        measurable,
        width,
        layoutDirection
    )

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun Density.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.maxWidth(
        this@LayoutModifier2,
        this,
        measurable,
        height,
        layoutDirection
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun Density.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.maxHeight(
        this@LayoutModifier2,
        this,
        measurable,
        width,
        layoutDirection
    )
}

// TODO(popam): deduplicate from the copy-pasted logic of Layout.kt without making it public
private object MeasuringIntrinsics {
    internal fun minWidth(
        modifier: LayoutModifier2,
        density: Density,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(density).measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.width
    }

    internal fun minHeight(
        modifier: LayoutModifier2,
        density: Density,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(density).measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.height
    }

    internal fun maxWidth(
        modifier: LayoutModifier2,
        density: Density,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(density).measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.width
    }

    internal fun maxHeight(
        modifier: LayoutModifier2,
        density: Density,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(density).measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.height
    }

    private class DefaultIntrinsicMeasurable(
        val measurable: IntrinsicMeasurable,
        val minMax: IntrinsicMinMax,
        val widthHeight: IntrinsicWidthHeight
    ) : Measurable {
        override val parentData: Any?
            get() = measurable.parentData

        override fun measure(constraints: Constraints): Placeable {
            if (widthHeight == IntrinsicWidthHeight.Width) {
                val width = if (minMax == IntrinsicMinMax.Max) {
                    measurable.maxIntrinsicWidth(constraints.maxHeight)
                } else {
                    measurable.minIntrinsicWidth(constraints.maxHeight)
                }
                return DummyPlaceable(width, constraints.maxHeight)
            }
            val height = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicHeight(constraints.maxWidth)
            } else {
                measurable.minIntrinsicHeight(constraints.maxWidth)
            }
            return DummyPlaceable(constraints.maxWidth, height)
        }

        override fun minIntrinsicWidth(height: IntPx): IntPx {
            return measurable.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: IntPx): IntPx {
            return measurable.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: IntPx): IntPx {
            return measurable.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: IntPx): IntPx {
            return measurable.maxIntrinsicHeight(width)
        }
    }

    private class IntrinsicsMeasureScope(
        density: Density
    ) : MeasureScope(), Density by density

    private class DummyPlaceable(width: IntPx, height: IntPx) : Placeable() {
        override fun get(line: AlignmentLine): IntPx? = null
        override val size = IntPxSize(width, height)
        override fun performPlace(position: IntPxPosition) { }
    }

    private enum class IntrinsicMinMax { Min, Max }
    private enum class IntrinsicWidthHeight { Width, Height }
}

/**
 * A [Modifier.Element] that changes the way a UI component is measured and laid out.
 */
@Deprecated("This interface is deprecated and will be removed in a future release. " +
        "The LayoutModifier2 should be used instead. " +
        "See LayoutPadding as an example of how to use the new API.")
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
