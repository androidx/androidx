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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.annotation.CallSuper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import kotlin.concurrent.thread

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
 *
 * ## Minimizing Interference
 *
 * This runner launches a simple opaque activity used to reduce benchmark interference from other
 * windows. Launching other activities is supported e.g. via ActivityTestRule and ActivityScenario -
 * the opaque activity will be relaunched if not actively running before each test, and after each
 * test's cleanup is complete.
 *
 * For example, sources of potential interference:
 * - live wallpaper rendering
 * - homescreen widget updates
 * - hotword detection
 * - status bar repaints
 * - running in background (some cores may be foreground-app only)
 *
 * ## Clock Stability
 *
 * While it is better for performance stability to lock clocks with the `./gradlew lockClocks` task
 * provided by the gradle plugin, this is not possible on most devices. The runner provides a
 * fallback mode for preventing thermal throttling.
 *
 * On devices that support [android.view.Window.setSustainedPerformanceMode], the runner will set
 * this mode on the window of every Activity launched (including the opaque Activity mentioned
 * above). The runner will also launch a continuously spinning Thread. Together, these ensure that
 * the app runs in the multithreaded stable performance mode, which locks the maximum clock
 * frequency to prevent thermal throttling. This ensures stable clock levels across all benchmarks,
 * even if a continuous suite of benchmarks runs for many minutes on end.
 */
@Suppress("unused") // Note: not referenced by code
open class AndroidBenchmarkRunner : AndroidJUnitRunner() {
    @CallSuper
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)

        // Because these values are used by WarningState, it's important to set this flag as early
        // as possible, before WarningState gets lazily initialized. Otherwise we may print false
        // warnings about needing the runner, when the runner simply hasn't initialized yet.
        runnerInUse = true
        sustainedPerformanceModeInUse = !CpuInfo.locked && isSustainedPerformanceModeSupported()

        if (sustainedPerformanceModeInUse) {
            // Keep at least one core busy. Together with a single threaded benchmark, this makes
            // the process get multi-threaded setSustainedPerformanceMode.
            //
            // We want to keep to the relatively lower clocks of the multi-threaded benchmark mode
            // to avoid any benchmarks running at higher clocks than any others.
            //
            // Note, thread names have 15 char max in Systrace
            thread(name = "BenchSpinThread") {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
                while (true) {}
            }
        }
    }

    @CallSuper
    override fun callActivityOnStart(activity: Activity) {
        super.callActivityOnStart(activity)

        @SuppressLint("NewApi") // window API guarded by [sustainedPerfMode]
        if (sustainedPerformanceModeInUse) {
            activity.window.setSustainedPerformanceMode(true)
        }
    }

    @CallSuper
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

    @CallSuper
    override fun onDestroy() {
        IsolationActivity.finishSingleton()
        super.waitForActivitiesToComplete()
        super.onDestroy()
    }

    internal companion object {
        /**
         * Tracks whether Runner is in use.
         */
        var runnerInUse = false

        /**
         * Tracks whether Runner is using [android.view.Window.setSustainedPerformanceMode] to
         * prevent thermal throttling.
         */
        var sustainedPerformanceModeInUse = false

        fun isSustainedPerformanceModeSupported(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isSustainedPerformanceModeSupported
            } else {
                false
            }
    }
}