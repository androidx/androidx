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
package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.substring
import java.awt.Rectangle
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import kotlin.math.max
import kotlin.math.min
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

@Suppress("DEPRECATION") // TODO https://youtrack.jetbrains.com/issue/CMP-9858
internal class DesktopTextInputService(private val component: PlatformComponent) :
    androidx.compose.ui.text.input.PlatformTextInputService {
    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: ((List<EditCommand>) -> Unit),
        val onImeActionPerformed: (ImeAction) -> Unit,
        val imeAction: ImeAction,
        var focusedRect: Rect? = null
    )

    private var currentInput: CurrentInput? = null

    // This is required to support input of accented characters using press-and-hold method (http://support.apple.com/kb/PH11264).
    // JDK currently properly supports this functionality only for TextComponent/JTextComponent descendants.
    // For our editor component we need this workaround.
    // After https://bugs.openjdk.java.net/browse/JDK-8074882 is fixed, this workaround should be replaced with a proper solution.
    var charKeyPressed: Boolean = false
    var needToDeletePreviousChar: Boolean = false

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        val input = CurrentInput(
            value = value,
            onEditCommand = onEditCommand,
            onImeActionPerformed = onImeActionPerformed,
            imeAction = imeOptions.imeAction
        )
        currentInput = input

        component.enableInput(methodRequestsForInput(input))
    }

    override fun stopInput() {
        // We may have been "started" with startInput() (with no arguments), but it's not a real
        // session in this case, so let's not call component.disableInput()
        if (currentInput == null) return

        component.disableInput()
        currentInput = null
    }

    override fun showSoftwareKeyboard() {
    }

    override fun hideSoftwareKeyboard() {
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        currentInput?.value = newValue
    }

    override fun notifyFocusedRect(rect: Rect) {
        currentInput?.focusedRect = rect
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.id) {
            KeyEvent.KEY_TYPED ->
                charKeyPressed = true
            KeyEvent.KEY_RELEASED ->
                charKeyPressed = false
        }
    }

    fun inputMethodTextChanged(event: InputMethodEvent) {
        if ((currentInput != null) && !event.isConsumed) {
            replaceInputMethodText(event)
            event.consume()
        }
    }

    private fun replaceInputMethodText(event: InputMethodEvent) {
        val input = currentInput ?: return

        val committed = event.committedText
        val composing = event.composingText
        val ops = mutableListOf<EditCommand>()

        if (needToDeletePreviousChar && input.value.selection.min > 0 && composing.isEmpty()) {
            needToDeletePreviousChar = false
            ops.add(DeleteSurroundingTextInCodePointsCommand(1, 0))
        }

        ops.add(CommitTextCommand(committed, 1))
        if (composing.isNotEmpty()) {
            ops.add(SetComposingTextCommand(composing, 1))
        }

        input.onEditCommand.invoke(ops)
    }

    private fun methodRequestsForInput(input: CurrentInput) =
        object : InputMethodRequests {
            override fun getLocationOffset(x: Int, y: Int): TextHitInfo? {
                if (input.value.composition != null) {
                    // TODO: to properly implement this method we need to somehow have access to
                    //  Paragraph at this point
                    return TextHitInfo.leading(0)
                }
                return null
            }

            override fun cancelLatestCommittedText(
                attributes: Array<AttributedCharacterIterator.Attribute>?
            ): AttributedCharacterIterator? {
                return null
            }

            override fun getInsertPositionOffset(): Int {
                val composedStartIndex = input.value.composition?.start ?: 0
                val composedEndIndex = input.value.composition?.end ?: 0

                val caretIndex = input.value.selection.start
                if (caretIndex < composedStartIndex) {
                    return caretIndex
                }
                if (caretIndex < composedEndIndex) {
                    return composedStartIndex
                }
                return caretIndex - (composedEndIndex - composedStartIndex)
            }

            override fun getCommittedTextLength() =
                input.value.text.length - (input.value.composition?.length ?: 0)

            override fun getSelectedText(
                attributes: Array<AttributedCharacterIterator.Attribute>?
            ): AttributedCharacterIterator {
                if (charKeyPressed && (hostOs == OS.MacOS)) {
                    needToDeletePreviousChar = true
                }
                val str = input.value.text.substring(input.value.selection)
                return AttributedString(str).iterator
            }

            override fun getTextLocation(offset: TextHitInfo?): Rectangle? {
                return input.focusedRect?.let {
                    val x = (it.right / component.density.density).toInt() +
                        component.locationOnScreen.x
                    val y = (it.top / component.density.density).toInt() +
                        component.locationOnScreen.y
                    Rectangle(x, y, it.width.toInt(), it.height.toInt())
                }
            }

            override fun getCommittedText(
                beginIndex: Int,
                endIndex: Int,
                attributes: Array<AttributedCharacterIterator.Attribute>?
            ): AttributedCharacterIterator {
                val comp = input.value.composition
                val text = input.value.text
                // When input is performed with Pinyin and backspace pressed,
                // comp is null and beginIndex > endIndex.
                // TODO Check is this an expected behavior?
                val range = TextRange(
                    start = beginIndex.coerceAtMost(text.length),
                    end = endIndex.coerceAtMost(text.length)
                )
                if (comp == null) {
                    val res = text.substring(range)
                    return AttributedString(res).iterator
                }
                val committed = text.substring(
                    TextRange(
                        min(range.min, comp.min).coerceAtMost(text.length),
                        max(range.max, comp.max).coerceAtMost(text.length)
                    )
                )
                return AttributedString(committed).iterator
            }
        }
}
