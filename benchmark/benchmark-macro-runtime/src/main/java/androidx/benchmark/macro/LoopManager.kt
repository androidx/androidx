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

import android.app.Instrumentation

internal typealias Scoped = LoopManagerScope.() -> Unit

/**
 * Define helpers that help drive tests.
 */
data class LoopManagerScope(
    val packageName: String,
    val instrumentation: Instrumentation,
    val collectors: List<Collector<*>>
) {
    // Kill Process
    fun killProcess() = killProcess(instrumentation, packageName)

    // Drop caches
    fun dropCaches() = dropCaches(instrumentation)

    // Compile step
    fun compile(mode: String, profileSaveTimeout: Long = 5_000) =
        compilationFilter(instrumentation, packageName, mode, profileSaveTimeout)

    fun pressHome() = pressHome(instrumentation)

    fun runWithMeasurementDisabled(block: () -> Unit) {
        try {
            collectors.stop()
            block.invoke()
        } finally {
            collectors.start()
        }
    }
}

/**
 * Can be used to kick off Macro-Benchmark tests.
 */
class LoopManager(
    private val packageName: String,
    private val instrumentation: Instrumentation,
    private val collectors: List<Collector<*>>
) {
    fun measureRepeated(iterations: Int, block: LoopManagerScope.(iteration: Int) -> Unit) {
        val scope = LoopManagerScope(packageName, instrumentation, collectors)
        collectors.start()
        for (i in 0..iterations) {
            block.invoke(scope, i)
        }
        collectors.stop()
    }
}
