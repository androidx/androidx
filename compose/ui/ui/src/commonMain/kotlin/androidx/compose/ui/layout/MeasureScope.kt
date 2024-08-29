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

import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.LookaheadCapablePlaceable
import androidx.compose.ui.node.checkMeasuredSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@DslMarker annotation class MeasureScopeMarker

/**
 * The receiver scope of a layout's measure lambda. The return value of the measure lambda is
 * [MeasureResult], which should be returned by [layout]
 */
@MeasureScopeMarker
@JvmDefaultWithCompatibility
interface MeasureScope : IntrinsicMeasureScope {
    /**
     * Sets the size and alignment lines of the measured layout, as well as the positioning block
     * that defines the children positioning logic. The [placementBlock] is a lambda used for
     * positioning children. [Placeable.placeAt] should be called on children inside placementBlock.
     * The [alignmentLines] can be used by the parent layouts to decide layout, and can be queried
     * using the [Placeable.get] operator. Note that alignment lines will be inherited by parent
     * layouts, such that indirect parents will be able to query them as well.
     *
     * @param width the measured width of the layout
     * @param height the measured height of the layout
     * @param alignmentLines the alignment lines defined by the layout
     * @param placementBlock block defining the children positioning of the current layout
     */
    fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<out AlignmentLine, Int> = emptyMap(),
        placementBlock: Placeable.PlacementScope.() -> Unit
    ) = layout(width, height, alignmentLines, null, placementBlock)

    /**
     * Sets the size and alignment lines of the measured layout, as well as the positioning block
     * that defines the children positioning logic. The [placementBlock] is a lambda used for
     * positioning children. [Placeable.placeAt] should be called on children inside placementBlock.
     * The [alignmentLines] can be used by the parent layouts to decide layout, and can be queried
     * using the [Placeable.get] operator. Note that alignment lines will be inherited by parent
     * layouts, such that indirect parents will be able to query them as well.
     *
     * @param width the measured width of the layout
     * @param height the measured height of the layout
     * @param alignmentLines the alignment lines defined by the layout
     * @param rulers a method to set Ruler values used by all placed children
     * @param placementBlock block defining the children positioning of the current layout
     */
    @Suppress("PrimitiveInCollection")
    fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<out AlignmentLine, Int> = emptyMap(),
        rulers: (RulerScope.() -> Unit)? = null,
        placementBlock: Placeable.PlacementScope.() -> Unit
    ): MeasureResult {
        checkMeasuredSize(width, height)
        return object : MeasureResult {
            override val width = width
            override val height = height
            override val alignmentLines = alignmentLines
            override val rulers = rulers

            override fun placeChildren() {
                // This isn't called from anywhere inside the compose framework. This might
                // be called by tests or external frameworks.
                if (this@MeasureScope is LookaheadCapablePlaceable) {
                    placementScope.placementBlock()
                } else {
                    SimplePlacementScope(width, layoutDirection).placementBlock()
                }
            }
        }
    }
}

/**
 * This is used by the default implementation of [MeasureScope.layout] and will never be called by
 * any implementation of [MeasureScope] in the compose framework.
 */
private class SimplePlacementScope(
    override val parentWidth: Int,
    override val parentLayoutDirection: LayoutDirection,
) : Placeable.PlacementScope()

/**
 * A scope used in [MeasureScope.layout] for the `rulers` parameter to allow a layout to define
 * [Ruler] values for children.
 *
 * @sample androidx.compose.ui.samples.RulerProducerUsage
 */
@MeasureScopeMarker
interface RulerScope : Density {
    /**
     * [LayoutCoordinates] of the position in the hierarchy that the [Ruler] will be
     * [provided][Ruler.provides].
     */
    val coordinates: LayoutCoordinates

    /** Provides a constant value for a [Ruler]. */
    infix fun Ruler.provides(value: Float)

    /**
     * Provides a [VerticalRuler] value that is relative to the left side in an LTR layout or right
     * side on an RTL layout.
     */
    infix fun VerticalRuler.providesRelative(value: Float)
}
