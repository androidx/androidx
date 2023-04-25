/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.input.key

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events before they are sent to the software keyboard.
 *
 * @param onInterceptKeyBeforeSoftKeyboard This callback is invoked when the user interacts with
 * the hardware keyboard. While implementing this callback, return true to stop propagation of this
 * event. If you return false, the key event will be sent to this
 * [SoftKeyboardInterceptionModifierNode]'s parent, and ultimately to the software keyboard.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onInterceptKeyBeforeSoftKeyboard(
    onInterceptKeyBeforeSoftKeyboard: (KeyEvent) -> Boolean
): Modifier = this then SoftKeyboardInterceptionElement(
    onKeyEvent = onInterceptKeyBeforeSoftKeyboard,
    onPreKeyEvent = null
)

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events before they are sent to the software keyboard. This
 * modifier is similar to [onInterceptKeyBeforeSoftKeyboard], but allows a parent composable to
 * intercept the hardware key event before any child.
 *
 * @param onPreInterceptKeyBeforeSoftKeyboard This callback is invoked when the user interacts
 * with the hardware keyboard. It gives ancestors of a focused component the chance to intercept a
 * [KeyEvent]. Return true to stop propagation of this event. If you return false, the key event
 * will be sent to this [SoftKeyboardInterceptionModifierNode]'s child. If none of the children
 * consume the event, it will be sent back up to the root [KeyInputModifierNode] using the
 * onKeyEvent callback, and ultimately to the software keyboard.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onPreInterceptKeyBeforeSoftKeyboard(
    onPreInterceptKeyBeforeSoftKeyboard: (KeyEvent) -> Boolean,
): Modifier = this then SoftKeyboardInterceptionElement(
    onKeyEvent = null,
    onPreKeyEvent = onPreInterceptKeyBeforeSoftKeyboard
)

private data class SoftKeyboardInterceptionElement(
    val onKeyEvent: ((KeyEvent) -> Boolean)?,
    val onPreKeyEvent: ((KeyEvent) -> Boolean)?
) : ModifierNodeElement<InterceptedKeyInputModifierNodeImpl>() {
    override fun create() = InterceptedKeyInputModifierNodeImpl(
        onEvent = onKeyEvent,
        onPreEvent = onPreKeyEvent
    )

    override fun update(node: InterceptedKeyInputModifierNodeImpl) {
        node.onEvent = onKeyEvent
        node.onPreEvent = onPreKeyEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        onKeyEvent?.let {
            name = "onKeyToSoftKeyboardInterceptedEvent"
            properties["onKeyToSoftKeyboardInterceptedEvent"] = it
        }
        onPreKeyEvent?.let {
            name = "onPreKeyToSoftKeyboardInterceptedEvent"
            properties["onPreKeyToSoftKeyboardInterceptedEvent"] = it
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class InterceptedKeyInputModifierNodeImpl(
    var onEvent: ((KeyEvent) -> Boolean)?,
    var onPreEvent: ((KeyEvent) -> Boolean)?
) : SoftKeyboardInterceptionModifierNode, Modifier.Node() {
    override fun onInterceptKeyBeforeSoftKeyboard(event: KeyEvent): Boolean =
        onEvent?.invoke(event) ?: false
    override fun onPreInterceptKeyBeforeSoftKeyboard(event: KeyEvent): Boolean =
        onPreEvent?.invoke(event) ?: false
}
