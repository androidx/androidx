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
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
import androidx.lifecycle.Lifecycle
import androidx.test.filters.LargeTest
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModePreventOverrideConfigTestCase(private val setMode: NightSetMode) {
    @get:Rule
    val activityRule = NightModeActivityTestRule(NightModePreventOverrideConfigActivity::class.java)

    @Test
    fun testActivityRecreate() {
        // Activity should be able to reach fully resumed state in default NIGHT_NO.
        waitUntilState(activityRule.activity, Lifecycle.State.RESUMED)
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_NO,
            activityRule.activity.resources.configuration
        )

        // Simulate the user setting night mode, which should force an activity recreate().
        setNightModeAndWaitForRecreate(
            activityRule,
            MODE_NIGHT_YES,
            setMode
        )

        // Activity should be able to reach fully resumed state again.
        waitUntilState(activityRule.activity, Lifecycle.State.RESUMED)

        // The requested night mode value should have been set by
        // updateResourcesConfigurationForNightMode().
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activityRule.activity.resources.configuration
        )
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
