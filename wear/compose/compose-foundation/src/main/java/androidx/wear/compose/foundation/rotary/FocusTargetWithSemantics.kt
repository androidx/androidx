/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus

/**
 * FocusTarget modifier with Semantics node. Uses underlying focusTarget node with additional
 * semantics node.
 */
@Stable internal fun Modifier.focusTargetWithSemantics() = this then FocusTargetWithSemanticsElement

private object FocusTargetWithSemanticsElement :
    ModifierNodeElement<FocusTargetWithSemanticsNode>() {

    override fun create() = FocusTargetWithSemanticsNode()

    override fun update(node: FocusTargetWithSemanticsNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "focusTargetWithSemanticsElement"
    }

    override fun hashCode() = "focusTargetWithSemanticsElement".hashCode()

    override fun equals(other: Any?) = other === this
}

internal class FocusTargetWithSemanticsNode() : DelegatingNode(), SemanticsModifierNode {

    private val focusTargetNode =
        delegate(FocusTargetModifierNode(onFocusChange = ::onFocusStateChange))

    private var requestFocus: (() -> Boolean)? = null

    private fun onFocusStateChange(previousState: FocusState, currentState: FocusState) {
        if (!isAttached) return
        if (currentState.isFocused != previousState.isFocused) invalidateSemantics()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        focused = focusTargetNode.focusState.isFocused
        if (requestFocus == null) {
            requestFocus = { focusTargetNode.requestFocus() }
        }
        requestFocus(action = requestFocus)
    }
}
