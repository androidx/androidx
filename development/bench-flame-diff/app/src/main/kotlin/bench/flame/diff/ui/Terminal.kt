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
package bench.flame.diff.ui

import bench.flame.diff.interop.FileWithId
import bench.flame.diff.interop.exitProcessWithError
import bench.flame.diff.interop.file
import bench.flame.diff.interop.id
import bench.flame.diff.interop.withId
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

internal fun CliktCommand.printFileTable(files: List<FileWithId>, trimBaseDir: File?) {
    val trimPrefix: String = if (trimBaseDir == null) "" else "${trimBaseDir.canonicalPath}/"
    terminal.println(table {
        whitespace = Whitespace.PRE_WRAP
        header { row("Id", "Name", "Last Modified") }
        body {
            files.map {
                val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(Date(it.file.lastModified()))
                row(it.id, it.file.canonicalPath.removePrefix(trimPrefix), lastModified)
            }
        }
    })
}

internal fun CliktCommand.promptPickFile(candidates: List<FileWithId>, trimBaseDir: File?): File {
    printFileTable(candidates, trimBaseDir)
    return terminal.prompt("Choose a file by id") {
        val number = it.toIntOrNull()
        val min = candidates.minOf { f -> f.id }
        val max = candidates.maxOf { f -> f.id }
        when {
            number == null || candidates.none { f -> f.id == number } ->
                ConversionResult.Invalid("Choose a number between $min and $max")
            else ->
                ConversionResult.Valid(candidates.single { f -> f.id == number }.file)
        }
    }!!
}

internal fun CliktCommand.promptProvideFile(
    prompt: String,
    includePattern: String = ".*",
    excludePattern: String? = null,
    srcDir: File? = null,
    defaultSrcDir: File? = null
): File {
    check(srcDir == null || srcDir.isDirectory)
    check(defaultSrcDir == null || defaultSrcDir.isDirectory)
    val baseDir = run {
        val src = srcDir ?: terminal.prompt(prompt, defaultSrcDir) {
            when {
                it.isBlank() || !File(it).exists() -> ConversionResult.Invalid("Invalid value")
                else -> ConversionResult.Valid(File(it))
            }
        }!!
        if (src.isFile) return src
        src
    }

    check(baseDir.isDirectory)
    echo("Looking for files in '${baseDir.absolutePath}' matching '$includePattern'...")
    val candidates = baseDir.walkTopDown()
        .filter { it.isFile }
        .filter { it.name.matches(Regex(includePattern)) }
        .filter { if (excludePattern == null) true else !it.name.matches(Regex(excludePattern)) }
        .sortedBy { -it.lastModified() }
        .withId()
        .toList()

    if (candidates.isEmpty()) exitProcessWithError(
        "No files matching '$includePattern'" +
                (if (excludePattern == null) "" else " (and excluding $excludePattern)") +
                " in '$baseDir'"
    )
    return promptPickFile(candidates, baseDir)
}

internal fun CliktCommand.promptOverwriteFile(file: File): Boolean = YesNoPrompt(
    "Overwrite existing file '${file.absolutePath}'",
    terminal
).ask()!!
