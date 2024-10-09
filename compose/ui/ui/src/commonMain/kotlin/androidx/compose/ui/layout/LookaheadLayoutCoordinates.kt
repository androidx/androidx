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

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.node.LookaheadDelegate
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

internal class LookaheadLayoutCoordinates(val lookaheadDelegate: LookaheadDelegate) :
    LayoutCoordinates {
    val coordinator: NodeCoordinator
        get() = lookaheadDelegate.coordinator

    override val size: IntSize
        get() = lookaheadDelegate.let { IntSize(it.width, it.height) }

    override val providedAlignmentLines: Set<AlignmentLine>
        get() = coordinator.providedAlignmentLines

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() {
            checkPrecondition(isAttached) { NodeCoordinator.ExpectAttachedLayoutCoordinates }
            return coordinator.layoutNode.outerCoordinator.wrappedBy?.let {
                it.lookaheadDelegate?.coordinates
            }
        }

    override val parentCoordinates: LayoutCoordinates?
        get() {
            checkPrecondition(isAttached) { NodeCoordinator.ExpectAttachedLayoutCoordinates }
            return coordinator.wrappedBy?.lookaheadDelegate?.coordinates
        }

    override val isAttached: Boolean
        get() = coordinator.isAttached

    override val introducesMotionFrameOfReference: Boolean
        get() = lookaheadDelegate.isPlacedUnderMotionFrameOfReference

    private val lookaheadOffset: Offset
        get() =
            lookaheadDelegate.rootLookaheadDelegate.let {
                localPositionOf(it.coordinates, Offset.Zero) -
                    coordinator.localPositionOf(it.coordinator, Offset.Zero)
            }

    override fun screenToLocal(relativeToScreen: Offset): Offset =
        coordinator.screenToLocal(relativeToScreen) + lookaheadOffset

    override fun localToScreen(relativeToLocal: Offset): Offset =
        coordinator.localToScreen(relativeToLocal + lookaheadOffset)

    override fun windowToLocal(relativeToWindow: Offset): Offset =
        coordinator.windowToLocal(relativeToWindow) + lookaheadOffset

    override fun localToWindow(relativeToLocal: Offset): Offset =
        coordinator.localToWindow(relativeToLocal + lookaheadOffset)

    override fun localToRoot(relativeToLocal: Offset): Offset =
        coordinator.localToRoot(relativeToLocal + lookaheadOffset)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset =
        localPositionOf(
            sourceCoordinates = sourceCoordinates,
            relativeToSource = relativeToSource,
            includeMotionFrameOfReference = true
        )

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset,
        includeMotionFrameOfReference: Boolean
    ): Offset {
        if (sourceCoordinates is LookaheadLayoutCoordinates) {
            val source = sourceCoordinates.lookaheadDelegate
            source.coordinator.onCoordinatesUsed()
            val commonAncestor = coordinator.findCommonAncestor(source.coordinator)

            return commonAncestor.lookaheadDelegate?.let { ancestor ->
                // Common ancestor is in lookahead
                val sourceInCommonAncestor =
                    source.positionIn(
                        ancestor = ancestor,
                        excludingAgnosticOffset = !includeMotionFrameOfReference
                    ) + relativeToSource.round()

                val lookaheadPosInAncestor =
                    lookaheadDelegate.positionIn(
                        ancestor = ancestor,
                        excludingAgnosticOffset = !includeMotionFrameOfReference
                    )

                (sourceInCommonAncestor - lookaheadPosInAncestor).toOffset()
            }
                ?: commonAncestor.let {
                    // The two coordinates are in two separate LookaheadLayouts
                    val sourceRoot = source.rootLookaheadDelegate

                    val sourcePosition =
                        source.positionIn(
                            ancestor = sourceRoot,
                            excludingAgnosticOffset = !includeMotionFrameOfReference
                        ) + sourceRoot.position + relativeToSource.round()

                    val rootDelegate = lookaheadDelegate.rootLookaheadDelegate
                    val lookaheadPosition =
                        lookaheadDelegate.positionIn(
                            ancestor = rootDelegate,
                            excludingAgnosticOffset = !includeMotionFrameOfReference
                        ) + rootDelegate.position

                    val relativePosition = (sourcePosition - lookaheadPosition).toOffset()

                    rootDelegate.coordinator.wrappedBy!!.localPositionOf(
                        sourceCoordinates = sourceRoot.coordinator.wrappedBy!!,
                        relativeToSource = relativePosition,
                        includeMotionFrameOfReference = includeMotionFrameOfReference
                    )
                }
        } else {
            // This is a case of mixed coordinates where `this` is lookahead coords, and
            // `sourceCoordinates` isn't. Therefore we'll break this into two parts:
            // local position in lookahead coords space && local position in regular layout coords
            // space.
            val rootDelegate = lookaheadDelegate.rootLookaheadDelegate

            val localLookaheadPos =
                localPositionOf(
                    sourceCoordinates = rootDelegate.lookaheadLayoutCoordinates,
                    relativeToSource = relativeToSource,
                    includeMotionFrameOfReference = includeMotionFrameOfReference
                ) - rootDelegate.position.toOffset()

            // If Lookahead is the hierarchy's absolute root (no parent), we may use its coordinates
            // directly
            val rootDelegateCoordinates =
                rootDelegate.coordinator.parentCoordinates ?: rootDelegate.coordinator.coordinates

            val localPos =
                rootDelegateCoordinates.localPositionOf(
                    sourceCoordinates = sourceCoordinates,
                    relativeToSource = Offset.Zero,
                    includeMotionFrameOfReference = includeMotionFrameOfReference
                )
            return localLookaheadPos + localPos
        }
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = coordinator.localBoundingBoxOf(sourceCoordinates, clipBounds)

    override fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        coordinator.transformFrom(sourceCoordinates, matrix)
    }

    override fun transformToScreen(matrix: Matrix) {
        coordinator.transformToScreen(matrix)
    }

    override fun get(alignmentLine: AlignmentLine): Int = lookaheadDelegate.get(alignmentLine)
}

internal val LookaheadDelegate.rootLookaheadDelegate: LookaheadDelegate
    get() {
        var root = layoutNode
        while (root.parent?.lookaheadRoot != null) {
            val lookaheadRoot = root.parent?.lookaheadRoot!!
            if (lookaheadRoot.isVirtualLookaheadRoot) {
                root = root.parent!!
            } else {
                root = root.parent!!.lookaheadRoot!!
            }
        }
        return root.outerCoordinator.lookaheadDelegate!!
    }
