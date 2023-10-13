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

package androidx.compose.ui.platform

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.text.input.*
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.test.assertContains
import org.jetbrains.skia.BreakIterator
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEventKind
import platform.UIKit.*

internal class UIKitTextInputService(
    showSoftwareKeyboard: () -> Unit,
    hideSoftwareKeyboard: () -> Unit,
    private val updateView: () -> Unit,
    private val textWillChange: () -> Unit,
    private val textDidChange: () -> Unit,
    private val selectionWillChange: () -> Unit,
    private val selectionDidChange: () -> Unit,
) : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: (List<EditCommand>) -> Unit
    )

    private val _showSoftwareKeyboard: () -> Unit = showSoftwareKeyboard
    private val _hideSoftwareKeyboard: () -> Unit = hideSoftwareKeyboard
    private var currentInput: CurrentInput? = null
    private var currentImeOptions: ImeOptions? = null
    private var currentImeActionHandler: ((ImeAction) -> Unit)? = null

    /**
     * Workaround to prevent calling textWillChange, textDidChange, selectionWillChange, and
     * selectionDidChange when the value of the current input is changed by the system (i.e., by the user
     * input) not by the state change of the Compose side. These 4 functions call methods of
     * UITextInputDelegateProtocol, which notifies the system that the text or the selection of the
     * current input has changed.
     *
     * This is to properly handle multi-stage input methods that depend on text selection, required by
     * languages such as Korean (Chinese and Japanese input methods depend on text marking). The writing
     * system of these languages contains letters that can be broken into multiple parts, and each keyboard
     * key corresponds to those parts. Therefore, the input system holds an internal state to combine these
     * parts correctly. However, the methods of UITextInputDelegateProtocol reset this state, resulting in
     * incorrect input. (e.g., 컴포즈 becomes ㅋㅓㅁㅍㅗㅈㅡ when not handled properly)
     *
     * @see _tempCurrentInputSession holds the same text and selection of the current input. It is used
     * instead of the old value passed to updateState. When the current value change is due to the
     * user input, updateState is not effective because _tempCurrentInputSession holds the same value.
     * However, when the current value change is due to the change of the user selection or to the
     * state change in the Compose side, updateState calls the 4 methods because the new value holds
     * these changes.
     */
    private var _tempCurrentInputSession: EditProcessor? = null

    /**
     * Workaround to prevent IME action from being called multiple times with hardware keyboards.
     * When the hardware return key is held down, iOS sends multiple newline characters to the application,
     * which makes UIKitTextInputService call the current IME action multiple times without an additional
     * debouncing logic.
     *
     * @see _tempHardwareReturnKeyPressed is set to true when the return key is pressed with a
     * hardware keyboard.
     * @see _tempImeActionIsCalledWithHardwareReturnKey is set to true when the
     * current IME action has been called within the current hardware return key press.
     */
    private var _tempHardwareReturnKeyPressed: Boolean = false
    private var _tempImeActionIsCalledWithHardwareReturnKey: Boolean = false

    /**
     * Workaround to fix voice dictation.
     * UIKit call insertText(text) and replaceRange(range,text) immediately,
     * but Compose recomposition happen on next draw frame.
     * So the value of getSelectedTextRange is in the old state when the replaceRange function is called.
     * @see _tempCursorPos helps to fix this behaviour. Permanently update _tempCursorPos in function insertText.
     * And after clear in updateState function.
     */
    private var _tempCursorPos: Int? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(value, onEditCommand)
        _tempCurrentInputSession = EditProcessor().apply {
            reset(value, null)
        }
        currentImeOptions = imeOptions
        currentImeActionHandler = onImeActionPerformed
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        currentInput = null
        _tempCurrentInputSession = null
        currentImeOptions = null
        currentImeActionHandler = null
        hideSoftwareKeyboard()
    }

    override fun showSoftwareKeyboard() {
        _showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        _hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        val internalOldValue = _tempCurrentInputSession?.toTextFieldValue()
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged = textChanged || internalOldValue == null || internalOldValue.selection != newValue.selection
        if (textChanged) {
            textWillChange()
        }
        if (selectionChanged) {
            selectionWillChange()
        }
        _tempCurrentInputSession?.reset(newValue, null)
        currentInput?.let { input ->
            input.value = newValue
            _tempCursorPos = null
        }
        if (textChanged) {
            textDidChange()
        }
        if (selectionChanged) {
            selectionDidChange()
        }
        if (textChanged || selectionChanged) {
            updateView()
        }
    }

    val skikoInput = object : IOSSkikoInput {

        /**
         * A Boolean value that indicates whether the text-entry object has any text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
         */
        override fun hasText(): Boolean = getState()?.text?.isNotEmpty() ?: false

        /**
         * Inserts a character into the displayed text.
         * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
         * @param text A string object representing the character typed on the system keyboard.
         */
        override fun insertText(text: String) {
            if (text == "\n") {
                if (runImeActionIfRequired()) {
                    return
                }
            }
            getCursorPos()?.let {
                _tempCursorPos = it + text.length
            }
            sendEditCommand(CommitTextCommand(text, 1))
        }

        /**
         * Deletes a character from the displayed text.
         * Remove the character just before the cursor from your class’s backing store and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
         */
        override fun deleteBackward() {
            // Before this function calls, iOS changes selection in setSelectedTextRange.
            // All needed characters should be allready selected, and we can just remove them.
            sendEditCommand(
                CommitTextCommand("", 0)
            )
        }

        /**
         * The text position for the end of a document.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
         */
        override fun endOfDocument(): Long = getState()?.text?.length?.toLong() ?: 0L

        /**
         * The range of selected text in a document.
         * If the text range has a length, it indicates the currently selected text.
         * If it has zero length, it indicates the caret (insertion point).
         * If the text-range object is nil, it indicates that there is no current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
         */
        override fun getSelectedTextRange(): IntRange? {
            val cursorPos = getCursorPos()
            if (cursorPos != null) {
                return cursorPos until cursorPos
            }
            val selection = getState()?.selection
            return if (selection != null) {
                selection.start until selection.end
            } else {
                null
            }
        }

        override fun setSelectedTextRange(range: IntRange?) {
            if (range != null) {
                sendEditCommand(
                    SetSelectionCommand(range.start, range.endInclusive + 1)
                )
            } else {
                sendEditCommand(
                    SetSelectionCommand(endOfDocument().toInt(), endOfDocument().toInt())
                )
            }
        }

        override fun selectAll() {
            sendEditCommand(
                SetSelectionCommand(0, endOfDocument().toInt())
            )
        }

        /**
         * Returns the text in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
         * @param range A range of text in a document.
         * @return A substring of a document that falls within the specified range.
         */
        override fun textInRange(range: IntRange): String {
            val text = getState()?.text
            return text?.substring(range.first, min(range.last + 1, text.length)) ?: ""
        }

        /**
         * Replaces the text in a document that is in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
         * @param range A range of text in a document.
         * @param text A string to replace the text in range.
         */
        override fun replaceRange(range: IntRange, text: String) {
            sendEditCommand(
                SetComposingRegionCommand(range.start, range.endInclusive + 1),
                SetComposingTextCommand(text, 1),
                FinishComposingTextCommand(),
            )
        }

        /**
         * Inserts the provided text and marks it to indicate that it is part of an active input session.
         * Setting marked text either replaces the existing marked text or,
         * if none is present, inserts it in place of the current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614465-setmarkedtext
         * @param markedText The text to be marked.
         * @param selectedRange A range within markedText that indicates the current selection.
         * This range is always relative to markedText.
         */
        override fun setMarkedText(markedText: String?, selectedRange: IntRange) {
            if (markedText != null) {
                sendEditCommand(
                    SetComposingTextCommand(markedText, 1)
                )
            }
        }

        /**
         * The range of currently marked text in a document.
         * If there is no marked text, the value of the property is nil.
         * Marked text is provisionally inserted text that requires user confirmation;
         * it occurs in multistage text input.
         * The current selection, which can be a caret or an extended range, always occurs within the marked text.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
         */
        override fun markedTextRange(): IntRange? {
            val composition = getState()?.composition
            return if (composition != null) {
                composition.start until composition.end
            } else {
                null
            }
        }

        /**
         * Unmarks the currently marked text.
         * After this method is called, the value of markedTextRange is nil.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
         */
        override fun unmarkText() {
            sendEditCommand(FinishComposingTextCommand())
        }

        /**
         * Returns the text position at a specified offset from another text position.
         */
        override fun positionFromPosition(position: Long, offset: Long): Long {
            val text = getState()?.text ?: return 0

            if (position + offset >= text.lastIndex + 1) {
                return (text.lastIndex + 1).toLong()
            }
            if (position + offset <= 0) {
                return 0
            }
            var resultPosition = position.toInt()
            val iterator = BreakIterator.makeCharacterInstance()
            iterator.setText(text)

            repeat(offset.absoluteValue.toInt()) {
                resultPosition = if (offset > 0) {
                    iterator.following(resultPosition)
                } else {
                    iterator.preceding(resultPosition)
                }
            }

            return resultPosition.toLong()
        }

        /**
         * Return the range for the text enclosing a text position in a text unit of a given granularity in a given direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614464-rangeenclosingposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * A text-range representing a text unit of the given granularity in the given direction, or nil if there is no such enclosing unit.
         * Whether a boundary position is enclosed depends on the given direction, using the same rule as the isPosition:withinTextUnit:inDirection: method.
         */
        override fun rangeEnclosingPosition(
            position: Int,
            withGranularity: UITextGranularity,
            inDirection: UITextDirection
        ): IntRange? {
            val text = getState()?.text ?: return null
            assert(position >= 0) { "rangeEnclosingPosition position >= 0" }

            fun String.isMeaningless(): Boolean {
                return when (withGranularity) {
                    UITextGranularity.UITextGranularityWord -> {
                        this.all { it in arrayOf(' ', ',') }
                    }

                    else -> false
                }
            }

            val iterator: BreakIterator = withGranularity.toTextIterator()
            iterator.setText(text)

            if (inDirection == UITextStorageDirectionForward) {
                return null
            } else if (inDirection == UITextStorageDirectionBackward) {
                var current: Int = position

                fun currentRange() = IntRange(current, position)
                fun nextAddition() = IntRange(iterator.preceding(current).coerceAtLeast(0), current)
                fun IntRange.text() = text.substring(start, endInclusive)

                while (
                    current == position
                    || currentRange().text().isMeaningless()
                    || nextAddition().text().isMeaningless()
                ) {
                    current = iterator.preceding(current)
                    if (current <= 0) {
                        current = 0
                        break
                    }
                }

                return IntRange(current, position)
            } else {
                error("Unknown inDirection: $inDirection")
            }
        }

        /**
         * Return whether a text position is at a boundary of a text unit of a specified granularity in a specified direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614553-isposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * TRUE if the text position is at the given text-unit boundary in the given direction; FALSE if it is not at the boundary.
         */
        override fun isPositionAtBoundary(
            position: Int,
            atBoundary: UITextGranularity,
            inDirection: UITextDirection
        ): Boolean {
            val text = getState()?.text ?: return false
            assert(position >= 0) { "isPositionAtBoundary position >= 0" }

            val iterator = atBoundary.toTextIterator()
            iterator.setText(text)
            return iterator.isBoundary(position)
        }

        /**
         * Return whether a text position is within a text unit of a specified granularity in a specified direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614491-isposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * TRUE if the text position is within a text unit of the specified granularity in the specified direction; otherwise, return FALSE.
         * If the text position is at a boundary, return TRUE only if the boundary is part of the text unit in the given direction.
         */
        override fun isPositionWithingTextUnit(
            position: Int,
            withinTextUnit: UITextGranularity,
            inDirection: UITextDirection
        ): Boolean {
            val text = getState()?.text ?: return false
            assert(position >= 0) { "isPositionWithingTextUnit position >= 0" }

            val iterator = withinTextUnit.toTextIterator()
            iterator.setText(text)

            if (inDirection == UITextStorageDirectionForward) {

            } else if (inDirection == UITextStorageDirectionBackward) {

            }
            return false // TODO: Write implementation
        }
    }

    val skikoUITextInputTraits = object : SkikoUITextInputTraits {
        override fun keyboardType(): UIKeyboardType =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Text -> UIKeyboardTypeDefault
                KeyboardType.Ascii -> UIKeyboardTypeASCIICapable
                KeyboardType.Number -> UIKeyboardTypeNumberPad
                KeyboardType.Phone -> UIKeyboardTypePhonePad
                KeyboardType.Uri -> UIKeyboardTypeURL
                KeyboardType.Email -> UIKeyboardTypeEmailAddress
                KeyboardType.Password -> UIKeyboardTypeASCIICapable // TODO Correct?
                KeyboardType.NumberPassword -> UIKeyboardTypeNumberPad // TODO Correct?
                KeyboardType.Decimal -> UIKeyboardTypeDecimalPad
                else -> UIKeyboardTypeDefault
            }

        override fun keyboardAppearance(): UIKeyboardAppearance = UIKeyboardAppearanceDefault
        override fun returnKeyType(): UIReturnKeyType =
            when (currentImeOptions?.imeAction) {
                ImeAction.Default -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.None -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.Go -> UIReturnKeyType.UIReturnKeyGo
                ImeAction.Search -> UIReturnKeyType.UIReturnKeySearch
                ImeAction.Send -> UIReturnKeyType.UIReturnKeySend
                ImeAction.Previous -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.Next -> UIReturnKeyType.UIReturnKeyNext
                ImeAction.Done -> UIReturnKeyType.UIReturnKeyDone
                else -> UIReturnKeyType.UIReturnKeyDefault
            }

        override fun textContentType(): UITextContentType? = null
//           TODO: Prevent Issue https://youtrack.jetbrains.com/issue/COMPOSE-319/iOS-Bug-password-TextField-changes-behavior-for-all-other-TextFieds
//            when (currentImeOptions?.keyboardType) {
//                KeyboardType.Password, KeyboardType.NumberPassword -> UITextContentTypePassword
//                KeyboardType.Email -> UITextContentTypeEmailAddress
//                KeyboardType.Phone -> UITextContentTypeTelephoneNumber
//                else -> null
//            }

        override fun isSecureTextEntry(): Boolean = false
//           TODO: Prevent Issue https://youtrack.jetbrains.com/issue/COMPOSE-319/iOS-Bug-password-TextField-changes-behavior-for-all-other-TextFieds
//            when (currentImeOptions?.keyboardType) {
//                KeyboardType.Password, KeyboardType.NumberPassword -> true
//                else -> false
//            }

        override fun enablesReturnKeyAutomatically(): Boolean = false

        override fun autocapitalizationType(): UITextAutocapitalizationType =
            when (currentImeOptions?.capitalization) {
                KeyboardCapitalization.None ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeNone

                KeyboardCapitalization.Characters ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters

                KeyboardCapitalization.Words ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeWords

                KeyboardCapitalization.Sentences ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences

                else ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            }

        override fun autocorrectionType(): UITextAutocorrectionType =
            when (currentImeOptions?.autoCorrect) {
                true -> UITextAutocorrectionType.UITextAutocorrectionTypeYes
                false -> UITextAutocorrectionType.UITextAutocorrectionTypeNo
                else -> UITextAutocorrectionType.UITextAutocorrectionTypeDefault
            }

    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        val nativeKeyEvent = event.nativeKeyEvent
        return when (nativeKeyEvent.key) {
            SkikoKey.KEY_ENTER -> handleEnterKey(nativeKeyEvent)
            SkikoKey.KEY_BACKSPACE -> handleBackspace(nativeKeyEvent)
            else -> false
        }
    }

    private fun handleEnterKey(event: NativeKeyEvent): Boolean {
        _tempImeActionIsCalledWithHardwareReturnKey = false
        return when (event.kind) {
            SkikoKeyboardEventKind.UP -> {
                _tempHardwareReturnKeyPressed = false
                false
            }

            SkikoKeyboardEventKind.DOWN -> {
                _tempHardwareReturnKeyPressed = true
                // This prevents two new line characters from being added for one hardware return key press.
                true
            }

            else -> false
        }
    }

    private fun handleBackspace(event: NativeKeyEvent): Boolean {
        // This prevents two characters from being removed for one hardware backspace key press.
        return event.kind == SkikoKeyboardEventKind.DOWN
    }

    private fun sendEditCommand(vararg commands: EditCommand) {
        val commandList = commands.toList()
        _tempCurrentInputSession?.apply(commandList)
        currentInput?.let { input ->
            input.onEditCommand(commandList)
        }
    }

    private fun getCursorPos(): Int? {
        if (_tempCursorPos != null) {
            return _tempCursorPos
        }
        val selection = getState()?.selection
        if (selection != null && selection.start == selection.end) {
            return selection.start
        }
        return null
    }

    private fun imeActionRequired(): Boolean =
        currentImeOptions?.run {
            singleLine || (
                imeAction != ImeAction.None
                    && imeAction != ImeAction.Default
                    && !(imeAction == ImeAction.Search && _tempHardwareReturnKeyPressed)
                )
        } ?: false

    private fun runImeActionIfRequired(): Boolean {
        val imeAction = currentImeOptions?.imeAction ?: return false
        val imeActionHandler = currentImeActionHandler ?: return false
        if (!imeActionRequired()) {
            return false
        }
        if (!_tempImeActionIsCalledWithHardwareReturnKey) {
            if (imeAction == ImeAction.Default) {
                imeActionHandler(ImeAction.Done)
            } else {
                imeActionHandler(imeAction)
            }
        }
        if (_tempHardwareReturnKeyPressed) {
            _tempImeActionIsCalledWithHardwareReturnKey = true
        }
        return true
    }

    private fun getState(): TextFieldValue? = currentInput?.value

}

private fun UITextGranularity.toTextIterator() =
    when (this) {
        UITextGranularity.UITextGranularitySentence -> BreakIterator.makeSentenceInstance()
        UITextGranularity.UITextGranularityLine -> BreakIterator.makeLineInstance()
        UITextGranularity.UITextGranularityWord -> BreakIterator.makeWordInstance()
        UITextGranularity.UITextGranularityCharacter -> BreakIterator.makeCharacterInstance()
        UITextGranularity.UITextGranularityParagraph -> TODO("UITextGranularityParagraph iterator")
        UITextGranularity.UITextGranularityDocument -> TODO("UITextGranularityDocument iterator")
        else -> error("Unknown granularity")
    }
