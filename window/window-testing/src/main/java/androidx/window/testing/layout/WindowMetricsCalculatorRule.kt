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
import androidx.window.layout.WindowMetricsCalculator
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that will sub out the actual [WindowMetricsCalculator] with a more simple one that
 * will support testing independent of the current platform. The fake [WindowMetricsCalculator] that
 * is used will return the width and height from the [android.util.DisplayMetrics] associated to
 * an [Activity]. The result of [WindowMetricsCalculator.computeCurrentWindowMetrics] and
 * [WindowMetricsCalculator.computeMaximumWindowMetrics] will be the same. For accurate results use
 * the Espresso Test framework with an actual [Activity] and use the actual
 * [WindowMetricsCalculator].
 */
class WindowMetricsCalculatorRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                WindowMetricsCalculator.overrideDecorator(StubMetricDecorator)
                try {
                    base.evaluate()
                } finally {
                    WindowMetricsCalculator.reset()
                }
            }
        }
    }
}