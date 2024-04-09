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
package bench.flame.diff.command

import bench.flame.diff.config.Paths
import bench.flame.diff.interop.isValidFileName
import bench.flame.diff.ui.promptOverwriteFile
import bench.flame.diff.ui.promptProvideFile
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.terminal.ConversionResult
import java.io.File

class Save : CliktCommand(help = "Save a trace file for future comparison.") {
    private val src by mutuallyExclusiveOptions(
        option("--src-file", help = "Path to a trace file.")
            .file(mustExist = true, canBeDir = false),
        option("--src-dir", help = "Path to a directory containing trace files.")
            .file(mustExist = true, canBeFile = false),
        name = "Trace source",
    ).single()

    private val dst by option(help = "Name for the saved trace file.")
        .check { it.isValidFileName() }

    private val pattern by option("--pattern", help = "Trace file name regex.")
        .default(Paths.traceFileNamePattern)

    override fun run() {
        val src = src // allows for smart casts
        val srcFile: File = when {
            src != null && src.isFile -> src
            else -> promptProvideFile("Provide trace source", pattern, src, Paths.outDir.toFile())
        }
        check(srcFile.exists() && srcFile.isFile)

        val dstFile: File =
            Paths.savedTracesDir.resolve(dst ?: promptDestinationName(srcFile.name)).toFile()
        if (dstFile.exists()) promptOverwriteFile(dstFile).let { overwrite ->
            if (!overwrite) return
        }

        dstFile.parentFile.mkdirs() // ensure destination dir is present
        srcFile.copyTo(dstFile, overwrite = true)
    }

    private fun promptDestinationName(default: String? = null): String =
        terminal.prompt("Provide destination file name", default) {
            when {
                it.isValidFileName() -> ConversionResult.Valid(it)
                else -> ConversionResult.Invalid("Invalid value")
            }
        }!!
}
