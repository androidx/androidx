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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.LayoutSubspaceMeasureScope
import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

/**
 * A [SubspaceMeasurable] and [SubspacePlaceable] object that is used to measure and lay out the
 * children of a [SubspaceLayoutModifierNode].
 *
 * See [androidx.compose.ui.node.NodeCoordinator]
 *
 * The [SubspaceLayoutModifierNodeCoordinator] is mapped 1:1 with a [SubspaceLayoutModifierNode] so
 * we don't need to maintain a separate node coordinator hierarchy. Instead, we can use the
 * hierarchy already present in [SubspaceModifier.Node] types.
 */
internal class SubspaceLayoutModifierNodeCoordinator(
    private val layoutModifierNode: SubspaceLayoutModifierNode
) : SubspaceLayoutCoordinates, SubspaceMeasurable, SubspacePlaceable() {

    private val baseNode: SubspaceModifier.Node
        get() = layoutModifierNode as SubspaceModifier.Node

    internal val layoutNode: SubspaceLayoutNode?
        get() = baseNode.layoutNode

    internal val parent: SubspaceLayoutModifierNodeCoordinator?
        get() =
            generateSequence(baseNode.parent) { it.parent }.firstNotNullOfOrNull { it.coordinator }

    internal val child: SubspaceLayoutModifierNodeCoordinator?
        get() =
            generateSequence(baseNode.child) { it.child }.firstNotNullOfOrNull { it.coordinator }

    /** The pose of this layout in the local coordinates space. */
    override val pose: Pose
        get() = layoutPose ?: Pose.Identity

    /**
     * The pose of this layout modifier node relative to the root entity of the Compose hierarchy.
     */
    override val poseInRoot: Pose
        get() =
            coordinatesInRoot?.poseInRoot?.let {
                pose.translate(it.translation).rotate(it.rotation)
            } ?: pose

    /**
     * The pose of this layout modifier node relative to its parent entity in the Compose hierarchy.
     */
    override val poseInParentEntity: Pose
        get() =
            coordinatesInParentEntity?.poseInParentEntity?.let {
                pose.translate(it.translation).rotate(it.rotation)
            } ?: pose

    /**
     * The layout coordinates of the parent [SubspaceLayoutNode] up to the root of the hierarchy
     * including application from any [SubspaceLayoutModifierNode] instances applied to this node.
     *
     * This applies the layout changes of all [SubspaceLayoutModifierNode] instances in the modifier
     * chain and then [layoutNode]'s parent or just [layoutNode]'s parent and this modifier if no
     * other [SubspaceLayoutModifierNode] is present.
     */
    private val coordinatesInRoot: SubspaceLayoutCoordinates?
        get() = parent ?: layoutNode?.measurableLayout?.parentCoordinatesInRoot

    /**
     * The layout coordinates up to the nearest parent [CoreEntity] including mutations from any
     * [SubspaceLayoutModifierNode] instances applied to this node.
     *
     * This applies the layout changes of all [SubspaceLayoutModifierNode] instances in the modifier
     * chain and then [layoutNode]'s parent or just [layoutNode]'s parent and this modifier if no
     * other [SubspaceLayoutModifierNode] is present.
     */
    private val coordinatesInParentEntity: SubspaceLayoutCoordinates?
        get() = parent ?: layoutNode?.measurableLayout?.parentCoordinatesInParentEntity

    /** The size of this layout in the local coordinates space. */
    override val size: IntVolumeSize
        get() = IntVolumeSize(width = measuredWidth, height = measuredHeight, depth = measuredDepth)

    private var subspaceMeasureResult: SubspaceMeasureResult? = null
    private var layoutPose: Pose? = null

    public override fun placeAt(pose: Pose) {
        layoutPose = pose
        subspaceMeasureResult?.placeChildren(
            object : SubspacePlacementScope() {
                public override val coordinates = this@SubspaceLayoutModifierNodeCoordinator
            }
        )
    }

    /**
     * Measures the wrapped content within the given [constraints].
     *
     * @param constraints the constraints to apply during measurement.
     * @return the [SubspacePlaceable] representing the measured child layout that can be positioned
     *   by its parent layout.
     */
    override fun measure(constraints: VolumeConstraints): SubspacePlaceable {
        with(layoutModifierNode) {
            val measurable: SubspaceMeasurable = child ?: layoutNode!!.measurableLayout
            val subspaceMeasureResult: SubspaceMeasureResult =
                LayoutSubspaceMeasureScope(layoutNode!!).measure(measurable, constraints).also {
                    this@SubspaceLayoutModifierNodeCoordinator.subspaceMeasureResult = it
                }

            measuredWidth = subspaceMeasureResult.width
            measuredHeight = subspaceMeasureResult.height
            measuredDepth = subspaceMeasureResult.depth
        }

        return this
    }

    /**
     * Adjusts layout of the wrapped content with a new [ParentLayoutParamsAdjustable].
     *
     * @param params the parameters to be adjusted.
     */
    override fun adjustParams(params: ParentLayoutParamsAdjustable) {
        layoutNode?.measurableLayout?.adjustParams(params)
    }
}
