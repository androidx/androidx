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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.IntermediateTextInputUIView
import androidx.compose.ui.window.BackgroundInputView
import androidx.compose.ui.window.OverlayInputView
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIPress
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth

// Due to unexpected delays between the commands to show/hide the keyboard,
// it may jump when switching between text fields.
// Adding a delay to the 'resignFirstResponder' function call to eliminate this issue.
private val CLEAR_FOCUS_DELAY: Long = 10L

internal class UIKitTextInputService(
    private val updateView: () -> Unit,
    private val view: UIView,
    private val viewConfiguration: ViewConfiguration,
    private val focusedViewsList: FocusedViewsList?,
    private var onInputStarted: () -> Unit,
    /**
     * Callback to handle keyboard presses. The parameter is a [Set] of [UIPress] objects.
     * Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    private var onKeyboardPresses: (Set<*>) -> Unit,
    private var focusManager: () -> ComposeSceneFocusManager?
) : PlatformTextInputService, TextToolbar {

    private var currentOnEditCommand: ((List<EditCommand>) -> Unit)? = null
    private var currentImeOptions: ImeOptions? = null
    private var currentImeActionHandler: ((ImeAction) -> Unit)? = null
    private var textUIView: IntermediateTextInputUIView? = null
    private var textLayoutResult : TextLayoutResult? = null

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
     * @see sessionEditProcessor holds the same text and selection of the current input. It is used
     * instead of the old value passed to updateState. When the current value change is due to the
     * user input, updateState is not effective because _tempCurrentInputSession holds the same value.
     * However, when the current value change is due to the change of the user selection or to the
     * state change in the Compose side, updateState calls the 4 methods because the new value holds
     * these changes.
     */
    private var sessionEditProcessor: EditProcessor? = null

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
    private val mainScope = MainScope()

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        sessionEditProcessor = EditProcessor().apply {
            reset(value, null)
        }
        currentOnEditCommand = onEditCommand
        currentImeOptions = imeOptions
        currentImeActionHandler = onImeActionPerformed

        attachIntermediateTextInputView()
        textUIView?.input = createSkikoInput()
        textUIView?.inputTraits = getUITextInputTraits(imeOptions)

        showSoftwareKeyboard()
        onInputStarted()
    }

    override fun stopInput() {
        flushEditCommandsIfNeeded(force = true)
        sessionEditProcessor = null
        currentImeOptions = null
        currentImeActionHandler = null
        hideSoftwareKeyboard()

        textUIView?.inputTraits = EmptyInputTraits
        textUIView?.input = null
        detachIntermediateTextInputView()
    }

    override fun showSoftwareKeyboard() {
        textUIView?.let {
            focusedViewsList?.addAndFocus(it)
        }
    }

    override fun hideSoftwareKeyboard() {
        textUIView?.let {
            focusedViewsList?.remove(it, delayMillis = CLEAR_FOCUS_DELAY)
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        val internalOldValue = sessionEditProcessor?.toTextFieldValue()
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged = textChanged || internalOldValue.selection != newValue.selection
        if (textChanged) {
            textUIView?.textWillChange()
        }
        if (selectionChanged) {
            textUIView?.selectionWillChange()
        }
        sessionEditProcessor?.let {
            it.reset(newValue, null)
            _tempCursorPos = null
        }
        if (textChanged) {
            textUIView?.textDidChange()
        }
        if (selectionChanged) {
            textUIView?.selectionDidChange()
        }
        if (textChanged || selectionChanged) {
            updateView()
        }
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (event.key) {
            Key.Enter -> handleEnterKey(event)
            Key.Backspace -> handleBackspace(event)
            Key.Escape -> handleEscape(event)
            else -> false
        }
    }

    fun updateTextFrame(rect: Rect) {
        textUIView?.setFrame(rect.toDpRect(view.density).asCGRect())
        showMenuOrUpdatePosition()
    }

    fun updateTextLayoutResult(textLayoutResult: TextLayoutResult) {
        this.textLayoutResult = textLayoutResult
    }

    private fun handleEnterKey(event: KeyEvent): Boolean {
        _tempImeActionIsCalledWithHardwareReturnKey = false
        return when (event.type) {
            KeyEventType.KeyUp -> {
                _tempHardwareReturnKeyPressed = false
                false
            }

            KeyEventType.KeyDown -> {
                _tempHardwareReturnKeyPressed = true
                // This prevents two new line characters from being added for one hardware return key press.
                true
            }

            else -> false
        }
    }

    private fun handleBackspace(event: KeyEvent): Boolean {
        // This prevents two characters from being removed for one hardware backspace key press.
        return event.type == KeyEventType.KeyDown
    }

    private fun handleEscape(event: KeyEvent): Boolean {
        return if (sessionEditProcessor != null) {
            if (event.type == KeyEventType.KeyDown) {
                focusManager()?.releaseFocus()
            }
            true
        } else {
            false
        }
    }

    private val editCommandsBatch = mutableListOf<EditCommand>()
    private var editBatchDepth: Int = 0
        set(value) {
            field = value
            flushEditCommandsIfNeeded()
        }

    private fun sendEditCommand(vararg commands: EditCommand) {
        sessionEditProcessor?.apply(commands.toList())

        editCommandsBatch.addAll(commands)
        flushEditCommandsIfNeeded()
    }

    fun flushEditCommandsIfNeeded(force: Boolean = false) {
        if ((force || editBatchDepth == 0) && editCommandsBatch.isNotEmpty()) {
            val commandList = editCommandsBatch.toList()
            editCommandsBatch.clear()

            currentOnEditCommand?.invoke(commandList)
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

    private var textInputServiceInvalidationsCount = 0
    private fun textMenuAppearanceChanged() {
        textInputServiceInvalidationsCount++
        mainScope.launch {
            // Time to show, hide or update state of context menu
            delay(500)
            textInputServiceInvalidationsCount--
        }
    }

    val hasInvalidations: Boolean get() = textInputServiceInvalidationsCount > 0

    private fun getState(): TextFieldValue? = sessionEditProcessor?.toTextFieldValue()

    // Fixes a problem where the menu is shown before the textUIView gets its final layout.
    private var showMenuOrUpdatePosition = {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (textUIView == null) {
            // If showMenu() is called and textUIView is not created,
            // then it means that showMenu() called in SelectionContainer without any textfields,
            // and IntermediateTextInputView must be created to show an editing menu
            attachIntermediateTextInputView()
            updateView()
        }
        showMenuOrUpdatePosition = {
            textUIView?.let { textUIView ->
                val density = view.density
                val offset = textUIView.frame.useContents { origin.asDpOffset().toOffset(density) }
                val target = rect.translate(-offset).toDpRect(density).asCGRect()
                textUIView.showTextMenu(
                    targetRect = target,
                    textActions = object : TextActions {
                        override val copy: (() -> Unit)? = onCopyRequested
                        override val cut: (() -> Unit)? = onCutRequested
                        override val paste: (() -> Unit)? = onPasteRequested
                        override val selectAll: (() -> Unit)? = onSelectAllRequested
                    }
                )
                textMenuAppearanceChanged()
            }
        }
        showMenuOrUpdatePosition()
    }

    /**
     * TODO on UIKit native behaviour is hide text menu, when touch outside
     */
    override fun hide() {
        showMenuOrUpdatePosition = {}
        textUIView?.let {
            it.hideTextMenu()
            textMenuAppearanceChanged()
        }
        if ((textUIView != null) && (sessionEditProcessor == null)) { // means that editing context menu shown in selection container
            textUIView?.resignFirstResponder()
            detachIntermediateTextInputView()
        }
    }

    override val status: TextToolbarStatus
        get() = if (textUIView?.isTextMenuShown() == true)
            TextToolbarStatus.Shown
        else
            TextToolbarStatus.Hidden

    private fun attachIntermediateTextInputView() {
        detachIntermediateTextInputView()
        showMenuOrUpdatePosition = {}
        textUIView = IntermediateTextInputUIView(
            doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
        ).also {
            it.setAutoresizingMask(
                UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            )
            it.onKeyboardPresses = onKeyboardPresses
            view.addSubview(it)
            it.setFrame(view.bounds)
        }
    }

    private fun detachIntermediateTextInputView() {
        showMenuOrUpdatePosition = {}
        textUIView?.let { view ->
            val outOfBoundsFrame = CGRectMake(-100000.0, 0.0, 1.0, 1.0)
            // Set out-of-bounds non-empty frame to hide text keyboard focus frame
            view.setFrame(outOfBoundsFrame)

            view.resetOnKeyboardPressesCallback()
            mainScope.launch {
                delay(CLEAR_FOCUS_DELAY)
                view.removeFromSuperview()
            }
        }
        textUIView = null
    }

    fun dispose() {
        stopInput()
        onInputStarted = { }
        onKeyboardPresses = { }
        focusManager = { null }
    }

    private fun hasFocusedNonComposeInputViewInWindowHierarchy(): Boolean {
        fun hasFocusedNonComposeInputView(view: UIView): Boolean {
            if (view.isFirstResponder) {
                return view !is IntermediateTextInputUIView &&
                    view !is OverlayInputView &&
                    view !is BackgroundInputView
            }
            return view.subviews.any { it is UIView && hasFocusedNonComposeInputView(it) }
        }
        return view.window?.let { hasFocusedNonComposeInputView(it) } ?: false
    }

    private fun createSkikoInput() = object : IOSSkikoInput {

        private var floatingCursorTranslation : Offset? = null

        override fun onResignFocus() {
            textInputServiceInvalidationsCount++
            mainScope.launch {
                if (hasFocusedNonComposeInputViewInWindowHierarchy()) {
                    focusManager()?.releaseFocus()
                }
                textInputServiceInvalidationsCount--
            }
        }

        override fun beginFloatingCursor(offset: DpOffset) {
            val cursorPos = getCursorPos() ?: getState()?.selection?.start ?: return
            val cursorRect = textLayoutResult?.getCursorRect(cursorPos) ?: return
            floatingCursorTranslation = cursorRect.center - offset.toOffset(view.density)
        }

        override fun updateFloatingCursor(offset: DpOffset) {
            val translation = floatingCursorTranslation ?: return
            val offsetPx = offset.toOffset(view.density)
            val pos = textLayoutResult
                ?.getOffsetForPosition(offsetPx + translation) ?: return

            sendEditCommand(SetSelectionCommand(pos, pos))
        }

        override fun endFloatingCursor() {
            floatingCursorTranslation = null
        }

        override fun beginEditBatch() {
            editBatchDepth++
        }

        override fun endEditBatch() {
            editBatchDepth--
        }

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
            val deleteCommand = if (getState()?.selection?.collapsed == true) {
                DeleteSurroundingTextCommand(lengthBeforeCursor = 1, lengthAfterCursor = 0)
            } else {
                CommitTextCommand("", 0)
            }
            sendEditCommand(deleteCommand)
        }

        /**
         * The text position for the end of a document.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
         */
        override fun endOfDocument(): Int = getState()?.text?.length ?: 0

        /**
         * The range of selected text in a document.
         * If the text range has a length, it indicates the currently selected text.
         * If it has zero length, it indicates the caret (insertion point).
         * If the text-range object is nil, it indicates that there is no current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
         */
        override fun getSelectedTextRange(): TextRange? = getState()?.selection

        override fun setSelectedTextRange(range: TextRange?) {
            if (range != null) {
                sendEditCommand(
                    SetSelectionCommand(range.start, range.end)
                )
            } else {
                sendEditCommand(
                    SetSelectionCommand(endOfDocument(), endOfDocument())
                )
            }
        }

        override fun selectAll() {
            sendEditCommand(
                SetSelectionCommand(0, endOfDocument())
            )
        }

        /**
         * Returns the text in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
         * @param range A range of text in a document.
         * @return A substring of a document that falls within the specified range.
         */
        override fun textInRange(range: TextRange): String? {
            if (isIncorrect(range)) {
                return null
            }
            val text = getState()?.text ?: return null
            return text.substring(range.start, range.end)
        }

        /**
         * Replaces the text in a document that is in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
         * @param range A range of text in a document.
         * @param text A string to replace the text in range.
         */
        override fun replaceRange(range: TextRange, text: String) {
            sendEditCommand(
                SetComposingRegionCommand(range.start, range.end),
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
        override fun setMarkedText(markedText: String?, selectedRange: TextRange) {
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
        override fun markedTextRange(): TextRange? {
            return getState()?.composition
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
         * Returned value must be in range between 0 and length of text (inclusive).
         */
        override fun positionFromPosition(position: Int, offset: Int): Int? {
            val text = getState()?.text ?: return null

            val newPosition = position + offset
            if (newPosition == text.length || newPosition == 0) {
                return newPosition
            }
            if (newPosition < 0 || newPosition > text.length) {
                return null
            }
            var resultPosition = position
            val iterator = BreakIterator.makeCharacterInstance()
            iterator.setText(text)

            repeat(offset.absoluteValue) {
                val iteratorResult = if (offset > 0) {
                    iterator.following(resultPosition)
                } else {
                    iterator.preceding(resultPosition)
                }

                if (iteratorResult == BreakIterator.DONE) {
                    return resultPosition
                } else {
                    resultPosition = iteratorResult
                }
            }

            return resultPosition
        }

        /**
         * Returns the text position at a specified offset from another text position.
         * Returned value must be in range between 0 and length of text (inclusive).
         */
        override fun verticalPositionFromPosition(position: Int, verticalOffset: Int): Int? {
            val text = getState()?.text ?: return null
            val layoutResult = textLayoutResult ?: return null

            val line = layoutResult.getLineForOffset(position)
            val lineStartOffset = layoutResult.getLineStart(line)
            val offsetInLine = position - lineStartOffset
            val targetLine = line + verticalOffset
            return when {
                targetLine < 0 -> 0
                targetLine >= layoutResult.lineCount -> text.length
                else -> {
                    val targetLineEnd = layoutResult.getLineEnd(targetLine)
                    val lineStart = layoutResult.getLineStart(targetLine)
                    positionFromPosition(
                        lineStart, min(offsetInLine, targetLineEnd - lineStart)
                    )
                }
            }
        }

        private fun isIncorrect(range: TextRange): Boolean =
            range.start < 0 || range.end > endOfDocument() || range.start > range.end
    }
}
