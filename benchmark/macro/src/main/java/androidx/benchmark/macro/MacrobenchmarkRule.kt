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

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking large app operations like startup.
 */
class MacrobenchmarkRule : TestRule {
    lateinit var benchmarkName: String

    fun measureRepeated(
        config: MacrobenchmarkConfig,
        setupBlock: MacrobenchmarkScope.(Boolean) -> Unit = {},
        measureBlock: MacrobenchmarkScope.() -> Unit
    ) {
        macrobenchmark(
            benchmarkName = benchmarkName,
            config = config,
            launchWithClearTask = true,
            setupBlock = setupBlock,
            measureBlock = measureBlock
        )
    }

    fun measureStartupRepeated(
        config: MacrobenchmarkConfig,
        startupMode: StartupMode,
        performStartup: MacrobenchmarkScope.() -> Unit
    ) {
        startupMacrobenchmark(
            benchmarkName = benchmarkName,
            config = config,
            startupMode = startupMode,
            performStartup = performStartup
        )
    }

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            benchmarkName = description.toUniqueName()
            base.evaluate()
        }
    }

    internal fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
