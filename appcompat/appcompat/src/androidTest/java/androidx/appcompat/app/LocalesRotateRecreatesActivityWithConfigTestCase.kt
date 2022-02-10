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

import android.app.Activity
import android.app.Instrumentation
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

@LargeTest
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesRotateRecreatesActivityWithConfigTestCase() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    @get:Rule
    public val activityRule: LocalesActivityTestRule<LocalesUpdateActivity> =
        LocalesActivityTestRule(
            LocalesUpdateActivity::class.java,
            initialTouchMode = false,
            // Let the test method launch its own activity so that we can ensure it's RESUMED.
            launchActivity = false
        )

    @After
    public fun teardown() {
        device.setOrientationNatural()
        device.waitForIdle(/* timeout= */5000)

        // Clean up after the default mode test.

        activityRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    @Test
    public fun testRotateRecreatesActivityWithConfig() {
        // Set locales to CUSTOM_LOCALE_LIST and wait for state RESUMED.
        val initialActivity = activityRule.launchActivity(null)
        LifecycleOwnerUtils.waitUntilState(initialActivity, Lifecycle.State.RESUMED)
        systemLocales = LocalesUpdateActivity.getConfigLocales(activityRule.activity.resources
            .configuration)

        setLocalesAndWaitForRecreate(initialActivity, CUSTOM_LOCALE_LIST)

        val localesActivity = activityRule.activity
        val config = localesActivity.resources.configuration

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to pull the orientation value now.
        val orientation = config.orientation

        // Assert that the current Activity has the expected locales.
        assertConfigurationLocalesEquals(LocalesUpdateActivity.overlayCustomAndSystemLocales(
            CUSTOM_LOCALE_LIST, systemLocales), config)

        // Now rotate the device. This should result in an onDestroy lifecycle event.
        localesActivity.resetOnDestroy()
        rotateDeviceAndWaitForRecreate(localesActivity) {
            localesActivity.expectOnDestroy(/* timeout= */ 5000)

            // Assert that we got a different activity and thus it was recreated.
            val rotatedLocalesActivity = activityRule.activity
            val rotatedConfig = rotatedLocalesActivity.resources.configuration
            assertNotSame(localesActivity, rotatedLocalesActivity)
            assertConfigurationLocalesEquals(
                LocalesUpdateActivity.overlayCustomAndSystemLocales(CUSTOM_LOCALE_LIST,
                    systemLocales),
                rotatedConfig
            )

            // On API level 26 and below, the configuration object is going to be identical
            // across configuration changes, so we need to compare against the cached value.
            assertNotSame(orientation, rotatedConfig.orientation)
        }
    }

    private fun rotateDeviceAndWaitForRecreate(activity: Activity, doThis: () -> Unit) {
        val monitor = Instrumentation.ActivityMonitor(activity::class.java.name, /* result= */
            null, /* block= */false)
        instrumentation.addMonitor(monitor)

        device.withOrientation(Orientation.LEFT) {
            // Wait for the activity to be recreated after rotation
            var count = 0
            var lastActivity: Activity? = activity
            while ((lastActivity == null || activity == lastActivity) && count < 5) {
                // If this times out, it will return null.
                lastActivity = monitor.waitForActivityWithTimeout(/* timeout= */ 1000L)
                count++
            }
            instrumentation.waitForIdleSync()

            // Ensure that we didn't time out
            assertNotNull("Activity was not recreated within 5000ms", lastActivity)
            assertNotEquals(
                "Activity was not recreated within 5000ms", activity, lastActivity
            )
            doThis()
        }
    }
}
