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

import androidx.test.runner.AndroidJUnitRunner

/**
 * Instrumentation runner for benchmarks, used to increase stability of measurements and minimize
 * interference.
 *
 * To use this runner, put the following in your module level `build.gradle`:
 *
 * ```
 * android {
 *     defaultConfig {
 *         testInstrumentationRunner "androidx.benchmark.AndroidBenchmarkRunner"
 *     }
 * }
 * ```
 */
@Suppress("unused") // Note: not referenced by code
class AndroidBenchmarkRunner : AndroidJUnitRunner() {
    private var firstWaitForActivities = false

    override fun waitForActivitiesToComplete() {
        // We don't call the super method here, since we have
        // an activity we intend to persist between tests
        // TODO: somehow wait for every activity but IsolationActivity

        // Before/After each test, from the test thread, synchronously launch
        // our IsolationActivity if it's not already resumed
        var isResumed = false
        runOnMainSync {
            val activity = IsolationActivity.singleton.get()
            if (activity != null) {
                isResumed = activity.resumed
            }
        }
        if (!isResumed) {
            IsolationActivity.launchSingleton()
        }
    }

    override fun onDestroy() {
        IsolationActivity.finishSingleton()
        super.waitForActivitiesToComplete()
        super.onDestroy()
    }

    /**
     * @hide
     */
    companion object {
        internal val TAG = "Benchmark"
    }
}