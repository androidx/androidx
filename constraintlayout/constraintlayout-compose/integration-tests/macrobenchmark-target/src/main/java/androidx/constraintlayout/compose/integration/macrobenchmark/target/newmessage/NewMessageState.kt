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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.newmessage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberNewMessageState(
    key: Any = Unit,
    initialLayoutState: NewMessageLayout
): NewMessageState {
    return remember(key) { NewMessageState(initialLayoutState) }
}

internal class NewMessageState(initialLayoutState: NewMessageLayout) {
    private var _currentState = mutableStateOf(initialLayoutState)

    val currentState: NewMessageLayout
        get() = _currentState.value

    fun setToFull() {
        _currentState.value = NewMessageLayout.Full
    }

    fun setToMini() {
        _currentState.value = NewMessageLayout.Mini
    }

    fun setToFab() {
        _currentState.value = NewMessageLayout.Fab
    }
}

internal enum class NewMessageLayout {
    Full,
    Mini,
    Fab
}
