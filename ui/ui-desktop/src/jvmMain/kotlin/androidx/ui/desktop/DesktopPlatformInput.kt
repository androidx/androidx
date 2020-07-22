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
package androidx.ui.desktop

import androidx.compose.ui.text.input.BackspaceKeyEditOp
import androidx.compose.ui.text.input.CommitTextEditOp
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.MoveCursorEditOp
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.geometry.Rect

import java.awt.event.KeyEvent

internal class DesktopPlatformInput : PlatformTextInputService {
    var onEditCommand: ((List<EditOperation>) -> Unit)? = null
    var onImeActionPerformed: ((ImeAction) -> Unit)? = null
    var imeAction: ImeAction? = null

    override fun startInput(
        value: TextFieldValue,
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed
        this.imeAction = imeAction
    }

    override fun stopInput() {
        this.onEditCommand = null
        this.onImeActionPerformed = null
        this.imeAction = null
    }

    override fun showSoftwareKeyboard() {}

    override fun hideSoftwareKeyboard() {}

    override fun onStateUpdated(value: TextFieldValue) {
    }

    override fun notifyFocusedRect(rect: Rect) {}

    @Suppress("UNUSED_PARAMETER")
    fun onKeyPressed(keyCode: Int, char: Char) {
        onEditCommand?.let {
            when (keyCode) {
                KeyEvent.VK_LEFT -> {
                    it.invoke(listOf(MoveCursorEditOp(-1)))
                }
                KeyEvent.VK_RIGHT -> {
                    it.invoke(listOf(MoveCursorEditOp(1)))
                }
                KeyEvent.VK_BACK_SPACE -> {
                    it.invoke(listOf(BackspaceKeyEditOp()))
                }
                KeyEvent.VK_ENTER -> {
                    if (imeAction == ImeAction.Unspecified) {
                        it.invoke(listOf(CommitTextEditOp("", 1)))
                    } else
                        onImeActionPerformed?.invoke(imeAction!!)
                }
                else -> Unit
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onKeyReleased(keyCode: Int, char: Char) {
    }

    private fun Char.isPrintable(): Boolean {
        val block = Character.UnicodeBlock.of(this)
        return (!Character.isISOControl(this)) &&
                this != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS
    }

    fun onKeyTyped(char: Char) {
        onEditCommand?.let {
            if (char.isPrintable()) {
                it.invoke(listOf(CommitTextEditOp(char.toString(), 1)))
            }
        }
    }
}