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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.implementedInJetBrainsFork

internal actual typealias NativePointerButtons = Int

internal actual typealias NativePointerKeyboardModifiers = Int

internal actual fun EmptyPointerKeyboardModifiers(): PointerKeyboardModifiers =
    implementedInJetBrainsFork()

actual data class PointerEvent
internal actual constructor(
    actual val changes: List<PointerInputChange>,
    internal val internalPointerEvent: InternalPointerEvent?
) {
    actual val buttons: PointerButtons
        get() = implementedInJetBrainsFork()

    actual val keyboardModifiers: PointerKeyboardModifiers
        get() = implementedInJetBrainsFork()

    actual var type: PointerEventType = implementedInJetBrainsFork()

    /** @param changes The changes. */
    actual constructor(changes: List<PointerInputChange>) : this(changes, null) {
        implementedInJetBrainsFork()
    }
}

actual val PointerButtons.isPrimaryPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerButtons.isBackPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerButtons.isForwardPressed: Boolean
    get() = implementedInJetBrainsFork()

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean = implementedInJetBrainsFork()

actual val PointerButtons.areAnyPressed: Boolean
    get() = implementedInJetBrainsFork()

actual fun PointerButtons.indexOfFirstPressed(): Int = implementedInJetBrainsFork()

actual fun PointerButtons.indexOfLastPressed(): Int = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = implementedInJetBrainsFork()

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = implementedInJetBrainsFork()
