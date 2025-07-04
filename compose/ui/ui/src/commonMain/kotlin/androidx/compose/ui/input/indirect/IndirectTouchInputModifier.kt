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

import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will allow
 * it to handle [IndirectTouchEvent]s, if it (or one of its children) is focused.
 *
 * @param onIndirectTouchEvent This callback is invoked when a user interacts with a touch input
 *   device that is not associated with a touchscreen. While implementing this callback, return true
 *   to stop propagation of this event. If you return false, the event will be sent to this
 *   [IndirectTouchEvent]'s parent.
 * @return true if the event is consumed, false otherwise.
 */
@ExperimentalIndirectTouchTypeApi
fun Modifier.onIndirectTouchEvent(onIndirectTouchEvent: (IndirectTouchEvent) -> Boolean): Modifier =
    this then
        IndirectTouchInputElement(
            onIndirectTouchEvent = onIndirectTouchEvent,
            onPreIndirectTouchEvent = null,
        )

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will allow
 * it to intercept [IndirectTouchEvent]s before a focused child receives it in
 * [onIndirectTouchEvent], if it (or one of its children) is focused.
 *
 * @param onPreIndirectTouchEvent This callback is invoked when a user interacts with a touch input
 *   device that is not associated with a touchscreen.s It gives ancestors of a focused component
 *   the chance to intercept an [IndirectTouchEvent]. Return true to indicate that you consumed the
 *   event and want to stop propagation of this event.
 * @return true if the event is consumed, false otherwise.
 */
@ExperimentalIndirectTouchTypeApi
fun Modifier.onPreIndirectTouchEvent(
    onPreIndirectTouchEvent: (IndirectTouchEvent) -> Boolean
): Modifier =
    this then
        IndirectTouchInputElement(
            onIndirectTouchEvent = null,
            onPreIndirectTouchEvent = onPreIndirectTouchEvent,
        )

@ExperimentalIndirectTouchTypeApi
private class IndirectTouchInputElement(
    val onIndirectTouchEvent: ((IndirectTouchEvent) -> Boolean)?,
    val onPreIndirectTouchEvent: ((IndirectTouchEvent) -> Boolean)?,
) : ModifierNodeElement<IndirectTouchInputNode>() {
    override fun create() =
        IndirectTouchInputNode(onEvent = onIndirectTouchEvent, onPreEvent = onPreIndirectTouchEvent)

    override fun update(node: IndirectTouchInputNode) {
        node.onEvent = onIndirectTouchEvent
        node.onPreEvent = onPreIndirectTouchEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        onIndirectTouchEvent?.let {
            name = "onIndirectTouchEvent"
            properties["onIndirectTouchEvent"] = it
        }
        onPreIndirectTouchEvent?.let {
            name = "onPreIndirectTouchEvent"
            properties["onPreIndirectTouchEvent"] = it
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectTouchInputElement) return false

        if (onIndirectTouchEvent !== other.onIndirectTouchEvent) return false
        if (onPreIndirectTouchEvent !== other.onPreIndirectTouchEvent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = onIndirectTouchEvent?.hashCode() ?: 0
        result = 31 * result + (onPreIndirectTouchEvent?.hashCode() ?: 0)
        return result
    }
}

@ExperimentalIndirectTouchTypeApi
private class IndirectTouchInputNode(
    var onEvent: ((IndirectTouchEvent) -> Boolean)?,
    var onPreEvent: ((IndirectTouchEvent) -> Boolean)?,
) : IndirectTouchInputModifierNode, Modifier.Node() {
    override fun onIndirectTouchEvent(event: IndirectTouchEvent) = onEvent?.invoke(event) == true

    override fun onPreIndirectTouchEvent(event: IndirectTouchEvent) =
        onPreEvent?.invoke(event) == true
}
