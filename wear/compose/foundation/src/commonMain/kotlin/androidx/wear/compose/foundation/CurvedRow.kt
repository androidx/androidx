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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Specifies how components will be laid down with respect to the anchor.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
inline class AnchorType internal constructor(internal val ratio: Float) {
    companion object {
        /**
         * Start the content of the [CurvedRow] on the anchor
         */
        val Start = AnchorType(0f)

        /**
         * Center the content of the [CurvedRow] around the anchor
         */
        val Center = AnchorType(0.5f)

        /**
         * End the content of the [CurvedRow] on the anchor
         */
        val End = AnchorType(1f)
    }

    override fun toString(): String {
        return when (this) {
            Center -> "AnchorType.Center"
            Start -> "AnchorType.Start"
            else -> "AnchorType.End"
        }
    }
}

/**
 * How to lay down components when they are thinner than the [CurvedRow]. Similar to vertical
 * alignment in a [Row].
 */
@Suppress("INLINE_CLASS_DEPRECATED")
inline class RadialAlignment internal constructor(internal val ratio: Float) {
    companion object {
        /**
         * Put the child closest to the center of the [CurvedRow], within the available space
         */
        val Inner = RadialAlignment(1f)

        /**
         * Put the child in the middle point of the available space.
         */
        val Center = RadialAlignment(0.5f)

        /**
         * Put the child farthest from the center of the [CurvedRow], within the available space
         */
        val Outer = RadialAlignment(0f)

        /**
         * Align the child in a custom position, 0 means Outer, 1 means Inner
         */
        fun Custom(ratio: Float): RadialAlignment {
            return RadialAlignment(ratio)
        }
    }
}

/**
 * A layout composable that places its children in an arc, rotating them as needed. This is
 * similar to a [Row] layout, that it's curved into a segment of an annulus.
 *
 * The thickness of the layout (the difference between the outer and inner radius) will be the
 * same as the thickest child, and the total angle taken is the sum of the children's angles.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.SimpleCurvedRow
 *
 * @param modifier The modifier to be applied to the CurvedRow.
 * @param anchor The angle at which children are laid out relative to, in degrees. An angle of 0
 * corresponds to the right (3 o'clock on a watch), 90 degrees is bottom (6 o'clock), and so on.
 * Default is 270 degrees (top of the screen)
 * @param anchorType Specify how the content is drawn with respect to the anchor. Default is to
 * center the content on the anchor.
 * @param radialAlignment Specifies the default radial alignment for children that don't specify
 * one. Radial alignment specifies where to lay down children that are thiner than the
 * CurvedRow, either closer to the center (INNER), apart from the center (OUTER) or in the middle
 * point (CENTER).
 * @param clockwise Specify if the children are laid out clockwise (the default) or
 * counter-clockwise
 */
@Composable
fun CurvedRow(
    modifier: Modifier = Modifier,
    anchor: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
    radialAlignment: RadialAlignment = RadialAlignment.Center,
    clockwise: Boolean = true,
    content: @Composable CurvedRowScope.() -> Unit
) {
    // Note that all angles in the function are in radians, and the anchor parameter is in degrees
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Layout(
            content = { CurvedRowScopeInstance.content() }
        ) { measurables, constraints ->
            require(constraints.hasBoundedHeight || constraints.hasBoundedWidth)
            // We take as much room as possible, the same in both dimensions, within the constraints
            val diameter = min(
                if (constraints.hasBoundedWidth) constraints.maxWidth else 0,
                if (constraints.hasBoundedHeight) constraints.maxHeight else 0,
            )
            val radius = diameter / 2f

            val measuredChildren = measurables.map { m ->
                NormalMeasuredChild(m)
            }

            // Measure the children, we only need an upper bound for the thickness of each element.
            measuredChildren.forEach {
                it.initialMeasurePass(radius)
            }
            val curvedRowThickness = measuredChildren.maxOfOrNull {
                it.estimateThickness(radius)
            } ?: 0f

            // Now we can radially position the children
            measuredChildren.forEach {
                it.calculateRadialPosition(radius, curvedRowThickness, radialAlignment)
            }

            // Compute to total angle all children take and where we need to start laying them out.
            val totalSweep = measuredChildren.map { it.sweep }.sum()
            var childAngleStart = -anchorType.ratio * totalSweep

            val clockwiseFactor = if (clockwise) 1 else -1

            layout(diameter, diameter) {
                measuredChildren.forEach { child ->
                    // Angle of the vector from the centre of the CurvedRow to the center of the child.
                    val centerAngle = anchor.toRadians() + clockwiseFactor *
                        (childAngleStart + child.sweep / 2)

                    child.place(radius, scope = this, centerAngle, clockwise)

                    childAngleStart += child.sweep
                }
            }
        }
    }
}

/**
 * Layout scope used for modifiers (and children in the future) that only make sense in an CurvedRow
 */
@LayoutScopeMarker
@Immutable
interface CurvedRowScope {
    /**
     * Specify the radial positioning of this element inside the [CurvedRow]. Similar to vertical
     * alignment in a [Row]
     */
    fun Modifier.radialAlignment(alignment: RadialAlignment): Modifier
}

internal object CurvedRowScopeInstance : CurvedRowScope {
    override fun Modifier.radialAlignment(alignment: RadialAlignment): Modifier =
        this.then(
            RadialAlignmentImpl(
                alignment,
                inspectorInfo = debugInspectorInfo {
                    name = "radialAlignment"
                    properties["alignment"] = alignment
                }
            )
        )
}
private abstract class MeasuredChild(
    val measurable: Measurable
) {
    lateinit var placeable: Placeable
    var width: Int = 0
    var height: Int = 0
    var sweep: Float = 0f
    var componentRadialPosition: Float = 0f

    abstract fun initialMeasurePass(radius: Float)
    abstract fun estimateThickness(radius: Float): Float
    abstract fun calculateRadialPosition(
        radius: Float,
        curvedRowThickness: Float,
        curvedRowRadialAlignment: RadialAlignment
    )

    abstract fun place(
        radius: Float,
        scope: Placeable.PlacementScope,
        centerAngle: Float,
        clockwise: Boolean
    )

    internal fun place(
        scope: Placeable.PlacementScope,
        positionX: Float,
        positionY: Float,
        rotation: Float
    ) {
        with(scope) {
            placeable.placeRelativeWithLayer(
                x = positionX.toInt(),
                y = positionY.toInt(),
                layerBlock = {
                    rotationZ = rotation.toDegrees() - 270f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
            )
        }
    }
}

private class NormalMeasuredChild(measurable: Measurable) : MeasuredChild(measurable) {

    override fun initialMeasurePass(radius: Float) {
        // This is the size biggest square box that fits in half a circle
        val biggestSize = (radius * 2 / sqrt(5f)).toInt()
        val actualConstraint = Constraints(maxWidth = biggestSize, maxHeight = biggestSize)
        placeable = measurable.measure(actualConstraint)
        width = placeable.width
        height = placeable.height
    }

    override fun estimateThickness(radius: Float): Float {
        // Compute the annulus we need as if the child was top aligned, this gives as an upper
        // bound on the thickness, but we need to recompute later, when we know the actual position.
        val (innerRadius, outerRadius) = computeAnnulusRadii(radius, 0f)
        return outerRadius - innerRadius
    }

    override fun calculateRadialPosition(
        radius: Float,
        curvedRowThickness: Float,
        curvedRowRadialAlignment: RadialAlignment
    ) {
        val radialAlignment = measurable.radialAlignment ?: curvedRowRadialAlignment

        // We know where we want it and the radial alignment, so we can compute it's positioning now
        val (innerRadius, outerRadius) = computeAnnulusRadii(
            radius - curvedRowThickness * radialAlignment.ratio,
            radialAlignment.ratio
        )
        componentRadialPosition = radius - outerRadius

        sweep = 2 * asin(width / 2 / innerRadius)
    }

    override fun place(
        radius: Float,
        scope: Placeable.PlacementScope,
        centerAngle: Float,
        clockwise: Boolean
    ) {
        // Distance from the center of the CurvedRow to the top left of the component.
        val radiusToTopLeft = radius - componentRadialPosition

        // Distance from the center of the CurvedRow to the top center of the component.
        val radiusToTopCenter = sqrt(sqr(radiusToTopLeft) - sqr(width / 2f))

        // To position this child, we move its center rotating it around the CurvedRow's center.
        val radiusToCenter = radiusToTopCenter - height / 2f
        val childCenterX = radius + radiusToCenter * cos(centerAngle)
        val childCenterY = radius / 2f + radiusToCenter * sin(centerAngle)

        // Then compute the position of the top left corner given that center.
        val positionX = childCenterX - width / 2f
        val positionY = childCenterY - height / 2f

        val rotationAngle = if (clockwise) centerAngle else centerAngle + PI.toFloat()

        place(scope, positionX, positionY, rotationAngle)
    }

    /**
     * Compute the inner and outer radii of the annulus sector required to fit the given box.
     *
     * @param targetRadius The distance we want, from the center of the circle the annulus is part
     * of, to a point on the side of the box (which point is determined with the radiusAlpha
     * parameter.)
     * @param radiusAlpha Which point on the side of the box we are measuring the radius to. 0 means
     * radius is to the outer point in the box, 1 means that it's to the inner point.
     * (And interpolation in-between)
     *
     */
    fun computeAnnulusRadii(targetRadius: Float, radiusAlpha: Float): Pair<Float, Float> {
        // The top side of the triangles we use, squared.
        val topSquared = sqr(width / 2f)

        // Project the radius we know to the line going from the center to the circle to the center
        // of the box
        val radiusInBox = sqrt(sqr(targetRadius) - topSquared)

        // Move to the top/bottom of the child box, then project back
        val outerRadius = sqrt(topSquared + sqr(radiusInBox + radiusAlpha * height))
        val innerRadius = sqrt(topSquared + sqr(radiusInBox - (1 - radiusAlpha) * height))

        return innerRadius to outerRadius
    }

    fun sqr(x: Float): Float = x * x
}

private fun Float.toRadians() = this * PI.toFloat() / 180f
private fun Float.toDegrees() = this * 180f / PI.toFloat()

internal class RadialAlignmentImpl(
    private val radialAlignment: RadialAlignment,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? CurvedRowParentData) ?: CurvedRowParentData(radialAlignment))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RadialAlignmentImpl && radialAlignment == other.radialAlignment
    }

    override fun hashCode(): Int = radialAlignment.hashCode()

    override fun toString(): String =
        "RadialAlignmentImpl($radialAlignment)"
}

/**
 * Parent Data associated with children of a CurvedRow
 */
internal data class CurvedRowParentData(
    var radialAlignment: RadialAlignment
)

internal val Measurable.radialAlignment: RadialAlignment?
    get() = (parentData as? CurvedRowParentData)?.radialAlignment
