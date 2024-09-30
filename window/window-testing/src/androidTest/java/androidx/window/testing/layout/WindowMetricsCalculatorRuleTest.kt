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

package androidx.window.testing.layout

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.TestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A test class for [WindowMetricsCalculatorRule] that tests using [StubWindowMetricsCalculator]
 * instead of the actual implementation.
 */
class WindowMetricsCalculatorRuleTest {
    private val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private val mWindowMetricsCalculatorRule = WindowMetricsCalculatorRule()

    // Set the override values to really weird numbers so they don't conflict with any real values.
    private val overrideLeft = 100
    private val overrideTop = 200
    private val overrideWidth = 300
    private val overrideHeight = 400
    private val overrideRect = Rect(overrideLeft, overrideTop, overrideWidth, overrideHeight)

    @get:Rule val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(mWindowMetricsCalculatorRule).around(activityRule)
    }

    @Test
    fun testCurrentWindowMetrics_matchesDisplayMetrics() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val displayMetrics = activity.resources.displayMetrics

            val actual = calculator.computeCurrentWindowMetrics(activity)

            assertEquals(0, actual.bounds.left)
            assertEquals(0, actual.bounds.top)
            assertEquals(displayMetrics.widthPixels, actual.bounds.right)
            assertEquals(displayMetrics.heightPixels, actual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_matchesMaximumMetrics() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()

            val currentMetrics = calculator.computeCurrentWindowMetrics(activity)
            val maximumMetrics = calculator.computeMaximumWindowMetrics(activity)

            assertEquals(currentMetrics.bounds.left, maximumMetrics.bounds.left)
            assertEquals(currentMetrics.bounds.top, maximumMetrics.bounds.top)
            assertEquals(currentMetrics.bounds.right, maximumMetrics.bounds.right)
            assertEquals(currentMetrics.bounds.bottom, maximumMetrics.bounds.bottom)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    fun testCurrentWindowMetrics_context_matchesWindowMetricsMetrics_30AndAbove() {
        Utils.assumePlatformAtOrAbove(Build.VERSION_CODES.R)

        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val wm = activity.getSystemService(WindowManager::class.java)
            val windowMetrics = wm.currentWindowMetrics.bounds
            val actual = calculator.computeCurrentWindowMetrics(activity as Context)

            assertEquals(0, actual.bounds.left)
            assertEquals(0, actual.bounds.top)
            assertEquals(windowMetrics.width(), actual.bounds.right)
            assertEquals(windowMetrics.height(), actual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_context_matchesDisplayRealSize_17to29() {
        Utils.assumePlatformAtOrBelow(Build.VERSION_CODES.Q)

        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displaySize = Point()
            // DefaultDisplay#getRealSize is used in StubWindowMetricsCalculator for compatibility
            // with older versions. We're just asserting that the value via
            // StubWindowMetricsCalculator#computeCurrentWindowMetrics is equal to this.
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealSize(displaySize)
            val actual = calculator.computeCurrentWindowMetrics(activity as Context)

            assertEquals(0, actual.bounds.left)
            assertEquals(0, actual.bounds.top)
            assertEquals(displaySize.x, actual.bounds.right)
            assertEquals(displaySize.y, actual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_context_matchesMaximumMetrics() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()

            val currentMetrics = calculator.computeCurrentWindowMetrics(activity as Context)
            val maximumMetrics = calculator.computeMaximumWindowMetrics(activity as Context)

            assertEquals(currentMetrics.bounds.left, maximumMetrics.bounds.left)
            assertEquals(currentMetrics.bounds.top, maximumMetrics.bounds.top)
            assertEquals(currentMetrics.bounds.right, maximumMetrics.bounds.right)
            assertEquals(currentMetrics.bounds.bottom, maximumMetrics.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_overrideWindowBoundsRect() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val displayMetrics = activity.resources.displayMetrics
            val actual = calculator.computeCurrentWindowMetrics(activity)

            assertEquals(0, actual.bounds.left)
            assertEquals(0, actual.bounds.top)
            assertEquals(displayMetrics.widthPixels, actual.bounds.right)
            assertEquals(displayMetrics.heightPixels, actual.bounds.bottom)

            mWindowMetricsCalculatorRule.overrideCurrentWindowBounds(
                TestWindowMetrics(overrideRect)
            )

            val overrideActual = calculator.computeCurrentWindowMetrics(activity)
            assertEquals(overrideLeft, overrideActual.bounds.left)
            assertEquals(overrideTop, overrideActual.bounds.top)
            assertEquals(overrideWidth, overrideActual.bounds.right)
            assertEquals(overrideHeight, overrideActual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_overrideWindowBoundsCoordinates() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val displayMetrics = activity.resources.displayMetrics
            val actual = calculator.computeCurrentWindowMetrics(activity)

            assertEquals(0, actual.bounds.left)
            assertEquals(0, actual.bounds.top)
            assertEquals(displayMetrics.widthPixels, actual.bounds.right)
            assertEquals(displayMetrics.heightPixels, actual.bounds.bottom)

            mWindowMetricsCalculatorRule.overrideCurrentWindowBounds(
                overrideLeft,
                overrideTop,
                overrideWidth,
                overrideHeight
            )

            val overrideActual = calculator.computeCurrentWindowMetrics(activity)
            assertEquals(overrideLeft, overrideActual.bounds.left)
            assertEquals(overrideTop, overrideActual.bounds.top)
            assertEquals(overrideWidth, overrideActual.bounds.right)
            assertEquals(overrideHeight, overrideActual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_context_overrideWindowBoundsRect() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val currentMetrics = calculator.computeCurrentWindowMetrics(activity as Context)
            val maximumMetrics = calculator.computeMaximumWindowMetrics(activity as Context)

            assertEquals(currentMetrics.bounds.left, maximumMetrics.bounds.left)
            assertEquals(currentMetrics.bounds.top, maximumMetrics.bounds.top)
            assertEquals(currentMetrics.bounds.right, maximumMetrics.bounds.right)
            assertEquals(currentMetrics.bounds.bottom, maximumMetrics.bounds.bottom)

            mWindowMetricsCalculatorRule.overrideCurrentWindowBounds(
                TestWindowMetrics(overrideRect)
            )

            val overrideActual = calculator.computeCurrentWindowMetrics(activity)
            assertEquals(overrideLeft, overrideActual.bounds.left)
            assertEquals(overrideTop, overrideActual.bounds.top)
            assertEquals(overrideWidth, overrideActual.bounds.right)
            assertEquals(overrideHeight, overrideActual.bounds.bottom)
        }
    }

    @Test
    fun testCurrentWindowMetrics_context_overrideWindowBoundsCoordinates() {
        activityRule.scenario.onActivity { activity ->
            val calculator = WindowMetricsCalculator.getOrCreate()
            val currentMetrics = calculator.computeCurrentWindowMetrics(activity as Context)
            val maximumMetrics = calculator.computeMaximumWindowMetrics(activity as Context)

            assertEquals(currentMetrics.bounds.left, maximumMetrics.bounds.left)
            assertEquals(currentMetrics.bounds.top, maximumMetrics.bounds.top)
            assertEquals(currentMetrics.bounds.right, maximumMetrics.bounds.right)
            assertEquals(currentMetrics.bounds.bottom, maximumMetrics.bounds.bottom)

            mWindowMetricsCalculatorRule.overrideCurrentWindowBounds(
                overrideLeft,
                overrideTop,
                overrideWidth,
                overrideHeight
            )

            val overrideActual = calculator.computeCurrentWindowMetrics(activity)
            assertEquals(overrideLeft, overrideActual.bounds.left)
            assertEquals(overrideTop, overrideActual.bounds.top)
            assertEquals(overrideWidth, overrideActual.bounds.right)
            assertEquals(overrideHeight, overrideActual.bounds.bottom)
        }
    }

    /**
     * Tests that when applying a [Statement] then the decorator is removed. This is necessary to
     * keep tests hermetic. If this fails on the last test run then the fake implementation of
     * [WindowMetricsCalculator] might be retained for other test classes.
     */
    @Test
    fun testException_resetsFactoryMethod() {
        ActivityScenario.launch(TestActivity::class.java).onActivity {
            WindowMetricsCalculator.reset()
            val expected = WindowMetricsCalculator.getOrCreate()
            try {
                WindowMetricsCalculatorRule()
                    .apply(
                        object : Statement() {
                            override fun evaluate() {
                                throw TestException
                            }
                        },
                        Description.EMPTY
                    )
                    .evaluate()
            } catch (e: TestException) {
                // Throw unexpected exceptions.
            }
            assertEquals(expected, WindowMetricsCalculator.getOrCreate())
        }
    }

    private object TestException : Exception("TEST EXCEPTION")
}
