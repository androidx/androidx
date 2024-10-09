/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build

import com.android.build.api.variant.HasDeviceTests
import org.gradle.api.Project

/**
 * Enable internal defaults for microbenchmark which can be used to set defaults we aren't ready to
 * apply publicly, or which require root to function.
 *
 * See [androidx.build.testConfiguration.INST_ARG_BLOCKLIST], which can be used to suppress some of
 * these args in CI.
 */
@Suppress("UnstableApiUsage") // usage of HasDeviceTests
internal fun HasDeviceTests.enableMicrobenchmarkInternalDefaults(project: Project) {
    if (project.hasBenchmarkPlugin()) {
        deviceTests.forEach { (_, deviceTest) ->
            // Enables CPU perf event counters both locally, and in CI
            deviceTest.instrumentationRunnerArguments.put(
                "androidx.benchmark.cpuEventCounter.enable",
                "true"
            )

            // Force AndroidX devs to disable JIT on rooted devices
            deviceTest.instrumentationRunnerArguments.put(
                "androidx.benchmark.requireJitDisabledIfRooted",
                "true"
            )

            // Check that speed compilation always used when benchmark invoked
            deviceTest.instrumentationRunnerArguments.put("androidx.benchmark.requireAot", "true")

            // Enables long-running method tracing on the UI thread, even if that risks ANR for
            // profiling convenience.
            // NOTE, this *must* be suppressed in CI!!
            deviceTest.instrumentationRunnerArguments.put(
                "androidx.benchmark.profiling.skipWhenDurationRisksAnr",
                "false"
            )
        }
    }
}
