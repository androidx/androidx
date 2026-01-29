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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.Locale
import platform.Foundation.*

/**
 * A native implementation of StringDelegate
 */

// TODO Remove once https://youtrack.jetbrains.com/issue/KT-23978 fixed
@Suppress("CAST_NEVER_SUCCEEDS")
internal class NativeStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: Locale): String =
        toUpperCase(string as NSString, locale)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun toUpperCase(string: NSString, locale: Locale): String =
        string.uppercaseStringWithLocale(locale.platformLocale)

    override fun toLowerCase(string: String, locale: Locale): String =
        toLowerCase(string as NSString, locale)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun toLowerCase(string: NSString, locale: Locale): String =
        string.lowercaseStringWithLocale(locale.platformLocale)

    override fun capitalize(string: String, locale: Locale): String =
        string.replaceFirstChar {
            if (it.isLowerCase())
                capitalize(it.toString() as NSString, locale)
            else
                it.toString()
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun capitalize(string: NSString, locale: Locale): String =
        string.capitalizedStringWithLocale(locale.platformLocale)

    override fun decapitalize(string: String, locale: Locale): String =
        string.replaceFirstChar { decapitalize(it.toString() as NSString, locale) }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun decapitalize(string: NSString, locale: Locale): String =
        string.lowercaseStringWithLocale(locale.platformLocale)
}

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    NativeStringDelegate()
