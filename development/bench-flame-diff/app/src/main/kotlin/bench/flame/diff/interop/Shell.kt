/*
 * Copyright 2024 The Android Open Source Project
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
package bench.flame.diff.interop

import bench.flame.diff.config.Paths
import com.github.ajalt.clikt.core.CliktCommand
import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.TimeUnit

internal class Shell(private val cwd: Path) {
    fun exec(vararg command: String): ExecutionResult {
        val handler = object : NuAbstractProcessHandler() {
            var outs = StringBuilder()
            var errs = StringBuilder()
            var exitCode: Int = 0

            override fun onExit(exitCode: Int) {
                this.exitCode = exitCode
            }

            override fun onStdout(buffer: ByteBuffer, closed: Boolean) = consumeLines(buffer, outs)

            override fun onStderr(buffer: ByteBuffer, closed: Boolean) = consumeLines(buffer, errs)

            private fun consumeLines(srcBuffer: ByteBuffer, destination: StringBuilder) {
                val dstBuffer = ByteArray(srcBuffer.remaining())
                srcBuffer.get(dstBuffer)
                destination.append(String(dstBuffer))
            }
        }

        NuProcessBuilder(command.asList()).also {
            it.setCwd(cwd)
            it.setProcessListener(handler)
        }.start().waitFor(0, TimeUnit.SECONDS)

        return ExecutionResult(handler.exitCode, handler.outs.toString(), handler.errs.toString())
    }

    data class ExecutionResult(val exitCode: Int, val stdOut: String, val stdErr: String)
}

internal val Shell.ExecutionResult.output get() = stdOut + stdErr

internal fun CliktCommand.execWithChecks(
    vararg command: String,
    cwd: Path = Paths.currentDir,
    checkIsSuccess: (Shell.ExecutionResult) -> Boolean = { it.exitCode == 0 },
    onError: (Shell.ExecutionResult) -> Unit = {
        exitProcessWithError(
            "error occurred while executing command '${command.asList()}'." +
                    "  \nexit code: ${it.exitCode}" +
                    "  \nstdout: ${it.stdOut}" +
                    "  \nstderr: ${it.stdErr}"
        )
    },
    onSuccess: (Shell.ExecutionResult) -> Unit = { },
) {
    val result = Shell(cwd).exec(*command)
    if (!checkIsSuccess(result)) onError(result) else onSuccess(result)
}
