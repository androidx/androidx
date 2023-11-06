/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.stableaidl.internal.process

import com.android.build.gradle.internal.LoggerWrapper
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessInfo
import com.android.ide.common.process.ProcessOutput
import com.android.ide.common.process.ProcessOutputHandler
import com.android.ide.common.process.ProcessResult
import com.google.common.base.Throwables
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.io.IOException
import java.util.function.Function
import java.util.stream.Collectors
import org.gradle.api.Action
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

/**
 * Implementation of ProcessExecutor that uses Gradle's mechanism to execute external processes.
 *
 * Cloned from `com.android.build.gradle.internal.process.GradleProcessExecutor`.
 */
class GradleProcessExecutor(
    private val execOperations: Function<Action<in ExecSpec>, ExecResult>
) : ProcessExecutor {
    override fun submit(
        processInfo: ProcessInfo,
        processOutputHandler: ProcessOutputHandler
    ): ListenableFuture<ProcessResult> {
        val res = SettableFuture.create<ProcessResult>()
        object : Thread() {
            override fun run() {
                try {
                    val result = execute(processInfo, processOutputHandler)
                    res.set(result)
                } catch (e: Throwable) {
                    res.setException(e)
                }
            }
        }.start()
        return res
    }

    override fun execute(
        processInfo: ProcessInfo,
        processOutputHandler: ProcessOutputHandler
    ): ProcessResult {
        val output = processOutputHandler.createOutput()
        val result: ExecResult = try {
            execOperations.apply(ExecAction(processInfo, output))
        } finally {
            try {
                output.close()
            } catch (e: IOException) {
                LoggerWrapper.getLogger(GradleProcessExecutor::class.java)
                    .warning(
                        "Exception while closing sub process streams: " +
                            Throwables.getStackTraceAsString(e)
                    )
            }
        }
        try {
            processOutputHandler.handleOutput(output)
        } catch (e: ProcessException) {
            return OutputHandlerFailedGradleProcessResult(e)
        }
        return GradleProcessResult(result, processInfo)
    }

    private class ExecAction(
        private val processInfo: ProcessInfo,
        private val processOutput: ProcessOutput
    ) : Action<ExecSpec> {
        override fun execute(execSpec: ExecSpec) {

            /*
             * Gradle doesn't work correctly when there are empty args.
             */
            val args = processInfo.args.stream()
                .map { a: String -> a.ifEmpty { "\"\"" } }
                .collect(Collectors.toList())
            execSpec.executable = processInfo.executable
            execSpec.args(args)
            execSpec.environment(processInfo.environment)
            execSpec.setStandardOutput(processOutput.standardOutput)
            execSpec.setErrorOutput(processOutput.errorOutput)
            val directory = processInfo.workingDirectory
            if (directory != null) {
                execSpec.workingDir = directory
            }

            // we want the caller to be able to do its own thing.
            execSpec.setIgnoreExitValue(true)
        }
    }
}
