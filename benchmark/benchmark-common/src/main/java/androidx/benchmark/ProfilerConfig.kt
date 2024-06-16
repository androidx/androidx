/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.os.Build

/**
 * Profiler configuration object.
 *
 * Will eventually allow further configuration, e.g. sampling frequency.
 */
@ExperimentalBenchmarkConfigApi
sealed class ProfilerConfig(internal val profiler: Profiler) {
    class StackSampling() :
        ProfilerConfig(
            profiler =
                if (Build.VERSION.SDK_INT >= 29) {
                    StackSamplingSimpleperf
                } else {
                    StackSamplingLegacy
                }
        )

    class MethodTracing() : ProfilerConfig(profiler = androidx.benchmark.MethodTracing) {
        companion object {
            /**
             * This value is true if method tracing affects future performance measurement within
             * the process on the current device.
             *
             * This is determined both by the OS version and ART mainline module version (if
             * present).
             *
             * If this value is true, no measurements can be made by microbenchmark after a method
             * trace completes, and further benchmarks will throw rather than capturing invalid
             * metrics until the next process restart.
             *
             * You can use `assumeFalse(MethodTracing.affectsMeasurementOnThisDevice)` to skip tests
             * that use method tracing, configure your profiler based on this flag, or select a
             * different device to avoid runtime crashes.
             *
             * Note that by default, microbenchmarks enable method tracing only when this value is
             * false.
             *
             * Method tracing is always performed after any measurement phases, so single method
             * benchmark runs are not affected - throwing will happen starting with the 2nd
             * benchmark in a suite (process invocation).
             */
            @Suppress("GetterSetterNames")
            @JvmField
            val AFFECTS_MEASUREMENTS_ON_THIS_DEVICE: Boolean =
                DeviceInfo.methodTracingAffectsMeasurements
        }
    }

    // used for tests
    internal class StackSamplingLegacy() : ProfilerConfig(profiler = StackSamplingLegacy)
}
