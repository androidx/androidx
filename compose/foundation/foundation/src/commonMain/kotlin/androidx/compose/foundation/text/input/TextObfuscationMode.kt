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

package androidx.compose.foundation.text.input

import kotlin.jvm.JvmInline

/**
 * Defines how the text will be obscured in secure text fields.
 *
 * Text obscuring refers to replacing the original text content with a mask via various methods. It
 * is most common in password fields.
 *
 * Users are generally accustomed to different default experiences for secure text input on various
 * platforms. On desktop, the convention is to keep the input entirely hidden. Conversely, mobile
 * platforms typically offer a brief reveal of the last typed character. This reveal lasts for a
 * short duration or until another character is entered, aiding users in tracking their input while
 * maintaining privacy by not exposing too much information.
 */
@JvmInline
value class TextObfuscationMode internal constructor(val value: Int) {
    companion object {
        /**
         * Do not obscure any content, making all the content visible.
         *
         * It can be useful when you want to briefly reveal the content by toggling a reveal icon.
         */
        val Visible = TextObfuscationMode(0)

        /**
         * Reveals the last typed character for a short amount of time.
         *
         * Forces reveal behavior regardless of platform settings. For platform-dependent behavior,
         * e.g. Androids "Show Passwords" setting, use [System].
         */
        val RevealLastTyped = TextObfuscationMode(1)

        /** All characters are hidden. */
        val Hidden = TextObfuscationMode(2)

        /**
         * Gives the choice to the platform to hide or show characters.
         *
         * On most platforms, the behavior depends on that platform's conventions (typically
         * defaulting to [Hidden]).
         *
         * Android Specific: If the system setting is set to "Show" this setting mimics
         * [RevealLastTyped], otherwise it mimics [Hidden]. Additionally, there are differences,
         * depending on the SDK version:
         * - SDK 37 and later: Respects granular platform settings that can differentiate between
         *   touch input and physical keyboard input.
         * - Below SDK 37: Respects the system-wide "Show passwords" toggle
         *   (`Settings.System.TEXT_SHOW_PASSWORD`) for all input types.
         */
        val System = TextObfuscationMode(3)
    }
}
