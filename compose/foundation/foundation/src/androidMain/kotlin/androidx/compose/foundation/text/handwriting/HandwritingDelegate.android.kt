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

package androidx.compose.foundation.text.handwriting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.input.internal.ComposeInputMethodManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo

/**
 * Configures an element to act as a stylus handwriting delegate which can handle text input from a
 * handwriting session which was triggered by stylus handwriting on a handwriting delegator.
 *
 * When this element gains focus, if there is an ongoing stylus handwriting delegation which was
 * triggered by stylus handwriting on a handwriting delegator, this element will receive text input
 * from the handwriting session via its input connection.
 *
 * A common use case is a component which looks like a text input field but does not actually
 * support text input itself, and clicking on this fake text input field causes a real text input
 * field to be shown. To support handwriting initiation in this case, a [handwritingDelegator]
 * modifier can be applied to the fake text input field to configure it as a delegator, and this
 * modifier can be applied to the real text input field. The `callback` implementation for the fake
 * text field's [handwritingDelegator] modifier is typically the same as the `onClick`
 * implementation its [clickable] modifier, which shows and focuses the real text input field.
 *
 * This function returns a no-op modifier on API levels below Android U (34) as stylus handwriting
 * is not supported.
 */
fun Modifier.handwritingDelegate(): Modifier =
    if (isStylusHandwritingSupported) then(HandwritingDelegateElement()) else this

private class HandwritingDelegateElement : ModifierNodeElement<HandwritingDelegateNode>() {
    override fun create() = HandwritingDelegateNode()

    override fun update(node: HandwritingDelegateNode) {}

    override fun hashCode() = 0

    override fun equals(other: Any?) = other is HandwritingDelegateElement

    override fun InspectorInfo.inspectableProperties() {
        name = "handwritingDelegate"
    }
}

private class HandwritingDelegateNode : FocusEventModifierNode, Modifier.Node() {
    private var focusState: FocusState? = null
    private val composeImm by lazy(LazyThreadSafetyMode.NONE) {
        ComposeInputMethodManager(requireView())
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (this.focusState != focusState) {
            this.focusState = focusState
            if (focusState.hasFocus) {
                composeImm.acceptStylusHandwritingDelegation()
            }
        }
    }
}
