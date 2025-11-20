/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitSelfAndChildren

/**
 * Implement this interface to create a modifier node that can be used to request changes in the
 * focus state of a [FocusTargetNode] down the hierarchy.
 */
interface FocusRequesterModifierNode : DelegatableNode

/**
 * Use this function to request focus. If the system grants focus to a component associated with
 * this [FocusRequester], its [onFocusChanged] modifiers will receive a [FocusState] object where
 * [FocusState.isFocused] is true.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 */
fun FocusRequesterModifierNode.requestFocus(): Boolean {
    visitSelfAndChildren(Nodes.FocusTarget) { focusTarget ->
        @OptIn(ExperimentalComposeUiApi::class)
        return if (ComposeUiFlags.isRequestFocusOnNonFocusableFocusTargetEnabled) {
            focusTarget.requestFocus()
        } else if (focusTarget.fetchFocusProperties().canFocus) {
            focusTarget.requestFocus()
        } else {
            focusTarget.findChildCorrespondingToFocusEnter(Enter) { it.requestFocus() }
        }
    }
    return false
}

/**
 * Deny requests to clear focus.
 *
 * Use this function to send a request to capture focus. If a component captures focus, it will send
 * a [FocusState] object to its associated [onFocusChanged] modifiers where
 * [FocusState.isCaptured]() == true.
 *
 * When a component is in a Captured state, all focus requests from other components are declined.
 *
 * @return true if the focus was successfully captured by one of the [focus][focusTarget] modifiers
 *   associated with this [FocusRequester]. False otherwise.
 * @sample androidx.compose.ui.samples.CaptureFocusSample
 */
fun FocusRequesterModifierNode.captureFocus(): Boolean {
    visitSelfAndChildren(Nodes.FocusTarget) {
        if (it.captureFocus()) {
            return true
        }
    }
    return false
}

/**
 * Use this function to send a request to free focus when one of the components associated with this
 * [FocusRequester] is in a Captured state. If a component frees focus, it will send a [FocusState]
 * object to its associated [onFocusChanged] modifiers where [FocusState.isCaptured]() == false.
 *
 * When a component is in a Captured state, all focus requests from other components are declined. .
 *
 * @return true if the captured focus was successfully released. i.e. At the end of this operation,
 *   one of the components associated with this [focusRequester] freed focus.
 * @sample androidx.compose.ui.samples.CaptureFocusSample
 */
fun FocusRequesterModifierNode.freeFocus(): Boolean {
    visitSelfAndChildren(Nodes.FocusTarget) { if (it.freeFocus()) return true }
    return false
}

/**
 * Use this function to request the focus target to save a reference to the currently focused child
 * in its saved instance state. After calling this, focus can be restored to the saved child by
 * making a call to [restoreFocusedChild].
 *
 * @return true if the focus target associated with this node has a focused child and we
 *   successfully saved a reference to it.
 */
fun FocusRequesterModifierNode.saveFocusedChild(): Boolean {
    visitSelfAndChildren(Nodes.FocusTarget) {
        if (it.saveFocusedChild()) {
            return true
        }
    }
    return false
}

/**
 * Use this function to restore focus to one of the children of the node pointed to by this
 * [FocusRequester]. This restores focus to a previously focused child that was saved by using
 * [saveFocusedChild].
 *
 * @return true if we successfully restored focus to one of the children of the [focusTarget]
 *   associated with this node.
 */
fun FocusRequesterModifierNode.restoreFocusedChild(): Boolean {
    visitSelfAndChildren(Nodes.FocusTarget) { if (it.restoreFocusedChild()) return true }
    return false
}

// TODO: Consider making this public, or make findActiveFocusTargetModifierNode public.
internal fun FocusRequesterModifierNode.pinFocusedChild(): PinnedHandle? {
    visitSelfAndChildren(Nodes.FocusTarget) {
        it.pinFocusedChild()?.let {
            return it
        }
    }
    return null
}
