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
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

internal object defaultSkikoKeyMapping : KeyMapping {
    override fun map(event: KeyEvent): KeyCommand? {
        return when {
            event.isCtrlPressed && event.isShiftPressed -> {
                when (event.key) {
                    Key.MoveHome,
                    Key.NumPadMoveHome -> KeyCommand.SELECT_HOME
                    Key.MoveEnd,
                    Key.NumPadMoveEnd -> KeyCommand.SELECT_END
                    else -> null
                }
            }
            else -> null
        } ?: defaultKeyMapping.map(event)
    }
}

internal fun createMacosDefaultKeyMapping(): KeyMapping {
    val common = commonKeyMapping(KeyModifiers.Meta)
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when {
                event.isMetaPressed && event.isCtrlPressed ->
                    when (event.key) {
                        Key.Spacebar -> KeyCommand.CHARACTER_PALETTE
                        else -> null
                    }

                event.isShiftPressed && event.isAltPressed ->
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

                event.isShiftPressed && event.isMetaPressed ->
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

                event.isMetaPressed ->
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.LINE_LEFT
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.LINE_RIGHT
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.HOME
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.END
                        Key.Backspace -> KeyCommand.DELETE_FROM_LINE_START
                        else -> null
                    }

                // Emacs-like shortcuts
                event.isCtrlPressed && event.isShiftPressed && event.isAltPressed -> {
                    when (event.key) {
                        Key.F -> KeyCommand.SELECT_RIGHT_WORD
                        Key.B -> KeyCommand.SELECT_LEFT_WORD
                        else -> null
                    }
                }

                event.isCtrlPressed && event.isAltPressed -> {
                    when (event.key) {
                        Key.F -> KeyCommand.RIGHT_WORD
                        Key.B -> KeyCommand.LEFT_WORD
                        else -> null
                    }
                }

                event.isCtrlPressed && event.isShiftPressed -> {
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

                event.isCtrlPressed -> {
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

                event.isShiftPressed ->
                    when (event.key) {
                        Key.MoveHome,
                        Key.NumPadMoveHome -> KeyCommand.SELECT_HOME
                        Key.MoveEnd,
                        Key.NumPadMoveEnd -> KeyCommand.SELECT_END
                        else -> null
                    }

                event.isAltPressed ->
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.NumPadDirectionLeft -> KeyCommand.LEFT_WORD
                        Key.DirectionRight,
                        Key.NumPadDirectionRight -> KeyCommand.RIGHT_WORD
                        Key.DirectionUp,
                        Key.NumPadDirectionUp -> KeyCommand.PREV_PARAGRAPH
                        Key.DirectionDown,
                        Key.NumPadDirectionDown -> KeyCommand.NEXT_PARAGRAPH
                        Key.Delete,
                        Key.NumPadDelete -> KeyCommand.DELETE_NEXT_WORD
                        Key.Backspace -> KeyCommand.DELETE_PREV_WORD
                        else -> null
                    }

                else -> null
            } ?: common.map(event)
        }
    }
}
