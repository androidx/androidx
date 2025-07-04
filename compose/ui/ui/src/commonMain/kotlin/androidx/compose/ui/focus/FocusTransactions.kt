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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Redirect
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitAncestors

/**
 * This function performs the request focus action.
 *
 * Note: Do not call this directly, consider using [requestFocus], which will check if any custom
 * focus [enter][FocusProperties.onEnter] and [exit][FocusProperties.onExit]
 * [properties][FocusProperties] have been specified.
 */
internal fun FocusTargetNode.performRequestFocus(): Boolean {
    val focusOwner = requireOwner().focusOwner
    val previousActiveNode = focusOwner.activeFocusTargetNode
    val previousFocusState = focusState
    if (previousActiveNode === this) {
        // Focus events should be sent again if focus is requested for an already focused node
        dispatchFocusCallbacks(previousFocusState, previousFocusState)
        return true
    }

    // Request owner focus if it doesn't already have focus
    if (previousActiveNode == null && !requestOwnerFocus()) {
        return false // Don't grant focus if requesting owner focus failed
    }

    // Find ancestor target and event nodes of the previous active target node
    var previousAncestorTargetNodes: MutableVector<FocusTargetNode>? = null
    if (previousActiveNode != null) {
        previousAncestorTargetNodes = mutableVectorOf()
        previousActiveNode.visitAncestors(Nodes.FocusTarget) { previousAncestorTargetNodes.add(it) }
    }

    // Diff the previous ancestor nodes with the ancestors of the new active target node.
    // We also check if the previous active node is an ancestor of the new active node, in which
    // case we don't need to clear focus from it.
    var shouldClearFocusFromPreviousActiveNode = true
    val ancestorTargetNodes = mutableVectorOf<FocusTargetNode>()
    visitAncestors(Nodes.FocusTarget) {
        val removed = previousAncestorTargetNodes?.remove(it)
        if (removed == null || !removed) {
            ancestorTargetNodes.add(it)
        }
        if (it === previousActiveNode) shouldClearFocusFromPreviousActiveNode = false
    }

    if (shouldClearFocusFromPreviousActiveNode) {
        if (previousActiveNode?.clearFocus(refreshFocusEvents = true) == false) {
            return false // Don't grant focus if clearing focus from the previous node was rejected
        }
    }

    grantFocus()

    // Notify ancestor target nodes of the previous active node that are no longer ActiveParent
    // The ancestors are traversed in the reversed order to dispatch events top->down
    previousAncestorTargetNodes?.forEachReversed {
        // Check if focus was cleared or redirected in a previous focus change callback
        if (focusOwner.activeFocusTargetNode !== this) {
            // The focus request was redirected or cancelled in a previous focus change callback
            return false
        }
        it.dispatchFocusCallbacks(ActiveParent, Inactive)
    }

    // Notify ancestor target nodes of the new active node that become ActiveParent
    // The ancestors are traversed in the reversed order to dispatch events top->down
    ancestorTargetNodes.forEachReversed {
        // Check if focus was cleared or redirected in a previous focus change callback
        if (focusOwner.activeFocusTargetNode !== this) {
            // The focus request was redirected or cancelled in a previous focus change callback
            return false
        }
        it.dispatchFocusCallbacks(
            previousState = if (it === previousActiveNode) Active else Inactive,
            newState = ActiveParent,
        )
    }

    // Check if focus was cleared or redirected in a previous focus change callback
    if (focusOwner.activeFocusTargetNode !== this) {
        // The focus request was redirected or cancelled in a previous focus change callback
        return false
    }

    // Send events to the new active node
    dispatchFocusCallbacks(previousFocusState, Active)

    // Check if focus was cleared or redirected in a previous focus change callback
    if (focusOwner.activeFocusTargetNode !== this) {
        // The focus request was redirected or cancelled in a previous focus change callback
        return false
    }

    @OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
    if (ComposeUiFlags.isViewFocusFixEnabled && requireLayoutNode().getInteropView() == null) {
        // This isn't an AndroidView, so we should be focused on this ComposeView
        requestOwnerFocus(FocusDirection.Next, null)
    }

    return true
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
    when (focusState) {
        Active -> {
            requireOwner().focusOwner.isFocusCaptured = true
            dispatchFocusCallbacks(Active, Captured)
            true
        }
        Captured -> true
        ActiveParent,
        Inactive -> false
    }

/**
 * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
 * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other nodes
 * from requesting focus.
 *
 * @return true if the captured focus was released. False Otherwise.
 */
internal fun FocusTargetNode.freeFocus() =
    when (focusState) {
        Captured -> {
            requireOwner().focusOwner.isFocusCaptured = false
            dispatchFocusCallbacks(previousState = Captured, newState = Active)
            true
        }
        Active -> true
        ActiveParent,
        Inactive -> false
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
    refreshFocusEvents: Boolean,
): Boolean =
    when (focusState) {
        Active -> {
            requireOwner().focusOwner.activeFocusTargetNode = null
            if (refreshFocusEvents) {
                dispatchFocusCallbacks(previousState = Active, newState = Inactive)
            }
            true
        }
        /**
         * If the node is [ActiveParent], we need to clear focus from the [Active] descendant first,
         * before clearing focus from this node.
         */
        ActiveParent ->
            if (clearChildFocus(forced, refreshFocusEvents)) {
                if (refreshFocusEvents) {
                    dispatchFocusCallbacks(previousState = ActiveParent, newState = Inactive)
                }
                true
            } else {
                false
            }

        /** If the node is [Captured], deny requests to clear focus, except for a forced clear. */
        Captured -> {
            if (forced) {
                requireOwner().focusOwner.activeFocusTargetNode = null
                if (refreshFocusEvents) {
                    dispatchFocusCallbacks(previousState = Captured, newState = Inactive)
                }
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
        ActiveParent -> {
            requireOwner().focusOwner.activeFocusTargetNode = this
        }

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
    refreshFocusEvents: Boolean = true,
): Boolean = activeChild?.clearFocus(forced, refreshFocusEvents) ?: true

private fun FocusTargetNode.requestOwnerFocus(
    focusDirection: FocusDirection? = null,
    previouslyFocusedRect: Rect? = null,
): Boolean = requireOwner().focusOwner.requestOwnerFocus(focusDirection, previouslyFocusedRect)

private fun FocusTargetNode.requireActiveChild(): FocusTargetNode {
    return requireNotNull(activeChild) { "ActiveParent with no focused child" }
}

internal enum class CustomDestinationResult {
    None,
    Cancelled,
    Redirected,
    RedirectCancelled,
}

internal fun FocusTargetNode.performCustomRequestFocus(
    focusDirection: FocusDirection
): CustomDestinationResult {
    when (focusState) {
        Active,
        Captured -> return None
        ActiveParent -> return requireActiveChild().performCustomClearFocus(focusDirection)
        Inactive -> {
            val focusParent = nearestAncestor(Nodes.FocusTarget) ?: return None
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
        if (it === Cancel) return Cancelled else if (it === Redirect) return Redirected
        return if (it.requestFocus()) Redirected else RedirectCancelled
    }
    return None
}

private fun FocusTargetNode.performCustomExit(
    focusDirection: FocusDirection
): CustomDestinationResult {
    fetchCustomExit(focusDirection) {
        if (it === Cancel) return Cancelled else if (it === Redirect) return Redirected
        return if (it.requestFocus()) Redirected else RedirectCancelled
    }
    return None
}
