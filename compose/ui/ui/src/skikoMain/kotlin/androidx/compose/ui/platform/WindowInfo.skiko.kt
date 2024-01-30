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

package androidx.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.EmptyPointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.unit.IntSize

/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
@JvmDefaultWithCompatibility
actual interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a
     * popup or dialog is visible, this property can be used to determine if the current window
     * is in focus.
     */
    actual val isWindowFocused: Boolean

    /**
     * Indicates the state of keyboard modifiers (pressed or not).
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    actual val keyboardModifiers: PointerKeyboardModifiers
        get() = WindowInfoImpl.GlobalKeyboardModifiers.value

    /**
     * Size of the window's content container in pixels.
     */
    @ExperimentalComposeUiApi
    val containerSize: IntSize get() = IntSize.Zero
}

internal class WindowInfoImpl : WindowInfo {
    override var isWindowFocused: Boolean by mutableStateOf(false)

    @ExperimentalComposeUiApi
    override var keyboardModifiers: PointerKeyboardModifiers by GlobalKeyboardModifiers

    @ExperimentalComposeUiApi
    override var containerSize: IntSize by mutableStateOf(IntSize.Zero)

    companion object {
        // One instance across all windows makes sense, since the state of KeyboardModifiers is
        // common for all windows.
        internal val GlobalKeyboardModifiers = mutableStateOf(EmptyPointerKeyboardModifiers())
    }
}
