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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextEditingDelegate
import androidx.compose.ui.platform.UIKitNativeTextInputContextMenuCustomAction
import androidx.compose.ui.platform.getUITextInputTraits
import androidx.compose.ui.scene.ComposeSceneFocusManager
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import platform.UIKit.UIView

internal abstract class TextInputConnection(
    protected val updateView: () -> Unit,
    protected val view: UIView,
    protected val coroutineScope: CoroutineScope,
    protected val focusedViewsList: FocusedViewsList?,
    private var focusManager: () -> ComposeSceneFocusManager?,
): TextEditingDelegate {

    private var textInputServiceInvalidationsCount = 0
    val hasInvalidations: Boolean
        get() = textInputServiceInvalidationsCount > 0

    /**
     * When `true`, [onTextFieldValueUpdated] callbacks coming back through the state snapshot flow
     * during an [edit] cycle are absorbed without notifying UITextInputDelegate. This avoids
     * resetting multi-stage IME state when UIKit itself initiated the change.
     */
    private var postponeSelectionUpdate: Boolean = false

    private var floatingCursorTranslation : Offset? = null
    private var currentRequest: PlatformTextInputMethodRequest? = null

    protected abstract val textInputView: UIView
    protected var currentTextFieldValue: TextFieldValue? = null
        private set

    protected val textLayoutResult get() = currentRequest?.textLayoutResult()
    protected val unclippedTextOffsetInRoot get() = currentRequest?.unclippedTextOffsetInRoot()
    protected val textFieldRectInRoot get() = currentRequest?.textFieldRectInRoot()

    fun start(request: PlatformTextInputMethodRequest) {
        currentRequest = request
        currentTextFieldValue = request.stateSnapshot()
        inputTraits = getUITextInputTraits(request.imeOptions)
        attachInputToView()
        showKeyboard()
    }

    protected abstract fun attachInputToView()

    open fun stop() {
        currentRequest = null
        currentTextFieldValue = null
        inputTraits = EmptyInputTraits

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

    open fun onTextFieldValueUpdated(newValue: TextFieldValue) {
        if (postponeSelectionUpdate) {
            currentTextFieldValue = newValue
            return
        }
        val internalOldValue = currentTextFieldValue
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged = textChanged || internalOldValue.selection != newValue.selection

        stateWillChange(textChanged, selectionChanged)
        currentTextFieldValue = newValue
        stateDidChange(textChanged, selectionChanged)
    }

    protected abstract fun stateWillChange(textChanged: Boolean, selectionChanged: Boolean)
    protected abstract fun stateDidChange(textChanged: Boolean, selectionChanged: Boolean)

    abstract fun onViewGeometryUpdated()

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (event.key) {
            Key.Enter -> handleEnterKey(event)
            Key.Backspace -> handleBackspace(event)
            Key.Escape -> handleEscape(event)
            else -> false
        }
    }

    abstract fun setAvailableEditMenuActions(
        copy: (() -> Unit)?,
        paste: (() -> Unit)?,
        cut: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<UIKitNativeTextInputContextMenuCustomAction>?
    )

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
        return if (currentRequest != null) {
            if (event.type == KeyEventType.KeyDown) {
                focusManager()?.releaseFocus()
            }
            true
        } else {
            false
        }
    }

    /**
     * Applies an editing operation produced by UIKit on top of the current text field state.
     *
     * The operation is executed via [PlatformTextInputMethodRequest.editText], so the change goes
     * through the regular Compose snapshot/recomposition path. While the edit runs,
     * [postponeSelectionUpdate] suppresses UITextInputDelegate notifications that would otherwise
     * fire from the snapshot subscription and reset multi-stage IME state.
     *
     * @param requireUpdateView when `true`, request an immediate redraw after the edit. Some UIKit
     * call sites (selection-only changes, marked text updates) rely on the next animation frame
     * driven by Compose's normal redraw cycle and pass `false` to avoid extra work.
     */
    private fun edit(
        requireUpdateView: Boolean = true,
        performCommand: TextEditingScope.() -> Unit
    ) {
        currentRequest?.let {
            postponeSelectionUpdate = true
            it.editText {
                performCommand()
            }
            if (requireUpdateView) {
                updateView()
            }
            onTextFieldValueUpdated(it.stateSnapshot())
            postponeSelectionUpdate = false
        }
    }

    private fun imeActionRequired(): Boolean =
        currentRequest?.imeOptions?.run {
            singleLine || (
                imeAction != ImeAction.None
                    && imeAction != ImeAction.Default
                    && !(imeAction == ImeAction.Search && _tempHardwareReturnKeyPressed)
                )
        } ?: false

    private fun runImeActionIfRequired(): Boolean {
        val imeAction = currentRequest?.imeOptions?.imeAction ?: return false
        val imeActionHandler = currentRequest?.onImeAction ?: return false
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

    override var inputTraits: SkikoUITextInputTraits = EmptyInputTraits

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
        val pos = textLayoutResult?.getOffsetForPosition(offsetPx + translation) ?: return

        edit(requireUpdateView = false) {
            setSelection(pos, pos)
        }
    }

    override fun beginFloatingCursor(offset: DpOffset) {
        val start = currentTextFieldValue?.selection?.start ?: return
        val cursorRect = textLayoutResult?.getCursorRect(start) ?: return
        floatingCursorTranslation = cursorRect.center - offset.toOffset(view.density)
    }

    override fun endFloatingCursor() {
        floatingCursorTranslation = null
    }

    override fun hasText(): Boolean = currentTextFieldValue?.text?.isNotEmpty() ?: false

    override fun insertText(text: String) {
        if (text == "\n") {
            if (runImeActionIfRequired()) {
                return
            }
        }
        edit {
            commitText(text, 1)
        }
    }

    override fun deleteBackward() {
        if (currentTextFieldValue?.selection?.collapsed == true) {
            edit {
                deleteSurroundingTextInCodePoints(1, 0)
            }
        } else {
            edit {
                commitText("", 0)
            }
        }
    }

    override fun endOfDocument(): Int = currentTextFieldValue?.text?.length ?: 0

    override fun getSelectedTextRange(): TextRange? = currentTextFieldValue?.selection

    override fun setSelectedTextRange(range: TextRange?) {
        edit(requireUpdateView = false) {
            if (range != null) {
                setSelection(range.start, range.end)
            } else {
                setSelection(endOfDocument(), endOfDocument())
            }
        }
    }

    override fun selectAll() {
        edit {
            setSelection(0, endOfDocument())
        }
    }

    override fun textInRange(range: TextRange): String? {
        if (isIncorrect(range)) {
            return null
        }
        val text = currentTextFieldValue?.text ?: return null
        return text.substring(range.start, range.end)
    }

    override fun replaceRange(range: TextRange, text: String) {
        edit {
            setComposingRegion(range.start, range.end)
            setComposingText(text, 1)
            finishComposingText()
        }
    }

    override fun setMarkedText(markedText: String?, selectedRange: TextRange) {
        if (markedText != null) {
            edit(requireUpdateView = false) {
                setComposingText(markedText, 1)
            }
        }
    }

    override fun markedTextRange(): TextRange? {
        return currentTextFieldValue?.composition
    }

    override fun unmarkText() {
        edit {
            finishComposingText()
        }
    }

    override fun positionFromPosition(position: Int, offset: Int): Int? {
        val text = currentTextFieldValue?.text ?: return null

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
        val text = currentTextFieldValue?.text ?: return null
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

    protected fun textMenuAppearanceChanged() {
        textInputServiceInvalidationsCount++
        coroutineScope.launch {
            // Time to show, hide or update state of context menu
            delay(500.milliseconds)
            textInputServiceInvalidationsCount--
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
    }
}

internal fun PlatformTextInputMethodRequest.stateSnapshot() =
    TextFieldValue(state.text, state.selection, state.composition)
