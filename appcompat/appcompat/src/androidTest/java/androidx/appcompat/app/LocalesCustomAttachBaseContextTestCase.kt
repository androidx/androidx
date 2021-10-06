/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.app

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.NightModeCustomAttachBaseContextActivity.CUSTOM_FONT_SCALE
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
import androidx.core.os.LocaleListCompat
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesCustomAttachBaseContextTestCase() {

    @get:Rule
    val activityRule = LocalesActivityTestRule(
        LocalesCustomAttachBaseContextActivity::class.java
    )

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    @Suppress("DEPRECATION")
    fun testNightModeIsMaintainedOnLocalesChange() {
        setNightModeAndWaitForRecreate(
            activityRule,
            MODE_NIGHT_YES,
            NightSetMode.LOCAL
        )
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activityRule.activity.resources.configuration
        )
        setLocalesAndWaitForRecreate(activityRule, CUSTOM_LOCALE_LIST)
        // Check that the custom configuration properties are maintained
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activityRule.activity.resources.configuration
        )
    }

    @Test
    fun testFontScaleIsMaintained() {
        // Check that the custom configuration properties are maintained.
        val config = activityRule.activity.resources.configuration
        assertEquals(CUSTOM_FONT_SCALE, config.fontScale)
    }

    @Test
    fun testFontScaleIsMaintainedOnLocalesChange() {
        // Set locales to CUSTOM_LOCALE_LIST.
        setLocalesAndWaitForRecreate(activityRule, CUSTOM_LOCALE_LIST)
        // Check that the custom configuration properties are maintained.
        val config = activityRule.activity.resources.configuration
        assertEquals(CUSTOM_FONT_SCALE, config.fontScale)
    }
}
