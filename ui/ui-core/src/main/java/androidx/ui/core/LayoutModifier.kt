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

/**
 * A [Modifier.Element] that changes how its wrapped content is measured and laid out.
 * It has the same measurement and layout functionality as the [androidx.ui.core.Layout]
 * component, while wrapping exactly one layout due to it being a modifier. In contrast,
 * the [androidx.ui.core.Layout] component is used to define the layout behavior of
 * multiple children.
 *
 * @see androidx.ui.core.Layout
 */
interface LayoutModifier : Modifier.Element {
    /**
     * The function used to measure the modifier. The [measurable] corresponds to the
     * wrapped content, and it can be measured with the desired constraints according
     * to the logic of the [LayoutModifier]. The modifier needs to choose its own
     * size, which can depend on the size chosen by the wrapped content (the obtained
     * [Placeable]), if the wrapped content was measured. The size needs to be returned
     * as part of a [MeasureScope.MeasureResult], alongside the placement logic of the
     * [Placeable], which defines how the wrapped content should be positioned inside
     * the [LayoutModifier]. A convenient way to create the [MeasureScope.MeasureResult]
     * is to use the [MeasureScope.layout] factory function.
     */
    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.minWidth(
        this@LayoutModifier,
        this,
        measurable,
        height,
        layoutDirection
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.minHeight(
        this@LayoutModifier,
        this,
        measurable,
        width,
        layoutDirection
    )

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.maxWidth(
        this@LayoutModifier,
        this,
        measurable,
        height,
        layoutDirection
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = MeasuringIntrinsics.maxHeight(
        this@LayoutModifier,
        this,
        measurable,
        width,
        layoutDirection
    )
}

// TODO(popam): deduplicate from the copy-pasted logic of Layout.kt without making it public
private object MeasuringIntrinsics {
    internal fun minWidth(
        modifier: LayoutModifier,
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
            val receiver = IntrinsicsMeasureScope(density, layoutDirection)
            receiver.measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.width
    }

    internal fun minHeight(
        modifier: LayoutModifier,
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
            val receiver = IntrinsicsMeasureScope(density, layoutDirection)
            receiver.measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.height
    }

    internal fun maxWidth(
        modifier: LayoutModifier,
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
            val receiver = IntrinsicsMeasureScope(density, layoutDirection)
            receiver.measure(measurable, constraints, layoutDirection)
        }
        return layoutResult.width
    }

    internal fun maxHeight(
        modifier: LayoutModifier,
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
            val receiver = IntrinsicsMeasureScope(density, layoutDirection)
            receiver.measure(measurable, constraints, layoutDirection)
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

        override fun measure(
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): Placeable {
            if (widthHeight == IntrinsicWidthHeight.Width) {
                val width = if (minMax == IntrinsicMinMax.Max) {
                    measurable.maxIntrinsicWidth(constraints.maxHeight, layoutDirection)
                } else {
                    measurable.minIntrinsicWidth(constraints.maxHeight, layoutDirection)
                }
                return DummyPlaceable(width, constraints.maxHeight)
            }
            val height = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicHeight(constraints.maxWidth, layoutDirection)
            } else {
                measurable.minIntrinsicHeight(constraints.maxWidth, layoutDirection)
            }
            return DummyPlaceable(constraints.maxWidth, height)
        }

        override fun minIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection): IntPx {
            return measurable.minIntrinsicWidth(height, layoutDirection)
        }

        override fun maxIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection): IntPx {
            return measurable.maxIntrinsicWidth(height, layoutDirection)
        }

        override fun minIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection): IntPx {
            return measurable.minIntrinsicHeight(width, layoutDirection)
        }

        override fun maxIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection): IntPx {
            return measurable.maxIntrinsicHeight(width, layoutDirection)
        }
    }

    private class IntrinsicsMeasureScope(
        density: Density,
        override val layoutDirection: LayoutDirection
    ) : MeasureScope(), Density by density

    private class DummyPlaceable(width: IntPx, height: IntPx) : Placeable() {
        override fun get(line: AlignmentLine): IntPx? = null
        override val measurementConstraints = Constraints()
        override val measuredSize = IntPxSize(width, height)
        override fun place(position: IntPxPosition) { }
    }

    private enum class IntrinsicMinMax { Min, Max }
    private enum class IntrinsicWidthHeight { Width, Height }
}
