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

package androidx.compose.ui.text.intl

import androidx.compose.runtime.Immutable
import platform.Foundation.*

@Deprecated(
    message = "Use platform.Foundation.NSLocale directly instead",
    replaceWith = ReplaceWith("platform.Foundation.NSLocale"),
)
typealias PlatformLocale = NSLocale

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList
            get() = LocaleList(NSLocale.preferredLanguages.map { Locale(NSLocale(it as String)) })
    }

@Immutable
actual class Locale internal constructor(
    internal val platformLocale: NSLocale,
) {
    // Strip NSLocale-specific keyword suffixes so toLanguageTag stays BCP47-compatible.
    private val languageTag: String =
        NSLocale.canonicalLanguageIdentifierFromString(
            platformLocale.localeIdentifier.substringBefore("@")
        )

    actual val language: String
        get() = platformLocale.languageCode
    actual val script: String
        get() = platformLocale.scriptCode.orEmpty()
    actual val region: String
        get() = platformLocale.countryCode.orEmpty()

    actual fun toLanguageTag(): String = languageTag

    actual override operator fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Locale) return false
        if (this === other) return true
        return toLanguageTag() == other.toLanguageTag()
    }

    actual override fun hashCode(): Int = toLanguageTag().hashCode()

    actual override fun toString(): String = toLanguageTag()

    actual companion object {
        actual val current: Locale
            get() = platformLocaleDelegate.current[0]
    }

    actual constructor(languageTag: String): this(NSLocale(languageTag))
}

/**
 * Creates [Locale] object from platform specific [NSLocale].
 */
fun NSLocale.toComposeLocale(): Locale = Locale(this)

private fun NSLocale.isRtl(): Boolean =
    NSLocale.characterDirectionForLanguage(languageCode) == NSLocaleLanguageDirectionRightToLeft

internal actual fun Locale.isRtl(): Boolean {
    return platformLocale.isRtl()
}