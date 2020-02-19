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

import android.os.Build
import android.os.Trace
import androidx.annotation.RestrictTo

/*
 * Simple reimplementation of TraceCompat, to avoid androidx.core dependency for two methods
 *
 * Note:
 * Benchmark isn't especially careful about terminating tracing sections, because our APIs are
 * called *from* the long-running synchronous chunks of work, instead of wrapping them.
 *
 * We try and match up begi/end pairs, but it's entirely possibly that ends will be missed due to
 * the app returning early from the benchmark loop (whether intentionally, or by crashing).
 *
 * Because there's no elegant way to fix this, we just accept that crashing benchmarks can result in
 * never-terminating traces from benchmark.
 */

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun beginTraceSection(sectionName: String) {
    if (Build.VERSION.SDK_INT >= 18) {
        Trace.beginSection(sectionName)
    }
}

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun endTraceSection() {
    if (Build.VERSION.SDK_INT >= 18) {
        Trace.endSection()
    }
}