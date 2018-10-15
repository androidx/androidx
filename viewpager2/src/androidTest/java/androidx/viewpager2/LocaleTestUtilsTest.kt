/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2

import android.content.res.Configuration
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_LTR
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

// Fetch default locale as early as possible (e.g., before initializing LocaleTestUtilsTest class)
private val DEFAULT_LANGUAGE = Locale.getDefault().toString()

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocaleTestUtilsTest {
    private val configuration: Configuration get() =
        InstrumentationRegistry.getTargetContext().resources.configuration
    private val Configuration.language: String get() =
        ConfigurationCompat.getLocales(this).get(0).toString()

    private lateinit var localeUtil: LocaleTestUtils
    private var expectRtlInDefaultLanguage: Boolean = false

    @Before
    fun setUp() {
        localeUtil = LocaleTestUtils(InstrumentationRegistry.getTargetContext())
        determineDefaultLayoutDirection()
    }

    @After
    fun tearDown() {
        localeUtil.resetLocale()
    }

    @Test
    fun test_defaultValues() {
        assertDefaultValues()
    }

    @Test
    fun test_setAndResetLocale() {
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.LTR_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.LTR_LANGUAGE, false)
        localeUtil.resetLocale()
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.RTL_LANGUAGE, true)
        localeUtil.resetLocale()
        assertDefaultValues()
    }

    @Test
    fun test_ltrRtlLanguagesExist() {
        val availableLanguages = Locale.getAvailableLocales().map { it.toString() }
        val getReason: (String, String) -> String = { name, code ->
            "$name test language '$code' does not exist on test device"
        }
        assertThat(getReason("Default", LocaleTestUtils.DEFAULT_TEST_LANGUAGE),
            LocaleTestUtils.DEFAULT_TEST_LANGUAGE, Matchers.isIn(availableLanguages))
        assertThat(getReason("LTR", LocaleTestUtils.LTR_LANGUAGE),
            LocaleTestUtils.LTR_LANGUAGE, Matchers.isIn(availableLanguages))
        assertThat(getReason("RTL", LocaleTestUtils.RTL_LANGUAGE),
            LocaleTestUtils.RTL_LANGUAGE, Matchers.isIn(availableLanguages))
    }

    private fun assertDefaultValues() {
        assertLocaleIs(DEFAULT_LANGUAGE, expectRtlInDefaultLanguage)
    }

    private fun assertLocaleIs(lang: String, expectRtl: Boolean) {
        assertThat(
            "Locale should be $lang",
            configuration.language,
            equalTo(lang)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertThat(
                "Layout direction should be ${if (expectRtl) "RTL" else "LTR"}",
                configuration.layoutDirection,
                equalTo(if (expectRtl) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR)
            )
        }
    }

    private fun determineDefaultLayoutDirection() {
        assertThat(
            "Locale must still be the default when determining the default layout direction",
            configuration.language,
            equalTo(DEFAULT_LANGUAGE)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            expectRtlInDefaultLanguage = configuration.layoutDirection == LAYOUT_DIRECTION_RTL
        }
    }
}