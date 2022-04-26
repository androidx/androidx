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

import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
public class NightModeRotationConfigChangesTestCase {

    @Before
    public fun setup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @After
    public fun teardown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    /**
     * Regression test for activities that specify a default night mode outside of attachBaseContext
     * and handle uiMode configuration changes.
     */
    @Test
    @SdkSuppress(minSdkVersion = 17)
    public fun testDefaultNightModeWithHandledRotation() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val result = Instrumentation.ActivityResult(0, Intent())
        val monitorA = ActivityMonitor(
            NightModeRotationConfigChangesActivityA::class.java.name, result, false
        )
        val monitorB = ActivityMonitor(
            NightModeRotationConfigChangesActivityB::class.java.name, result, false
        )
        instr.addMonitor(monitorA)
        instr.addMonitor(monitorB)

        // Start activity A, which sets default night mode YES in onCreate.
        instr.startActivitySync(
            Intent(instr.context, NightModeRotationConfigChangesActivityA::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
        )
        val activityA = monitorA.waitForActivityWithTimeout(3000) as NightModeActivity
        assertNotNull(activityA)
        waitUntilState(activityA, Lifecycle.State.RESUMED)

        // Check that the initial state is correct.
        assertEquals(
            UI_MODE_NIGHT_YES,
            activityA.resources.configuration.uiMode and UI_MODE_NIGHT_MASK
        )

        // Start activity B, which forces the device into landscape mode.
        instr.startActivitySync(
            Intent(instr.context, NightModeRotationConfigChangesActivityB::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
        )
        val activityB = monitorB.waitForActivityWithTimeout(3000) as ComponentActivity
        assertNotNull(activityB)
        waitUntilState(activityB, Lifecycle.State.RESUMED)

        // Activity A is hidden, wait for it to stop.
        waitUntilState(activityA, Lifecycle.State.CREATED)

        // Stop Activity B and wait for it to be destroyed.
        instr.runOnMainSync {
            activityB.finish()
        }
        waitUntilState(activityB, Lifecycle.State.DESTROYED)

        // Wait for Activity A to resume.
        waitUntilState(activityA, Lifecycle.State.RESUMED)

        // Check that the final state is correct.
        assertEquals(
            UI_MODE_NIGHT_YES,
            activityA.resources.configuration.uiMode and UI_MODE_NIGHT_MASK
        )
    }
}
