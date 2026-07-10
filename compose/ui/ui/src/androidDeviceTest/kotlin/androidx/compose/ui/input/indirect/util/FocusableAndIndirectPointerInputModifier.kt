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
package androidx.compose.ui.input.indirect.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus

/**
 * A custom modifier that combines the behavior of `focusable` and `onIndirectPointerInput` by
 * delegating to custom, public node implementations.
 *
 * This version does not depend on any `internal` classes and is suitable for use in test modules
 * across different packages.
 *
 * @param onEvent Callback to receive [androidx.compose.ui.input.indirect.IndirectPointerEvent]s.
 * @param onCancel Callback for when the indirect pointer input stream is cancelled.
 * @param onFocusChange Callback for when focus changes.
 */
internal fun Modifier.focusableWithIndirectInput(
    onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    onCancel: () -> Unit,
    onFocusChange: (previous: FocusState, current: FocusState) -> Unit,
): Modifier {
    return this then
        FocusableAndIndirectPointerInputElement(
            onEvent = onEvent,
            onCancel = onCancel,
            onFocusChange = onFocusChange,
        )
}

private class FocusableAndIndirectPointerInputElement(
    val onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    val onCancel: () -> Unit,
    val onFocusChange: (previous: FocusState, current: FocusState) -> Unit,
) : ModifierNodeElement<FocusableAndIndirectPointerInputNode>() {

    override fun create(): FocusableAndIndirectPointerInputNode =
        FocusableAndIndirectPointerInputNode(
            onEvent = onEvent,
            onCancel = onCancel,
            onFocusChange = onFocusChange,
        )

    override fun update(node: FocusableAndIndirectPointerInputNode) {
        node.onEvent = onEvent
        node.onCancel = onCancel
        node.onFocusChange = onFocusChange
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusableWithIndirectInput"
        properties["onEvent"] = onEvent
        properties["onCancel"] = onCancel
        properties["onFocusChange"] = onFocusChange
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FocusableAndIndirectPointerInputElement) return false
        if (onEvent !== other.onEvent) return false
        if (onCancel !== other.onCancel) return false
        if (onFocusChange !== other.onFocusChange) return false
        return true
    }

    override fun hashCode(): Int {
        var result = onEvent.hashCode()
        result = 31 * result + onCancel.hashCode()
        result = 31 * result + onFocusChange.hashCode()
        return result
    }
}

// To allow indirect events to work properly with delegation, developers must implement the
// interface at the top of the class (even if they pass along the functions to the delegate).
private class FocusableAndIndirectPointerInputNode(
    var onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    var onCancel: () -> Unit,
    var onFocusChange: (previous: FocusState, current: FocusState) -> Unit,
) : DelegatingNode(), SemanticsModifierNode, IndirectPointerInputModifierNode {
    private var currentOnFocusChange = onFocusChange

    private val focusTargetNode =
        delegate(
            FocusTargetModifierNode(
                onFocusChange = { previous, current ->
                    currentOnFocusChange(previous, current)
                    invalidateSemantics()
                }
            )
        )

    private val focusPropertiesModifierNode =
        delegate(
            object : Modifier.Node(), FocusPropertiesModifierNode {
                override fun applyFocusProperties(focusProperties: FocusProperties) {
                    focusProperties.canFocus = true
                }
            }
        )

    private val indirectPointerInputNode =
        delegate(SimpleIndirectPointerInputNode(onEvent, onCancel))

    // Enables testing semantic APIs (requestFocus(), assertIsFocused(), etc.)
    override fun SemanticsPropertyReceiver.applySemantics() {
        requestFocus { focusTargetNode.requestFocus() }
        focused = focusTargetNode.focusState.isFocused
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        indirectPointerInputNode.onEvent(event, pass)
    }

    override fun onCancelIndirectPointerInput() {
        indirectPointerInputNode.onCancel()
    }
}

private class SimpleIndirectPointerInputNode(
    var onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    var onCancel: () -> Unit,
) : Modifier.Node(), IndirectPointerInputModifierNode {
    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        onEvent(event, pass)
    }

    override fun onCancelIndirectPointerInput() {
        onCancel()
    }
}
