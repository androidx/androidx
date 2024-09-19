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

package androidx.compose.ui.layout

import androidx.compose.ui.internal.requirePreconditionNotNull
import androidx.compose.ui.internal.throwIllegalArgumentExceptionForNullCheck
import androidx.compose.ui.node.LayoutModifierNodeCoordinator
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.checkMeasuredSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/** The receiver scope of a layout's intrinsic approach measurements lambdas. */
sealed interface ApproachIntrinsicMeasureScope : IntrinsicMeasureScope {

    /** Constraints used to measure the layout in the lookahead pass. */
    val lookaheadConstraints: Constraints

    /**
     * Size of the [ApproachLayoutModifierNode] measured during the lookahead pass using
     * [lookaheadConstraints]. This size can be used as the target size for the
     * [ApproachLayoutModifierNode] to approach the destination (i.e. lookahead) size.
     */
    val lookaheadSize: IntSize
}

/**
 * [ApproachMeasureScope] provides access to lookahead results to allow [ApproachLayoutModifierNode]
 * to leverage lookahead results to define how measurements and placements approach their
 * destination.
 *
 * [ApproachMeasureScope.lookaheadSize] provides the target size of the layout. By knowing the
 * target size and position, layout adjustments such as animations can be defined in
 * [ApproachLayoutModifierNode] to morph the layout gradually in both size and position to arrive at
 * its precalculated bounds.
 */
sealed interface ApproachMeasureScope : ApproachIntrinsicMeasureScope, MeasureScope

internal class ApproachMeasureScopeImpl(
    val coordinator: LayoutModifierNodeCoordinator,
    var approachNode: ApproachLayoutModifierNode,
) : ApproachMeasureScope, MeasureScope by coordinator, LookaheadScope {
    override val lookaheadConstraints: Constraints
        get() =
            requirePreconditionNotNull(coordinator.lookaheadConstraints) {
                "Error: Lookahead constraints requested before lookahead measure."
            }

    override val lookaheadSize: IntSize
        get() = coordinator.lookaheadDelegate!!.measureResult.let { IntSize(it.width, it.height) }

    internal var approachMeasureRequired: Boolean = false

    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        if (this is LookaheadLayoutCoordinates) return this
        if (this is NodeCoordinator) {
            return lookaheadDelegate?.lookaheadLayoutCoordinates ?: this
        }
        throwIllegalArgumentExceptionForNullCheck("Unsupported LayoutCoordinates")
    }

    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() {
            val lookaheadRoot = coordinator.layoutNode.lookaheadRoot
            requirePreconditionNotNull(lookaheadRoot) {
                "Error: Requesting LookaheadScopeCoordinates is not permitted from outside of a" +
                    " LookaheadScope."
            }
            return if (lookaheadRoot.isVirtualLookaheadRoot) {
                lookaheadRoot.parent?.innerCoordinator
                    // Root node is in a lookahead scope
                    ?: lookaheadRoot.children[0].outerCoordinator
            } else {
                lookaheadRoot.outerCoordinator
            }
        }

    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<out AlignmentLine, Int>,
        rulers: (RulerScope.() -> Unit)?,
        placementBlock: Placeable.PlacementScope.() -> Unit
    ): MeasureResult {
        checkMeasuredSize(width, height)
        return object : MeasureResult {
            override val width = width
            override val height = height

            @Suppress("PrimitiveInCollection") override val alignmentLines = alignmentLines
            override val rulers = rulers

            override fun placeChildren() {
                coordinator.placementScope.placementBlock()
            }
        }
    }

    // Intermediate layout pass is post-lookahead. Therefore isLookingAhead is always false.
    override val isLookingAhead: Boolean
        get() = false
}
