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

package androidx.compose.ui.relocation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.toSize

/**
 * A node that can respond to [bringIntoView] requests from its children by moving or adjusting its
 * content.
 */
public interface BringIntoViewModifierNode : DelegatableNode {
    /**
     * Moves or adjusts this node's content so that [boundsProvider] will be in visible bounds. Must
     * ensure that the request is propagated up to the parent node.
     *
     * This method will not return until this request has been satisfied or interrupted by a newer
     * request.
     *
     * @param childCoordinates The [LayoutCoordinates] of the child node making the request. This
     *   parent can use these [LayoutCoordinates] to translate [boundsProvider] into its own
     *   coordinates.
     * @param boundsProvider A function returning the rectangle to bring into view, relative to
     *   [childCoordinates]. The function may return a different value over time, if the bounds of
     *   the request change while the request is being processed. If the rectangle cannot be
     *   calculated, e.g. because [childCoordinates] is not attached, return null.
     */
    public suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    )
}

/**
 * Bring this node into visible bounds. Does nothing if the node is not attached.
 *
 * This method will not return until this request is satisfied or a newer request interrupts it. If
 * this call is interrupted by a newer call, this method will throw a
 * [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * @param bounds provides the bounds (In local coordinates) that should be brought into view. The
 *   function may return a different value over time, if the bounds of the request change while the
 *   request is being processed. If you don't provide bounds, the whole node bounds will be used.
 */
public suspend fun DelegatableNode.bringIntoView(bounds: (() -> Rect?)? = null) {
    if (!node.isAttached) return
    val parent = nearestAncestor(Nodes.BringIntoView) ?: return
    val layoutCoordinates = requireLayoutCoordinates()

    parent.bringIntoView(layoutCoordinates) {
        // If the rect is not specified, use a rectangle representing the entire composable.
        // If the coordinates are detached when this call is made, we don't bother even
        // submitting the request, but if the coordinates become detached while the request
        // is being handled we just return a null Rect.
        bounds?.invoke() ?: layoutCoordinates.takeIf { it.isAttached }?.size?.toSize()?.toRect()
    }
}
