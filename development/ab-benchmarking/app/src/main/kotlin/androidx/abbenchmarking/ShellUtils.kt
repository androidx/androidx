/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.abbenchmarking

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

// Define the root of the git repository, relative to the app's execution directory.
// The execution directory is .../app, so three levels up is .../frameworks/support
internal val projectRoot = File("../../../").canonicalFile

/**
 * Executes a shell command and captures its output.
 *
 * @param command The command and its arguments to run.
 * @param workingDir The directory in which to run the command. Defaults to the project root.
 * @return The stdout of the command.
 * @throws RuntimeException if the command fails.
 */
internal fun runCommand(vararg command: String, workingDir: File = projectRoot): String {
    val process =
        ProcessBuilder(*command)
            .directory(workingDir) // Set the correct working directory
            .redirectErrorStream(true) // Redirect stderr to stdout for easier capture
            .start()
    val output = process.inputStream.bufferedReader().readText()
    val completed = process.waitFor(5, TimeUnit.MINUTES) // Add a timeout
    if (!completed) {
        process.destroy()
        throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
    }
    if (process.exitValue() != 0) {
        throw RuntimeException(
            "Command failed with exit code ${process.exitValue()}: ${command.joinToString(" ")}\nOutput:\n$output"
        )
    }
    return output
}

/**
 * Executes a shell command silently, redirecting output to a temporary file. Suitable for
 * long-running processes like Gradle builds.
 *
 * @throws RuntimeException if the command fails, times out, or cannot be executed.
 */
internal fun runCommandSilently(
    vararg command: String,
    workingDir: File = projectRoot,
    environment: Map<String, String> = emptyMap(),
) {
    // Create a temporary file to capture the output
    val tempOutputFile = Files.createTempFile("benchmark-run-", ".log").toFile()
    try {
        val processBuilder =
            ProcessBuilder(*command)
                .directory(workingDir)
                // Redirect stdout and stderr to the temporary file
                .redirectOutput(ProcessBuilder.Redirect.to(tempOutputFile))
                .redirectError(ProcessBuilder.Redirect.to(tempOutputFile))

        processBuilder.environment().putAll(environment)

        val process = processBuilder.start()
        // Wait for the process. Use a longer timeout suitable for Gradle builds.
        val completed = process.waitFor(60, TimeUnit.MINUTES)
        if (!completed) {
            process.destroy()
            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
        }
        if (process.exitValue() != 0) {
            // If failed, read the output from the file to include in the error message.
            // We read the last ~8KB for context, as the full log might be very large.
            val errorOutput = tempOutputFile.readText().takeLast(8192)
            throw RuntimeException(
                "Command failed with exit code ${process.exitValue()}: ${command.joinToString(" ")}\n\n--- Captured Output (last 8KB) ---\n$errorOutput\n----------------------------------\n"
            )
        }
    } catch (e: Exception) {
        // Catch potential IOExceptions during process startup
        if (e !is RuntimeException) {
            throw RuntimeException("Error executing command: ${command.joinToString(" ")}", e)
        }
        throw e
    } finally {
        // Ensure the temporary file is deleted, even if an error occurred
        tempOutputFile.delete()
    }
}
