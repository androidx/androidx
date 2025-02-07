/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ContentlessEntity

/**
 * A layout composable that arranges its children in a horizontal sequence. For arranging children
 * vertically, see [SpatialColumn].
 *
 * @param modifier Appearance modifiers to apply to this Composable.
 * @param alignment The default alignment for child elements within the row.
 * @param curveRadius The radial distance (in Dp) of the polar coordinate system of this row. It is
 *   a positive value. Setting this value to Dp.Infinity or a non-positive value will flatten the
 *   row. When a row is curved, its elements will be oriented so that they lie tangent to the curved
 *   row. A typical curved row has a curve radius of 825.dp.
 * @param name A string name to associated with the SpatialRow. This can be useful identifying the
 *   SpatialRow when debugging spatial applications.
 * @param content The composable content to be laid out horizontally in the row.
 */
@Composable
@SubspaceComposable
public fun SpatialRow(
    modifier: SubspaceModifier = SubspaceModifier,
    alignment: SpatialAlignment = SpatialAlignment.Center,
    curveRadius: Dp = Dp.Infinity,
    name: String = defaultSpatialRowName(),
    content: @Composable @SubspaceComposable SpatialRowScope.() -> Unit,
) {
    SubspaceLayout(
        modifier = modifier,
        content = { SpatialRowScopeInstance.content() },
        coreEntity =
            rememberCoreContentlessEntity {
                ContentlessEntity.create(this, name = name, pose = Pose.Identity)
            },
        name = name,
        measurePolicy =
            RowColumnMeasurePolicy(
                orientation = LayoutOrientation.Horizontal,
                alignment = alignment,
                curveRadius = if (curveRadius > 0.dp) curveRadius else Dp.Infinity,
            ),
    )
}

/** Scope for customizing the layout of children within a [SpatialRow]. */
@LayoutScopeMarker
public interface SpatialRowScope {
    /**
     * Sizes the element's width proportionally to its [weight] relative to other weighted sibling
     * elements in the [SpatialRow].
     *
     * The parent divides the remaining horizontal space after measuring unweighted children and
     * distributes it according to the weights.
     *
     * If [fill] is true, the element will occupy its entire allocated width. Otherwise, it can be
     * smaller, potentially making the [SpatialRow] smaller as unused space isn't redistributed.
     *
     * @param weight The proportional width for this element relative to other weighted siblings.
     *   Must be positive.
     * @param fill Whether the element should fill its entire allocated width.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): SubspaceModifier

    /**
     * Aligns the element vertically within the [SpatialRow], overriding the row's default vertical
     * alignment.
     *
     * @param alignment The vertical alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Vertical): SubspaceModifier

    /**
     * Aligns the element depthwise within the [SpatialRow], overriding the row's default depth
     * alignment.
     *
     * @param alignment The depth alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier
}

internal object SpatialRowScopeInstance : SpatialRowScope {
    override fun SubspaceModifier.weight(weight: Float, fill: Boolean): SubspaceModifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this then
            LayoutWeightElement(
                // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                weight = weight.coerceAtMost(Float.MAX_VALUE),
                fill = fill,
            )
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Vertical): SubspaceModifier {
        return this then RowColumnAlignElement(verticalSpatialAlignment = alignment)
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier {
        return this then RowColumnAlignElement(depthSpatialAlignment = alignment)
    }
}

private var spatialRowNamePart: Int = 0

private fun defaultSpatialRowName(): String {
    return "SpatialRow-${spatialRowNamePart++}"
}
