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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

/** Provides information about the Window that is hosting this compose hierarchy. */
@Stable
public interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a popup
     * or dialog is visible, this property can be used to determine if the current window is in
     * focus.
     */
    public val isWindowFocused: Boolean

    /** Indicates the state of keyboard modifiers (pressed or not). */
    public val keyboardModifiers: PointerKeyboardModifiers
        get() = WindowInfoImpl.GlobalKeyboardModifiers.value

    /**
     * The size of the window in pixels that can used by the application in some way. Note that this
     * may be a larger size than is available to the Compose hierarchy (if Compose does not fill the
     * window). In addition, it may not be safe to display content using the entire window size, for
     * example in situations where insets representing cutouts or obscuring system UI reduce the
     * amount of available space. Even though all pixels included in this size may not be usable at
     * any given time, it's relative stability means it is generally the correct signal to drive the
     * overall layout structure displaying in the window.
     */
    public val containerSize: IntSize
        get() = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

    /**
     * The size of the window represented as a [DpSize] that can used by the application in some
     * way. Note that this may be a larger size than is available to the Compose hierarchy (if
     * Compose does not fill the window). In addition, it may not be safe to display content using
     * the entire window size, for example in situations where insets representing cutouts or
     * obscuring system UI reduce the amount of available space. Even though all pixels included in
     * this size may not be usable at any given time, it's relative stability means it is generally
     * the correct signal to drive the overall layout structure displaying in the window.
     */
    public val containerDpSize: DpSize
        get() = DpSize.Unspecified
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
        internal val GlobalKeyboardModifiers = mutableStateOf(PointerKeyboardModifiers())
    }
}
