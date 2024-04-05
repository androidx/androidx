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

package androidx.compose.ui.platform.l10n

import androidx.compose.ui.platform.Translations

/**
 * Returns the translation for the given locale; `null` if there isn't one.
 */
internal fun translationFor(localeTag: String) = when(localeTag) {
    "" -> Translations.en()
    "en_AU" -> Translations.enAU()
    "en_CA" -> Translations.enCA()
    "en_GB" -> Translations.enGB()
    "en_IN" -> Translations.enIN()
    "af" -> Translations.af()
    "am" -> Translations.am()
    "ar" -> Translations.ar()
    "as" -> Translations.`as`()
    "az" -> Translations.az()
    "be" -> Translations.be()
    "bg" -> Translations.bg()
    "bn" -> Translations.bn()
    "bs" -> Translations.bs()
    "ca" -> Translations.ca()
    "cs" -> Translations.cs()
    "da" -> Translations.da()
    "de" -> Translations.de()
    "el" -> Translations.el()
    "es" -> Translations.es()
    "es_US" -> Translations.esUS()
    "et" -> Translations.et()
    "eu" -> Translations.eu()
    "fa" -> Translations.fa()
    "fi" -> Translations.fi()
    "fr" -> Translations.fr()
    "fr_CA" -> Translations.frCA()
    "gl" -> Translations.gl()
    "gu" -> Translations.gu()
    "hi" -> Translations.hi()
    "hr" -> Translations.hr()
    "hu" -> Translations.hu()
    "hy" -> Translations.hy()
    "in" -> Translations.`in`()
    "id" -> Translations.`in`()
    "is" -> Translations.`is`()
    "it" -> Translations.it()
    "iw" -> Translations.iw()
    "he" -> Translations.iw()
    "ja" -> Translations.ja()
    "ka" -> Translations.ka()
    "kk" -> Translations.kk()
    "km" -> Translations.km()
    "kn" -> Translations.kn()
    "ko" -> Translations.ko()
    "ky" -> Translations.ky()
    "lo" -> Translations.lo()
    "lt" -> Translations.lt()
    "lv" -> Translations.lv()
    "mk" -> Translations.mk()
    "ml" -> Translations.ml()
    "mn" -> Translations.mn()
    "mr" -> Translations.mr()
    "ms" -> Translations.ms()
    "my" -> Translations.my()
    "nb" -> Translations.nb()
    "ne" -> Translations.ne()
    "nl" -> Translations.nl()
    "or" -> Translations.or()
    "pa" -> Translations.pa()
    "pl" -> Translations.pl()
    "pt" -> Translations.pt()
    "pt_BR" -> Translations.ptBR()
    "pt_PT" -> Translations.ptPT()
    "ro" -> Translations.ro()
    "ru" -> Translations.ru()
    "si" -> Translations.si()
    "sk" -> Translations.sk()
    "sl" -> Translations.sl()
    "sq" -> Translations.sq()
    "sr" -> Translations.sr()
    "sv" -> Translations.sv()
    "sw" -> Translations.sw()
    "ta" -> Translations.ta()
    "te" -> Translations.te()
    "th" -> Translations.th()
    "tl" -> Translations.tl()
    "tr" -> Translations.tr()
    "uk" -> Translations.uk()
    "ur" -> Translations.ur()
    "uz" -> Translations.uz()
    "vi" -> Translations.vi()
    "zh_CN" -> Translations.zhCN()
    "zh_HK" -> Translations.zhHK()
    "zh_TW" -> Translations.zhTW()
    "zu" -> Translations.zu()
    else -> null
}
