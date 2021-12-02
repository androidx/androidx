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

package androidx.benchmark.integration.dryrun.benchmark

import android.app.Application
import android.os.Bundle
import androidx.benchmark.argumentSource

/**
 * Hack to enable overriding benchmark arguments (since we can't easily do this in CI, per apk)
 *
 * The *correct* way to do this would be to put the following in benchmark/build.gradle:
 *
 * ```
 * android {
 *     defaultConfig {
 *         // Enable startup measurement mode
 *         testInstrumentationRunnerArgument 'androidx.benchmark.startupMode.enable', 'true'
 *         // Enable dry run mode, which overrides startup
 *         testInstrumentationRunnerArgument 'androidx.benchmark.dryRunMode.enable', 'true'
 *     }
 * }
 * ```
 */
class ArgumentInjectingApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        argumentSource = Bundle().apply {
            putString("androidx.benchmark.startupMode.enable", "true") // this should be ignored
            putString("androidx.benchmark.dryRunMode.enable", "true")
            putString("androidx.benchmark.profiling.mode", "none") // noop, tests "none" arg parsing
        }
    }
}