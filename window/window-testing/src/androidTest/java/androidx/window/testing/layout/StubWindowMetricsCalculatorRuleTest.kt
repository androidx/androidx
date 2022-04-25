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

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.core.ExperimentalWindowApi
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
 * A test class for [StubWindowMetricsCalculatorRule] that tests using
 * [StubWindowMetricsCalculator] instead of the actual implementation.
 */
@OptIn(ExperimentalWindowApi::class)
class StubWindowMetricsCalculatorRuleTest {
    private val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private val stubWindowMetricsCalculatorRule = StubWindowMetricsCalculatorRule()

    @get:Rule
    val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(stubWindowMetricsCalculatorRule).around(activityRule)
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
                StubWindowMetricsCalculatorRule().apply(
                    object : Statement() {
                        override fun evaluate() {
                            throw TestException
                        }
                    },
                    Description.EMPTY
                ).evaluate()
            } catch (e: TestException) {
                // Throw unexpected exceptions.
            }
            assertEquals(expected, WindowMetricsCalculator.getOrCreate())
        }
    }

    private object TestException : Exception("TEST EXCEPTION")
}