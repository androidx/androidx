/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.Nodes.FocusTarget
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.util.trace

internal fun FocusTargetNode.requestFocus(focusDirection: FocusDirection): Boolean? {
    trace("FocusTransactions:requestFocus") {
        if (!fetchFocusProperties().canFocus) return false
        return requireTransactionManager().withNewTransaction(
            onCancelled = { if (node.isAttached) dispatchFocusCallbacks() }
        ) {
            when (performCustomRequestFocus(focusDirection)) {
                None -> performRequestFocus()
                Redirected -> true
                Cancelled,
                RedirectCancelled -> null
            }
        }
    }
}

/**
 * This function performs the request focus action.
 *
 * Note: Do not call this directly, consider using [requestFocus], which will check if any custom
 * focus [enter][FocusProperties.enter] and [exit][FocusProperties.exit]
 * [properties][FocusProperties] have been specified.
 */
internal fun FocusTargetNode.performRequestFocus(): Boolean {
    val success =
        when (focusState) {
            Active,
            Captured -> true
            ActiveParent -> clearChildFocus() && grantFocus()
            Inactive -> {
                val parent = nearestAncestor(FocusTarget)
                if (parent != null) {
                    val prevState = parent.focusState
                    val success = parent.requestFocusForChild(this)
                    if (success && prevState !== parent.focusState) {
                        parent.dispatchFocusCallbacks()
                    }
                    success
                } else {
                    requestFocusForOwner() && grantFocus()
                }
            }
        }
    if (success) dispatchFocusCallbacks()
    return success
}

/**
 * Deny requests to clear focus.
 *
 * This is used when a component wants to hold onto focus (eg. A phone number field with an invalid
 * number.
 *
 * @return true if the focus was successfully captured. False otherwise.
 */
internal fun FocusTargetNode.captureFocus() =
    requireTransactionManager().withNewTransaction {
        when (focusState) {
            Active -> {
                focusState = Captured
                dispatchFocusCallbacks()
                true
            }
            Captured -> true
            ActiveParent,
            Inactive -> false
        }
    }

/**
 * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
 * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other nodes
 * from requesting focus.
 *
 * @return true if the captured focus was released. False Otherwise.
 */
internal fun FocusTargetNode.freeFocus() =
    requireTransactionManager().withNewTransaction {
        when (focusState) {
            Captured -> {
                focusState = Active
                dispatchFocusCallbacks()
                true
            }
            Active -> true
            ActiveParent,
            Inactive -> false
        }
    }

/**
 * This function clears focus from this node.
 *
 * Note: This function should only be called by a parent [focus node][FocusTargetNode] to clear
 * focus from one of its child [focus node][FocusTargetNode]s. It does not change the state of the
 * parent.
 */
internal fun FocusTargetNode.clearFocus(
    forced: Boolean = false,
    refreshFocusEvents: Boolean
): Boolean =
    when (focusState) {
        Active -> {
            focusState = Inactive
            if (refreshFocusEvents) dispatchFocusCallbacks()
            true
        }
        /**
         * If the node is [ActiveParent], we need to clear focus from the [Active] descendant first,
         * before clearing focus from this node.
         */
        ActiveParent ->
            if (clearChildFocus(forced, refreshFocusEvents)) {
                focusState = Inactive
                if (refreshFocusEvents) dispatchFocusCallbacks()
                true
            } else {
                false
            }

        /** If the node is [Captured], deny requests to clear focus, except for a forced clear. */
        Captured -> {
            if (forced) {
                focusState = Inactive
                if (refreshFocusEvents) dispatchFocusCallbacks()
            }
            forced
        }
        /** Nothing to do if the node is not focused. */
        Inactive -> true
    }

/**
 * This function grants focus to this node. Note: This is a private function that just changes the
 * state of this node and does not affect any other nodes in the hierarchy.
 */
private fun FocusTargetNode.grantFocus(): Boolean {
    // When we grant focus to this node, we need to observe changes to the canFocus property.
    // If canFocus is set to false, we need to clear focus.
    observeReads { fetchFocusProperties() }
    // No Focused Children, or we don't want to propagate focus to children.
    when (focusState) {
        Inactive,
        ActiveParent -> focusState = Active
        Active,
        Captured -> {
            /* Already focused. */
        }
    }
    return true
}

/** This function clears any focus from the focused child. */
private fun FocusTargetNode.clearChildFocus(
    forced: Boolean = false,
    refreshFocusEvents: Boolean = true
): Boolean = activeChild?.clearFocus(forced, refreshFocusEvents) ?: true

/**
 * Focusable children of this [focus node][FocusTargetNode] can use this function to request focus.
 *
 * @param childNode: The node that is requesting focus.
 * @return true if focus was granted, false otherwise.
 */
private fun FocusTargetNode.requestFocusForChild(childNode: FocusTargetNode): Boolean {

    // Only this node's children can ask for focus.
    if (childNode.nearestAncestor(FocusTarget) != this) {
        error("Non child node cannot request focus.")
    }

    return when (focusState) {
        // If this node is [Active], it can give focus to the requesting child.
        Active -> childNode.grantFocus().also { success -> if (success) focusState = ActiveParent }
        // If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
        // remove focus from the currently focused child and grant it to the requesting child.
        ActiveParent -> {
            requireActiveChild()
            clearChildFocus() && childNode.grantFocus()
        }
        // If this node is not [Active], we must gain focus first before granting it
        // to the requesting child.
        Inactive -> {
            val focusParent = nearestAncestor(FocusTarget)
            when {
                // If this node is the root, request focus from the compose owner.
                focusParent == null && requestFocusForOwner() -> {
                    childNode.grantFocus().also { success ->
                        if (success) focusState = ActiveParent
                    }
                }
                // For non-root nodes, request focus for this node before the child.
                // We request focus even if this is a deactivated node, as we will end up taking
                // focus away and granting it to the child.
                focusParent != null && focusParent.requestFocusForChild(this) -> {
                    requestFocusForChild(childNode).also { success ->
                        // Verify that focus state was granted to the child.
                        // If this child didn't take focus then we can end up in a situation where
                        // a deactivated parent is focused.
                        check(this.focusState == ActiveParent) { "Deactivated node is focused" }
                        if (success) focusParent.dispatchFocusCallbacks()
                    }
                }

                // Could not gain focus, so have no focus to give.
                else -> false
            }
        }
        // If this node is [Captured], decline requests from the children.
        Captured -> false
    }
}

private fun FocusTargetNode.requestFocusForOwner(): Boolean {
    return requireOwner().focusOwner.requestFocusForOwner(null, null)
}

private fun FocusTargetNode.requireActiveChild(): FocusTargetNode {
    return requireNotNull(activeChild) { "ActiveParent with no focused child" }
}

internal enum class CustomDestinationResult {
    None,
    Cancelled,
    Redirected,
    RedirectCancelled
}

internal fun FocusTargetNode.performCustomRequestFocus(
    focusDirection: FocusDirection
): CustomDestinationResult {
    when (focusState) {
        Active,
        Captured -> return None
        ActiveParent -> return requireActiveChild().performCustomClearFocus(focusDirection)
        Inactive -> {
            val focusParent = nearestAncestor(FocusTarget) ?: return None
            return when (focusParent.focusState) {
                Captured -> Cancelled
                ActiveParent -> focusParent.performCustomRequestFocus(focusDirection)
                Active -> focusParent.performCustomEnter(focusDirection)
                Inactive ->
                    focusParent.performCustomRequestFocus(focusDirection).takeUnless { it == None }
                        ?: focusParent.performCustomEnter(focusDirection)
            }
        }
    }
}

internal fun FocusTargetNode.performCustomClearFocus(
    focusDirection: FocusDirection
): CustomDestinationResult =
    when (focusState) {
        Active,
        Inactive -> None
        Captured -> Cancelled
        ActiveParent ->
            requireActiveChild().performCustomClearFocus(focusDirection).takeUnless { it == None }
                ?: performCustomExit(focusDirection)
    }

private fun FocusTargetNode.performCustomEnter(
    focusDirection: FocusDirection
): CustomDestinationResult {
    fetchCustomEnter(focusDirection) {
        if (it === Cancel) return Cancelled
        return if (it.focus()) Redirected else RedirectCancelled
    }
    return None
}

private fun FocusTargetNode.performCustomExit(
    focusDirection: FocusDirection
): CustomDestinationResult {
    fetchCustomExit(focusDirection) {
        if (it === Cancel) return Cancelled
        return if (it.focus()) Redirected else RedirectCancelled
    }
    return None
}
