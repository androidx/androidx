/*
 * Copyright 2020 The Android Open Source Project
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
import android.text.TextUtils
import androidx.appcompat.app.NightModeCustomAttachBaseContextActivity.CUSTOM_FONT_SCALE
import androidx.appcompat.app.NightModeCustomAttachBaseContextActivity.CUSTOM_LOCALE
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * This is one approach to customizing Activity configuration that's used in google3.
 * <p>
 *
 */
@SdkSuppress(minSdkVersion = 16)
@MediumTest
@RunWith(AndroidJUnit4::class)
class NightModeRtlTestUtilsRegressionTestCase {
    private var restoreConfig: (() -> Unit)? = null

    @get:Rule
    val activityRule = NightModeActivityTestRule(
        NightModeCustomAttachBaseContextActivity::class.java,
        initialTouchMode = false,
        launchActivity = false
    )

    @Suppress("DEPRECATION")
    @Before
    fun setup() {
        // Duplicated from the Google Calendar app's RtlTestUtils.java
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val locale = CUSTOM_LOCALE
        val configuration = resources.configuration

        // Preserve initial state for later.
        val initialConfig = Configuration(configuration)
        val initialLocale = Locale.getDefault()
        restoreConfig = {
            Locale.setDefault(initialLocale)
            resources.updateConfiguration(initialConfig, resources.displayMetrics)
        }

        // Set up the custom state.
        configuration.fontScale = CUSTOM_FONT_SCALE
        configuration.setLayoutDirection(locale)
        configuration.setLocale(locale)
        Locale.setDefault(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Launch the activity last.
        activityRule.launchActivity(null)
    }

    @After
    fun teardown() {
        // Restore the initial state.
        restoreConfig?.invoke()
    }

    @Test
    @Suppress("DEPRECATION")
    fun testLocaleIsMaintained() {
        // Check that the custom configuration properties are maintained
        val config = activityRule.activity.resources.configuration
        assertEquals(CUSTOM_LOCALE, config.locale)
        assertEquals(TextUtils.getLayoutDirectionFromLocale(CUSTOM_LOCALE), config.layoutDirection)
    }

    @Test
    fun testFontScaleIsMaintained() {
        // Check that the custom configuration properties are maintained
        val config = activityRule.activity.resources.configuration
        assertEquals(CUSTOM_FONT_SCALE, config.fontScale)
    }
}
