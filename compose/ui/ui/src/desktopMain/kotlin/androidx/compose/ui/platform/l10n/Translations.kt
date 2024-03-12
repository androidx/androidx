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
 * Maps each locale tag we have a translation for to a function that creates the translation.
 */
internal val TranslationProviderByLocaleTag = mapOf(
    "" to Translations::en,
    "en_AU" to Translations::enAU,
    "en_CA" to Translations::enCA,
    "en_GB" to Translations::enGB,
    "en_IN" to Translations::enIN,
    "af" to Translations::af,
    "am" to Translations::am,
    "ar" to Translations::ar,
    "as" to Translations::`as`,
    "az" to Translations::az,
    "be" to Translations::be,
    "bg" to Translations::bg,
    "bn" to Translations::bn,
    "bs" to Translations::bs,
    "ca" to Translations::ca,
    "cs" to Translations::cs,
    "da" to Translations::da,
    "de" to Translations::de,
    "el" to Translations::el,
    "es" to Translations::es,
    "es_US" to Translations::esUS,
    "et" to Translations::et,
    "eu" to Translations::eu,
    "fa" to Translations::fa,
    "fi" to Translations::fi,
    "fr" to Translations::fr,
    "fr_CA" to Translations::frCA,
    "gl" to Translations::gl,
    "gu" to Translations::gu,
    "hi" to Translations::hi,
    "hr" to Translations::hr,
    "hu" to Translations::hu,
    "hy" to Translations::hy,
    "in" to Translations::`in`,
    "id" to Translations::`in`,
    "is" to Translations::`is`,
    "it" to Translations::it,
    "iw" to Translations::iw,
    "he" to Translations::iw,
    "ja" to Translations::ja,
    "ka" to Translations::ka,
    "kk" to Translations::kk,
    "km" to Translations::km,
    "kn" to Translations::kn,
    "ko" to Translations::ko,
    "ky" to Translations::ky,
    "lo" to Translations::lo,
    "lt" to Translations::lt,
    "lv" to Translations::lv,
    "mk" to Translations::mk,
    "ml" to Translations::ml,
    "mn" to Translations::mn,
    "mr" to Translations::mr,
    "ms" to Translations::ms,
    "my" to Translations::my,
    "nb" to Translations::nb,
    "ne" to Translations::ne,
    "nl" to Translations::nl,
    "or" to Translations::or,
    "pa" to Translations::pa,
    "pl" to Translations::pl,
    "pt" to Translations::pt,
    "pt_BR" to Translations::ptBR,
    "pt_PT" to Translations::ptPT,
    "ro" to Translations::ro,
    "ru" to Translations::ru,
    "si" to Translations::si,
    "sk" to Translations::sk,
    "sl" to Translations::sl,
    "sq" to Translations::sq,
    "sr" to Translations::sr,
    "sv" to Translations::sv,
    "sw" to Translations::sw,
    "ta" to Translations::ta,
    "te" to Translations::te,
    "th" to Translations::th,
    "tl" to Translations::tl,
    "tr" to Translations::tr,
    "uk" to Translations::uk,
    "ur" to Translations::ur,
    "uz" to Translations::uz,
    "vi" to Translations::vi,
    "zh_CN" to Translations::zhCN,
    "zh_HK" to Translations::zhHK,
    "zh_TW" to Translations::zhTW,
    "zu" to Translations::zu,
)
