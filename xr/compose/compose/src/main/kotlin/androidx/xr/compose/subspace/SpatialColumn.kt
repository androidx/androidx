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
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ContentlessEntity

/**
 * A layout composable that arranges its children in a vertical sequence.
 *
 * For arranging children horizontally, see [SpatialRow].
 *
 * @param modifier Modifiers to apply to the layout.
 * @param alignment The default alignment for child elements within the column.
 * @param name The name of the layout.
 * @param content The composable content to be laid out vertically.
 */
@Composable
@SubspaceComposable
public fun SpatialColumn(
    modifier: SubspaceModifier = SubspaceModifier,
    alignment: SpatialAlignment = SpatialAlignment.Center,
    name: String = defaultSpatialColumnName(),
    content: @Composable @SubspaceComposable SpatialColumnScope.() -> Unit,
) {
    SubspaceLayout(
        modifier = modifier,
        content = { SpatialColumnScopeInstance.content() },
        coreEntity =
            rememberCoreContentlessEntity {
                ContentlessEntity.create(this, name = name, pose = Pose.Identity)
            },
        name = name,
        measurePolicy =
            RowColumnMeasurePolicy(
                orientation = LayoutOrientation.Vertical,
                alignment = alignment,
                curveRadius = Dp.Infinity,
            ),
    )
}

/** Scope for customizing the layout of children within a [SpatialColumn]. */
@LayoutScopeMarker
public interface SpatialColumnScope {
    /**
     * Sizes the element's height proportionally to its [weight] relative to other weighted sibling
     * elements in the [SpatialColumn].
     *
     * The parent divides the remaining vertical space after measuring unweighted children and
     * distributes it according to the weights.
     *
     * If [fill] is true, the element will occupy its entire allocated height. Otherwise, it can be
     * smaller, potentially making the [SpatialColumn] smaller as unused space isn't redistributed.
     *
     * @param weight The proportional height for this element relative to other weighted siblings.
     *   Must be positive.
     * @param fill Whether the element should fill its entire allocated height.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): SubspaceModifier

    /**
     * Aligns the element within the [SpatialColumn] horizontally. This will override the horizontal
     * alignment value passed to the [SpatialColumn].
     *
     * @param alignment The horizontal alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Horizontal): SubspaceModifier

    /**
     * Aligns the element within the [SpatialColumn] depthwise. This will override the depth
     * alignment value passed to the [SpatialColumn].
     *
     * @param alignment The depth alignment to use for the element.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier
}

internal object SpatialColumnScopeInstance : SpatialColumnScope {
    override fun SubspaceModifier.weight(weight: Float, fill: Boolean): SubspaceModifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this then
            LayoutWeightElement(
                // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                weight = weight.coerceAtMost(Float.MAX_VALUE),
                fill = fill,
            )
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Horizontal): SubspaceModifier {
        return this then RowColumnAlignElement(horizontalSpatialAlignment = alignment)
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier {
        return this then RowColumnAlignElement(depthSpatialAlignment = alignment)
    }
}

private var spatialColumnNamePart: Int = 0

private fun defaultSpatialColumnName(): String {
    return "SpatialColumn-${spatialColumnNamePart++}"
}
