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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
@JvmDefaultWithCompatibility
expect interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a
     * popup or dialog is visible, this property can be used to determine if the current window
     * is in focus.
     */
    val isWindowFocused: Boolean

    /**
     * Indicates the state of keyboard modifiers (pressed or not).
     */
    // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-412/Remove-getExperimentalComposeUiApi-from-val-keyboardModifiers)
    //  remove get:ExperimentalComposeUiApi, as it actually isn't experimental
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    open val keyboardModifiers: PointerKeyboardModifiers
}

@Composable
internal fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { callback.value(it) }
    }
}
