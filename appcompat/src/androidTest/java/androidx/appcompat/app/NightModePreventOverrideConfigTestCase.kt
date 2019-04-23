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

import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForDestroy

import android.content.res.Configuration

import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.LifecycleOwnerUtils

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NightModePreventOverrideConfigTestCase {

    @get:Rule
    val activityRule = ActivityTestRule(
        NightModePreventOverrideConfigActivity::class.java,
        false,
        false
    )

    @Before
    @Throws(Throwable::class)
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below.
        activityRule.runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testActivityRecreate() {
        activityRule.launchActivity(null)

        // Activity should be able to reach fully resumed state in default NIGHT_NO.
        LifecycleOwnerUtils.waitUntilState(activityRule, Lifecycle.State.RESUMED)
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_NO,
            activityRule.activity.resources.configuration
        )

        // Simulate the user setting night mode, which should force an activity recreate().
        setNightModeAndWaitForDestroy(
            activityRule,
            MODE_NIGHT_YES,
            NightSetMode.LOCAL
        )

        // Activity should be able to reach fully resumed state again.
        LifecycleOwnerUtils.waitUntilState(activityRule, Lifecycle.State.RESUMED)

        // The requested night mode value should have been set by
        // updateResourcesConfigurationForNightMode().
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activityRule.activity.resources.configuration
        )
    }
}
