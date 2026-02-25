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

internal actual val platformDefaultKeyMapping =
    object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when (event.modifiers) {
                KeyModifiers.AltShift ->
                    when (event.key) {
                        Key.DirectionLeft -> KeyCommand.SELECT_LINE_LEFT
                        Key.DirectionRight -> KeyCommand.SELECT_LINE_RIGHT
                        Key.DirectionUp -> KeyCommand.SELECT_HOME
                        Key.DirectionDown -> KeyCommand.SELECT_END
                        else -> null
                    }
                KeyModifiers.Alt ->
                    when (event.key) {
                        Key.DirectionLeft -> KeyCommand.LINE_LEFT
                        Key.DirectionRight -> KeyCommand.LINE_RIGHT
                        Key.DirectionUp -> KeyCommand.HOME
                        Key.DirectionDown -> KeyCommand.END
                        else -> null
                    }
                else -> null
            } ?: defaultKeyMapping.map(event)
        }
    }
