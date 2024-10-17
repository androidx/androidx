/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.collection.MutableLongSet
import androidx.compose.foundation.text.DeadKeyCombiner
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.appendCodePointX
import androidx.compose.foundation.text.cancelsTextSelection
import androidx.compose.foundation.text.input.internal.selection.TextFieldPreparedSelection
import androidx.compose.foundation.text.input.internal.selection.TextFieldPreparedSelection.Companion.NoCharacterFound
import androidx.compose.foundation.text.input.internal.selection.TextFieldPreparedSelectionState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.foundation.text.showCharacterPalette
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange

/** Factory function to create a platform specific [TextFieldKeyEventHandler]. */
internal expect fun createTextFieldKeyEventHandler(): TextFieldKeyEventHandler

/** Returns whether this key event is created by the software keyboard. */
internal expect val KeyEvent.isFromSoftKeyboard: Boolean

/**
 * Handles KeyEvents coming to a BasicTextField. This is mostly to support hardware keyboard but any
 * KeyEvent can also be sent by the IME or other platform systems.
 *
 * This class is left abstract to make sure that each platform extends from it. Platforms can decide
 * to extend or completely override KeyEvent actions defined here.
 */
internal abstract class TextFieldKeyEventHandler {
    private val preparedSelectionState = TextFieldPreparedSelectionState()
    private val deadKeyCombiner = DeadKeyCombiner()
    private val keyMapping = platformDefaultKeyMapping

    /**
     * We hold a reference to the all key down events that we receive and consume so that we can
     * also consume the corresponding key up events. Otherwise the up events get sent to the
     * ancestor nodes where the behavior is unpredictable. Please refer to b/353554186 for more
     * information.
     */
    // TODO(b/307580000) Factor this state out into a class to manage key inputs.
    private var currentlyConsumedDownKeys: MutableLongSet? = null

    open fun onPreKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        focusManager: FocusManager,
        keyboardController: SoftwareKeyboardController
    ): Boolean {
        val selection = textFieldState.visualText.selection
        return if (!selection.collapsed && event.cancelsTextSelection()) {
            textFieldSelectionState.deselect()
            true
        } else {
            false
        }
    }

    open fun onKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Unit
    ): Boolean {
        val keyCode = event.key.keyCode

        if (event.type == KeyEventType.KeyUp) {
            if (currentlyConsumedDownKeys?.contains(keyCode) == true) {
                currentlyConsumedDownKeys?.remove(keyCode)
                return true
            } else {
                return false
            }
        }

        if (event.type == KeyEventType.Unknown) {
            return false
        }

        val consumed =
            processKeyDownEvent(
                event = event,
                textFieldState = textFieldState,
                textLayoutState = textLayoutState,
                textFieldSelectionState = textFieldSelectionState,
                editable = editable,
                singleLine = singleLine,
                onSubmit = onSubmit
            )

        if (consumed) {
            // initialize if it hasn't been initialized yet.
            val currentlyConsumedDownKeys =
                currentlyConsumedDownKeys
                    ?: MutableLongSet(initialCapacity = 3).also { currentlyConsumedDownKeys = it }
            currentlyConsumedDownKeys += keyCode
        }

        return consumed
    }

    private fun processKeyDownEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Unit
    ): Boolean {
        if (event.isTypedEvent) {
            val codePoint = deadKeyCombiner.consume(event)
            if (codePoint != null) {
                val text = StringBuilder(2).appendCodePointX(codePoint).toString()
                return if (editable) {
                    textFieldState.replaceSelectedText(
                        newText = text,
                        clearComposition = true,
                        restartImeIfContentChanges = !event.isFromSoftKeyboard
                    )
                    preparedSelectionState.resetCachedX()
                    true
                } else {
                    false
                }
            }
        }

        val command = keyMapping.map(event)
        if (command == null || (command.editsText && !editable)) {
            return false
        }
        var consumed = true
        preparedSelectionContext(textFieldState, textLayoutState, event.isFromSoftKeyboard) {
            when (command) {
                KeyCommand.COPY -> textFieldSelectionState.copy(false)
                KeyCommand.PASTE -> textFieldSelectionState.paste()
                KeyCommand.CUT -> textFieldSelectionState.cut()
                KeyCommand.LEFT_CHAR -> collapseLeftOr { moveCursorLeft() }
                KeyCommand.RIGHT_CHAR -> collapseRightOr { moveCursorRight() }
                KeyCommand.LEFT_WORD -> moveCursorLeftByWord()
                KeyCommand.RIGHT_WORD -> moveCursorRightByWord()
                KeyCommand.PREV_PARAGRAPH -> moveCursorPrevByParagraph()
                KeyCommand.NEXT_PARAGRAPH -> moveCursorNextByParagraph()
                KeyCommand.UP -> moveCursorUpByLine()
                KeyCommand.DOWN -> moveCursorDownByLine()
                KeyCommand.PAGE_UP -> moveCursorUpByPage()
                KeyCommand.PAGE_DOWN -> moveCursorDownByPage()
                KeyCommand.LINE_START -> moveCursorToLineStart()
                KeyCommand.LINE_END -> moveCursorToLineEnd()
                KeyCommand.LINE_LEFT -> moveCursorToLineLeftSide()
                KeyCommand.LINE_RIGHT -> moveCursorToLineRightSide()
                KeyCommand.HOME -> moveCursorToHome()
                KeyCommand.END -> moveCursorToEnd()
                KeyCommand.DELETE_PREV_CHAR -> {
                    deleteIfSelectedOr {
                        getPrecedingCharacterIndex()
                            .takeIf { it != NoCharacterFound }
                            ?.let { TextRange(it, selection.end) }
                    }
                }
                KeyCommand.DELETE_NEXT_CHAR -> {
                    // Note that some software keyboards, such as Samsung, go through this code
                    // path instead of making calls on the InputConnection directly.
                    deleteIfSelectedOr {
                        getNextCharacterIndex()
                            .takeIf { it != NoCharacterFound }
                            ?.let { TextRange(selection.start, it) }
                    }
                }
                KeyCommand.DELETE_PREV_WORD -> {
                    deleteIfSelectedOr { TextRange(getPreviousWordOffset(), selection.end) }
                }
                KeyCommand.DELETE_NEXT_WORD -> {
                    deleteIfSelectedOr { TextRange(selection.start, getNextWordOffset()) }
                }
                KeyCommand.DELETE_FROM_LINE_START -> {
                    deleteIfSelectedOr { TextRange(getLineStartByOffset(), selection.end) }
                }
                KeyCommand.DELETE_TO_LINE_END -> {
                    deleteIfSelectedOr { TextRange(selection.start, getLineEndByOffset()) }
                }
                KeyCommand.NEW_LINE -> {
                    if (!singleLine) {
                        textFieldState.replaceSelectedText(
                            newText = "\n",
                            clearComposition = true,
                            restartImeIfContentChanges = !event.isFromSoftKeyboard
                        )
                    } else {
                        onSubmit()
                    }
                }
                KeyCommand.TAB -> {
                    if (!singleLine) {
                        textFieldState.replaceSelectedText(
                            newText = "\t",
                            clearComposition = true,
                            restartImeIfContentChanges = !event.isFromSoftKeyboard
                        )
                    } else {
                        consumed = false // let propagate to focus system
                    }
                }
                KeyCommand.SELECT_ALL -> selectAll()
                KeyCommand.SELECT_LEFT_CHAR -> moveCursorLeft().selectMovement()
                KeyCommand.SELECT_RIGHT_CHAR -> moveCursorRight().selectMovement()
                KeyCommand.SELECT_LEFT_WORD -> moveCursorLeftByWord().selectMovement()
                KeyCommand.SELECT_RIGHT_WORD -> moveCursorRightByWord().selectMovement()
                KeyCommand.SELECT_PREV_PARAGRAPH -> moveCursorPrevByParagraph().selectMovement()
                KeyCommand.SELECT_NEXT_PARAGRAPH -> moveCursorNextByParagraph().selectMovement()
                KeyCommand.SELECT_LINE_START -> moveCursorToLineStart().selectMovement()
                KeyCommand.SELECT_LINE_END -> moveCursorToLineEnd().selectMovement()
                KeyCommand.SELECT_LINE_LEFT -> moveCursorToLineLeftSide().selectMovement()
                KeyCommand.SELECT_LINE_RIGHT -> moveCursorToLineRightSide().selectMovement()
                KeyCommand.SELECT_UP -> moveCursorUpByLine().selectMovement()
                KeyCommand.SELECT_DOWN -> moveCursorDownByLine().selectMovement()
                KeyCommand.SELECT_PAGE_UP -> moveCursorUpByPage().selectMovement()
                KeyCommand.SELECT_PAGE_DOWN -> moveCursorDownByPage().selectMovement()
                KeyCommand.SELECT_HOME -> moveCursorToHome().selectMovement()
                KeyCommand.SELECT_END -> moveCursorToEnd().selectMovement()
                KeyCommand.DESELECT -> deselect()
                KeyCommand.UNDO -> {
                    textFieldState.undo()
                }
                KeyCommand.REDO -> {
                    textFieldState.redo()
                }
                KeyCommand.CHARACTER_PALETTE -> {
                    showCharacterPalette()
                }
            }
        }
        return consumed
    }

    private inline fun preparedSelectionContext(
        state: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        isFromSoftKeyboard: Boolean,
        block: TextFieldPreparedSelection.() -> Unit
    ) {
        val layoutResult = textLayoutState.layoutResult
        val visibleTextLayoutHeight = textLayoutState.getVisibleTextLayoutHeight()
        val preparedSelection =
            TextFieldPreparedSelection(
                state = state,
                textLayoutResult = layoutResult,
                isFromSoftKeyboard = isFromSoftKeyboard,
                visibleTextLayoutHeight = visibleTextLayoutHeight,
                textPreparedSelectionState = preparedSelectionState
            )
        preparedSelection.block()
        if (preparedSelection.selection != preparedSelection.initialValue.selection) {
            // selection changes are applied atomically at the end of context evaluation
            state.selectCharsIn(preparedSelection.selection)
        }
    }

    /**
     * Returns the current viewport height of TextField to help calculate where cursor should travel
     * when page down and up events are received. If the text layout is not calculated, returns
     * [Float.NaN].
     */
    private fun TextLayoutState.getVisibleTextLayoutHeight(): Float {
        return textLayoutNodeCoordinates
            ?.takeIf { it.isAttached }
            ?.let { textLayoutCoordinates ->
                decoratorNodeCoordinates
                    ?.takeIf { it.isAttached }
                    ?.localBoundingBoxOf(textLayoutCoordinates)
            }
            ?.size
            ?.height ?: Float.NaN
    }
}
