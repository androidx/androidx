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

package androidx.compose.ui.platform

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AppCompatActivityLocaleTest {
    lateinit var defaultLocaleListCompat: LocaleListCompat

    @get:Rule
    val composeTestRule =
        createAndroidComposeRule<AppCompatActivity>(effectContext = StandardTestDispatcher())

    @Before
    fun setup() {
        defaultLocaleListCompat = AppCompatDelegate.getApplicationLocales()
    }

    @After
    fun teardown() {
        composeTestRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(defaultLocaleListCompat)
        }
    }

    @Test
    fun defaultLocalesAreCorrect() {
        lateinit var localeList: LocaleList
        lateinit var locale: Locale

        composeTestRule.setContent {
            locale = LocalLocale.current
            localeList = LocalLocaleList.current
        }

        assertEquals(LocaleListCompat.getDefault().toComposeLocaleList(), localeList)
        assertEquals(Locale(java.util.Locale.getDefault()), locale)
    }

    @Test
    fun localeIsCorrectWhenSetBeforeSetContent() {
        composeTestRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es-MX"))
        }

        lateinit var localeList: LocaleList
        lateinit var locale: Locale

        composeTestRule.setContent {
            locale = LocalLocale.current
            localeList = LocalLocaleList.current
        }

        assertEquals(LocaleListCompat.getDefault().toComposeLocaleList(), localeList)
        assertEquals(Locale("es-MX"), locale)
    }

    @Test
    fun localeIsCorrectWhenSetAfterSetContent() {
        lateinit var localeList: LocaleList
        lateinit var locale: Locale

        composeTestRule.setContent {
            locale = LocalLocale.current
            localeList = LocalLocaleList.current
        }

        composeTestRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es-MX"))
        }
        // Set content again, since the Activity was recreated
        composeTestRule.setContent {
            locale = LocalLocale.current
            localeList = LocalLocaleList.current
        }

        assertEquals(LocaleListCompat.getDefault().toComposeLocaleList(), localeList)
        assertEquals(Locale("es-MX"), locale)
    }
}

private fun LocaleListCompat.toComposeLocaleList(): LocaleList =
    LocaleList(List(size()) { Locale(get(it)!!) })
