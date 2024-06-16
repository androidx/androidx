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

package androidx.benchmark.darwin.gradle

import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations

/** Executes the [ExecOperations] quietly. */
fun ExecOperations.executeQuietly(args: List<String>) {
    val output = ByteArrayOutputStream()
    // Combine stdout and stderr here. So when we need to surface exceptions
    // the timeline is consistent.
    output.use {
        val result = exec { spec ->
            spec.commandLine = args
            spec.standardOutput = output
            spec.errorOutput = output
            // Throw a better exception
            spec.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            // Throw the exception with the full context.
            throw GradleException(
                """
                    ${output.toString(Charsets.UTF_8)}
                """
                    .trimIndent()
            )
        }
    }
}
