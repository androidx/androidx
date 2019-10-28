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

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry

/**
 * This allows tests to override arguments from code
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.TESTS)
var argumentSource: Bundle? = null

internal object Arguments {
    val additionalTestOutputDir: String?
    val outputEnable: Boolean
    val startupMode: Boolean
    val dryRunMode: Boolean
    val suppressedErrors: Set<String>

    init {
        val prefix = "androidx.benchmark"
        val arguments = argumentSource ?: InstrumentationRegistry.getArguments()

        dryRunMode = arguments.getString("$prefix.dryRunMode.enable")?.toBoolean() ?: false

        startupMode = !dryRunMode &&
                (arguments.getString("$prefix.startupMode.enable")?.toBoolean() ?: false)

        outputEnable = !dryRunMode &&
                (arguments.getString("$prefix.output.enable")?.toBoolean() ?: false)

        // Transform comma-delimited list into set of suppressed errors
        // E.g. "DEBUGGABLE, UNLOCKED" -> setOf("DEBUGGABLE", "UNLOCKED")
        suppressedErrors = arguments.getString("androidx.benchmark.suppressErrors", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        additionalTestOutputDir = arguments.getString("additionalTestOutputDir")
    }
}