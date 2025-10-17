/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.text.l10n.translationFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal actual value class ContextMenuStrings actual constructor(actual val value: Int) {
    actual companion object {
        actual val Cut = ContextMenuStrings(0)
        actual val Copy = ContextMenuStrings(1)
        actual val Paste = ContextMenuStrings(2)
        actual val SelectAll = ContextMenuStrings(3)
        actual val Autofill = ContextMenuStrings(4)
    }
}

private fun getTranslation(string: ContextMenuStrings, locale: Locale): String {
    val tag = localeTag(language = locale.language, region = locale.region)
    val translation = translationByLocaleTag.getOrPut(tag) {
        findTranslation(locale)
    }
    return translation[string] ?: error("Missing translation for $string")
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: ContextMenuStrings): String {
    val locale = Locale.current
    return getTranslation(string, locale)
}

internal fun getLocalizedString(string: ContextMenuStrings): String {
    val locale = Locale.current
    return getTranslation(string, locale)
}

/**
 * A single translation; should contain all the [ContextMenuStrings].
 */
internal typealias Translation = Map<ContextMenuStrings, String>

/**
 * Translations we've already loaded, mapped by the locale tag (see [localeTag]).
 */
private val translationByLocaleTag = mutableMapOf<String, Translation>()

/**
 * Returns the tag for the given locale.
 *
 * Note that this is our internal format; this isn't the same as [Locale.toLanguageTag].
 */
private fun localeTag(language: String, region: String) = when {
    language == "" -> ""
    region == "" -> language
    else -> "${language}_$region"
}

/**
 * Returns a sequence of locale tags to use as keys to look up the translation for the given locale.
 *
 * Note that we don't need to check children (e.g. use `fr_FR` if `fr` is missing) because the
 * translations should never have a missing parent.
 */
private fun localeTagChain(locale: Locale) = sequence {
    if (locale.region != "") {
        yield(localeTag(language = locale.language, region = locale.region))
    }
    if (locale.language != "") {
        yield(localeTag(language = locale.language, region = ""))
    }
    yield(localeTag("", ""))
}

/**
 * Finds a [Translation] for the given locale.
 */
private fun findTranslation(locale: Locale): Map<ContextMenuStrings, String> {
    // We don't need to merge translations because each one should contain all the strings.
    return localeTagChain(locale).firstNotNullOf { translationFor(it) }
}

/**
 * This object is only needed to provide a namespace for the [Translation] provider functions
 * (e.g. [Translations.en]), to avoid polluting the global namespace.
 */
internal object Translations
