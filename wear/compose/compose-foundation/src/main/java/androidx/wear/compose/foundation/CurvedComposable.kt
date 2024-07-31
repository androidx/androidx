/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Component that allows normal composables to be part of a [CurvedLayout].
 *
 * @param modifier The [CurvedModifier] to apply to this curved composable.
 * @param radialAlignment How to align this component if it's thinner than the container.
 * @param rotationLocked by default (when this is false), the component will be rotated as it moves
 *   around the circle, so its base always faces the center. If set to true, it won't be rotated and
 *   only moved into position, for example, an upwards pointing arrow will remain pointing upwards
 *   wherever it appears on the circle. Note that this is not taken into account when computing the
 *   size this will take in the layout, so it's best suited for square/circular things and may
 *   require manual sizing when used in other contexts.
 * @param content The composable(s) that will be wrapped and laid out as part of the parent
 *   container. This has a [BoxScope], since it's wrapped inside a Box.
 */
public fun CurvedScope.curvedComposable(
    modifier: CurvedModifier = CurvedModifier,
    radialAlignment: CurvedAlignment.Radial = CurvedAlignment.Radial.Center,
    rotationLocked: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) =
    add(
        CurvedComposableChild(
            curvedLayoutDirection.absoluteClockwise(),
            radialAlignment,
            rotationLocked,
            content
        ),
        modifier
    )

/**
 * Component that allows normal composables to be part of a [CurvedLayout].
 *
 * @param modifier The [CurvedModifier] to apply to this curved composable.
 * @param radialAlignment How to align this component if it's thinner than the container.
 * @param content The composable(s) that will be wrapped and laid out as part of the parent
 *   container. This has a [BoxScope], since it's wrapped inside a Box.
 */
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for " +
        "Wear OS 1.4. A newer overload is available with additional parameter to control rotation.",
    level = DeprecationLevel.HIDDEN
)
public fun CurvedScope.curvedComposable(
    modifier: CurvedModifier = CurvedModifier,
    radialAlignment: CurvedAlignment.Radial = CurvedAlignment.Radial.Center,
    content: @Composable BoxScope.() -> Unit
) =
    add(
        CurvedComposableChild(
            curvedLayoutDirection.absoluteClockwise(),
            radialAlignment,
            false,
            content
        ),
        modifier
    )

internal class CurvedComposableChild(
    val clockwise: Boolean,
    val radialAlignment: CurvedAlignment.Radial,
    val rotationLocked: Boolean,
    val content: @Composable BoxScope.() -> Unit
) : CurvedChild() {
    lateinit var placeable: Placeable

    @Composable
    override fun SubComposition() {
        // Ensure we have a 1-1 match between CurvedComposable and composable child
        Box(content = content)
    }

    override fun CurvedMeasureScope.initializeMeasure(measurables: Iterator<Measurable>) {
        // TODO: check that we actually match adding a parent data modifier to the Box in
        // composeIfNeeded and verifying this measurable has it?
        placeable = measurables.next().measure(Constraints())
    }

    override fun doEstimateThickness(maxRadius: Float): Float {
        // Compute the annulus we need as if the child was top aligned, this gives as an upper
        // bound on the thickness, but we need to recompute later, when we know the actual position.
        val (innerRadius, outerRadius) = computeAnnulusRadii(maxRadius, 0f)
        return outerRadius - innerRadius
    }

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo {
        val parentInnerRadius = parentOuterRadius - parentThickness

        // We know where we want it and the radial alignment, so we can compute it's positioning now
        val (myInnerRadius, myOuterRadius) =
            computeAnnulusRadii(
                lerp(parentOuterRadius, parentInnerRadius, radialAlignment.ratio),
                radialAlignment.ratio
            )

        val sweepRadians = 2f * asin(placeable.width / 2f / myInnerRadius)
        return PartialLayoutInfo(
            sweepRadians,
            myOuterRadius,
            thickness = myOuterRadius - myInnerRadius,
            measureRadius = (myInnerRadius + myOuterRadius) / 2 // !?
        )
    }

    private var parentSweepRadians: Float = 0f

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float = parentStartAngleRadians.also { this.parentSweepRadians = parentSweepRadians }

    override fun (Placeable.PlacementScope).placeIfNeeded() =
        place(placeable, layoutInfo!!, parentSweepRadians, clockwise, rotationLocked)

    /**
     * Compute the inner and outer radii of the annulus sector required to fit the given box.
     *
     * @param targetRadius The distance we want, from the center of the circle the annulus is part
     *   of, to a point on the side of the box (which point is determined with the radiusAlpha
     *   parameter.)
     * @param radiusAlpha Which point on the side of the box we are measuring the radius to. 0 means
     *   radius is to the outer point in the box, 1 means that it's to the inner point. (And
     *   interpolation in-between)
     */
    private fun computeAnnulusRadii(targetRadius: Float, radiusAlpha: Float): Pair<Float, Float> {
        // The top side of the triangles we use, squared.
        val topSquared = pow2(placeable.width / 2f)

        // Project the radius we know to the line going from the center to the circle to the center
        // of the box
        val radiusInBox = sqrt(pow2(targetRadius) - topSquared)

        // Move to the top/bottom of the child box, then project back
        val outerRadius = sqrt(topSquared + pow2(radiusInBox + radiusAlpha * placeable.height))
        val innerRadius =
            sqrt(topSquared + pow2(radiusInBox - (1 - radiusAlpha) * placeable.height))

        return innerRadius to outerRadius
    }
}

internal fun (Placeable.PlacementScope).place(
    placeable: Placeable,
    layoutInfo: CurvedLayoutInfo,
    parentSweepRadians: Float,
    clockwise: Boolean,
    rotationLocked: Boolean = false
) {
    with(layoutInfo) {
        // Distance from the center of the CurvedRow to the top left of the component.
        val radiusToTopLeft = outerRadius

        // Distance from the center of the CurvedRow to the top center of the component.
        val radiusToTopCenter =
            sqrt((pow2(radiusToTopLeft) - pow2(placeable.width / 2f)).coerceAtLeast(0f))

        // To position this child, we move its center rotating it around the CurvedRow's center.
        val radiusToCenter = radiusToTopCenter - placeable.height / 2f
        val centerAngle = startAngleRadians + parentSweepRadians / 2f
        val childCenterX = centerOffset.x + radiusToCenter * cos(centerAngle)
        val childCenterY = centerOffset.y + radiusToCenter * sin(centerAngle)

        // Then compute the position of the top left corner given that center.
        val positionX = (childCenterX - placeable.width / 2f).roundToInt()
        val positionY = (childCenterY - placeable.height / 2f).roundToInt()

        val rotationAngle = centerAngle + if (clockwise) 0f else PI.toFloat()

        placeable.placeWithLayer(
            x = positionX,
            y = positionY,
            layerBlock =
                if (rotationLocked) DefaultLayerBlock
                else {
                    {
                        rotationZ = rotationAngle.toDegrees() - 270f
                        // Rotate around the center of the placeable.
                        transformOrigin = TransformOrigin.Center
                    }
                }
        )
    }
}

private val DefaultLayerBlock: GraphicsLayerScope.() -> Unit = {}

private fun pow2(x: Float): Float = x * x
