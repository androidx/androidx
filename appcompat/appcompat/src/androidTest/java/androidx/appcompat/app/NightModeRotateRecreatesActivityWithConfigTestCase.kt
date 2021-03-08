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

import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
import androidx.appcompat.testutils.TestUtilsActions.rotateScreenOrientation
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.testutils.LifecycleOwnerUtils
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
public class NightModeRotateRecreatesActivityWithConfigTestCase(private val setMode: NightSetMode) {
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
        // Clean up after the default mode test.
        if (setMode == NightSetMode.DEFAULT) {
            activityRule.runOnUiThread {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    @FlakyTest // b/182209264
    @Test
    public fun testRotateRecreatesActivityWithConfig() {
        // Don't run this test on SDK 26 because it has issues with setRequestedOrientation. Also
        // don't run it on SDK 24 (Nexus Player) or SDK 23 (Pixel C) because those devices only
        // support a single orientation and there doesn't seem to be a way to query supported
        // screen orientations.
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt == 26 || sdkInt == 24 || sdkInt == 23) {
            return
        }

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
        nightModeActivity.resetOnCreate()
        nightModeActivity.resetOnDestroy()
        onView(ViewMatchers.isRoot()).perform(rotateScreenOrientation(nightModeActivity))
        nightModeActivity.expectOnDestroy(5000)

        // Slow devices might need some time between onDestroy and onCreate.
        nightModeActivity.expectOnCreate(5000)

        // Assert that we got a different activity and thus it was recreated.
        val rotatedNightModeActivity = activityRule.activity
        val rotatedConfig = rotatedNightModeActivity.resources.configuration
        assertNotSame(nightModeActivity, rotatedNightModeActivity)
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rotatedConfig)

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to compare against the cached value.
        assertNotSame(orientation, rotatedConfig.orientation)
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
