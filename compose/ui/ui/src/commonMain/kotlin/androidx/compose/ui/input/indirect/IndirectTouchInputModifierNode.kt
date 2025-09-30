/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.ui.input.indirect

import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.DelegatableNode

/**
 * A [androidx.compose.ui.Modifier.Node] that receives [IndirectTouchEvent]s. This modifier node
 * only receives events if it is focused, or if a child is focused. If you are implementing this
 * node, make sure to use this node with a focus modifier (such as focusTarget or focusable), or
 * make this node also delegate to a [androidx.compose.ui.focus.FocusTargetModifierNode].
 */
interface IndirectTouchInputModifierNode : DelegatableNode {

    /**
     * Handles [IndirectTouchEvent]s that are dispatched to the node. A node can only receive
     * [IndirectTouchEvent]s if it is focused, or if a child is focused.
     *
     * @param event The [IndirectTouchEvent] that has been dispatched.
     * @param pass The [PointerEventPass] in which this function is being called.
     */
    fun onIndirectTouchEvent(event: IndirectTouchEvent, pass: PointerEventPass)

    /**
     * Invoked to notify the handler that no more calls to [IndirectTouchInputModifierNode] will be
     * made, until at least new pointers exist. This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to Compose.
     */
    // TODO (jjw): Add support for focus cancel.
    fun onCancelIndirectTouchInput()
}
