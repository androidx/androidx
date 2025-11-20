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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.EmptyPointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

/** Provides information about the Window that is hosting this compose hierarchy. */
@Stable
interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a popup
     * or dialog is visible, this property can be used to determine if the current window is in
     * focus.
     */
    val isWindowFocused: Boolean

    /** Indicates the state of keyboard modifiers (pressed or not). */
    val keyboardModifiers: PointerKeyboardModifiers
        get() = WindowInfoImpl.GlobalKeyboardModifiers.value

    /**
     * Size of the window. This size excludes insets, such as any system bars, so it is not safe to
     * assume that this size matches the available space of the compose hierarchy hosted inside this
     * window. Instead this size should be used as a breakpoint when changing between UI
     * configurations, or similar window-dependent configuration.
     */
    val containerSize: IntSize
        get() = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

    /**
     * Size of the window represented as [DpSize]. This size excludes insets, such as any system
     * bars, so it is not safe to assume that this size matches the available space of the compose
     * hierarchy hosted inside this window. Instead this size should be used as a breakpoint when
     * changing between UI configurations, or similar window-dependent configuration.
     */
    val containerDpSize: DpSize
        get() = DpSize.Unspecified
}

@Composable
internal fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { callback.value(it) }
    }
}

internal class WindowInfoImpl : WindowInfo {
    private val _containerSize = mutableStateOf(IntSize.Zero)

    private val _containerDpSize = mutableStateOf(DpSize.Zero)

    override var isWindowFocused: Boolean by mutableStateOf(false)

    override var keyboardModifiers: PointerKeyboardModifiers
        get() = GlobalKeyboardModifiers.value
        set(value) {
            GlobalKeyboardModifiers.value = value
        }

    override var containerSize: IntSize
        get() = _containerSize.value
        set(value) {
            _containerSize.value = value
        }

    override var containerDpSize: DpSize
        get() = _containerDpSize.value
        set(value) {
            _containerDpSize.value = value
        }

    companion object {
        // One instance across all windows makes sense, since the state of KeyboardModifiers is
        // common for all windows.
        internal val GlobalKeyboardModifiers = mutableStateOf(EmptyPointerKeyboardModifiers())
    }
}
