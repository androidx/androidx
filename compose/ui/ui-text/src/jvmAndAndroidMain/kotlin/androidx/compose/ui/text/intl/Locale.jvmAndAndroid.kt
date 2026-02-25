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

package androidx.compose.ui.text.intl

import androidx.compose.runtime.Immutable
import java.util.Locale as JavaLocale

@Deprecated(
    message = "Use java.util.Locale directly instead",
    replaceWith = ReplaceWith("java.util.Locale"),
)
typealias PlatformLocale = JavaLocale

@Immutable
actual class Locale(val platformLocale: JavaLocale) {
    actual companion object {
        actual val current: Locale
            get() = platformLocaleDelegate.current[0]
    }

    actual constructor(languageTag: String) : this(parseLanguageTag(languageTag))

    actual val language: String
        get() = platformLocale.language

    actual val script: String
        get() = platformLocale.script

    actual val region: String
        get() = platformLocale.country

    actual fun toLanguageTag(): String = platformLocale.toLanguageTag()

    actual override operator fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Locale) return false
        if (this === other) return true
        return toLanguageTag() == other.toLanguageTag()
    }

    // We don't use data class since we cannot offer copy function here.
    actual override fun hashCode(): Int = toLanguageTag().hashCode()

    actual override fun toString(): String = toLanguageTag()
}

private fun parseLanguageTag(languageTag: String): JavaLocale {
    val platformLocale = JavaLocale.forLanguageTag(languageTag)
    if (platformLocale.toLanguageTag() == "und") {
        System.err.println(
            "The language tag $languageTag is not well-formed. Locale is resolved " +
                "to Undetermined. Note that underscore '_' is not a valid subtag delimiter and " +
                "must be replaced with '-'."
        )
    }
    return platformLocale
}
