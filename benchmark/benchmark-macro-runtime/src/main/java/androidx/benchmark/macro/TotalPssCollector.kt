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

package androidx.benchmark.macro

import com.android.helpers.TotalPssHelper

/**
 * Helps capture memory related metrics.
 */
class TotalPssCollector(
    private val processNames: List<String>,
    private val minIterations: Int? = null,
    private val maxIterations: Int? = null,
    private val sleepTimeMs: Int? = null,
    private val thresholdTimeMs: Int? = null,
    private val helper: TotalPssHelper = TotalPssHelper()
) : Collector<Long> by helper.collector() {
    init {
        helper.setUp(*processNames.toTypedArray())
        if (minIterations != null) {
            helper.setMinIterations(minIterations)
        }
        if (maxIterations != null) {
            helper.setMaxIterations(maxIterations)
        }
        if (sleepTimeMs != null) {
            helper.setSleepTime(sleepTimeMs)
        }
        if (thresholdTimeMs != null) {
            helper.setSleepTime(thresholdTimeMs)
        }
    }
}
