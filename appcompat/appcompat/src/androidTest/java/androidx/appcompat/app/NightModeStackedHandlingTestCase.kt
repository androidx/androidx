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

@file:Suppress("deprecation")

package androidx.appcompat.app

import android.app.Activity
import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeUtils
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
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
        val activityA = monitorA.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityA)
        activityA.startActivity(
            Intent(instr.context, NightModeActivityB::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "B")
            }
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C.
        val activityB = monitorB.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityB)
        activityB.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivity::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "C")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB, Lifecycle.State.CREATED)

        // Toggle default night mode.
        val activityC = monitorC.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityC)
        activityC.runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        }

        // Activity C should receive a configuration change.
        activityC.expectOnConfigurationChange(3000)

        // Activities A and B should recreate() in the background.
        val activityA2 = expectRecreate(monitorA, activityA) as NightModeActivity
        val activityB2 = expectRecreate(monitorB, activityB) as NightModeActivity

        // Activity C should have received a night mode configuration change.
        listOf(activityC, activityA2, activityB2).forEach { activity ->
            activityC.runOnUiThread {
                NightModeUtils.assertConfigurationNightModeEquals(
                    "Activity ${activity.title}'s effective configuration has night mode set",
                    Configuration.UI_MODE_NIGHT_YES,
                    activityC.effectiveConfiguration!!
                )
            }
        }
    }

    /**
     * Regression test for the following scenario:
     *
     * If you have a stack of activities where every activity has `android:configChanges="uiMode"`
     * and you call [AppCompatDelegate.setDefaultNightMode] from thread other than the top
     * activity, then it can cause the bottom activity to not receive `onConfigurationChanged`.
     *
     * Eg:
     * - Activity A DOES intercept uiMode config changes in manifest
     * - Activity B DOES as well
     * - Activity C DOES as well
     *
     * Here is your stack : A > B > C (C on top)
     *
     * Call [AppCompatDelegate.setDefaultNightMode] with a new mode on activity C (but not directly
     * from this activity, ex with RX AndroidSchedulers.mainThread or an handler). Activity C
     * receives both `onConfigurationChanged` and `onNightModeChanged`, but activities A and B
     * may not receive either callback or change their configurations.
     *
     * Process:
     * 1. A > B > C > setDefaultNightMode YES
     * 2. Go back to A (B & C destroyed) > B > C > setDefaultNightMode NO (wrong config for A)
     * 3. repeat (YES/NO/YES/NO...)
     */
    @Test
    @SdkSuppress(minSdkVersion = 17)
    public fun testDefaultNightModeWithStackedActivitiesAndNavigation() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val result = Instrumentation.ActivityResult(0, Intent())
        val monitorA = ActivityMonitor(
            NightModeUiModeConfigChangesActivity::class.java.name,
            result, false
        )
        val monitorB = ActivityMonitor(
            NightModeUiModeConfigChangesActivityB::class.java.name,
            result, false
        )
        val monitorC = ActivityMonitor(
            NightModeUiModeConfigChangesActivityC::class.java.name,
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
            Intent(instr.context, NightModeUiModeConfigChangesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(NightModeActivity.KEY_TITLE, "A")
            }
        )

        // From activity A, start activity B.
        val activityA = monitorA.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull("Activity A started within 3000ms", activityA)
        activityA.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivityB::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "B")
            }
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C.
        val activityB = monitorB.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull("Activity B started within 3000ms", activityB)
        activityB.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivityC::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "C")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB, Lifecycle.State.CREATED)

        // Wait for activity C to start.
        val activityC = monitorC.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull("Activity C started within 3000ms", activityC)

        // Toggle default night mode from a non-UI thread.
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        }

        // Activities A, B, and C should all receive configuration changes.
        listOf(activityA, activityB, activityC).forEach { activity ->
            activity.expectOnConfigurationChange(3000)
        }

        // Activities A, B, and C should have all received the new configuration.
        listOf(activityA, activityB, activityC).forEach { activity ->
            activity.runOnUiThread {
                NightModeUtils.assertConfigurationNightModeEquals(
                    "Activity ${activity.title}'s effective configuration has night mode set",
                    Configuration.UI_MODE_NIGHT_YES,
                    activity.effectiveConfiguration!!
                )
            }
        }

        // Tear down activities C and B, in that order.
        listOf(activityC, activityB).forEach { activity ->
            activity.runOnUiThread {
                activity.finish()
            }
            waitUntilState(activity, Lifecycle.State.DESTROYED)
        }

        // Activity A is in the foreground, wait for it to resume.
        waitUntilState(activityA, Lifecycle.State.RESUMED)

        // From activity A, start activity B again.
        activityA.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivityB::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "B2")
            }
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C. Double-check the return, since the monitor could
        // trigger on Activity B's lifecycle if the platform does something unexpected.
        val activityB2 = monitorB.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotSame("Monitor responded to activity B2 lifecycle", activityB, activityB2)
        assertNotNull("Activity B2 started within 3000ms", activityB2)
        activityB2.startActivity(
            Intent(instr.context, NightModeUiModeConfigChangesActivityC::class.java).apply {
                putExtra(NightModeActivity.KEY_TITLE, "C2")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB2, Lifecycle.State.CREATED)

        // Wait for activity C to start. Double-check the return.
        val activityC2 = monitorC.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotSame("Monitor responded to Activity C2 lifecycle", activityC, activityC2)
        assertNotNull("Activity C2 started within 3000ms", activityC2)

        // Prepare activities A, B, and C to track configuration changes.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.resetOnConfigurationChange()
        }

        // Toggle default night mode again from a non-UI thread.
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        }

        // Activities A, B, and C should all receive configuration changes.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.expectOnConfigurationChange(3000)
        }

        // Activities A, B, and C should have all received the new configuration.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.runOnUiThread {
                NightModeUtils.assertConfigurationNightModeEquals(
                    "Activity ${activity.title}'s effective configuration has night mode set",
                    Configuration.UI_MODE_NIGHT_NO,
                    activity.effectiveConfiguration!!
                )
            }
        }
    }

    private fun expectRecreate(monitor: ActivityMonitor, activity: Activity): Activity {
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
