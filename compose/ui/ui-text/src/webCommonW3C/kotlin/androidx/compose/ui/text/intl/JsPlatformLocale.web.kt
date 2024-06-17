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

// TODO https://youtrack.jetbrains.com/issue/COMPOSE-1256/Implement-public-JsLocale
//  Remove TODO in a separate PR, this implementation should be reviewed separately.
class JsPlatformLocale internal constructor(internal val locale: IntlLocale)

actual typealias PlatformLocale = JsPlatformLocale

internal actual val PlatformLocale.language: String
    get() = locale.language

internal actual val PlatformLocale.script: String
    get() = locale.script ?: ""

internal actual val PlatformLocale.region: String
    get() = locale.region ?: ""

internal actual fun PlatformLocale.getLanguageTag(): String = locale.baseName

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList
            get() = LocaleList(
                userPreferredLanguages().map {
                    Locale(JsPlatformLocale(it.toIntlLocale()))
                }
            )

        override fun parseLanguageTag(languageTag: String): PlatformLocale {
            return JsPlatformLocale(languageTag.toIntlLocale())
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