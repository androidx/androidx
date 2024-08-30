/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.text.input

import androidx.annotation.RestrictTo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.text.AtomicReference
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutResult

/**
 * Handles communication with the IME. Informs about the IME changes via [EditCommand]s and
 * provides utilities for working with software keyboard.
 *
 * This class is responsible for ensuring there is only one open [TextInputSession] which will
 * interact with software keyboards. Start new a TextInputSession by calling [startInput] and
 * close it with [stopInput].
 */
// Open for testing purposes.
open class TextInputService(private val platformTextInputService: PlatformTextInputService) {
    private val _currentInputSession: AtomicReference<TextInputSession?> =
        AtomicReference(null)

    internal val currentInputSession: TextInputSession?
        get() = _currentInputSession.get()

    /**
     * Start text input session for given client.
     *
     * If there is a previous [TextInputSession] open, it will immediately be closed by this call
     * to [startInput].
     *
     * @param value initial [TextFieldValue]
     * @param imeOptions IME configuration
     * @param onEditCommand callback to inform about changes requested by IME
     * @param onImeActionPerformed callback to inform if an IME action such as [ImeAction.Done]
     * etc occurred.
     */
    open fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ): TextInputSession {
        platformTextInputService.startInput(
            value,
            imeOptions,
            onEditCommand,
            onImeActionPerformed
        )
        val nextSession = TextInputSession(this, platformTextInputService)
        _currentInputSession.set(nextSession)
        return nextSession
    }

    /**
    * Restart input and show the keyboard. This should only be called when starting a new
    * `PlatformTextInputModifierNode.textInputSession`.
    */
    @InternalTextApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun startInput() {
        platformTextInputService.startInput()
        val nextSession = TextInputSession(this, platformTextInputService)
        _currentInputSession.set(nextSession)
    }

    /**
     * Stop text input session.
     *
     * If the [session] is not the currently open session, no action will occur.
     *
     * @param session the session returned by [startInput] call.
     */
    open fun stopInput(session: TextInputSession) {
        if (_currentInputSession.compareAndSet(session, null)) {
            platformTextInputService.stopInput()
        }
    }

    @InternalTextApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun stopInput() {
        platformTextInputService.stopInput()
    }

    /**
     * Request showing onscreen keyboard.
     *
     * This call will be ignored if there is not an open [TextInputSession], as it means there is
     * nothing that will accept typed input. The most common way to open a TextInputSession is to
     * set the focus to an editable text composable.
     *
     * There is no guarantee that the keyboard will be shown. The software keyboard or
     * system service may silently ignore this request.
     */
    @Deprecated(
        message = "Use SoftwareKeyboardController.show or " +
            "TextInputSession.showSoftwareKeyboard instead.",
        replaceWith = ReplaceWith("textInputSession.showSoftwareKeyboard()")
    )
    // TODO(b/183448615) @InternalTextApi
    fun showSoftwareKeyboard() {
        if (currentInputSession != null) {
            platformTextInputService.showSoftwareKeyboard()
        }
    }

    /**
     * Hide onscreen keyboard.
     */
    @Deprecated(
        message = "Use SoftwareKeyboardController.hide or " +
            "TextInputSession.hideSoftwareKeyboard instead.",
        replaceWith = ReplaceWith("textInputSession.hideSoftwareKeyboard()")
    )
    // TODO(b/183448615) @InternalTextApi
    fun hideSoftwareKeyboard(): Unit = platformTextInputService.hideSoftwareKeyboard()
}

/**
 * Represents a input session for interactions between a soft keyboard and editable text.
 *
 * This session may be closed at any time by [TextInputService] or by calling [dispose], after
 * which [isOpen] will return false and all further calls will have no effect.
 */
@Deprecated("Use PlatformTextInputModifierNode instead.")
class TextInputSession(
    private val textInputService: TextInputService,
    private val platformTextInputService: PlatformTextInputService
) {
    /**
     * If this session is currently open.
     *
     * A session may be closed at any time by [TextInputService] or by calling [dispose].
     */
    val isOpen: Boolean
        get() = textInputService.currentInputSession == this

    /**
     * Close this input session.
     *
     * All further calls to this object will have no effect, and [isOpen] will return false.
     *
     * Note, [TextInputService] may also close this input session at any time without calling
     * dispose. Calling dispose after this session has been closed has no effect.
     */
    fun dispose() {
        textInputService.stopInput(this)
    }

    /**
     * Execute [block] if [isOpen] is true.
     *
     * This function will only check [isOpen] once, and may execute the action after the input
     * session closes in the case of concurrent execution.
     *
     * @param block action to take if isOpen
     * @return true if an action was performed
     */
    private inline fun ensureOpenSession(block: () -> Unit): Boolean {
        return isOpen.also { applying ->
            if (applying) {
                block()
            }
        }
    }

    /**
     * Notify the focused rectangle to the system.
     *
     * The system can ignore this information or use it to for additional functionality.
     *
     * For example, desktop systems show a popup near the focused input area (for some languages).
     *
     * If the session is not open, no action will be performed.
     *
     * @param rect the rectangle that describes the boundaries on the screen that requires focus
     * @return false if this session expired and no action was performed
     */
    fun notifyFocusedRect(rect: Rect): Boolean = ensureOpenSession {
        platformTextInputService.notifyFocusedRect(rect)
    }

    /**
     * Notify the input service of layout and position changes.
     *
     * @param textFieldValue the text field's [TextFieldValue]
     * @param offsetMapping the offset mapping for the visual transformation
     * @param textLayoutResult the text field's [TextLayoutResult]
     * @param textFieldToRootTransform function that modifies a matrix to be a transformation matrix
     *   from local coordinates to the root composable coordinates
     * @param innerTextFieldBounds visible bounds of the text field in text layout coordinates, or
     *   an empty rectangle if the text field is not visible
     * @param decorationBoxBounds visible bounds of the decoration box in text layout coordinates,
     *   or an empty rectangle if the decoration box is not visible
     */
    fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) = ensureOpenSession {
        platformTextInputService.updateTextLayoutResult(
            textFieldValue,
            offsetMapping,
            textLayoutResult,
            textFieldToRootTransform,
            innerTextFieldBounds,
            decorationBoxBounds
        )
    }

    /**
     * Notify IME about the new [TextFieldValue] and latest state of the editing buffer. [oldValue]
     * is the state of the buffer before the changes applied by the [newValue].
     *
     * [oldValue] represents the changes that was requested by IME on the buffer, and [newValue]
     * is the final state of the editing buffer that was requested by the application. In cases
     * where [oldValue] is not equal to [newValue], it would mean the IME suggested value is
     * rejected, and the IME connection will be restarted with the newValue.
     *
     * If the session is not open, action will be performed.
     *
     * @param oldValue the value that was requested by IME on the buffer
     * @param newValue final state of the editing buffer that was requested by the application
     * @return false if this session expired and no action was performed
     */
    fun updateState(
        oldValue: TextFieldValue?,
        newValue: TextFieldValue
    ): Boolean = ensureOpenSession {
        platformTextInputService.updateState(oldValue, newValue)
    }

    /**
     * Request showing onscreen keyboard.
     *
     * This call will have no effect if this session is not open.
     *
     * This should be used instead of [TextInputService.showSoftwareKeyboard] when implementing a
     * new editable text composable to show the keyboard in response to events related to that
     * composable.
     *
     * There is no guarantee that the keyboard will be shown. The software keyboard or
     * system service may silently ignore this request.
     *
     * @return false if this session expired and no action was performed
     */
    // TODO(b/241399013) Deprecate when out of API freeze.
    // @Deprecated("Use SoftwareKeyboardController.show() instead.")
    fun showSoftwareKeyboard(): Boolean = ensureOpenSession {
        platformTextInputService.showSoftwareKeyboard()
    }

    /**
     * Hide onscreen keyboard for a specific [TextInputSession].
     *
     * This call will have no effect if this session is not open.
     *
     * This should be used instead of [TextInputService.showSoftwareKeyboard] when implementing a
     * new editable text composable to hide the keyboard in response to events related to that
     * composable.
     *
     * @return false if this session expired and no action was performed
     */
    // TODO(b/241399013) Deprecate when out of API freeze.
    // @Deprecated("Use SoftwareKeyboardController.hide() instead.")
    fun hideSoftwareKeyboard(): Boolean = ensureOpenSession {
        platformTextInputService.hideSoftwareKeyboard()
    }
}

/**
 * Platform specific text input service.
 */
interface PlatformTextInputService {
    /**
     * Start text input session for given client.
     *
     * @see TextInputService.startInput
     */
    fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    )

    /**
     * Restart input and show the keyboard. This should only be called when starting a new
     * `PlatformTextInputModifierNode.textInputSession`.
     *
     * @see TextInputService.startInput
     */
    fun startInput() {}

    /**
     * Stop text input session.
     *
     * @see TextInputService.stopInput
     */
    fun stopInput()

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     *
     * @see TextInputService.showSoftwareKeyboard
     */
    fun showSoftwareKeyboard()

    /**
     * Hide software keyboard
     *
     * @see TextInputService.hideSoftwareKeyboard
     */
    fun hideSoftwareKeyboard()

    /**
     * Notify the new editor model to IME.
     *
     * @see TextInputSession.updateState
     */
    fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue)

    /**
     * Notify the focused rectangle to the system.
     *
     * The system can ignore this information or use it to for additional functionality.
     *
     * For example, desktop systems show a popup near the focused input area (for some languages).
     */
    // TODO(b/262648050) Try to find a better API.
    fun notifyFocusedRect(rect: Rect) {
    }

    /**
     * Notify the input service of layout and position changes.
     *
     * @see TextInputSession.updateTextLayoutResult
     */
    fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
    }
}
