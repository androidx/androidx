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
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
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
        // Start the content of the CurvedRow on the anchor
        val Start = AnchorType(0f)
        // Center the content of the CurvedRow around the anchor
        val Center = AnchorType(0.5f)
        // End the content of the CurvedRow on the anchor
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
 * @param clockwise Specify if the children are laid out clockwise (the default) or
 * counter-clockwise
 */
@Composable
fun CurvedRow(
    modifier: Modifier = Modifier,
    anchor: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
    clockwise: Boolean = true,
    content: @Composable () -> Unit
) {
    // Note that all angles in the function are in radians, and the anchor parameter is in degrees
    Box(modifier = modifier) {
        Layout(
            content = content
        ) { measurables, constraints ->
            require(constraints.hasBoundedHeight || constraints.hasBoundedWidth)
            // We take as much room as possible, the same in both dimensions, within the constraints
            val diameter = min(
                if (constraints.hasBoundedWidth) constraints.maxWidth else 0,
                if (constraints.hasBoundedHeight) constraints.maxHeight else 0,
            )

            val measuredChildren = measurables.map { m ->
                NormalMeasuredChild(m)
            }

            // Measure the children, we only need an upper bound for the thickness of each element.
            measuredChildren.forEach {
                it.initialMeasurePass(diameter / 2f)
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

                    child.place(diameter / 2f, scope = this, centerAngle, clockwise)

                    childAngleStart += child.sweep
                }
            }
        }
    }
}

private abstract class MeasuredChild(
    val measurable: Measurable
) {
    lateinit var placeable: Placeable
    var width: Int = 0
    var height: Int = 0
    var thickness: Float = 0f
    var sweep: Float = 0f

    abstract fun initialMeasurePass(radius: Float)
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
        val biggestSize = (radius * sqrt(2f)).toInt()
        val actualConstraint = Constraints(maxWidth = biggestSize, maxHeight = biggestSize)
        placeable = measurable.measure(actualConstraint)
        width = placeable.width
        height = placeable.height

        // Distance we want from the center of the CurvedRow to the Top Center of the child's
        // containing box.
        val radiusInBox = sqrt(sqr(radius) - sqr(width / 2f))

        val innerRadius = sqrt(sqr(width / 2f) + sqr(radiusInBox - height))
        thickness = radius - innerRadius

        sweep = 2 * asin(width / 2 / innerRadius)
    }

    override fun place(
        radius: Float,
        scope: Placeable.PlacementScope,
        centerAngle: Float,
        clockwise: Boolean
    ) {
        // Distance from the center of the CurvedRow to the top left of the component.
        val radiusToTopLeft = radius

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

    fun sqr(x: Float): Float = x * x
}

private fun Float.toRadians() = this * PI.toFloat() / 180f
private fun Float.toDegrees() = this * 180f / PI.toFloat()
