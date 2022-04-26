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

package androidx.benchmark.macro.junit4

import android.Manifest
import androidx.annotation.IntRange
import androidx.benchmark.Arguments
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.macrobenchmarkWithStartupMode
import androidx.test.rule.GrantPermissionRule
import org.junit.Assume.assumeTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking large app operations like startup, scrolling, or animations.
 *
 * ```
 *     @get:Rule
 *     val benchmarkRule = MacrobenchmarkRule()
 *
 *     @Test
 *     fun startup() = benchmarkRule.measureRepeated(
 *         packageName = "mypackage.myapp",
 *         metrics = listOf(StartupTimingMetric()),
 *         iterations = 5,
 *         startupMode = StartupMode.COLD,
 *         setupBlock = {
 *           pressHome()
 *         }
 *     ) { // this = MacrobenchmarkScope
 *         val intent = Intent()
 *         intent.setPackage("mypackage.myapp")
 *         intent.setAction("mypackage.myapp.myaction")
 *         startActivityAndWait(intent)
 *     }
 * ```
 *
 * See the [Macrobenchmark Guide](https://developer.android.com/studio/profile/macrobenchmark)
 * for more information on macrobenchmarks.
 */
public class MacrobenchmarkRule : TestRule {
    private lateinit var currentDescription: Description

    /**
     * Measure behavior of the specified [packageName] given a set of [metrics].
     *
     * This performs a macrobenchmark with the below control flow:
     * ```
     *     resetAppCompilation()
     *     compile(compilationMode)
     *     repeat(iterations) {
     *         setupBlock()
     *         captureTrace {
     *             measureBlock()
     *         }
     *     }
     * ```
     *
     * @param packageName Package name of the app being measured.
     * @param metrics List of metrics to measure.
     * @param compilationMode Mode of compilation used before capturing measurement, such as
     * [CompilationMode.Partial], defaults to [CompilationMode.DEFAULT].
     * @param startupMode Optional mode to force app launches performed with
     * [MacrobenchmarkScope.startActivityAndWait] (and similar variants) to be of the assigned
     * type. For example, `COLD` launches kill the process before the measureBlock, to ensure
     * startups will go through full process creation. Generally, leave as null for non-startup
     * benchmarks.
     * @param iterations Number of times the [measureBlock] will be run during measurement. Note
     * that total iteration count may not match, due to warmup iterations needed for the
     * [compilationMode].
     * @param setupBlock The block performing app actions each iteration, prior to the
     * [measureBlock]. For example, navigating to a UI where scrolling will be measured.
     * @param measureBlock The block performing app actions to benchmark each iteration.
     */
    @JvmOverloads
    public fun measureRepeated(
        packageName: String,
        metrics: List<Metric>,
        compilationMode: CompilationMode = CompilationMode.DEFAULT,
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
            packageName = packageName,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = iterations,
            startupMode = startupMode,
            setupBlock = setupBlock,
            measureBlock = measureBlock
        )
    }

    override fun apply(base: Statement, description: Description): Statement {
        // Grant external storage, as it may be needed for test output directory.
        return RuleChain
            .outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(::applyInternal)
            .apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            assumeTrue(Arguments.RuleType.Macrobenchmark in Arguments.enabledRules)
            currentDescription = description
            base.evaluate()
        }
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
