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

@file:Suppress("deprecation")

package androidx.appcompat.app

import android.app.Activity
import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.appcompat.testutils.LocalesUtils
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesStackedHandlingTestCase {

    @Before
    fun setUp() {
        LocalesUtils.initCustomLocaleList()
    }

    /**
     * Regression test for the following scenario:
     *
     * If you have a stack of activities which includes one with android:configChanges="locale" and
     * android:configChanges="layoutDirection" and you call AppCompatDelegate.setApplicationLocales
     * it can cause other activities to not be recreated.
     *
     * Eg:
     * - Activity A DOESN'T intercept locales changes and layoutDirection changes in manifest
     * - Activity B DOESN'T intercept locales changes and layoutDirection changes in manifest
     * - Activity C DOES intercept both locales and layoutDirection changes in manifest
     *
     * Here is your stack : A > B > C (C on top)
     *
     * Call AppCompatDelegate.setApplicationLocales with a new mode on activity C. Activity C
     * receives the change in onConfigurationChanged but there is a good chance that activity A
     * and/or B were not recreated.
     */
    @Test
    fun testLocalesWithStackedActivities() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val result = Instrumentation.ActivityResult(0, Intent())
        val monitorA = ActivityMonitor(LocalesActivityA::class.java.name, result, false)
        val monitorB = ActivityMonitor(LocalesActivityB::class.java.name, result, false)
        val monitorC = ActivityMonitor(
            LocalesConfigChangesActivity::class.java.name,
            result, false
        )
        instr.addMonitor(monitorA)
        instr.addMonitor(monitorB)
        instr.addMonitor(monitorC)

        instr.runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }

        // Start activity A.
        instr.startActivitySync(
            Intent(instr.context, LocalesActivityA::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(LocalesUpdateActivity.KEY_TITLE, "A")
            }
        )

        // From activity A, start activity B.
        val activityA = monitorA.waitForActivityWithTimeout(/* timeout= */ 3000)
            as LocalesUpdateActivity
        assertNotNull(activityA)
        activityA.startActivity(
            Intent(instr.context, LocalesActivityB::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "B")
            }
        )

        var systemLocales = LocalesUpdateActivity.getConfigLocales(
            activityA.resources.configuration
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C.
        val activityB =
            monitorB.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotNull(activityB)
        activityB.startActivity(
            Intent(instr.context, LocalesConfigChangesActivity::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "C")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB, Lifecycle.State.CREATED)

        // apply CUSTOM_LOCALE_LIST
        val activityC =
            monitorC.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotNull(activityC)
        activityC.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(CUSTOM_LOCALE_LIST)
        }

        // Activity C should receive a configuration change.
        activityC.expectOnConfigurationChange(/* timeout= */ 3000)

        // Activities A and B should recreate() in the background.
        val activityA2 = expectRecreate(monitorA, activityA) as LocalesUpdateActivity
        val activityB2 = expectRecreate(monitorB, activityB) as LocalesUpdateActivity

        var expectedLocales = LocalesUpdateActivity.overlayCustomAndSystemLocales(
            CUSTOM_LOCALE_LIST, systemLocales
        )
        // Activity C should have received a locales configuration change.
        listOf(activityC, activityA2, activityB2).forEach { activity ->
            activityC.runOnUiThread {
                assertConfigurationLocalesEquals(
                    "Activity ${activity.title}'s effective configuration has locales set",
                    expectedLocales,
                    activityC.effectiveConfiguration!!
                )
            }
        }
    }

    /**
     * Regression test for the following scenario:
     *
     * If you have a stack of activities where every activity has `android:configChanges="locale"`
     * and android:configChanges="layoutDirection" and you call
     * [AppCompatDelegate.setApplicationLocales] from thread other than the top activity,
     * then it can cause the bottom activity to not receive `onConfigurationChanged`.
     *
     * Eg:
     * - Activity A DOES intercept locales and layoutDirection changes in manifest
     * - Activity B DOES intercept locales and layoutDirection changes in manifest
     * - Activity C DOES intercept locales and layoutDirection changes in manifest
     *
     * Here is your stack : A > B > C (C on top)
     *
     * Call [AppCompatDelegate.setApplicationLocales] with a new mode on activity C (but not
     * directly
     * from this activity, ex with RX AndroidSchedulers.mainThread or an handler). Activity C
     * receives both `onConfigurationChanged` and `onLocalesChanged`, but activities A and B
     * may not receive either callback or change their configurations.
     *
     * Process:
     * 1. A > B > C > setApplicationLocales YES
     * 2. Go back to A (B & C destroyed) > B > C > setApplicationLocales NO (wrong config for A)
     * 3. repeat (YES/NO/YES/NO...)
     */
    @Test
    fun testLocalesWithStackedActivitiesAndNavigation() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val result = Instrumentation.ActivityResult(0, Intent())
        val monitorA = ActivityMonitor(
            LocalesConfigChangesActivity::class.java.name,
            result, false
        )
        val monitorB = ActivityMonitor(
            LocalesConfigChangesActivityA::class.java.name,
            result, false
        )
        val monitorC = ActivityMonitor(
            LocalesConfigChangesActivityB::class.java.name,
            result, false
        )
        instr.addMonitor(monitorA)
        instr.addMonitor(monitorB)
        instr.addMonitor(monitorC)

        instr.runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }

        // Start activity A.
        instr.startActivitySync(
            Intent(instr.context, LocalesConfigChangesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(LocalesUpdateActivity.KEY_TITLE, "A")
            }
        )

        // From activity A, start activity B.
        val activityA =
            monitorA.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotNull("Activity A started within 3000ms", activityA)
        activityA.startActivity(
            Intent(instr.context, LocalesConfigChangesActivityA::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "B")
            }
        )

        var systemLocales = LocalesUpdateActivity.getConfigLocales(
            activityA.resources.configuration
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C.
        val activityB =
            monitorB.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotNull("Activity B started within 3000ms", activityB)
        activityB.startActivity(
            Intent(instr.context, LocalesConfigChangesActivityB::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "C")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB, Lifecycle.State.CREATED)

        // Wait for activity C to start.
        val activityC =
            monitorC.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotNull("Activity C started within 3000ms", activityC)

        // Change locales from a non-UI thread.
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setApplicationLocales(CUSTOM_LOCALE_LIST)
        }

        // Activities A, B, and C should all receive configuration changes.
        listOf(activityA, activityB, activityC).forEach { activity ->
            activity.expectOnConfigurationChange(/* timeout= */ 3000)
        }

        var expectedLocales = LocalesUpdateActivity.overlayCustomAndSystemLocales(
            CUSTOM_LOCALE_LIST, systemLocales
        )

        // Activities A, B, and C should have all received the new configuration.
        listOf(activityA, activityB, activityC).forEach { activity ->
            activity.runOnUiThread {
                assertConfigurationLocalesEquals(
                    "Activity ${activity.title}'s effective configuration has locales set",
                    expectedLocales,
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
            Intent(instr.context, LocalesConfigChangesActivityA::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "B2")
            }
        )

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // From activity B, start activity C. Double-check the return, since the monitor could
        // trigger on Activity B's lifecycle if the platform does something unexpected.
        val activityB2 =
            monitorB.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotSame("Monitor responded to activity B2 lifecycle", activityB, activityB2)
        assertNotNull("Activity B2 started within 3000ms", activityB2)
        activityB2.startActivity(
            Intent(instr.context, LocalesConfigChangesActivityB::class.java).apply {
                putExtra(LocalesUpdateActivity.KEY_TITLE, "C2")
            }
        )

        // Activity B is hidden, wait for it to stop.
        waitUntilState(activityB2, Lifecycle.State.CREATED)

        // Wait for activity C to start. Double-check the return.
        val activityC2 =
            monitorC.waitForActivityWithTimeout(/* timeout= */ 3000) as LocalesUpdateActivity
        assertNotSame("Monitor responded to Activity C2 lifecycle", activityC, activityC2)
        assertNotNull("Activity C2 started within 3000ms", activityC2)

        // Prepare activities A, B, and C to track configuration changes.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.resetOnConfigurationChange()
        }

        // Change locales again from a non-UI thread.
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }

        // Activities A, B, and C should all receive configuration changes.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.expectOnConfigurationChange(/* timeout= */ 3000)
        }

        // Activities A, B, and C should have all received the new configuration.
        listOf(activityA, activityB2, activityC2).forEach { activity ->
            activity.runOnUiThread {
                assertConfigurationLocalesEquals(
                    "Activity ${activity.title}'s effective configuration has locales set",
                    systemLocales,
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
                activityResult = monitor.waitForActivityWithTimeout(/* timeout= */ 3000)
            } while (activityResult != null && activityResult == activity)
        }

        assertNotNull("Recreated activity " + activity.title, activityResult)
        return activityResult!!
    }

    @After
    fun teardown() {
        LocalesUpdateActivity.teardown()
    }
}
