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

package androidx.benchmark.macro

import androidx.annotation.IntRange
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking large app operations like startup.
 */
class MacrobenchmarkRule : TestRule {
    private lateinit var currentDescription: Description

    /**
     * Measure behavior of the specified [packageName] given a set of [metrics].
     *
     * @param packageName Package name of the app being measured.
     * @param metrics List of metrics to measure.
     * @param compilationMode Mode of compilation used before capturing measurement, such as
     * [SPEED_PROFILE], which performs
     * @param startupMode Optional mode to force app launches performed with
     * [MacrobenchmarkScope.launchIntentAndWait] (and similar variants) to be of the assigned
     * type. For example, `COLD` launches kill the process before the measureBlock, to ensure
     * startups will go through full process creation. Generally, leave as null for non-startup
     * benchmarks.
     * @param iterations Number of times the [measureBlock] will be run during measurement.
     * @param measureBlock The block performing app actions to benchmark.
     */
    fun measureRepeated(
        packageName: String,
        metrics: List<Metric>,
        compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
        startupMode: StartupMode? = null,
        @IntRange(from = 1)
        iterations: Int,
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
        measureBlock: MacrobenchmarkScope.() -> Unit
    ) {
        macrobenchmarkWithStartupMode(
            uniqueName = currentDescription.toUniqueName(),
            className = currentDescription.className,
            testName = currentDescription.methodName,
            config = MacrobenchmarkConfig(
                packageName, metrics, compilationMode, iterations
            ),
            startupMode = startupMode,
            setupBlock = setupBlock,
            measureBlock = measureBlock
        )
    }

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            currentDescription = description
            base.evaluate()
        }
    }

    internal fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
