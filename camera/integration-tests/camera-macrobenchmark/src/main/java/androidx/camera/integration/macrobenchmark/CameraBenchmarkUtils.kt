/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.StartupInsightsConfig
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.test.platform.app.InstrumentationRegistry

object CameraBenchmarkUtils {
    private const val TARGET_PACKAGE = "androidx.camera.integration.macrobenchmark.target"

    const val SESSION_CONFIG_BIND_ACTIVITY =
        "androidx.camera.integration.macrobenchmark.target.SESSION_CONFIG_BIND_ACTIVITY"
    const val GROUPABLE_FEATURE_DISABLING_ACTIVITY =
        "androidx.camera.integration.macrobenchmark.target.GROUPABLE_FEATURE_DISABLING_ACTIVITY"

    /**
     * Grants the CAMERA permission to the app under test.
     *
     * The more usual GrantPermissionRule approach doesn't work here as that's designed for tests
     * that run within the same process as the app being tested, which is not the case for
     * macro-benchmarks.
     */
    fun grantCameraPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $TARGET_PACKAGE android.permission.CAMERA"
        )
    }

    @OptIn(ExperimentalBenchmarkConfigApi::class, ExperimentalPerfettoCaptureApi::class)
    fun MacrobenchmarkRule.measureStartupDefault(
        setupIntent: Intent.() -> Unit = {},
        measureBlock: MacrobenchmarkScope.() -> Unit = {},
    ) {
        // Based on the default values in MacrobenchUtils.measureStartup()
        return measureRepeated(
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            metrics = listOf(StartupTimingMetric()),
            packageName = TARGET_PACKAGE,
            iterations = 10,
            experimentalConfig =
                ExperimentalConfig(startupInsightsConfig = StartupInsightsConfig(true)),
            setupBlock = { pressHome() },
        ) {
            val intent = Intent()
            intent.setPackage(TARGET_PACKAGE)
            setupIntent(intent)
            startActivityAndWait(intent)

            measureBlock()
        }
    }
}
