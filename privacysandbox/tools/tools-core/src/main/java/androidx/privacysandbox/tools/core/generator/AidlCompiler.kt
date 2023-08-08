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

package androidx.privacysandbox.tools.core.generator

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class AidlCompiler(
    private val aidlCompilerPath: Path,
    private val frameworkAidlPath: Path?,
    private val aidlCompileTimeoutMs: Long = 10_000,
) {

    fun compile(workingDir: Path, sources: List<Path>) {
        // TODO(b/269458005): Remove this fallback and make frameworkAidlPath non-nullable
        //  (dependent on our callers always providing the argument)
        val bundleFile = Files.createTempFile("bundle", "aidl")
        val preProcessedAidlPath = if (frameworkAidlPath != null) {
            frameworkAidlPath
        } else {
            bundleFile.writeText("parcelable android.os.Bundle;")
            bundleFile.toAbsolutePath()
        }

        try {
            val command =
                listOf(
                    aidlCompilerPath.toString(),
                    "--lang=java",
                    "--include=$workingDir",
                    "--preprocessed=$preProcessedAidlPath",
                    "--out=$workingDir",
                    *sources.map(Path::toString).toTypedArray()
                )
            val process = ProcessBuilder(command).start()
            val timedOut = !process.waitFor(aidlCompileTimeoutMs, TimeUnit.MILLISECONDS)

            if (timedOut) {
                throw Exception("AIDL compiler timed out: $command")
            }
            if (process.exitValue() != 0) {
                throw Exception(
                    "AIDL compiler didn't terminate successfully (exit code: " +
                        "${process.exitValue()}).\n" +
                        "Command: '$command'\n" +
                        "Errors: ${getProcessErrors(process)}"
                )
            }
        } finally {
            bundleFile.deleteIfExists()
        }
    }

    private fun getProcessErrors(process: Process): String {
        try {
            process.errorStream.bufferedReader().use { outputReader ->
                return outputReader.lines().toArray()
                    .joinToString(separator = "\n\t", prefix = "\n\t")
            }
        } catch (e: IOException) {
            return "Error when printing output of command: ${e.message}"
        }
    }
}
