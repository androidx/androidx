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

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Create a modifier for testing indirect touch input. For production use, use foundation gestures
 * detectors or make your own Modifier.Node implementing [IndirectTouchInputModifierNode], see
 * [IndirectTouchInputNode] below for details.
 *
 * @param onEvent A callback that is invoked when an indirect touch event is received.
 * @param onCancel A callback that is invoked when the pointer input is cancelled.
 */
internal fun Modifier.onIndirectTouchInput(
    onEvent: (event: IndirectTouchEvent, pass: PointerEventPass) -> Unit,
    onCancel: () -> Unit = {},
): Modifier = this.then(IndirectTouchInputElement(onEvent, onCancel))

internal class IndirectTouchInputElement(
    val onEvent: (IndirectTouchEvent, PointerEventPass) -> Unit,
    val onCancel: () -> Unit,
) : ModifierNodeElement<IndirectTouchInputNode>() {

    override fun create(): IndirectTouchInputNode {
        return IndirectTouchInputNode(onEvent, onCancel)
    }

    override fun update(node: IndirectTouchInputNode) {
        node.onEvent = onEvent
        node.onCancel = onCancel
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indirectTouchInput"
        properties["onEvent"] = onEvent
        properties["onCancel"] = onCancel
    }

    override fun hashCode(): Int {
        var result = onEvent.hashCode()
        result = 31 * result + onCancel.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectTouchInputElement) return false
        if (onEvent !== other.onEvent) return false
        if (onCancel !== other.onCancel) return false
        return true
    }
}

/**
 * A [Modifier.Node] that can be used to test indirect touch events. This is a very simple version
 * that doesn't track state (which you would need for production).
 */
internal class IndirectTouchInputNode(
    var onEvent: (IndirectTouchEvent, PointerEventPass) -> Unit,
    var onCancel: () -> Unit,
) : IndirectTouchInputModifierNode, Modifier.Node() {
    override fun onIndirectTouchEvent(event: IndirectTouchEvent, pass: PointerEventPass) {
        onEvent(event, pass)
    }

    override fun onCancelIndirectTouchInput() {
        onCancel()
    }

    override fun onDetach() {
        onCancel()
        super.onDetach()
    }
}
