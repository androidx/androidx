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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.usingNativeTextInput
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.BackgroundInputView
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.IntermediateTextInputUIView
import androidx.compose.ui.window.IntermediateTextScrollView
import androidx.compose.ui.window.OverlayInputView
import androidx.compose.ui.window.PlatformTextLayoutDirection
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
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

@Suppress("DEPRECATION") // TODO https://youtrack.jetbrains.com/issue/CMP-9858
internal class UIKitTextInputService(
    private val updateView: UIKitTextInputService.() -> Unit,
    private val view: UIView,
    private val viewConfiguration: ViewConfiguration,
    private val focusedViewsList: FocusedViewsList?,
    private var onInputStarted: () -> Unit,
    /**
     * Callback to handle keyboard presses. The parameter is a [Set] of [UIPress] objects.
     * Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    private var onKeyboardPresses: (Set<*>) -> Unit,
    private var focusManager: () -> ComposeSceneFocusManager?,
    coroutineContext: kotlin.coroutines.CoroutineContext
) : androidx.compose.ui.text.input.PlatformTextInputService, TextToolbar, UIKitNativeTextInputContext {

    private val coroutineScope = CoroutineScope(coroutineContext)

    var usingNativeTextInput by mutableStateOf(false)
        private set

    private var currentOnEditCommand: ((List<EditCommand>) -> Unit)? = null
    private var currentImeOptions: ImeOptions? = null
    private var currentImeActionHandler: ((ImeAction) -> Unit)? = null
    private var textUIView: IntermediateTextInputUIView? = null
    private val scrollView by lazy { IntermediateTextScrollView() }
    private var textLayoutResult: TextLayoutResult? = null

    private var currentFocusedRect: Rect? = null

    /**
     * Matches DefaultCursorThickness
     *
     * Must be at least 1.dp to make caret interactable
     */
    private val cursorThickness = 2.dp

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
        usingNativeTextInput = imeOptions.platformImeOptions?.usingNativeTextInput ?: false
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
        textLayoutResult = null

        hideSoftwareKeyboard()

        textUIView?.inputTraits = EmptyInputTraits
        textUIView?.input = null

        detachIntermediateTextInputView()
        usingNativeTextInput = false

        selectionTintColor = null
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
        if (!usingNativeTextInput && (textChanged || selectionChanged)) {
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
        if (usingNativeTextInput) {
            textFieldFrameInRoot = rect
        } else {
            textUIView?.setFrame(rect.toDpRect(view.density).asCGRect())
        }
        showMenuOrUpdatePosition()
    }

    private fun calculateContentBounds(textLayoutResult: TextLayoutResult, textFieldFrame: Rect, unclippedTextPosition: Offset): Rect {
        val textSize = textLayoutResult.size.toSize()
        val contentBounds = Rect(
            offset = Offset(x = textFieldFrame.left - unclippedTextPosition.x, y = textFieldFrame.top - unclippedTextPosition.y),
            size = textSize
        )
        return contentBounds
    }

    private fun calculateContentInsets(textFieldFrame: Rect, contentBounds: Rect): DpInsets = with(view.density) {
        return DpInsets(
            left = max(0f, -contentBounds.left).toDp(),
            top = max(0f, -contentBounds.top).toDp(),
            right = max(0f, textFieldFrame.width - contentBounds.width + contentBounds.left).toDp(),
            bottom = max(0f, textFieldFrame.height - contentBounds.height + contentBounds.top).toDp()
        )
    }

    fun updateFocusedRect(rect: Rect) {
        currentFocusedRect = rect
    }

    private var textFieldFrameInRoot: Rect? = null
    private var clippingTextFrame: Rect? = null
    private var currentContentBounds: Rect? = null
    private var currentContentInsets: DpInsets? = null
    fun updateClippingTextFrame(rect: Rect) {
        clippingTextFrame = rect
    }

    fun updateUnclippedTextPosition(offset: Offset) {
        if (usingNativeTextInput) {
            // Since Compose content is rendered on a MetalView and the UITextInput-implementing
            // view is overlayed on top of it, we need to synchronize the Compose text
            // field with the IntermediateTextScrollView (which contains IntermediateUITextView)
            // to ensure native iOS text input controls
            // align correctly with the rendered text.
            val rect = textFieldFrameInRoot ?: return
            val layoutResult = textLayoutResult ?: return
            val contentBounds = calculateContentBounds(
                layoutResult,
                rect,
                offset
            )
            currentContentBounds = contentBounds
            val contentInsets = calculateContentInsets(rect, contentBounds)
            currentContentInsets = contentInsets
            scrollView.setFrame(
                rect.toDpRect(view.density),
                contentBounds.toDpRect(view.density),
                contentInsets
            )
        }
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

        if (usingNativeTextInput) {
            // For Native Text Input it's essential to trigger view update right after send edit command,
            // otherwise UIKit calls may use an invalid layout state
            coroutineScope.launch {
                updateView()
            }
        }
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
        coroutineScope.launch {
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
        showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
            onAutofillRequested = null
        )
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?
    ) {
        if (!usingNativeTextInput) {
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
                    textUIView.showEditMenuAtRect(
                        targetRect = target,
                        copy = onCopyRequested,
                        cut = onCutRequested,
                        paste = onPasteRequested,
                        selectAll = onSelectAllRequested,
                        customActions = emptyList<CMPEditMenuCustomAction>()
                    )
                    textMenuAppearanceChanged()
                }
            }
            showMenuOrUpdatePosition()
        }
    }

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

    override fun updateNativeTextInputEditMenuState(
        copy: (() -> Unit)?,
        paste: (() -> Unit)?,
        cut: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<UIKitNativeTextInputContextMenuCustomAction>?
    ) {
        // This path is native text input only, shouldn't be called elsewhere
        textUIView?.updateMenuActions(
            copy,
            paste,
            cut,
            selectAll,
            customActions?.map { action ->
                CMPEditMenuCustomAction(action.title, action.action)
            } ?: emptyList()
        )
    }

    override fun usingNativeTextInput(): Boolean = usingNativeTextInput

    // If not specified, iOS would use the default system tint color
    private var selectionTintColor: Color? = null
    private fun setupTintColor() {
        textUIView?.let {
            val uiColor = selectionTintColor?.toUIColor()
            it.setTintColor(uiColor)
        }
    }

    override fun updateNativeTextInputTintColor(color: Color?) {
        selectionTintColor = color
        setupTintColor()
    }

    override val status: TextToolbarStatus
        get() = if (usingNativeTextInput) {
            // In this case the menu is controlled by UIKit.
            TextToolbarStatus.Hidden
        } else {
            if (textUIView?.isTextMenuShown() == true)
                TextToolbarStatus.Shown
            else
                TextToolbarStatus.Hidden
        }

    private fun attachIntermediateTextInputView() {
        detachIntermediateTextInputView()
        if (usingNativeTextInput) {
            textUIView = IntermediateTextInputUIView(
                doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
                usingNativeTextInput = usingNativeTextInput,
                coroutineScope = coroutineScope
            ).also {
                view.addSubview(scrollView)
                scrollView.textView = it

                it.onKeyboardPresses = onKeyboardPresses
                it.clipsToBounds = false
                it.input = createSkikoInput()
                it.inputTraits = getUITextInputTraits(currentImeOptions)

                // Resizing should be done later
                it.resignFirstResponder()
                it.becomeFirstResponder()
            }
            setupTintColor()
        } else {
            textUIView = IntermediateTextInputUIView(
                doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
                usingNativeTextInput = usingNativeTextInput,
                coroutineScope = coroutineScope
            ).also {
                it.setAutoresizingMask(
                    UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
                )
                it.onKeyboardPresses = onKeyboardPresses
                view.addSubview(it)
                it.setFrame(view.bounds)
            }
        }
    }

    private fun detachIntermediateTextInputView() {
        // Out-of-bounds non-empty frame is required to hide text keyboard focus frame
        val outOfBoundsFrame = CGRectMake(-100000.0, 0.0, 1.0, 1.0)

        if (usingNativeTextInput) {
            textUIView?.input = null
            textUIView?.inputTraits = EmptyInputTraits

            textUIView?.let { textView ->
                textView.setFrame(outOfBoundsFrame)

                textView.resetOnKeyboardPressesCallback()
                coroutineScope.launch {
                    delay(CLEAR_FOCUS_DELAY)
                    if (scrollView.textView == textView) {
                        scrollView.textView = null
                        textView.removeFromSuperview()
                    }
                }
            }
            textUIView = null
            scrollView.removeFromSuperview()
        } else {
            showMenuOrUpdatePosition = {}
            textUIView?.let { view ->
                view.setFrame(outOfBoundsFrame)

                view.resetOnKeyboardPressesCallback()
                coroutineScope.launch {
                    delay(CLEAR_FOCUS_DELAY)
                    view.removeFromSuperview()
                }
            }
            textUIView = null
        }
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
            coroutineScope.launch {
                if (hasFocusedNonComposeInputViewInWindowHierarchy()) {
                    focusManager()?.releaseFocus()
                }
                textInputServiceInvalidationsCount--
            }
        }

        override fun beginFloatingCursor(offset: DpOffset) {
            val cursorPos = if (usingNativeTextInput) {
                getState()?.selection?.start ?: return
            } else getCursorPos() ?: getState()?.selection?.start ?: return
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

        override fun caretDpRectForPosition(position: Int): DpRect? {
            val text = getState()?.text ?: return null
            if (position < 0 || position > text.length) {
                return null
            }
            val currentTextLayoutResult = textLayoutResult ?: return null
            if (position > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) {
                return null
            }
            val rect = currentTextLayoutResult.getCursorRect(position)
            return rect.toDpRect(view.density).let {
                val hafWidth = cursorThickness / 2
                val center = (it.left + it.right) / 2
                it.copy(left = center - hafWidth, right = center + hafWidth)
            }
        }

        override fun selectionDpRectsForRange(range: TextRange): List<TextSelectionRect> {
            // Native selection rects are required for correct work of the text editing menu
            // Without them, it will be impossible to call the text editing menu by tapping on the selected area
            if (range.collapsed || isIncorrect(range)) {
                return emptyList()
            }
            val currentTextLayoutResult = textLayoutResult ?: return emptyList()

            val startSelectionHandleRect = currentTextLayoutResult.getCursorRect(range.start)
            val endSelectionHandleRect = currentTextLayoutResult.getCursorRect(range.end)

            val firstLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
            val lastLineNumber = currentTextLayoutResult.getLineForOffset(range.end)

            return if (firstLineNumber == lastLineNumber) {
                listOf(
                    TextSelectionRect(
                        dpRect = Rect(
                            topLeft = startSelectionHandleRect.topLeft,
                            bottomRight = endSelectionHandleRect.bottomRight
                        ).toDpRect(view.density),
                        writingDirection = TextDirection.Content,
                        containsStart = true,
                        containsEnd = true,
                        isVertical = false
                    )
                )
            } else {
                // TODO Consider RTL Layout
                // We require separate rects for start line, end line and everything in between them
                val contentInsets = currentContentInsets ?: return emptyList()
                val contentRect = currentContentBounds?.let {
                    with(view.density) {
                        Rect(
                            top = it.top + contentInsets.top.toPx(),
                            left = it.left + contentInsets.left.toPx(),
                            right = it.right + contentInsets.right.toPx(),
                            bottom = it.bottom + contentInsets.bottom.toPx()
                        )
                    }
                } ?: return emptyList()

                val firstLineSelectionRect = TextSelectionRect(
                    dpRect = Rect(
                        top = startSelectionHandleRect.top,
                        left = startSelectionHandleRect.left,
                        right = contentRect.right,
                        bottom = startSelectionHandleRect.bottom
                    ).toDpRect(view.density),
                    writingDirection = TextDirection.Content,
                    containsStart = true,
                    containsEnd = false,
                    isVertical = false
                )

                val middleAreaSelectionRect = TextSelectionRect(
                    dpRect = Rect(
                        top = startSelectionHandleRect.bottom,
                        left = contentRect.left,
                        right = contentRect.right,
                        bottom = endSelectionHandleRect.top
                    ).toDpRect(view.density),
                    writingDirection = TextDirection.Content,
                    containsStart = false,
                    containsEnd = false,
                    isVertical = false
                )

                val lastLineStartRect = currentTextLayoutResult.getCursorRect(
                    currentTextLayoutResult.getLineStart(lastLineNumber)
                )
                val lastLineRect = TextSelectionRect(
                    dpRect = Rect(
                        topLeft = lastLineStartRect.topLeft,
                        bottomRight = endSelectionHandleRect.bottomRight
                    ).toDpRect(view.density),
                    writingDirection = TextDirection.Content,
                    containsStart = false,
                    containsEnd = true,
                    isVertical = false
                )

                listOf(
                    firstLineSelectionRect,
                    middleAreaSelectionRect,
                    lastLineRect
                )
            }
        }

        override fun firstSelectionRectForRange(range: TextRange): DpRect? {
            if (range.collapsed || isIncorrect(range)) {
                return null
            }
            val currentTextLayoutResult = textLayoutResult ?: return null

            val startHandleLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
            val endHandleLineNumber = currentTextLayoutResult.getLineForOffset(range.end)

            val startHandleRect = currentTextLayoutResult.getCursorRect(range.start)

            return if (startHandleLineNumber == endHandleLineNumber) {
                Rect(
                    topLeft = startHandleRect.topLeft,
                    bottomRight = currentTextLayoutResult.getCursorRect(range.end).bottomRight
                ).toDpRect(view.density)
            } else {
                val startLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
                val startLineRight = currentTextLayoutResult.getLineRight(startLineNumber)
                Rect(
                    startHandleRect.left,
                    startHandleRect.top,
                    startLineRight,
                    startHandleRect.bottom
                ).toDpRect(view.density)
            }
        }

        override fun closestPositionToPoint(point: DpOffset): Int? {
            return textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
        }

        override fun closestPositionToPoint(point: DpOffset, withinRange: TextRange): Int? {
            val pointOffset =
                textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
                    ?: return null
            return pointOffset.coerceIn(withinRange.start, withinRange.end)
        }

        override fun characterRangeAtPoint(point: DpOffset): TextRange? {
            val pointOffset =
                textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
                    ?: return null
            return textLayoutResult?.getWordBoundary(pointOffset)
        }

        override fun positionWithinRange(
            range: TextRange,
            farthestInDirection: PlatformTextLayoutDirection
        ): Int? {
            if (isIncorrect(range)) return null
            return when (farthestInDirection) {
                PlatformTextLayoutDirection.Up -> range.start
                PlatformTextLayoutDirection.Down -> range.end
                else -> {
                    val layout = textLayoutResult ?: return null
                    val startLine = layout.getLineForOffset(range.start)
                    val endLine = layout.getLineForOffset(range.end)
                    val lineRange = startLine..endLine

                    when (farthestInDirection) {
                        PlatformTextLayoutDirection.Left -> {
                            lineRange.minByOrNull {
                                when (it) {
                                    startLine -> layout.getHorizontalPosition(range.start, true)
                                    else -> layout.getLineLeft(it)
                                }
                            }
                        }
                        PlatformTextLayoutDirection.Right -> {
                            lineRange.maxByOrNull {
                                when (it) {
                                    endLine -> layout.getHorizontalPosition(range.end, true)
                                    else -> layout.getLineRight(it)
                                }
                            }
                        }
                        else -> null
                    }
                }
            }
        }

        private fun isIncorrect(range: TextRange): Boolean {
            return range.start < 0 || range.end > endOfDocument() || range.start > range.end
        }
    }
}

internal data class TextSelectionRect(
    val dpRect: DpRect,
    val writingDirection: TextDirection,
    val containsStart: Boolean,
    val containsEnd: Boolean,
    val isVertical: Boolean
)

// Insets in DP
internal data class DpInsets(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp)

