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

package androidx.compose.ui.text.intl

internal actual val PlatformLocale.language: String
    get() = _language

internal actual val PlatformLocale.script: String
    get() = _script ?: ""

internal actual val PlatformLocale.region: String
    get() = _region ?: ""

internal actual fun PlatformLocale.getLanguageTag(): String = _baseName

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList
            get() = LocaleList(
                userPreferredLanguages().map {
                    Locale(it.toIntlLocale())
                }
            )

        override fun parseLanguageTag(languageTag: String): PlatformLocale {
            return languageTag.toIntlLocale()
        }
    }

// The list of RTL languages is taken from https://github.com/openjdk/jdk/blob/master/src/java.desktop/share/classes/java/awt/ComponentOrientation.java#L156
private val rtlLanguagesSet = setOf("ar", "fa", "he", "iw", "ji", "ur", "yi")

// Implemented according to ComponentOrientation.getOrientation (AWT),
// since there is no js API for this.
internal actual fun PlatformLocale.isRtl(): Boolean = this.language in rtlLanguagesSet

// K/JS and K/Wasm stdlib doesn't have this type. Therefore, we declare it here.
// Ideally it would not be necessary, or at least we would make it internal, but Compose common API
// requires that expect PlatformLocale is actualized by a "standard/native" type of Locale.
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale
// Note: Since Compose common code introduced PlatformLocale with extension properties with the same names,
// we had to change the names of the properties in kotlin to avoid the name shadowing.
actual external class PlatformLocale {
    @JsName("language")
    internal val _language: String
    @JsName("script")
    internal val _script: String?
    @JsName("region")
    internal val _region: String?
    @JsName("baseName")
    internal val _baseName: String
}

internal fun parseLanguageTagToIntlLocale(languageTag: String): PlatformLocale =
    js("new Intl.Locale(languageTag)")

internal expect fun userPreferredLanguages(): List<String>

private fun String.toIntlLocale(): PlatformLocale = parseLanguageTagToIntlLocale(this)