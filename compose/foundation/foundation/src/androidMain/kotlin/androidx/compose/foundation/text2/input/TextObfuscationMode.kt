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

package androidx.compose.foundation.text2.input

import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Defines how the text will be obscured in secure text fields.
 *
 * Text obscuring refers to replacing the original text content with a mask via various methods.
 * It is most common in password fields.
 *
 * The default behavior for typing input on Desktop has always been to keep it completely hidden.
 * However, on mobile devices, the default behavior is to briefly reveal the last typed character
 * for a short period or until another character is typed. This helps the user to follow the text
 * input while also protecting their privacy by not revealing too much information to others.
 */
@ExperimentalFoundationApi
@JvmInline
value class TextObfuscationMode internal constructor(val value: Int) {
    companion object {
        /**
         * Do not obscure any content, making all the content visible.
         *
         * It can be useful when you want to briefly reveal the content by clicking a reveal button.
         */
        val Visible = TextObfuscationMode(0)

        /**
         * Default behavior on mobile devices. Reveals the last typed character for a short amount
         * of time.
         *
         * Note; this feature also depends on a system setting called
         * [Settings.System.TEXT_SHOW_PASSWORD]. If the system setting is disabled, this option
         * behaves exactly as [Hidden].
         */
        val RevealLastTyped = TextObfuscationMode(1)

        /**
         * Default behavior on desktop platforms. All characters are hidden.
         */
        val Hidden = TextObfuscationMode(2)
    }
}