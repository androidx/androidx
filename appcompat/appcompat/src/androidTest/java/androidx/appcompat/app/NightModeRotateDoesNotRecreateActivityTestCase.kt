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

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
import androidx.appcompat.testutils.TestUtilsActions.rotateScreenOrientation
import androidx.appcompat.testutils.TestUtilsActions.setScreenOrientation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.filters.LargeTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeRotateDoesNotRecreateActivityTestCase(private val setMode: NightSetMode) {
    @get:Rule
    val activityRule = NightModeActivityTestRule(NightModeRotateDoesNotRecreateActivity::class.java)

    @Test
    fun testRotateDoesNotRecreateActivity() {
        // Don't run this test on SDK 26 because it has issues with setRequestedOrientation. Also
        // don't run it on SDK 24 (Nexus Player) or SDK 23 (Pixel C) because those devices only
        // support a single orientation and there doesn't seem to be a way to query supported
        // screen orientations.
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt == 26 || sdkInt == 24 || sdkInt == 23) {
            return
        }

        // Set local night mode to YES
        setNightModeAndWaitForRecreate(activityRule, MODE_NIGHT_YES, setMode)

        val activity = activityRule.activity
        val config = activity.resources.configuration

        // On API level 26 and below, the configuration object is going to be identical
        // across configuration changes, so we need to pull the orientation value now.
        val orientation = config.orientation

        // Assert that the current Activity is 'dark'
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, config)

        // Now rotate the device
        activity.resetOnConfigurationChange()
        onView(isRoot()).perform(rotateScreenOrientation(activity))
        activity.expectOnConfigurationChange(5000)

        // Assert that we got the same activity.
        val activity2 = activityRule.activity
        val config2 = activity2.resources.configuration

        // And assert that we have the same Activity, and thus was not recreated.
        assertSame(activity, activity2)
        assertNotSame(orientation, config2.orientation)
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, config2)

        // Reset the requested orientation and wait for it to apply.
        activity.resetOnConfigurationChange()
        onView(isRoot()).perform(setScreenOrientation(activity,
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED))
        activity.expectOnConfigurationChange(5000)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = if (Build.VERSION.SDK_INT >= 17) {
            listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
        } else {
            listOf(NightSetMode.DEFAULT)
        }
    }
}
