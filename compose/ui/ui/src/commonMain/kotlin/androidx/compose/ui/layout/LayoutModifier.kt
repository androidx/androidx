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

package androidx.compose.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A [Modifier.Element] that changes how its wrapped content is measured and laid out.
 * It has the same measurement and layout functionality as the [androidx.compose.ui.layout.Layout]
 * component, while wrapping exactly one layout due to it being a modifier. In contrast,
 * the [androidx.compose.ui.layout.Layout] component is used to define the layout behavior of
 * multiple children.
 *
 * @sample androidx.compose.ui.samples.LayoutModifierSample
 *
 * @see androidx.compose.ui.layout.Layout
 */
@JvmDefaultWithCompatibility
interface LayoutModifier : Modifier.Element {
    /**
     * The function used to measure the modifier. The [measurable] corresponds to the
     * wrapped content, and it can be measured with the desired constraints according
     * to the logic of the [LayoutModifier]. The modifier needs to choose its own
     * size, which can depend on the size chosen by the wrapped content (the obtained
     * [Placeable]), if the wrapped content was measured. The size needs to be returned
     * as part of a [MeasureResult], alongside the placement logic of the
     * [Placeable], which defines how the wrapped content should be positioned inside
     * the [LayoutModifier]. A convenient way to create the [MeasureResult]
     * is to use the [MeasureScope.layout] factory function.
     *
     * A [LayoutModifier] uses the same measurement and layout concepts and principles as a
     * [Layout], the only difference is that they apply to exactly one child. For a more detailed
     * explanation of measurement and layout, see [MeasurePolicy].
     */
    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = MeasuringIntrinsics.minWidth(
        this@LayoutModifier,
        this,
        measurable,
        height
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = MeasuringIntrinsics.minHeight(
        this@LayoutModifier,
        this,
        measurable,
        width
    )

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = MeasuringIntrinsics.maxWidth(
        this@LayoutModifier,
        this,
        measurable,
        height
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = MeasuringIntrinsics.maxHeight(
        this@LayoutModifier,
        this,
        measurable,
        width
    )
}

// TODO(popam): deduplicate from the copy-pasted logic of Layout.kt without making it public
private object MeasuringIntrinsics {
    fun minWidth(
        modifier: LayoutModifier,
        intrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(intrinsicMeasureScope, intrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.width
    }

    fun minHeight(
        modifier: LayoutModifier,
        intrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(intrinsicMeasureScope, intrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.height
    }

    fun maxWidth(
        modifier: LayoutModifier,
        intrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(intrinsicMeasureScope, intrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.width
    }

    fun maxHeight(
        modifier: LayoutModifier,
        intrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(modifier) {
            IntrinsicsMeasureScope(intrinsicMeasureScope, intrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
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
                val height =
                    if (constraints.hasBoundedHeight) constraints.maxHeight else LargeDimension
                return EmptyPlaceable(width, height)
            }
            val height = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicHeight(constraints.maxWidth)
            } else {
                measurable.minIntrinsicHeight(constraints.maxWidth)
            }
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else LargeDimension
            return EmptyPlaceable(width, height)
        }

        override fun minIntrinsicWidth(height: Int): Int {
            return measurable.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            return measurable.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            return measurable.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            return measurable.maxIntrinsicHeight(width)
        }
    }

    private class EmptyPlaceable(width: Int, height: Int) : Placeable() {
        init {
            measuredSize = IntSize(width, height)
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
        }
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
 * @see androidx.compose.ui.layout.LayoutModifier
 */
fun Modifier.layout(
    measure: MeasureScope.(Measurable, Constraints) -> MeasureResult
) = this then LayoutElement(measure)

private data class LayoutElement(
    val measure: MeasureScope.(Measurable, Constraints) -> MeasureResult
) : ModifierNodeElement<LayoutModifierImpl>() {
    override fun create() = LayoutModifierImpl(measure)

    override fun update(node: LayoutModifierImpl) {
        node.measureBlock = measure
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layout"
        properties["measure"] = measure
    }
}

internal class LayoutModifierImpl(
    var measureBlock: MeasureScope.(Measurable, Constraints) -> MeasureResult
) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ) = measureBlock(measurable, constraints)

    override fun toString(): String {
        return "LayoutModifierImpl(measureBlock=$measureBlock)"
    }
}
