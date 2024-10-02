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
import android.content.ContextWrapper
import android.os.Build
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestActivity
import androidx.window.TestActivityEdgeToEdge
import androidx.window.WindowTestUtils.Companion.assumePlatformBeforeR
import androidx.window.WindowTestUtils.Companion.assumePlatformROrAbove
import androidx.window.WindowTestUtils.Companion.assumePlatformUOrAbove
import androidx.window.WindowTestUtils.Companion.isInMultiWindowMode
import androidx.window.WindowTestUtils.Companion.runActionsAcrossActivityLifecycle
import androidx.window.layout.util.DisplayHelper.getRealSizeForDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [WindowMetricsCalculatorCompat] class. */
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
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val bounds =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity).bounds
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
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val bounds =
                WindowMetricsCalculatorCompat().computeMaximumWindowMetrics(activity).bounds
            val windowMetricsBounds = activity.windowManager.maximumWindowMetrics.bounds
            assertEquals(windowMetricsBounds, bounds)
        }
    }

    @Test
    fun testDensityMatchesDisplayMetricsDensity() {
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val calculator = WindowMetricsCalculatorCompat()
            val windowMetrics = calculator.computeCurrentWindowMetrics(activity)
            val maxWindowMetrics = calculator.computeMaximumWindowMetrics(activity)
            assertEquals(activity.resources.displayMetrics.density, windowMetrics.density)
            assertEquals(windowMetrics.density, maxWindowMetrics.density)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testConvertedWindowMetricsMatchesPlatformWindowMetrics() {
        assumePlatformUOrAbove()
        runActionsAcrossActivityLifecycle(activityScenarioRule, {}) { activity: TestActivity ->
            val calculator = WindowMetricsCalculatorCompat()
            val windowMetrics = calculator.computeCurrentWindowMetrics(activity)
            val wm = activity.getSystemService(WindowManager::class.java)
            val androidWindowMetrics = wm.currentWindowMetrics
            assertEquals(androidWindowMetrics.bounds, windowMetrics.bounds)
            assertEquals(androidWindowMetrics.density, windowMetrics.density)
        }
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testDpBoundsMatchCalculatedDimension() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        activityScenarioRule.scenario.onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
            val displayMetrics = activity.resources.displayMetrics
            val widthDp =
                TypedValue.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    windowMetrics.bounds.width().toFloat(),
                    displayMetrics
                )
            val heightDp =
                TypedValue.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    windowMetrics.bounds.height().toFloat(),
                    displayMetrics
                )

            assertEquals(
                "Width DP must be within 1dp of configuration value.",
                widthDp,
                windowMetrics.widthDp,
                1f
            )
            assertEquals(
                "Height DP must be within 1dp of configuration value.",
                heightDp,
                windowMetrics.heightDp,
                1f
            )
        }
    }

    @Test
    fun testWindowMetricBoundsMatchesEdgeToEdgeFullScreenView() {
        ActivityScenario.launch(TestActivityEdgeToEdge::class.java).onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
            val rootView = activity.findViewById<FrameLayout>(androidx.window.test.R.id.view_home)

            assertEquals(
                "Full screen view width must match window metrics width",
                windowMetrics.bounds.width(),
                rootView.width
            )
            assertEquals(
                "Full screen view height must match window metrics height",
                windowMetrics.bounds.height(),
                rootView.height
            )
        }
    }

    private fun testGetCurrentWindowBoundsMatchesRealDisplaySize(
        initialAction: ActivityAction<TestActivity>
    ) {
        val assertWindowBoundsMatchesDisplayAction: ActivityAction<TestActivity> =
            AssertCurrentWindowBoundsEqualsRealDisplaySizeAction()
        runActionsAcrossActivityLifecycle(
            activityScenarioRule,
            initialAction,
            assertWindowBoundsMatchesDisplayAction
        )
    }

    private fun testGetMaximumWindowBoundsMatchesRealDisplaySize(
        initialAction: ActivityAction<TestActivity>
    ) {
        val assertWindowBoundsMatchesDisplayAction: ActivityAction<TestActivity> =
            AssertMaximumWindowBoundsEqualsRealDisplaySizeAction()
        runActionsAcrossActivityLifecycle(
            activityScenarioRule,
            initialAction,
            assertWindowBoundsMatchesDisplayAction
        )
    }

    private fun assumeNotMultiWindow() {
        val scenario = activityScenarioRule.scenario
        try {
            scenario.onActivity { activity: TestActivity ->
                Assume.assumeFalse(isInMultiWindowMode(activity))
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
            val display: Display =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display ?: throw Exception("No display for activity")
                } else {
                    @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
                }
            val calculator = WindowMetricsCalculatorCompat()
            val realDisplaySize = getRealSizeForDisplay(display)
            val bounds = calculator.computeCurrentWindowMetrics(activity).bounds
            assertNotEquals("Device can not have zero width", 0, realDisplaySize.x.toLong())
            assertNotEquals("Device can not have zero height", 0, realDisplaySize.y.toLong())
            assertEquals(
                "Window bounds width does not match real display width",
                realDisplaySize.x.toLong(),
                bounds.width().toLong()
            )
            assertEquals(
                "Window bounds height does not match real display height",
                realDisplaySize.y.toLong(),
                bounds.height().toLong()
            )
        }
    }

    private class AssertMaximumWindowBoundsEqualsRealDisplaySizeAction :
        ActivityAction<TestActivity> {
        override fun perform(activity: TestActivity) {
            val display: Display =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display ?: throw Exception("No display for activity")
                } else {
                    @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
                }
            val calculator = WindowMetricsCalculatorCompat()
            val realDisplaySize = getRealSizeForDisplay(display)
            val bounds = calculator.computeMaximumWindowMetrics(activity).bounds
            assertEquals(
                "Window bounds width does not match real display width",
                realDisplaySize.x.toLong(),
                bounds.width().toLong()
            )
            assertEquals(
                "Window bounds height does not match real display height",
                realDisplaySize.y.toLong(),
                bounds.height().toLong()
            )
        }
    }
}
