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

import androidx.test.platform.app.InstrumentationRegistry

sealed class CompilationMode(
    // for modes other than [None], is argument passed `cmd package compile`
    private val compileArgument: String?
) {
    internal fun compileArgument(): String {
        if (compileArgument == null) {
            throw UnsupportedOperationException("No compileArgument for mode $this")
        }
        return compileArgument
    }

    object None : CompilationMode(null) {
        override fun toString() = "CompilationMode.None"
    }

    class SpeedProfile(val warmupIterations: Int = 3) : CompilationMode("speed-profile") {
        override fun toString() = "CompilationMode.SpeedProfile(iterations=$warmupIterations)"
    }

    object Speed : CompilationMode("speed") {
        override fun toString() = "CompilationMode.Speed"
    }
}

internal fun CompilationMode.compile(packageName: String, block: () -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // Clear profile between runs.
    clearProfile(instrumentation, packageName)
    if (this == CompilationMode.None) {
        return // nothing to do
    }
    if (this is CompilationMode.SpeedProfile) {
        repeat(this.warmupIterations) {
            block()
        }
    }
    // TODO: merge in below method
    compilationFilter(
        InstrumentationRegistry.getInstrumentation(),
        packageName,
        compileArgument()
    )
}