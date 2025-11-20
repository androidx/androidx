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

package androidx.compose.ui.focus

import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.visitAncestors

/**
 * The [FocusInvalidationManager] allows us to schedule focus related nodes for invalidation. These
 * nodes are invalidated after onApplyChanges. It does this by registering an onApplyChangesListener
 * when nodes are scheduled for invalidation.
 */
internal class FocusInvalidationManager(
    private val focusOwner: FocusOwner,
    private val owner: Owner,
) {
    private val focusTargetNodes = mutableScatterSetOf<FocusTargetNode>()
    private val focusEventNodes = mutableScatterSetOf<FocusEventModifierNode>()

    private var isInvalidationScheduled = false

    fun scheduleInvalidation(node: FocusTargetNode) {
        if (focusTargetNodes.add(node)) scheduleInvalidation()
    }

    fun scheduleInvalidation(node: FocusEventModifierNode) {
        if (focusEventNodes.add(node)) scheduleInvalidation()
    }

    fun scheduleInvalidation() {
        if (!isInvalidationScheduled) {
            owner.registerOnEndApplyChangesListener(::invalidateNodes)
            isInvalidationScheduled = true
        }
    }

    fun hasPendingInvalidation(): Boolean = isInvalidationScheduled

    private fun invalidateNodes() {
        val activeFocusTargetNode = focusOwner.activeFocusTargetNode
        if (activeFocusTargetNode == null) {
            // If there is no active focus node, dispatch the Inactive state to event nodes.
            focusEventNodes.forEach { it.onFocusEvent(Inactive) }
        } else if (activeFocusTargetNode.isAttached) {
            if (focusTargetNodes.contains(activeFocusTargetNode)) {
                activeFocusTargetNode.invalidateFocus()
            }

            val activeFocusTargetNodeState = activeFocusTargetNode.focusState
            var traversedFocusTargetCount = 0
            activeFocusTargetNode.visitAncestors(
                Nodes.FocusTarget or Nodes.FocusEvent,
                includeSelf = true,
            ) {
                // Keep track of whether we traversed past the first target node ancestor of the
                // active focus target node, so that all the subsequent event nodes are sent the
                // ActiveParent state rather than Active/Captured.
                if (it.isKind(Nodes.FocusTarget)) traversedFocusTargetCount++

                // Don't send events to event nodes that were not invalidated.
                if (it !is FocusEventModifierNode || !focusEventNodes.contains(it)) {
                    return@visitAncestors
                }

                // Event nodes that are between the active focus target and the first ancestor
                // target receive the Active/Captured state, while the event nodes further up
                // receive the ActiveParent state.
                if (traversedFocusTargetCount <= 1) {
                    it.onFocusEvent(activeFocusTargetNodeState)
                } else {
                    it.onFocusEvent(ActiveParent)
                }

                // Remove the event node from the list of invalidated nodes, so that we only send a
                // single event per node.
                focusEventNodes.remove(it)
            }

            // Send the Inactive state to the event nodes that are not in the active node ancestors.
            focusEventNodes.forEach { it.onFocusEvent(Inactive) }
        }

        invalidateOwnerFocusState()
        focusTargetNodes.clear()
        focusEventNodes.clear()
        isInvalidationScheduled = false
    }

    /**
     * At the end of the invalidations, we need to ensure that the focus system is in a valid state.
     */
    private fun invalidateOwnerFocusState() {
        // If an active item is removed, we currently clear focus from the hierarchy. We don't
        // clear focus from the root because that could cause initial focus logic to be re-run.
        // Now that all the invalidations are complete, we run owner.clearFocus() if needed.
        if (focusOwner.activeFocusTargetNode == null || focusOwner.rootState == Inactive) {
            focusOwner.clearOwnerFocus()
        }
    }
}
