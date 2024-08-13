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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.OnImeAction
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction

/** Clears the text in this node in similar way to IME. */
fun SemanticsNodeInteraction.performTextClearance() {
    performTextReplacement("")
}

/**
 * Sends the given text to this node in similar way to IME.
 *
 * @param text Text to send.
 */
fun SemanticsNodeInteraction.performTextInput(text: String) {
    @OptIn(ExperimentalTestApi::class) invokeGlobalAssertions()
    tryPerformAccessibilityChecks()
    getNodeAndFocus()
    performSemanticsAction(SemanticsActions.InsertTextAtCursor) { it(AnnotatedString(text)) }
}

/**
 * Sends the given selection to this node in similar way to IME.
 *
 * @param selection selection to send
 */
@ExperimentalTestApi
fun SemanticsNodeInteraction.performTextInputSelection(selection: TextRange) {
    getNodeAndFocus(requireEditable = false)
    performSemanticsAction(SemanticsActions.SetSelection) {
        // Pass true as the last parameter since this range is relative to the text before any
        // VisualTransformation is applied.
        it(selection.min, selection.max, true)
    }
}

/**
 * Replaces existing text with the given text in this node in similar way to IME.
 *
 * This does not reflect text selection. All the text gets cleared out and new inserted.
 *
 * @param text Text to send.
 */
fun SemanticsNodeInteraction.performTextReplacement(text: String) {
    getNodeAndFocus()
    performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(text)) }
}

/**
 * Sends to this node the IME action associated with it in a similar way to the IME.
 *
 * The node needs to define its IME action in semantics via [SemanticsPropertyReceiver.onImeAction].
 *
 * @throws AssertionError if the node does not support input or does not define IME action.
 * @throws IllegalStateException if the node did is not an editor or would not be able to establish
 *   an input connection (e.g. does not define [ImeAction][SemanticsProperties.ImeAction] or
 *   [OnImeAction] or is not focused).
 */
fun SemanticsNodeInteraction.performImeAction() {
    val errorOnFail = "Failed to perform IME action."
    assert(hasPerformImeAction()) { errorOnFail }
    assert(!hasImeAction(ImeAction.Default)) { errorOnFail }
    @OptIn(ExperimentalTestApi::class) invokeGlobalAssertions()
    tryPerformAccessibilityChecks()
    val node = getNodeAndFocus(errorOnFail, requireEditable = false)

    wrapAssertionErrorsWithNodeInfo(selector, node) {
        performSemanticsAction(OnImeAction) {
            assertOnJvm(it()) {
                buildGeneralErrorMessage(
                    "Failed to perform IME action, handler returned false.",
                    selector,
                    node
                )
            }
        }
    }
}

private fun SemanticsNodeInteraction.getNodeAndFocus(
    errorOnFail: String = "Failed to perform text input.",
    requireEditable: Boolean = true
): SemanticsNode {
    @OptIn(ExperimentalTestApi::class) invokeGlobalAssertions()
    tryPerformAccessibilityChecks()
    val node = fetchSemanticsNode(errorOnFail)
    assert(isEnabled()) { errorOnFail }
    assert(hasRequestFocusAction()) { errorOnFail }
    if (requireEditable) {
        assert(hasSetTextAction()) { errorOnFail }
        assert(hasInsertTextAtCursorAction()) { errorOnFail }
    }

    if (!isFocused().matches(node)) {
        // Get focus
        performSemanticsAction(SemanticsActions.RequestFocus)
    }

    return node
}

internal expect inline fun <R> wrapAssertionErrorsWithNodeInfo(
    selector: SemanticsSelector,
    node: SemanticsNode,
    block: () -> R
): R

internal expect inline fun assertOnJvm(value: Boolean, lazyMessage: () -> Any)
