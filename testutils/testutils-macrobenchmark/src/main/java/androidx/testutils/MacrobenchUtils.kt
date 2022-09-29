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
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingLegacyMetric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.isSupportedWithVmSettings
import androidx.benchmark.macro.junit4.MacrobenchmarkRule

/**
 * Basic, always-usable compilation modes, when baseline profiles aren't available.
 *
 * Over time, it's expected very few macrobenchmarks will reference this directly, as more libraries
 * gain baseline profiles.
 */
val BASIC_COMPILATION_MODES = if (Build.VERSION.SDK_INT < 24) {
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
        CompilationMode.Full()
    )
}

val STARTUP_MODES = listOf(
    StartupMode.HOT,
    StartupMode.WARM,
    StartupMode.COLD
).filter {
    // skip StartupMode.HOT on Angler, API 23 - it works locally with same build on Bullhead,
    // but not in Jetpack CI (b/204572406)
    !(Build.VERSION.SDK_INT == 23 && it == StartupMode.HOT && Build.DEVICE == "angler")
}

/**
 * Default compilation modes to test for all AndroidX macrobenchmarks.
 *
 * Baseline profiles are only supported from Nougat (API 24),
 * currently through Android 12 (API 31)
 */
@Suppress("ConvertTwoComparisonsToRangeCheck") // lint doesn't understand range checks
val COMPILATION_MODES = if (Build.VERSION.SDK_INT >= 24 && Build.VERSION.SDK_INT <= 31) {
    listOf(CompilationMode.Partial())
} else {
    emptyList()
} + BASIC_COMPILATION_MODES

/**
 * Temporary, while transitioning to new metrics
 */
fun getStartupMetrics() = if (Build.VERSION.SDK_INT >= 29) {
    listOf(StartupTimingMetric(), StartupTimingLegacyMetric())
} else {
    listOf(StartupTimingMetric())
}

fun MacrobenchmarkRule.measureStartup(
    compilationMode: CompilationMode,
    startupMode: StartupMode,
    packageName: String,
    iterations: Int = 10,
    setupIntent: Intent.() -> Unit = {}
) = measureRepeated(
    packageName = packageName,
    metrics = getStartupMetrics(),
    compilationMode = compilationMode,
    iterations = iterations,
    startupMode = startupMode,
    setupBlock = {
        pressHome()
    }
) {
    val intent = Intent()
    intent.setPackage(packageName)
    setupIntent(intent)
    startActivityAndWait(intent)
}

fun createStartupCompilationParams(
    startupModes: List<StartupMode> = STARTUP_MODES,
    compilationModes: List<CompilationMode> = COMPILATION_MODES
): List<Array<Any>> = mutableListOf<Array<Any>>().apply {
    for (startupMode in startupModes) {
        for (compilationMode in compilationModes) {
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
): List<Array<Any>> = mutableListOf<Array<Any>>().apply {
    for (compilationMode in compilationModes) {
        // Skip configs that can't run, so they don't clutter Studio benchmark
        // output with AssumptionViolatedException dumps
        if (compilationMode.isSupportedWithVmSettings()) {
            add(arrayOf(compilationMode))
        }
    }
}