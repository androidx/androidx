/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

/**
 * Wraps key modifiers (e.g. [KeyEvent.isCtrlPressed]), making it easy to match an entire set of
 * them exactly.
 *
 * For example, to match "ctrl" use `keyEvent.modifiers == KeyModifiers.Ctrl`. Unlike
 * `keyEvent.isCtrlPressed` this will not match, e.g., "ctrl-alt".
 */
@JvmInline
internal value class KeyModifiers private constructor(private val flags: Int) {
    /**
     * Creates a [KeyModifiers] instance representing the state given by arguments.
     *
     * @param isAltPressed Whether the "Alt" key modifier is pressed.
     * @param isCtrlPressed Whether the "Ctrl" key modifier is pressed.
     * @param isMetaPressed Whether the "Meta" key modifier is pressed.
     * @param isShiftPressed Whether the "Shift" key modifier is pressed.
     */
    constructor(
        isAltPressed: Boolean = false,
        isCtrlPressed: Boolean = false,
        isMetaPressed: Boolean = false,
        isShiftPressed: Boolean = false,
    ) : this(
        (if (isAltPressed) ALT_FLAG else 0) or
            (if (isCtrlPressed) CTRL_FLAG else 0) or
            (if (isMetaPressed) META_FLAG else 0) or
            (if (isShiftPressed) SHIFT_FLAG else 0)
    )

    /**
     * Returns a [KeyModifiers] instance that represents the state where the key modifiers of both
     * `this` and [other] are pressed.
     */
    operator fun plus(other: KeyModifiers) = KeyModifiers(flags or other.flags)

    @Suppress("unused")
    companion object {
        /** The bit flag for the "Alt" key modifier. */
        private const val ALT_FLAG = 0b0001

        /** The bit flag for the "Ctrl" key modifier. */
        private const val CTRL_FLAG = 0b0010

        /** The bit flag for the "Meta" key modifier. */
        private const val META_FLAG = 0b0100

        /** The bit flag for the "Shift" key modifier. */
        private const val SHIFT_FLAG = 0b1000

        /** A [KeyModifiers] instance representing no key modifiers being pressed. */
        @JvmStatic val None = KeyModifiers(0)

        /** A [KeyModifiers] instance representing only the "Alt" key modifier being pressed. */
        @JvmStatic val Alt = KeyModifiers(ALT_FLAG)

        /** A [KeyModifiers] instance representing only the "Ctrl" key modifier being pressed. */
        @JvmStatic val Ctrl = KeyModifiers(CTRL_FLAG)

        /** A [KeyModifiers] instance representing only the "Meta" key modifier being pressed. */
        @JvmStatic val Meta = KeyModifiers(META_FLAG)

        /** A [KeyModifiers] instance representing only the "Shift" key modifier being pressed. */
        @JvmStatic val Shift = KeyModifiers(SHIFT_FLAG)

        /**
         * A [KeyModifiers] instance representing the "Alt" and "Shift" key modifiers being pressed.
         */
        @JvmStatic val AltShift: KeyModifiers = Alt + Shift

        /**
         * A [KeyModifiers] instance representing the "Ctrl" and "Shift" key modifiers being
         * pressed.
         */
        @JvmStatic val CtrlShift: KeyModifiers = Ctrl + Shift

        /**
         * A [KeyModifiers] instance representing the "Shift" and "Meta" key modifiers being
         * pressed.
         */
        @JvmStatic val ShiftMeta: KeyModifiers = Meta + Shift

        /**
         * A [KeyModifiers] instance representing the "Ctrl" and "Alt" key modifiers being pressed.
         */
        @JvmStatic val CtrlAlt: KeyModifiers = Ctrl + Alt

        /**
         * A [KeyModifiers] instance representing the "Ctrl" and "Meta" key modifiers being pressed.
         */
        @JvmStatic val CtrlMeta: KeyModifiers = Ctrl + Meta

        /**
         * A [KeyModifiers] instance representing the "Alt" and "Meta" key modifiers being pressed.
         */
        @JvmStatic val AltMeta: KeyModifiers = Meta + Shift
    }
}

/** Returns a [KeyModifiers] instance representing the state of key modifiers in the [KeyEvent]. */
internal val KeyEvent.modifiers
    get() =
        KeyModifiers(
            isAltPressed = isAltPressed,
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isShiftPressed = isShiftPressed,
        )
