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

import android.app.Activity
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that will sub out the actual [WindowMetricsCalculator] with a more simple one that
 * will support testing independent of the current platform. The fake [WindowMetricsCalculator] that
 * is used will return the width and height from the [android.util.DisplayMetrics] associated to an
 * [Activity]. The result of [WindowMetricsCalculator.computeCurrentWindowMetrics] and
 * [WindowMetricsCalculator.computeMaximumWindowMetrics] will be the same. For accurate results use
 * the Espresso Test framework with an actual [Activity] and use the actual
 * [WindowMetricsCalculator].
 */
class WindowMetricsCalculatorRule : TestRule {

    private val stubWindowMetricsCalculator = StubWindowMetricsCalculator()
    private val decorator = StubMetricDecorator(stubWindowMetricsCalculator)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                WindowMetricsCalculator.overrideDecorator(decorator)
                try {
                    base.evaluate()
                } finally {
                    WindowMetricsCalculator.reset()
                }
            }
        }
    }

    /** Overrides the window bounds with a new [WindowMetrics]. */
    fun overrideCurrentWindowBounds(windowMetrics: WindowMetrics) {
        stubWindowMetricsCalculator.overrideWindowBounds(windowMetrics.bounds)
    }

    /** Overrides the window bounds with a new rectangle defined by the specified coordinates. */
    fun overrideCurrentWindowBounds(left: Int, top: Int, right: Int, bottom: Int) {
        stubWindowMetricsCalculator.overrideWindowBounds(left, top, right, bottom)
    }
}
