/*
 * Copyright 2020 The Android Open Source Project
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
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NightModeLocalBeforeAttachBaseTestCase {
    @get:Rule
    val rule = NightModeActivityTestRule(
        NightModeLocalBeforeAttachBaseActivity::class.java,
        launchActivity = false
    )

    @Test
    fun testLocalDayNightBeforeAttachBaseContext() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val counter = CreatedActivityCounter()

        try {
            application.registerActivityLifecycleCallbacks(counter)

            // Now launch the Activity and wait for everything to settle down
            rule.launchActivity(null)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        } finally {
            application.unregisterActivityLifecycleCallbacks(counter)
        }

        // Assert that the Activity was created only once (no recreates)
        assertEquals(1, counter.createdCount)
        // ...and that it is night mode (set in the Activity)
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rule.activity)
    }

    private class CreatedActivityCounter : Application.ActivityLifecycleCallbacks {
        var createdCount = 0
            private set

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity.javaClass == NightModeLocalBeforeAttachBaseActivity::class.java) {
                createdCount++
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
