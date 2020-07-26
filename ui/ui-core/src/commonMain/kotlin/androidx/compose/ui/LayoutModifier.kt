/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [Modifier.Element] that changes how its wrapped content is measured and laid out.
 * It has the same measurement and layout functionality as the [androidx.compose.ui.Layout]
 * component, while wrapping exactly one layout due to it being a modifier. In contrast,
 * the [androidx.compose.ui.Layout] component is used to define the layout behavior of
 * multiple children.
 *
 * @see androidx.compose.ui.Layout
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
        height: Int,
        layoutDirection: LayoutDirection
    ): Int = MeasuringIntrinsics.minWidth(
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
        width: Int,
        layoutDirection: LayoutDirection
    ): Int = MeasuringIntrinsics.minHeight(
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
        height: Int,
        layoutDirection: LayoutDirection
    ): Int = MeasuringIntrinsics.maxWidth(
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
        width: Int,
        layoutDirection: LayoutDirection
    ): Int = MeasuringIntrinsics.maxHeight(
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
        h: Int,
        layoutDirection: LayoutDirection
    ): Int {
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
        w: Int,
        layoutDirection: LayoutDirection
    ): Int {
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
        h: Int,
        layoutDirection: LayoutDirection
    ): Int {
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
        w: Int,
        layoutDirection: LayoutDirection
    ): Int {
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

        override fun minIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int {
            return measurable.minIntrinsicWidth(height, layoutDirection)
        }

        override fun maxIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int {
            return measurable.maxIntrinsicWidth(height, layoutDirection)
        }

        override fun minIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
            return measurable.minIntrinsicHeight(width, layoutDirection)
        }

        override fun maxIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
            return measurable.maxIntrinsicHeight(width, layoutDirection)
        }
    }

    private class IntrinsicsMeasureScope(
        density: Density,
        override val layoutDirection: LayoutDirection
    ) : MeasureScope(), Density by density

    private class DummyPlaceable(width: Int, height: Int) : Placeable() {
        init {
            measuredSize = IntSize(width, height)
        }
        override fun get(line: AlignmentLine): Int = AlignmentLine.Unspecified
        override fun place(position: IntOffset) { }
    }

    private enum class IntrinsicMinMax { Min, Max }
    private enum class IntrinsicWidthHeight { Width, Height }
}

/**
 * Creates a [LayoutModifier] that allows changing how the wrapped element is measured and laid out.
 *
 * This is a convenience API of creating a custom [LayoutModifier] modifier, without having to
 * create a class or an object that implements the [LayoutModifier] interface. The intrinsic
 * measurements follow the default logic provided by the [LayoutModifier].
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.ConvenienceLayoutModifierSample
 *
 * @see androidx.compose.ui.LayoutModifier
 */
fun Modifier.layout(
    measure: MeasureScope.(Measurable, Constraints) -> MeasureScope.MeasureResult
) = this.then(LayoutModifierImpl(measure))

private data class LayoutModifierImpl(
    val measure: MeasureScope.(Measurable, Constraints) -> MeasureScope.MeasureResult
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ) = measure(measurable, constraints)
}