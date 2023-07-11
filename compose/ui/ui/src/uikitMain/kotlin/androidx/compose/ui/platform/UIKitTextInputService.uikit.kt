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

import androidx.compose.ui.text.input.*
import kotlin.math.min
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.ios.SkikoUITextInputTraits

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
        currentImeOptions = imeOptions
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        currentInput = null
        currentImeOptions = null
        hideSoftwareKeyboard()
    }

    override fun showSoftwareKeyboard() {
        _showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        _hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        val textChanged = oldValue == null || oldValue.text != newValue.text
        val selectionChanged = textChanged || oldValue == null || oldValue.selection != newValue.selection
        if (textChanged) {
            textWillChange()
        }
        if (selectionChanged) {
            selectionWillChange()
        }
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
        updateView()
    }

    val skikoInput = object : SkikoInput {

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
            sendEditCommand(
                BackspaceCommand()
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

        override fun textContentType(): UITextContentType? =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Password, KeyboardType.NumberPassword -> UITextContentTypePassword
                KeyboardType.Email -> UITextContentTypeEmailAddress
                KeyboardType.Phone -> UITextContentTypeTelephoneNumber
                else -> null
            }

        override fun isSecureTextEntry(): Boolean =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Password, KeyboardType.NumberPassword -> true
                else -> false
            }

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

    private fun sendEditCommand(vararg commands: EditCommand) {
        currentInput?.let { input ->
            input.onEditCommand(commands.toList())
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

    private fun getState(): TextFieldValue? = currentInput?.value

}
