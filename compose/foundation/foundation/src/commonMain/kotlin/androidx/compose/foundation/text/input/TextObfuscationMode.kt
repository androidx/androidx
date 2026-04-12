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
         * Note; on Android this feature also depends on a system setting called
         * `Settings.System.TEXT_SHOW_PASSWORD`. If the system setting is disabled, this option
         * behaves exactly as [Hidden].
         */
        val RevealLastTyped = TextObfuscationMode(1)

        /** All characters are hidden. */
        val Hidden = TextObfuscationMode(2)
    }
}

/**
 * Platform dependent default obfuscation mode for secure text fields.
 *
 * This is set to [TextObfuscationMode.RevealLastTyped] on Android.
 */
// TODO(b/425658491); Make this public
internal expect val TextObfuscationMode.Companion.Default: TextObfuscationMode
