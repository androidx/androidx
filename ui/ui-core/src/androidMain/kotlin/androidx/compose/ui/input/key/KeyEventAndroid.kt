/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.key

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import androidx.compose.ui.input.key.KeyEventType.KeyDown
import androidx.compose.ui.input.key.KeyEventType.KeyUp
import androidx.compose.ui.input.key.KeyEventType.Unknown
import android.view.KeyEvent as AndroidKeyEvent

@OptIn(ExperimentalKeyInput::class)
internal inline class KeyEventAndroid(val keyEvent: AndroidKeyEvent) : KeyEvent2 {

    override val key: Key
        get() = Key(keyEvent.keyCode)

    override val type: KeyEventType
        get() = when (keyEvent.action) {
            ACTION_DOWN -> KeyDown
            ACTION_UP -> KeyUp
            else -> Unknown
        }
}
