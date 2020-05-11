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

    sendTextInputCommand(DeleteAllEditOp())
}

/**
 * Sends the given text to this node in similar way to IME.
 *
 * @param text Text to send.
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 *
 */
fun SemanticsNodeInteraction.doSendText(text: String, alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        doClick()
    }
    // TODO: There should be some assertion on focus in the future.
    // TODO: Calling this twice replaces the text instead of appending it. Why?
    sendTextInputCommand(CommitTextEditOp(text, 1))
}

internal fun SemanticsNodeInteraction.sendTextInputCommand(command: EditOperation) {
    val owner = (fetchSemanticsNode().componentNode.owner as AndroidOwner)

    runOnUiThread {
        val textInputService = owner.textInputService as TextInputServiceForTests?
            ?: throw IllegalStateException ("Text input service wrapper not set up! Did you use " +
                    "ComposeTestRule?")

        val onEditCommand = textInputService.onEditCommand
            ?: throw IllegalStateException("No input session started. Missing a focus?")

        onEditCommand.invoke(listOf(command))
    }
}