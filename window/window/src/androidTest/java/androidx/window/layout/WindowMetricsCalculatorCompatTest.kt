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
package androidx.window.layout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestActivity
import androidx.window.core.ExperimentalWindowApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [WindowMetricsCalculatorCompat] class.  */
@LargeTest
@RunWith(AndroidJUnit4::class)
class WindowMetricsCalculatorCompatTest {

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun testGetCurrentWindowBounds_matchParentWindowSize_avoidCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetCurrentWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetCurrentWindowBounds_withWrappedContext() {
        activityScenarioRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()

            // Test that this does not crash.
            calculator.computeCurrentWindowMetrics(ContextWrapper(activity))
        }
    }

    @Test
    fun testGetCurrentWindowBounds_fixedWindowSize_avoidCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetCurrentWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = 100
            lp.height = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetCurrentWindowBounds_matchParentWindowSize_layoutBehindCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetCurrentWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetCurrentWindowBounds_fixedWindowSize_layoutBehindCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetCurrentWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = 100
            lp.height = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            activity.window.attributes = lp
        }
    }

    @SuppressLint("NewApi")
    @Test
    fun testGetCurrentWindowBounds_postR() {
        assumePlatformROrAbove()
        runActionsAcrossActivityLifecycle({ }) { activity: TestActivity ->
            val bounds = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(activity).bounds
            val windowMetricsBounds = activity.windowManager.currentWindowMetrics.bounds
            assertEquals(windowMetricsBounds, bounds)
        }
    }

    @Test
    fun testGetMaximumWindowBounds_matchParentWindowSize_avoidCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetMaximumWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetMaximumWindowBounds_fixedWindowSize_avoidCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetMaximumWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = 100
            lp.height = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetMaximumWindowBounds_matchParentWindowSize_layoutBehindCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetMaximumWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            activity.window.attributes = lp
        }
    }

    @Test
    fun testGetMaximumWindowBounds_fixedWindowSize_layoutBehindCutouts_preR() {
        assumePlatformBeforeR()
        assumeNotMultiWindow()
        testGetMaximumWindowBoundsMatchesRealDisplaySize { activity: TestActivity ->
            val lp = activity.window.attributes
            lp.width = 100
            lp.height = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            activity.window.attributes = lp
        }
    }

    @SuppressLint("NewApi")
    @Test
    fun testGetMaximumWindowBounds_postR() {
        assumePlatformROrAbove()
        runActionsAcrossActivityLifecycle({ }) { activity: TestActivity ->
            val bounds = WindowMetricsCalculatorCompat.computeMaximumWindowMetrics(activity).bounds
            val windowMetricsBounds = activity.windowManager.maximumWindowMetrics.bounds
            assertEquals(windowMetricsBounds, bounds)
        }
    }

    @SuppressLint("NewApi")
    @Test
    @OptIn(ExperimentalWindowApi::class)
    fun testGetWindowInsetsCompat_currentWindowMetrics_postR() {
        assumePlatformROrAbove()
        runActionsAcrossActivityLifecycle({ }) { activity: TestActivity ->
            val windowMetrics = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(activity)
            val windowInsets = windowMetrics.getWindowInsets()
            val platformInsets = activity.windowManager.currentWindowMetrics.windowInsets
            val platformWindowInsets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
            assertEquals(platformWindowInsets, windowInsets)
        }
    }

    @SuppressLint("NewApi")
    @Test
    @OptIn(ExperimentalWindowApi::class)
    fun testGetWindowInsetsCompat_maximumWindowMetrics_postR() {
        assumePlatformROrAbove()
        runActionsAcrossActivityLifecycle({ }) { activity: TestActivity ->
            val windowMetrics = WindowMetricsCalculatorCompat.computeMaximumWindowMetrics(activity)
            val windowInsets = windowMetrics.getWindowInsets()
            val platformInsets = activity.windowManager.maximumWindowMetrics.windowInsets
            val platformWindowInsets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
            assertEquals(platformWindowInsets, windowInsets)
        }
    }

    private fun testGetCurrentWindowBoundsMatchesRealDisplaySize(
        initialAction: ActivityAction<TestActivity>
    ) {
        val assertWindowBoundsMatchesDisplayAction: ActivityAction<TestActivity> =
            AssertCurrentWindowBoundsEqualsRealDisplaySizeAction()
        runActionsAcrossActivityLifecycle(initialAction, assertWindowBoundsMatchesDisplayAction)
    }

    private fun testGetMaximumWindowBoundsMatchesRealDisplaySize(
        initialAction: ActivityAction<TestActivity>
    ) {
        val assertWindowBoundsMatchesDisplayAction: ActivityAction<TestActivity> =
            AssertMaximumWindowBoundsEqualsRealDisplaySizeAction()
        runActionsAcrossActivityLifecycle(initialAction, assertWindowBoundsMatchesDisplayAction)
    }

    /**
     * Creates and launches an activity performing the supplied actions at various points in the
     * activity lifecycle.
     *
     * @param initialAction the action that will run once before the activity is created.
     * @param verifyAction the action to run once after each change in activity lifecycle state.
     */
    private fun runActionsAcrossActivityLifecycle(
        initialAction: ActivityAction<TestActivity>,
        verifyAction: ActivityAction<TestActivity>
    ) {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity(initialAction)
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity(verifyAction)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity(verifyAction)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity(verifyAction)
    }

    private fun assumeNotMultiWindow() {
        val scenario = activityScenarioRule.scenario
        try {
            scenario.onActivity { activity: TestActivity ->
                Assume.assumeFalse(
                    isInMultiWindowMode(
                        activity
                    )
                )
            }
        } catch (e: RuntimeException) {
            if (e.cause is AssumptionViolatedException) {
                val failedAssumption = e.cause as AssumptionViolatedException?
                throw failedAssumption!!
            }
            throw e
        }
    }

    private class AssertCurrentWindowBoundsEqualsRealDisplaySizeAction :
        ActivityAction<TestActivity> {
        override fun perform(activity: TestActivity) {
            val display: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: throw Exception("No display for activity")
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
            val realDisplaySize = WindowMetricsCalculatorCompat.getRealSizeForDisplay(display)
            val bounds = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(activity).bounds
            assertNotEquals("Device can not have zero width", 0, realDisplaySize.x.toLong())
            assertNotEquals("Device can not have zero height", 0, realDisplaySize.y.toLong())
            assertEquals(
                "Window bounds width does not match real display width",
                realDisplaySize.x.toLong(), bounds.width().toLong()
            )
            assertEquals(
                "Window bounds height does not match real display height",
                realDisplaySize.y.toLong(), bounds.height().toLong()
            )
        }
    }

    private class AssertMaximumWindowBoundsEqualsRealDisplaySizeAction :
        ActivityAction<TestActivity> {
        override fun perform(activity: TestActivity) {
            val display: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: throw Exception("No display for activity")
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
            val realDisplaySize = WindowMetricsCalculatorCompat.getRealSizeForDisplay(display)
            val bounds = WindowMetricsCalculatorCompat.computeMaximumWindowMetrics(activity).bounds
            assertEquals(
                "Window bounds width does not match real display width",
                realDisplaySize.x.toLong(), bounds.width().toLong()
            )
            assertEquals(
                "Window bounds height does not match real display height",
                realDisplaySize.y.toLong(), bounds.height().toLong()
            )
        }
    }

    private companion object {
        private fun isInMultiWindowMode(activity: Activity): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity.isInMultiWindowMode
            } else false
        }

        private fun assumePlatformBeforeR() {
            Assume.assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        }

        private fun assumePlatformROrAbove() {
            Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        }
    }
}
