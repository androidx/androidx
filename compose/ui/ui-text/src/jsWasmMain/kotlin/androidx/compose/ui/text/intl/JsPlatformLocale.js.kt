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

internal class JsLocale(val locale: IntlLocale) : PlatformLocale {

    constructor(languageTag: String): this(languageTag.toIntlLocale())

    override val language: String
        get() = locale.language

    override val script: String
        get() = locale.script ?: ""

    override val region: String
        get() = locale.region ?: ""

    override fun toLanguageTag(): String = locale.baseName
}

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList
            get() = LocaleList(
                userPreferredLanguages().map {
                    Locale(JsLocale(it))
                }
            )


        override fun parseLanguageTag(languageTag: String): PlatformLocale {
            return JsLocale(languageTag)
        }
    }

// The list of RTL languages is taken from https://github.com/openjdk/jdk/blob/master/src/java.desktop/share/classes/java/awt/ComponentOrientation.java#L156
private val rtlLanguagesSet = setOf("ar", "fa", "he", "iw", "ji", "ur", "yi")

// Implemented according to ComponentOrientation.getOrientation (AWT),
// since there is no js API for this.
internal actual fun PlatformLocale.isRtl(): Boolean = this.language in rtlLanguagesSet

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale
internal external interface IntlLocale {
    val language: String
    val script: String?
    val region: String?
    val baseName: String
}

internal fun parseLanguageTagToIntlLocale(languageTag: String): IntlLocale = js("new Intl.Locale(languageTag)")

internal expect fun userPreferredLanguages(): List<String>

private fun String.toIntlLocale(): IntlLocale = parseLanguageTagToIntlLocale(this)