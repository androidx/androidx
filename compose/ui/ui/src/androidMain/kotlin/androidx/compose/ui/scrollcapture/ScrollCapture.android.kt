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

package androidx.compose.ui.scrollcapture

import android.graphics.Point
import android.view.ScrollCaptureCallback
import android.view.ScrollCaptureTarget
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.isVisible
import androidx.compose.ui.semantics.SemanticsActions.ScrollByOffset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties.Disabled
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

/** Separate class to host the implementation of scroll capture for dex verification. */
@RequiresApi(31)
internal class ScrollCapture : ComposeScrollCaptureCallback.ScrollCaptureSessionListener {

    var scrollCaptureInProgress: Boolean by mutableStateOf(false)
        private set

    /**
     * Implements scroll capture (long screenshots) support for a composition. Finds a single
     * [ScrollCaptureTarget] to propose to the platform. Searches over the semantics tree to find
     * nodes that publish vertical scroll semantics (namely [ScrollByOffset] and
     * [VerticalScrollAxisRange]) and then uses logic similar to how the platform searches [View]
     * targets to select the deepest, largest scroll container. If a target is found, an
     * implementation of [ScrollCaptureCallback] is created for it (see
     * [ComposeScrollCaptureCallback]) and given to the platform.
     *
     * The platform currently only supports scroll capture for containers that scroll vertically.
     * The API supports horizontal as well, but it's not used. To keep this code simpler and avoid
     * having dead code, we only implement vertical scroll capture as well.
     *
     * See go/compose-long-screenshots for more background.
     */
    // Required not to be inlined for class verification.
    fun onScrollCaptureSearch(
        view: View,
        semanticsOwner: SemanticsOwner,
        coroutineContext: CoroutineContext,
        targets: Consumer<ScrollCaptureTarget>
    ) {
        // Search the semantics tree for scroll containers.
        val candidates = mutableVectorOf<ScrollCaptureCandidate>()
        visitScrollCaptureCandidates(
            fromNode = semanticsOwner.unmergedRootSemanticsNode,
            onCandidate = candidates::add
        )

        // Sort to find the deepest node with the biggest bounds in the dimension(s) that the node
        // supports scrolling in.
        candidates.sortWith(
            compareBy(
                { it.depth },
                { it.viewportBoundsInWindow.height },
            )
        )
        val candidate = candidates.lastOrNull() ?: return

        // If we found a candidate, create a capture callback for it and give it to the system.
        val coroutineScope = CoroutineScope(coroutineContext)
        val callback =
            ComposeScrollCaptureCallback(
                node = candidate.node,
                viewportBoundsInWindow = candidate.viewportBoundsInWindow,
                coroutineScope = coroutineScope,
                listener = this
            )
        val localVisibleRectOfCandidate = candidate.coordinates.boundsInRoot()
        val windowOffsetOfCandidate = candidate.viewportBoundsInWindow.topLeft
        targets.accept(
            ScrollCaptureTarget(
                    view,
                    localVisibleRectOfCandidate.roundToIntRect().toAndroidRect(),
                    windowOffsetOfCandidate.let { Point(it.x, it.y) },
                    callback
                )
                .apply { scrollBounds = candidate.viewportBoundsInWindow.toAndroidRect() }
        )
    }

    override fun onSessionStarted() {
        scrollCaptureInProgress = true
    }

    override fun onSessionEnded() {
        scrollCaptureInProgress = false
    }
}

/**
 * Walks the tree of [SemanticsNode]s rooted at [fromNode] to find nodes that look scrollable and
 * calculate their nesting depth.
 */
private fun visitScrollCaptureCandidates(
    fromNode: SemanticsNode,
    depth: Int = 0,
    onCandidate: (ScrollCaptureCandidate) -> Unit
) {
    fromNode.visitDescendants { node ->
        // Invisible/disabled nodes can't be candidates, nor can any of their descendants.
        if (!node.isVisible || Disabled in node.unmergedConfig) {
            return@visitDescendants false
        }

        val nodeCoordinates =
            checkPreconditionNotNull(node.findCoordinatorToGetBounds()) {
                    "Expected semantics node to have a coordinator."
                }
                .coordinates

        // Zero-sized nodes can't be candidates, and by definition would clip all their children so
        // they and their descendants can't be candidates either.
        val viewportBoundsInWindow = nodeCoordinates.boundsInWindow().roundToIntRect()
        if (viewportBoundsInWindow.isEmpty) {
            return@visitDescendants false
        }

        // If the node is visible, we need to check if it's scrollable.
        // TODO(b/329295945) Support explicit opt-in/-out.
        // Don't care about horizontal scroll containers.
        if (!node.canScrollVertically) {
            // Not a scrollable, so can't be a candidate, but its descendants might be.
            return@visitDescendants true
        }

        // We found a node that looks scrollable! Report it, then visit its children with an
        // incremented depth counter.
        val candidateDepth = depth + 1
        onCandidate(
            ScrollCaptureCandidate(
                node = node,
                depth = candidateDepth,
                viewportBoundsInWindow = viewportBoundsInWindow,
                coordinates = nodeCoordinates,
            )
        )
        visitScrollCaptureCandidates(
            fromNode = node,
            depth = candidateDepth,
            onCandidate = onCandidate
        )
        // We've just visited descendants ourselves, don't need this visit call to do it.
        return@visitDescendants false
    }
}

internal val SemanticsNode.scrollCaptureScrollByAction
    get() = unmergedConfig.getOrNull(ScrollByOffset)

private val SemanticsNode.canScrollVertically: Boolean
    get() {
        val scrollByOffset = scrollCaptureScrollByAction
        val verticalScrollAxisRange = unmergedConfig.getOrNull(VerticalScrollAxisRange)
        return scrollByOffset != null &&
            verticalScrollAxisRange != null &&
            verticalScrollAxisRange.maxValue() > 0f
    }

/**
 * Visits all the descendants of this [SemanticsNode].
 *
 * @param onNode Function called for each [SemanticsNode]. Iff this function returns true, the
 *   children of the current node will be visited.
 */
private inline fun SemanticsNode.visitDescendants(onNode: (SemanticsNode) -> Boolean) {
    val nodes = mutableVectorOf<SemanticsNode>()
    nodes.addAll(getChildrenForSearch())
    while (nodes.isNotEmpty()) {
        val node = nodes.removeAt(nodes.lastIndex)
        val visitChildren = onNode(node)
        if (visitChildren) {
            nodes.addAll(node.getChildrenForSearch())
        }
    }
}

private fun SemanticsNode.getChildrenForSearch() =
    getChildren(
        includeDeactivatedNodes = false,
        includeReplacedSemantics = false,
        includeFakeNodes = false
    )

/**
 * Information about a potential [ScrollCaptureTarget] needed to both select the final candidate and
 * create its [ComposeScrollCaptureCallback].
 */
private class ScrollCaptureCandidate(
    val node: SemanticsNode,
    val depth: Int,
    val viewportBoundsInWindow: IntRect,
    val coordinates: LayoutCoordinates,
) {
    override fun toString(): String =
        "ScrollCaptureCandidate(node=$node, " +
            "depth=$depth, " +
            "viewportBoundsInWindow=$viewportBoundsInWindow, " +
            "coordinates=$coordinates)"
}
