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

package androidx.testutils

import android.content.Intent
import android.os.Build
import androidx.benchmark.macro.ArtMetric
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.isSupportedWithVmSettings
import androidx.benchmark.macro.junit4.MacrobenchmarkRule

/** Compilation modes to sweep over for jetpack internal macrobenchmarks */
val COMPILATION_MODES =
    if (Build.VERSION.SDK_INT < 24) {
        // other modes aren't supported
        listOf(CompilationMode.Full())
    } else {
        listOf(
            CompilationMode.None(),
            CompilationMode.Interpreted,
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = 3
            ),
            /* For simplicity we use `Partial()`, which will only install baseline profiles if
             * available, which would not be useful for macrobenchmarks that don't include baseline
             * profiles. However baseline profiles are expected to make their way into essentially every
             * jetpack macrobenchmark over time.
             */
            CompilationMode.Partial(),
            CompilationMode.Full()
        )
    }

val STARTUP_MODES =
    listOf(StartupMode.HOT, StartupMode.WARM, StartupMode.COLD).filter {
        // skip StartupMode.HOT on Angler, API 23 - it works locally with same build on Bullhead,
        // but not in Jetpack CI (b/204572406)
        !(Build.VERSION.SDK_INT == 23 && it == StartupMode.HOT && Build.DEVICE == "angler")
    }

/** Temporary, while transitioning to new metrics */
@OptIn(ExperimentalMetricApi::class)
fun getStartupMetrics() =
    listOfNotNull(
        StartupTimingMetric(),
        if (Build.VERSION.SDK_INT >= 24) ArtMetric() else null,
        TraceSectionMetric("StartupTracingInitializer", TraceSectionMetric.Mode.First),
        MemoryUsageMetric(MemoryUsageMetric.Mode.Last)
    )

fun MacrobenchmarkRule.measureStartup(
    compilationMode: CompilationMode,
    startupMode: StartupMode,
    packageName: String,
    iterations: Int = 10,
    metrics: List<Metric> = getStartupMetrics(),
    setupIntent: Intent.() -> Unit = {}
) =
    measureRepeated(
        packageName = packageName,
        metrics = metrics,
        compilationMode = compilationMode,
        iterations = iterations,
        startupMode = startupMode,
        setupBlock = { pressHome() }
    ) {
        val intent = Intent()
        intent.setPackage(packageName)
        setupIntent(intent)
        startActivityAndWait(intent)
    }

/** Baseline Profile compilation mode is considered primary, and always worth measuring */
private fun CompilationMode.isPrimary(): Boolean {
    return if (Build.VERSION.SDK_INT < 24) {
        true
    } else {
        this is CompilationMode.Partial &&
            this.warmupIterations == 0 &&
            (this.baselineProfileMode == BaselineProfileMode.UseIfAvailable ||
                this.baselineProfileMode == BaselineProfileMode.Require)
    }
}

fun createStartupCompilationParams(
    startupModes: List<StartupMode> = STARTUP_MODES,
    compilationModes: List<CompilationMode> = COMPILATION_MODES
): List<Array<Any>> =
    mutableListOf<Array<Any>>().apply {
        // To save CI resources, avoid measuring startup combinations which have non-primary
        // compilation or startup mode (BP, cold respectively) in the default case
        val minimalIntersection =
            startupModes == STARTUP_MODES && compilationModes == COMPILATION_MODES

        for (startupMode in startupModes) {
            for (compilationMode in compilationModes) {
                if (
                    minimalIntersection &&
                        startupMode != StartupMode.COLD &&
                        !compilationMode.isPrimary()
                ) {
                    continue
                }

                // Skip configs that can't run, so they don't clutter Studio benchmark
                // output with AssumptionViolatedException dumps
                if (compilationMode.isSupportedWithVmSettings()) {
                    add(arrayOf(startupMode, compilationMode))
                }
            }
        }
    }

fun createCompilationParams(
    compilationModes: List<CompilationMode> = COMPILATION_MODES
): List<Array<Any>> =
    mutableListOf<Array<Any>>().apply {
        for (compilationMode in compilationModes) {
            // Skip configs that can't run, so they don't clutter Studio benchmark
            // output with AssumptionViolatedException dumps
            if (compilationMode.isSupportedWithVmSettings()) {
                add(arrayOf(compilationMode))
            }
        }
    }
