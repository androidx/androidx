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

package androidx.compose.ui.platform.l10n

import androidx.compose.ui.platform.Translations

/**
 * Returns the translation for the given locale; `null` if there isn't one.
 */
internal fun translationFor(localeTag: String) = when(localeTag) {
    "ar" -> Translations.ar()
    "eu" -> Translations.eu()
    "bn" -> Translations.bn()
    "bho" -> Translations.bho()
    "bg" -> Translations.bg()
    "zh_HK" -> Translations.zhHK()
    "zh_CN" -> Translations.zhCN()
    "zh_TW" -> Translations.zhTW()
    "ca" -> Translations.ca()
    "ca_ES" -> Translations.caES()
    "hr" -> Translations.hr()
    "cs" -> Translations.cs()
    "da" -> Translations.da()
    "nl_BE" -> Translations.nlBE()
    "nl" -> Translations.nl()
    "en_AU" -> Translations.enAU()
    "en_IN" -> Translations.enIN()
    "en_IE" -> Translations.enIE()
    "en_GB" -> Translations.enGB()
    "en_ZA" -> Translations.enZA()
    "" -> Translations.en()
    "fa" -> Translations.fa()
    "fi" -> Translations.fi()
    "fr_BE" -> Translations.frBE()
    "fr_CA" -> Translations.frCA()
    "fr" -> Translations.fr()
    "gl" -> Translations.gl()
    "de" -> Translations.de()
    "el" -> Translations.el()
    "iw" -> Translations.iw()
    "he" -> Translations.iw()
    "hi" -> Translations.hi()
    "hu" -> Translations.hu()
    "in" -> Translations.`in`()
    "id" -> Translations.`in`()
    "it" -> Translations.it()
    "ja" -> Translations.ja()
    "kn" -> Translations.kn()
    "ko" -> Translations.ko()
    "ms" -> Translations.ms()
    "mr" -> Translations.mr()
    "nb" -> Translations.nb()
    "pl" -> Translations.pl()
    "pt_BR" -> Translations.ptBR()
    "pt" -> Translations.pt()
    "ro" -> Translations.ro()
    "ru" -> Translations.ru()
    "sk" -> Translations.sk()
    "sl" -> Translations.sl()
    "es_AR" -> Translations.esAR()
    "es_CL" -> Translations.esCL()
    "es_CO" -> Translations.esCO()
    "es_MX" -> Translations.esMX()
    "es" -> Translations.es()
    "sv" -> Translations.sv()
    "th" -> Translations.th()
    "tr" -> Translations.tr()
    "ta" -> Translations.ta()
    "te" -> Translations.te()
    "uk" -> Translations.uk()
    "vi" -> Translations.vi()
    else -> null
}
