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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.DeadKeyCombiner
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.appendCodePointX
import androidx.compose.foundation.text.cancelsTextSelection
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.foundation.text.showCharacterPalette
import androidx.compose.foundation.text2.input.internal.TextFieldPreparedSelection.Companion.NoCharacterFound
import androidx.compose.foundation.text2.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * Factory function to create a platform specific [TextFieldKeyEventHandler].
 */
internal expect fun createTextFieldKeyEventHandler(): TextFieldKeyEventHandler

/**
 * Handles KeyEvents coming to a BasicTextField2. This is mostly to support hardware keyboard but
 * any KeyEvent can also be sent by the IME or other platform systems.
 *
 * This class is left abstract to make sure that each platform extends from it. Platforms can
 * decide to extend or completely override KeyEvent actions defined here.
 */
@OptIn(ExperimentalFoundationApi::class)
internal abstract class TextFieldKeyEventHandler {
    private val preparedSelectionState = TextFieldPreparedSelectionState()
    private val deadKeyCombiner = DeadKeyCombiner()
    private val keyMapping = platformDefaultKeyMapping

    open fun onPreKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        focusManager: FocusManager,
        keyboardController: SoftwareKeyboardController
    ): Boolean {
        val selection = textFieldState.text.selectionInChars
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
        if (event.type != KeyEventType.KeyDown) {
            return false
        }

        if (event.isTypedEvent) {
            val codePoint = deadKeyCombiner.consume(event)
            if (codePoint != null) {
                val text = StringBuilder(2).appendCodePointX(codePoint).toString()
                return if (editable) {
                    textFieldState.editUntransformedTextAsUser {
                        commitComposition()
                        commitText(text, 1)
                    }
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
        preparedSelectionContext(textFieldState, textLayoutState) {
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
                KeyCommand.DELETE_PREV_CHAR ->
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            deleteSurroundingText(
                                selection.end - getPrecedingCharacterIndex(),
                                0
                            )
                        }
                    }
                KeyCommand.DELETE_NEXT_CHAR -> {
                    // Note that some software keyboards, such as Samsung, go through this code
                    // path instead of making calls on the InputConnection directly.
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            val nextCharacterIndex = getNextCharacterIndex()
                            // If there's no next character, it means the cursor is at the end of the
                            // text, and this should be a no-op. See b/199919707.
                            if (nextCharacterIndex != NoCharacterFound) {
                                deleteSurroundingText(0, nextCharacterIndex - selection.end)
                            }
                        }
                    }
                }

                KeyCommand.DELETE_PREV_WORD ->
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            getPreviousWordOffset()?.let {
                                deleteSurroundingText(selection.end - it, 0)
                            }
                        }
                    }
                KeyCommand.DELETE_NEXT_WORD ->
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            getNextWordOffset()?.let {
                                deleteSurroundingText(0, it - selection.end)
                            }
                        }
                    }
                KeyCommand.DELETE_FROM_LINE_START ->
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            getLineStartByOffset()?.let {
                                deleteSurroundingText(selection.end - it, 0)
                            }
                        }
                    }
                KeyCommand.DELETE_TO_LINE_END ->
                    textFieldState.editUntransformedTextAsUser {
                        if (!deleteIfSelected()) {
                            getLineEndByOffset()?.let {
                                deleteSurroundingText(0, it - selection.end)
                            }
                        }
                    }
                KeyCommand.NEW_LINE ->
                    if (!singleLine) {
                        textFieldState.editUntransformedTextAsUser {
                            commitComposition()
                            commitText("\n", 1)
                        }
                    } else {
                        onSubmit()
                    }

                KeyCommand.TAB ->
                    if (!singleLine) {
                        textFieldState.editUntransformedTextAsUser {
                            commitComposition()
                            commitText("\t", 1)
                        }
                    } else {
                        consumed = false // let propagate to focus system
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
        block: TextFieldPreparedSelection.() -> Unit
    ) {
        val preparedSelection = TextFieldPreparedSelection(
            state = state,
            textLayoutState = textLayoutState,
            textPreparedSelectionState = preparedSelectionState
        )
        preparedSelection.block()
        if (preparedSelection.selection != preparedSelection.initialValue.selectionInChars) {
            // selection changes are applied atomically at the end of context evaluation
            state.selectCharsIn(preparedSelection.selection)
        }
    }
}
