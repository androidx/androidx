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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

internal object DefaultSkikoKeyMapping : KeyMapping {
    override fun map(event: KeyEvent): KeyCommand? {
        return when (event.key) {
            Key.MoveHome, Key.NumPadMoveHome -> {
                when (event.modifiers) {
                    KeyModifiers.Ctrl -> KeyCommand.HOME
                    KeyModifiers.CtrlShift -> KeyCommand.SELECT_HOME
                    else -> null
                }
            }
            Key.MoveEnd, Key.NumPadMoveEnd -> {
                when (event.modifiers) {
                    KeyModifiers.Ctrl -> KeyCommand.END
                    KeyModifiers.CtrlShift -> KeyCommand.SELECT_END
                    else -> null
                }
            }
            else -> null
        } ?: defaultKeyMapping.map(event)
    }
}

internal fun createMacOsDefaultKeyMapping(): KeyMapping {
    val common = commonKeyMapping(KeyModifiers.Meta)
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            val keyModifiers = event.modifiers
            when (event.key) {
                Key.Delete, Key.NumPadDelete -> {
                    when (keyModifiers) {
                        KeyModifiers.None,
                        KeyModifiers.Shift,
                        KeyModifiers.Ctrl,
                        KeyModifiers.CtrlShift -> KeyCommand.DELETE_NEXT_CHAR
                        KeyModifiers.Alt,
                        KeyModifiers.AltShift -> KeyCommand.DELETE_NEXT_WORD
                        else -> null
                    }
                }
                Key.Backspace -> {
                    when (keyModifiers) {
                        KeyModifiers.None,
                        KeyModifiers.Shift,
                        KeyModifiers.Ctrl,
                        KeyModifiers.CtrlShift -> KeyCommand.DELETE_PREV_CHAR
                        KeyModifiers.Meta,
                        KeyModifiers.ShiftMeta -> KeyCommand.DELETE_FROM_LINE_START
                        KeyModifiers.Alt,
                        KeyModifiers.AltShift -> KeyCommand.DELETE_PREV_WORD
                        else -> null
                    }
                }
                Key.Enter, Key.NumPadEnter -> {
                    when (keyModifiers) {
                        KeyModifiers.None,
                        KeyModifiers.Alt,
                        KeyModifiers.Shift,
                        KeyModifiers.AltShift -> KeyCommand.NEW_LINE
                        else -> null
                    }
                }
                Key.MoveHome, Key.NumPadMoveHome -> {
                    when (event.modifiers) {
                        KeyModifiers.Meta -> KeyCommand.HOME
                        KeyModifiers.ShiftMeta -> KeyCommand.SELECT_HOME
                        else -> null
                    }
                }
                Key.MoveEnd, Key.NumPadMoveEnd -> {
                    when (event.modifiers) {
                        KeyModifiers.Meta -> KeyCommand.END
                        KeyModifiers.ShiftMeta -> KeyCommand.SELECT_END
                        else -> null
                    }
                }
                else -> null
            }?.let {
                return it
            }

            return when (keyModifiers) {
                KeyModifiers.CtrlMeta -> {
                    when (event.key) {
                        Key.Spacebar -> KeyCommand.CHARACTER_PALETTE
                        else -> null
                    }
                }

                KeyModifiers.AltShift -> {
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
                }

                KeyModifiers.ShiftMeta -> {
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.SELECT_LINE_LEFT
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.SELECT_LINE_RIGHT
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.SELECT_HOME
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.SELECT_END
                        else -> null
                    }
                }

                KeyModifiers.Meta -> {
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.LINE_LEFT
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.LINE_RIGHT
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.HOME
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.END
                        else -> null
                    }
                }

                // Emacs-like shortcuts
                KeyModifiers.CtrlShiftAlt -> {
                    when (event.key) {
                        Key.F -> KeyCommand.SELECT_RIGHT_WORD
                        Key.B -> KeyCommand.SELECT_LEFT_WORD
                        else -> null
                    }
                }

                KeyModifiers.CtrlAlt -> {
                    when (event.key) {
                        Key.F -> KeyCommand.RIGHT_WORD
                        Key.B -> KeyCommand.LEFT_WORD
                        else -> null
                    }
                }

                KeyModifiers.CtrlShift -> {
                    when (event.key) {
                        Key.F -> KeyCommand.SELECT_RIGHT_CHAR
                        Key.B -> KeyCommand.SELECT_LEFT_CHAR
                        Key.P -> KeyCommand.SELECT_UP
                        Key.N -> KeyCommand.SELECT_DOWN
                        Key.A -> KeyCommand.SELECT_LINE_START
                        Key.E -> KeyCommand.SELECT_LINE_END
                        else -> null
                    }
                }

                KeyModifiers.Ctrl -> {
                    when (event.key) {
                        Key.F -> KeyCommand.LEFT_CHAR
                        Key.B -> KeyCommand.RIGHT_CHAR
                        Key.P -> KeyCommand.UP
                        Key.N -> KeyCommand.DOWN
                        Key.A -> KeyCommand.LINE_START
                        Key.E -> KeyCommand.LINE_END
                        Key.H -> KeyCommand.DELETE_PREV_CHAR
                        Key.D -> KeyCommand.DELETE_NEXT_CHAR
                        Key.K -> KeyCommand.DELETE_TO_LINE_END
                        Key.O -> KeyCommand.NEW_LINE
                        else -> null
                    }
                }
                // end of emacs-like shortcuts

                KeyModifiers.Shift ->
                    when (event.key) {
                        Key.MoveHome,
                        Key.NumPadMoveHome -> KeyCommand.SELECT_HOME
                        Key.MoveEnd,
                        Key.NumPadMoveEnd -> KeyCommand.SELECT_END
                        else -> null
                    }

                KeyModifiers.Alt ->
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.LEFT_WORD
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.RIGHT_WORD
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.PREV_PARAGRAPH
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.NEXT_PARAGRAPH
                        else -> null
                    }

                else -> null
            } ?: common.map(event)
        }
    }
}

internal fun createWindowsDefaultKeyMapping(): KeyMapping {
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            val keyModifiers = event.modifiers

            if ((keyModifiers == KeyModifiers.Alt) && (event.key == Key.Backspace)) {
                return KeyCommand.UNDO
            }

            return DefaultSkikoKeyMapping.map(event)
        }
    }
}

private val KeyModifiers.Companion.CtrlShiftAlt
    get() = KeyModifiers.Ctrl + KeyModifiers.Shift + KeyModifiers.Alt