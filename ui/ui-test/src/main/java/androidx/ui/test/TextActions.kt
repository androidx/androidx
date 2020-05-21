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

package androidx.ui.test

import androidx.ui.core.AndroidOwner
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.DeleteAllEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.ImeAction
import androidx.ui.text.TextSemanticsProperties

/**
 * Clears the text in this node in similar way to IME.
 *
 * Note performing this operation requires to get a focus.
 *
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.doClearText(alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        doClick()
    }
    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(DeleteAllEditOp()))
}

/**
 * Sends the given text to this node in similar way to IME.
 *
 * @param text Text to send.
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.doSendText(text: String, alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        doClick()
    }
    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(CommitTextEditOp(text, 1)))
}

/**
 * Replaces existing text with the given text in this node in similar way to IME.
 *
 * This does not reflect text selection. All the text gets cleared out and new inserted.
 *
 * @param text Text to send.
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.doReplaceText(text: String, alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        doClick()
    }

    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(DeleteAllEditOp(), CommitTextEditOp(text, 1)))
}

/**
 * Sends to this node the IME action associated with it in similar way to IME.
 *
 * The node needs to define its IME action in semantics.
 *
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 *
 * @throws AssertionError if the node does not support input or does not define IME action.
 * @throws IllegalStateException if tne node did not establish input connection (e.g. is not
 * focused)
 */
fun SemanticsNodeInteraction.doSendImeAction(alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        doClick()
    }

    val errorOnFail = "Failed to send IME action."
    val node = fetchSemanticsNode(errorOnFail)

    assert(hasInputMethodsSupport()) { errorOnFail }

    val actionSpecified = node.config.getOrElse(TextSemanticsProperties.ImeAction) {
        ImeAction.Unspecified
    }
    if (actionSpecified == ImeAction.Unspecified) {
        throw AssertionError(buildGeneralErrorMessage(
            "Failed to send IME action as current node does not specify any.", selector, node))
    }

    val owner = node.componentNode.owner as AndroidOwner

    runOnUiThread {
        val textInputService = owner.getTextInputServiceOrDie()

        val onImeActionPerformed = textInputService.onImeActionPerformed
            ?: throw IllegalStateException("No input session started. Missing a focus?")

        onImeActionPerformed.invoke(actionSpecified)
    }
}

internal fun SemanticsNodeInteraction.sendTextInputCommand(command: List<EditOperation>) {
    val errorOnFail = "Failed to send text input."
    val node = fetchSemanticsNode(errorOnFail)
    val owner = node.componentNode.owner as AndroidOwner

    assert(hasInputMethodsSupport()) { errorOnFail }

    runOnUiThread {
        val textInputService = owner.getTextInputServiceOrDie()

        val onEditCommand = textInputService.onEditCommand
            ?: throw IllegalStateException("No input session started. Missing a focus?")

        onEditCommand(command)
    }
}

internal fun AndroidOwner.getTextInputServiceOrDie(): TextInputServiceForTests {
    return this.textInputService as TextInputServiceForTests?
        ?: throw IllegalStateException ("Text input service wrapper not set up! Did you use " +
                "ComposeTestRule?")
}