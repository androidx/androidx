/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.benchmark.perfetto.PerfettoConfig

/**
 * Annotates declarations that are considered experimental within the Benchmark API, and are likely
 * to change before becoming stable. Using experimental features can potentially break your code if
 * the design or behavior changes.
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalBenchmarkConfigApi

@ExperimentalBenchmarkConfigApi
public class ExperimentalConfig(
    val perfettoConfig: PerfettoConfig? = null,
    val startupInsightsConfig: StartupInsightsConfig? = null
)

/** Configuration for startup insights. */
@ExperimentalBenchmarkConfigApi
public class StartupInsightsConfig(val isEnabled: Boolean) {
    /**
     * Base URL for linking to more information about specific startup reasons. This URL should
     * accept a reason ID as a direct suffix. For example, a base URL of
     * `https://developer.android.com/[...]/slow-start-reason#` could be combined with a reason ID
     * of `MAIN_THREAD_MONITOR_CONTENTION` to create a complete URL like:
     * `https://developer.android.com/[...]/slow-start-reason#MAIN_THREAD_MONITOR_CONTENTION`
     */
    val reasonHelpUrlBase: String? = Arguments.startupInsightsHelpUrlBase
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get
}
