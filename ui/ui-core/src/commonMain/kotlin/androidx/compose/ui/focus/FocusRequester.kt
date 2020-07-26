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

import androidx.compose.ui.node.ModifiedFocusRequesterNode

private val focusRequesterNotInitialized = "FocusRequester is not initialized. One reason for " +
        "this is that you requesting focus changes during composition. Focus requests should " +
        "not be made during composition, but should be made in response to some event."

/**
 * The [FocusRequester] is used in conjunction with [Modifier.focusRequester][focusRequester] to
 * send requests for focus state change.
 *
 * @see focusRequester
 */
@ExperimentalFocus
class FocusRequester {

    // TODO(b/161183156): Add support for multiple focus requester nodes.
    //  ie. the same focus requester can be used in multiple focusRequesterModifiers.
    internal var focusRequesterNode: ModifiedFocusRequesterNode? = null

    /**
     * Use this function to request focus. If the system grants focus to the component associated
     * with this [FocusRequester], its [state][FocusState2] will be set to
     * [Active][FocusState2.Active].
     */
    fun requestFocus() {
        val focusRequesterNode = focusRequesterNode
        checkNotNull(focusRequesterNode) { focusRequesterNotInitialized }
        focusRequesterNode.findFocusNode()?.requestFocus()
    }

    /**
     * Deny requests to clear focus.
     *
     * Use this function to send a request to capture the focus. If a component is captured, it's
     * [state][FocusState2] will be set to [Captured][FocusState2.Captured]. When a
     * component is in this state, it holds onto focus until [freeFocus] is called. When a
     * component is in the [Captured][FocusState2.Captured] state, all focus requests from
     * other components are declined.
     *
     * @return true if the focus was successfully captured. false otherwise.
     */
    fun captureFocus(): Boolean {
        val focusRequesterNode = focusRequesterNode
        checkNotNull(focusRequesterNode) { focusRequesterNotInitialized }
        return focusRequesterNode.findFocusNode()?.captureFocus() ?: false
    }

    /**
     * Use this function to send a request to release focus when the component is in a
     * [Captured][FocusState2.Captured] state.
     *
     * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
     * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
     * nodes from requesting focus.
     *
     * @return true if the focus was successfully released. false otherwise.
     */
    fun freeFocus(): Boolean {
        val focusRequesterNode = focusRequesterNode
        checkNotNull(focusRequesterNode) { focusRequesterNotInitialized }
        return focusRequesterNode.findFocusNode()?.freeFocus() ?: false
    }
}
