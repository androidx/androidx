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

/**
 * When a user presses a key on a hardware keyboard, a [KeyEvent] is sent to the
 * [KeyInputModifier] that is currently active.
 *
 * @property key the key that was pressed.
 * @property type the [type][KeyEventType] of key event.
 */
@ExperimentalKeyInput
interface KeyEvent {
    /**
     * The key that was pressed.
     */
    val key: Key

    /**
     * The [type][KeyEventType] of key event.
     */
    val type: KeyEventType

    /**
     * Indicates the status of the Alt key.
     */
    val alt: Alt
}

/**
 * The type of Key Event.
 */
@ExperimentalKeyInput
enum class KeyEventType {
    /**
     * Unknown key event.
     */
    Unknown,

    /**
     * Type of KeyEvent sent when the user lifts their finger off a key on the keyboard.
     */
    KeyUp,

    /**
     * Type of KeyEvent sent when the user presses down their finger on a key on the keyboard.
     */
    KeyDown
}

/**
 * Indicates the status of the Alt key.
 */
@ExperimentalKeyInput
interface Alt {
    /**
     * Indicates whether the Alt key is pressed.
     */
    val isPressed: Boolean
        get() = isLeftAltPressed || isRightAltPressed

    /**
     * Indicates whether the left Alt key is pressed.
     */
    val isLeftAltPressed: Boolean

    /**
     * Indicates whether the right Alt key is pressed.
     */
    val isRightAltPressed: Boolean
}
