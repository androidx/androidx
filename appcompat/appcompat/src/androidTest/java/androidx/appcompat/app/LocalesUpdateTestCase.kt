/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.webkit.WebView
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWait
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.core.os.LocaleListCompat
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.waitForExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
//  setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesUpdateTestCase() {
    @get:Rule
    val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    var systemLocales = LocaleListCompat.getEmptyLocaleList()
    var expectedLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setUp() {
        // Since no locales are applied as of now, current configuration will have system
        // locales.
        systemLocales = LocalesUpdateActivity.getConfigLocales(rule.activity
            .resources.configuration)
        expectedLocales = LocalesUpdateActivity.overlayCustomAndSystemLocales(CUSTOM_LOCALE_LIST,
            systemLocales)
    }
    @Test
    fun testDialogDoesNotOverrideActivityConfiguration() {
        setLocalesAndWaitForRecreate(rule, CUSTOM_LOCALE_LIST)
        // Now show a AppCompatDialog
        lateinit var dialog: AppCompatDialog
        rule.runOnUiThread {
            dialog = AppCompatDialog(rule.activity)
            dialog.show()
        }
        rule.waitForExecution()
        // Now dismiss the dialog
        rule.runOnUiThread { dialog.dismiss() }

        // Assert that the locales are unchanged
        assertConfigurationLocalesEquals(
            expectedLocales,
            rule.activity.resources.configuration
        )
    }

    @Test
    fun testLoadingWebViewMaintainsConfiguration() {
        setLocalesAndWaitForRecreate(rule, CUSTOM_LOCALE_LIST)

        // Now load a WebView into the Activity
        rule.runOnUiThread { WebView(rule.activity) }

        // Now assert that the context still has applied locales.
        assertEquals(
            expectedLocales,
            LocalesUpdateActivity.getConfigLocales(rule.activity.resources.configuration)
        )
    }

    @Test
    fun testOnLocalesChangedCalled() {
        val activity = rule.activity
        // Set local night mode to YES
        setLocalesAndWait(rule, CUSTOM_LOCALE_LIST)
        // Assert that the Activity received a new value
        assertEquals(expectedLocales, activity.lastLocalesAndReset)
    }

    @Test
    fun testOnConfigurationChangeNotCalled() {
        var activity = rule.activity
        // Set locales to CUSTOM_LOCALE_LIST.
        LocalesUtils.setLocalesAndWait(
            rule,
            CUSTOM_LOCALE_LIST
        )
        // Assert that onConfigurationChange was not called on the original activity
        assertNull(activity.lastConfigurationChangeAndClear)

        activity = rule.activity
        // Set locales back to system locales.
        setLocalesAndWait(
            rule,
            LocaleListCompat.getEmptyLocaleList()
        )
        // Assert that onConfigurationChange was not called
        assertNull(activity.lastConfigurationChangeAndClear)
    }
}
