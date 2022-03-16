/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import kotlin.math.PI
import kotlin.math.min

/**
 * Specifies how components will be laid down with respect to the anchor.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class AnchorType internal constructor(internal val ratio: Float) {
    companion object {
        /**
         * Start the content of the [CurvedLayout] on the anchor
         */
        val Start = AnchorType(0f)

        /**
         * Center the content of the [CurvedLayout] around the anchor
         */
        val Center = AnchorType(0.5f)

        /**
         * End the content of the [CurvedLayout] on the anchor
         */
        val End = AnchorType(1f)
    }

    override fun toString(): String {
        return when (this) {
            Center -> "AnchorType.Center"
            Start -> "AnchorType.Start"
            End -> "AnchorType.End"
            else -> "unknown"
        }
    }
}

/**
 * A layout composable that places its children in an arc, rotating them as needed.
 * This will layout children using a [curvedRow], that similar to a [Row] layout,
 * that it's curved into a segment of an annulus.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.SimpleCurvedWorld
 *
 * @param modifier The modifier to be applied to the CurvedRow.
 * @param anchor The angle at which children are laid out relative to, in degrees. An angle of 0
 * corresponds to the right (3 o'clock on a watch), 90 degrees is bottom (6 o'clock), and so on.
 * Default is 270 degrees (top of the screen)
 * @param anchorType Specify how the content is drawn with respect to the anchor. Default is to
 * center the content on the anchor.
 * @param radialAlignment Specifies the radial alignment for children, if not specified, children
 * can choose their own radial Alignment. Alignment specifies where to lay down children that are
 * thiner than the CurvedRow, either closer to the center (INNER), apart from the center (OUTER) or
 * in the middle point (CENTER).
 * @param clockwise Specify if the children are laid out clockwise (the default) or
 * counter-clockwise
 * @param contentBuilder Specifies the content of this layout, currently there are 4 available
 * elements defined in foundations for this DSL: the sub-layouts [curvedRow] and [curvedColumn],
 * [basicCurvedText] and [curvedComposable] (used to add normal composables to curved layouts)
 */
@Composable
public fun CurvedLayout(
    modifier: Modifier = Modifier,
    anchor: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
    // TODO: reimplement as modifiers
    radialAlignment: CurvedAlignment.Radial? = null,
    clockwise: Boolean = true,
    contentBuilder: CurvedScope.() -> Unit
) {
    // Note that all angles in the function are in radians, and the anchor parameter is in degrees

    val curvedRowChild by remember {
        derivedStateOf {
            CurvedRowChild(clockwise, radialAlignment, contentBuilder)
        }
    }

    Layout(
        modifier = modifier.drawWithContent {
            drawContent()
            with(curvedRowChild) { draw() }
        },

        content = {
            curvedRowChild.SubComposition()
        }
    ) { measurables, constraints ->
        require(constraints.hasBoundedHeight || constraints.hasBoundedWidth)
        // We take as much room as possible, the same in both dimensions, within the constraints
        val diameter = min(
            if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE,
            if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE,
        )
        val radius = diameter / 2f

        // Give the curved row scope the information needed to measure and map measurables
        // to children.
        with(curvedRowChild) {
            val mapped = initializeMeasure(measurables, 0)
            require(mapped == measurables.size)
        }

        curvedRowChild.estimateThickness(radius)

        curvedRowChild.radialPosition(
            parentOuterRadius = radius,
            parentThickness = curvedRowChild.estimatedThickness,
        )

        val totalSweep = curvedRowChild.sweepRadians

        // Apply anchor & anchorType
        var layoutAngleStart = anchor.toRadians() -
            (if (clockwise) anchorType.ratio else 1f - anchorType.ratio) * totalSweep

        curvedRowChild.angularPosition(layoutAngleStart, totalSweep, Offset(radius, radius))

        // Place the composable children
        layout(diameter, diameter) {
            with(curvedRowChild) { placeIfNeeded() }
        }
    }
}

/**
 * Class representing the dimensions of an annulus segment. Used for [CurvedLayout] and its
 * children.
 *
 * @param outerRadius The distance from the center of the root CurvedLayout to the outer curve
 * of the segment
 * @param thickness The distance between inner and outer radius of the segment.
 * @param centerOffset The center of the circle this segment is part of.
 * @param measureRadius The radius to be used if there is a need to transform between angles and
 * curved widths.
 * @param startAngleRadians The angle at which the segment starts. In radians.
 */
@Immutable
internal class CurvedLayoutInfo internal constructor(
    val sweepRadians: Float,
    val outerRadius: Float,
    val thickness: Float,
    val centerOffset: Offset,
    val measureRadius: Float, // TODO: remove this from here or generalize
    val startAngleRadians: Float
) {
    val innerRadius = outerRadius - thickness
}

// Partially computed CurvedLayoutInfo
@Immutable
internal class PartialLayoutInfo(
    val sweepRadians: Float,
    val outerRadius: Float,
    val thickness: Float,
    val measureRadius: Float, // TODO: remove this from here or generalize
)

/**
 * Base class for children of a [CurvedLayout].
 *
 * It has similarities with a LayoutNode in the compose-ui world, but needs several changes to work
 * on curved elements:
 * Still uses the basic compose phases: composition, measurement,  placement & draw, but
 * measurament is split into sub-phases:
 * <pre>
 * 1. During composition [CurvedChild#ComposeIfNeeded] is called.
 * 2. During measurement [CurvedChild#initializeMeasure], [CurvedChild#estimateThickness],
 * [CurvedChild#radialPosition] & [CurvedChild#angularPosition] will be called, in order.
 * 3. During placement [CurvedChild#placeIfNeeded] is called.
 * 4. During drawing [CurvedChild#draw] is called.
 * </pre>
 * See those functions for specifics.
 */
internal abstract class CurvedChild() {
    // This value temporarily holds the result of the call to radialPosition, until we can call
    // angularPosition and construct the full CurvedLayoutInfo
    private lateinit var partialLayoutInfo: PartialLayoutInfo

    // All information needed for layout, set during angularPosition and read on draw/placement
    internal var layoutInfo by mutableStateOf<CurvedLayoutInfo?>(null)
        private set

    // Estimation of our thickness, this SHOULD be an upper bound.
    internal var estimatedThickness: Float = 0f
        private set

    // We only need to expose this so containers can angular position its children.
    internal val sweepRadians: Float
        get() = partialLayoutInfo.sweepRadians

    /**
     * Compose the content. This may generate some compose-ui nodes, but has to match
     * initializeMeasure's matching behavior (initializeMeasure should return the index parameter +
     * the number of nodes generated, and ideally check that they are the right measurable(s))
     */
    @Composable
    open fun SubComposition() {}

    /**
     * Initialize the Child to do a measure pass.
     *
     * @param measurables: The measurables on the CurvedLayout, used to map to our nodes as we walk
     * the tree.
     * @param index: The current index in the measurables array
     * @return The new index in the measurables array, taking into account how many items we
     * mapped.
     */
    open fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int = index

    /**
     * Estimate the thickness of this component given the maximus radius it can take.
     */
    fun estimateThickness(maxRadius: Float): Float = doEstimateThickness(maxRadius)
        .also { estimatedThickness = it }

    abstract fun doEstimateThickness(maxRadius: Float): Float

    /**
     * Compute our radial positioning relative to the parent.
     *
     * Note that parentInnerRadius & parentOuterRadius are similar to min & max Constraints in
     * compose, but for curved components is important to know absolute values of the possible,
     * radius. Curved things draw very differently on radius 50 to 100 than on radius 300 to 350.
     *
     *
     * @param parentOuterRadius The outer radius of the space we have in the parent container
     * @param parentThickness The thickness of the space we have in the parent container
     * Return A [PartialLayoutInfo] representing most of the information needed to layout this
     * component (all except it's angular position)
     */
    abstract fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo

    fun radialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo = doRadialPosition(parentOuterRadius, parentThickness)
        .also { partialLayoutInfo = it }

    /**
     * Called by the parent during angular layout to compute our starting angle (relative to the
     * space we have in the parent).
     *
     * @param parentStartAngleRadians The start angle we have available in our parent.
     * @param parentSweepRadians The sweep we have available in our parent.
     * @param centerOffset The center of the circle this curved component is a part of.
     * return This [CurvedChild] absolute angular position, in radians.
     */
    fun angularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        val angularPosition = doAngularPosition(
            parentStartAngleRadians,
            parentSweepRadians,
            centerOffset
        )
        layoutInfo = CurvedLayoutInfo(
            sweepRadians,
            partialLayoutInfo.outerRadius,
            partialLayoutInfo.thickness,
            centerOffset,
            partialLayoutInfo.measureRadius,
            angularPosition
        )
        return angularPosition
    }

    open fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float = parentStartAngleRadians

    /**
     * If this component generated a child composable, this is the opportunity to place it.
     */
    open fun (Placeable.PlacementScope).placeIfNeeded() {}

    /**
     * A chance for this component to draw itself.
     */
    open fun DrawScope.draw() {}
}

internal fun Float.toRadians() = this * PI.toFloat() / 180f
internal fun Float.toDegrees() = this * 180f / PI.toFloat()
internal fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float = map(selector).sum()
internal fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}
