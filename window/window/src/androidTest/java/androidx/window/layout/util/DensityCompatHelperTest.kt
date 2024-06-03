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

import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics as AndroidWindowMetrics
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestActivity
import androidx.window.WindowTestUtils.Companion.assumePlatformBeforeU
import androidx.window.WindowTestUtils.Companion.assumePlatformROrAbove
import androidx.window.WindowTestUtils.Companion.assumePlatformUOrAbove
import androidx.window.WindowTestUtils.Companion.runActionsAcrossActivityLifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the [DensityCompatHelper] class. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DensityCompatHelperTest {

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun testDensityFromContext_beforeU() {
        assumePlatformBeforeU()
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val helper = DensityCompatHelper.getInstance()
            assertEquals(activity.resources.displayMetrics.density, helper.density(activity))
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testDensityFromContext_UOrAbove() {
        assumePlatformUOrAbove()
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val wm = activity.getSystemService(WindowManager::class.java)
            val helper = DensityCompatHelper.getInstance()
            assertEquals(wm.currentWindowMetrics.density, helper.density(activity))
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testDensityFromConfiguration_beforeU() {
        assumePlatformBeforeU()
        assumePlatformROrAbove()
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val helper = DensityCompatHelper.getInstance()

            @Suppress("DEPRECATION")
            val fakeWindowMetrics =
                AndroidWindowMetrics(
                    Rect(0, 0, 0, 0),
                    WindowInsets.Builder().build(),
                )

            val density = helper.density(activity.resources.configuration, fakeWindowMetrics)
            val expectedDensity =
                activity.resources.configuration.densityDpi.toFloat() /
                    DisplayMetrics.DENSITY_DEFAULT
            assertEquals(expectedDensity, density)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testDensityFromWindowMetrics_UOrAbove() {
        assumePlatformUOrAbove()
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val helper = DensityCompatHelper.getInstance()

            val fakeDensity = 123.456f
            val fakeWindowMetrics =
                AndroidWindowMetrics(Rect(0, 0, 0, 0), WindowInsets.Builder().build(), fakeDensity)

            val density = helper.density(activity.resources.configuration, fakeWindowMetrics)
            assertEquals(fakeDensity, density)
        }
    }
}
