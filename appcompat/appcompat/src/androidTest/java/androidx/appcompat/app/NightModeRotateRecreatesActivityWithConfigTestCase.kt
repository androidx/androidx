/*
 * Copyright 2019 The Android Open Source Project
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
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 18)
public class NightModeRotateRecreatesActivityWithConfigTestCase(private val setMode: NightSetMode) {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    public val activityRule: NightModeActivityTestRule<NightModeActivity> =
        NightModeActivityTestRule(
            NightModeActivity::class.java,
            initialTouchMode = false,
            // Let the test method launch its own activity so that we can ensure it's RESUMED.
            launchActivity = false
        )

    @After
    public fun teardown() {
        device.setOrientationNatural()
        device.waitForIdle(5000)

        // Clean up after the default mode test.
        if (setMode == NightSetMode.DEFAULT) {
            activityRule.runOnUiThread {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    @Test
    public fun testRotateRecreatesActivityWithConfig() {
        // Set local night mode to MODE_NIGHT_YES and wait for state RESUMED.
        val initialActivity = activityRule.launchActivity(null)
        LifecycleOwnerUtils.waitUntilState(initialActivity, Lifecycle.State.RESUMED)
        setNightModeAndWaitForRecreate(initialActivity, MODE_NIGHT_YES, setMode)

        val nightModeActivity = activityRule.activity
        val config = nightModeActivity.resources.configuration

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to pull the orientation value now.
        val orientation = config.orientation

        // Assert that the current Activity is 'dark'
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, config)

        // Now rotate the device. This should result in an onDestroy lifecycle event.
        nightModeActivity.resetOnDestroy()
        rotateDeviceAndWaitForRecreate(nightModeActivity)
        nightModeActivity.expectOnDestroy(5000)

        // Assert that we got a different activity and thus it was recreated.
        val rotatedNightModeActivity = activityRule.activity
        val rotatedConfig = rotatedNightModeActivity.resources.configuration
        assertNotSame(nightModeActivity, rotatedNightModeActivity)
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rotatedConfig)

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to compare against the cached value.
        assertNotSame(orientation, rotatedConfig.orientation)
    }

    private fun rotateDeviceAndWaitForRecreate(activity: Activity) {
        val monitor = Instrumentation.ActivityMonitor(activity::class.java.name, null, false)
        instrumentation.addMonitor(monitor)

        // Rotate
        device.setOrientationLeft()

        // Wait for the activity to be recreated after rotation
        var count = 0
        var lastActivity: Activity? = activity
        while ((lastActivity == null || activity == lastActivity) && count < 5) {
            // If this times out, it will return null.
            lastActivity = monitor.waitForActivityWithTimeout(1000L)
            count++
        }
        instrumentation.waitForIdleSync()

        // Ensure that we didn't time out
        assertNotNull("Activity was not recreated within 5000ms", lastActivity)
        assertNotEquals("Activity was not recreated within 5000ms", activity, lastActivity)
    }

    public companion object {
        @JvmStatic
        @Parameterized.Parameters
        public fun data(): List<NightSetMode> = if (Build.VERSION.SDK_INT >= 17) {
            listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
        } else {
            listOf(NightSetMode.DEFAULT)
        }
    }
}
