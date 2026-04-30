/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextEditingDelegate
import androidx.compose.ui.platform.getUITextInputTraits
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.BackgroundInputView
import androidx.compose.ui.window.ComposeTextInputView
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.NativeTextInputView
import androidx.compose.ui.window.OverlayInputView
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import platform.UIKit.UIView

internal abstract class TextInputConnection(
    protected val updateView: () -> Unit,
    protected val view: UIView,
    protected val coroutineScope: CoroutineScope,
    protected val focusedViewsList: FocusedViewsList?,
    override var onKeyboardPresses: (Set<*>) -> Unit,
    private var focusManager: () -> ComposeSceneFocusManager?,
): TextEditingDelegate {
    fun start(
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
        inputTraits = getUITextInputTraits(imeOptions)
        currentImeActionHandler = onImeActionPerformed
        attachInputToView()
        showKeyboard()
    }

    protected abstract fun attachInputToView()

    open fun stop() {
        flushEditCommandsIfNeeded(force = true)
        sessionEditProcessor = null
        currentOnEditCommand = null
        currentImeOptions = null
        inputTraits = EmptyInputTraits
        currentImeActionHandler = null
        textLayoutResult = null

        dismissKeyboard()

        detachView()
    }

    protected abstract fun detachView()

    fun showKeyboard() {
        focusedViewsList?.addAndFocus(textInputView)
    }

    fun dismissKeyboard() {
        focusedViewsList?.remove(textInputView, delayMillis = CLEAR_FOCUS_DELAY)
    }

    fun updateState(newValue: TextFieldValue) {
        val internalOldValue = sessionEditProcessor?.toTextFieldValue()
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged = textChanged || internalOldValue.selection != newValue.selection

        stateWillChange(textChanged, selectionChanged)

        sessionEditProcessor?.let {
            it.reset(newValue, null)
            _tempCursorPos = null
        }

        stateDidChange(textChanged, selectionChanged)
    }

    protected abstract fun stateWillChange(textChanged: Boolean, selectionChanged: Boolean)
    protected abstract fun stateDidChange(textChanged: Boolean, selectionChanged: Boolean)

    fun updateTextLayoutResult(textLayoutResult: TextLayoutResult) {
        this.textLayoutResult = textLayoutResult
    }

    open fun updateViewGeometry(
        textFieldFrame: Rect,
        unclippedTextPosition: Offset
    ) {
        textFieldFrameInRoot = textFieldFrame
        updateTextViewPosition(unclippedTextPosition)
    }
    protected abstract fun updateTextViewPosition(unclippedTextPosition: Offset)

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (event.key) {
            Key.Enter -> handleEnterKey(event)
            Key.Backspace -> handleBackspace(event)
            Key.Escape -> handleEscape(event)
            else -> false
        }
    }

    fun flushEditCommandsIfNeeded(force: Boolean = false) {
        if ((force || editBatchDepth == 0) && editCommandsBatch.isNotEmpty()) {
            val commandList = editCommandsBatch.toList()
            editCommandsBatch.clear()

            currentOnEditCommand?.invoke(commandList)
        }
    }

    val hasInvalidations: Boolean
        get() = textInputServiceInvalidationsCount > 0

    protected var textInputServiceInvalidationsCount = 0

    protected abstract val textInputView: UIView
    protected var currentOnEditCommand: ((List<EditCommand>) -> Unit)? = null
    protected var currentImeOptions: ImeOptions? = null
    override var inputTraits: SkikoUITextInputTraits = EmptyInputTraits
    protected var currentImeActionHandler: ((ImeAction) -> Unit)? = null

    protected var textFieldFrameInRoot: Rect? = null

    protected var textLayoutResult: TextLayoutResult? = null

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
    protected var sessionEditProcessor: EditProcessor? = null

    /**
     * Workaround to fix voice dictation.
     * UIKit call insertText(text) and replaceRange(range,text) immediately,
     * but Compose recomposition happen on next draw frame.
     * So the value of getSelectedTextRange is in the old state when the replaceRange function is called.
     * @see _tempCursorPos helps to fix this behaviour. Permanently update _tempCursorPos in function insertText.
     * And after clear in updateState function.
     */
    private var _tempCursorPos: Int? = null

    protected var floatingCursorTranslation : Offset? = null

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

    protected open fun sendEditCommand(vararg commands: EditCommand) {
        sessionEditProcessor?.apply(commands.toList())

        editCommandsBatch.addAll(commands)
        flushEditCommandsIfNeeded()
    }

    protected fun getState(): TextFieldValue? = sessionEditProcessor?.toTextFieldValue()

    protected fun getCursorPos(): Int? {
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

    /**
     * Returns true if there is a focused view in the window hierarchy that is an external
     * text input — i.e. a native UITextField or UITextView inserted via interop, not one of
     * Compose's own input views.
     *
     * Used to distinguish the case where the user tapped a native interop text field (in which
     * case Compose focus should be released) from the case where focus simply moved to another
     * Compose text field (in which case Compose handles focus internally and no action is needed).
     */
    private fun hasFocusedExternalInputViewInWindowHierarchy(): Boolean {
        fun hasFocusedExternalInputView(view: UIView): Boolean {
            if (view.isFirstResponder) {
                return view !is NativeTextInputView &&
                    view !is ComposeTextInputView &&
                    view !is OverlayInputView &&
                    view !is BackgroundInputView
            }
            return view.subviews.any { it is UIView && hasFocusedExternalInputView(it) }
        }
        return view.window?.let { hasFocusedExternalInputView(it) } ?: false
    }

    override fun onResignFocus() {
        textInputServiceInvalidationsCount++
        coroutineScope.launch {
            if (hasFocusedExternalInputViewInWindowHierarchy()) {
                focusManager()?.releaseFocus()
            }
            textInputServiceInvalidationsCount--
        }
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

    override fun hasText(): Boolean = getState()?.text?.isNotEmpty() ?: false

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

    override fun deleteBackward() {
        val deleteCommand = if (getState()?.selection?.collapsed == true) {
            DeleteSurroundingTextCommand(lengthBeforeCursor = 1, lengthAfterCursor = 0)
        } else {
            CommitTextCommand("", 0)
        }
        sendEditCommand(deleteCommand)
    }

    override fun endOfDocument(): Int = getState()?.text?.length ?: 0

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

    override fun textInRange(range: TextRange): String? {
        if (isIncorrect(range)) {
            return null
        }
        val text = getState()?.text ?: return null
        return text.substring(range.start, range.end)
    }

    override fun replaceRange(range: TextRange, text: String) {
        sendEditCommand(
            SetComposingRegionCommand(range.start, range.end),
            SetComposingTextCommand(text, 1),
            FinishComposingTextCommand(),
        )
    }

    override fun setMarkedText(markedText: String?, selectedRange: TextRange) {
        if (markedText != null) {
            sendEditCommand(
                SetComposingTextCommand(markedText, 1)
            )
        }
    }

    override fun markedTextRange(): TextRange? {
        return getState()?.composition
    }

    override fun unmarkText() {
        sendEditCommand(FinishComposingTextCommand())
    }

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

    protected fun isIncorrect(range: TextRange): Boolean {
        return range.start < 0 || range.end > endOfDocument() || range.start > range.end
    }

    companion object {
        // Due to unexpected delays between the commands to show/hide the keyboard,
        // it may jump when switching between text fields.
        // Adding a delay to the 'resignFirstResponder' function call to eliminate this issue.
        internal const val CLEAR_FOCUS_DELAY: Long = 10L

        internal val NoOpOnKeyboardPresses: (Set<*>) -> Unit = {}
    }
}
