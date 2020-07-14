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
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForRecreate
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import junit.framework.TestCase.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NightModeForegroundDialogTestCase {
    @get:Rule
    val rule = NightModeActivityTestRule(NightModeActivity::class.java)

    @Test
    fun testNightModeChangeWithForegroundDialog() {
        val firstActivity = rule.activity
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO, firstActivity)

        // Open a dialog on top of the activity.
        rule.runOnUiThread {
            val frag = NightModeDialogFragment.newInstance()
            frag.show(firstActivity.supportFragmentManager, "dialog")
        }

        // Now change the DayNight mode on the foreground activity.
        setNightModeAndWaitForRecreate(
            firstActivity,
            MODE_NIGHT_YES,
            NightSetMode.DEFAULT
        )

        // Ensure that it was recreated.
        assertNotSame(rule.activity, firstActivity)
        waitUntilState(rule.activity, Lifecycle.State.RESUMED)
    }
}
