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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.internal.DesktopLegacyPlatformTextInputServiceAdapter.CurrentInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
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
import java.awt.event.InputMethodListener
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import java.text.CharacterIterator
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job

internal actual fun createLegacyPlatformTextInputServiceAdapter():
    LegacyPlatformTextInputServiceAdapter = DesktopLegacyPlatformTextInputServiceAdapter()

internal class DesktopLegacyPlatformTextInputServiceAdapter :
    LegacyPlatformTextInputServiceAdapter() {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: ((List<EditCommand>) -> Unit),
        val onImeActionPerformed: ((ImeAction) -> Unit),
        val imeAction: ImeAction,
        var focusedRect: Rect? = null
    )

    private var job: Job? = null
    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        val node = textInputModifierNode ?: return
        node.launchTextInputSession {
            val input = CurrentInput(
                value, onEditCommand, onImeActionPerformed, imeOptions.imeAction
            )
            currentInput = input
            try {
                startInputMethod(LegacyTextInputMethodRequest(input, node.layoutCoordinates))
            } finally {
                currentInput = null
            }
        }
    }

    override fun stopInput() {
        job?.cancel()
        job = null
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        currentInput?.let { input ->
            input.value = newValue
        }
    }

    override fun notifyFocusedRect(rect: Rect) {
        currentInput?.let { input ->
            input.focusedRect = rect
        }
    }

    override fun startStylusHandwriting() {
        // Noop for desktop
    }
}

internal class LegacyTextInputMethodRequest(
    private val input: CurrentInput,
    private val coordinates: LayoutCoordinates?,
) : PlatformTextInputMethodRequest,
    InputMethodRequests,
    InputMethodListener {

    // This is required to support input of accented characters using press-and-hold method
    // (http://support.apple.com/kb/PH11264). JDK currently properly supports this functionality
    // only for TextComponent/JTextComponent descendants. For our editor component we need this
    // workaround. After https://bugs.openjdk.java.net/browse/JDK-8074882 is fixed, this workaround
    // should be replaced with a proper solution.
    internal var charKeyPressed: Boolean = false
    private var needToDeletePreviousChar: Boolean = false

    override val inputMethodListener: InputMethodListener
        get() = this
    override val inputMethodRequests: InputMethodRequests
        get() = this

    override fun inputMethodTextChanged(event: InputMethodEvent) {
        if (event.isConsumed) return
        replaceInputMethodText(event)
        event.consume()
    }

    override fun caretPositionChanged(event: InputMethodEvent) {
        if (event.isConsumed) return
        inputMethodCaretPositionChanged(event)
        event.consume()
    }

    private fun inputMethodCaretPositionChanged(
        @Suppress("UNUSED_PARAMETER") event: InputMethodEvent
    ) {
        // Which OSes and which input method could produce such events? We need to have some
        // specific cases in mind before implementing this
    }

    // Visible for testing.
    internal fun replaceInputMethodText(event: InputMethodEvent) {
        if (event.text == null) {
            return
        }
        val committed = event.text.toStringUntil(event.committedCharacterCount)
        val composing = event.text.toStringFrom(event.committedCharacterCount)
        val ops = mutableListOf<EditCommand>()

        if (needToDeletePreviousChar && isMac && input.value.selection.min > 0) {
            needToDeletePreviousChar = false
            ops.add(DeleteSurroundingTextInCodePointsCommand(1, 0))
        }

        // newCursorPosition == 1 leads to effectively ignoring of this parameter in EditCommands
        // processing. the cursor will be set after the inserted text.
        if (committed.isNotEmpty()) {
            ops.add(CommitTextCommand(committed, 1))
        }
        if (composing.isNotEmpty()) {
            ops.add(SetComposingTextCommand(composing, 1))
        }

        input.onEditCommand.invoke(ops)
    }

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
        if (charKeyPressed) {
            needToDeletePreviousChar = true
        }
        val str = input.value.text.substring(input.value.selection)
        return AttributedString(str).iterator
    }

    override fun getTextLocation(offset: TextHitInfo): Rectangle? {
        return input.focusedRect?.let {
            val (x, y) = coordinates?.localToScreen(it.topRight) ?: return null
            Rectangle(x.toInt(), y.toInt(), it.width.toInt(), it.height.toInt())
        }
    }

    override fun getCommittedText(
        beginIndex: Int,
        endIndex: Int,
        attributes: Array<AttributedCharacterIterator.Attribute>?
    ): AttributedCharacterIterator {
        val comp = input.value.composition
        val text = input.value.text
        val range = TextRange(beginIndex, endIndex.coerceAtMost(text.length))
        if (comp == null) {
            val res = text.substring(range)
            return AttributedString(res).iterator
        }
        val committed = text.substring(
            TextRange(
                min(range.min, comp.min),
                max(range.max, comp.max).coerceAtMost(text.length)
            )
        )
        return AttributedString(committed).iterator
    }
}

private fun AttributedCharacterIterator.toStringUntil(index: Int): String {
    val strBuf = StringBuffer()
    var i = index
    if (i > 0) {
        var c: Char = setIndex(0)
        while (i > 0) {
            strBuf.append(c)
            c = next()
            i--
        }
    }
    return String(strBuf)
}

private fun AttributedCharacterIterator.toStringFrom(index: Int): String {
    val strBuf = StringBuffer()
    var c: Char = setIndex(index)
    while (c != CharacterIterator.DONE) {
        strBuf.append(c)
        c = next()
    }
    return String(strBuf)
}

internal val isMac = System.getProperty("os.name").lowercase(Locale.ENGLISH).startsWith("mac")
