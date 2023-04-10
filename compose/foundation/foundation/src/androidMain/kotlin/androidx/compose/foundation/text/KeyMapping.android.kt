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

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

internal actual val platformDefaultKeyMapping = object : KeyMapping {
    override fun map(event: KeyEvent): KeyCommand? = when {
        event.isShiftPressed && event.isAltPressed ->
            when (event.key) {
                MappedKeys.DirectionLeft -> KeyCommand.SELECT_LINE_LEFT
                MappedKeys.DirectionRight -> KeyCommand.SELECT_LINE_RIGHT
                MappedKeys.DirectionUp -> KeyCommand.SELECT_HOME
                MappedKeys.DirectionDown -> KeyCommand.SELECT_END
                else -> null
            }

        event.isAltPressed ->
            when (event.key) {
                MappedKeys.DirectionLeft -> KeyCommand.LINE_LEFT
                MappedKeys.DirectionRight -> KeyCommand.LINE_RIGHT
                MappedKeys.DirectionUp -> KeyCommand.HOME
                MappedKeys.DirectionDown -> KeyCommand.END
                else -> null
            }

        else -> null
    } ?: defaultKeyMapping.map(event)
}

internal actual object MappedKeys {
    actual val A: Key = Key(AndroidKeyEvent.KEYCODE_A)
    actual val C: Key = Key(AndroidKeyEvent.KEYCODE_C)
    actual val H: Key = Key(AndroidKeyEvent.KEYCODE_H)
    actual val V: Key = Key(AndroidKeyEvent.KEYCODE_V)
    actual val Y: Key = Key(AndroidKeyEvent.KEYCODE_Y)
    actual val X: Key = Key(AndroidKeyEvent.KEYCODE_X)
    actual val Z: Key = Key(AndroidKeyEvent.KEYCODE_Z)
    actual val Backslash: Key = Key(AndroidKeyEvent.KEYCODE_BACKSLASH)
    actual val DirectionLeft: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
    actual val DirectionRight: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
    actual val DirectionUp: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_UP)
    actual val DirectionDown: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
    actual val PageUp: Key = Key(AndroidKeyEvent.KEYCODE_PAGE_UP)
    actual val PageDown: Key = Key(AndroidKeyEvent.KEYCODE_PAGE_DOWN)
    actual val MoveHome: Key = Key(AndroidKeyEvent.KEYCODE_MOVE_HOME)
    actual val MoveEnd: Key = Key(AndroidKeyEvent.KEYCODE_MOVE_END)
    actual val Insert: Key = Key(AndroidKeyEvent.KEYCODE_INSERT)
    actual val Enter: Key = Key(AndroidKeyEvent.KEYCODE_ENTER)
    actual val Backspace: Key = Key(AndroidKeyEvent.KEYCODE_DEL)
    actual val Delete: Key = Key(AndroidKeyEvent.KEYCODE_FORWARD_DEL)
    actual val Paste: Key = Key(AndroidKeyEvent.KEYCODE_PASTE)
    actual val Cut: Key = Key(AndroidKeyEvent.KEYCODE_CUT)
    actual val Copy: Key = Key(AndroidKeyEvent.KEYCODE_COPY)
    actual val Tab: Key = Key(AndroidKeyEvent.KEYCODE_TAB)
}
