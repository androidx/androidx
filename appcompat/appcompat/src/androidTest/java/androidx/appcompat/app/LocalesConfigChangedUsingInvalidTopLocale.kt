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

package androidx.appcompat.app

import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 32)
class LocalesConfigChangedUsingInvalidTopLocale() {

    var CUSTOM_LOCALES_INVALID_LOCALES_AT_TOP = LocaleListCompat.forLanguageTags("XX,hi,es")
    var CUSTOM_LOCALES_AFTER_REARRANGEMENT = LocaleListCompat.forLanguageTags("hi,XX,es")

    @get:Rule
    val activityRule: LocalesActivityTestRule<LocalesUpdateActivity> =
        LocalesActivityTestRule(
            LocalesUpdateActivity::class.java,
            initialTouchMode = false,
            // Let the test method launch its own activity so that we can ensure it's RESUMED.
            launchActivity = false
        )

    @Test
    fun testInvalidLocaleDoesNotRecreateActivityInLoop() {
        val initialActivity = activityRule.launchActivity(null)
        // initial locales of a new activity will be the same as system locales.
        var systemLocales = LocalesUpdateActivity.getConfigLocales(
            initialActivity.resources.configuration
        )

        LocalesUtils.setLocalesAndWaitForRecreate(
            initialActivity,
            CUSTOM_LOCALES_INVALID_LOCALES_AT_TOP
        )
        // Wait for locales to get applied.
        Thread.sleep(/* millis = */ 2000)

        // record activity state at the end of locales application process.
        val localesActivity2 = activityRule.activity

        // wait for 2 seconds and record activity again.
        Thread.sleep(/* millis = */ 2000)
        val localesActivity3 = activityRule.activity

        // Assert that activity has not been recreated since the recreation by
        // setApplicationLocales i.e. it is not stuck in a recreation loop.
        Assert.assertEquals(localesActivity3, localesActivity2)

        // After activity recreation framework rearranges input custom locales and brings forwards
        // the most suitable locale. Hence, in the new expected locales the invalid locale that
        // was at the first position in the input locales should be moved to a later position.
        // In this case "XX,hi,es" is rearranged to "hi,XX,es"
        var expectedConfigLocalesAfterRearrangement =
            LocalesUpdateActivity.overlayCustomAndSystemLocales(
                CUSTOM_LOCALES_AFTER_REARRANGEMENT,
                systemLocales
            )

        // Assert that the current Activity has the new adjusted locales.
        assertConfigurationLocalesEquals(
            expectedConfigLocalesAfterRearrangement,
            localesActivity3.resources.configuration
        )
    }

    @After
    fun teardown() {
        // Clean up
        activityRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }
}
