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
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.foundation.text.showCharacterPalette
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.TextFieldPreparedSelection.Companion.NoCharacterFound
import androidx.compose.foundation.text2.input.selectCharsIn
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

/**
 * Handles KeyEvents coming to a BasicTextField. This is mostly to support hardware keyboard but
 * any KeyEvent can also be sent by the IME or other platform systems.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldKeyEventHandler {
    private val preparedSelectionState = TextFieldPreparedSelectionState()
    private val deadKeyCombiner = DeadKeyCombiner()
    private val keyMapping = platformDefaultKeyMapping
    private var filter: TextEditFilter? = null

    fun setFilter(filter: TextEditFilter?) {
        this.filter = filter
    }

    fun onKeyEvent(
        event: KeyEvent,
        state: TextFieldState,
        textLayoutState: TextLayoutState,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Unit
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) {
            return false
        }
        val editCommand = event.toTypedEditCommand()
        if (editCommand != null) {
            return if (editable) {
                editCommand.applyOnto(state)
                preparedSelectionState.resetCachedX()
                true
            } else {
                false
            }
        }
        val command = keyMapping.map(event)
        if (command == null || (command.editsText && !editable)) {
            return false
        }
        var consumed = true
        preparedSelectionContext(state, textLayoutState) {
            when (command) {
                // TODO(halilibo): implement after selection is supported.
                KeyCommand.COPY, // -> selectionManager.copy(false)
                    // TODO(siyamed): cut & paste will cause a reset input
                KeyCommand.PASTE, // -> selectionManager.paste()
                KeyCommand.CUT -> moveCursorRight() // selectionManager.cut()
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
                    deleteIfSelectedOr {
                        DeleteSurroundingTextCommand(
                            selection.end - getPrecedingCharacterIndex(),
                            0
                        )
                    }?.applyOnto(state)

                KeyCommand.DELETE_NEXT_CHAR -> {
                    // Note that some software keyboards, such as Samsungs, go through this code
                    // path instead of making calls on the InputConnection directly.
                    deleteIfSelectedOr {
                        val nextCharacterIndex = getNextCharacterIndex()
                        // If there's no next character, it means the cursor is at the end of the
                        // text, and this should be a no-op. See b/199919707.
                        if (nextCharacterIndex != NoCharacterFound) {
                            DeleteSurroundingTextCommand(0, nextCharacterIndex - selection.end)
                        } else {
                            null
                        }
                    }?.applyOnto(state)
                }

                KeyCommand.DELETE_PREV_WORD ->
                    deleteIfSelectedOr {
                        getPreviousWordOffset()?.let {
                            DeleteSurroundingTextCommand(selection.end - it, 0)
                        }
                    }?.applyOnto(state)

                KeyCommand.DELETE_NEXT_WORD ->
                    deleteIfSelectedOr {
                        getNextWordOffset()?.let {
                            DeleteSurroundingTextCommand(0, it - selection.end)
                        }
                    }?.applyOnto(state)

                KeyCommand.DELETE_FROM_LINE_START ->
                    deleteIfSelectedOr {
                        getLineStartByOffset()?.let {
                            DeleteSurroundingTextCommand(selection.end - it, 0)
                        }
                    }?.applyOnto(state)

                KeyCommand.DELETE_TO_LINE_END ->
                    deleteIfSelectedOr {
                        getLineEndByOffset()?.let {
                            DeleteSurroundingTextCommand(0, it - selection.end)
                        }
                    }?.applyOnto(state)

                KeyCommand.NEW_LINE ->
                    if (!singleLine) {
                        CommitTextCommand("\n", 1).applyOnto(state)
                    } else {
                        onSubmit()
                    }

                KeyCommand.TAB ->
                    if (!singleLine) {
                        CommitTextCommand("\t", 1).applyOnto(state)
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
                    // undoManager?.makeSnapshot(value)
                    // undoManager?.undo()?.let { this@TextFieldKeyInput.onValueChange(it) }
                }

                KeyCommand.REDO -> {
                    // undoManager?.redo()?.let { this@TextFieldKeyInput.onValueChange(it) }
                }

                KeyCommand.CHARACTER_PALETTE -> {
                    showCharacterPalette()
                }
            }
        }
        // undoManager?.forceNextSnapshot()
        return consumed
    }

    private fun KeyEvent.toTypedEditCommand(): CommitTextCommand? {
        if (!isTypedEvent) {
            return null
        }

        val codePoint = deadKeyCombiner.consume(this) ?: return null
        val text = StringBuilder(2).appendCodePointX(codePoint).toString()
        return CommitTextCommand(text, 1)
    }

    private inline fun preparedSelectionContext(
        state: TextFieldState,
        textLayoutState: TextLayoutState,
        block: TextFieldPreparedSelection.() -> Unit
    ) {
        val preparedSelection = TextFieldPreparedSelection(
            state = state,
            textLayoutState = textLayoutState,
            textPreparedSelectionState = preparedSelectionState
        )
        preparedSelection.block()
        if (preparedSelection.selection != preparedSelection.initialValue.selection) {
            // update the editProcessor with the latest selection state.
            // this has to be a reset because EditCommands do not inform IME.
            state.edit {
                selectCharsIn(preparedSelection.selection)
            }
        }
    }

    /**
     * Helper function to apply a list of EditCommands in the scope of [TextFieldPreparedSelection]
     */
    private fun List<EditCommand>.applyOnto(state: TextFieldState) {
        state.editProcessor.update(
            this.toMutableList().apply {
                add(0, FinishComposingTextCommand)
            },
            filter
        )
    }

    private fun EditCommand.applyOnto(state: TextFieldState) {
        state.editProcessor.update(listOf(FinishComposingTextCommand, this), filter)
    }
}
