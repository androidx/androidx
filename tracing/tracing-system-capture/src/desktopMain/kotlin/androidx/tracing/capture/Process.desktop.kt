/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.tracing.capture

import java.io.File
import java.nio.file.Files

/** The current shell being used by the $USER */
internal val SHELL by lazy(LazyThreadSafetyMode.PUBLICATION) { System.getenv("SHELL") }

internal data class Output(val builder: ProcessBuilder, val stdout: File, val stderr: File) :
    AutoCloseable {
    override fun close() {
        stdout.delete()
        stderr.delete()
    }
}

internal fun <T> processBuilder(command: String, action: (output: Output) -> T): T {
    val stdout = Files.createTempFile("tracing_stdout", ".tmp").toFile()
    val stderr = Files.createTempFile("tracing_stderr", ".tmp").toFile()
    // Use the current shell to dispatch the command.
    // Use `exec` to make sure that the process assumes identity of the
    // executed command after the command is dispatched
    // That way process.destroy() rightfully sends a SIGTERM.
    val builder = ProcessBuilder(SHELL, "-c", "exec $command")
    // This is a good default to have.
    // If the builder decides to change things, it still can prior to calling builder.start()
    builder.redirectInput(/* source= */ ProcessBuilder.Redirect.PIPE)
    builder.redirectOutput(/* file= */ stdout)
    builder.redirectError(/* file= */ stderr)
    return action(Output(builder = builder, stdout = stdout, stderr = stderr))
}
