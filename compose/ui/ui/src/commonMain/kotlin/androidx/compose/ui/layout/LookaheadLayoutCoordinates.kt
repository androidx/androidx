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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.LookaheadDelegate
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

/**
 * [LookaheadLayoutCoordinates] interface holds layout coordinates from both the lookahead
 * calculation and the post-lookahead layout pass.
 */
@Deprecated(
    "LookaheadLayoutCoordinates class has been removed. localLookaheadPositionOf" +
        "can be achieved in LookaheadScope using" +
        " LayoutCoordinates.localLookaheadPositionOf(LayoutCoordinates) function.",
    replaceWith = ReplaceWith("LayoutCoordinates")
)
@ExperimentalComposeUiApi
sealed interface LookaheadLayoutCoordinates : LayoutCoordinates

@Suppress("DEPRECATION")
internal class LookaheadLayoutCoordinatesImpl(val lookaheadDelegate: LookaheadDelegate) :
    LookaheadLayoutCoordinates {
    val coordinator: NodeCoordinator
        get() = lookaheadDelegate.coordinator

    override val size: IntSize
        get() = lookaheadDelegate.let { IntSize(it.width, it.height) }
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = coordinator.providedAlignmentLines

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { NodeCoordinator.ExpectAttachedLayoutCoordinates }
            return coordinator.layoutNode.outerCoordinator.wrappedBy?.let {
                it.lookaheadDelegate?.coordinates
            }
        }
    override val parentCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { NodeCoordinator.ExpectAttachedLayoutCoordinates }
            return coordinator.wrappedBy?.lookaheadDelegate?.coordinates
        }

    override val isAttached: Boolean
        get() = coordinator.isAttached

    private val lookaheadOffset: Offset
        get() = lookaheadDelegate.rootLookaheadDelegate.let {
            localPositionOf(it.coordinates, Offset.Zero) -
                coordinator.localPositionOf(it.coordinator, Offset.Zero)
        }

    override fun windowToLocal(relativeToWindow: Offset): Offset =
        coordinator.windowToLocal(relativeToWindow) + lookaheadOffset

    override fun localToWindow(relativeToLocal: Offset): Offset =
        coordinator.localToWindow(relativeToLocal + lookaheadOffset)

    override fun localToRoot(relativeToLocal: Offset): Offset =
        coordinator.localToRoot(relativeToLocal + lookaheadOffset)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        if (sourceCoordinates is LookaheadLayoutCoordinatesImpl) {
            val source = sourceCoordinates.lookaheadDelegate
            val commonAncestor = coordinator.findCommonAncestor(source.coordinator)

            return commonAncestor.lookaheadDelegate?.let { ancestor ->
                // Common ancestor is in lookahead
                (source.positionIn(ancestor) + relativeToSource.round() -
                    lookaheadDelegate.positionIn(ancestor)).toOffset()
            } ?: commonAncestor.let {
                // The two coordinates are in two separate LookaheadLayouts
                val sourceRoot = source.rootLookaheadDelegate
                val relativePosition = source.positionIn(sourceRoot) +
                    sourceRoot.position + relativeToSource.round() -
                    with(lookaheadDelegate) {
                        (positionIn(rootLookaheadDelegate) + rootLookaheadDelegate.position)
                    }

                lookaheadDelegate.rootLookaheadDelegate.coordinator.wrappedBy!!.localPositionOf(
                    sourceRoot.coordinator.wrappedBy!!, relativePosition.toOffset()
                )
            }
        } else {
            val rootDelegate = lookaheadDelegate.rootLookaheadDelegate
            // This is a case of mixed coordinates where `this` is lookahead coords, and
            // `sourceCoordinates` isn't. Therefore we'll break this into two parts:
            // local position in lookahead coords space && local position in regular layout coords
            // space.
            return localPositionOf(rootDelegate.lookaheadLayoutCoordinates, relativeToSource) +
                rootDelegate.coordinator.coordinates.localPositionOf(sourceCoordinates, Offset.Zero)
        }
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = coordinator.localBoundingBoxOf(sourceCoordinates, clipBounds)

    override fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        coordinator.transformFrom(sourceCoordinates, matrix)
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
