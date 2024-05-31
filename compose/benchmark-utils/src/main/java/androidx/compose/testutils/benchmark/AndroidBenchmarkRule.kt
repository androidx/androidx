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

package androidx.compose.testutils.benchmark

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCaseRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Rule to be used to run Android benchmarks. */
class AndroidBenchmarkRule : TestRule {

    @Suppress("DEPRECATION")
    private val activityTestRule =
        androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)

    val benchmarkRule = BenchmarkRule()

    override fun apply(base: Statement, description: Description?): Statement {
        return benchmarkRule.apply(activityTestRule.apply(base, description), description!!)
    }

    /**
     * Runs benchmark for the given [AndroidTestCase].
     *
     * Note that UI setup and benchmark measurements must be explicitly scheduled to the UI thread,
     * as the block runs on the current (test) thread.
     *
     * @param givenTestCase The test case to be executed
     * @param block The benchmark instruction to be performed over the given test case
     */
    fun <T : AndroidTestCase> runBenchmarkFor(
        givenTestCase: () -> T,
        @WorkerThread block: AndroidTestCaseRunner<T>.() -> Unit
    ) {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Cannot invoke runBenchmarkFor from the main thread"
        }
        require(givenTestCase !is ComposeTestCase) {
            "Expected ${AndroidTestCase::class.simpleName}!"
        }
        lateinit var runner: AndroidTestCaseRunner<T>
        runOnUiThread { runner = AndroidTestCaseRunner(givenTestCase, activityTestRule.activity) }

        block(runner)
    }

    /** Convenience proxy for [BenchmarkRule.measureRepeatedOnMainThread]. */
    @WorkerThread
    fun measureRepeatedOnUiThread(@UiThread block: BenchmarkRule.Scope.() -> Unit) {
        benchmarkRule.measureRepeatedOnMainThread(block)
    }

    /** Convenience proxy for `ActivityTestRule.runOnUiThread` */
    fun runOnUiThread(block: () -> Unit) {
        activityTestRule.runOnUiThread(block)
    }
}
