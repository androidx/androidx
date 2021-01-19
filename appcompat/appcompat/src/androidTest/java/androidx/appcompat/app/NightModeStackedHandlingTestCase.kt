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

@file:Suppress("DEPRECATION")

package androidx.appcompat.app

import android.app.Activity
import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
public class NightModeStackedHandlingTestCase {

    /**
     * Regression test for the following scenario:
     *
     * If you have a stack of activities which includes one with android:configChanges="uiMode"
     * and you call AppCompatDelegate.setDefaultNightMode it can cause other activities to not be
     * recreated.
     *
     * Eg:
     * - Activity A DOESN'T intercept uiMode config changes in manifest
     * - Activity B DOESN'T intercept uiMode config changes in manifest
     * - Activity C DOES
     *
     * Here is your stack : A > B > C (C on top)
     *
     * Call AppCompatDelegate.setDefaultNightMode with a new mode on activity C. Activity C
     * receives the change in onConfigurationChanged but there is a good chance that activity A
     * and/or B were not recreated.
     */
    @Test
    @SdkSuppress(minSdkVersion = 17)
    public fun testDefaultNightModeWithStackedActivities() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val result = Instrumentation.ActivityResult(0, Intent())
        val monitorA = ActivityMonitor(NightModeActivityA::class.java.name, result, false)
        val monitorB = ActivityMonitor(NightModeActivityB::class.java.name, result, false)
        val monitorC = ActivityMonitor(
            NightModeUiModeConfigChangesActivity::class.java.name,
            result, false
        )
        instr.addMonitor(monitorA)
        instr.addMonitor(monitorB)
        instr.addMonitor(monitorC)

        instr.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        }

        // Start activity A.
        instr.startActivitySync(
            Intent(instr.context, NightModeActivityA::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(NightModeActivity.KEY_TITLE, "A")
            }
        )

        // From activity A, start activity B.
        var activityA = monitorA.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityA)
        activityA.startActivity(
            Intent(instr.context, NightModeActivityB::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "B")
            }
        )

        // From activity B, start activity C.
        val activityB = monitorB.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityB)
        activityB.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivity::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "C")
            }
        )

        // Toggle default night mode.
        val activityC = monitorC.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityC)
        activityC.runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        }

        // Activity C should receive a configuration change.
        activityC.expectOnConfigurationChange(3000)

        // Activities A and B should recreate().
        val activityA2 = expectRecreate(monitorA, activityA) as NightModeActivity
        val activityB2 = expectRecreate(monitorB, activityB) as NightModeActivity

        // Activity C should have received a night mode configuration change.
        activityC.runOnUiThread {
            NightModeUtils.assertConfigurationNightModeEquals(
                "Activity A's effective configuration has night mode set",
                Configuration.UI_MODE_NIGHT_YES,
                activityC.effectiveConfiguration!!
            )
        }

        // Activity A should have been recreated in night mode.
        activityA2.runOnUiThread {
            NightModeUtils.assertConfigurationNightModeEquals(
                "Activity A's effective configuration has night mode set",
                Configuration.UI_MODE_NIGHT_YES,
                activityA2.effectiveConfiguration!!
            )
        }

        // Activity B should have been recreated in night mode.
        activityB2.runOnUiThread {
            NightModeUtils.assertConfigurationNightModeEquals(
                "Activity B's effective configuration has night mode set",
                Configuration.UI_MODE_NIGHT_YES,
                activityB2.effectiveConfiguration!!
            )
        }
    }

    fun expectRecreate(monitor: ActivityMonitor, activity: Activity): Activity {
        // The documentation says "Block until an Activity is created that matches this monitor."
        // This statement is true, but there are some other true statements like: "Block until an
        // Activity is destroyed" or "Block until an Activity is resumed"...
        var activityResult: Activity?
        synchronized(monitor) {
            do {
                // this call will release synchronization monitor's monitor
                activityResult = monitor.waitForActivityWithTimeout(3000)
            } while (activityResult != null && activityResult == activity)
        }

        assertNotNull("Recreated activity " + activity.title, activityResult)
        return activityResult!!
    }
}
