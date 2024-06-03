/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.layout.util

import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestActivity
import androidx.window.WindowTestUtils.Companion.assumePlatformROrAbove
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [BoundsHelper]. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BoundsHelperTest {

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    fun testCurrentWindowBounds_postR() {
        assumePlatformROrAbove()
        activityScenarioRule.scenario.onActivity { activity ->
            val currentBounds = BoundsHelper.getInstance().currentWindowBounds(activity)
            val expectedBounds =
                activity.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
            assertEquals(expectedBounds, currentBounds)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    fun testMaximumWindowBounds_postR() {
        assumePlatformROrAbove()
        activityScenarioRule.scenario.onActivity { activity ->
            val currentBounds = BoundsHelper.getInstance().maximumWindowBounds(activity)
            val expectedBounds =
                activity.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            assertEquals(expectedBounds, currentBounds)
        }
    }
}
