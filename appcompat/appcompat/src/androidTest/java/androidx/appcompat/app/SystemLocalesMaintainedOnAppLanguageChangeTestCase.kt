/*
 * Copyright 2022 The Android Open Source Project
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
@file:Suppress("deprecation")

package androidx.appcompat.app

import android.os.Build
import android.os.LocaleList
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SystemLocalesMaintainedOnAppLanguageChangeTestCase {
    @get:Rule
    val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    private var expectedSystemLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setUp() {
        // Since no locales are applied as of now, current configuration will have system
        // locales.
        expectedSystemLocales = LocalesUpdateActivity.getConfigLocales(
            rule.activity.resources.configuration
        )
    }

    @Ignore
    @Test
    fun testGetSystemLocales_noAppLocalesSet_systemLocalesSameAsExpectedSystemLocales() {
        val context = InstrumentationRegistry.getInstrumentation().context
        assertEquals(expectedSystemLocales, LocaleManagerCompat.getSystemLocales(context))
    }

    @Ignore
    @Test
    fun testGetSystemLocales_afterAppLocalesSet_systemLocalesSameAsExpectedSystemLocales() {
        LocalesUtils.setLocalesAndWaitForRecreate(rule.activity, LocalesUtils.CUSTOM_LOCALE_LIST)
        // verify the custom locales were applied.
        assertEquals(LocalesUtils.CUSTOM_LOCALE_LIST, AppCompatDelegate.getApplicationLocales())

        val context = InstrumentationRegistry.getInstrumentation().context
        // verify correct system locales are returned.
        assertEquals(expectedSystemLocales, LocaleManagerCompat.getSystemLocales(context))
       }

    @After
    fun teardown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // cleaning up any platform-persisted locales.
            AppCompatDelegate.Api33Impl.localeManagerSetApplicationLocales(
                AppCompatDelegate.getLocaleManagerForApplication(),
                LocaleList.getEmptyLocaleList()
            )
        }
    }
}
