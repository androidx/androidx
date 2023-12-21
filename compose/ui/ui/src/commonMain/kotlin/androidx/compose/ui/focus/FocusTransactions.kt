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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.Nodes.FocusTarget
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.observeReads

/**
 * Request focus for this node.
 *
 * In Compose, the parent [FocusNode][FocusTargetNode] controls focus for its focusable
 * children. Calling this function will send a focus request to this
 * [FocusNode][FocusTargetNode]'s parent [FocusNode][FocusTargetNode].
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun FocusTargetNode.requestFocus(): Boolean {
    return when (performCustomRequestFocus(Enter)) {
        None -> performRequestFocus()
        Redirected -> true
        Cancelled, RedirectCancelled -> false
    }
}

/**
 * This function performs the request focus action.
 *
 * Note: Do not call this directly, consider using [requestFocus], which will check if any
 * custom focus [enter][FocusProperties.enter] and [exit][FocusProperties.exit]
 * [properties][FocusProperties] have been specified.
 */
internal fun FocusTargetNode.performRequestFocus(): Boolean {
    when (focusStateImpl) {
        Active, Captured -> {
            // There is no change in focus state, but we send a focus event to notify the user
            // that the focus request is completed.
            refreshFocusEventNodes()
            return true
        }
        ActiveParent -> return (clearChildFocus() && grantFocus()).also { success ->
            if (success) refreshFocusEventNodes()
        }
        Inactive -> return nearestAncestor(FocusTarget)
            ?.requestFocusForChild(this)
            ?: (requestFocusForOwner() && grantFocus()).also { success ->
                if (success) refreshFocusEventNodes()
            }
    }
}

/**
 * Deny requests to clear focus.
 *
 * This is used when a component wants to hold onto focus (eg. A phone number field with an
 * invalid number.
 *
 * @return true if the focus was successfully captured. False otherwise.
 */
internal fun FocusTargetNode.captureFocus() = when (focusStateImpl) {
    Active -> {
        focusStateImpl = Captured
        refreshFocusEventNodes()
        true
    }
    Captured -> true
    ActiveParent, Inactive -> false
}

/**
 * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
 * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
 * nodes from requesting focus.
 *
 * @return true if the captured focus was released. False Otherwise.
 */
internal fun FocusTargetNode.freeFocus() = when (focusStateImpl) {
    Captured -> {
        focusStateImpl = Active
        refreshFocusEventNodes()
        true
    }
    Active -> true
    ActiveParent, Inactive -> false
}

/**
 * This function clears focus from this node.
 *
 * Note: This function should only be called by a parent [focus node][FocusTargetNode] to
 * clear focus from one of its child [focus node][FocusTargetNode]s. It does not change the
 * state of the parent.
 */
internal fun FocusTargetNode.clearFocus(
    forced: Boolean = false,
    refreshFocusEvents: Boolean
): Boolean = when (focusStateImpl) {
    Active -> {
        focusStateImpl = Inactive
        if (refreshFocusEvents) refreshFocusEventNodes()
        true
    }
    /**
     * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
     * first, before clearing focus from this node.
     */
    ActiveParent -> if (clearChildFocus(forced, refreshFocusEvents)) {
        focusStateImpl = Inactive
        if (refreshFocusEvents) refreshFocusEventNodes()
        true
    } else {
        false
    }

    /**
     * If the node is [Captured], deny requests to clear focus, except for a forced clear.
     */
    Captured -> {
        if (forced) {
            focusStateImpl = Inactive
            if (refreshFocusEvents) refreshFocusEventNodes()
        }
        forced
    }
    /**
     * Nothing to do if the node is not focused.
     */
    Inactive -> true
}

/**
 * This function grants focus to this node.
 * Note: This is a private function that just changes the state of this node and does not affect any
 * other nodes in the hierarchy.
 */
private fun FocusTargetNode.grantFocus(): Boolean {
    // When we grant focus to this node, we need to observe changes to the canFocus property.
    // If canFocus is set to false, we need to clear focus.
    observeReads { fetchFocusProperties() }
    // No Focused Children, or we don't want to propagate focus to children.
    when (focusStateImpl) {
        Inactive, ActiveParent -> focusStateImpl = Active
        Active, Captured -> { /* Already focused. */ }
    }
    return true
}

/** This function clears any focus from the focused child. */
private fun FocusTargetNode.clearChildFocus(
    forced: Boolean = false,
    refreshFocusEvents: Boolean = true
): Boolean = activeChild?.clearFocus(forced, refreshFocusEvents) ?: true

/**
 * Focusable children of this [focus node][FocusTargetNode] can use this function to request
 * focus.
 *
 * @param childNode: The node that is requesting focus.
 * @return true if focus was granted, false otherwise.
 */
private fun FocusTargetNode.requestFocusForChild(
    childNode: FocusTargetNode
): Boolean {

    // Only this node's children can ask for focus.
    if (childNode.nearestAncestor(FocusTarget) != this) {
        error("Non child node cannot request focus.")
    }

    return when (focusStateImpl) {
        // If this node is [Active], it can give focus to the requesting child.
        Active -> childNode.grantFocus().also { success ->
            if (success) {
                focusStateImpl = ActiveParent
                childNode.refreshFocusEventNodes()
                refreshFocusEventNodes()
            }
        }
        // If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
        // remove focus from the currently focused child and grant it to the requesting child.
        ActiveParent -> {
            checkNotNull(activeChild)
            (clearChildFocus() && childNode.grantFocus()).also { success ->
                if (success) childNode.refreshFocusEventNodes()
            }
        }
        // If this node is not [Active], we must gain focus first before granting it
        // to the requesting child.
        Inactive -> {
            val focusParent = nearestAncestor(FocusTarget)
            when {
                // If this node is the root, request focus from the compose owner.
                focusParent == null && requestFocusForOwner() -> {
                    focusStateImpl = Active
                    refreshFocusEventNodes()
                    requestFocusForChild(childNode)
                }
                // For non-root nodes, request focus for this node before the child.
                // We request focus even if this is a deactivated node, as we will end up taking
                // focus away and granting it to the child.
                focusParent != null && focusParent.requestFocusForChild(this) -> {
                    requestFocusForChild(childNode).also {
                        // Verify that focus state was granted to the child.
                        // If this child didn't take focus then we can end up in a situation where
                        // a deactivated parent is focused.
                        check(this.focusState == ActiveParent)
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
    return coordinator?.layoutNode?.owner?.requestFocus() ?: error("Owner not initialized.")
}

internal enum class CustomDestinationResult { None, Cancelled, Redirected, RedirectCancelled }

internal fun FocusTargetNode.performCustomRequestFocus(
    focusDirection: FocusDirection
): CustomDestinationResult {
    when (focusStateImpl) {
        Active, Captured -> return None
        ActiveParent ->
            return checkNotNull(activeChild).performCustomClearFocus(focusDirection)
        Inactive -> {
            val focusParent = nearestAncestor(FocusTarget) ?: return None
            return when (focusParent.focusStateImpl) {
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
): CustomDestinationResult = when (focusStateImpl) {
    Active, Inactive -> None
    Captured -> Cancelled
    ActiveParent ->
        checkNotNull(activeChild).performCustomClearFocus(focusDirection).takeUnless { it == None }
            ?: performCustomExit(focusDirection)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun FocusTargetNode.performCustomEnter(
    focusDirection: FocusDirection
): CustomDestinationResult {
    fetchCustomEnter(focusDirection) {
        if (it === Cancel) return Cancelled
        return if (it.focus()) Redirected else RedirectCancelled
    }
    return None
}

@OptIn(ExperimentalComposeUiApi::class)
private fun FocusTargetNode.performCustomExit(
    focusDirection: FocusDirection
): CustomDestinationResult {
    fetchCustomExit(focusDirection) {
        if (it === Cancel) return Cancelled
        return if (it.focus()) Redirected else RedirectCancelled
    }
    return None
}
