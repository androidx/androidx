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
package androidx.metrics.performance.test

import androidx.metrics.performance.JankStats
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copy of JankStatsTest which verifies behavior in software rendering, as a regression test for
 * b/436880904
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SoftwareJankStatsTest {
    private lateinit var jankStats: JankStats

    @Rule
    @JvmField
    var delayedActivityRule: ActivityScenarioRule<SoftwareActivity> =
        ActivityScenarioRule(SoftwareActivity::class.java)

    @Before
    fun setup() {
        delayedActivityRule.scenario.onActivity { activity: DelayedActivity ->
            jankStats =
                JankStats.createAndTrack(activity.window) {
                    fail("Software renderer should never emit frames")
                }
        }
    }

    @Test
    fun testDisableSoftware() {
        assertTrue(jankStats.isTrackingEnabled)
        jankStats.isTrackingEnabled = false // this would previously crash, see b/436880904
        delayedActivityRule.scenario.onActivity {} // just used to sync on main thread
    }
}
