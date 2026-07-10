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
 * Create a modifier for testing indirect pointer input. For production use, use foundation gestures
 * detectors or make your own Modifier.Node implementing [IndirectPointerInputModifierNode], see
 * [IndirectPointerInputNode] below for details.
 *
 * @param onEvent A callback that is invoked when an indirect pointer event is received.
 * @param onCancel A callback that is invoked when the pointer input is cancelled.
 */
internal fun Modifier.onIndirectPointerInput(
    onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    onCancel: () -> Unit = {},
): Modifier = this.then(IndirectPointerInputElement(onEvent, onCancel))

internal class IndirectPointerInputElement(
    val onEvent: (IndirectPointerEvent, PointerEventPass) -> Unit,
    val onCancel: () -> Unit,
) : ModifierNodeElement<IndirectPointerInputNode>() {

    override fun create(): IndirectPointerInputNode {
        return IndirectPointerInputNode(onEvent, onCancel)
    }

    override fun update(node: IndirectPointerInputNode) {
        node.onEvent = onEvent
        node.onCancel = onCancel
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indirectPointerInput"
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
        if (other !is IndirectPointerInputElement) return false
        if (onEvent !== other.onEvent) return false
        if (onCancel !== other.onCancel) return false
        return true
    }
}

/**
 * A [Modifier.Node] that can be used to test indirect pointer events. This is a very simple version
 * that doesn't track state (which you would need for production).
 */
internal class IndirectPointerInputNode(
    var onEvent: (IndirectPointerEvent, PointerEventPass) -> Unit,
    var onCancel: () -> Unit,
) : IndirectPointerInputModifierNode, Modifier.Node() {
    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        onEvent(event, pass)
    }

    override fun onCancelIndirectPointerInput() {
        onCancel()
    }
}
