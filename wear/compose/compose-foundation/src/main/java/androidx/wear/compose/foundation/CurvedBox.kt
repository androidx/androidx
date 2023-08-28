/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull

/**
 * A layout composable that places its children on top of each other and on an arc. This is
 * similar to a [Box] layout, but curved into a segment of an annulus.
 *
 * The thickness of the layout (the difference between the outer and inner radius) will be the
 * same as the thickest child, and the angle taken will be the biggest angle of the
 * children.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedBoxSample
 *
 * @param modifier The [CurvedModifier] to apply to this curved row.
 * @param radialAlignment Radial alignment specifies where to lay down children that are thinner
 * than the CurvedBox, either closer to the center [CurvedAlignment.Radial.Inner], apart from
 * the center [CurvedAlignment.Radial.Outer] or in the
 * middle point [CurvedAlignment.Radial.Center]. If unspecified, they can choose for themselves.
 * @param angularAlignment Angular alignment specifies where to lay down children that are thinner
 * than the CurvedBox, either at the [CurvedAlignment.Angular.Start] of the layout,
 * at the [CurvedAlignment.Angular.End], or [CurvedAlignment.Angular.Center].
 * If unspecified or null, they can choose for themselves.
 * @param contentBuilder Specifies the content of this layout, currently there are 5 available
 * elements defined in foundation for this DSL: the sub-layouts [curvedBox], [curvedRow]
 * and [curvedColumn], [basicCurvedText] and [curvedComposable]
 * (used to add normal composables to curved layouts)
 */
public fun CurvedScope.curvedBox(
    modifier: CurvedModifier = CurvedModifier,
    radialAlignment: CurvedAlignment.Radial? = null,
    angularAlignment: CurvedAlignment.Angular? = null,
    contentBuilder: CurvedScope.() -> Unit
) = add(
    CurvedBoxChild(
        curvedLayoutDirection,
        radialAlignment,
        angularAlignment,
        contentBuilder
    ),
    modifier
)

internal class CurvedBoxChild(
    curvedLayoutDirection: CurvedLayoutDirection,
    private val radialAlignment: CurvedAlignment.Radial? = null,
    private val angularAlignment: CurvedAlignment.Angular? = null,
    contentBuilder: CurvedScope.() -> Unit
) : ContainerChild(curvedLayoutDirection, reverseLayout = false, contentBuilder) {

    override fun doEstimateThickness(maxRadius: Float) =
        children.fastMaxOfOrNull { it.estimateThickness(maxRadius) } ?: 0f

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        // position children, take max sweep.
        val maxSweep = children.fastMaxOfOrNull { child ->
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
        } ?: 0f
        return PartialLayoutInfo(
            maxSweep,
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
        children.fastForEach { child ->
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
