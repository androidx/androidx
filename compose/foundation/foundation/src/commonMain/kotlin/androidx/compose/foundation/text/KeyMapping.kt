/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

internal interface KeyMapping {
    fun map(event: KeyEvent): KeyCommand?
}

// each platform can define its own key mapping, on Android its just defaultKeyMapping, but on
// desktop, the value depends on the current OS
internal expect val platformDefaultKeyMapping: KeyMapping

// It's common for all platforms key mapping
internal fun commonKeyMapping(systemShortcutModifiers: KeyModifiers): KeyMapping {
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            val keyModifiers = event.modifiers
            return when (keyModifiers) {
                systemShortcutModifiers + KeyModifiers.Shift ->
                    when (event.key) {
                        Key.Z -> KeyCommand.REDO
                        else -> null
                    }
                systemShortcutModifiers ->
                    when (event.key) {
                        Key.C,
                        Key.Insert,
                        Key.NumPadInsert -> KeyCommand.COPY
                        Key.V -> KeyCommand.PASTE
                        Key.X -> KeyCommand.CUT
                        Key.A -> KeyCommand.SELECT_ALL
                        Key.Y -> KeyCommand.REDO
                        Key.Z -> KeyCommand.UNDO
                        else -> null
                    }
                KeyModifiers.Shift ->
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.SELECT_LEFT_CHAR
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.SELECT_RIGHT_CHAR
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.SELECT_UP
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.SELECT_DOWN
                        Key.PageUp,
                        Key.NumPadPageUp -> KeyCommand.SELECT_PAGE_UP
                        Key.PageDown,
                        Key.NumPadPageDown -> KeyCommand.SELECT_PAGE_DOWN
                        Key.MoveHome,
                        Key.NumPadMoveHome -> KeyCommand.SELECT_LINE_START
                        Key.MoveEnd,
                        Key.NumPadMoveEnd -> KeyCommand.SELECT_LINE_END
                        Key.Insert,
                        Key.NumPadInsert -> KeyCommand.PASTE
                        else -> null
                    }
                KeyModifiers.None ->
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.LEFT_CHAR
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.RIGHT_CHAR
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.UP
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.DOWN
                        Key.DirectionCenter -> KeyCommand.CENTER
                        Key.PageUp,
                        Key.NumPadPageUp -> KeyCommand.PAGE_UP
                        Key.PageDown,
                        Key.NumPadPageDown -> KeyCommand.PAGE_DOWN
                        Key.MoveHome,
                        Key.NumPadMoveHome -> KeyCommand.LINE_START
                        Key.MoveEnd,
                        Key.NumPadMoveEnd -> KeyCommand.LINE_END
                        Key.Enter,
                        Key.NumPadEnter -> KeyCommand.NEW_LINE
                        Key.Backspace -> KeyCommand.DELETE_PREV_CHAR
                        Key.Delete -> KeyCommand.DELETE_NEXT_CHAR
                        Key.Paste -> KeyCommand.PASTE
                        Key.Cut -> KeyCommand.CUT
                        Key.Copy -> KeyCommand.COPY
                        Key.Tab -> KeyCommand.TAB
                        else -> null
                    }
                else -> null
            }
        }
    }
}

// It's "default" or actually "non macOS" key mapping
internal val defaultKeyMapping: KeyMapping =
    commonKeyMapping(KeyModifiers.Ctrl).let { common ->
        object : KeyMapping {
            override fun map(event: KeyEvent): KeyCommand? {
                val keyModifiers = event.modifiers
                when (event.key) {
                    Key.Backspace ->
                        when (keyModifiers) {
                            KeyModifiers.None,
                            KeyModifiers.Shift,
                            KeyModifiers.ShiftMeta -> KeyCommand.DELETE_PREV_CHAR
                            KeyModifiers.Ctrl,
                            KeyModifiers.CtrlShift -> KeyCommand.DELETE_PREV_WORD
                            KeyModifiers.Alt -> KeyCommand.DELETE_FROM_LINE_START
                            else -> null
                        }
                    else -> null
                }?.let {
                    return it
                }

                return when (event.modifiers) {
                    KeyModifiers.CtrlShift ->
                        when (event.key) {
                            Key.DirectionLeft,
                            Key.NumPadDirectionLeft -> KeyCommand.SELECT_LEFT_WORD
                            Key.DirectionRight,
                            Key.NumPadDirectionRight -> KeyCommand.SELECT_RIGHT_WORD
                            Key.DirectionUp,
                            Key.NumPadDirectionUp -> KeyCommand.SELECT_PREV_PARAGRAPH
                            Key.DirectionDown,
                            Key.NumPadDirectionDown -> KeyCommand.SELECT_NEXT_PARAGRAPH
                            else -> null
                        }
                    KeyModifiers.Ctrl ->
                        when (event.key) {
                            Key.DirectionLeft,
                            Key.NumPadDirectionLeft -> KeyCommand.LEFT_WORD
                            Key.DirectionRight,
                            Key.NumPadDirectionRight -> KeyCommand.RIGHT_WORD
                            Key.DirectionUp,
                            Key.NumPadDirectionUp -> KeyCommand.PREV_PARAGRAPH
                            Key.DirectionDown,
                            Key.NumPadDirectionDown -> KeyCommand.NEXT_PARAGRAPH
                            Key.H -> KeyCommand.DELETE_PREV_CHAR
                            Key.Delete -> KeyCommand.DELETE_NEXT_WORD
                            Key.Backslash -> KeyCommand.DESELECT
                            else -> null
                        }
                    KeyModifiers.Shift ->
                        when (event.key) {
                            Key.MoveHome,
                            Key.NumPadMoveHome -> KeyCommand.SELECT_LINE_START
                            Key.MoveEnd,
                            Key.NumPadMoveEnd -> KeyCommand.SELECT_LINE_END
                            else -> null
                        }
                    KeyModifiers.Alt ->
                        when (event.key) {
                            Key.Delete -> KeyCommand.DELETE_TO_LINE_END
                            else -> null
                        }
                    else -> null
                } ?: common.map(event)
            }
        }
    }
