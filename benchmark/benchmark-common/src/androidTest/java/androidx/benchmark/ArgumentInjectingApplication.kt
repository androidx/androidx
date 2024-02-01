/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.app.Application
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Hack to enable overriding benchmark arguments (since we can't easily do this in CI, per apk)
 *
 * The *correct* way to do this would be to put the following in benchmark/build.gradle:
 *
 * ```
 * android {
 *     defaultConfig {
 *         testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
 *             "CODE-COVERAGE,DEBUGGABLE,EMULATOR,LOW-BATTERY,UNLOCKED"
 *     }
 * }
 * ```
 */
class ArgumentInjectingApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        argumentSource = Bundle().apply {
            // allow cli args to pass through
            putAll(InstrumentationRegistry.getArguments())

            // Since these benchmark correctness tests run as part of the regular
            // (non-performance-test) suite, they will have debuggable=true, won't be clock-locked,
            // can run with low-battery or on an emulator, and code coverage enabled.
            // We also don't have the activity up for these correctness tests, instead
            // leaving testing that behavior to the junit4 module.
            putString(
                "androidx.benchmark.suppressErrors",
                "ACTIVITY-MISSING,CODE-COVERAGE,DEBUGGABLE,EMULATOR,LOW-BATTERY,UNLOCKED," +
                    "UNSUSTAINED-ACTIVITY-MISSING,ENG-BUILD"
            )
            putString(
                "androidx.benchmark.thermalThrottle.sleepDurationSeconds",
                "0"
            )
            // TODO: consider moving default directory to files dir.
            putString(
                "additionalTestOutputDir",
                InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
            )
        }
        // Since we don't care about measurement accuracy in these correctness test, enable method
        // tracing to occur multiple times without killing the process.
        BenchmarkState.enableMethodTracingAffectsMeasurementError = false
    }
}
