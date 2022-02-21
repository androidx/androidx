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

import androidx.appcompat.Orientation
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.appcompat.withOrientation
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.testutils.LifecycleOwnerUtils
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@LargeTest
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(minSdkVersion = 18, maxSdkVersion = 31)
class LocalesRotateDoesNotRecreateActivityTestCase() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val activityRule: LocalesActivityTestRule<LocalesRotateDoesNotRecreateActivity> =
        LocalesActivityTestRule(
            LocalesRotateDoesNotRecreateActivity::class.java,
            initialTouchMode = false,
            // Let the test method launch its own activity so that we can ensure it's RESUMED.
            launchActivity = false
        )

    @Test
    fun testRotateDoesNotRecreateActivity() {
        // Set locales to CUSTOM_LOCALE_LIST and wait for state RESUMED.
        val initialActivity = activityRule.launchActivity(null)
        var systemLocales = LocalesUpdateActivity.getConfigLocales(
            initialActivity.resources.configuration)
        LifecycleOwnerUtils.waitUntilState(initialActivity, Lifecycle.State.RESUMED)
        setLocalesAndWaitForRecreate(initialActivity, CUSTOM_LOCALE_LIST)

        val localesActivity = activityRule.activity
        val config = localesActivity.resources.configuration

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to pull the orientation value now.
        val orientation = config.orientation
        val expectedLocales = LocalesUpdateActivity.overlayCustomAndSystemLocales(
            CUSTOM_LOCALE_LIST, systemLocales)
        // Assert that the current Activity has the new locales.
        assertConfigurationLocalesEquals(expectedLocales, config)

        // Now rotate the device. This should NOT result in a lifecycle event, just a call to
        // onConfigurationChanged.
        localesActivity.resetOnConfigurationChange()
        device.withOrientation(Orientation.LEFT) {
            instrumentation.waitForIdleSync()
            localesActivity.expectOnConfigurationChange(5000)

            // Assert that we got the same activity and thus it was not recreated.
            val rotatedLocalesActivity = activityRule.activity
            val rotatedConfig = rotatedLocalesActivity.resources.configuration
            assertSame(localesActivity, rotatedLocalesActivity)
            assertConfigurationLocalesEquals(expectedLocales, rotatedConfig)

            // On API level 26 and below, the configuration object is going to be identical
            // across configuration changes, so we need to compare against the cached value.
            assertNotSame(orientation, rotatedConfig.orientation)
        }
    }

    @After
    fun teardown() {
        device.setOrientationNatural()
        // setOrientationNatural may need some time rotate orientation to natural, so we wait for
        // the operation to end for 5000ms.
        device.waitForIdle(/* timeout= */5000)

        // Clean up
        activityRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }
}
