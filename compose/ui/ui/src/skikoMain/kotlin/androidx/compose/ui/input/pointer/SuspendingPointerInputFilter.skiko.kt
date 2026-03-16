/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * Calls [onEvent] when a [PointerEvent] with type [eventType] is reported to the
 * specified input [pass].
 *
 * Note that this function is experimental, and can be replaced in the future
 * by high-level functions similar to `clickable`, `hoverable`, etc.
 */
@ExperimentalComposeUiApi
fun Modifier.onPointerEvent(
    eventType: PointerEventType,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
): Modifier = this.then(
    OnPointerEventModifierElement(
        eventType = eventType,
        pass = pass,
        onEvent = onEvent
    )
)

private class OnPointerEventModifierElement(
    private val eventType: PointerEventType,
    private val pass: PointerEventPass,
    private val onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) : ModifierNodeElement<OnPointerEventModifierNode>() {
    override fun create() = OnPointerEventModifierNode(
        eventType = eventType,
        pass = pass,
        onEvent = onEvent
    )

    override fun update(node: OnPointerEventModifierNode) = node.update(
        eventType = eventType,
        pass = pass,
        onEvent = onEvent
    )

    override fun InspectorInfo.inspectableProperties() {
        name = "onPointerEvent"
        properties["eventType"] = eventType
        properties["pass"] = pass
        properties["onEvent"] = onEvent
    }

    override fun hashCode(): Int {
        var result = eventType.hashCode()
        result = 31 * result + pass.hashCode()
        result = 31 * result + onEvent.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnPointerEventModifierElement) return false
        return eventType == other.eventType &&
            pass == other.pass &&
            onEvent === other.onEvent
    }
}

private class OnPointerEventModifierNode(
    private var eventType: PointerEventType,
    private var pass: PointerEventPass,
    private var onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) : DelegatingNode() {

    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode(
            pointerInputEventHandler = {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        if (event.type == eventType) {
                            onEvent(event)
                        }
                    }
                }
            }
        )
    )

    fun update(
        eventType: PointerEventType,
        pass: PointerEventPass,
        onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
    ) {
        var pointerInputNodeNeedsReset = false

        this.eventType = eventType
        this.onEvent = onEvent

        if (this.pass != pass) {
            this.pass = pass
            pointerInputNodeNeedsReset = true
        }

        if (pointerInputNodeNeedsReset) {
            pointerInputNode.resetPointerInputHandler()
        }
    }
}