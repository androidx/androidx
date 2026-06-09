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

import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSLocale

class LocaleTest {
    @Test
    fun platformLocale_sharesTheAttributes() {
        val platformLocale = NSLocale("sr-Latn-SR")
        val locale = platformLocale.toComposeLocale()

        assertEquals(platformLocale, locale.platformLocale)
        assertEquals("sr", locale.language)
        assertEquals("Latn", locale.script)
        assertEquals("SR", locale.region)
        assertEquals("sr-Latn-SR", locale.toLanguageTag())
    }

    @Test
    fun forLanguageTag_withoutRegion() {
        val locale = Locale("ja")

        assertEquals("ja", locale.language)
        assertEquals("", locale.script)
        assertEquals("", locale.region)
        assertEquals("ja", locale.toLanguageTag())
    }

    @Test
    fun forLanguageTag_withScript() {
        val locale = Locale("zh-Hant-TW")

        assertEquals("zh", locale.language)
        assertEquals("Hant", locale.script)
        assertEquals("TW", locale.region)
        assertEquals("zh-Hant-TW", locale.toLanguageTag())
    }
}
