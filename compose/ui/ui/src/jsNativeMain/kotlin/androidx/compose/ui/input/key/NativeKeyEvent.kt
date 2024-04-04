/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers

actual class NativeKeyEvent(
    val key: Key,
    val value: String?,
    val modifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(0),
    val kind: KeyEventType,
    val timestamp: Long = 0
) {
    constructor(
        key: Key,
        value: String?,
        modifiers: Int,
        kind: KeyEventType,
        timestamp: Long
    ) : this(key, value, modifiers.convertToKeyboardModifiers(), kind, timestamp)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NativeKeyEvent

        if (key != other.key) return false
        if (value != other.value) return false
        if (modifiers != other.modifiers) return false
        if (kind != other.kind) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + modifiers.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}


private fun Int.convertToKeyboardModifiers(): PointerKeyboardModifiers {
    val keyboardModifiers = object {
        val META = 1
        val CONTROL = 2
        val ALT = 4
        val SHIFT = 8
    }

    return PointerKeyboardModifiers(
        isMetaPressed = this and keyboardModifiers.META != 0,
        isCtrlPressed = this and keyboardModifiers.CONTROL != 0,
        isShiftPressed = this and keyboardModifiers.SHIFT != 0,
        isAltPressed = this and keyboardModifiers.ALT != 0,
    )
}