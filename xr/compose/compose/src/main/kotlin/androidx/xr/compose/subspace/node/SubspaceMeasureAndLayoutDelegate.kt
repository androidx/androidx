/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.subspace.node

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.util.fastForEach
import androidx.xr.compose.subspace.node.SubspaceLayoutNode.LayoutState
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

internal class SubspaceMeasureAndLayoutDelegate(private val root: SubspaceLayoutNode) {
    val snapshotStateObserver: SnapshotStateObserver = SnapshotStateObserver(::run)

    /**
     * Set of nodes that have invalidated measurement and need to to call "remeasure()".
     * Automatically sorted by depth (parents first)
     */
    private val nodesPendingMeasure = SubspaceDepthSortedSet(extraAssertions = false)

    /**
     * Set of nodes that have invalidated layout and need to call "replace". Automatically sorted by
     * depth.
     */
    private val nodesPendingLayout = SubspaceDepthSortedSet(extraAssertions = false)

    /**
     * Set of nodes that have invalidated Core Entities and need to be updated. Automatically sorted
     * by depth "
     */
    private val nodesPendingEntityUpdate = SubspaceDepthSortedSet(extraAssertions = false)

    /**
     * The current state of the measure and layout scheduler. With this we determine if we should
     * postpone the request of remeasurement and relayout of the current node until the current
     * measurement and layout pass has finished.
     */
    private var measureAndLayoutInProgress = false

    /**
     * List of measure invalidation requests that are in the queue to be processed in the next
     * measurement and layout pass.
     */
    private val postponedRequests = mutableListOf<SubspaceLayoutNode>()

    private val onCommitAffectingMeasure: (SubspaceLayoutNode) -> Unit = { layoutNode ->
        layoutNode.requestMeasure()
    }

    private val onCommitAffectingLayout: (SubspaceLayoutNode) -> Unit = { layoutNode ->
        layoutNode.requestLayout()
    }

    private val onCommitAffectingEntityUpdate: (SubspaceLayoutNode) -> Unit = {
        it.requestEntityUpdate()
    }

    /**
     * Requests remeasure for this [node] and nodes affected by its measure result.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestMeasure(node: SubspaceLayoutNode, forceRequest: Boolean = false): Boolean {
        if (node.layoutState == LayoutState.Measuring) {
            return false
        }

        if (node.layoutState == LayoutState.LayingOut) {
            postponedRequests.add(node)
            return false
        }
        if (!node.isAttached || (node.measurePending && !forceRequest)) {
            return false
        }

        node.measurePending = true
        node.layoutPending = true
        nodesPendingMeasure.add(node)
        return !measureAndLayoutInProgress
    }

    /**
     * Requests relayout for this [node] and nodes affected by its position.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestLayout(node: SubspaceLayoutNode, forceRequest: Boolean = false): Boolean {
        if (
            node.layoutState == LayoutState.Measuring || node.layoutState == LayoutState.LayingOut
        ) {
            return false
        }
        if (!node.isAttached || (node.layoutPending && !forceRequest)) {
            return false
        }

        node.layoutPending = true
        nodesPendingLayout.add(node)
        return !measureAndLayoutInProgress
    }

    fun requestEntityUpdate(node: SubspaceLayoutNode, forceRequest: Boolean = false): Boolean {
        if (!node.isAttached || (node.entityUpdatePending && !forceRequest)) return false

        node.entityUpdatePending = true
        nodesPendingEntityUpdate.add(node)
        return !measureAndLayoutInProgress
    }

    /**
     * This is the main measure and layout pass, triggered by the [snapshotStateObserver]. It
     * processes all nodes that have been marked as "dirty" for measure or layout.
     */
    fun measureAndLayout() {
        if (
            nodesPendingMeasure.isEmpty() &&
                nodesPendingLayout.isEmpty() &&
                nodesPendingEntityUpdate.isEmpty()
        ) {
            return // No measurement, layout, or entity updates scheduled.
        }
        measureAndLayoutInProgress = true

        try {
            if (nodesPendingMeasure.firstOrNull() == root) {
                processRootNode()
            } else {
                processMeasureRequests()
                processLayoutRequests()
            }
            processEntityUpdateRequests()
        } finally {
            measureAndLayoutInProgress = false
            drainPostponedRequests()
        }
    }

    private fun processRootNode() {
        nodesPendingMeasure.clear()
        nodesPendingLayout.clear()
        snapshotStateObserver.observeReads(root, onCommitAffectingMeasure) {
            root.measurableLayout.measure(VolumeConstraints())
        }

        snapshotStateObserver.observeReads(root, onCommitAffectingLayout) {
            root.measurableLayout.placeAt(Pose.Identity)
        }
    }

    private fun processMeasureRequests() {
        nodesPendingMeasure.drain { originalNode ->
            if (originalNode.measurePending && originalNode.isAttached) {
                var node = originalNode
                var sizeChanged = node.remeasureWithSnapshot()
                while (sizeChanged && node.parent != null) {
                    node = checkNotNull(node.parent) { "Node detached during measurement." }
                    sizeChanged = node.remeasureWithSnapshot()
                }
                requestLayout(node, true)
            }
        }
    }

    private fun processLayoutRequests() {
        nodesPendingLayout.drain { node ->
            if (node.layoutPending && node.isAttached) {
                snapshotStateObserver.observeReads(node, onCommitAffectingLayout) { node.replace() }
            }
        }
    }

    private fun processEntityUpdateRequests() {
        nodesPendingEntityUpdate.drain { node ->
            if (node.entityUpdatePending && node.isAttached) {
                snapshotStateObserver.observeReads(node, onCommitAffectingEntityUpdate) {
                    node.updateCoreEntityProperties()
                }
            }
        }
    }

    private fun drainPostponedRequests() {
        postponedRequests.fastForEach { it.requestMeasure() }
        postponedRequests.clear()
    }

    private fun SubspaceLayoutNode.remeasureWithSnapshot(): Boolean {
        var result = false
        snapshotStateObserver.observeReads(this, onCommitAffectingMeasure) { result = remeasure() }
        return result
    }
}
