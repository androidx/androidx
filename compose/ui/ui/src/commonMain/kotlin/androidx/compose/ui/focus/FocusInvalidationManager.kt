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

import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitSelfAndChildren
import androidx.compose.ui.util.fastForEach

/**
 * The [FocusInvalidationManager] allows us to schedule focus related nodes for invalidation. These
 * nodes are invalidated after onApplyChanges. It does this by registering an onApplyChangesListener
 * when nodes are scheduled for invalidation.
 */
internal class FocusInvalidationManager(
    private val onRequestApplyChangesListener: (() -> Unit) -> Unit,
    private val invalidateOwnerFocusState: () -> Unit,
    private val rootFocusStateFetcher: () -> FocusState
) {
    private val focusTargetNodes = mutableListOf<FocusTargetNode>()
    private val focusEventNodes = mutableListOf<FocusEventModifierNode>()
    private val focusPropertiesNodes = mutableListOf<FocusPropertiesModifierNode>()
    private val focusTargetsWithInvalidatedFocusEvents = mutableListOf<FocusTargetNode>()

    fun scheduleInvalidation(node: FocusTargetNode) {
        focusTargetNodes.scheduleInvalidation(node)
    }

    fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusEventNodes.scheduleInvalidation(node)
    }

    fun scheduleInvalidation(node: FocusPropertiesModifierNode) {
        focusPropertiesNodes.scheduleInvalidation(node)
    }

    fun hasPendingInvalidation(): Boolean {
        return focusTargetNodes.isNotEmpty() ||
            focusPropertiesNodes.isNotEmpty() ||
            focusEventNodes.isNotEmpty()
    }

    private fun <T> MutableList<T>.scheduleInvalidation(node: T) {
        if (add(node)) {
            // If this is the first node scheduled for invalidation,
            // we set up a listener that runs after onApplyChanges.
            if (focusTargetNodes.size + focusEventNodes.size + focusPropertiesNodes.size == 1) {
                onRequestApplyChangesListener.invoke(::invalidateNodes)
            }
        }
    }

    private fun invalidateNodes() {
        if (!rootFocusStateFetcher().hasFocus) {
            // If root doesn't have focus, skip full invalidation and default to the Inactive state.
            focusEventNodes.fastForEach { it.onFocusEvent(Inactive) }
            focusTargetNodes.fastForEach { node ->
                if (node.isAttached && !node.isInitialized()) {
                    node.initializeFocusState(Inactive)
                }
            }
            focusTargetNodes.clear()
            focusEventNodes.clear()
            focusPropertiesNodes.clear()
            focusTargetsWithInvalidatedFocusEvents.clear()
            invalidateOwnerFocusState()
            return
        }

        // Process all the invalidated FocusProperties nodes.
        focusPropertiesNodes.fastForEach {
            // We don't need to invalidate a focus properties node if it was scheduled for
            // invalidation earlier in the composition but was then removed.
            if (!it.node.isAttached) return@fastForEach

            it.visitSelfAndChildren(Nodes.FocusTarget) { focusTarget ->
                focusTargetNodes.add(focusTarget)
            }
        }
        focusPropertiesNodes.clear()

        // Process all the focus events nodes.
        focusEventNodes.fastForEach { focusEventNode ->
            // When focus nodes are removed, the corresponding focus events are scheduled for
            // invalidation. If the focus event was also removed, we don't need to invalidate it.
            // We call onFocusEvent with the default value, just to make it easier for the user,
            // so that they don't have to keep track of whether they caused a focused item to be
            // removed (Which would cause it to lose focus).
            if (!focusEventNode.node.isAttached) {
                focusEventNode.onFocusEvent(Inactive)
                return@fastForEach
            }

            var requiresUpdate = true
            var aggregatedNode = false
            var focusTarget: FocusTargetNode? = null
            focusEventNode.visitSelfAndChildren(Nodes.FocusTarget) {

                // If there are multiple focus targets associated with this focus event node,
                // we need to calculate the aggregated state.
                if (focusTarget != null) {
                    aggregatedNode = true
                }

                focusTarget = it

                // If the associated focus node is already scheduled for invalidation, it will
                // send an onFocusEvent if the invalidation causes a focus state change.
                // However this onFocusEvent was invalidated, so we have to ensure that we call
                // onFocusEvent even if the focus state didn't change.
                if (it in focusTargetNodes) {
                    requiresUpdate = false
                    focusTargetsWithInvalidatedFocusEvents.add(it)
                    return@visitSelfAndChildren
                }
            }

            if (requiresUpdate) {
                focusEventNode.onFocusEvent(
                    if (aggregatedNode) {
                        focusEventNode.getFocusState()
                    } else {
                        focusTarget?.focusState ?: Inactive
                    }
                )
            }
        }
        focusEventNodes.clear()

        // Process all the focus target nodes.
        focusTargetNodes.fastForEach {
            // We don't need to invalidate the focus target if it was scheduled for invalidation
            // earlier in the composition but was then removed.
            if (!it.isAttached) return@fastForEach

            val preInvalidationState = it.focusState
            it.invalidateFocus()
            if (
                preInvalidationState != it.focusState ||
                    it in focusTargetsWithInvalidatedFocusEvents
            ) {
                it.dispatchFocusCallbacks()
            }
        }
        focusTargetNodes.clear()
        // Clear the set so we can reuse it
        focusTargetsWithInvalidatedFocusEvents.clear()

        invalidateOwnerFocusState()

        checkPrecondition(focusPropertiesNodes.isEmpty()) { "Unprocessed FocusProperties nodes" }
        checkPrecondition(focusEventNodes.isEmpty()) { "Unprocessed FocusEvent nodes" }
        checkPrecondition(focusTargetNodes.isEmpty()) { "Unprocessed FocusTarget nodes" }
    }
}
