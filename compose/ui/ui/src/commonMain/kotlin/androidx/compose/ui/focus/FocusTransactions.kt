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

import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode

/**
 * Request focus for this node.
 *
 * In Compose, the parent [FocusNode][ModifiedFocusNode] controls focus for its focusable
 * children. Calling this function will send a focus request to this
 * [FocusNode][ModifiedFocusNode]'s parent [FocusNode][ModifiedFocusNode].
 */
internal fun ModifiedFocusNode.requestFocus() {
    when (focusState) {
        Active, Captured, Deactivated, DeactivatedParent -> {
            // There is no change in focus state, but we send a focus event to notify the user
            // that the focus request is completed.
            sendOnFocusEvent(focusState)
        }
        ActiveParent -> if (clearChildFocus()) grantFocus()

        Inactive -> {
            val focusParent = findParentFocusNode()
                if (focusParent != null) {
                    focusParent.requestFocusForChild(this)
                } else if (requestFocusForOwner()) {
                    grantFocus()
                }
        }
    }
}

/**
 * Activate this node so that it can be focused.
 *
 * Deactivated nodes are excluded from focus search, and reject requests to gain focus.
 * Calling this function activates a deactivated node.
 */
internal fun ModifiedFocusNode.activateNode() {
    when (focusState) {
        ActiveParent, Active, Captured, Inactive -> { }
        Deactivated -> focusState = Inactive
        DeactivatedParent -> focusState = ActiveParent
    }
}

/**
 * Deactivate this node so that it can't be focused.
 *
 * Deactivated nodes are excluded from focus search.
 */
internal fun ModifiedFocusNode.deactivateNode() {
    when (focusState) {
        ActiveParent -> focusState = DeactivatedParent
        Active, Captured -> {
            layoutNode.owner?.focusManager?.clearFocus(force = true)
            focusState = Deactivated
        }
        Inactive -> focusState = Deactivated
        Deactivated, DeactivatedParent -> { }
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
internal fun ModifiedFocusNode.captureFocus() = when (focusState) {
    Active -> {
        focusState = Captured
        true
    }
    Captured -> true
    ActiveParent, Deactivated, DeactivatedParent, Inactive -> false
}

/**
 * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
 * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
 * nodes from requesting focus.
 *
 * @return true if the captured focus was released. False Otherwise.
 */
internal fun ModifiedFocusNode.freeFocus() = when (focusState) {
    Captured -> {
        focusState = Active
        true
    }
    Active -> true
    ActiveParent, Deactivated, DeactivatedParent, Inactive -> false
}

/**
 * This function clears focus from this node.
 *
 * Note: This function should only be called by a parent [focus node][ModifiedFocusNode] to
 * clear focus from one of its child [focus node][ModifiedFocusNode]s. It does not change the
 * state of the parent.
 */
internal fun ModifiedFocusNode.clearFocus(forcedClear: Boolean = false): Boolean {
    return when (focusState) {
        Active -> {
            focusState = Inactive
            true
        }
        /**
         * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
         * first, before clearing focus from this node.
         */
        ActiveParent -> if (clearChildFocus()) {
            focusState = Inactive
            true
        } else {
            false
        }
        /**
         * If the node is [DeactivatedParent], we need to clear focus from the [Active] descendant
         * first, before clearing focus from this node.
         */
        DeactivatedParent -> if (clearChildFocus()) {
            focusState = Deactivated
            true
        } else {
            false
        }

        /**
         * If the node is [Captured], deny requests to clear focus, except for a forced clear.
         */
        Captured -> {
            if (forcedClear) {
                focusState = Inactive
            }
            forcedClear
        }
        /**
         * Nothing to do if the node is not focused.
         */
        Inactive, Deactivated -> true
    }
}

// This function grants focus to this node.
// Note: This is a private function that just changes the state of this node and does not affect any
// other nodes in the hierarchy.
private fun ModifiedFocusNode.grantFocus() {
    // No Focused Children, or we don't want to propagate focus to children.
    focusState = when (focusState) {
        Inactive, Active, ActiveParent -> Active
        Captured -> Captured
        Deactivated, DeactivatedParent -> error("Granting focus to a deactivated node.")
    }
}

// This function grants focus to the specified child.
// Note: This is a private function and should only be called by a parent to grant focus to one of
// its child. It does not affect any other nodes in the hierarchy.
private fun ModifiedFocusNode.grantFocusToChild(childNode: ModifiedFocusNode): Boolean {
    childNode.grantFocus()
    focusedChild = childNode
    return true
}

// This function clears any focus from the focused child.
private fun ModifiedFocusNode.clearChildFocus(): Boolean {
    return if (requireNotNull(focusedChild).clearFocus()) {
        focusedChild = null
        true
    } else {
        false
    }
}

/**
 * Focusable children of this [focus node][ModifiedFocusNode] can use this function to request
 * focus.
 *
 * @param childNode: The node that is requesting focus.
 * @return true if focus was granted, false otherwise.
 */
private fun ModifiedFocusNode.requestFocusForChild(childNode: ModifiedFocusNode): Boolean {

    // Only this node's children can ask for focus.
    if (!focusableChildren(excludeDeactivated = false).contains(childNode)) {
        error("Non child node cannot request focus.")
    }

   return when (focusState) {
        /**
         * If this node is [Active], it can give focus to the requesting child.
         */
        Active -> {
            focusState = ActiveParent
            grantFocusToChild(childNode)
        }
        /**
         * If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
         * remove focus from the currently focused child and grant it to the requesting child.
         */
        ActiveParent -> if (clearChildFocus()) grantFocusToChild(childNode) else false

        DeactivatedParent -> when {
            // DeactivatedParent && NoFocusChild is used to indicate an intermediate state where
            // this parent requested focus so that it can transfer it to a child.
            focusedChild == null -> grantFocusToChild(childNode)
            clearChildFocus() -> grantFocusToChild(childNode)
            else -> false
        }
        /**
         * If this node is not [Active], we must gain focus first before granting it
         * to the requesting child.
         */
        Inactive -> {
            val focusParent = findParentFocusNode()
            when {
                // If this node is the root, request focus from the compose owner.
                focusParent == null && requestFocusForOwner() -> {
                    focusState = Active
                    requestFocusForChild(childNode)
                }
                // For non-root nodes, request focus for this node before the child.
                focusParent != null && focusParent.requestFocusForChild(this) ->
                    requestFocusForChild(childNode)

                // Could not gain focus, so have no focus to give.
                else -> false
            }
        }
        /**
         * If this node is [Captured], decline requests from the children.
         */
        Captured -> false
        /**
         * If this node is [Deactivated], send a requestFocusForChild to its parent to attempt to
         * change its state to [DeactivatedParent] before granting focus to the child.
         */
        Deactivated -> {
            activateNode()
            val childGrantedFocus = requestFocusForChild(childNode)
            deactivateNode()
            childGrantedFocus
        }
    }
}

private fun ModifiedFocusNode.requestFocusForOwner(): Boolean {
    return layoutNode.owner?.requestFocus() ?: error("Owner not initialized.")
}
