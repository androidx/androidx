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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.JsLocale
import androidx.compose.ui.text.intl.PlatformLocale

/**
 * A JS implementation of StringDelegate
 */
internal class JsStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: PlatformLocale): String =
        toLocaleUpperCase(string, locale.language)

    override fun toLowerCase(string: String, locale: PlatformLocale): String =
        toLocaleLowerCase(string, locale.language)

    override fun capitalize(string: String, locale: PlatformLocale): String {
        return string.replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecaseImpl(locale)
            } else {
                it.toString()
            }
        }
    }

    // Copy-pasted from kotlin _OneToManyTitlecaseMappings.kt (internal in stdlib, but doesn't take locale into account)
    private fun Char.titlecaseImpl(locale: PlatformLocale): String {
        val uppercase = toLocaleUpperCase(this.toString(), locale.language)
        if (uppercase.length > 1) {
            return if (this == '\u0149') uppercase else uppercase[0] + uppercase.substring(1).lowercase()
        }
        return titlecaseChar().toString()
    }

    override fun decapitalize(string: String, locale: PlatformLocale): String {
        return string.replaceFirstChar {
            toLocaleLowerCase(it.toString(), locale.language)
        }
    }
}

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    JsStringDelegate()

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/toLocaleUpperCase
internal fun toLocaleUpperCase(text: String, locale: String): String =
    js("text.toLocaleUpperCase(locale)")

//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/toLocaleLowerCase
internal fun toLocaleLowerCase(text: String, locale: String): String =
    js("text.toLocaleLowerCase(locale)")