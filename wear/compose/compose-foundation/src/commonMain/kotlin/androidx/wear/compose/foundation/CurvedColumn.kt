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

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset

/**
 * A curved layout composable that places its children stacked as part of an arc (the first child
 * will be the outermost). This is similar to a [Column] layout, that it's curved into a segment of
 * an annulus.
 *
 * The thickness of the layout (the difference between the outer and inner radius) will be the
 * sum of the thickness of its children, and the angle taken will be the biggest angle of the
 * children.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedRowAndColumn
 *
 * @param modifier The [CurvedModifier] to apply to this curved column.
 * @param radialDirection Order to lay out components, outside in or inside out. The default is to
 * inherit from the containing [curvedColumn] or [CurvedLayout]
 * @param angularAlignment Angular alignment specifies where to lay down children that are thinner
 * than the curved column, either at the (START) of the layout, at the (END), or (CENTER).
 * If unspecified or null, they can choose for themselves.
 */
public fun CurvedScope.curvedColumn(
    modifier: CurvedModifier = CurvedModifier,
    radialDirection: CurvedDirection.Radial? = null,
    angularAlignment: CurvedAlignment.Angular? = null,
    contentBuilder: CurvedScope.() -> Unit
) = add(
    CurvedColumnChild(
        curvedLayoutDirection.copy(overrideRadial = radialDirection),
        angularAlignment,
        contentBuilder
    ),
    modifier
)

internal class CurvedColumnChild(
    curvedLayoutDirection: CurvedLayoutDirection,
    private val angularAlignment: CurvedAlignment.Angular?,
    contentBuilder: CurvedScope.() -> Unit
) : ContainerChild(curvedLayoutDirection, !curvedLayoutDirection.outsideIn(), contentBuilder) {
    override fun doEstimateThickness(maxRadius: Float): Float =
        maxRadius - children.fold(maxRadius) { currentMaxRadius, node ->
            currentMaxRadius - node.estimateThickness(currentMaxRadius)
        }

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        // Compute space used by weighted children and space left
        val weights = childrenInLayoutOrder.map { node ->
            (node.computeParentData() as? CurvedScopeParentData)?.weight ?: 0f
        }
        val sumWeights = weights.sum()
        val extraSpace = parentThickness - childrenInLayoutOrder.mapIndexed { ix, node ->
            if (weights[ix] == 0f) {
                node.estimatedThickness
            } else {
                0f
            }
        }.sum()

        // position children
        var outerRadius = parentOuterRadius
        childrenInLayoutOrder.forEachIndexed { ix, node ->
            val actualThickness = if (weights[ix] > 0f) {
                    extraSpace * weights[ix] / sumWeights
                } else {
                    node.estimatedThickness
                }

            node.radialPosition(
                outerRadius,
                actualThickness
            )
            outerRadius -= actualThickness
        }
        var maxSweep = childrenInLayoutOrder.maxOfOrNull { it.sweepRadians } ?: 0f

        return PartialLayoutInfo(
            maxSweep,
            parentOuterRadius,
            parentOuterRadius - outerRadius,
            (parentOuterRadius + outerRadius) / 2 // ?
        )
    }

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        children.forEach { child ->
            var childAngularPosition = parentStartAngleRadians
            var childSweep = parentSweepRadians
            if (angularAlignment != null) {
                childAngularPosition = parentStartAngleRadians + angularAlignment.ratio *
                    (parentSweepRadians - child.sweepRadians)
                childSweep = child.sweepRadians
            }

            child.angularPosition(
                childAngularPosition,
                childSweep,
                centerOffset
            )
        }
        return parentStartAngleRadians
    }
}
