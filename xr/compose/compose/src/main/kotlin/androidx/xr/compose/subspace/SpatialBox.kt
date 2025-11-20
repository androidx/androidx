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

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastForEachIndexed
import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.ParentLayoutParamsModifier
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import kotlin.math.max

/**
 * A layout composable that sizes itself to fit its content, subject to incoming constraints.
 *
 * A layout composable with [content]. The [SpatialBox] will size itself to fit the content, subject
 * to the incoming constraints. When children are smaller than the parent, by default they will be
 * positioned inside the [SpatialBox] according to the [alignment]. For individually specifying the
 * alignments of the children layouts, use the [SpatialBoxScope.align] modifier. By default, the
 * content will be measured without the [SpatialBox]'s incoming min constraints. If
 * [propagateMinConstraints] is set to `true`, the min size set on the [SpatialBox] will also be
 * applied to the content.
 *
 * Note: If the content has multiple children, they might overlap depending on their positioning.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param alignment The default alignment of children within the [SpatialBox].
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param content The content of the [SpatialBox].
 */
@Composable
@SubspaceComposable
public fun SpatialBox(
    modifier: SubspaceModifier = SubspaceModifier,
    alignment: SpatialAlignment = SpatialAlignment.Center,
    propagateMinConstraints: Boolean = false,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    SubspaceLayout(
        modifier = modifier,
        content = { SpatialBoxScopeInstance.content() },
        measurePolicy = SpatialBoxMeasurePolicy(alignment, propagateMinConstraints),
    )
}

/** [SubspaceMeasurePolicy] for [SpatialBox]. */
internal class SpatialBoxMeasurePolicy(
    private val alignment: SpatialAlignment,
    private val propagateMinConstraints: Boolean,
) : SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        if (measurables.isEmpty()) {
            return layout(constraints.minWidth, constraints.minHeight, constraints.minDepth) {}
        }

        val contentConstraints =
            if (propagateMinConstraints) {
                constraints
            } else {
                constraints.copy(minWidth = 0, minHeight = 0, minDepth = 0)
            }

        val placeables = arrayOfNulls<SubspacePlaceable>(measurables.size)
        var boxWidth = constraints.minWidth
        var boxHeight = constraints.minHeight
        var boxDepth = constraints.minDepth
        measurables.fastForEachIndexed { index, measurable ->
            val placeable = measurable.measure(contentConstraints)
            placeables[index] = placeable
            boxWidth = max(boxWidth, placeable.measuredWidth)
            boxHeight = max(boxHeight, placeable.measuredHeight)
            boxDepth = max(boxDepth, placeable.measuredDepth)
        }

        return layout(boxWidth, boxHeight, boxDepth) {
            val space = IntVolumeSize(boxWidth, boxHeight, boxDepth)
            placeables.forEachIndexed { index, placeable ->
                placeable as SubspacePlaceable
                val measurable = measurables[index]
                val childSpatialAlignment =
                    SpatialBoxParentData(alignment).also { measurable.adjustParams(it) }.alignment
                placeable.place(
                    Pose(childSpatialAlignment.position(placeable.size(), space, layoutDirection))
                )
            }
        }
    }

    private fun SubspacePlaceable.size() =
        IntVolumeSize(measuredWidth, measuredHeight, measuredDepth)
}

/** Scope for the children of [SpatialBox]. */
@LayoutScopeMarker
public interface SpatialBoxScope {
    /**
     * Positions the content element at a specific [SpatialAlignment] within the [SpatialBox]. This
     * alignment overrides the default [alignment] of the [SpatialBox].
     *
     * @param alignment The desired alignment for the content.
     * @return The modified SubspaceModifier.
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment): SubspaceModifier
}

internal object SpatialBoxScopeInstance : SpatialBoxScope {
    override fun SubspaceModifier.align(alignment: SpatialAlignment): SubspaceModifier {
        return this then LayoutAlignElement(alignment = alignment)
    }
}

private class LayoutAlignElement(val alignment: SpatialAlignment) :
    SubspaceModifierNodeElement<LayoutAlignNode>() {
    override fun create(): LayoutAlignNode = LayoutAlignNode(alignment)

    override fun update(node: LayoutAlignNode) {
        node.alignment = alignment
    }

    override fun hashCode(): Int {
        return alignment.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? LayoutAlignElement ?: return false
        return alignment == otherModifier.alignment
    }
}

private class LayoutAlignNode(var alignment: SpatialAlignment) :
    SubspaceModifier.Node(), ParentLayoutParamsModifier {
    override fun adjustParams(params: ParentLayoutParamsAdjustable) {
        if (params is SpatialBoxParentData) {
            params.alignment = alignment
        }
    }
}

private data class SpatialBoxParentData(var alignment: SpatialAlignment) :
    ParentLayoutParamsAdjustable
