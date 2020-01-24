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
import android.os.Bundle
import android.os.Environment
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * This allows tests to override arguments from code
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.TESTS)
var argumentSource: Bundle? = null

internal object Arguments {
    val testOutputDir: File
    val outputEnable: Boolean
    val startupMode: Boolean
    val dryRunMode: Boolean
    val suppressedErrors: Set<String>
    val profilingMode: ProfilingMode

    enum class ProfilingMode {
        None, Sampled, Method
    }

    val prefix = "androidx.benchmark."

    private fun Bundle.getArgument(key: String, defaultValue: String = "") =
        getString(prefix + key, defaultValue)

    @SuppressLint("DefaultLocale")
    private fun Bundle.getProfilingMode(): ProfilingMode {
        val argumentName = "profiling.mode"
        val argumentValue = getArgument(argumentName, "none")
        return try {
            ProfilingMode.valueOf(argumentValue.capitalize())
        } catch (e: IllegalArgumentException) {
            val validOptions = ProfilingMode.values().joinToString()
            throw IllegalArgumentException(
                "Could not parse $prefix$argumentName=$argumentValue, must be one of:$validOptions",
                e
            )
        }
    }

    init {
        val arguments = argumentSource ?: InstrumentationRegistry.getArguments()

        dryRunMode = arguments.getArgument("dryRunMode.enable")?.toBoolean() ?: false

        startupMode = !dryRunMode &&
                (arguments.getArgument("startupMode.enable")?.toBoolean() ?: false)

        outputEnable = !dryRunMode &&
                (arguments.getArgument("output.enable")?.toBoolean() ?: false)

        // Transform comma-delimited list into set of suppressed errors
        // E.g. "DEBUGGABLE, UNLOCKED" -> setOf("DEBUGGABLE", "UNLOCKED")
        suppressedErrors = arguments.getArgument("suppressErrors", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        profilingMode = if (outputEnable) arguments.getProfilingMode() else ProfilingMode.None

        val additionalTestOutputDir = arguments.getString("additionalTestOutputDir")
        @Suppress("DEPRECATION") // Legacy code path for versions of agp older than 3.6
        testOutputDir = additionalTestOutputDir?.let { File(it) }
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }
}