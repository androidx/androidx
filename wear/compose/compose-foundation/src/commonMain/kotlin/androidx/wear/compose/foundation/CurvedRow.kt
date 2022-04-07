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

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.geometry.Offset

/**
 * A layout composable that places its children in an arc, rotating them as needed. This is
 * similar to a [Row] layout, that it's curved into a segment of an annulus.
 *
 * The thickness of the layout (the difference between the outer and inner radius) will be the
 * same as the thickest child, and the total angle taken is the sum of the children's angles.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedRowAndColumn
 *
 * @param modifier The [CurvedModifier] to apply to this curved row.
 * @param radialAlignment Radial alignment specifies where to lay down children that are thinner
 * than the CurvedRow, either closer to the center (INNER), apart from the center (OUTER) or in the
 * middle point (CENTER). If unspecified, they can choose for themselves.
 * @param clockwise Specify if the children are laid out clockwise (the default) or
 * counter-clockwise
 */
public fun CurvedScope.curvedRow(
    modifier: CurvedModifier = CurvedModifier,
    radialAlignment: CurvedAlignment.Radial? = null,
    clockwise: Boolean = true,
    contentBuilder: CurvedScope.() -> Unit
) = add(CurvedRowChild(clockwise, radialAlignment, contentBuilder), modifier)

internal class CurvedRowChild(
    clockwise: Boolean,
    val radialAlignment: CurvedAlignment.Radial? = null,
    contentBuilder: CurvedScope.() -> Unit
) : ContainerChild(!clockwise, contentBuilder) {

    override fun doEstimateThickness(maxRadius: Float) =
        children.maxOfOrNull { it.estimateThickness(maxRadius) } ?: 0f

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        // position children, sum angles.
        var totalSweep = children.sumOf { child ->
            var childRadialPosition = parentOuterRadius
            var childThickness = parentThickness
            if (radialAlignment != null) {
                childRadialPosition = parentOuterRadius - radialAlignment.ratio *
                    (parentThickness - child.estimatedThickness)
                childThickness = child.estimatedThickness
            }

            child.radialPosition(
                childRadialPosition,
                childThickness
            )
            child.sweepRadians
        }

        return PartialLayoutInfo(
            totalSweep,
            parentOuterRadius,
            parentThickness,
            parentOuterRadius - parentThickness / 2
        )
    }

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        val weights = childrenInLayoutOrder.map { node ->
            (node.computeParentData() as? CurvedScopeParentData)?.weight ?: 0f
        }
        val sumWeights = weights.sum()
        val extraSpace = parentSweepRadians - childrenInLayoutOrder.mapIndexed { ix, node ->
            if (weights[ix] == 0f) {
                node.sweepRadians
            } else {
                0f
            }
        }.sum()

        var currentStartAngle = parentStartAngleRadians
        childrenInLayoutOrder.forEachIndexed { ix, node ->
            val actualSweep = if (weights[ix] > 0f) {
                    extraSpace * weights[ix] / sumWeights
                } else {
                    node.sweepRadians
                }

            node.angularPosition(
                currentStartAngle,
                actualSweep,
                centerOffset
            )
            currentStartAngle += actualSweep
        }
        return parentStartAngleRadians
    }
}
